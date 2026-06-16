package izzied.dev.model;

public enum Priority {
	LOW(0xFF8BC34A),
	MEDIUM(0xFFFFC107),
	HIGH(0xFFFF7043),
	CRITICAL(0xFFE53935);

	private final int color;

	Priority(int color) {
		this.color = color;
	}

	public int getColor() {
		return color;
	}

	public Priority next() {
		Priority[] values = values();
		return values[(ordinal() + 1) % values.length];
	}
}
