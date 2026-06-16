package izzied.dev.service;

import izzied.dev.model.HudSettings;
import izzied.dev.model.Note;
import izzied.dev.model.NoteCategory;
import izzied.dev.model.NoteType;
import izzied.dev.model.NotesData;
import izzied.dev.model.Priority;
import izzied.dev.search.NotesSearchIndex;
import izzied.dev.search.SearchFilter;
import izzied.dev.storage.NotesStorage;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NotesManager {
	private final NotesStorage storage;
	private final NotesSearchIndex searchIndex = new NotesSearchIndex();
	private NotesData data;

	public NotesManager(NotesStorage storage) {
		this.storage = storage;
		this.data = storage.load();
		ensureCategoryExists();
		rebuildSearch();
	}

	public List<NoteCategory> getCategories() {
		return Collections.unmodifiableList(data.getCategories());
	}

	public List<Note> getNotes() {
		return Collections.unmodifiableList(data.getNotes());
	}

	public HudSettings getHudSettings() {
		return data.getHud();
	}

	public NoteCategory createCategory(String name) {
		NoteCategory category = new NoteCategory(name);
		data.getCategories().add(category);
		save();
		return category;
	}

	public void renameCategory(String categoryId, String name) {
		findCategory(categoryId).ifPresent(category -> {
			category.setName(name);
			save();
		});
	}

	public void deleteCategory(String categoryId) {
		if (data.getCategories().size() <= 1) {
			return;
		}
		Optional<NoteCategory> category = findCategory(categoryId);
		if (category.isEmpty()) {
			return;
		}
		data.getCategories().remove(category.get());
		String fallbackId = data.getCategories().get(0).getId();
		for (Note note : data.getNotes()) {
			if (categoryId.equals(note.getCategoryId())) {
				note.setCategoryId(fallbackId);
			}
		}
		save();
	}

	public void toggleCategoryCollapsed(String categoryId) {
		findCategory(categoryId).ifPresent(category -> {
			category.setCollapsed(!category.isCollapsed());
			save();
		});
	}

	public int countNotes(String categoryId) {
		int count = 0;
		for (Note note : data.getNotes()) {
			if (categoryId.equals(note.getCategoryId())) {
				count++;
			}
		}
		return count;
	}

	public Note createNote(String categoryId, NoteType type) {
		Note note = new Note();
		note.setCategoryId(resolveCategoryId(categoryId));
		note.setType(type);
		if (type == NoteType.TASK) {
			note.setTitle("New Task Note");
			note.setPriority(Priority.MEDIUM);
		}
		data.getNotes().add(note);
		save();
		return note;
	}

	public void updateNote(Note note, String title, String content) {
		note.setTitle(title);
		note.setContent(content);
		save();
	}

	public void deleteNote(String noteId) {
		data.getNotes().removeIf(note -> note.getId().equals(noteId));
		save();
	}

	public Note duplicateNote(String noteId) {
		Optional<Note> source = findNote(noteId);
		if (source.isEmpty()) {
			return null;
		}
		Note copy = source.get().copy(source.get().getCategoryId());
		data.getNotes().add(copy);
		save();
		return copy;
	}

	public void moveNote(String noteId, String categoryId) {
		findNote(noteId).ifPresent(note -> {
			note.setCategoryId(resolveCategoryId(categoryId));
			save();
		});
	}

	public void togglePinned(String noteId) {
		findNote(noteId).ifPresent(note -> {
			note.setPinned(!note.isPinned());
			save();
		});
	}

	public void cyclePriority(String noteId) {
		findNote(noteId).ifPresent(note -> {
			note.setPriority(note.getPriority().next());
			save();
		});
	}

	public void scheduleReminder(String noteId, Duration delay, boolean repeating) {
		findNote(noteId).ifPresent(note -> {
			izzied.dev.model.Reminder reminder = note.getReminder();
			if (reminder == null) {
				reminder = new izzied.dev.model.Reminder();
				note.setReminder(reminder);
			}
			reminder.setEnabled(true);
			reminder.setTriggerAt(Instant.now().plus(delay));
			if (repeating) {
				reminder.setRepeat(izzied.dev.model.ReminderRepeat.CUSTOM);
				reminder.setRepeatSeconds(Math.max(1, delay.toSeconds()));
			} else {
				reminder.setRepeat(izzied.dev.model.ReminderRepeat.NONE);
			}
			save();
		});
	}

	public void snoozeReminder(String noteId, Duration duration) {
		findNote(noteId).ifPresent(note -> {
			if (note.getReminder() != null) {
				note.getReminder().snooze(duration);
				save();
			}
		});
	}

	public void disableReminder(String noteId) {
		findNote(noteId).ifPresent(note -> {
			if (note.getReminder() != null) {
				note.getReminder().setEnabled(false);
				save();
			}
		});
	}

	public List<Note> search(String query, SearchFilter filter) {
		return searchIndex.search(data.getNotes(), query, filter);
	}

	public List<Note> getPinnedNotes() {
		List<Note> pinned = new ArrayList<>();
		for (Note note : data.getNotes()) {
			if (note.isPinned()) {
				pinned.add(note);
			}
		}
		pinned.sort(
			Comparator.comparingInt((Note note) -> note.getPriority().ordinal()).reversed()
				.thenComparing(Note::getCreatedAt)
		);
		return pinned;
	}

	public Optional<Note> findNote(String noteId) {
		return data.getNotes().stream().filter(note -> note.getId().equals(noteId)).findFirst();
	}

	public Optional<NoteCategory> findCategory(String categoryId) {
		return data.getCategories().stream().filter(category -> category.getId().equals(categoryId)).findFirst();
	}

	public String getCategoryName(String categoryId) {
		return findCategory(categoryId).map(NoteCategory::getName).orElse("Uncategorized");
	}

	public void toggleHudVisible() {
		data.getHud().setVisible(!data.getHud().isVisible());
		save();
	}

	public void save() {
		ensureCategoryExists();
		rebuildSearch();
		storage.save(data);
	}

	private void rebuildSearch() {
		searchIndex.rebuild(data.getNotes(), data.getCategories());
	}

	private void ensureCategoryExists() {
		if (data.getCategories().isEmpty()) {
			data.getCategories().add(new NoteCategory("General"));
		}
		String fallbackId = data.getCategories().get(0).getId();
		for (Note note : data.getNotes()) {
			if (note.getCategoryId() == null || findCategory(note.getCategoryId()).isEmpty()) {
				note.setCategoryId(fallbackId);
			}
		}
	}

	private String resolveCategoryId(String categoryId) {
		return findCategory(categoryId).map(NoteCategory::getId).orElse(data.getCategories().get(0).getId());
	}
}
