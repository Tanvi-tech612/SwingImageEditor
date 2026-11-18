package com.imageeditor.model;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.UUID;

public class Layer {

    private final String id;
    private String name;
    private BufferedImage image;
    private boolean visible;
    private float opacity;
    private int x;
    private int y;

    public Layer(String name, BufferedImage image) {
        this(UUID.randomUUID().toString(), name, image, true, 1.0f, 0, 0);
    }

    public Layer(String id, String name, BufferedImage image, boolean visible, float opacity) {
        this(id, name, image, visible, opacity, 0, 0);
    }

    public Layer(String id, String name, BufferedImage image, boolean visible, float opacity, int x, int y) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "Untitled Layer";
        this.image = image;
        this.visible = visible;
        this.opacity = clampOpacity(opacity);
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = clampOpacity(opacity);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point getPosition() {
        return new Point(x, y);
    }

    public int getWidth() {
        return image != null ? image.getWidth() : 0;
    }

    public int getHeight() {
        return image != null ? image.getHeight() : 0;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, getWidth(), getHeight());
    }

    public boolean containsPoint(double docX, double docY) {
        if (image == null || getWidth() <= 0 || getHeight() <= 0) {
            return false;
        }
        return docX >= x && docX < x + getWidth()
                && docY >= y && docY < y + getHeight();
    }

    private static float clampOpacity(float opacity) {
        return Math.max(0.0f, Math.min(1.0f, opacity));
    }
}
