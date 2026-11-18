package com.imageeditor.command;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.CropOperation;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

public class CropDocumentCommand implements Command {

    private final ImageDocument document;
    private final Rectangle cropArea;
    private final Map<Layer, BufferedImage> originalImages = new LinkedHashMap<>();
    private final Map<Layer, Point> originalPositions = new LinkedHashMap<>();
    private final Map<Layer, BufferedImage> croppedImages = new LinkedHashMap<>();
    private boolean initialized = false;

    public CropDocumentCommand(ImageDocument document, Rectangle cropArea) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null.");
        }
        if (document.getLayerCount() == 0 || !document.hasImage()) {
            throw new IllegalStateException("Please open an image first.");
        }

        int canvasWidth = document.getCanvasWidth();
        int canvasHeight = document.getCanvasHeight();
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            throw new IllegalStateException("Document has no valid image dimensions.");
        }

        if (cropArea == null) {
            throw new IllegalArgumentException("Please select a crop area first.");
        }
        if (cropArea.width <= 0 || cropArea.height <= 0) {
            throw new IllegalArgumentException("Crop selection must have positive width and height.");
        }
        if (cropArea.x < 0 || cropArea.y < 0
                || (long) cropArea.x + cropArea.width > canvasWidth
                || (long) cropArea.y + cropArea.height > canvasHeight) {
            throw new IllegalArgumentException("Crop area extends outside the document boundaries.");
        }

        this.document = document;
        this.cropArea = new Rectangle(cropArea);
    }

    @Override
    public void execute() {
        if (!initialized) {
            CropOperation cropOperation = new CropOperation(cropArea);
            for (Layer layer : document.getLayers()) {
                BufferedImage original = layer.getImage();
                originalImages.put(layer, original);
                originalPositions.put(layer, layer.getPosition());
                BufferedImage cropped = (original != null)
                        ? cropOperation.applyToLayer(original, layer.getX(), layer.getY())
                        : null;
                croppedImages.put(layer, cropped);
            }
            initialized = true;
        }

        for (Map.Entry<Layer, BufferedImage> entry : croppedImages.entrySet()) {
            Layer layer = entry.getKey();
            layer.setImage(entry.getValue());
            layer.setPosition(0, 0);
        }
    }

    @Override
    public void undo() {
        if (!initialized) {
            return;
        }

        for (Map.Entry<Layer, BufferedImage> entry : originalImages.entrySet()) {
            Layer layer = entry.getKey();
            layer.setImage(entry.getValue());
            Point originalPos = originalPositions.get(layer);
            if (originalPos != null) {
                layer.setPosition(originalPos.x, originalPos.y);
            }
        }
    }

    public Rectangle getCropArea() {
        return new Rectangle(cropArea);
    }
}

