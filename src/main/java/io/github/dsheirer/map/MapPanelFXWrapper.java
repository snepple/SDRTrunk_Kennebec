package io.github.dsheirer.map;

import javafx.embed.swing.SwingNode;
import javafx.scene.layout.BorderPane;
import javax.swing.SwingUtilities;

/**
 * Temporary Scaffolding: Wrapper for MapPanel.
 * This is technical debt to be resolved in a future mapping migration phase.
 */
public class MapPanelFXWrapper extends BorderPane {

    public MapPanelFXWrapper(MapPanel mapPanel) {
        SwingNode swingNode = new SwingNode();
        // Use SwingUtilities.invokeLater to instantiate/attach the map
        SwingUtilities.invokeLater(() -> {
            swingNode.setContent(mapPanel);
        });
        this.setCenter(swingNode);
    }
}
