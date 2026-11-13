package com.imageeditor;

import com.imageeditor.command.*;
import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {

    @Test
    public void testAddLayerCommandUndoRedo() {
        ImageDocument doc = new ImageDocument();
        CommandHistory history = new CommandHistory();

        Layer l1 = new Layer("L1", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        Layer l2 = new Layer("L2", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        history.executeCommand(new AddLayerCommand(doc, l1));
        assertEquals(1, doc.getLayerCount());
        assertSame(l1, doc.getSelectedLayer());

        history.executeCommand(new AddLayerCommand(doc, l2));
        assertEquals(2, doc.getLayerCount());
        assertSame(l2, doc.getSelectedLayer());

        // Undo adding L2
        assertTrue(history.canUndo());
        history.undo();
        assertEquals(1, doc.getLayerCount());
        assertSame(l1, doc.getSelectedLayer());

        // Redo adding L2
        assertTrue(history.canRedo());
        history.redo();
        assertEquals(2, doc.getLayerCount());
        assertSame(l2, doc.getSelectedLayer());
    }

    @Test
    public void testDeleteLayerCommandUndoRedo() {
        ImageDocument doc = new ImageDocument();
        CommandHistory history = new CommandHistory();

        Layer l1 = new Layer("L1", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        Layer l2 = new Layer("L2", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        doc.addLayer(l1);
        doc.addLayer(l2);

        // Delete L2 (index 1)
        history.executeCommand(new DeleteLayerCommand(doc, l2, 1));
        assertEquals(1, doc.getLayerCount());
        assertSame(l1, doc.getSelectedLayer());

        // Undo deletion: L2 should be restored at index 1
        history.undo();
        assertEquals(2, doc.getLayerCount());
        assertEquals(1, doc.indexOf(l2));
        assertSame(l2, doc.getSelectedLayer());

        // Redo deletion
        history.redo();
        assertEquals(1, doc.getLayerCount());
        assertFalse(doc.getLayers().contains(l2));
    }

    @Test
    public void testReorderLayerCommandUndoRedo() {
        ImageDocument doc = new ImageDocument();
        CommandHistory history = new CommandHistory();

        Layer l1 = new Layer("L1", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        Layer l2 = new Layer("L2", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        doc.addLayer(l1); // 0
        doc.addLayer(l2); // 1

        history.executeCommand(new ReorderLayerCommand(doc, 0, 1));
        assertEquals(0, doc.indexOf(l2));
        assertEquals(1, doc.indexOf(l1));

        // Undo: restores original order
        history.undo();
        assertEquals(0, doc.indexOf(l1));
        assertEquals(1, doc.indexOf(l2));

        // Redo: reorders again
        history.redo();
        assertEquals(0, doc.indexOf(l2));
        assertEquals(1, doc.indexOf(l1));
    }

    @Test
    public void testToggleVisibilityCommandUndoRedo() {
        Layer layer = new Layer("L", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        CommandHistory history = new CommandHistory();

        assertTrue(layer.isVisible());

        history.executeCommand(new ToggleVisibilityCommand(layer, false));
        assertFalse(layer.isVisible());

        history.undo();
        assertTrue(layer.isVisible());

        history.redo();
        assertFalse(layer.isVisible());
    }

    @Test
    public void testChangeOpacityCommandUndoRedo() {
        Layer layer = new Layer("L", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        CommandHistory history = new CommandHistory();

        assertEquals(1.0f, layer.getOpacity(), 0.0001f);

        history.executeCommand(new ChangeOpacityCommand(layer, 1.0f, 0.4f));
        assertEquals(0.4f, layer.getOpacity(), 0.0001f);

        history.undo();
        assertEquals(1.0f, layer.getOpacity(), 0.0001f);

        history.redo();
        assertEquals(0.4f, layer.getOpacity(), 0.0001f);
    }

    @Test
    public void testNewOperationClearsRedoHistory() {
        ImageDocument doc = new ImageDocument();
        CommandHistory history = new CommandHistory();

        Layer l1 = new Layer("L1", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        Layer l2 = new Layer("L2", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        Layer l3 = new Layer("L3", new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        history.executeCommand(new AddLayerCommand(doc, l1));
        history.executeCommand(new AddLayerCommand(doc, l2));

        // Undo l2
        history.undo();
        assertTrue(history.canRedo());

        // Perform new operation l3
        history.executeCommand(new AddLayerCommand(doc, l3));

        // Redo stack must be cleared
        assertFalse(history.canRedo());
    }
}
