package com.imageeditor.ui;

import com.imageeditor.controller.EditorController;
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
    private String currentFileName;

    public MainFrame() {

        setTitle("Swing Image Editor");
        setSize(1050, 720);
        setMinimumSize(new Dimension(850, 550));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        controller = new EditorController();

        canvasPanel = new CanvasPanel();
        layersPanel = new LayersPanel();

        // Setup Menu Bar
        setupMenuBar();

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
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

        openButton.setToolTipText("Open an image file from disk (PNG, JPG)");
        saveButton.setToolTipText("Save current edited image to disk");
        grayscaleButton.setToolTipText("Convert image to black and white grayscale");
        invertButton.setToolTipText("Invert image colors");
        brightnessButton.setToolTipText("Adjust image brightness level (-255 to 255)");
        rotateButton.setToolTipText("Rotate image 90 degrees clockwise");

        styleButton(openButton, true);
        styleButton(saveButton, true);
        styleButton(grayscaleButton, false);
        styleButton(invertButton, false);
        styleButton(brightnessButton, false);
        styleButton(rotateButton, false);

        toolbar.add(openButton);
        toolbar.add(saveButton);

        undoButton = new JButton("Undo");
        redoButton = new JButton("Redo");
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        styleButton(undoButton, false);
        styleButton(redoButton, false);

        JPanel separator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(75, 78, 86));
                g.drawLine(getWidth() / 2, 2, getWidth() / 2, getHeight() - 3);
            }
        };
        separator.setOpaque(false);
        separator.setPreferredSize(new Dimension(12, 26));
        toolbar.add(separator);

        toolbar.add(grayscaleButton);
        toolbar.add(invertButton);
        toolbar.add(brightnessButton);
        toolbar.add(rotateButton);
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

        undoButton.addActionListener(e -> {
            controller.undo();
            updateCanvas();
            updateUndoRedoButtons();
        });

        redoButton.addActionListener(e -> {
            controller.redo();
            updateCanvas();
            updateUndoRedoButtons();
        });

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

        operationsMenu.add(grayItem);
        operationsMenu.add(invertItem);
        operationsMenu.add(brightItem);
        operationsMenu.add(rotateItem);

        menuBar.add(fileMenu);
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

        BufferedImage img = controller.getDocument().getImage();
        canvasPanel.setImage(img);
        layersPanel.updateLayer(img, currentFileName);
        updateUndoRedoButtons();

        if (img != null) {
            infoLabel.setText(img.getWidth() + " × " + img.getHeight() + " px");
        } else {
            infoLabel.setText("No image loaded");
        }
    }

    private void updateUndoRedoButtons() {
        undoButton.setEnabled(controller.canUndo());
        redoButton.setEnabled(controller.canRedo());
    }
}
