package izzied.dev.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import izzied.dev.NoteMod;
import izzied.dev.model.NoteCategory;
import izzied.dev.model.NotesData;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

public class NotesStorage {
	private final Gson gson = new GsonBuilder()
		.registerTypeAdapter(Instant.class, new InstantTypeAdapter())
		.setPrettyPrinting()
		.create();
	private final Path storagePath;

	public NotesStorage() {
		storagePath = FabricLoader.getInstance().getConfigDir().resolve(NoteMod.MOD_ID).resolve("notes.json");
	}

	public NotesData load() {
		if (!Files.exists(storagePath)) {
			NotesData data = new NotesData();
			data.getCategories().add(new NoteCategory("Projects"));
			data.getCategories().add(new NoteCategory("SMP"));
			data.getCategories().add(new NoteCategory("Resources"));
			save(data);
			return data;
		}
		try (Reader reader = Files.newBufferedReader(storagePath, StandardCharsets.UTF_8)) {
			NotesData data = gson.fromJson(reader, NotesData.class);
			return data == null ? new NotesData() : data;
		} catch (Exception exception) {
			NoteMod.LOGGER.error("Failed to load notes from {}", storagePath, exception);
			return new NotesData();
		}
	}

	public void save(NotesData data) {
		try {
			Files.createDirectories(storagePath.getParent());
			Path tempPath = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
			Files.move(tempPath, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException atomicMoveFailed) {
			saveWithoutAtomicMove(data);
		}
	}

	private void saveWithoutAtomicMove(NotesData data) {
		try {
			Files.createDirectories(storagePath.getParent());
			try (Writer writer = Files.newBufferedWriter(storagePath, StandardCharsets.UTF_8)) {
				gson.toJson(data, writer);
			}
		} catch (IOException exception) {
			NoteMod.LOGGER.error("Failed to save notes to {}", storagePath, exception);
		}
	}

	public Path getStoragePath() {
		return storagePath;
	}
}
