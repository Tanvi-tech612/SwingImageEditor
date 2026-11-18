package com.imageeditor;

import com.imageeditor.command.MoveLayerCommand;
import com.imageeditor.controller.EditorController;
import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.BrightnessOperation;
import com.imageeditor.operations.GrayscaleOperation;
import com.imageeditor.operations.InvertOperation;
import com.imageeditor.operations.RotateOperation;
import com.imageeditor.ui.CanvasPanel;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class MoveLayerFeatureTest {

    private static BufferedImage createSolidImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }

    private static void dispatchMousePress(CanvasPanel panel, int x, int y) {
        MouseEvent press = new MouseEvent(
                panel, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1
        );
        for (MouseListener ml : panel.getMouseListeners()) {
            ml.mousePressed(press);
        }
    }

    private static void dispatchMouseDrag(CanvasPanel panel, int x, int y) {
        MouseEvent drag = new MouseEvent(
                panel, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, x, y, 0, false, MouseEvent.NOBUTTON
        );
        for (MouseMotionListener mml : panel.getMouseMotionListeners()) {
            mml.mouseDragged(drag);
        }
    }

    private static void dispatchMouseRelease(CanvasPanel panel, int x, int y) {
        MouseEvent release = new MouseEvent(
                panel, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                0, x, y, 1, false, MouseEvent.BUTTON1
        );
        for (MouseListener ml : panel.getMouseListeners()) {
            ml.mouseReleased(release);
        }
    }

    private static void dispatchMouseMove(CanvasPanel panel, int x, int y) {
        MouseEvent move = new MouseEvent(
                panel, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                0, x, y, 0, false, MouseEvent.NOBUTTON
        );
        for (MouseMotionListener mml : panel.getMouseMotionListeners()) {
            mml.mouseMoved(move);
        }
    }

    private static Point docToScreen(CanvasPanel panel, double docX, double docY) {
        Rectangle bounds = panel.getDisplayedImageBounds();
        double scale = panel.getCurrentDisplayScale();
        int sx = bounds.x + (int) Math.round(docX * scale);
        int sy = bounds.y + (int) Math.round(docY * scale);
        return new Point(sx, sy);
    }

    // Scenario 1: Move top layer right, down, left, and up with mouse dragging & scaling conversion
    @Test
    public void testDragTopLayerInAllFourDirections() {
        EditorController controller = new EditorController();
        Layer bg = new Layer("Background", createSolidImage(400, 300, Color.RED));
        Layer top = new Layer("Top", createSolidImage(80, 60, Color.BLUE));
        controller.addLayer(bg);
        controller.addLayer(top);
        controller.selectLayer(top);

        CanvasPanel canvas = new CanvasPanel();
        canvas.setController(controller);
        canvas.setSize(600, 450);

        // Hover over top layer -> MOVE_CURSOR; hover outside -> DEFAULT_CURSOR
        Point overTop = docToScreen(canvas, 40, 30);
        dispatchMouseMove(canvas, overTop.x, overTop.y);
        assertEquals(Cursor.MOVE_CURSOR, canvas.getCursor().getType());

        Point outsideTop = docToScreen(canvas, 250, 200);
        dispatchMouseMove(canvas, outsideTop.x, outsideTop.y);
        assertEquals(Cursor.DEFAULT_CURSOR, canvas.getCursor().getType());

        // Drag RIGHT (+100) and DOWN (+80) with multiple intermediate drag steps
        Point start = docToScreen(canvas, 40, 30);
        Point mid = docToScreen(canvas, 90, 70);
        Point end = docToScreen(canvas, 140, 110);

        dispatchMousePress(canvas, start.x, start.y);
        assertTrue(canvas.isLayerDragging());

        dispatchMouseDrag(canvas, mid.x, mid.y);
        assertEquals(50, top.getX(), 2);
        assertEquals(40, top.getY(), 2);

        dispatchMouseDrag(canvas, end.x, end.y);
        dispatchMouseRelease(canvas, end.x, end.y);
        assertFalse(canvas.isLayerDragging());

        assertEquals(100, top.getX(), 2);
        assertEquals(80, top.getY(), 2);
        assertEquals(0, bg.getX());
        assertEquals(0, bg.getY());

        // Verify composite renders blue at new position and red at old (0,0)
        BufferedImage composite = controller.getDocument().getCompositeImage();
        assertEquals(Color.RED.getRGB(), composite.getRGB(10, 10));
        assertEquals(Color.BLUE.getRGB(), composite.getRGB(top.getX() + 10, top.getY() + 10));

        // Now drag LEFT (-60) and UP (-50) after resizing canvas window
        canvas.setSize(900, 650);
        int beforeX = top.getX();
        int beforeY = top.getY();
        Point start2 = docToScreen(canvas, beforeX + 20, beforeY + 20);
        Point end2 = docToScreen(canvas, beforeX + 20 - 60, beforeY + 20 - 50);

        dispatchMousePress(canvas, start2.x, start2.y);
        dispatchMouseDrag(canvas, end2.x, end2.y);
        dispatchMouseRelease(canvas, end2.x, end2.y);

        assertEquals(beforeX - 60, top.getX(), 2);
        assertEquals(beforeY - 50, top.getY(), 2);
    }

    // Scenario 2: Move a layer to the center & verify newly imported layers default to centered
    @Test
    public void testMoveLayerToCenterAndImportedLayerCenteredByDefault() throws Exception {
        EditorController controller = new EditorController();
        controller.addLayer(new Layer("Background", createSolidImage(400, 300, Color.DARK_GRAY)));

        // Import a 100x60 image as a new layer -> should default to centered: (150, 120)
        File layerFile = File.createTempFile("imported_layer_", ".png");
        layerFile.deleteOnExit();
        assertTrue(ImageIO.write(createSolidImage(100, 60, Color.GREEN), "png", layerFile));

        Layer imported = controller.addLayerFromImage(layerFile);
        assertEquals(150, imported.getX(), "Imported layer should default to centered X");
        assertEquals(120, imported.getY(), "Imported layer should default to centered Y");

        // Move another layer explicitly to the center of the canvas
        Layer box = new Layer("Box", createSolidImage(40, 40, Color.YELLOW));
        controller.addLayer(box);
        controller.selectLayer(box);
        controller.moveSelectedLayer(180, 130);

        assertEquals(180, box.getX());
        assertEquals(130, box.getY());
        BufferedImage composite = controller.getDocument().getCompositeImage();
        assertEquals(Color.YELLOW.getRGB(), composite.getRGB(200, 150));
    }

    // Scenario 3: Move a layer partially and completely outside canvas boundaries
    @Test
    public void testMoveLayerOutsideCanvasBoundaries() {
        EditorController controller = new EditorController();
        Layer bg = new Layer("Background", createSolidImage(200, 200, Color.WHITE));
        BufferedImage origOverlay = createSolidImage(60, 60, Color.MAGENTA);
        Layer overlay = new Layer("Overlay", origOverlay);
        controller.addLayer(bg);
        controller.addLayer(overlay);
        controller.selectLayer(overlay);

        // Move partially outside top-left (-30, -30)
        controller.moveSelectedLayer(-30, -30);
        assertEquals(-30, overlay.getX());
        assertEquals(-30, overlay.getY());
        assertEquals(60, overlay.getWidth(), "Moving outside canvas must not crop/alter layer dimensions");
        assertEquals(60, overlay.getHeight());
        assertSame(origOverlay, overlay.getImage());

        BufferedImage compPartial = controller.getDocument().getCompositeImage();
        assertEquals(200, compPartial.getWidth());
        assertEquals(200, compPartial.getHeight());
        assertEquals(Color.MAGENTA.getRGB(), compPartial.getRGB(10, 10));
        assertEquals(Color.WHITE.getRGB(), compPartial.getRGB(40, 40));

        // Move completely outside bottom-right (300, 300)
        controller.moveSelectedLayer(300, 300);
        assertEquals(300, overlay.getX());
        assertEquals(300, overlay.getY());

        BufferedImage compOutside = controller.getDocument().getCompositeImage();
        assertEquals(Color.WHITE.getRGB(), compOutside.getRGB(10, 10));
        assertEquals(Color.WHITE.getRGB(), compOutside.getRGB(190, 190));

        // Render CanvasPanel with layer outside canvas (must not throw)
        CanvasPanel canvas = new CanvasPanel();
        canvas.setController(controller);
        canvas.setSize(400, 400);
        BufferedImage offscreen = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = offscreen.createGraphics();
        canvas.paint(g);
        g.dispose();

        // Undo brings it back to (-30, -30) and then (0, 0) with pixels intact
        controller.undo();
        assertEquals(-30, overlay.getX());
        assertEquals(-30, overlay.getY());
        controller.undo();
        assertEquals(0, overlay.getX());
        assertEquals(0, overlay.getY());
        assertEquals(Color.MAGENTA.getRGB(), overlay.getImage().getRGB(30, 30));
    }

    // Scenario 4: Move a transparent PNG layer
    @Test
    public void testMoveTransparentPngLayer() throws Exception {
        BufferedImage pngImg = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = pngImg.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, 60, 60);
        g.setColor(new Color(0, 255, 0, 128));
        g.fillRect(20, 20, 20, 20);
        g.dispose();

        File pngFile = File.createTempFile("movable_transparent_", ".png");
        pngFile.deleteOnExit();
        assertTrue(ImageIO.write(pngImg, "png", pngFile));

        EditorController controller = new EditorController();
        controller.addLayer(new Layer("Background", createSolidImage(200, 200, Color.BLACK)));
        Layer pngLayer = controller.addLayerFromImage(pngFile);

        controller.moveSelectedLayer(100, 50);
        assertEquals(100, pngLayer.getX());
        assertEquals(50, pngLayer.getY());

        BufferedImage comp = controller.getDocument().getCompositeImage();
        // Transparent outer border of pngLayer at (105, 55) shows pure black background
        assertEquals(Color.BLACK.getRGB(), comp.getRGB(105, 55));
        // Semi-transparent green center of pngLayer at (130, 80) blends over black -> ~128 green
        Color blended = new Color(comp.getRGB(130, 80));
        assertTrue(blended.getGreen() > 120 && blended.getGreen() < 136,
                "Expected semi-transparent green blended over black, got G=" + blended.getGreen());
    }

    // Scenario 5: Move a resized layer (and resize a moved layer)
    @Test
    public void testMoveResizedLayerAndResizeMovedLayer() {
        EditorController controller = new EditorController();
        controller.addLayer(new Layer("Background", createSolidImage(300, 300, Color.GRAY)));
        Layer layer = new Layer("Overlay", createSolidImage(100, 100, Color.CYAN));
        controller.addLayer(layer);
        controller.selectLayer(layer);

        // Resize first, then move
        controller.resizeSelectedLayer(50, 40);
        assertEquals(50, layer.getWidth());
        assertEquals(40, layer.getHeight());

        controller.moveSelectedLayer(120, 90);
        assertEquals(120, layer.getX());
        assertEquals(90, layer.getY());
        assertEquals(50, layer.getWidth());
        assertEquals(40, layer.getHeight());

        // Resize again after moving -> position must remain (120, 90)
        controller.resizeSelectedLayer(80, 60);
        assertEquals(120, layer.getX());
        assertEquals(90, layer.getY());
        assertEquals(80, layer.getWidth());
        assertEquals(60, layer.getHeight());
    }

    // Scenario 6: Move one layer while keeping all other layers stationary and stacking order intact
    @Test
    public void testMoveOneLayerKeepsOthersStationaryAndPreservesOrder() {
        EditorController controller = new EditorController();
        ImageDocument doc = controller.getDocument();

        Layer l1 = new Layer("Layer 1", createSolidImage(300, 300, Color.RED));
        Layer l2 = new Layer("Layer 2", createSolidImage(80, 80, Color.GREEN));
        l2.setPosition(50, 50);
        Layer l3 = new Layer("Layer 3", createSolidImage(60, 60, Color.BLUE));
        l3.setPosition(180, 160);

        controller.addLayer(l1);
        controller.addLayer(l2);
        controller.addLayer(l3);
        controller.selectLayer(l2);

        CanvasPanel canvas = new CanvasPanel();
        canvas.setController(controller);
        canvas.setSize(400, 400);

        // Click OUTSIDE l2 (e.g., on l3 at 200, 180) -> must NOT drag l2 or l3
        Point onL3 = docToScreen(canvas, 200, 180);
        Point dragTarget = docToScreen(canvas, 230, 210);
        dispatchMousePress(canvas, onL3.x, onL3.y);
        assertFalse(canvas.isLayerDragging());
        dispatchMouseDrag(canvas, dragTarget.x, dragTarget.y);
        dispatchMouseRelease(canvas, dragTarget.x, dragTarget.y);

        assertEquals(50, l2.getX());
        assertEquals(50, l2.getY());
        assertEquals(180, l3.getX());
        assertEquals(160, l3.getY());

        // Now drag l2 directly
        Point onL2 = docToScreen(canvas, 80, 80);
        Point moveL2To = docToScreen(canvas, 130, 140);
        dispatchMousePress(canvas, onL2.x, onL2.y);
        dispatchMouseDrag(canvas, moveL2To.x, moveL2To.y);
        dispatchMouseRelease(canvas, moveL2To.x, moveL2To.y);

        assertEquals(100, l2.getX(), 2);
        assertEquals(110, l2.getY(), 2);
        // l1 and l3 untouched, order preserved
        assertEquals(0, l1.getX());
        assertEquals(0, l1.getY());
        assertEquals(180, l3.getX());
        assertEquals(160, l3.getY());
        assertEquals(0, doc.indexOf(l1));
        assertEquals(1, doc.indexOf(l2));
        assertEquals(2, doc.indexOf(l3));
    }

    // Scenarios 7 & 8: Single command per drag, undo/redo, and multiple sequential moves
    @Test
    public void testMoveLayerUndoRedoAndMultipleMovesHistory() {
        EditorController controller = new EditorController();
        controller.addLayer(new Layer("Background", createSolidImage(400, 300, Color.WHITE)));
        Layer layer = new Layer("Movable", createSolidImage(80, 80, Color.ORANGE));
        controller.addLayer(layer);
        controller.selectLayer(layer);
        controller.getCommandHistory().clear();

        CanvasPanel canvas = new CanvasPanel();
        canvas.setController(controller);
        canvas.setSize(500, 400);

        // Move 1: multi-step drag (should register ONLY ONE command!)
        Point p0 = docToScreen(canvas, 40, 40);
        Point pMid1 = docToScreen(canvas, 60, 50);
        Point pMid2 = docToScreen(canvas, 80, 60);
        Point pEnd = docToScreen(canvas, 100, 70);

        dispatchMousePress(canvas, p0.x, p0.y);
        dispatchMouseDrag(canvas, pMid1.x, pMid1.y);
        dispatchMouseDrag(canvas, pMid2.x, pMid2.y);
        dispatchMouseDrag(canvas, pEnd.x, pEnd.y);
        dispatchMouseRelease(canvas, pEnd.x, pEnd.y);

        int pos1X = layer.getX();
        int pos1Y = layer.getY();
        assertEquals(60, pos1X, 2);
        assertEquals(30, pos1Y, 2);

        // A single undo must revert the entire multi-step drag straight back to (0, 0)
        assertTrue(controller.canUndo());
        controller.undo();
        assertEquals(0, layer.getX());
        assertEquals(0, layer.getY());
        assertFalse(controller.canUndo(), "Multi-step drag must create only ONE command in history");

        // Redo restores pos1
        assertTrue(controller.canRedo());
        controller.redo();
        assertEquals(pos1X, layer.getX());
        assertEquals(pos1Y, layer.getY());

        // Perform two more sequential moves
        controller.moveSelectedLayer(150, 100);
        controller.moveSelectedLayer(220, 170);

        assertEquals(220, layer.getX());
        assertEquals(170, layer.getY());

        controller.undo();
        assertEquals(150, layer.getX());
        assertEquals(100, layer.getY());

        controller.undo();
        assertEquals(pos1X, layer.getX());
        assertEquals(pos1Y, layer.getY());

        controller.undo();
        assertEquals(0, layer.getX());
        assertEquals(0, layer.getY());

        controller.redo();
        assertEquals(pos1X, layer.getX());
        assertEquals(pos1Y, layer.getY());

        controller.redo();
        assertEquals(150, layer.getX());
        assertEquals(100, layer.getY());

        // Direct MoveLayerCommand execute / undo / redo verification
        Layer direct = new Layer("Direct", createSolidImage(20, 20, Color.PINK));
        MoveLayerCommand cmd = new MoveLayerCommand(direct, 0, 0, 45, 75);
        cmd.execute();
        assertEquals(45, direct.getX());
        assertEquals(75, direct.getY());
        cmd.undo();
        assertEquals(0, direct.getX());
        assertEquals(0, direct.getY());
        cmd.redo();
        assertEquals(45, direct.getX());
        assertEquals(75, direct.getY());
    }

    // Scenario 9: Compatibility with Crop (including cropping moved layers!), Grayscale, Brightness, Invert, Rotate, Resize
    @Test
    public void testCompatibilityWithCropResizeAndImageOperations() {
        EditorController controller = new EditorController();
        Layer bg = new Layer("Background", createSolidImage(200, 200, Color.RED));
        Layer top = new Layer("Top", createSolidImage(60, 60, new Color(100, 150, 200, 255)));
        controller.addLayer(bg);
        controller.addLayer(top);
        controller.selectLayer(top);

        // Move top layer to (100, 80)
        controller.moveSelectedLayer(100, 80);

        // Crop mode on CanvasPanel must NOT drag the layer
        CanvasPanel canvas = new CanvasPanel();
        canvas.setController(controller);
        canvas.setSize(400, 400);
        canvas.setCropMode(true);
        Point cropStart = docToScreen(canvas, 110, 90);
        Point cropEnd = docToScreen(canvas, 150, 130);
        dispatchMousePress(canvas, cropStart.x, cropStart.y);
        dispatchMouseDrag(canvas, cropEnd.x, cropEnd.y);
        dispatchMouseRelease(canvas, cropEnd.x, cropEnd.y);
        assertEquals(100, top.getX(), "Crop mode drag must not move the layer");
        assertEquals(80, top.getY());
        assertNotNull(canvas.getCropAreaInImageCoordinates());
        canvas.setCropMode(false);

        // Image operations preserve moved layer position
        controller.applyOperation(new GrayscaleOperation());
        assertEquals(100, top.getX());
        assertEquals(80, top.getY());
        controller.undo();

        controller.applyOperation(new BrightnessOperation(25));
        assertEquals(100, top.getX());
        assertEquals(80, top.getY());
        controller.undo();

        controller.applyOperation(new InvertOperation());
        assertEquals(100, top.getX());
        assertEquals(80, top.getY());
        controller.undo();

        controller.applyOperation(new RotateOperation());
        assertEquals(100, top.getX());
        assertEquals(80, top.getY());
        controller.undo();

        controller.resizeSelectedLayer(50, 50);
        assertEquals(100, top.getX());
        assertEquals(80, top.getY());
        assertEquals(50, top.getWidth());

        // Crop document [50..150) x [50..150) containing moved top layer at (100, 80) size 50x50
        // In cropped 100x100 document, top layer pixels sit at (100-50, 80-50) = (50, 30)
        controller.cropDocument(new Rectangle(50, 50, 100, 100));
        assertEquals(100, bg.getWidth());
        assertEquals(100, top.getWidth());
        assertEquals(0, top.getX());
        assertEquals(0, top.getY());

        // Pixel at (10, 10) on cropped top layer is transparent (outside original moved bounds)
        assertEquals(0, (top.getImage().getRGB(10, 10) >>> 24) & 0xFF);
        // Pixel at (60, 40) on cropped top layer has top layer's color!
        Color croppedTopPixel = new Color(top.getImage().getRGB(60, 40), true);
        assertEquals(255, croppedTopPixel.getAlpha());
        assertEquals(100, croppedTopPixel.getRed(), 2);
        assertEquals(150, croppedTopPixel.getGreen(), 2);
        assertEquals(200, croppedTopPixel.getBlue(), 2);

        // Undo Crop restores BOTH original image dimensions AND moved position (100, 80)
        controller.undo();
        assertEquals(200, bg.getWidth());
        assertEquals(50, top.getWidth());
        assertEquals(100, top.getX());
        assertEquals(80, top.getY());
    }
}
