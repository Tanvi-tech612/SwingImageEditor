
package com.imageeditor.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class CanvasPanel extends JPanel {

    private BufferedImage image;

    public CanvasPanel() {
        setBackground(Color.DARK_GRAY);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image == null) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 20));

            String message = "Open an image to get started";

            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2;

            g.drawString(message, x, y);
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();

        double scaleX = (double) getWidth() / image.getWidth();
        double scaleY = (double) getHeight() / image.getHeight();

        double scale = Math.min(scaleX, scaleY);

        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);

        int x = (getWidth() - newWidth) / 2;
        int y = (getHeight() - newHeight) / 2;

        g2d.drawImage(image, x, y, newWidth, newHeight, this);

        g2d.dispose();
    }
}
