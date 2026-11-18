package com.imageeditor;

import com.imageeditor.command.LayerResizeCommand;
import com.imageeditor.controller.EditorController;
import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.BrightnessOperation;
import com.imageeditor.operations.GrayscaleOperation;
import com.imageeditor.operations.InvertOperation;
import com.imageeditor.operations.ResizeOperation;
import com.imageeditor.operations.RotateOperation;
import com.imageeditor.ui.MainFrame;
import com.imageeditor.ui.ResizeLayerDialog;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class LayerResizeFeatureTest {

    private static BufferedImage createSolidImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }

    // Scenario 1: Resize a selected layer to smaller dimensions
    @Test
    public void testResizeSelectedLayerSmaller() {
        EditorController controller = new EditorController();
        BufferedImage original = createSolidImage(500, 400, new Color(40, 120, 220, 255));
        Layer layer = new Layer("Background", original);
        controller.addLayer(layer);

        AtomicInteger notifications = new AtomicInteger(0);
        controller.addChangeListener(notifications::incrementAndGet);

        controller.resizeSelectedLayer(250, 200);

        assertEquals(250, layer.getWidth());
        assertEquals(200, layer.getHeight());
        assertNotSame(original, layer.getImage(), "Original BufferedImage must not be mutated in place");
        assertEquals(500, original.getWidth(), "Original BufferedImage dimensions must remain intact");
        assertEquals(400, original.getHeight());

        Color pixel = new Color(layer.getImage().getRGB(125, 100), true);
        assertEquals(255, pixel.getAlpha());
        assertEquals(40, pixel.getRed(), 2);
        assertEquals(120, pixel.getGreen(), 2);
        assertEquals(220, pixel.getBlue(), 2);
        assertTrue(notifications.get() > 0, "UI change listeners must be notified");
    }

    // Scenario 2: Resize a selected layer to larger dimensions
    @Test
    public void testResizeSelectedLayerLarger() {
        EditorController controller = new EditorController();
        BufferedImage original = createSolidImage(100, 80, new Color(200, 80, 50, 255));
        Layer layer = new Layer("Photo", original);
        controller.addLayer(layer);

        controller.resizeSelectedLayer(320, 240);

        assertEquals(320, layer.getWidth());
        assertEquals(240, layer.getHeight());
        assertEquals(100, original.getWidth());
        assertEquals(80, original.getHeight());

        Color pixel = new Color(layer.getImage().getRGB(160, 120), true);
        assertEquals(200, pixel.getRed(), 2);
        assertEquals(80, pixel.getGreen(), 2);
        assertEquals(50, pixel.getBlue(), 2);
    }

    // Scenario 3 & 4: Maintain Aspect Ratio enabled vs. disabled in ResizeLayerDialog
    @Test
    public void testAspectRatioMaintenanceAndIndependentDimensions() {
        // Static proportional math checks
        assertEquals(200, ResizeLayerDialog.calculateProportionalHeight(250, 500, 400));
        assertEquals(125, ResizeLayerDialog.calculateProportionalWidth(100, 500, 400));
        assertEquals(1, ResizeLayerDialog.calculateProportionalHeight(1, 500, 100));

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        ResizeLayerDialog dialog = new ResizeLayerDialog(null, "Layer 1", 500, 400);
        try {
            // Displays current dimensions on open
            assertEquals("500", dialog.getWidthField().getText());
            assertEquals("400", dialog.getHeightField().getText());
            assertTrue(dialog.getMaintainAspectRatioCheckBox().isSelected());

            // Maintain Aspect Ratio ENABLED: changing width updates height proportionally
            dialog.getWidthField().setText("250");
            assertEquals("200", dialog.getHeightField().getText());

            // Maintain Aspect Ratio ENABLED: changing height updates width proportionally
            dialog.getHeightField().setText("100");
            assertEquals("125", dialog.getWidthField().getText());

            // Maintain Aspect Ratio DISABLED: allow independent width & height values
            dialog.getMaintainAspectRatioCheckBox().setSelected(false);
            dialog.getWidthField().setText("300");
            assertEquals("100", dialog.getHeightField().getText(),
                    "Height must remain unchanged when Maintain Aspect Ratio is disabled");

            dialog.getHeightField().setText("450");
            assertEquals("300", dialog.getWidthField().getText(),
                    "Width must remain unchanged when Maintain Aspect Ratio is disabled");
        } finally {
            dialog.dispose();
        }
    }

    // Scenario 5: Resize a transparent PNG layer and verify transparency is preserved
    @Test
    public void testResizeTransparentPngPreservesAlpha() throws Exception {
        BufferedImage transparentImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = transparentImg.createGraphics();
        // Left half: fully transparent (alpha 0); Right half: semi-transparent green (alpha 128)
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, 50, 100);
        g.setColor(new Color(0, 200, 50, 128));
        g.fillRect(50, 0, 50, 100);
        g.dispose();

        // Round-trip through an actual PNG file to simulate opening a transparent PNG layer
        File pngFile = File.createTempFile("transparent_layer_", ".png");
        pngFile.deleteOnExit();
        assertTrue(ImageIO.write(transparentImg, "png", pngFile));

        EditorController controller = new EditorController();
        Layer pngLayer = controller.addLayerFromImage(pngFile);

        controller.resizeSelectedLayer(60, 60);

        assertEquals(60, pngLayer.getWidth());
        assertEquals(60, pngLayer.getHeight());

        // Far-left pixel must remain fully transparent
        int leftAlpha = (pngLayer.getImage().getRGB(5, 30) >>> 24) & 0xFF;
        assertEquals(0, leftAlpha, "Fully transparent region must remain transparent after resize");

        // Far-right pixel must retain semi-transparency (~128) and green channel
        int rightArgb = pngLayer.getImage().getRGB(50, 30);
        int rightAlpha = (rightArgb >>> 24) & 0xFF;
        int rightGreen = (rightArgb >>> 8) & 0xFF;
        assertTrue(Math.abs(rightAlpha - 128) <= 3, "Semi-transparent alpha must be preserved, got " + rightAlpha);
        assertTrue(Math.abs(rightGreen - 200) <= 5, "Green channel must be preserved, got " + rightGreen);
    }

    // Scenario 6: Resize one layer in a multi-layer document; other layers & properties remain unchanged
    @Test
    public void testMultiLayerResizeAffectsOnlySelectedLayerAndPreservesProperties() {
        EditorController controller = new EditorController();
        ImageDocument doc = controller.getDocument();

        BufferedImage bgImg = createSolidImage(200, 200, Color.RED);
        Layer bgLayer = new Layer("Background", bgImg);
        bgLayer.setOpacity(0.9f);
        bgLayer.setVisible(true);

        BufferedImage midImg = createSolidImage(100, 100, Color.BLUE);
        Layer midLayer = new Layer("Middle", midImg);
        midLayer.setOpacity(0.75f);
        midLayer.setVisible(true);

        BufferedImage topImg = createSolidImage(80, 60, Color.GREEN);
        Layer topLayer = new Layer("TopHidden", topImg);
        topLayer.setOpacity(0.4f);
        topLayer.setVisible(false);

        controller.addLayer(bgLayer);
        controller.addLayer(midLayer);
        controller.addLayer(topLayer);
        controller.selectLayer(midLayer);

        // Resize ONLY the selected middle layer to 50x40
        controller.resizeSelectedLayer(50, 40);

        // Selected layer resized; properties and order unchanged
        assertEquals(50, midLayer.getWidth());
        assertEquals(40, midLayer.getHeight());
        assertEquals("Middle", midLayer.getName());
        assertEquals(0.75f, midLayer.getOpacity(), 0.0001f);
        assertTrue(midLayer.isVisible());
        assertSame(midLayer, doc.getSelectedLayer());
        assertEquals(1, doc.indexOf(midLayer));

        // Other layers completely unaffected (same BufferedImage objects and dimensions)
        assertSame(bgImg, bgLayer.getImage());
        assertEquals(200, bgLayer.getWidth());
        assertEquals(200, bgLayer.getHeight());
        assertEquals(0.9f, bgLayer.getOpacity(), 0.0001f);
        assertTrue(bgLayer.isVisible());
        assertEquals(0, doc.indexOf(bgLayer));

        assertSame(topImg, topLayer.getImage());
        assertEquals(80, topLayer.getWidth());
        assertEquals(60, topLayer.getHeight());
        assertEquals(0.4f, topLayer.getOpacity(), 0.0001f);
        assertFalse(topLayer.isVisible());
        assertEquals(2, doc.indexOf(topLayer));

        // Composite image retains 200x200 canvas, shows blended blue at top-left (10,10) and pure red outside resized midLayer at (80,80)
        BufferedImage composite = doc.getCompositeImage();
        assertEquals(200, composite.getWidth());
        assertEquals(200, composite.getHeight());

        Color insideResized = new Color(composite.getRGB(10, 10), true);
        assertTrue(insideResized.getBlue() > 150, "Resized blue middle layer should render at top-left (10, 10)");

        Color outsideResized = new Color(composite.getRGB(80, 80), true);
        assertTrue(outsideResized.getRed() > 200 && outsideResized.getBlue() < 10,
                "Area outside resized middle layer (80, 80) should reveal red background layer");
    }

    // Scenario 7: Undo and redo resizing multiple times
    @Test
    public void testUndoRedoResizingMultipleTimes() {
        EditorController controller = new EditorController();
        BufferedImage original = createSolidImage(500, 400, Color.ORANGE);
        Layer layer = new Layer("Layer A", original);
        controller.addLayer(layer);

        // First resize: 500x400 -> 250x200
        controller.resizeSelectedLayer(250, 200);
        BufferedImage resized1 = layer.getImage();
        assertEquals(250, layer.getWidth());
        assertEquals(200, layer.getHeight());

        // Undo -> 500x400
        assertTrue(controller.canUndo());
        controller.undo();
        assertSame(original, layer.getImage());
        assertEquals(500, layer.getWidth());
        assertEquals(400, layer.getHeight());

        // Redo -> 250x200 (reuses already-created resized image)
        assertTrue(controller.canRedo());
        controller.redo();
        assertSame(resized1, layer.getImage());
        assertEquals(250, layer.getWidth());
        assertEquals(200, layer.getHeight());

        // Second resize: 250x200 -> 600x300
        controller.resizeSelectedLayer(600, 300);
        BufferedImage resized2 = layer.getImage();
        assertEquals(600, layer.getWidth());
        assertEquals(300, layer.getHeight());

        // Undo twice back to 500x400
        controller.undo();
        assertSame(resized1, layer.getImage());
        assertEquals(250, layer.getWidth());
        assertEquals(200, layer.getHeight());

        controller.undo();
        assertSame(original, layer.getImage());
        assertEquals(500, layer.getWidth());
        assertEquals(400, layer.getHeight());

        // Redo twice back to 600x300
        controller.redo();
        assertSame(resized1, layer.getImage());
        controller.redo();
        assertSame(resized2, layer.getImage());
        assertEquals(600, layer.getWidth());
        assertEquals(300, layer.getHeight());

        // Direct LayerResizeCommand execute/undo/redo check
        Layer standalone = new Layer("Direct", createSolidImage(80, 60, Color.CYAN));
        LayerResizeCommand cmd = new LayerResizeCommand(standalone, 40, 30);
        cmd.execute();
        BufferedImage cached = cmd.getResizedImage();
        assertNotNull(cached);
        assertEquals(40, standalone.getWidth());
        cmd.undo();
        assertEquals(80, standalone.getWidth());
        cmd.redo();
        assertSame(cached, standalone.getImage());
        assertEquals(40, standalone.getWidth());
    }

    // Scenario 8: Invalid dimensions & error handling
    @Test
    public void testInvalidDimensionsAndErrorHandling() {
        // No layer selected
        EditorController emptyController = new EditorController();
        assertThrows(IllegalStateException.class, () -> emptyController.resizeSelectedLayer(100, 100));

        // Selected layer with null image
        EditorController nullImgController = new EditorController();
        nullImgController.addLayer(new Layer("EmptyLayer", null));
        assertThrows(IllegalStateException.class, () -> nullImgController.resizeSelectedLayer(100, 100));

        // Controller & ResizeOperation dimension checks
        EditorController controller = new EditorController();
        controller.addLayer(new Layer("Valid", createSolidImage(100, 100, Color.WHITE)));

        assertThrows(IllegalArgumentException.class, () -> controller.resizeSelectedLayer(0, 100));
        assertThrows(IllegalArgumentException.class, () -> controller.resizeSelectedLayer(100, 0));
        assertThrows(IllegalArgumentException.class, () -> controller.resizeSelectedLayer(-50, 100));
        assertThrows(IllegalArgumentException.class, () -> controller.resizeSelectedLayer(100, -10));
        assertThrows(IllegalArgumentException.class, () -> controller.resizeSelectedLayer(ResizeOperation.MAX_DIMENSION + 1, 100));
        assertThrows(IllegalArgumentException.class, () -> controller.resizeSelectedLayer(100, ResizeOperation.MAX_DIMENSION + 1));
        assertThrows(IllegalArgumentException.class, () -> controller.resizeSelectedLayer(9000, 9000)); // 81M pixels > 50M max

        // Dialog parser validation checks
        assertThrows(IllegalArgumentException.class, () -> ResizeLayerDialog.parseAndValidate("", "100"));
        assertThrows(IllegalArgumentException.class, () -> ResizeLayerDialog.parseAndValidate("100", "   "));
        assertThrows(IllegalArgumentException.class, () -> ResizeLayerDialog.parseAndValidate("abc", "100"));
        assertThrows(IllegalArgumentException.class, () -> ResizeLayerDialog.parseAndValidate("100", "12.5"));
        assertThrows(IllegalArgumentException.class, () -> ResizeLayerDialog.parseAndValidate("0", "100"));
        assertThrows(IllegalArgumentException.class, () -> ResizeLayerDialog.parseAndValidate("-5", "100"));
        assertThrows(IllegalArgumentException.class, () -> ResizeLayerDialog.parseAndValidate("999999999999", "100"));
        assertThrows(IllegalArgumentException.class, () -> ResizeLayerDialog.parseAndValidate("9000", "9000"));

        Dimension ok = ResizeLayerDialog.parseAndValidate(" 250 ", " 200 ");
        assertEquals(new Dimension(250, 200), ok);

        // Command constructor checks
        assertThrows(IllegalArgumentException.class, () -> new LayerResizeCommand(null, 100, 100));
        assertThrows(IllegalStateException.class, () -> new LayerResizeCommand(new Layer("Null", null), 100, 100));
    }

    // Scenario 9: Existing Crop, Grayscale, Brightness, Invert, Rotate & UI controls still work
    @Test
    public void testExistingOperationsAndUiIntegrationRemainIntact() {
        EditorController controller = new EditorController();
        BufferedImage img = createSolidImage(100, 80, new Color(100, 150, 200, 255));
        Layer layer = new Layer("Main", img);
        controller.addLayer(layer);

        // Resize -> Grayscale -> Brightness -> Invert -> Rotate -> Crop
        controller.resizeSelectedLayer(80, 60);
        assertEquals(80, layer.getWidth());
        assertEquals(60, layer.getHeight());

        controller.applyOperation(new GrayscaleOperation());
        Color gray = new Color(layer.getImage().getRGB(10, 10));
        assertEquals(gray.getRed(), gray.getGreen());
        assertEquals(gray.getGreen(), gray.getBlue());

        controller.applyOperation(new BrightnessOperation(20));
        controller.applyOperation(new InvertOperation());

        controller.applyOperation(new RotateOperation());
        assertEquals(60, layer.getWidth());
        assertEquals(80, layer.getHeight());

        controller.cropDocument(new Rectangle(10, 10, 30, 40));
        assertEquals(30, layer.getWidth());
        assertEquals(40, layer.getHeight());

        // Undo all 6 operations back to original 100x80
        for (int i = 0; i < 6; i++) {
            assertTrue(controller.canUndo());
            controller.undo();
        }
        assertEquals(100, layer.getWidth());
        assertEquals(80, layer.getHeight());

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        // Verify MainFrame has Resize Layer toolbar button and Operations -> Resize Layer... menu item
        MainFrame frame = new MainFrame();
        try {
            boolean foundResizeButton = false;
            boolean foundCropButton = false;
            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JPanel panel) {
                    for (Component inner : panel.getComponents()) {
                        if (inner instanceof JButton btn) {
                            if ("Resize Layer".equals(btn.getText())) foundResizeButton = true;
                            if ("Crop".equals(btn.getText())) foundCropButton = true;
                        }
                    }
                }
            }
            assertTrue(foundResizeButton, "Resize Layer button must exist in MainFrame toolbar");
            assertTrue(foundCropButton, "Existing Crop button must remain in MainFrame toolbar");

            boolean foundResizeMenu = false;
            JMenuBar menuBar = frame.getJMenuBar();
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                JMenu menu = menuBar.getMenu(i);
                if (menu != null && "Operations".equals(menu.getText())) {
                    for (int j = 0; j < menu.getItemCount(); j++) {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && "Resize Layer...".equals(item.getText())) {
                            foundResizeMenu = true;
                        }
                    }
                }
            }
            assertTrue(foundResizeMenu, "Operations menu must contain Resize Layer... item");
        } finally {
            frame.dispose();
        }
    }
}

