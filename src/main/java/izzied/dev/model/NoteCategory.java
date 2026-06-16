package izzied.dev.model;

import java.time.Instant;
import java.util.UUID;

public class NoteCategory {
	private String id = UUID.randomUUID().toString();
	private String name = "New Category";
	private boolean collapsed;
	private Instant createdAt = Instant.now();

	public NoteCategory() {
	}

	public NoteCategory(String name) {
		setName(name);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name == null || name.isBlank() ? "New Category" : name;
	}

	public void setName(String name) {
		this.name = name == null || name.isBlank() ? "New Category" : name.strip();
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
	}

	public Instant getCreatedAt() {
		return createdAt == null ? Instant.now() : createdAt;
	}
}
