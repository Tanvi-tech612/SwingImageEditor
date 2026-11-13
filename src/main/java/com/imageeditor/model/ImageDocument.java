package com.imageeditor.model;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageDocument {

    private final List<Layer> layers = new ArrayList<>();
    private Layer selectedLayer = null;
    private int layerCounter = 0;

    public ImageDocument() {
    }

    public List<Layer> getLayers() {
        return Collections.unmodifiableList(layers);
    }

    public int getLayerCount() {
        return layers.size();
    }

    public Layer getLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            return layers.get(index);
        }
        return null;
    }

    public int indexOf(Layer layer) {
        return layers.indexOf(layer);
    }

    public Layer getSelectedLayer() {
        return selectedLayer;
    }

    public int getSelectedIndex() {
        return layers.indexOf(selectedLayer);
    }

    public void setSelectedLayer(Layer layer) {
        if (layer == null || layers.contains(layer)) {
            this.selectedLayer = layer;
        }
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < layers.size()) {
            this.selectedLayer = layers.get(index);
        }
    }

    public String generateNextLayerName() {
        layerCounter++;
        return "Layer " + layerCounter;
    }

    public void addLayer(Layer layer) {
        addLayer(layers.size(), layer);
    }

    public void addLayer(int index, Layer layer) {
        if (layer == null) {
            throw new IllegalArgumentException("Layer cannot be null.");
        }
        int insertIndex = Math.max(0, Math.min(index, layers.size()));
        layers.add(insertIndex, layer);
        selectedLayer = layer;
    }

    public boolean removeLayer(Layer layer) {
        if (layer == null) {
            return false;
        }
        if (layers.size() <= 1) {
            // Prevent deleting the final remaining layer
            return false;
        }
        int index = layers.indexOf(layer);
        if (index == -1) {
            return false;
        }
        layers.remove(index);
        if (selectedLayer == layer) {
            int newIndex = Math.min(index, layers.size() - 1);
            selectedLayer = layers.get(newIndex);
        }
        return true;
    }

    public Layer removeLayerAt(int index) {
        if (layers.size() <= 1 || index < 0 || index >= layers.size()) {
            return null;
        }
        Layer removed = layers.remove(index);
        if (selectedLayer == removed) {
            int newIndex = Math.min(index, layers.size() - 1);
            selectedLayer = layers.get(newIndex);
        }
        return removed;
    }

    public boolean canMoveUp(Layer layer) {
        if (layer == null) return false;
        int index = layers.indexOf(layer);
        return index >= 0 && index < layers.size() - 1;
    }

    public boolean canMoveDown(Layer layer) {
        if (layer == null) return false;
        int index = layers.indexOf(layer);
        return index > 0;
    }

    public void moveLayer(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= layers.size() || toIndex < 0 || toIndex >= layers.size() || fromIndex == toIndex) {
            return;
        }
        Layer layer = layers.remove(fromIndex);
        layers.add(toIndex, layer);
    }

    public boolean moveLayerUp(Layer layer) {
        int index = layers.indexOf(layer);
        if (index >= 0 && index < layers.size() - 1) {
            Collections.swap(layers, index, index + 1);
            return true;
        }
        return false;
    }

    public boolean moveLayerDown(Layer layer) {
        int index = layers.indexOf(layer);
        if (index > 0) {
            Collections.swap(layers, index, index - 1);
            return true;
        }
        return false;
    }

    public void clear() {
        layers.clear();
        selectedLayer = null;
        layerCounter = 0;
    }

    public int getCanvasWidth() {
        int maxW = 0;
        for (Layer l : layers) {
            if (l.getImage() != null) {
                maxW = Math.max(maxW, l.getImage().getWidth());
            }
        }
        return maxW;
    }

    public int getCanvasHeight() {
        int maxH = 0;
        for (Layer l : layers) {
            if (l.getImage() != null) {
                maxH = Math.max(maxH, l.getImage().getHeight());
            }
        }
        return maxH;
    }

    public BufferedImage getCompositeImage() {
        if (layers.isEmpty()) {
            return null;
        }
        int width = getCanvasWidth();
        int height = getCanvasHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = composite.createGraphics();

        try {
            // Render from index 0 (bottom layer) to index size-1 (top layer)
            for (Layer layer : layers) {
                if (!layer.isVisible() || layer.getOpacity() <= 0.0f || layer.getImage() == null) {
                    continue;
                }
                float alpha = Math.max(0.0f, Math.min(1.0f, layer.getOpacity()));
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.drawImage(layer.getImage(), 0, 0, null);
            }
        } finally {
            g2d.dispose();
        }

        return composite;
    }

    // --- Backward Compatibility methods for existing code ---

    public BufferedImage getImage() {
        if (selectedLayer != null && selectedLayer.getImage() != null) {
            return selectedLayer.getImage();
        }
        return getCompositeImage();
    }

    public void setImage(BufferedImage image) {
        if (image == null) {
            clear();
            return;
        }
        if (layers.isEmpty()) {
            Layer bg = new Layer("Background", image);
            addLayer(bg);
        } else if (selectedLayer != null) {
            selectedLayer.setImage(image);
        } else {
            layers.get(0).setImage(image);
        }
    }

    public boolean hasImage() {
        if (layers.isEmpty()) {
            return false;
        }
        for (Layer l : layers) {
            if (l.getImage() != null) {
                return true;
            }
        }
        return false;
    }
}