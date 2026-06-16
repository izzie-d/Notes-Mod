package izzied.dev.service;

import izzied.dev.model.Note;
import izzied.dev.model.Reminder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.time.Duration;
import java.time.Instant;

public class ReminderManager {
	private final NotesManager manager;
	private long lastCheckMillis;

	public ReminderManager(NotesManager manager) {
		this.manager = manager;
	}

	public void tick(Minecraft client) {
		long nowMillis = System.currentTimeMillis();
		if (nowMillis - lastCheckMillis < 1000) {
			return;
		}
		lastCheckMillis = nowMillis;
		Instant now = Instant.now();
		boolean changed = false;
		for (Note note : manager.getNotes()) {
			Reminder reminder = note.getReminder();
			if (reminder == null || !reminder.isEnabled() || reminder.getTriggerAt().isAfter(now)) {
				continue;
			}
			notify(client, note);
			reminder.scheduleNext();
			changed = true;
		}
		if (changed) {
			manager.save();
		}
	}

	private void notify(Minecraft client, Note note) {
		if (client.player == null) {
			return;
		}
		client.player.sendSystemMessage(Component.literal("Reminder: " + note.getTitle() + "  (Snooze: open Notes)"));
		if (note.getReminder() != null && note.getReminder().isSound()) {
			client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 1.0F);
		}
	}

	public void snoozeTenMinutes(String noteId) {
		manager.snoozeReminder(noteId, Duration.ofMinutes(10));
	}
}
