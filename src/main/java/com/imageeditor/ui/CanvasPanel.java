package com.imageeditor.ui;

import com.imageeditor.model.ImageDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class CanvasPanel extends JPanel {

    private BufferedImage image;
    private ImageDocument document;

    public CanvasPanel() {
        setBackground(new Color(24, 25, 27));
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public void setDocument(ImageDocument document) {
        this.document = document;
        repaint();
    }

    public BufferedImage getImage() {
        if (document != null) {
            BufferedImage comp = document.getCompositeImage();
            if (comp != null) return comp;
        }
        return image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        BufferedImage displayImage = getImage();

        if (displayImage == null) {
            drawEmptyState(g2d);
            g2d.dispose();
            return;
        }

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int padding = 24;
        int availWidth = Math.max(1, getWidth() - padding * 2);
        int availHeight = Math.max(1, getHeight() - padding * 2);

        double scaleX = (double) availWidth / displayImage.getWidth();
        double scaleY = (double) availHeight / displayImage.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int newWidth = Math.max(1, (int) (displayImage.getWidth() * scale));
        int newHeight = Math.max(1, (int) (displayImage.getHeight() * scale));

        int x = (getWidth() - newWidth) / 2;
        int y = (getHeight() - newHeight) / 2;

        // Subtle drop shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillRoundRect(x + 3, y + 4, newWidth, newHeight, 4, 4);

        // Draw composite image
        g2d.drawImage(displayImage, x, y, newWidth, newHeight, this);

        // Subtle outline border
        g2d.setColor(new Color(255, 255, 255, 35));
        g2d.drawRect(x, y, newWidth, newHeight);

        g2d.dispose();
    }

    private void drawEmptyState(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        int boxW = 340;
        int boxH = 170;
        int boxX = centerX - boxW / 2;
        int boxY = centerY - boxH / 2;

        // Background card
        g2d.setColor(new Color(32, 33, 36));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Dashed border
        Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 4.0f}, 0.0f);
        g2d.setStroke(dashed);
        g2d.setColor(new Color(65, 68, 75));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Icon
        int iconSize = 40;
        int iconX = centerX - iconSize / 2;
        int iconY = boxY + 24;

        g2d.setStroke(new BasicStroke(1.8f));
        g2d.setColor(new Color(115, 120, 132));
        g2d.drawRoundRect(iconX, iconY, iconSize, iconSize, 6, 6);
        g2d.fillOval(iconX + 24, iconY + 8, 7, 7);
        int[] xPoints = {iconX + 6, iconX + 18, iconX + 26, iconX + 34};
        int[] yPoints = {iconY + 32, iconY + 18, iconY + 26, iconY + 32};
        g2d.drawPolyline(xPoints, yPoints, 4);

        // Title
        g2d.setColor(new Color(215, 218, 225));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm1 = g2d.getFontMetrics();
        String title = "Open an image to get started";
        g2d.drawString(title, centerX - fm1.stringWidth(title) / 2, iconY + iconSize + 30);

        // Subtitle
        g2d.setColor(new Color(130, 134, 145));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm2 = g2d.getFontMetrics();
        String subtitle = "Supports PNG and JPG formats";
        g2d.drawString(subtitle, centerX - fm2.stringWidth(subtitle) / 2, iconY + iconSize + 52);
    }
}
