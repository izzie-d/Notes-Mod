package izzied.dev.model;

import java.util.UUID;

public class ChecklistItem {
	private String id = UUID.randomUUID().toString();
	private String text = "";
	private boolean done;

	public ChecklistItem() {
	}

	public ChecklistItem(String text, boolean done) {
		this.text = text;
		this.done = done;
	}

	public String getId() {
		return id;
	}

	public String getText() {
		return text == null ? "" : text;
	}

	public void setText(String text) {
		this.text = text == null ? "" : text;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public ChecklistItem copy() {
		return new ChecklistItem(getText(), done);
	}
}
