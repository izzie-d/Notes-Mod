package izzied.dev.model;

public class ProgressEntry {
	private String label = "";
	private int current;
	private int target = 1;

	public ProgressEntry() {
	}

	public ProgressEntry(String label, int current, int target) {
		this.label = label == null ? "" : label;
		this.current = Math.max(0, current);
		this.target = Math.max(1, target);
	}

	public String getLabel() {
		return label == null ? "" : label;
	}

	public int getCurrent() {
		return current;
	}

	public int getTarget() {
		return Math.max(1, target);
	}

	public float getRatio() {
		return Math.min(1.0F, current / (float) getTarget());
	}

	public ProgressEntry copy() {
		return new ProgressEntry(getLabel(), current, getTarget());
	}
}
