package izzied.dev.search;

import izzied.dev.model.Priority;

public class SearchFilter {
	private String categoryId;
	private Priority priority;
	private boolean remindersOnly;
	private boolean recentlyEditedOnly;

	public String getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public boolean isRemindersOnly() {
		return remindersOnly;
	}

	public void setRemindersOnly(boolean remindersOnly) {
		this.remindersOnly = remindersOnly;
	}

	public boolean isRecentlyEditedOnly() {
		return recentlyEditedOnly;
	}

	public void setRecentlyEditedOnly(boolean recentlyEditedOnly) {
		this.recentlyEditedOnly = recentlyEditedOnly;
	}
}
