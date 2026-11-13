package com.imageeditor.controller;

import com.imageeditor.command.AddLayerCommand;
import com.imageeditor.command.ChangeOpacityCommand;
import com.imageeditor.command.Command;
import com.imageeditor.command.CommandHistory;
import com.imageeditor.command.DeleteLayerCommand;
import com.imageeditor.command.ImageOperationCommand;
import com.imageeditor.command.ReorderLayerCommand;
import com.imageeditor.command.ToggleVisibilityCommand;
import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.ImageOperation;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EditorController {

    private final ImageDocument document;
    private final CommandHistory commandHistory;
    private final List<Runnable> changeListeners = new ArrayList<>();

    public EditorController() {
        this.document = new ImageDocument();
        this.commandHistory = new CommandHistory();
    }

    public ImageDocument getDocument() {
        return document;
    }

    public CommandHistory getCommandHistory() {
        return commandHistory;
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    public void notifyChange() {
        for (Runnable listener : new ArrayList<>(changeListeners)) {
            listener.run();
        }
    }

    public void executeCommand(Command command) {
        if (command == null) {
            return;
        }
        commandHistory.executeCommand(command);
        notifyChange();
    }

    public boolean canUndo() {
        return commandHistory.canUndo();
    }

    public boolean canRedo() {
        return commandHistory.canRedo();
    }

    public void undo() {
        if (!canUndo()) {
            return;
        }
        commandHistory.undo();
        notifyChange();
    }

    public void redo() {
        if (!canRedo()) {
            return;
        }
        commandHistory.redo();
        notifyChange();
    }

    public void loadImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);

        if (image == null) {
            throw new IOException("Unsupported image format.");
        }

        document.clear();
        String name = file.getName();
        Layer bgLayer = new Layer(name, image);
        document.addLayer(bgLayer);
        commandHistory.clear();
        notifyChange();
    }

    public Layer addNewBlankLayer() {
        int width = document.hasImage() ? document.getCanvasWidth() : 800;
        int height = document.hasImage() ? document.getCanvasHeight() : 600;
        BufferedImage blank = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
        Layer layer = new Layer(document.generateNextLayerName(), blank);
        executeCommand(new AddLayerCommand(document, layer));
        return layer;
    }

    public Layer addLayerFromImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Unsupported image format.");
        }
        Layer newLayer = new Layer(file.getName(), image);
        executeCommand(new AddLayerCommand(document, newLayer));
        return newLayer;
    }

    public void addLayer(Layer layer) {
        if (layer != null) {
            executeCommand(new AddLayerCommand(document, layer));
        }
    }

    public void deleteSelectedLayer() {
        if (document.getLayerCount() <= 1) {
            throw new IllegalStateException("Cannot delete the final remaining layer.");
        }
        Layer selected = document.getSelectedLayer();
        if (selected != null) {
            int index = document.indexOf(selected);
            executeCommand(new DeleteLayerCommand(document, selected, index));
        }
    }

    public void selectLayer(Layer layer) {
        document.setSelectedLayer(layer);
        notifyChange();
    }

    public void selectLayer(int index) {
        document.setSelectedIndex(index);
        notifyChange();
    }

    public void moveSelectedLayerUp() {
        Layer selected = document.getSelectedLayer();
        if (document.canMoveUp(selected)) {
            int oldIndex = document.indexOf(selected);
            int newIndex = oldIndex + 1;
            executeCommand(new ReorderLayerCommand(document, oldIndex, newIndex));
        }
    }

    public void moveSelectedLayerDown() {
        Layer selected = document.getSelectedLayer();
        if (document.canMoveDown(selected)) {
            int oldIndex = document.indexOf(selected);
            int newIndex = oldIndex - 1;
            executeCommand(new ReorderLayerCommand(document, oldIndex, newIndex));
        }
    }

    public void toggleLayerVisibility(Layer layer) {
        if (layer != null) {
            executeCommand(new ToggleVisibilityCommand(layer));
        }
    }

    public void setLayerOpacity(Layer layer, float newOpacity, float previousOpacity) {
        if (layer != null && Math.abs(newOpacity - previousOpacity) > 0.001f) {
            layer.setOpacity(previousOpacity);
            executeCommand(new ChangeOpacityCommand(layer, previousOpacity, newOpacity));
        }
    }

    public void previewLayerOpacity(Layer layer, float opacity) {
        if (layer != null) {
            layer.setOpacity(opacity);
            notifyChange();
        }
    }

    public void applyOperation(ImageOperation operation) {
        if (!document.hasImage()) {
            throw new IllegalStateException("Please open an image first.");
        }

        Layer targetLayer = document.getSelectedLayer();
        if (targetLayer == null && document.getLayerCount() > 0) {
            targetLayer = document.getLayer(0);
        }
        if (targetLayer == null) {
            throw new IllegalStateException("No active layer found to apply operation.");
        }

        ImageOperationCommand command = new ImageOperationCommand(document, targetLayer, operation);
        executeCommand(command);
    }

    public void saveImage(File file, String format) throws IOException {
        if (!document.hasImage()) {
            throw new IllegalStateException("Please open an image first.");
        }

        BufferedImage composite = document.getCompositeImage();
        if (composite == null) {
            composite = document.getImage();
        }
        if (composite == null) {
            throw new IllegalStateException("No image to save.");
        }

        BufferedImage imageToSave = composite;

        if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
            BufferedImage rgbImage = new BufferedImage(
                    composite.getWidth(),
                    composite.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(composite, 0, 0, Color.WHITE, null);
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

    public BufferedImage copyImage(BufferedImage source) {
        if (source == null) return null;
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
