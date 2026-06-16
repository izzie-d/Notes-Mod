package izzied.dev.model;

import java.time.Duration;
import java.time.Instant;

public class Reminder {
	private boolean enabled = true;
	private Instant triggerAt = Instant.now().plus(Duration.ofMinutes(30));
	private ReminderRepeat repeat = ReminderRepeat.NONE;
	private long repeatMinutes;
	private long repeatSeconds;
	private boolean sound = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Instant getTriggerAt() {
		return triggerAt == null ? Instant.now() : triggerAt;
	}

	public void setTriggerAt(Instant triggerAt) {
		this.triggerAt = triggerAt == null ? Instant.now() : triggerAt;
	}

	public ReminderRepeat getRepeat() {
		return repeat == null ? ReminderRepeat.NONE : repeat;
	}

	public void setRepeat(ReminderRepeat repeat) {
		this.repeat = repeat == null ? ReminderRepeat.NONE : repeat;
	}

	public long getRepeatMinutes() {
		return repeatMinutes;
	}

	public void setRepeatMinutes(long repeatMinutes) {
		this.repeatMinutes = Math.max(0, repeatMinutes);
	}

	public long getRepeatSeconds() {
		if (repeatSeconds > 0) {
			return repeatSeconds;
		}
		return repeatMinutes > 0 ? Duration.ofMinutes(repeatMinutes).toSeconds() : 0;
	}

	public void setRepeatSeconds(long repeatSeconds) {
		this.repeatSeconds = Math.max(0, repeatSeconds);
		this.repeatMinutes = this.repeatSeconds / 60;
	}

	public boolean isSound() {
		return sound;
	}

	public void setSound(boolean sound) {
		this.sound = sound;
	}

	public void snooze(Duration duration) {
		triggerAt = Instant.now().plus(duration);
		enabled = true;
	}

	public void scheduleNext() {
		if (getRepeat() == ReminderRepeat.NONE) {
			enabled = false;
			return;
		}
		long seconds = getRepeatSeconds();
		if (seconds <= 0) {
			seconds = Duration.ofMinutes(getRepeat().getDefaultMinutes()).toSeconds();
		}
		triggerAt = Instant.now().plus(Duration.ofSeconds(seconds));
	}

	public Reminder copy() {
		Reminder copy = new Reminder();
		copy.enabled = enabled;
		copy.triggerAt = getTriggerAt();
		copy.repeat = getRepeat();
		copy.repeatMinutes = repeatMinutes;
		copy.repeatSeconds = repeatSeconds;
		copy.sound = sound;
		return copy;
	}
}
