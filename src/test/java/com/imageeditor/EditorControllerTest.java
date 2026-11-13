package com.imageeditor;

import com.imageeditor.controller.EditorController;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.InvertOperation;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class EditorControllerTest {

    @Test
    public void testControllerInitialization() {
        EditorController controller = new EditorController();
        assertNotNull(controller.getDocument());
        assertNotNull(controller.getCommandHistory());
        assertFalse(controller.canUndo());
        assertFalse(controller.canRedo());
    }

    @Test
    public void testAddAndSelectLayer() {
        EditorController controller = new EditorController();
        Layer l1 = controller.addNewBlankLayer();
        assertNotNull(l1);
        assertEquals(1, controller.getDocument().getLayerCount());
        assertSame(l1, controller.getDocument().getSelectedLayer());
        assertTrue(controller.canUndo());

        Layer l2 = controller.addNewBlankLayer();
        assertEquals(2, controller.getDocument().getLayerCount());
        assertSame(l2, controller.getDocument().getSelectedLayer());

        controller.selectLayer(l1);
        assertSame(l1, controller.getDocument().getSelectedLayer());
    }

    @Test
    public void testDeleteSelectedLayer() {
        EditorController controller = new EditorController();
        Layer l1 = controller.addNewBlankLayer();

        // Attempting to delete when only 1 layer exists throws IllegalStateException
        assertThrows(IllegalStateException.class, controller::deleteSelectedLayer);

        Layer l2 = controller.addNewBlankLayer();
        assertEquals(2, controller.getDocument().getLayerCount());
        assertSame(l2, controller.getDocument().getSelectedLayer());

        // Deleting l2
        controller.deleteSelectedLayer();
        assertEquals(1, controller.getDocument().getLayerCount());
        assertSame(l1, controller.getDocument().getSelectedLayer());

        // Undo deletion
        controller.undo();
        assertEquals(2, controller.getDocument().getLayerCount());
        assertSame(l2, controller.getDocument().getSelectedLayer());
    }

    @Test
    public void testMoveLayers() {
        EditorController controller = new EditorController();
        Layer l1 = controller.addNewBlankLayer(); // index 0
        Layer l2 = controller.addNewBlankLayer(); // index 1

        assertSame(l2, controller.getDocument().getSelectedLayer());
        // l2 is at top (index 1), can move down
        controller.moveSelectedLayerDown();
        assertEquals(0, controller.getDocument().indexOf(l2));
        assertEquals(1, controller.getDocument().indexOf(l1));

        // Undo move
        controller.undo();
        assertEquals(1, controller.getDocument().indexOf(l2));
        assertEquals(0, controller.getDocument().indexOf(l1));
    }

    @Test
    public void testVisibilityAndOpacity() {
        EditorController controller = new EditorController();
        Layer l1 = controller.addNewBlankLayer();

        assertTrue(l1.isVisible());
        controller.toggleLayerVisibility(l1);
        assertFalse(l1.isVisible());

        controller.undo();
        assertTrue(l1.isVisible());

        controller.setLayerOpacity(l1, 0.3f, 1.0f);
        assertEquals(0.3f, l1.getOpacity(), 0.001f);

        controller.undo();
        assertEquals(1.0f, l1.getOpacity(), 0.001f);
    }

    @Test
    public void testApplyOperationAppliesToSelectedLayer() {
        EditorController controller = new EditorController();

        // Create 2 layers with distinct colors
        BufferedImage img1 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = img1.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, 10, 10);
        g1.dispose();
        Layer l1 = new Layer("Layer 1", img1);
        controller.addLayer(l1);

        BufferedImage img2 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 10, 10);
        g2.dispose();
        Layer l2 = new Layer("Layer 2", img2);
        controller.addLayer(l2);

        // Select Layer 1
        controller.selectLayer(l1);

        // Apply Invert to Layer 1 (White inverted becomes Black)
        controller.applyOperation(new InvertOperation());

        Color l1Pixel = new Color(l1.getImage().getRGB(5, 5));
        assertEquals(0, l1Pixel.getRed());
        assertEquals(0, l1Pixel.getGreen());
        assertEquals(0, l1Pixel.getBlue());

        // Layer 2 should remain White
        Color l2Pixel = new Color(l2.getImage().getRGB(5, 5));
        assertEquals(255, l2Pixel.getRed());
        assertEquals(255, l2Pixel.getGreen());
        assertEquals(255, l2Pixel.getBlue());

        // Select Layer 2 and then Undo the operation on Layer 1
        controller.selectLayer(l2);
        controller.undo();

        // Layer 1 should be restored to White even though Layer 2 was currently selected!
        Color restoredL1Pixel = new Color(l1.getImage().getRGB(5, 5));
        assertEquals(255, restoredL1Pixel.getRed());
        assertEquals(255, restoredL1Pixel.getGreen());
        assertEquals(255, restoredL1Pixel.getBlue());
    }

    @Test
    public void testDocumentChangeListener() {
        EditorController controller = new EditorController();
        AtomicInteger notificationCount = new AtomicInteger(0);

        controller.addChangeListener(notificationCount::incrementAndGet);

        controller.addNewBlankLayer();
        assertTrue(notificationCount.get() > 0);

        int countBefore = notificationCount.get();
        controller.undo();
        assertTrue(notificationCount.get() > countBefore);
    }
}
