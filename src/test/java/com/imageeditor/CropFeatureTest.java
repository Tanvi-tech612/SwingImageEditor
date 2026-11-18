package com.imageeditor;

import com.imageeditor.command.CropDocumentCommand;
import com.imageeditor.controller.EditorController;
import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.CropOperation;
import com.imageeditor.ui.CanvasPanel;
import com.imageeditor.ui.MainFrame;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CropFeatureTest {

    private static BufferedImage createQuadrantImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int halfW = width / 2;
        int halfH = height / 2;
        // Top-left: Red, Top-right: Green, Bottom-left: Blue, Bottom-right: Yellow
        g.setColor(Color.RED);
        g.fillRect(0, 0, halfW, halfH);
        g.setColor(Color.GREEN);
        g.fillRect(halfW, 0, width - halfW, halfH);
        g.setColor(Color.BLUE);
        g.fillRect(0, halfH, halfW, height - halfH);
        g.setColor(Color.YELLOW);
        g.fillRect(halfW, halfH, width - halfW, height - halfH);
        g.dispose();
        return img;
    }

    private static void simulateDrag(CanvasPanel panel, int x1, int y1, int x2, int y2) {
        long now = System.currentTimeMillis();
        MouseEvent press = new MouseEvent(
                panel, MouseEvent.MOUSE_PRESSED, now,
                MouseEvent.BUTTON1_DOWN_MASK, x1, y1, 1, false, MouseEvent.BUTTON1
        );
        MouseEvent drag = new MouseEvent(
                panel, MouseEvent.MOUSE_DRAGGED, now + 10,
                MouseEvent.BUTTON1_DOWN_MASK, x2, y2, 0, false, MouseEvent.NOBUTTON
        );
        MouseEvent release = new MouseEvent(
                panel, MouseEvent.MOUSE_RELEASED, now + 20,
                0, x2, y2, 1, false, MouseEvent.BUTTON1
        );

        for (MouseListener ml : panel.getMouseListeners()) {
            ml.mousePressed(press);
        }
        for (MouseMotionListener mml : panel.getMouseMotionListeners()) {
            mml.mouseDragged(drag);
        }
        for (MouseListener ml : panel.getMouseListeners()) {
            ml.mouseReleased(release);
        }
    }

    // Scenario 1: Crop the central portion of an image
    @Test
    public void testCropCentralPortion() {
        EditorController controller = new EditorController();
        BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, 200, 100);
        g.setColor(Color.MAGENTA);
        g.fillRect(50, 25, 100, 50);
        g.dispose();

        controller.addLayer(new Layer("Background", img));
        AtomicInteger notifications = new AtomicInteger(0);
        controller.addChangeListener(notifications::incrementAndGet);

        controller.cropDocument(new Rectangle(50, 25, 100, 50));

        BufferedImage cropped = controller.getDocument().getCompositeImage();
        assertNotNull(cropped);
        assertEquals(100, cropped.getWidth());
        assertEquals(50, cropped.getHeight());
        assertEquals(Color.MAGENTA.getRGB(), cropped.getRGB(0, 0));
        assertEquals(Color.MAGENTA.getRGB(), cropped.getRGB(99, 49));
        assertTrue(notifications.get() > 0, "Change listener must be notified after crop");
    }

    // Scenarios 2 & 3: Top-left to bottom-right and bottom-right to top-left drags
    @Test
    public void testDragTopLeftToBottomRightAndReverse() {
        BufferedImage img = createQuadrantImage(1000, 500);
        CanvasPanel canvas = new CanvasPanel();
        canvas.setSize(800, 600);
        canvas.setImage(img);
        canvas.setCropMode(true);
        assertEquals(Cursor.CROSSHAIR_CURSOR, canvas.getCursor().getType());

        Rectangle disp = canvas.getDisplayedImageBounds();
        assertNotNull(disp);
        // Select central 50% top-left to bottom-right
        int sx1 = disp.x + disp.width / 4;
        int sy1 = disp.y + disp.height / 4;
        int sx2 = disp.x + (disp.width * 3) / 4;
        int sy2 = disp.y + (disp.height * 3) / 4;

        simulateDrag(canvas, sx1, sy1, sx2, sy2);
        Rectangle rectForward = canvas.getCropAreaInImageCoordinates();
        assertNotNull(rectForward);
        assertEquals(250, rectForward.x, 1);
        assertEquals(125, rectForward.y, 1);
        assertEquals(500, rectForward.width, 2);
        assertEquals(250, rectForward.height, 2);

        // Now drag bottom-right to top-left
        simulateDrag(canvas, sx2, sy2, sx1, sy1);
        Rectangle rectReverse = canvas.getCropAreaInImageCoordinates();
        assertEquals(rectForward, rectReverse, "Bottom-right to top-left drag must yield identical rectangle");

        // Also test top-right to bottom-left
        simulateDrag(canvas, sx2, sy1, sx1, sy2);
        assertEquals(rectForward, canvas.getCropAreaInImageCoordinates());
    }

    // Scenario 4: Crop near image boundaries (clamping to displayed image edges)
    @Test
    public void testCropNearBoundariesClampsCleanly() {
        BufferedImage img = createQuadrantImage(400, 300);
        CanvasPanel canvas = new CanvasPanel();
        canvas.setSize(600, 500);
        canvas.setImage(img);
        canvas.setCropMode(true);

        Rectangle disp = canvas.getDisplayedImageBounds();
        assertNotNull(disp);

        // Drag from outside top-left into the center
        simulateDrag(canvas, 0, 0, disp.x + disp.width / 2, disp.y + disp.height / 2);
        Rectangle topLeftCrop = canvas.getCropAreaInImageCoordinates();
        assertNotNull(topLeftCrop);
        assertEquals(0, topLeftCrop.x);
        assertEquals(0, topLeftCrop.y);
        assertEquals(200, topLeftCrop.width, 2);
        assertEquals(150, topLeftCrop.height, 2);

        // Drag from center to outside bottom-right
        simulateDrag(canvas, disp.x + disp.width / 2, disp.y + disp.height / 2, 599, 499);
        Rectangle bottomRightCrop = canvas.getCropAreaInImageCoordinates();
        assertNotNull(bottomRightCrop);
        assertEquals(400, bottomRightCrop.x + bottomRightCrop.width);
        assertEquals(300, bottomRightCrop.y + bottomRightCrop.height);

        // Dragging completely outside image on the left must be rejected (zero width)
        simulateDrag(canvas, 0, 10, Math.max(0, disp.x - 5), 200);
        assertNull(canvas.getCropAreaInImageCoordinates());
    }

    // Scenario 5: Crop after resizing the application window (both before drag and after drag)
    @Test
    public void testCropAfterResizingCanvas() {
        EditorController controller = new EditorController();
        BufferedImage img = createQuadrantImage(800, 400);
        controller.addLayer(new Layer("Background", img));

        CanvasPanel canvas = new CanvasPanel();
        canvas.setDocument(controller.getDocument());
        canvas.setSize(500, 400);
        canvas.setCropMode(true);

        // Resize canvas BEFORE dragging
        canvas.setSize(1200, 800);
        Rectangle dispAfterResize = canvas.getDisplayedImageBounds();
        assertNotNull(dispAfterResize);

        // Select top-right quadrant (Green)
        int midX = dispAfterResize.x + dispAfterResize.width / 2;
        int topY = dispAfterResize.y;
        int rightX = dispAfterResize.x + dispAfterResize.width;
        int midY = dispAfterResize.y + dispAfterResize.height / 2;
        simulateDrag(canvas, midX, topY, rightX, midY);

        Rectangle beforeSecondResize = canvas.getCropAreaInImageCoordinates();
        assertNotNull(beforeSecondResize);

        // Resize canvas AGAIN before applying crop
        canvas.setSize(700, 500);
        Rectangle afterSecondResize = canvas.getCropAreaInImageCoordinates();
        assertEquals(beforeSecondResize, afterSecondResize,
                "Crop selection in image coordinates must remain invariant after window resize");

        controller.cropDocument(afterSecondResize);
        BufferedImage result = controller.getDocument().getCompositeImage();
        assertNotNull(result);
        assertEquals(400, result.getWidth(), 2);
        assertEquals(200, result.getHeight(), 2);
        assertEquals(Color.GREEN.getRGB(), result.getRGB(result.getWidth() / 2, result.getHeight() / 2));
    }

    // Scenario 6: Cancel crop without changing the document
    @Test
    public void testCancelCropLeavesDocumentUnchanged() {
        EditorController controller = new EditorController();
        BufferedImage img = createQuadrantImage(120, 80);
        controller.addLayer(new Layer("Background", img));

        CanvasPanel canvas = new CanvasPanel();
        canvas.setDocument(controller.getDocument());
        canvas.setSize(500, 400);
        canvas.setCropMode(true);

        Rectangle disp = canvas.getDisplayedImageBounds();
        simulateDrag(canvas, disp.x + 10, disp.y + 10, disp.x + 60, disp.y + 60);
        assertNotNull(canvas.getCropAreaInImageCoordinates());

        // Cancel crop
        canvas.setCropMode(false);
        assertFalse(canvas.isCropMode());
        assertNull(canvas.getCropAreaInImageCoordinates());
        assertEquals(Cursor.DEFAULT_CURSOR, canvas.getCursor().getType());

        // Document remains completely unchanged
        assertEquals(120, controller.getDocument().getCanvasWidth());
        assertEquals(80, controller.getDocument().getCanvasHeight());
    }

    // Scenarios 7 & 8: Undo and Redo a crop
    @Test
    public void testCropUndoAndRedo() {
        EditorController controller = new EditorController();
        BufferedImage img = createQuadrantImage(100, 100);
        Layer bg = new Layer("Background", img);
        controller.addLayer(bg);

        Rectangle cropRect = new Rectangle(50, 50, 50, 50); // Bottom-right (Yellow)
        controller.cropDocument(cropRect);

        assertEquals(50, bg.getWidth());
        assertEquals(50, bg.getHeight());
        assertEquals(Color.YELLOW.getRGB(), bg.getImage().getRGB(10, 10));

        // Undo
        assertTrue(controller.canUndo());
        controller.undo();
        assertEquals(100, bg.getWidth());
        assertEquals(100, bg.getHeight());
        assertEquals(Color.RED.getRGB(), bg.getImage().getRGB(10, 10));
        assertEquals(Color.YELLOW.getRGB(), bg.getImage().getRGB(75, 75));

        // Redo
        assertTrue(controller.canRedo());
        controller.redo();
        assertEquals(50, bg.getWidth());
        assertEquals(50, bg.getHeight());
        assertEquals(Color.YELLOW.getRGB(), bg.getImage().getRGB(10, 10));

        // Undo a second time to verify original image data was not lost during redo
        controller.undo();
        assertEquals(100, bg.getWidth());
        assertEquals(100, bg.getHeight());
        assertEquals(Color.RED.getRGB(), bg.getImage().getRGB(10, 10));
    }

    // Scenarios 9, 10, 11: Multi-layer crop, alignment, transparency, smaller layers, opacity & visibility
    @Test
    public void testMultiLayerCropPreservesAlignmentOrderPropertiesAndTransparency() {
        EditorController controller = new EditorController();
        ImageDocument doc = controller.getDocument();

        // Layer 0 (bottom, 100x100): solid RED with a BLUE marker at (30, 40)
        BufferedImage img0 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g0 = img0.createGraphics();
        g0.setColor(Color.RED);
        g0.fillRect(0, 0, 100, 100);
        g0.setColor(Color.BLUE);
        g0.fillRect(30, 40, 10, 10);
        g0.dispose();
        Layer layer0 = new Layer("Bottom", img0);
        layer0.setOpacity(0.8f);
        layer0.setVisible(true);

        // Layer 1 (middle, smaller 50x50): transparent background with semi-transparent GREEN square at (30, 40)
        BufferedImage img1 = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(new Color(0, 255, 0, 128));
        g1.fillRect(30, 40, 10, 10);
        g1.dispose();
        Layer layer1 = new Layer("SmallOverlay", img1);
        layer1.setOpacity(0.65f);
        layer1.setVisible(true);

        // Layer 2 (top, hidden, very small 15x15 completely outside crop area)
        BufferedImage img2 = new BufferedImage(15, 15, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.CYAN);
        g2.fillRect(0, 0, 15, 15);
        g2.dispose();
        Layer layer2 = new Layer("HiddenSmall", img2);
        layer2.setOpacity(0.4f);
        layer2.setVisible(false);

        controller.addLayer(layer0);
        controller.addLayer(layer1);
        controller.addLayer(layer2);
        controller.selectLayer(layer1);

        // Crop document region [20..80) x [30..80) -> 60x50
        Rectangle cropRect = new Rectangle(20, 30, 60, 50);
        controller.cropDocument(cropRect);

        // Layer count, order, selection, visibility, opacity must all be preserved (not flattened!)
        assertEquals(3, doc.getLayerCount());
        assertEquals(0, doc.indexOf(layer0));
        assertEquals(1, doc.indexOf(layer1));
        assertEquals(2, doc.indexOf(layer2));
        assertSame(layer1, doc.getSelectedLayer());

        assertEquals(0.8f, layer0.getOpacity(), 0.0001f);
        assertTrue(layer0.isVisible());
        assertEquals(0.65f, layer1.getOpacity(), 0.0001f);
        assertTrue(layer1.isVisible());
        assertEquals(0.4f, layer2.getOpacity(), 0.0001f);
        assertFalse(layer2.isVisible());

        // Every layer has cropped dimensions 60x50
        assertEquals(60, layer0.getWidth());
        assertEquals(50, layer0.getHeight());
        assertEquals(60, layer1.getWidth());
        assertEquals(50, layer1.getHeight());
        assertEquals(60, layer2.getWidth());
        assertEquals(50, layer2.getHeight());

        // Alignment check: document pixel (30, 40) moved to (30 - 20, 40 - 30) = (10, 10) on BOTH layer0 and layer1
        assertEquals(Color.BLUE.getRGB(), layer0.getImage().getRGB(10, 10));
        assertEquals(Color.RED.getRGB(), layer0.getImage().getRGB(0, 0));

        // Semi-transparent pixel on layer1 preserved at exact aligned position (10, 10)
        int overlayPixel = layer1.getImage().getRGB(10, 10);
        assertEquals(128, (overlayPixel >>> 24) & 0xFF, "Alpha channel must be preserved");
        assertEquals(255, (overlayPixel >>> 8) & 0xFF, "Green channel must be preserved");

        // Region outside smaller layer1 (originally x >= 50, now x >= 30) must be fully transparent
        assertEquals(0, (layer1.getImage().getRGB(45, 35) >>> 24) & 0xFF);

        // Layer2 (15x15) was completely outside [20..80)x[30..80), so its cropped image is fully transparent
        assertEquals(0, (layer2.getImage().getRGB(0, 0) >>> 24) & 0xFF);

        // Undo restores all original layer sizes, pixels, and properties
        controller.undo();
        assertEquals(100, layer0.getWidth());
        assertEquals(100, layer0.getHeight());
        assertEquals(50, layer1.getWidth());
        assertEquals(50, layer1.getHeight());
        assertEquals(15, layer2.getWidth());
        assertEquals(15, layer2.getHeight());
        assertEquals(Color.CYAN.getRGB(), layer2.getImage().getRGB(5, 5));
        assertEquals(0.65f, layer1.getOpacity(), 0.0001f);
        assertFalse(layer2.isVisible());

        // Redo re-applies exact crop across all layers
        controller.redo();
        assertEquals(60, layer0.getWidth());
        assertEquals(60, layer1.getWidth());
        assertEquals(60, layer2.getWidth());
        assertEquals(Color.BLUE.getRGB(), layer0.getImage().getRGB(10, 10));
        assertEquals(128, (layer1.getImage().getRGB(10, 10) >>> 24) & 0xFF);
    }

    // Scenario 12: Save the cropped document and reopen it
    @Test
    public void testSaveAndReopenCroppedDocument() throws Exception {
        EditorController controller = new EditorController();
        File testImage = new File("test-image.jpg");
        controller.loadImage(testImage);

        int origW = controller.getDocument().getCanvasWidth();
        int origH = controller.getDocument().getCanvasHeight();
        assertTrue(origW > 100 && origH > 100);

        Rectangle cropRect = new Rectangle(20, 30, 80, 60);
        int expectedPixel = controller.getDocument().getCompositeImage().getRGB(20 + 15, 30 + 25);

        controller.cropDocument(cropRect);

        File savedPng = File.createTempFile("cropped_doc_", ".png");
        savedPng.deleteOnExit();
        controller.saveImage(savedPng, "png");

        EditorController reopenController = new EditorController();
        reopenController.loadImage(savedPng);

        BufferedImage reopened = reopenController.getDocument().getCompositeImage();
        assertNotNull(reopened);
        assertEquals(80, reopened.getWidth());
        assertEquals(60, reopened.getHeight());
        assertEquals(expectedPixel, reopened.getRGB(15, 25));
    }

    // Section 8: Validation and error handling (empty doc, null selection, zero size, out-of-bounds, null layer image)
    @Test
    public void testValidationAndErrorHandling() {
        EditorController emptyController = new EditorController();
        assertThrows(IllegalStateException.class,
                () -> emptyController.cropDocument(new Rectangle(0, 0, 10, 10)));

        // Document with only a null-image layer
        EditorController nullOnlyController = new EditorController();
        nullOnlyController.addLayer(new Layer("Empty", null));
        assertThrows(IllegalStateException.class,
                () -> nullOnlyController.cropDocument(new Rectangle(0, 0, 10, 10)));

        // Valid document + null layer image alongside valid layer
        EditorController controller = new EditorController();
        Layer validLayer = new Layer("Valid", createQuadrantImage(100, 100));
        Layer nullImgLayer = new Layer("NullImg", null);
        controller.addLayer(validLayer);
        controller.addLayer(nullImgLayer);

        // Null selection
        assertThrows(IllegalArgumentException.class, () -> controller.cropDocument(null));
        // Zero width / zero height / negative size
        assertThrows(IllegalArgumentException.class, () -> controller.cropDocument(new Rectangle(10, 10, 0, 20)));
        assertThrows(IllegalArgumentException.class, () -> controller.cropDocument(new Rectangle(10, 10, 20, 0)));
        assertThrows(IllegalArgumentException.class, () -> controller.cropDocument(new Rectangle(10, 10, -5, 20)));
        // Negative x/y or extending outside document
        assertThrows(IllegalArgumentException.class, () -> controller.cropDocument(new Rectangle(-1, 0, 20, 20)));
        assertThrows(IllegalArgumentException.class, () -> controller.cropDocument(new Rectangle(0, -1, 20, 20)));
        assertThrows(IllegalArgumentException.class, () -> controller.cropDocument(new Rectangle(80, 10, 25, 20)));
        assertThrows(IllegalArgumentException.class, () -> controller.cropDocument(new Rectangle(10, 90, 20, 15)));

        // Valid crop with a null-image layer must succeed without crashing
        controller.cropDocument(new Rectangle(10, 10, 40, 30));
        assertEquals(40, validLayer.getWidth());
        assertEquals(30, validLayer.getHeight());
        assertNull(nullImgLayer.getImage());

        controller.undo();
        assertEquals(100, validLayer.getWidth());
        assertNull(nullImgLayer.getImage());

        // Zero-width / zero-height mouse clicks in CanvasPanel are rejected
        CanvasPanel canvas = new CanvasPanel();
        canvas.setSize(400, 300);
        canvas.setImage(createQuadrantImage(100, 100));
        canvas.setCropMode(true);
        Rectangle disp = canvas.getDisplayedImageBounds();
        simulateDrag(canvas, disp.x + 20, disp.y + 20, disp.x + 20, disp.y + 20);
        assertNull(canvas.getCropAreaInImageCoordinates());
        simulateDrag(canvas, disp.x + 20, disp.y + 20, disp.x + 50, disp.y + 20);
        assertNull(canvas.getCropAreaInImageCoordinates());
        simulateDrag(canvas, disp.x + 20, disp.y + 20, disp.x + 20, disp.y + 50);
        assertNull(canvas.getCropAreaInImageCoordinates());

        // Direct CropOperation and CropDocumentCommand validation
        assertThrows(IllegalArgumentException.class, () -> new CropOperation(null));
        assertThrows(IllegalArgumentException.class, () -> new CropOperation(new Rectangle(0, 0, 0, 10)));
        assertThrows(IllegalArgumentException.class, () -> new CropDocumentCommand(null, new Rectangle(0, 0, 10, 10)));
    }

    // Visual check: CanvasPanel renders darkened area outside the selection rectangle
    @Test
    public void testCanvasDarkensOutsideSelection() {
        BufferedImage whiteImg = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = whiteImg.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        CanvasPanel canvas = new CanvasPanel();
        canvas.setSize(400, 400);
        canvas.setImage(whiteImg);
        canvas.setCropMode(true);

        Rectangle disp = canvas.getDisplayedImageBounds();
        int selX1 = disp.x + disp.width / 4;
        int selY1 = disp.y + disp.height / 4;
        int selX2 = disp.x + (disp.width * 3) / 4;
        int selY2 = disp.y + (disp.height * 3) / 4;
        simulateDrag(canvas, selX1, selY1, selX2, selY2);

        BufferedImage rendered = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D rg = rendered.createGraphics();
        canvas.paint(rg);
        rg.dispose();

        Color inside = new Color(rendered.getRGB(disp.x + disp.width / 2, disp.y + disp.height / 2));
        Color outside = new Color(rendered.getRGB(disp.x + 5, disp.y + 5));
        assertTrue(inside.getRed() > 240, "Pixel inside selection should remain bright white");
        assertTrue(outside.getRed() < 150, "Pixel outside selection should be visibly darkened, got R=" + outside.getRed());
    }

    // UI integration check: MainFrame toolbar buttons and Operations -> Crop menu item
    @Test
    public void testMainFrameHasCropControlsAndExistingControls() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        MainFrame frame = new MainFrame();
        try {
            boolean foundCrop = false;
            boolean foundApplyCrop = false;
            boolean foundCancelCrop = false;
            boolean foundSave = false;
            boolean foundOpen = false;
            boolean foundUndo = false;
            boolean foundRedo = false;

            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JPanel panel) {
                    for (Component inner : panel.getComponents()) {
                        if (inner instanceof JButton btn) {
                            switch (btn.getText()) {
                                case "Crop" -> foundCrop = true;
                                case "Apply Crop" -> {
                                    foundApplyCrop = true;
                                    assertFalse(btn.isEnabled(), "Apply Crop should be initially disabled");
                                }
                                case "Cancel Crop" -> {
                                    foundCancelCrop = true;
                                    assertFalse(btn.isEnabled(), "Cancel Crop should be initially disabled");
                                }
                                case "Save Image" -> foundSave = true;
                                case "Open Image" -> foundOpen = true;
                                case "Undo" -> foundUndo = true;
                                case "Redo" -> foundRedo = true;
                                default -> {}
                            }
                        }
                    }
                }
            }

            assertTrue(foundCrop, "Crop button must exist in MainFrame toolbar");
            assertTrue(foundApplyCrop, "Apply Crop button must exist in MainFrame toolbar");
            assertTrue(foundCancelCrop, "Cancel Crop button must exist in MainFrame toolbar");
            assertTrue(foundSave && foundOpen && foundUndo && foundRedo, "Existing toolbar buttons must be preserved");

            // Check Operations -> Crop menu item
            JMenuBar menuBar = frame.getJMenuBar();
            assertNotNull(menuBar);
            boolean foundCropMenuItem = false;
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu != null && "Operations".equals(menu.getText())) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && "Crop".equals(item.getText())) {
                            foundCropMenuItem = true;
                        }
                    }
                }
            }
            assertTrue(foundCropMenuItem, "Operations menu must contain Crop item");
        } finally {
            frame.dispose();
        }
    }
}

