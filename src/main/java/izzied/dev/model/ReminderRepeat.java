package izzied.dev.model;

public enum ReminderRepeat {
	NONE(0),
	HOURLY(60),
	DAILY(1440),
	CUSTOM(120);

	private final long defaultMinutes;

	ReminderRepeat(long defaultMinutes) {
		this.defaultMinutes = defaultMinutes;
	}

	public long getDefaultMinutes() {
		return defaultMinutes;
	}
}
