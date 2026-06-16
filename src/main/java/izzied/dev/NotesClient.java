package izzied.dev;

import izzied.dev.gui.NotesScreen;
import izzied.dev.hud.NotesHudOverlay;
import izzied.dev.service.NotesManager;
import izzied.dev.service.ReminderManager;
import com.mojang.blaze3d.platform.InputConstants;
import izzied.dev.storage.NotesStorage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class NotesClient implements ClientModInitializer {
	private static NotesManager manager;
	private static ReminderManager reminderManager;
	private static KeyMapping openNotesKey;
	private static KeyMapping toggleHudKey;
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(NoteMod.MOD_ID, "notes"));

	@Override
	public void onInitializeClient() {
		NotesStorage storage = new NotesStorage();
		manager = new NotesManager(storage);
		reminderManager = new ReminderManager(manager);

		openNotesKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.notemod.open_notes",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_N,
			KEY_CATEGORY
		));
		toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.notemod.toggle_hud",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_H,
			KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openNotesKey.consumeClick()) {
				client.setScreen(new NotesScreen(manager));
			}
			while (toggleHudKey.consumeClick()) {
				manager.toggleHudVisible();
			}
			reminderManager.tick(client);
		});
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(NoteMod.MOD_ID, "pinned_notes"),
			(extractor, deltaTracker) -> NotesHudOverlay.extractRenderState(extractor, manager)
		);
		NoteMod.LOGGER.info("Notes client systems loaded from {}", storage.getStoragePath());
	}

	public static NotesManager getManager() {
		return manager;
	}

	public static void openScreen() {
		Minecraft client = Minecraft.getInstance();
		if (client != null) {
			client.setScreen(new NotesScreen(manager));
		}
	}
}
