package izzied.dev.search;

import izzied.dev.model.ChecklistItem;
import izzied.dev.model.Note;
import izzied.dev.model.NoteCategory;
import izzied.dev.model.ProgressEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotesSearchIndex {
	private final Map<String, String> searchableText = new HashMap<>();

	public void rebuild(Collection<Note> notes, Collection<NoteCategory> categories) {
		searchableText.clear();
		Map<String, String> categoryNames = new HashMap<>();
		for (NoteCategory category : categories) {
			categoryNames.put(category.getId(), category.getName());
		}
		for (Note note : notes) {
			StringBuilder builder = new StringBuilder()
				.append(note.getTitle()).append(' ')
				.append(note.getContent()).append(' ')
				.append(note.getPriority().name()).append(' ')
				.append(categoryNames.getOrDefault(note.getCategoryId(), ""));
			for (ChecklistItem item : note.getChecklist()) {
				builder.append(' ').append(item.getText());
			}
			for (ProgressEntry progress : note.getProgress()) {
				builder.append(' ').append(progress.getLabel());
			}
			searchableText.put(note.getId(), normalize(builder.toString()));
		}
	}

	public List<Note> search(Collection<Note> notes, String query, SearchFilter filter) {
		String needle = normalize(query);
		Instant recentCutoff = Instant.now().minus(Duration.ofDays(7));
		List<Note> results = new ArrayList<>();
		for (Note note : notes) {
			if (filter != null && filter.getCategoryId() != null && !filter.getCategoryId().equals(note.getCategoryId())) {
				continue;
			}
			if (filter != null && filter.getPriority() != null && filter.getPriority() != note.getPriority()) {
				continue;
			}
			if (filter != null && filter.isRemindersOnly() && note.getReminder() == null) {
				continue;
			}
			if (filter != null && filter.isRecentlyEditedOnly() && note.getEditedAt().isBefore(recentCutoff)) {
				continue;
			}
			String haystack = searchableText.getOrDefault(note.getId(), "");
			if (needle.isEmpty() || haystack.contains(needle)) {
				results.add(note);
			}
		}
		results.sort(
			Comparator.comparingInt((Note note) -> note.getPriority().ordinal()).reversed()
				.thenComparing(Note::getCreatedAt)
		);
		return results;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
	}
}
