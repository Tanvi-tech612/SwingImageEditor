package com.imageeditor.ui;

import com.imageeditor.controller.EditorController;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.BrightnessOperation;
import com.imageeditor.operations.GrayscaleOperation;
import com.imageeditor.operations.InvertOperation;
import com.imageeditor.operations.RotateOperation;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {

    private final CanvasPanel canvasPanel;
    private final LayersPanel layersPanel;
    private final EditorController controller;

    private final JLabel statusLabel;
    private final JLabel infoLabel;
    private final JButton undoButton;
    private final JButton redoButton;
    private final JButton cropButton;
    private final JButton applyCropButton;
    private final JButton cancelCropButton;
    private String currentFileName;

    public MainFrame() {

        setTitle("Swing Image Editor");
        setSize(1240, 720);
        setMinimumSize(new Dimension(850, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        controller = new EditorController();

        canvasPanel = new CanvasPanel();
        canvasPanel.setController(controller);
        canvasPanel.setDocument(controller.getDocument());
        canvasPanel.setCropSelectionListener(this::onCropSelectionChanged);
        canvasPanel.setLayerMoveStatusListener(this::onLayerMoveStatusChanged);

        layersPanel = new LayersPanel(controller);

        // Listen for document changes
        controller.addChangeListener(this::updateCanvas);

        // Setup Menu Bar
        setupMenuBar();

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
        toolbar.setBackground(new Color(40, 42, 46));
        toolbar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(55, 58, 62)),
                new EmptyBorder(2, 6, 2, 6)
        ));

        JButton openButton = new JButton("Open Image");
        JButton saveButton = new JButton("Save Image");
        JButton grayscaleButton = new JButton("Grayscale");
        JButton invertButton = new JButton("Invert");
        JButton brightnessButton = new JButton("Brightness");
        JButton rotateButton = new JButton("Rotate");
        JButton resizeLayerButton = new JButton("Resize Layer");
        cropButton = new JButton("Crop");
        applyCropButton = new JButton("Apply Crop");
        cancelCropButton = new JButton("Cancel Crop");
        undoButton = new JButton("Undo");
        redoButton = new JButton("Redo");
        applyCropButton.setEnabled(false);
        cancelCropButton.setEnabled(false);
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);

        openButton.setToolTipText("Open an image file from disk (PNG, JPG)");
        saveButton.setToolTipText("Save current edited image to disk");
        grayscaleButton.setToolTipText("Convert image to black and white grayscale");
        invertButton.setToolTipText("Invert image colors");
        brightnessButton.setToolTipText("Adjust image brightness level (-255 to 255)");
        rotateButton.setToolTipText("Rotate image 90 degrees clockwise");
        resizeLayerButton.setToolTipText("Resize the currently selected layer by width and height");
        cropButton.setToolTipText("Select a rectangular region to crop the document");
        applyCropButton.setToolTipText("Apply the current crop selection to all layers");
        cancelCropButton.setToolTipText("Cancel crop selection mode without modifying the image");
        undoButton.setToolTipText("Undo last action (Ctrl+Z)");
        redoButton.setToolTipText("Redo last undone action (Ctrl+Y)");

        styleButton(openButton, true);
        styleButton(saveButton, true);
        styleButton(grayscaleButton, false);
        styleButton(invertButton, false);
        styleButton(brightnessButton, false);
        styleButton(rotateButton, false);
        styleButton(resizeLayerButton, false);
        styleButton(cropButton, false);
        styleButton(applyCropButton, false);
        styleButton(cancelCropButton, false);
        styleButton(undoButton, false);
        styleButton(redoButton, false);

        toolbar.add(openButton);
        toolbar.add(saveButton);
        toolbar.add(createToolbarSeparator());

        toolbar.add(grayscaleButton);
        toolbar.add(invertButton);
        toolbar.add(brightnessButton);
        toolbar.add(rotateButton);
        toolbar.add(resizeLayerButton);
        toolbar.add(createToolbarSeparator());

        toolbar.add(cropButton);
        toolbar.add(applyCropButton);
        toolbar.add(cancelCropButton);
        toolbar.add(createToolbarSeparator());

        toolbar.add(undoButton);
        toolbar.add(redoButton);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(33, 35, 38));
        statusBar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(55, 58, 62)),
                new EmptyBorder(6, 14, 6, 14)
        ));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(165, 168, 175));

        infoLabel = new JLabel("No image loaded");
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(140, 143, 150));

        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(infoLabel, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);
        add(canvasPanel, BorderLayout.CENTER);
        add(layersPanel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        // Action Listeners
        openButton.addActionListener(e -> openImage());
        saveButton.addActionListener(e -> saveImage());

        undoButton.addActionListener(e -> performUndo());
        redoButton.addActionListener(e -> performRedo());

        grayscaleButton.addActionListener(e ->
                applyOperation(new GrayscaleOperation(), "Grayscale")
        );

        invertButton.addActionListener(e ->
                applyOperation(new InvertOperation(), "Invert")
        );

        brightnessButton.addActionListener(e ->
                adjustBrightness()
        );

        rotateButton.addActionListener(e ->
                applyOperation(new RotateOperation(), "Rotate")
        );

        resizeLayerButton.addActionListener(e -> resizeSelectedLayer());

        cropButton.addActionListener(e -> enterCropMode());
        applyCropButton.addActionListener(e -> applyCrop());
        cancelCropButton.addActionListener(e -> cancelCrop());
    }

    private JPanel createToolbarSeparator() {
        JPanel separator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(75, 78, 86));
                g.drawLine(getWidth() / 2, 2, getWidth() / 2, getHeight() - 3);
            }
        };
        separator.setOpaque(false);
        separator.setPreferredSize(new Dimension(10, 26));
        return separator;
    }

    private void styleButton(JButton button, boolean isPrimary) {
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(5, 12, 5, 12));
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Image...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        openItem.addActionListener(e -> openImage());

        JMenuItem saveItem = new JMenuItem("Save Image...");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(e -> saveImage());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoItem.addActionListener(e -> performUndo());

        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        redoItem.addActionListener(e -> performRedo());

        editMenu.add(undoItem);
        editMenu.add(redoItem);

        // Filter / Operations Menu
        JMenu operationsMenu = new JMenu("Operations");
        JMenuItem grayItem = new JMenuItem("Grayscale");
        grayItem.addActionListener(e -> applyOperation(new GrayscaleOperation(), "Grayscale"));

        JMenuItem invertItem = new JMenuItem("Invert");
        invertItem.addActionListener(e -> applyOperation(new InvertOperation(), "Invert"));

        JMenuItem brightItem = new JMenuItem("Brightness...");
        brightItem.addActionListener(e -> adjustBrightness());

        JMenuItem rotateItem = new JMenuItem("Rotate 90° Clockwise");
        rotateItem.addActionListener(e -> applyOperation(new RotateOperation(), "Rotate"));

        JMenuItem resizeLayerItem = new JMenuItem("Resize Layer...");
        resizeLayerItem.addActionListener(e -> resizeSelectedLayer());

        JMenuItem cropItem = new JMenuItem("Crop");
        cropItem.addActionListener(e -> enterCropMode());

        operationsMenu.add(grayItem);
        operationsMenu.add(invertItem);
        operationsMenu.add(brightItem);
        operationsMenu.add(rotateItem);
        operationsMenu.add(resizeLayerItem);
        operationsMenu.addSeparator();
        operationsMenu.add(cropItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(operationsMenu);
        setJMenuBar(menuBar);
    }

    private void openImage() {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Image");
        fileChooser.setAcceptAllFileFilterUsed(true);

        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg");
        fileChooser.addChoosableFileFilter(imageFilter);
        fileChooser.setFileFilter(imageFilter);

        fileChooser.setCurrentDirectory(
                new File(System.getProperty("user.home"), "Documents")
        );

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();

            try {
                exitCropMode(false);
                controller.loadImage(selectedFile);
                currentFileName = selectedFile.getName();

                setTitle("Swing Image Editor - " + currentFileName);
                statusLabel.setText("Opened: " + currentFileName);

                updateCanvas();

            } catch (IOException ex) {

                JOptionPane.showMessageDialog(
                        this,
                        "Could not open image: " + ex.getMessage(),
                        "Image Loading Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void saveImage() {

        if (!controller.getDocument().hasImage()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please open an image first.",
                    "No Image",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image");

        fileChooser.setCurrentDirectory(
                new File(System.getProperty("user.home"), "Documents")
        );

        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG Images (*.png)", "png");
        FileNameExtensionFilter jpgFilter = new FileNameExtensionFilter("JPG Images (*.jpg, *.jpeg)", "jpg", "jpeg");

        fileChooser.addChoosableFileFilter(pngFilter);
        fileChooser.addChoosableFileFilter(jpgFilter);
        fileChooser.setFileFilter(pngFilter);

        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile == null) {
                return;
            }

            String fileName = selectedFile.getName();
            int dotIndex = fileName.lastIndexOf('.');
            String format;

            if (dotIndex == -1) {
                if (fileChooser.getFileFilter() == jpgFilter) {
                    format = "jpg";
                } else {
                    format = "png";
                }
                selectedFile = new File(selectedFile.getAbsolutePath() + "." + format);
            } else if (dotIndex == fileName.length() - 1) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid file name. Please provide a valid file extension (.png, .jpg, .jpeg).",
                        "Invalid Format",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            } else {
                String ext = fileName.substring(dotIndex + 1).toLowerCase();
                if (ext.equals("png")) {
                    format = "png";
                } else if (ext.equals("jpg") || ext.equals("jpeg")) {
                    format = "jpg";
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Invalid image format: ." + ext + "\nSupported formats are PNG (.png) and JPG (.jpg, .jpeg).",
                            "Invalid Format",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            }

            if (selectedFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "File \"" + selectedFile.getName() + "\" already exists. Do you want to overwrite it?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                controller.saveImage(selectedFile, format);
                statusLabel.setText("Saved: " + selectedFile.getName());

                JOptionPane.showMessageDialog(
                        this,
                        "Image saved successfully to:\n" + selectedFile.getAbsolutePath(),
                        "Image Saved",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (IOException | IllegalStateException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Could not save image: " + ex.getMessage(),
                        "Image Saving Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void applyOperation(com.imageeditor.operations.ImageOperation operation, String opName) {

        try {
            if (canvasPanel.isCropMode()) {
                exitCropMode(false);
            }
            controller.applyOperation(operation);
            statusLabel.setText("Applied: " + opName);
            updateCanvas();

        } catch (IllegalStateException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "No Image",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void resizeSelectedLayer() {
        Layer selectedLayer = controller.getDocument().getSelectedLayer();
        if (selectedLayer == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please open an image and select a layer first.",
                    "No Layer Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (selectedLayer.getImage() == null || selectedLayer.getWidth() <= 0 || selectedLayer.getHeight() <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "The selected layer has no valid image to resize.",
                    "Invalid Layer",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (canvasPanel.isCropMode()) {
            exitCropMode(false);
        }

        ResizeLayerDialog dialog = new ResizeLayerDialog(
                this,
                selectedLayer.getName(),
                selectedLayer.getWidth(),
                selectedLayer.getHeight()
        );
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        int newWidth = dialog.getSelectedWidth();
        int newHeight = dialog.getSelectedHeight();

        try {
            controller.resizeSelectedLayer(newWidth, newHeight);
            statusLabel.setText("Resized layer \"" + selectedLayer.getName() + "\" to " + newWidth + " × " + newHeight + " px");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Resize Layer Error",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void enterCropMode() {
        if (!controller.getDocument().hasImage()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please open an image first.",
                    "No Image",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        canvasPanel.setCropMode(true);
        cropButton.setEnabled(false);
        applyCropButton.setEnabled(true);
        cancelCropButton.setEnabled(true);
        statusLabel.setText("Crop mode: click and drag on the image to select an area");
    }

    private void applyCrop() {
        if (!controller.getDocument().hasImage()) {
            exitCropMode(false);
            JOptionPane.showMessageDialog(
                    this,
                    "Please open an image first.",
                    "No Image",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Rectangle cropArea = canvasPanel.getCropAreaInImageCoordinates();
        if (cropArea == null || cropArea.width <= 0 || cropArea.height <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a valid crop area by clicking and dragging over the image.",
                    "No Crop Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            controller.cropDocument(cropArea);
            exitCropMode(false);
            statusLabel.setText("Applied: Crop (" + cropArea.width + " × " + cropArea.height + " px)");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Invalid Crop Area",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void cancelCrop() {
        exitCropMode(true);
    }

    private void exitCropMode(boolean updateStatus) {
        boolean wasCropMode = canvasPanel.isCropMode();
        canvasPanel.setCropMode(false);
        cropButton.setEnabled(true);
        applyCropButton.setEnabled(false);
        cancelCropButton.setEnabled(false);
        if (updateStatus && wasCropMode) {
            statusLabel.setText("Crop cancelled");
        }
    }

    private void onCropSelectionChanged(Rectangle selection) {
        if (!canvasPanel.isCropMode()) {
            return;
        }
        if (selection != null && selection.width > 0 && selection.height > 0) {
            statusLabel.setText("Crop selection: " + selection.width + " × " + selection.height
                    + " px at (" + selection.x + ", " + selection.y + ") — click Apply Crop or Cancel Crop");
        } else {
            statusLabel.setText("Crop mode: click and drag on the image to select an area");
        }
    }

    private void onLayerMoveStatusChanged(String message) {
        if (message != null && !message.isEmpty()) {
            statusLabel.setText(message);
        }
    }

    private void performUndo() {
        if (canvasPanel.isCropMode()) {
            exitCropMode(false);
        }
        if (controller.canUndo()) {
            controller.undo();
            statusLabel.setText("Undid last action");
        }
    }

    private void performRedo() {
        if (canvasPanel.isCropMode()) {
            exitCropMode(false);
        }
        if (controller.canRedo()) {
            controller.redo();
            statusLabel.setText("Redid last action");
        }
    }

    private void adjustBrightness() {

        if (!controller.getDocument().hasImage()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please open an image first.",
                    "No Image",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        String input = JOptionPane.showInputDialog(
                this,
                "Enter brightness value (-255 to 255):",
                "Brightness",
                JOptionPane.PLAIN_MESSAGE
        );

        if (input == null) {
            return;
        }

        try {

            int value = Integer.parseInt(input);

            if (value < -255 || value > 255) {
                JOptionPane.showMessageDialog(
                        this,
                        "Enter a value between -255 and 255."
                );
                return;
            }

            applyOperation(new BrightnessOperation(value), "Brightness (" + value + ")");

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid integer."
            );
        }
    }

    private void updateCanvas() {
        BufferedImage composite = controller.getDocument().getCompositeImage();
        canvasPanel.setImage(composite);
        layersPanel.updateLayers();
        updateUndoRedoButtons();

        if (composite != null) {
            int layerCount = controller.getDocument().getLayerCount();
            infoLabel.setText(composite.getWidth() + " × " + composite.getHeight() + " px (" + layerCount + " layer" + (layerCount == 1 ? "" : "s") + ")");
        } else {
            if (canvasPanel.isCropMode()) {
                exitCropMode(false);
            }
            infoLabel.setText("No image loaded");
        }
    }

    private void updateUndoRedoButtons() {
        undoButton.setEnabled(controller.canUndo());
        redoButton.setEnabled(controller.canRedo());
    }
}
