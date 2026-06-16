package izzied.dev.model;

public class HudSettings {
	private boolean visible = true;
	private int x = 12;
	private int y = 12;
	private float scale = 1.0F;
	private float opacity = 0.82F;

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = Math.max(0, x);
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = Math.max(0, y);
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = Math.max(0.5F, Math.min(2.0F, scale));
	}

	public float getOpacity() {
		return opacity;
	}

	public void setOpacity(float opacity) {
		this.opacity = Math.max(0.15F, Math.min(1.0F, opacity));
	}
}
