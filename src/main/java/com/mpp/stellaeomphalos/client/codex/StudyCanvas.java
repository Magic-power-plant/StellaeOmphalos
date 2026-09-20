package com.mpp.stellaeomphalos.client.codex;

/**
 * Pure, bounded viewport math. All values are logical coordinates rather than framebuffer pixels.
 */
public final class StudyCanvas {
    public record Point(double x, double y) {}

    private double scale = 1, centerX, centerY;
    private double width = 512, height = 512, viewportWidth = 330, viewportHeight = 190;
    private Point focus;
    private String lastTarget = "";
    private long lastClick = Long.MIN_VALUE;

    public void bounds(double width, double height, double viewportWidth, double viewportHeight) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.viewportWidth = Math.max(1, viewportWidth);
        this.viewportHeight = Math.max(1, viewportHeight);
        clamp();
    }

    public double scale() {
        return scale;
    }

    public Point center() {
        return new Point(centerX, centerY);
    }

    public void focus(Point point) {
        focus = point;
    }

    public void zoom(double delta, double focusThreshold) {
        if (!Double.isFinite(delta)) return;
        scale = Math.max(1, Math.min(focus == null ? focusThreshold : 10, scale + delta));
        clamp();
    }

    public void centerStep(double focusThreshold, double branchThreshold) {
        if (focus != null && scale >= focusThreshold) {
            double factor = scale >= branchThreshold ? 1 : .2;
            centerX += (focus.x() - centerX) * factor;
            centerY += (focus.y() - centerY) * factor;
            clamp();
        }
    }

    public void pan(double dx, double dy) {
        centerX -= dx / scale;
        centerY -= dy / scale;
        clamp();
    }

    public void clamp() {
        double x = Math.max(0, (width - viewportWidth / scale) / 2),
                y = Math.max(0, (height - viewportHeight / scale) / 2);
        centerX = Math.max(-x, Math.min(x, centerX));
        centerY = Math.max(-y, Math.min(y, centerY));
    }

    public Point screen(Point point) {
        return new Point(
                (point.x() - centerX) * scale + viewportWidth / 2,
                (point.y() - centerY) * scale + viewportHeight / 2);
    }

    public boolean doubleClick(String target, long millis, Point point) {
        boolean match =
                target.equals(lastTarget)
                        && millis >= lastClick
                        && lastClick != Long.MIN_VALUE
                        && millis - lastClick <= 400;
        lastTarget = target;
        lastClick = millis;
        if (match) {
            focus = point;
            scale = 9.9;
            centerX = point.x();
            centerY = point.y();
            clamp();
            lastClick = Long.MIN_VALUE;
        }
        return match;
    }

    public static boolean clickable(double localScale, double threshold) {
        return localScale >= threshold;
    }

    public void reset() {
        scale = 1;
        centerX = centerY = 0;
        focus = null;
        lastTarget = "";
        lastClick = Long.MIN_VALUE;
    }
}
