package izzied.dev.gui;

import izzied.dev.model.Note;
import izzied.dev.model.NoteCategory;
import izzied.dev.model.NoteType;
import izzied.dev.model.Priority;
import izzied.dev.search.SearchFilter;
import izzied.dev.service.NotesManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NotesScreen extends Screen {
	private static final int SIDEBAR_WIDTH = 142;
	private static final int LIST_WIDTH = 182;
	private static final int PANEL_PAD = 10;
	private static final int LINE_HEIGHT = 12;
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	private final NotesManager manager;
	private final SearchFilter filter = new SearchFilter();
	private String selectedCategoryId;
	private String selectedNoteId;
	private List<Note> visibleNotes = new ArrayList<>();
	private EditBox searchBox;
	private EditBox categoryNameBox;
	private EditBox titleBox;
	private Button deleteNoteButton;
	private Button duplicateButton;
	private Button pinButton;
	private Button priorityButton;
	private Button deleteCategoryButton;
	private Button reminderFilterButton;
	private Button priorityFilterButton;
	private Button recentFilterButton;
	private EditBox reminderHoursBox;
	private EditBox reminderMinutesBox;
	private EditBox reminderSecondsBox;
	private boolean contentFocused;
	private int contentCursor;
	private int selectionAnchor = -1;
	private boolean draggingContentSelection;
	private int categoryScroll;
	private int noteScroll;
	private int contentScroll;

	public NotesScreen(NotesManager manager) {
		super(Component.literal("Notes"));
		this.manager = manager;
		refreshVisibleNotes();
		if (!visibleNotes.isEmpty()) {
			selectedNoteId = visibleNotes.get(0).getId();
		}
	}

	@Override
	protected void init() {
		clearWidgets();
		int listX = listX();
		int editorX = editorX();
		int bottom = panelBottom();

		searchBox = new EditBox(font, listX + PANEL_PAD, panelY() + 34, LIST_WIDTH - PANEL_PAD * 2, 20, Component.literal("Search notes"));
		searchBox.setHint(Component.literal("Search"));
		searchBox.setMaxLength(120);
		searchBox.setResponder(value -> refreshVisibleNotes());
		addRenderableWidget(searchBox);

		reminderFilterButton = addRenderableWidget(button(listX + PANEL_PAD, panelY() + 58, 48, 18, "Rem", "Show notes with reminders", button -> {
			filter.setRemindersOnly(!filter.isRemindersOnly());
			refreshSelection();
		}));
		priorityFilterButton = addRenderableWidget(button(listX + PANEL_PAD + 54, panelY() + 58, 58, 18, "P: Any", "Cycle priority filter", button -> {
			Priority current = filter.getPriority();
			filter.setPriority(current == null ? Priority.LOW : (current == Priority.CRITICAL ? null : current.next()));
			refreshSelection();
		}));
		recentFilterButton = addRenderableWidget(button(listX + PANEL_PAD + 118, panelY() + 58, 52, 18, "Recent", "Show notes edited in the last 7 days", button -> {
			filter.setRecentlyEditedOnly(!filter.isRecentlyEditedOnly());
			refreshSelection();
		}));

		categoryNameBox = new EditBox(font, panelX() + 12, bottom - 30, SIDEBAR_WIDTH - 24, 18, Component.literal("Category name"));
		categoryNameBox.setHint(Component.literal("Category"));
		categoryNameBox.setMaxLength(48);
		categoryNameBox.setResponder(value -> {
			if (selectedCategoryId != null && !value.isBlank()) {
				manager.renameCategory(selectedCategoryId, value);
			}
		});
		addRenderableWidget(categoryNameBox);

		titleBox = new EditBox(font, editorX + PANEL_PAD, panelY() + 40, panelRight() - editorX - PANEL_PAD * 2, 22, Component.literal("Note title"));
		titleBox.setHint(Component.literal("Title"));
		titleBox.setMaxLength(120);
		titleBox.setResponder(value -> getSelectedNote().ifPresent(note -> manager.updateNote(note, value, note.getContent())));
		addRenderableWidget(titleBox);

		addRenderableWidget(button(panelX() + 12, bottom - 55, 62, 20, "+ Cat", "Create category", button -> {
			NoteCategory category = manager.createCategory("New Category");
			selectedCategoryId = category.getId();
			filter.setCategoryId(selectedCategoryId);
			refreshSelection();
		}));
		deleteCategoryButton = addRenderableWidget(button(panelX() + 80, bottom - 55, 50, 20, "Del", "Delete category", button -> confirmDeleteCategory()));

		addRenderableWidget(button(listX + PANEL_PAD, bottom - 32, 52, 20, "+ Note", "Create standard note", button -> createNote(NoteType.STANDARD)));
		addRenderableWidget(button(listX + PANEL_PAD + 58, bottom - 32, 52, 20, "+ Task", "Create task note", button -> createNote(NoteType.TASK)));
		duplicateButton = addRenderableWidget(button(listX + PANEL_PAD + 116, bottom - 32, 54, 20, "Copy", "Duplicate selected note", button -> duplicateSelected()));

		int actionY = panelY() + 12;
		int actionX = editorX + PANEL_PAD;
		pinButton = addRenderableWidget(button(actionX, actionY, 48, 20, "Pin", "Pin note to HUD", button -> getSelectedNote().ifPresent(note -> {
			manager.togglePinned(note.getId());
			refreshSelection();
		})));
		priorityButton = addRenderableWidget(button(actionX + 54, actionY, 70, 20, "Priority", "Cycle task priority", button -> getSelectedNote().ifPresent(note -> {
			manager.cyclePriority(note.getId());
			refreshSelection();
		})));
		addRenderableWidget(button(actionX + 130, actionY, 52, 20, "Move", "Move note to next category", button -> moveSelectedToNextCategory()));
		deleteNoteButton = addRenderableWidget(button(panelRight() - PANEL_PAD - 48, actionY, 48, 20, "Del", "Delete selected note", button -> confirmDeleteNote()));

		int reminderY = panelY() + 66;
		reminderHoursBox = reminderBox(actionX, reminderY, "H");
		reminderMinutesBox = reminderBox(actionX + 34, reminderY, "M");
		reminderSecondsBox = reminderBox(actionX + 68, reminderY, "S");
		addRenderableWidget(reminderHoursBox);
		addRenderableWidget(reminderMinutesBox);
		addRenderableWidget(reminderSecondsBox);
		addRenderableWidget(button(actionX + 106, reminderY, 42, 18, "Set", "Set one-time reminder using H/M/S fields", button -> scheduleCustomReminder(false)));
		addRenderableWidget(button(actionX + 154, reminderY, 54, 18, "Repeat", "Set repeating reminder using H/M/S fields", button -> scheduleCustomReminder(true)));
		addRenderableWidget(button(actionX + 214, reminderY, 58, 18, "Snooze", "Snooze reminder using H/M/S fields", button -> snoozeCustomReminder()));
		addRenderableWidget(button(actionX + 278, reminderY, 48, 18, "Clear", "Disable reminder", button -> getSelectedNote().ifPresent(note -> manager.disableReminder(note.getId()))));

		refreshSelection();
	}

	private Button button(int x, int y, int w, int h, String label, String tooltip, Button.OnPress action) {
		return Button.builder(Component.literal(label), action)
			.bounds(x, y, w, h)
			.tooltip(Tooltip.create(Component.literal(tooltip)))
			.build();
	}

	private EditBox reminderBox(int x, int y, String hint) {
		EditBox box = new EditBox(font, x, y, 28, 18, Component.literal(hint));
		box.setHint(Component.literal(hint));
		box.setMaxLength(3);
		box.setValue("0");
		return box;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float tickDelta) {
		extractor.fill(0, 0, width, height, 0x99000000);
		extractor.fill(panelX(), panelY(), panelRight(), panelBottom(), 0xFF15171B);
		extractor.outline(panelX(), panelY(), panelWidth(), panelHeight(), 0xFF536179);
		extractor.fill(panelX(), panelY(), panelRight(), panelY() + 30, 0xFF202630);
		extractor.fill(panelX(), panelY() + 30, listX(), panelBottom(), 0xFF1A1E26);
		extractor.fill(listX(), panelY() + 30, editorX(), panelBottom(), 0xFF20242B);
		extractor.fill(editorX(), panelY() + 30, panelRight(), panelBottom(), 0xFF171A20);
		extractor.text(font, Component.literal("Notes"), panelX() + 12, panelY() + 11, 0xFFE8EDF2, false);
		extractor.text(font, Component.literal("Categories"), panelX() + 12, panelY() + 34, 0xFF94A3B8, false);
		extractor.text(font, Component.literal("Editor"), editorX() + PANEL_PAD, panelY() + 34, 0xFF94A3B8, false);
		drawCategories(extractor, mouseX, mouseY);
		drawNoteList(extractor, mouseX, mouseY);
		drawEditor(extractor);
		super.extractRenderState(extractor, mouseX, mouseY, tickDelta);
		drawContentCaret(extractor);
	}

	private void drawCategories(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		int y = categoryListTop() - categoryScroll;
		boolean allSelected = selectedCategoryId == null;
		if (y > categoryListTop() - 22 && y < panelBottom() - 62) {
			int color = allSelected ? 0xFF2F7DD1 : (isHover(mouseX, mouseY, panelX() + 8, y, SIDEBAR_WIDTH - 16, 20) ? 0xFF252B34 : 0);
			if (color != 0) {
				extractor.fill(panelX() + 8, y, listX() - 8, y + 20, color);
			}
			extractor.text(font, "*", panelX() + 14, y + 6, 0xFFCBD5E1, false);
			extractor.text(font, "All Notes", panelX() + 28, y + 6, 0xFFE2E8F0, false);
			String count = String.valueOf(manager.getNotes().size());
			extractor.text(font, count, listX() - 24 - font.width(count), y + 6, 0xFF94A3B8, false);
		}
		y += 22;
		for (NoteCategory category : manager.getCategories()) {
			boolean selected = category.getId().equals(selectedCategoryId);
			if (y > categoryListTop() - 22 && y < panelBottom() - 62) {
				int color = selected ? 0xFF2F7DD1 : (isHover(mouseX, mouseY, panelX() + 8, y, SIDEBAR_WIDTH - 16, 20) ? 0xFF252B34 : 0);
				if (color != 0) {
					extractor.fill(panelX() + 8, y, listX() - 8, y + 20, color);
				}
				String arrow = category.isCollapsed() ? ">" : "v";
				extractor.text(font, arrow, panelX() + 14, y + 6, 0xFFCBD5E1, false);
				extractor.text(font, trim(category.getName(), SIDEBAR_WIDTH - 60), panelX() + 28, y + 6, 0xFFE2E8F0, false);
				String count = String.valueOf(manager.countNotes(category.getId()));
				extractor.text(font, count, listX() - 24 - font.width(count), y + 6, 0xFF94A3B8, false);
			}
			y += 22;
		}
	}

	private void drawNoteList(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
		int y = noteListTop() - noteScroll;
		for (Note note : visibleNotes) {
			if (manager.findCategory(note.getCategoryId()).map(NoteCategory::isCollapsed).orElse(false)) {
				continue;
			}
			boolean selected = note.getId().equals(selectedNoteId);
			if (y > noteListTop() - 44 && y < panelBottom() - 42) {
				int color = selected ? 0xFF375A7F : (isHover(mouseX, mouseY, listX() + 8, y, LIST_WIDTH - 16, 42) ? 0xFF2B313B : 0xFF232831);
				extractor.fill(listX() + 8, y, editorX() - 8, y + 40, color);
				extractor.fill(listX() + 8, y, listX() + 11, y + 40, note.getPriority().getColor());
				extractor.text(font, trim(note.getTitle(), LIST_WIDTH - 42), listX() + 18, y + 7, 0xFFF8FAFC, false);
				String meta = note.getType().name().toLowerCase() + " | " + manager.getCategoryName(note.getCategoryId());
				extractor.text(font, trim(meta, LIST_WIDTH - 42), listX() + 18, y + 22, 0xFF94A3B8, false);
				if (note.isPinned()) {
					extractor.text(font, "*", editorX() - 24, y + 7, 0xFFFFD166, false);
				}
			}
			y += 44;
		}
	}

	private void drawEditor(GuiGraphicsExtractor extractor) {
		int x = editorX() + PANEL_PAD;
		int y = panelY() + 90;
		int right = panelRight() - PANEL_PAD;
		getSelectedNote().ifPresentOrElse(note -> {
			extractor.text(font, Component.literal("Created " + DATE_FORMAT.format(note.getCreatedAt())), x, y, 0xFF64748B, false);
			extractor.text(font, Component.literal("Edited " + DATE_FORMAT.format(note.getEditedAt())), x + 150, y, 0xFF64748B, false);
			String typeLine = note.getType() == NoteType.TASK ? "Task | " + note.getPriority().name() : "Standard note";
			extractor.text(font, Component.literal(typeLine), x, y + 14, note.getPriority().getColor(), false);
			extractor.fill(x, contentTop(), right, contentBottom(), contentFocused ? 0xFF202833 : 0xFF1A1F27);
			extractor.outline(x, contentTop(), right - x, contentBottom() - contentTop(), contentFocused ? 0xFF5EA1F2 : 0xFF334155);
			drawContent(extractor, note, x + 8, contentTop() + 8, right - x - 16, contentBottom() - contentTop() - 16);
			drawAutocompleteHint(extractor, note, x + 8, contentBottom() - 13, right - x - 16);
		}, () -> extractor.centeredText(font, Component.literal("Create or select a note"), editorX() + (panelRight() - editorX()) / 2, panelY() + panelHeight() / 2, 0xFF94A3B8));
	}

	private void drawContent(GuiGraphicsExtractor extractor, Note note, int x, int y, int contentWidth, int contentHeight) {
		int lineY = y - contentScroll;
		String[] lines = note.getContent().split("\n", -1);
		if (lines.length == 1 && lines[0].isEmpty()) {
			extractor.text(font, "Start writing...  Item tracker: @item minecraft:gold_ingot 20", x, y, 0xFF64748B, false);
			return;
		}
		int offset = 0;
		for (String rawLine : lines) {
			String line = stripCarriageReturn(rawLine);
			if (lineY > y - LINE_HEIGHT && lineY < y + contentHeight) {
				drawSelectionForLine(extractor, line, offset, x, lineY);
				extractor.text(font, trim(line, contentWidth), x, lineY, 0xFFE2E8F0, false);
			}
			offset += line.length() + 1;
			lineY += LINE_HEIGHT;
		}
	}

	private void drawSelectionForLine(GuiGraphicsExtractor extractor, String line, int lineStart, int x, int y) {
		if (!hasSelection()) {
			return;
		}
		int start = Math.max(selectionStart(), lineStart);
		int end = Math.min(selectionEnd(), lineStart + line.length());
		if (start > end || (start == end && start != lineStart)) {
			return;
		}
		int localStart = Math.max(0, start - lineStart);
		int localEnd = Math.max(localStart, end - lineStart);
		int startX = x + font.width(line.substring(0, Math.min(localStart, line.length())));
		int endX = x + font.width(line.substring(0, Math.min(localEnd, line.length())));
		extractor.fill(startX, y, Math.max(startX + 1, endX), y + 11, 0x66375A7F);
	}

	private void drawContentCaret(GuiGraphicsExtractor extractor) {
		if (!contentFocused || hasSelection() || getSelectedNote().isEmpty()) {
			return;
		}
		int x = editorX() + PANEL_PAD + 8;
		int y = contentTop() + 8 - contentScroll;
		String content = getSelectedNote().get().getContent();
		String before = content.substring(0, Math.min(contentCursor, content.length()));
		String lastLine = before.substring(before.lastIndexOf('\n') + 1);
		int lineCount = (int) before.chars().filter(ch -> ch == '\n').count();
		int caretX = x + Math.min(font.width(stripCarriageReturn(lastLine)), Math.max(0, panelRight() - editorX() - PANEL_PAD * 2 - 24));
		int caretY = y + lineCount * LINE_HEIGHT;
		if (caretY > contentTop() && caretY < contentBottom() - 8) {
			extractor.fill(caretX, caretY, caretX + 1, caretY + 10, 0xFFFFFFFF);
		}
	}

	private void drawAutocompleteHint(GuiGraphicsExtractor extractor, Note note, int x, int y, int width) {
		String suggestion = findAutocomplete(note.getContent()).orElse("");
		if (!suggestion.isEmpty()) {
			extractor.text(font, trim("Tab completes: " + suggestion, width), x, y, 0xFF94A3B8, false);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		contentFocused = false;
		clearSelection();
		if (handleCategoryClick(event.x(), event.y()) || handleNoteClick(event.x(), event.y()) || handleContentClick(event.x(), event.y())) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingContentSelection && contentFocused) {
			getSelectedNote().ifPresent(note -> contentCursor = cursorFromMouse(note.getContent(), event.x(), event.y()));
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingContentSelection = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (!isInsidePanel(mouseX, mouseY)) {
			return false;
		}
		if (mouseX < listX()) {
			categoryScroll = Math.max(0, categoryScroll - (int) (verticalAmount * 18));
			return true;
		}
		if (mouseX < editorX()) {
			noteScroll = Math.max(0, noteScroll - (int) (verticalAmount * 22));
			return true;
		}
		contentScroll = Math.max(0, contentScroll - (int) (verticalAmount * 20));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!contentFocused && getFocused() instanceof EditBox) {
			return super.keyPressed(event);
		}
		boolean control = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
		if (contentFocused) {
			editContentByKey(event, control);
			return true;
		}
		if (control && event.key() == GLFW.GLFW_KEY_N) {
			createNote(NoteType.STANDARD);
			return true;
		}
		if (control && event.key() == GLFW.GLFW_KEY_D) {
			duplicateSelected();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_DELETE && getSelectedNote().isPresent()) {
			confirmDeleteNote();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (contentFocused && event.isAllowedChatCharacter()) {
			insertContent(event.codepointAsString());
			return true;
		}
		return super.charTyped(event);
	}

	private boolean editContentByKey(KeyEvent event, boolean control) {
		if (control && event.key() == GLFW.GLFW_KEY_A) {
			getSelectedNote().ifPresent(note -> {
				selectionAnchor = 0;
				contentCursor = note.getContent().length();
			});
			return true;
		}
		if (control && event.key() == GLFW.GLFW_KEY_C && minecraft != null) {
			getSelectedNote().ifPresent(note -> minecraft.keyboardHandler.setClipboard(selectedContent(note)));
			return true;
		}
		if (control && event.key() == GLFW.GLFW_KEY_X && minecraft != null) {
			getSelectedNote().ifPresent(note -> {
				minecraft.keyboardHandler.setClipboard(selectedContent(note));
				if (hasSelection()) {
					deleteSelectedContent(note);
				}
			});
			return true;
		}
		if (control && event.key() == GLFW.GLFW_KEY_V && minecraft != null) {
			insertContent(minecraft.keyboardHandler.getClipboard());
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_TAB) {
			autocompleteItemId();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
			deleteContentBeforeCursor();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_DELETE) {
			deleteContentAfterCursor();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ENTER) {
			insertContent("\n");
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_LEFT) {
			clearSelection();
			contentCursor = Math.max(0, contentCursor - 1);
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_RIGHT) {
			clearSelection();
			getSelectedNote().ifPresent(note -> contentCursor = Math.min(note.getContent().length(), contentCursor + 1));
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_HOME) {
			clearSelection();
			contentCursor = 0;
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_END) {
			clearSelection();
			getSelectedNote().ifPresent(note -> contentCursor = note.getContent().length());
			return true;
		}
		return false;
	}

	private String selectedContent(Note note) {
		if (!hasSelection()) {
			return "";
		}
		String content = note.getContent();
		return content.substring(Math.min(selectionStart(), content.length()), Math.min(selectionEnd(), content.length()));
	}

	private void insertContent(String value) {
		getSelectedNote().ifPresent(note -> {
			String content = note.getContent();
			if (hasSelection()) {
				int start = Math.min(selectionStart(), content.length());
				int end = Math.min(selectionEnd(), content.length());
				String updated = content.substring(0, start) + value + content.substring(end);
				contentCursor = start + value.length();
				clearSelection();
				manager.updateNote(note, note.getTitle(), updated);
				return;
			}
			int cursor = Math.min(contentCursor, content.length());
			String updated = content.substring(0, cursor) + value + content.substring(cursor);
			contentCursor = cursor + value.length();
			manager.updateNote(note, note.getTitle(), updated);
		});
	}

	private void deleteContentBeforeCursor() {
		getSelectedNote().ifPresent(note -> {
			if (hasSelection()) {
				deleteSelectedContent(note);
				return;
			}
			String content = note.getContent();
			if (contentCursor <= 0 || content.isEmpty()) {
				return;
			}
			int cursor = Math.min(contentCursor, content.length());
			String updated = content.substring(0, cursor - 1) + content.substring(cursor);
			contentCursor = cursor - 1;
			manager.updateNote(note, note.getTitle(), updated);
		});
	}

	private void deleteContentAfterCursor() {
		getSelectedNote().ifPresent(note -> {
			if (hasSelection()) {
				deleteSelectedContent(note);
				return;
			}
			String content = note.getContent();
			if (contentCursor >= content.length()) {
				return;
			}
			int cursor = Math.max(0, contentCursor);
			String updated = content.substring(0, cursor) + content.substring(cursor + 1);
			manager.updateNote(note, note.getTitle(), updated);
		});
	}

	private void autocompleteItemId() {
		getSelectedNote().ifPresent(note -> findAutocomplete(note.getContent()).ifPresent(match -> {
			String content = note.getContent();
			LineToken token = itemTokenAtCursor(content);
			if (token == null) {
				return;
			}
			String updated = content.substring(0, token.start) + match + content.substring(token.end);
			contentCursor = token.start + match.length();
			clearSelection();
			manager.updateNote(note, note.getTitle(), updated);
		}));
	}

	private void scheduleCustomReminder(boolean repeating) {
		Duration delay = customReminderDuration();
		getSelectedNote().ifPresent(note -> manager.scheduleReminder(note.getId(), delay, repeating));
	}

	private void snoozeCustomReminder() {
		Duration delay = customReminderDuration();
		getSelectedNote().ifPresent(note -> manager.snoozeReminder(note.getId(), delay));
	}

	private Duration customReminderDuration() {
		long hours = parseNonNegative(reminderHoursBox == null ? "" : reminderHoursBox.getValue());
		long minutes = parseNonNegative(reminderMinutesBox == null ? "" : reminderMinutesBox.getValue());
		long seconds = parseNonNegative(reminderSecondsBox == null ? "" : reminderSecondsBox.getValue());
		long totalSeconds = hours * 3600L + minutes * 60L + seconds;
		return Duration.ofSeconds(Math.max(1, totalSeconds));
	}

	private long parseNonNegative(String value) {
		try {
			return Math.max(0, Long.parseLong(value.strip()));
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	private Optional<String> findAutocomplete(String content) {
		LineToken token = itemTokenAtCursor(content);
		if (token == null || token.value.isBlank()) {
			return Optional.empty();
		}
		String prefix = token.value.toLowerCase();
		return BuiltInRegistries.ITEM.keySet().stream()
			.map(Identifier::toString)
			.sorted(Comparator.naturalOrder())
			.filter(id -> id.startsWith(prefix) || pathPart(id).startsWith(prefix))
			.findFirst();
	}

	private LineToken itemTokenAtCursor(String content) {
		int safeCursor = clamp(contentCursor, 0, content.length());
		int lineStart = content.lastIndexOf('\n', Math.max(0, safeCursor - 1)) + 1;
		int lineEnd = content.indexOf('\n', safeCursor);
		if (lineEnd < 0) {
			lineEnd = content.length();
		}
		if (lineEnd < lineStart) {
			lineEnd = lineStart;
		}
		String line = stripCarriageReturn(content.substring(lineStart, lineEnd));
		String stripped = line.stripLeading();
		int leading = line.length() - stripped.length();
		if (!stripped.startsWith("@item ") && !stripped.startsWith("@collect ")) {
			return null;
		}
		int commandLength = stripped.startsWith("@item ") ? 6 : 9;
		int tokenStartLocal = leading + commandLength;
		while (tokenStartLocal < line.length() && Character.isWhitespace(line.charAt(tokenStartLocal))) {
			tokenStartLocal++;
		}
		int tokenEndLocal = tokenStartLocal;
		while (tokenEndLocal < line.length() && !Character.isWhitespace(line.charAt(tokenEndLocal))) {
			tokenEndLocal++;
		}
		int tokenStart = lineStart + tokenStartLocal;
		int tokenEnd = lineStart + tokenEndLocal;
		tokenStart = clamp(tokenStart, 0, content.length());
		tokenEnd = clamp(tokenEnd, tokenStart, content.length());
		if (safeCursor < tokenStart || safeCursor > tokenEnd) {
			return null;
		}
		return new LineToken(tokenStart, tokenEnd, content.substring(tokenStart, tokenEnd));
	}

	private boolean handleCategoryClick(double mouseX, double mouseY) {
		if (mouseX < panelX() || mouseX >= listX() || mouseY < categoryListTop() || mouseY > panelBottom() - 64) {
			return false;
		}
		int y = categoryListTop() - categoryScroll;
		if (mouseY >= y && mouseY <= y + 20) {
			selectedCategoryId = null;
			filter.setCategoryId(null);
			refreshSelection();
			return true;
		}
		y += 22;
		for (NoteCategory category : manager.getCategories()) {
			if (mouseY >= y && mouseY <= y + 20) {
				selectedCategoryId = category.getId();
				filter.setCategoryId(selectedCategoryId);
				if (mouseX < panelX() + 28) {
					manager.toggleCategoryCollapsed(category.getId());
				}
				refreshSelection();
				return true;
			}
			y += 22;
		}
		return false;
	}

	private boolean handleNoteClick(double mouseX, double mouseY) {
		if (mouseX < listX() || mouseX >= editorX() || mouseY < noteListTop() || mouseY > panelBottom() - 40) {
			return false;
		}
		int y = noteListTop() - noteScroll;
		for (Note note : visibleNotes) {
			if (manager.findCategory(note.getCategoryId()).map(NoteCategory::isCollapsed).orElse(false)) {
				continue;
			}
			if (mouseY >= y && mouseY <= y + 40) {
				selectedNoteId = note.getId();
				selectedCategoryId = note.getCategoryId();
				contentCursor = note.getContent().length();
				clearSelection();
				refreshSelection();
				return true;
			}
			y += 44;
		}
		return false;
	}

	private boolean handleContentClick(double mouseX, double mouseY) {
		int x = editorX() + PANEL_PAD;
		if (mouseX >= x && mouseX <= panelRight() - PANEL_PAD && mouseY >= contentTop() && mouseY <= contentBottom()) {
			contentFocused = true;
			clearFocus();
			getSelectedNote().ifPresent(note -> {
				contentCursor = cursorFromMouse(note.getContent(), mouseX, mouseY);
				selectionAnchor = contentCursor;
				draggingContentSelection = true;
			});
			return true;
		}
		return false;
	}

	private void createNote(NoteType type) {
		Note note = manager.createNote(selectedCategoryId, type);
		selectedNoteId = note.getId();
		selectedCategoryId = note.getCategoryId();
		refreshSelection();
	}

	private void duplicateSelected() {
		getSelectedNote().ifPresent(note -> {
			Note copy = manager.duplicateNote(note.getId());
			if (copy != null) {
				selectedNoteId = copy.getId();
				refreshSelection();
			}
		});
	}

	private void confirmDeleteNote() {
		getSelectedNote().ifPresent(note -> minecraft.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				manager.deleteNote(note.getId());
				selectedNoteId = null;
			}
			minecraft.setScreen(this);
			refreshSelection();
		}, Component.literal("Delete note?"), Component.literal(note.getTitle()))));
	}

	private void confirmDeleteCategory() {
		manager.findCategory(selectedCategoryId).ifPresent(category -> minecraft.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				manager.deleteCategory(category.getId());
				selectedCategoryId = manager.getCategories().get(0).getId();
				filter.setCategoryId(selectedCategoryId);
				selectedNoteId = null;
			}
			minecraft.setScreen(this);
			refreshSelection();
		}, Component.literal("Delete category?"), Component.literal(category.getName() + " notes will move to another category."))));
	}

	private void moveSelectedToNextCategory() {
		getSelectedNote().ifPresent(note -> {
			List<NoteCategory> categories = manager.getCategories();
			for (int i = 0; i < categories.size(); i++) {
				if (categories.get(i).getId().equals(note.getCategoryId())) {
					NoteCategory next = categories.get((i + 1) % categories.size());
					manager.moveNote(note.getId(), next.getId());
					selectedCategoryId = next.getId();
					filter.setCategoryId(selectedCategoryId);
					refreshSelection();
					return;
				}
			}
		});
	}

	private void refreshSelection() {
		refreshVisibleNotes();
		getSelectedNote().ifPresentOrElse(note -> {
			titleBox.setValue(note.getTitle());
			contentCursor = Math.min(contentCursor, note.getContent().length());
			pinButton.setMessage(Component.literal(note.isPinned() ? "Unpin" : "Pin"));
			priorityButton.setMessage(Component.literal(note.getPriority().name()));
		}, () -> {
			titleBox.setValue("");
			contentCursor = 0;
			clearSelection();
			pinButton.setMessage(Component.literal("Pin"));
			priorityButton.setMessage(Component.literal("Priority"));
		});
		if (selectedCategoryId == null) {
			categoryNameBox.setValue("");
			categoryNameBox.setHint(Component.literal("All categories"));
		} else {
			manager.findCategory(selectedCategoryId).ifPresent(category -> categoryNameBox.setValue(category.getName()));
			categoryNameBox.setHint(Component.literal("Category"));
		}
		boolean hasNote = getSelectedNote().isPresent();
		deleteNoteButton.active = hasNote;
		duplicateButton.active = hasNote;
		pinButton.active = hasNote;
		priorityButton.active = hasNote;
		categoryNameBox.active = selectedCategoryId != null;
		deleteCategoryButton.active = selectedCategoryId != null && manager.getCategories().size() > 1;
		reminderFilterButton.setMessage(Component.literal(filter.isRemindersOnly() ? "Rem*" : "Rem"));
		priorityFilterButton.setMessage(Component.literal(filter.getPriority() == null ? "P: Any" : "P: " + filter.getPriority().name().charAt(0)));
		recentFilterButton.setMessage(Component.literal(filter.isRecentlyEditedOnly() ? "Recent*" : "Recent"));
	}

	private void refreshVisibleNotes() {
		filter.setCategoryId(selectedCategoryId);
		visibleNotes = manager.search(searchBox == null ? "" : searchBox.getValue(), filter);
	}

	private Optional<Note> getSelectedNote() {
		return selectedNoteId == null ? Optional.empty() : manager.findNote(selectedNoteId);
	}

	private int cursorFromMouse(String content, double mouseX, double mouseY) {
		if (content.isEmpty()) {
			return 0;
		}
		String[] lines = content.split("\n", -1);
		int lineIndex = Math.max(0, Math.min(lines.length - 1, (int) ((mouseY - (contentTop() + 8) + contentScroll) / LINE_HEIGHT)));
		int offset = 0;
		for (int i = 0; i < lineIndex; i++) {
			offset += stripCarriageReturn(lines[i]).length() + 1;
		}
		String line = stripCarriageReturn(lines[lineIndex]);
		int localX = Math.max(0, (int) mouseX - (editorX() + PANEL_PAD + 8));
		int column = 0;
		for (int i = 1; i <= line.length(); i++) {
			int previousWidth = font.width(line.substring(0, i - 1));
			int currentWidth = font.width(line.substring(0, i));
			int midpoint = previousWidth + Math.max(1, (currentWidth - previousWidth) / 2);
			if (localX < midpoint) {
				break;
			}
			column = i;
		}
		return Math.min(content.length(), offset + column);
	}

	private boolean hasSelection() {
		return selectionAnchor >= 0 && selectionAnchor != contentCursor;
	}

	private int selectionStart() {
		return Math.max(0, Math.min(selectionAnchor, contentCursor));
	}

	private int selectionEnd() {
		return Math.max(selectionAnchor, contentCursor);
	}

	private void clearSelection() {
		selectionAnchor = -1;
		draggingContentSelection = false;
	}

	private void deleteSelectedContent(Note note) {
		String content = note.getContent();
		int start = Math.min(selectionStart(), content.length());
		int end = Math.min(selectionEnd(), content.length());
		String updated = content.substring(0, start) + content.substring(end);
		contentCursor = start;
		clearSelection();
		manager.updateNote(note, note.getTitle(), updated);
	}

	private String stripCarriageReturn(String line) {
		return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
	}

	private boolean isHover(int mouseX, int mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
	}

	private boolean isInsidePanel(double mouseX, double mouseY) {
		return mouseX >= panelX() && mouseX <= panelRight() && mouseY >= panelY() && mouseY <= panelBottom();
	}

	private int panelWidth() {
		return Math.min(width - 36, 900);
	}

	private int panelHeight() {
		return Math.min(height - 36, 520);
	}

	private int panelX() {
		return (width - panelWidth()) / 2;
	}

	private int panelY() {
		return (height - panelHeight()) / 2;
	}

	private int panelRight() {
		return panelX() + panelWidth();
	}

	private int panelBottom() {
		return panelY() + panelHeight();
	}

	private int listX() {
		return panelX() + SIDEBAR_WIDTH;
	}

	private int editorX() {
		return listX() + LIST_WIDTH;
	}

	private int categoryListTop() {
		return panelY() + 54;
	}

	private int noteListTop() {
		return panelY() + 82;
	}

	private int contentTop() {
		return panelY() + 124;
	}

	private int contentBottom() {
		return panelBottom() - 22;
	}

	private String trim(String value, int maxWidth) {
		if (font.width(value) <= maxWidth) {
			return value;
		}
		return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("..."))) + "...";
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private String pathPart(String itemId) {
		int separator = itemId.indexOf(':');
		if (separator < 0 || separator + 1 >= itemId.length()) {
			return itemId;
		}
		return itemId.substring(separator + 1);
	}

	private record LineToken(int start, int end, String value) {
	}

}
