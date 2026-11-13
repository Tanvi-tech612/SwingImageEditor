package com.imageeditor.ui;

import com.imageeditor.controller.EditorController;
import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class LayersPanel extends JPanel {

    private EditorController controller;
    private final JPanel layersListPanel;
    private final JLabel emptyLabel;
    private final JButton addButton;
    private final JButton deleteButton;
    private final JButton moveUpButton;
    private final JButton moveDownButton;
    private final JSlider opacitySlider;
    private final JLabel opacityLabel;

    private boolean isUpdatingSlider = false;
    private float dragStartOpacity = 1.0f;

    public LayersPanel() {
        this(null);
    }

    public LayersPanel(EditorController controller) {
        this.controller = controller;

        setPreferredSize(new Dimension(260, 0));
        setLayout(new BorderLayout());
        setBackground(new Color(36, 37, 40));
        setBorder(new MatteBorder(0, 1, 0, 0, new Color(55, 58, 62)));

        // Top Header and Controls Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(new Color(43, 45, 48));
        topPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(55, 58, 62)),
                new EmptyBorder(8, 10, 8, 10)
        ));

        // Header Title and Toolbar buttons
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel title = new JLabel("Layers");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(new Color(220, 220, 225));
        headerRow.add(title, BorderLayout.WEST);

        // Buttons: Add (+), Delete (-), Move Up (▲), Move Down (▼)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonsPanel.setOpaque(false);

        addButton = createIconButton("+", "Add Layer");
        deleteButton = createIconButton("−", "Delete Selected Layer");
        moveUpButton = createIconButton("▲", "Move Layer Up");
        moveDownButton = createIconButton("▼", "Move Layer Down");

        buttonsPanel.add(addButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(moveUpButton);
        buttonsPanel.add(moveDownButton);
        headerRow.add(buttonsPanel, BorderLayout.EAST);

        topPanel.add(headerRow);
        topPanel.add(Box.createVerticalStrut(8));

        // Opacity Control Row
        JPanel opacityRow = new JPanel(new BorderLayout(6, 0));
        opacityRow.setOpaque(false);

        opacityLabel = new JLabel("Opacity: 100%");
        opacityLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        opacityLabel.setForeground(new Color(180, 183, 190));
        opacityLabel.setPreferredSize(new Dimension(85, 18));
        opacityRow.add(opacityLabel, BorderLayout.WEST);

        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.setOpaque(false);
        opacitySlider.setFocusable(false);
        opacitySlider.setEnabled(false);
        opacitySlider.setToolTipText("Adjust selected layer opacity");
        opacityRow.add(opacitySlider, BorderLayout.CENTER);

        topPanel.add(opacityRow);
        add(topPanel, BorderLayout.NORTH);

        // Layers List Panel
        layersListPanel = new JPanel();
        layersListPanel.setLayout(new BoxLayout(layersListPanel, BoxLayout.Y_AXIS));
        layersListPanel.setBackground(new Color(30, 31, 34));
        layersListPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

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

        // Action Listeners
        setupActionListeners();
    }

    public void setController(EditorController controller) {
        this.controller = controller;
        updateLayers();
    }

    private JButton createIconButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(new Color(210, 213, 220));
        btn.setBackground(new Color(55, 58, 64));
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(70, 74, 82)),
                new EmptyBorder(2, 7, 2, 7)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tooltip);
        btn.setEnabled(false);
        return btn;
    }

    private void setupActionListeners() {
        addButton.addActionListener(e -> showAddLayerPopup());

        deleteButton.addActionListener(e -> {
            if (controller != null) {
                try {
                    controller.deleteSelectedLayer();
                } catch (IllegalStateException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot Delete Layer", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        moveUpButton.addActionListener(e -> {
            if (controller != null) {
                controller.moveSelectedLayerUp();
            }
        });

        moveDownButton.addActionListener(e -> {
            if (controller != null) {
                controller.moveSelectedLayerDown();
            }
        });

        opacitySlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (controller != null && controller.getDocument() != null) {
                    Layer selected = controller.getDocument().getSelectedLayer();
                    if (selected != null) {
                        dragStartOpacity = selected.getOpacity();
                    }
                }
            }
        });

        opacitySlider.addChangeListener(e -> {
            if (isUpdatingSlider || controller == null || controller.getDocument() == null) {
                return;
            }
            Layer selected = controller.getDocument().getSelectedLayer();
            if (selected == null) {
                return;
            }

            int val = opacitySlider.getValue();
            opacityLabel.setText("Opacity: " + val + "%");
            float currentOpacity = val / 100.0f;

            if (opacitySlider.getValueIsAdjusting()) {
                controller.previewLayerOpacity(selected, currentOpacity);
            } else {
                controller.setLayerOpacity(selected, currentOpacity, dragStartOpacity);
                dragStartOpacity = currentOpacity;
            }
        });
    }

    private void showAddLayerPopup() {
        if (controller == null) return;

        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(new Color(45, 48, 54));

        JMenuItem addBlankItem = new JMenuItem("New Blank Layer");
        styleMenuItem(addBlankItem);
        addBlankItem.addActionListener(e -> controller.addNewBlankLayer());
        popup.add(addBlankItem);

        JMenuItem addImageItem = new JMenuItem("Add Image as Layer...");
        styleMenuItem(addImageItem);
        addImageItem.addActionListener(e -> addImageLayer());
        popup.add(addImageItem);

        popup.show(addButton, 0, addButton.getHeight());
    }

    private void styleMenuItem(JMenuItem item) {
        item.setBackground(new Color(45, 48, 54));
        item.setForeground(new Color(220, 220, 225));
        item.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private void addImageLayer() {
        if (controller == null) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Add Image as Layer");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg");
        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setFileFilter(filter);

        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Documents"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                controller.addLayerFromImage(selectedFile);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Could not open image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void updateLayers() {
        if (controller == null) {
            updateLayer(null, null);
            return;
        }

        ImageDocument doc = controller.getDocument();
        layersListPanel.removeAll();

        if (doc == null || doc.getLayerCount() == 0) {
            layersListPanel.add(emptyLabel);
            addButton.setEnabled(true);
            deleteButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            opacitySlider.setEnabled(false);
            opacityLabel.setText("Opacity: 100%");
        } else {
            addButton.setEnabled(true);
            Layer selected = doc.getSelectedLayer();
            deleteButton.setEnabled(doc.getLayerCount() > 1 && selected != null);
            moveUpButton.setEnabled(doc.canMoveUp(selected));
            moveDownButton.setEnabled(doc.canMoveDown(selected));

            if (selected != null) {
                opacitySlider.setEnabled(true);
                isUpdatingSlider = true;
                int percent = Math.round(selected.getOpacity() * 100);
                opacitySlider.setValue(percent);
                opacityLabel.setText("Opacity: " + percent + "%");
                dragStartOpacity = selected.getOpacity();
                isUpdatingSlider = false;
            } else {
                opacitySlider.setEnabled(false);
                opacityLabel.setText("Opacity: 100%");
            }

            // Display in stacking order: Topmost layer at the top of the UI list
            for (int i = doc.getLayerCount() - 1; i >= 0; i--) {
                Layer layer = doc.getLayer(i);
                boolean isSelected = (layer == selected);
                JPanel card = createLayerCard(layer, isSelected);
                layersListPanel.add(card);
                layersListPanel.add(Box.createVerticalStrut(6));
            }
        }

        layersListPanel.revalidate();
        layersListPanel.repaint();
    }

    private JPanel createLayerCard(Layer layer, boolean isSelected) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (isSelected) {
            card.setBackground(new Color(50, 60, 78));
            card.setBorder(new CompoundBorder(
                    new MatteBorder(1, 1, 1, 1, new Color(82, 130, 255)),
                    new EmptyBorder(6, 8, 6, 8)
            ));
        } else {
            card.setBackground(new Color(42, 44, 49));
            card.setBorder(new CompoundBorder(
                    new MatteBorder(1, 1, 1, 1, new Color(58, 61, 68)),
                    new EmptyBorder(6, 8, 6, 8)
            ));
        }

        // Thumbnail
        int thumbSize = 38;
        BufferedImage thumb = new BufferedImage(thumbSize, thumbSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = thumb.createGraphics();
        g.setColor(new Color(28, 29, 32));
        g.fillRect(0, 0, thumbSize, thumbSize);

        BufferedImage img = layer.getImage();
        if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scale = Math.min((double) thumbSize / img.getWidth(), (double) thumbSize / img.getHeight());
            int w = Math.max(1, (int) (img.getWidth() * scale));
            int h = Math.max(1, (int) (img.getHeight() * scale));
            g.drawImage(img, (thumbSize - w) / 2, (thumbSize - h) / 2, w, h, null);
        }
        g.dispose();

        JLabel thumbLabel = new JLabel(new ImageIcon(thumb));
        thumbLabel.setBorder(new MatteBorder(1, 1, 1, 1, isSelected ? new Color(82, 130, 255) : new Color(68, 72, 80)));
        card.add(thumbLabel, BorderLayout.WEST);

        // Center info: Name and dimensions/opacity
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(layer.getName());
        nameLabel.setFont(new Font("SansSerif", isSelected ? Font.BOLD : Font.PLAIN, 12));
        nameLabel.setForeground(isSelected ? new Color(255, 255, 255) : new Color(220, 222, 228));

        int opPercent = Math.round(layer.getOpacity() * 100);
        String subtext = layer.getWidth() + " × " + layer.getHeight() + " px (" + opPercent + "%)";
        JLabel sizeLabel = new JLabel(subtext);
        sizeLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sizeLabel.setForeground(new Color(150, 153, 162));

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(sizeLabel);
        card.add(infoPanel, BorderLayout.CENTER);

        // Right: Visibility Toggle Button
        JButton visButton = new JButton(layer.isVisible() ? "●" : "○");
        visButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        visButton.setForeground(layer.isVisible() ? new Color(75, 200, 110) : new Color(130, 133, 140));
        visButton.setToolTipText(layer.isVisible() ? "Layer is visible (click to hide)" : "Layer is hidden (click to show)");
        visButton.setContentAreaFilled(false);
        visButton.setBorderPainted(false);
        visButton.setFocusPainted(false);
        visButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        visButton.addActionListener(e -> {
            if (controller != null) {
                controller.toggleLayerVisibility(layer);
            }
        });
        card.add(visButton, BorderLayout.EAST);

        // Click on card selects layer
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (controller != null) {
                    controller.selectLayer(layer);
                }
            }
        });

        return card;
    }

    public void updateLayer(BufferedImage image, String layerName) {
        if (controller != null) {
            updateLayers();
            return;
        }

        layersListPanel.removeAll();
        if (image == null) {
            layersListPanel.add(emptyLabel);
        } else {
            Layer temp = new Layer(layerName != null ? layerName : "Background", image);
            layersListPanel.add(createLayerCard(temp, true));
        }
        layersListPanel.revalidate();
        layersListPanel.repaint();
    }
}

