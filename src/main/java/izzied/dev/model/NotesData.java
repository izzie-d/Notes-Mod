package izzied.dev.model;

import java.util.ArrayList;
import java.util.List;

public class NotesData {
	private int schemaVersion = 1;
	private List<NoteCategory> categories = new ArrayList<>();
	private List<Note> notes = new ArrayList<>();
	private HudSettings hud = new HudSettings();

	public int getSchemaVersion() {
		return schemaVersion;
	}

	public List<NoteCategory> getCategories() {
		if (categories == null) {
			categories = new ArrayList<>();
		}
		return categories;
	}

	public List<Note> getNotes() {
		if (notes == null) {
			notes = new ArrayList<>();
		}
		return notes;
	}

	public HudSettings getHud() {
		if (hud == null) {
			hud = new HudSettings();
		}
		return hud;
	}
}
