package izzied.dev.hud;

import izzied.dev.model.HudSettings;
import izzied.dev.model.Note;
import izzied.dev.service.NotesManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class NotesHudOverlay {
	private NotesHudOverlay() {
	}

	public static void extractRenderState(GuiGraphicsExtractor extractor, NotesManager manager) {
		HudSettings settings = manager.getHudSettings();
		List<Note> pinnedNotes = manager.getPinnedNotes();
		if (!settings.isVisible() || pinnedNotes.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		int alpha = (int) (Math.max(0.15F, Math.min(1.0F, settings.getOpacity())) * 255.0F);
		int background = (alpha << 24) | 0x111827;
		int x = settings.getX();
		int y = settings.getY();
		int width = 210;
		int height = 10;
		for (int i = 0; i < Math.min(4, pinnedNotes.size()); i++) {
			Note note = pinnedNotes.get(i);
			height += 14;
			height += Math.max(1, Math.min(3, contentLineCount(note.getContent()))) * 12;
			height += 4;
		}

		extractor.pose().pushMatrix();
		extractor.pose().scale(settings.getScale(), settings.getScale());
		extractor.fill(x, y, x + width, y + height, background);
		extractor.outline(x, y, width, height, 0xAA475569);

		int lineY = y + 8;
		for (int i = 0; i < Math.min(4, pinnedNotes.size()); i++) {
			Note note = pinnedNotes.get(i);
			extractor.text(font, trim(font, note.getTitle(), width - 18), x + 8, lineY, note.getPriority().getColor(), false);
			lineY += 12;
			int rendered = 0;
			for (String line : note.getContent().split("\\R")) {
				if (rendered >= 3) {
					break;
				}
				HudLine hudLine = hudLine(line);
				if (hudLine.text().isBlank()) {
					continue;
				}
				extractor.text(font, trim(font, "- " + hudLine.text(), width - 22), x + 12, lineY, hudLine.color(), false);
				lineY += 12;
				rendered++;
			}
			if (rendered == 0) {
				extractor.text(font, "- No details", x + 12, lineY, 0xFF94A3B8, false);
				lineY += 12;
			}
			lineY += 4;
		}
		extractor.pose().popMatrix();
	}

	private static int contentLineCount(String content) {
		int count = 0;
		for (String line : content.split("\\R")) {
			if (!hudLine(line).text().isBlank()) {
				count++;
			}
		}
		return count;
	}

	private static HudLine hudLine(String line) {
		String trimmed = line.strip();
		if (trimmed.startsWith("[ ]")) {
			return new HudLine(trimmed.substring(3).strip(), 0xFFE2E8F0);
		}
		if (trimmed.startsWith("[x]") || trimmed.startsWith("[X]")) {
			return new HudLine(trimmed.substring(3).strip(), 0xFF94A3B8);
		}
		CollectionParts collection = CollectionParts.parse(trimmed);
		if (collection != null) {
			int current = countInventoryItem(collection.itemId());
			boolean complete = current >= collection.target();
			return new HudLine(collection.label() + " " + current + "/" + collection.target(), complete ? 0xFFFFAA00 : 0xFFE2E8F0);
		}
		return new HudLine(trimmed, 0xFFE2E8F0);
	}

	private static int countInventoryItem(Identifier itemId) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return 0;
		}
		Optional<Item> target = BuiltInRegistries.ITEM.getOptional(itemId);
		if (target.isEmpty()) {
			return 0;
		}
		Inventory inventory = client.player.getInventory();
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && stack.getItem() == target.get()) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static String trim(Font font, String value, int maxWidth) {
		if (font.width(value) <= maxWidth) {
			return value;
		}
		return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("..."))) + "...";
	}

	private record HudLine(String text, int color) {
	}

	private record CollectionParts(Identifier itemId, int target) {
		String label() {
			String path = itemId.getPath().replace('_', ' ');
			if (path.isBlank()) {
				return itemId.toString();
			}
			return path.substring(0, 1).toUpperCase() + path.substring(1);
		}

		static CollectionParts parse(String line) {
			String[] parts = line.split("\\s+");
			if (parts.length < 3 || (!parts[0].equalsIgnoreCase("@item") && !parts[0].equalsIgnoreCase("@collect"))) {
				return null;
			}
			Identifier id = Identifier.tryParse(parts[1].contains(":") ? parts[1] : "minecraft:" + parts[1]);
			if (id == null) {
				return null;
			}
			try {
				return new CollectionParts(id, Math.max(1, Integer.parseInt(parts[2])));
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
	}
}
