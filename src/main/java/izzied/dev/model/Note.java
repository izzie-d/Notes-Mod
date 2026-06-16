package izzied.dev.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Note {
	private String id = UUID.randomUUID().toString();
	private String categoryId;
	private NoteType type = NoteType.STANDARD;
	private String title = "Untitled Note";
	private String content = "";
	private Instant createdAt = Instant.now();
	private Instant editedAt = Instant.now();
	private boolean pinned;
	private Priority priority = Priority.MEDIUM;
	private List<ChecklistItem> checklist = new ArrayList<>();
	private List<ProgressEntry> progress = new ArrayList<>();
	private Reminder reminder;

	public String getId() {
		return id;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
		touch();
	}

	public NoteType getType() {
		return type == null ? NoteType.STANDARD : type;
	}

	public void setType(NoteType type) {
		this.type = type == null ? NoteType.STANDARD : type;
		touch();
	}

	public String getTitle() {
		return title == null || title.isBlank() ? "Untitled Note" : title;
	}

	public void setTitle(String title) {
		this.title = title == null || title.isBlank() ? "Untitled Note" : title.strip();
		touch();
	}

	public String getContent() {
		return content == null ? "" : content;
	}

	public void setContent(String content) {
		this.content = content == null ? "" : content;
		touch();
	}

	public Instant getCreatedAt() {
		return createdAt == null ? Instant.now() : createdAt;
	}

	public Instant getEditedAt() {
		return editedAt == null ? getCreatedAt() : editedAt;
	}

	public boolean isPinned() {
		return pinned;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
		touch();
	}

	public Priority getPriority() {
		return priority == null ? Priority.MEDIUM : priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority == null ? Priority.MEDIUM : priority;
		touch();
	}

	public List<ChecklistItem> getChecklist() {
		if (checklist == null) {
			checklist = new ArrayList<>();
		}
		return checklist;
	}

	public List<ProgressEntry> getProgress() {
		if (progress == null) {
			progress = new ArrayList<>();
		}
		return progress;
	}

	public Reminder getReminder() {
		return reminder;
	}

	public void setReminder(Reminder reminder) {
		this.reminder = reminder;
		touch();
	}

	public void touch() {
		editedAt = Instant.now();
	}

	public Note copy(String targetCategoryId) {
		Note copy = new Note();
		copy.categoryId = targetCategoryId;
		copy.type = getType();
		copy.title = getTitle() + " Copy";
		copy.content = getContent();
		copy.priority = getPriority();
		copy.pinned = false;
		for (ChecklistItem item : getChecklist()) {
			copy.getChecklist().add(item.copy());
		}
		for (ProgressEntry entry : getProgress()) {
			copy.getProgress().add(entry.copy());
		}
		if (reminder != null) {
			copy.reminder = reminder.copy();
		}
		copy.createdAt = Instant.now();
		copy.editedAt = copy.createdAt;
		return copy;
	}
}
