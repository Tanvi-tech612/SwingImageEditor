package com.imageeditor.ui;

import com.imageeditor.controller.EditorController;
import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class CanvasPanel extends JPanel {

    private static final int PADDING = 24;

    private BufferedImage image;
    private ImageDocument document;
    private EditorController controller;

    // Crop selection state
    private boolean cropMode = false;
    private boolean cropDragging = false;
    private Point cropDragStartScreen = null;
    private Point cropDragCurrentScreen = null;
    private Rectangle selectedImageArea = null;
    private int lastImageWidth = -1;
    private int lastImageHeight = -1;
    private Consumer<Rectangle> cropSelectionListener;

    // Layer drag-and-move state
    private boolean layerDragging = false;
    private Layer draggingLayer = null;
    private Point layerDragStartScreen = null;
    private int layerDragOriginalX = 0;
    private int layerDragOriginalY = 0;
    private double layerDragScale = 1.0;
    private Consumer<String> layerMoveStatusListener;

    static final class DisplayBounds {
        final int x;
        final int y;
        final int width;
        final int height;
        final double scale;
        final int imageWidth;
        final int imageHeight;

        DisplayBounds(int x, int y, int width, int height, double scale, int imageWidth, int imageHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.scale = scale;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }
    }

    public CanvasPanel() {
        setBackground(new Color(24, 25, 27));
        setupMouseListeners();
    }

    private void setupMouseListeners() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 && !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                if (cropMode) {
                    handleCropMousePressed(e);
                } else {
                    handleLayerMousePressed(e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (cropMode) {
                    handleCropMouseDragged(e);
                } else if (layerDragging) {
                    handleLayerMouseDragged(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (cropMode) {
                    handleCropMouseReleased(e);
                } else if (layerDragging) {
                    handleLayerMouseReleased(e);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (!cropMode && !layerDragging) {
                    updateHoverCursor(e.getPoint());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!cropMode && !layerDragging) {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    // --- Crop mouse handlers ---

    private void handleCropMousePressed(MouseEvent e) {
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null) {
            return;
        }
        Point clamped = clampToDisplayBounds(e.getPoint(), bounds);
        cropDragging = true;
        cropDragStartScreen = clamped;
        cropDragCurrentScreen = clamped;
        selectedImageArea = null;
        notifyCropSelectionChanged();
        repaint();
    }

    private void handleCropMouseDragged(MouseEvent e) {
        if (!cropDragging || cropDragStartScreen == null) {
            return;
        }
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null) {
            clearCropSelection();
            return;
        }
        cropDragCurrentScreen = clampToDisplayBounds(e.getPoint(), bounds);
        selectedImageArea = convertScreenRectToImageRect(cropDragStartScreen, cropDragCurrentScreen, bounds);
        notifyCropSelectionChanged();
        repaint();
    }

    private void handleCropMouseReleased(MouseEvent e) {
        if (!cropDragging || cropDragStartScreen == null) {
            return;
        }
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null) {
            clearCropSelection();
            return;
        }
        cropDragCurrentScreen = clampToDisplayBounds(e.getPoint(), bounds);
        cropDragging = false;
        selectedImageArea = convertScreenRectToImageRect(cropDragStartScreen, cropDragCurrentScreen, bounds);
        if (selectedImageArea == null) {
            cropDragStartScreen = null;
            cropDragCurrentScreen = null;
        }
        notifyCropSelectionChanged();
        repaint();
    }

    // --- Layer drag-and-move mouse handlers ---

    private void handleLayerMousePressed(MouseEvent e) {
        if (document == null) {
            return;
        }
        Layer selected = document.getSelectedLayer();
        if (selected == null || !selected.isVisible() || selected.getImage() == null) {
            return;
        }
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null || bounds.scale <= 0) {
            return;
        }

        double docX = (e.getX() - bounds.x) / bounds.scale;
        double docY = (e.getY() - bounds.y) / bounds.scale;
        if (!selected.containsPoint(docX, docY)) {
            return;
        }

        layerDragging = true;
        draggingLayer = selected;
        layerDragStartScreen = e.getPoint();
        layerDragOriginalX = selected.getX();
        layerDragOriginalY = selected.getY();
        layerDragScale = bounds.scale;
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        repaint();
    }

    private void handleLayerMouseDragged(MouseEvent e) {
        if (!layerDragging || draggingLayer == null || layerDragStartScreen == null) {
            return;
        }
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        double scale = (bounds != null && bounds.scale > 0) ? bounds.scale : layerDragScale;

        int deltaDocX = (int) Math.round((e.getX() - layerDragStartScreen.x) / scale);
        int deltaDocY = (int) Math.round((e.getY() - layerDragStartScreen.y) / scale);
        int newX = layerDragOriginalX + deltaDocX;
        int newY = layerDragOriginalY + deltaDocY;

        draggingLayer.setPosition(newX, newY);
        notifyLayerMoveStatus("Moving \"" + draggingLayer.getName() + "\" to (" + newX + ", " + newY + ")");
        repaint();
    }

    private void handleLayerMouseReleased(MouseEvent e) {
        if (!layerDragging || draggingLayer == null || layerDragStartScreen == null) {
            cancelActiveLayerDrag(false);
            return;
        }

        DisplayBounds bounds = calculateCurrentDisplayBounds();
        double scale = (bounds != null && bounds.scale > 0) ? bounds.scale : layerDragScale;

        int deltaDocX = (int) Math.round((e.getX() - layerDragStartScreen.x) / scale);
        int deltaDocY = (int) Math.round((e.getY() - layerDragStartScreen.y) / scale);
        int finalX = layerDragOriginalX + deltaDocX;
        int finalY = layerDragOriginalY + deltaDocY;

        Layer movedLayer = draggingLayer;
        int origX = layerDragOriginalX;
        int origY = layerDragOriginalY;

        layerDragging = false;
        draggingLayer = null;
        layerDragStartScreen = null;

        if (finalX != origX || finalY != origY) {
            if (controller != null) {
                controller.moveLayerPosition(movedLayer, origX, origY, finalX, finalY);
            } else {
                movedLayer.setPosition(finalX, finalY);
                repaint();
            }
            notifyLayerMoveStatus("Moved layer \"" + movedLayer.getName() + "\" to (" + finalX + ", " + finalY + ")");
        } else {
            repaint();
        }

        updateHoverCursor(e.getPoint());
    }

    private void cancelActiveLayerDrag(boolean restoreOriginalPosition) {
        if (layerDragging && draggingLayer != null && restoreOriginalPosition) {
            draggingLayer.setPosition(layerDragOriginalX, layerDragOriginalY);
        }
        layerDragging = false;
        draggingLayer = null;
        layerDragStartScreen = null;
    }

    private void updateHoverCursor(Point screenPoint) {
        if (cropMode) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            return;
        }
        if (isPointOverSelectedLayer(screenPoint)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public boolean isPointOverSelectedLayer(Point screenPoint) {
        if (document == null || screenPoint == null) {
            return false;
        }
        Layer selected = document.getSelectedLayer();
        if (selected == null || !selected.isVisible() || selected.getImage() == null) {
            return false;
        }
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null || bounds.scale <= 0) {
            return false;
        }
        double docX = (screenPoint.x - bounds.x) / bounds.scale;
        double docY = (screenPoint.y - bounds.y) / bounds.scale;
        return selected.containsPoint(docX, docY);
    }

    public Point screenToDocumentCoordinates(Point screenPoint) {
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null || screenPoint == null || bounds.scale <= 0) {
            return null;
        }
        int docX = (int) Math.round((screenPoint.x - bounds.x) / bounds.scale);
        int docY = (int) Math.round((screenPoint.y - bounds.y) / bounds.scale);
        return new Point(docX, docY);
    }

    public boolean isLayerDragging() {
        return layerDragging;
    }

    public void setController(EditorController controller) {
        this.controller = controller;
        if (controller != null) {
            this.document = controller.getDocument();
        }
        repaint();
    }

    public void setLayerMoveStatusListener(Consumer<String> listener) {
        this.layerMoveStatusListener = listener;
    }

    private void notifyLayerMoveStatus(String message) {
        if (layerMoveStatusListener != null) {
            layerMoveStatusListener.accept(message);
        }
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        int newW = image != null ? image.getWidth() : -1;
        int newH = image != null ? image.getHeight() : -1;
        if (newW != lastImageWidth || newH != lastImageHeight) {
            lastImageWidth = newW;
            lastImageHeight = newH;
            clearCropSelectionInternal(true);
        }
        repaint();
    }

    public void setDocument(ImageDocument document) {
        this.document = document;
        repaint();
    }

    public BufferedImage getImage() {
        if (document != null) {
            BufferedImage comp = document.getCompositeImage();
            if (comp != null) {
                return comp;
            }
        }
        return image;
    }

    public void setCropMode(boolean enabled) {
        if (enabled && layerDragging) {
            cancelActiveLayerDrag(true);
        }
        this.cropMode = enabled;
        setCursor(enabled
                ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : Cursor.getDefaultCursor());
        clearCropSelectionInternal(true);
        repaint();
    }

    public boolean isCropMode() {
        return cropMode;
    }

    public void clearCropSelection() {
        clearCropSelectionInternal(true);
        repaint();
    }

    private void clearCropSelectionInternal(boolean notify) {
        cropDragging = false;
        cropDragStartScreen = null;
        cropDragCurrentScreen = null;
        selectedImageArea = null;
        if (notify) {
            notifyCropSelectionChanged();
        }
    }

    public void setCropSelectionListener(Consumer<Rectangle> listener) {
        this.cropSelectionListener = listener;
    }

    private void notifyCropSelectionChanged() {
        if (cropSelectionListener != null) {
            cropSelectionListener.accept(getCropAreaInImageCoordinates());
        }
    }

    public boolean hasValidCropSelection() {
        return getCropAreaInImageCoordinates() != null;
    }

    public Rectangle getCropAreaInImageCoordinates() {
        if (!cropMode || selectedImageArea == null) {
            return null;
        }
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null || bounds.imageWidth <= 0 || bounds.imageHeight <= 0) {
            return null;
        }

        int imgW = bounds.imageWidth;
        int imgH = bounds.imageHeight;

        int x1 = Math.max(0, Math.min(imgW, selectedImageArea.x));
        int y1 = Math.max(0, Math.min(imgH, selectedImageArea.y));
        int x2 = Math.max(0, Math.min(imgW, selectedImageArea.x + selectedImageArea.width));
        int y2 = Math.max(0, Math.min(imgH, selectedImageArea.y + selectedImageArea.height));

        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) {
            return null;
        }
        return new Rectangle(x1, y1, w, h);
    }

    public Rectangle getDisplayedImageBounds() {
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null) {
            return null;
        }
        return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public double getCurrentDisplayScale() {
        DisplayBounds bounds = calculateCurrentDisplayBounds();
        return bounds != null ? bounds.scale : 1.0;
    }

    DisplayBounds calculateCurrentDisplayBounds() {
        if (document != null && document.hasImage()) {
            int canvasW = document.getCanvasWidth();
            int canvasH = document.getCanvasHeight();
            if (canvasW > 0 && canvasH > 0) {
                return calculateDisplayBounds(canvasW, canvasH);
            }
        }
        if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
            return calculateDisplayBounds(image.getWidth(), image.getHeight());
        }
        return null;
    }

    DisplayBounds calculateDisplayBounds(BufferedImage displayImage) {
        if (displayImage == null) {
            return null;
        }
        return calculateDisplayBounds(displayImage.getWidth(), displayImage.getHeight());
    }

    DisplayBounds calculateDisplayBounds(int contentWidth, int contentHeight) {
        if (contentWidth <= 0 || contentHeight <= 0) {
            return null;
        }

        int availWidth = Math.max(1, getWidth() - PADDING * 2);
        int availHeight = Math.max(1, getHeight() - PADDING * 2);

        double scaleX = (double) availWidth / contentWidth;
        double scaleY = (double) availHeight / contentHeight;
        double scale = Math.min(scaleX, scaleY);

        int newWidth = Math.max(1, (int) (contentWidth * scale));
        int newHeight = Math.max(1, (int) (contentHeight * scale));

        int x = (getWidth() - newWidth) / 2;
        int y = (getHeight() - newHeight) / 2;

        return new DisplayBounds(x, y, newWidth, newHeight, scale, contentWidth, contentHeight);
    }

    private Point clampToDisplayBounds(Point p, DisplayBounds bounds) {
        int clampedX = Math.max(bounds.x, Math.min(bounds.x + bounds.width, p.x));
        int clampedY = Math.max(bounds.y, Math.min(bounds.y + bounds.height, p.y));
        return new Point(clampedX, clampedY);
    }

    private Rectangle convertScreenRectToImageRect(Point p1, Point p2, DisplayBounds bounds) {
        if (p1 == null || p2 == null || bounds == null || bounds.scale <= 0) {
            return null;
        }

        int screenX1 = Math.max(bounds.x, Math.min(bounds.x + bounds.width, Math.min(p1.x, p2.x)));
        int screenY1 = Math.max(bounds.y, Math.min(bounds.y + bounds.height, Math.min(p1.y, p2.y)));
        int screenX2 = Math.max(bounds.x, Math.min(bounds.x + bounds.width, Math.max(p1.x, p2.x)));
        int screenY2 = Math.max(bounds.y, Math.min(bounds.y + bounds.height, Math.max(p1.y, p2.y)));

        if (screenX2 - screenX1 <= 0 || screenY2 - screenY1 <= 0) {
            return null;
        }

        int imageX1 = (int) Math.round((screenX1 - bounds.x) / bounds.scale);
        int imageY1 = (int) Math.round((screenY1 - bounds.y) / bounds.scale);
        int imageX2 = (screenX2 >= bounds.x + bounds.width)
                ? bounds.imageWidth
                : (int) Math.round((screenX2 - bounds.x) / bounds.scale);
        int imageY2 = (screenY2 >= bounds.y + bounds.height)
                ? bounds.imageHeight
                : (int) Math.round((screenY2 - bounds.y) / bounds.scale);

        imageX1 = Math.max(0, Math.min(bounds.imageWidth, imageX1));
        imageY1 = Math.max(0, Math.min(bounds.imageHeight, imageY1));
        imageX2 = Math.max(0, Math.min(bounds.imageWidth, imageX2));
        imageY2 = Math.max(0, Math.min(bounds.imageHeight, imageY2));

        int width = imageX2 - imageX1;
        int height = imageY2 - imageY1;
        if (width <= 0 || height <= 0) {
            return null;
        }

        return new Rectangle(imageX1, imageY1, width, height);
    }

    private Rectangle getActiveScreenSelectionRect(DisplayBounds bounds) {
        if (bounds == null) {
            return null;
        }
        if (cropDragging && cropDragStartScreen != null && cropDragCurrentScreen != null) {
            int sx1 = Math.max(bounds.x, Math.min(bounds.x + bounds.width, Math.min(cropDragStartScreen.x, cropDragCurrentScreen.x)));
            int sy1 = Math.max(bounds.y, Math.min(bounds.y + bounds.height, Math.min(cropDragStartScreen.y, cropDragCurrentScreen.y)));
            int sx2 = Math.max(bounds.x, Math.min(bounds.x + bounds.width, Math.max(cropDragStartScreen.x, cropDragCurrentScreen.x)));
            int sy2 = Math.max(bounds.y, Math.min(bounds.y + bounds.height, Math.max(cropDragStartScreen.y, cropDragCurrentScreen.y)));
            if (sx2 > sx1 && sy2 > sy1) {
                return new Rectangle(sx1, sy1, sx2 - sx1, sy2 - sy1);
            }
            return null;
        }
        if (selectedImageArea != null) {
            int sx1 = bounds.x + (int) Math.round(selectedImageArea.x * bounds.scale);
            int sy1 = bounds.y + (int) Math.round(selectedImageArea.y * bounds.scale);
            int sx2 = (selectedImageArea.x + selectedImageArea.width >= bounds.imageWidth)
                    ? bounds.x + bounds.width
                    : bounds.x + (int) Math.round((selectedImageArea.x + selectedImageArea.width) * bounds.scale);
            int sy2 = (selectedImageArea.y + selectedImageArea.height >= bounds.imageHeight)
                    ? bounds.y + bounds.height
                    : bounds.y + (int) Math.round((selectedImageArea.y + selectedImageArea.height) * bounds.scale);

            sx1 = Math.max(bounds.x, Math.min(bounds.x + bounds.width, sx1));
            sy1 = Math.max(bounds.y, Math.min(bounds.y + bounds.height, sy1));
            sx2 = Math.max(bounds.x, Math.min(bounds.x + bounds.width, sx2));
            sy2 = Math.max(bounds.y, Math.min(bounds.y + bounds.height, sy2));

            if (sx2 > sx1 && sy2 > sy1) {
                return new Rectangle(sx1, sy1, sx2 - sx1, sy2 - sy1);
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean hasDocLayers = document != null && document.hasImage();
        if (!hasDocLayers && image == null) {
            drawEmptyState(g2d);
            g2d.dispose();
            return;
        }

        DisplayBounds bounds = calculateCurrentDisplayBounds();
        if (bounds == null) {
            drawEmptyState(g2d);
            g2d.dispose();
            return;
        }

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Subtle drop shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillRoundRect(bounds.x + 3, bounds.y + 4, bounds.width, bounds.height, 4, 4);

        if (hasDocLayers) {
            // Subtle dark checkerboard background so moved/transparent areas are clearly visible
            drawTransparencyCheckerboard(g2d, bounds.x, bounds.y, bounds.width, bounds.height);

            // Render each layer directly at its own (x, y) document coordinates
            Graphics2D canvasG = (Graphics2D) g2d.create();
            try {
                canvasG.clipRect(bounds.x, bounds.y, bounds.width, bounds.height);
                canvasG.translate(bounds.x, bounds.y);
                double scaleX = (double) bounds.width / bounds.imageWidth;
                double scaleY = (double) bounds.height / bounds.imageHeight;
                canvasG.scale(scaleX, scaleY);

                for (Layer layer : document.getLayers()) {
                    if (!layer.isVisible() || layer.getOpacity() <= 0.0f || layer.getImage() == null) {
                        continue;
                    }
                    float alpha = Math.max(0.0f, Math.min(1.0f, layer.getOpacity()));
                    canvasG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    canvasG.drawImage(layer.getImage(), layer.getX(), layer.getY(), null);
                }
            } finally {
                canvasG.dispose();
            }

            // Visual guide around the layer actively being dragged
            if (layerDragging && draggingLayer != null && draggingLayer.getImage() != null) {
                drawDraggedLayerOutline(g2d, bounds, draggingLayer);
            }
        } else {
            g2d.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }

        // Subtle outline border around document canvas
        g2d.setColor(new Color(255, 255, 255, 35));
        g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Draw crop overlay when in crop mode
        if (cropMode) {
            drawCropOverlay(g2d, bounds);
        }

        g2d.dispose();
    }

    private void drawTransparencyCheckerboard(Graphics2D g2d, int x, int y, int width, int height) {
        int tileSize = 10;
        Color light = new Color(46, 48, 54);
        Color dark = new Color(36, 38, 43);

        Graphics2D clipG = (Graphics2D) g2d.create();
        try {
            clipG.clipRect(x, y, width, height);
            clipG.setColor(dark);
            clipG.fillRect(x, y, width, height);
            clipG.setColor(light);
            for (int row = 0; row * tileSize < height; row++) {
                for (int col = (row % 2); col * tileSize < width; col += 2) {
                    clipG.fillRect(x + col * tileSize, y + row * tileSize, tileSize, tileSize);
                }
            }
        } finally {
            clipG.dispose();
        }
    }

    private void drawDraggedLayerOutline(Graphics2D g2d, DisplayBounds bounds, Layer layer) {
        double scaleX = (double) bounds.width / bounds.imageWidth;
        double scaleY = (double) bounds.height / bounds.imageHeight;
        int sx = bounds.x + (int) Math.round(layer.getX() * scaleX);
        int sy = bounds.y + (int) Math.round(layer.getY() * scaleY);
        int sw = Math.max(1, (int) Math.round(layer.getWidth() * scaleX));
        int sh = Math.max(1, (int) Math.round(layer.getHeight() * scaleY));

        Stroke dashed = new BasicStroke(
                1.5f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f,
                new float[]{5.0f, 3.0f},
                0.0f
        );
        g2d.setStroke(dashed);
        g2d.setColor(new Color(82, 130, 255, 220));
        g2d.drawRect(sx, sy, sw, sh);
    }

    private void drawCropOverlay(Graphics2D g2d, DisplayBounds bounds) {
        Rectangle sel = getActiveScreenSelectionRect(bounds);

        if (sel == null) {
            g2d.setColor(new Color(0, 0, 0, 45));
            g2d.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            return;
        }

        int imgRight = bounds.x + bounds.width;
        int imgBottom = bounds.y + bounds.height;
        int selRight = sel.x + sel.width;
        int selBottom = sel.y + sel.height;

        // Visually darken the area outside the selection within the displayed image
        g2d.setColor(new Color(0, 0, 0, 145));
        if (sel.y > bounds.y) {
            g2d.fillRect(bounds.x, bounds.y, bounds.width, sel.y - bounds.y);
        }
        if (selBottom < imgBottom) {
            g2d.fillRect(bounds.x, selBottom, bounds.width, imgBottom - selBottom);
        }
        if (sel.x > bounds.x) {
            g2d.fillRect(bounds.x, sel.y, sel.x - bounds.x, sel.height);
        }
        if (selRight < imgRight) {
            g2d.fillRect(selRight, sel.y, imgRight - selRight, sel.height);
        }

        // Subtle rule-of-thirds grid lines inside selection
        if (sel.width >= 30 && sel.height >= 30) {
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.setColor(new Color(255, 255, 255, 75));
            int thirdW = sel.width / 3;
            int thirdH = sel.height / 3;
            g2d.drawLine(sel.x + thirdW, sel.y, sel.x + thirdW, selBottom);
            g2d.drawLine(sel.x + thirdW * 2, sel.y, sel.x + thirdW * 2, selBottom);
            g2d.drawLine(sel.x, sel.y + thirdH, selRight, sel.y + thirdH);
            g2d.drawLine(sel.x, sel.y + thirdH * 2, selRight, sel.y + thirdH * 2);
        }

        // High-contrast selection border (dark shadow + dashed white outline)
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.drawRect(sel.x, sel.y, sel.width, sel.height);

        Stroke dashed = new BasicStroke(
                1.5f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f,
                new float[]{5.0f, 3.0f},
                0.0f
        );
        g2d.setStroke(dashed);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(sel.x, sel.y, sel.width, sel.height);

        // Corner handles
        g2d.setStroke(new BasicStroke(1.0f));
        int handleSize = 6;
        int half = handleSize / 2;
        int[][] corners = {
                {sel.x, sel.y},
                {selRight, sel.y},
                {sel.x, selBottom},
                {selRight, selBottom}
        };
        for (int[] corner : corners) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(corner[0] - half, corner[1] - half, handleSize, handleSize);
            g2d.setColor(new Color(30, 32, 36));
            g2d.drawRect(corner[0] - half, corner[1] - half, handleSize, handleSize);
        }

        // Dimension badge
        if (selectedImageArea != null) {
            String dimText = selectedImageArea.width + " × " + selectedImageArea.height + " px";
            g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
            FontMetrics fm = g2d.getFontMetrics();
            int textW = fm.stringWidth(dimText);
            int textH = fm.getHeight();
            int badgeW = textW + 12;
            int badgeH = textH + 4;
            int badgeX = Math.max(bounds.x, Math.min(imgRight - badgeW, sel.x));
            int badgeY = sel.y - badgeH - 6;
            if (badgeY < 4) {
                badgeY = sel.y + 6;
            }

            g2d.setColor(new Color(20, 22, 26, 210));
            g2d.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
            g2d.setColor(new Color(240, 242, 248));
            g2d.drawString(dimText, badgeX + 6, badgeY + fm.getAscent() + 2);
        }
    }

    private void drawEmptyState(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        int boxW = 340;
        int boxH = 170;
        int boxX = centerX - boxW / 2;
        int boxY = centerY - boxH / 2;

        // Background card
        g2d.setColor(new Color(32, 33, 36));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Dashed border
        Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 4.0f}, 0.0f);
        g2d.setStroke(dashed);
        g2d.setColor(new Color(65, 68, 75));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Icon
        int iconSize = 40;
        int iconX = centerX - iconSize / 2;
        int iconY = boxY + 24;

        g2d.setStroke(new BasicStroke(1.8f));
        g2d.setColor(new Color(115, 120, 132));
        g2d.drawRoundRect(iconX, iconY, iconSize, iconSize, 6, 6);
        g2d.fillOval(iconX + 24, iconY + 8, 7, 7);
        int[] xPoints = {iconX + 6, iconX + 18, iconX + 26, iconX + 34};
        int[] yPoints = {iconY + 32, iconY + 18, iconY + 26, iconY + 32};
        g2d.drawPolyline(xPoints, yPoints, 4);

        // Title
        g2d.setColor(new Color(215, 218, 225));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 15));
        FontMetrics fm1 = g2d.getFontMetrics();
        String title = "Open an image to get started";
        g2d.drawString(title, centerX - fm1.stringWidth(title) / 2, iconY + iconSize + 30);

        // Subtitle
        g2d.setColor(new Color(130, 134, 145));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm2 = g2d.getFontMetrics();
        String subtitle = "Supports PNG and JPG formats";
        g2d.drawString(subtitle, centerX - fm2.stringWidth(subtitle) / 2, iconY + iconSize + 52);
    }
}
