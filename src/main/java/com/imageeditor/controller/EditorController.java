
package com.imageeditor.controller;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.operations.ImageOperation;
import com.imageeditor.command.CommandHistory;
import com.imageeditor.command.ImageOperationCommand;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class EditorController {

    private final ImageDocument document;
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();

    public EditorController() {
        document = new ImageDocument();
        commandHistory = new CommandHistory();
    }

    public ImageDocument getDocument() {
        return document;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }

        BufferedImage current = document.getImage();
        if (current != null) {
            redoStack.push(copyImage(current));
        }

        document.setImage(undoStack.pop());
    }

    public void redo() {
        if (!canRedo()) {
            return;
        }

        BufferedImage current = document.getImage();
        if (current != null) {
            undoStack.push(copyImage(current));
        }

        document.setImage(redoStack.pop());
    }

    public void loadImage(File file) throws IOException {

        BufferedImage image = ImageIO.read(file);

        if (image == null) {
            throw new IOException("Unsupported image format.");
        }

        if (document.hasImage()) {
            undoStack.push(copyImage(document.getImage()));
        }
        redoStack.clear();
        document.setImage(image);
        commandHistory.clear();
    }

    public void applyOperation(ImageOperation operation) {

        if (!document.hasImage()) {
            throw new IllegalStateException("Please open an image first.");
        }

        BufferedImage currentImage = document.getImage();
        undoStack.push(copyImage(currentImage));
        redoStack.clear();

        BufferedImage processedImage = operation.apply(currentImage);
        document.setImage(processedImage);
    }

    public void saveImage(File file, String format) throws IOException {

        if (!document.hasImage()) {
            throw new IllegalStateException("Please open an image first.");
        }

        BufferedImage currentImage = document.getImage();
        BufferedImage imageToSave = currentImage;

        if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
            BufferedImage rgbImage = new BufferedImage(
                    currentImage.getWidth(),
                    currentImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(currentImage, 0, 0, Color.WHITE, null);
            g.dispose();
            imageToSave = rgbImage;
            format = "jpg";
        }

        boolean written = ImageIO.write(imageToSave, format, file);

        if (!written) {
            throw new IOException("Failed to save image in format: " + format);
        }
    }

    public void saveImage(File file) throws IOException {

        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == name.length() - 1) {
            throw new IllegalArgumentException("File must have a valid extension (.png, .jpg, .jpeg).");
        }

        String ext = name.substring(dotIndex + 1).toLowerCase();

        if (!ext.equals("png") && !ext.equals("jpg") && !ext.equals("jpeg")) {
            throw new IllegalArgumentException("Unsupported image format: " + ext);
        }

        String format = ext.equals("jpeg") ? "jpg" : ext;
        saveImage(file, format);
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                source.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : source.getType()
        );
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }
}
