
package com.imageeditor.ui;

import com.imageeditor.controller.EditorController;
import com.imageeditor.operations.GrayscaleOperation;
import com.imageeditor.operations.InvertOperation;
import com.imageeditor.operations.BrightnessOperation;
import com.imageeditor.operations.RotateOperation;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {

    private final CanvasPanel canvasPanel;
    private final EditorController controller;

    public MainFrame() {

        setTitle("Swing Image Editor");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        controller = new EditorController();

        canvasPanel = new CanvasPanel();
        LayersPanel layersPanel = new LayersPanel();

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton openButton = new JButton("Open Image");
        JButton saveButton = new JButton("Save Image");
        JButton grayscaleButton = new JButton("Grayscale");
        JButton invertButton = new JButton("Invert");
        JButton brightnessButton = new JButton("Brightness");
        JButton rotateButton = new JButton("Rotate");

        toolbar.add(openButton);
        toolbar.add(saveButton);
        toolbar.add(grayscaleButton);
        toolbar.add(invertButton);
        toolbar.add(brightnessButton);
        toolbar.add(rotateButton);

        add(toolbar, BorderLayout.NORTH);
        add(canvasPanel, BorderLayout.CENTER);
        add(layersPanel, BorderLayout.EAST);

        openButton.addActionListener(e -> openImage());

        saveButton.addActionListener(e -> saveImage());

        grayscaleButton.addActionListener(e ->
                applyOperation(new GrayscaleOperation())
        );

        invertButton.addActionListener(e ->
                applyOperation(new InvertOperation())
        );

        brightnessButton.addActionListener(e ->
                adjustBrightness()
        );

        rotateButton.addActionListener(e ->
                applyOperation(new RotateOperation())
        );
    }

    private void openImage() {

        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setAcceptAllFileFilterUsed(true);

        fileChooser.setCurrentDirectory(
                new File(System.getProperty("user.home"), "Documents")
        );

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();

            try {
                controller.loadImage(selectedFile);

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

    private void applyOperation(com.imageeditor.operations.ImageOperation operation) {

        try {

            controller.applyOperation(operation);

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

            applyOperation(new BrightnessOperation(value));

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid integer."
            );
        }
    }

    private void updateCanvas() {

        canvasPanel.setImage(
                controller.getDocument().getImage()
        );
    }
}
