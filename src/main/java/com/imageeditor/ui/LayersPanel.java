package com.imageeditor.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LayersPanel extends JPanel {

    private final JPanel layersListPanel;
    private final JLabel emptyLabel;

    public LayersPanel() {
        setPreferredSize(new Dimension(220, 0));
        setLayout(new BorderLayout());
        setBackground(new Color(36, 37, 40));
        setBorder(new MatteBorder(0, 1, 0, 0, new Color(55, 58, 62)));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(43, 45, 48));
        headerPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(55, 58, 62)),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel title = new JLabel("Layers");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(new Color(220, 220, 225));
        headerPanel.add(title, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        layersListPanel = new JPanel();
        layersListPanel.setLayout(new BoxLayout(layersListPanel, BoxLayout.Y_AXIS));
        layersListPanel.setBackground(new Color(30, 31, 34));
        layersListPanel.setBorder(new EmptyBorder(12, 10, 12, 10));

        emptyLabel = new JLabel("No active layers");
        emptyLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        emptyLabel.setForeground(new Color(130, 133, 140));
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        layersListPanel.add(emptyLabel);

        JScrollPane scrollPane = new JScrollPane(layersListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(30, 31, 34));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateLayer(BufferedImage image, String layerName) {
        layersListPanel.removeAll();

        if (image == null) {
            layersListPanel.add(emptyLabel);
        } else {
            JPanel card = new JPanel(new BorderLayout(10, 0));
            card.setBackground(new Color(45, 48, 54));
            card.setBorder(new CompoundBorder(
                    new MatteBorder(1, 1, 1, 1, new Color(60, 64, 72)),
                    new EmptyBorder(8, 8, 8, 8)
            ));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

            int thumbSize = 38;
            BufferedImage thumb = new BufferedImage(thumbSize, thumbSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            g.setColor(new Color(30, 30, 30));
            g.fillRect(0, 0, thumbSize, thumbSize);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double scale = Math.min((double) thumbSize / image.getWidth(), (double) thumbSize / image.getHeight());
            int w = Math.max(1, (int) (image.getWidth() * scale));
            int h = Math.max(1, (int) (image.getHeight() * scale));
            g.drawImage(image, (thumbSize - w) / 2, (thumbSize - h) / 2, w, h, null);
            g.dispose();

            JLabel thumbLabel = new JLabel(new ImageIcon(thumb));
            thumbLabel.setBorder(new MatteBorder(1, 1, 1, 1, new Color(75, 80, 90)));
            card.add(thumbLabel, BorderLayout.WEST);

            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            String displayName = layerName != null && !layerName.isEmpty() ? layerName : "Background";
            JLabel nameLabel = new JLabel(displayName);
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            nameLabel.setForeground(new Color(230, 230, 235));

            JLabel sizeLabel = new JLabel(image.getWidth() + " × " + image.getHeight() + " px");
            sizeLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
            sizeLabel.setForeground(new Color(150, 153, 160));

            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(2));
            infoPanel.add(sizeLabel);
            card.add(infoPanel, BorderLayout.CENTER);

            JLabel visLabel = new JLabel("●");
            visLabel.setForeground(new Color(75, 200, 110));
            visLabel.setToolTipText("Layer is visible");
            card.add(visLabel, BorderLayout.EAST);

            layersListPanel.add(card);
        }

        layersListPanel.revalidate();
        layersListPanel.repaint();
    }
}
