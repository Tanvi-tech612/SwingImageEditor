package com.imageeditor;

import com.imageeditor.controller.EditorController;
import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.BrightnessOperation;
import com.imageeditor.operations.GrayscaleOperation;
import com.imageeditor.operations.InvertOperation;
import com.imageeditor.operations.RotateOperation;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class LayerSystemIntegrationTest {

    @Test
    public void testAllFifteenScenarios() throws Exception {
        EditorController controller = new EditorController();
        ImageDocument document = controller.getDocument();

        // 1. Open an image and verify that it appears as the background layer.
        File testImageFile = new File("test-image.jpg");
        controller.loadImage(testImageFile);
        assertEquals(1, document.getLayerCount(), "Scenario 1: Should have 1 layer after opening image");
        Layer backgroundLayer = document.getLayer(0);
        assertNotNull(backgroundLayer);
        assertEquals("test-image.jpg", backgroundLayer.getName());
        assertSame(backgroundLayer, document.getSelectedLayer());

        // 2. Add a second layer.
        Layer layer2 = controller.addNewBlankLayer();
        assertEquals(2, document.getLayerCount(), "Scenario 2: Document should contain 2 layers");
        assertEquals("Layer 1", layer2.getName());
        assertSame(layer2, document.getSelectedLayer(), "Newly created layer must be selected");

        // 3. Add a third layer.
        Layer layer3 = controller.addNewBlankLayer();
        assertEquals(3, document.getLayerCount(), "Scenario 3: Document should contain 3 layers");
        assertEquals("Layer 2", layer3.getName());
        assertSame(layer3, document.getSelectedLayer(), "Third layer must be selected");

        // 4. Select different layers.
        controller.selectLayer(backgroundLayer);
        assertSame(backgroundLayer, document.getSelectedLayer(), "Scenario 4: Should select background layer");
        controller.selectLayer(layer2);
        assertSame(layer2, document.getSelectedLayer(), "Scenario 4: Should select layer 2");
        controller.selectLayer(layer3);
        assertSame(layer3, document.getSelectedLayer(), "Scenario 4: Should select layer 3");

        // 5. Hide and show layers.
        assertTrue(layer3.isVisible());
        controller.toggleLayerVisibility(layer3);
        assertFalse(layer3.isVisible(), "Scenario 5: Layer 3 should be hidden");
        controller.toggleLayerVisibility(layer3);
        assertTrue(layer3.isVisible(), "Scenario 5: Layer 3 should be visible again");

        // 6. Delete a layer.
        controller.selectLayer(layer3);
        controller.deleteSelectedLayer();
        assertEquals(2, document.getLayerCount(), "Scenario 6: Document should now have 2 layers");
        assertFalse(document.getLayers().contains(layer3));
        assertNotNull(document.getSelectedLayer(), "Scenario 6: A valid layer should remain selected");

        // Undo deletion to restore layer 3 for subsequent tests
        controller.undo();
        assertEquals(3, document.getLayerCount(), "Undo restored 3 layers");

        // 7. Move layers up and down.
        // Currently: [backgroundLayer (0), layer2 (1), layer3 (2)]
        controller.selectLayer(layer2);
        assertEquals(1, document.indexOf(layer2));
        controller.moveSelectedLayerDown();
        assertEquals(0, document.indexOf(layer2), "Scenario 7: layer2 moved down to index 0");
        assertEquals(1, document.indexOf(backgroundLayer));
        controller.moveSelectedLayerUp();
        assertEquals(1, document.indexOf(layer2), "Scenario 7: layer2 moved back up to index 1");

        // 8. Change opacity.
        controller.selectLayer(layer2);
        assertEquals(1.0f, layer2.getOpacity(), 0.0001f);
        controller.setLayerOpacity(layer2, 0.65f, 1.0f);
        assertEquals(0.65f, layer2.getOpacity(), 0.0001f, "Scenario 8: Opacity should be updated to 0.65");

        // 9. Undo each of these actions.
        // Undo opacity change
        assertTrue(controller.canUndo(), "Scenario 9: Should be able to undo opacity change");
        controller.undo();
        assertEquals(1.0f, layer2.getOpacity(), 0.0001f, "Scenario 9: Opacity restored to 1.0");

        // Undo move up
        assertTrue(controller.canUndo());
        controller.undo();
        assertEquals(0, document.indexOf(layer2), "Scenario 9: Move up undone");

        // Undo move down
        assertTrue(controller.canUndo());
        controller.undo();
        assertEquals(1, document.indexOf(layer2), "Scenario 9: Move down undone");

        // 10. Redo each of these actions.
        assertTrue(controller.canRedo(), "Scenario 10: Should be able to redo");
        controller.redo(); // Redo move down
        assertEquals(0, document.indexOf(layer2));
        controller.redo(); // Redo move up
        assertEquals(1, document.indexOf(layer2));
        controller.redo(); // Redo opacity change
        assertEquals(0.65f, layer2.getOpacity(), 0.0001f);

        // 11. Perform several consecutive operations and undo them in reverse order.
        controller.selectLayer(layer3);
        controller.toggleLayerVisibility(layer3); // op A: hide layer3
        controller.setLayerOpacity(layer3, 0.4f, 1.0f); // op B: opacity 0.4
        controller.moveSelectedLayerDown(); // op C: move layer3 down to index 1

        assertEquals(1, document.indexOf(layer3));
        assertEquals(0.4f, layer3.getOpacity(), 0.0001f);
        assertFalse(layer3.isVisible());

        // Undo in reverse order:
        controller.undo(); // undo C (move down)
        assertEquals(2, document.indexOf(layer3), "Scenario 11: Undo C restored position");

        controller.undo(); // undo B (opacity)
        assertEquals(1.0f, layer3.getOpacity(), 0.0001f, "Scenario 11: Undo B restored opacity");

        controller.undo(); // undo A (visibility)
        assertTrue(layer3.isVisible(), "Scenario 11: Undo A restored visibility");

        // 12. Perform an undo followed by a new operation and verify that redo history is cleared.
        controller.addNewBlankLayer(); // adds layer4
        assertTrue(controller.canUndo());
        controller.undo(); // undo adding layer4
        assertTrue(controller.canRedo(), "Redo available after undo");

        // Perform a new operation
        controller.addNewBlankLayer();
        assertFalse(controller.canRedo(), "Scenario 12: Redo stack must be cleared after a new operation");

        // 13. Verify that the final layer cannot be deleted.
        EditorController singleLayerController = new EditorController();
        Layer soleLayer = singleLayerController.addNewBlankLayer();
        assertEquals(1, singleLayerController.getDocument().getLayerCount());
        assertThrows(IllegalStateException.class, singleLayerController::deleteSelectedLayer,
                "Scenario 13: Must prevent deleting final remaining layer");
        assertEquals(1, singleLayerController.getDocument().getLayerCount(), "Final layer was not deleted");

        // 14. Verify that image-processing operations still work.
        BufferedImage testImg = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = testImg.createGraphics();
        g.setColor(new Color(100, 150, 200));
        g.fillRect(0, 0, 20, 20);
        g.dispose();

        Layer opLayer = new Layer("OpLayer", testImg);
        singleLayerController.addLayer(opLayer);
        singleLayerController.selectLayer(opLayer);

        // Test Grayscale
        singleLayerController.applyOperation(new GrayscaleOperation());
        Color grayPixel = new Color(opLayer.getImage().getRGB(5, 5));
        assertEquals(grayPixel.getRed(), grayPixel.getGreen(), "Scenario 14: Grayscale red == green");
        assertEquals(grayPixel.getGreen(), grayPixel.getBlue(), "Scenario 14: Grayscale green == blue");

        // Test Undo of Grayscale restores original colors
        singleLayerController.undo();
        Color restoredPixel = new Color(opLayer.getImage().getRGB(5, 5));
        assertEquals(100, restoredPixel.getRed());
        assertEquals(150, restoredPixel.getGreen());
        assertEquals(200, restoredPixel.getBlue());

        // Test Invert
        singleLayerController.applyOperation(new InvertOperation());
        Color invertPixel = new Color(opLayer.getImage().getRGB(5, 5));
        assertEquals(255 - 100, invertPixel.getRed());
        assertEquals(255 - 150, invertPixel.getGreen());
        assertEquals(255 - 200, invertPixel.getBlue());
        singleLayerController.undo();

        // Test Brightness
        singleLayerController.applyOperation(new BrightnessOperation(20));
        Color brightPixel = new Color(opLayer.getImage().getRGB(5, 5));
        assertEquals(120, brightPixel.getRed());
        assertEquals(170, brightPixel.getGreen());
        assertEquals(220, brightPixel.getBlue());
        singleLayerController.undo();

        // Test Rotate
        singleLayerController.applyOperation(new RotateOperation());
        assertEquals(20, opLayer.getImage().getWidth());
        assertEquals(20, opLayer.getImage().getHeight());
        singleLayerController.undo();

        // 15. Verify that the composite image renders correctly.
        ImageDocument compDoc = new ImageDocument();
        BufferedImage bgImg = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bgG = bgImg.createGraphics();
        bgG.setColor(Color.RED);
        bgG.fillRect(0, 0, 30, 30);
        bgG.dispose();
        Layer bottom = new Layer("Bottom", bgImg);
        compDoc.addLayer(bottom);

        BufferedImage fgImg = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        Graphics2D fgG = fgImg.createGraphics();
        fgG.setColor(Color.GREEN);
        fgG.fillRect(0, 0, 30, 30);
        fgG.dispose();
        Layer top = new Layer("Top", fgImg);
        compDoc.addLayer(top);

        BufferedImage composite = compDoc.getCompositeImage();
        assertNotNull(composite, "Scenario 15: Composite image should not be null");
        assertEquals(30, composite.getWidth());
        assertEquals(30, composite.getHeight());
        Color compPixel = new Color(composite.getRGB(15, 15));
        assertEquals(Color.GREEN.getRGB(), compPixel.getRGB(), "Scenario 15: Top layer (Green) obscures Bottom layer (Red)");

        // Hide top layer and verify red is visible
        top.setVisible(false);
        BufferedImage compHidden = compDoc.getCompositeImage();
        Color hiddenCompPixel = new Color(compHidden.getRGB(15, 15));
        assertEquals(Color.RED.getRGB(), hiddenCompPixel.getRGB(), "Scenario 15: Hidden top layer reveals red bottom layer");
    }
}
