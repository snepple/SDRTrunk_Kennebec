package io.github.dsheirer.gui.playlist;

import javafx.scene.control.SplitPane;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Insets;

/**
 * Conceptual HIG-compliant redesign of the PlaylistEditor.
 * Features a Master-Detail layout (Sidebar Navigation) instead of a flat TabPane.
 */
public class HIGPlaylistEditor extends BorderPane {

    public HIGPlaylistEditor() {
        // HIG Master-Detail Layout (Sidebar)
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.25); // 25% sidebar, 75% detail view

        // --- Master View (Sidebar) ---
        VBox sidebar = new VBox(0);
        sidebar.setStyle("-fx-background-color: #F2F2F7;"); // HIG Grouped Background Color

        Label sidebarHeader = new Label("Library");
        sidebarHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8E8E93; -fx-padding: 16 8 8 16;"); // HIG Section Header

        ListView<String> navigationList = new ListView<>();
        navigationList.getItems().addAll("Channels", "Aliases", "Playlists", "Streaming", "Radio Reference", "Two Tones");
        navigationList.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-control-inner-background: transparent;");

        // Removing default focus ring and styling cells to look like HIG sidebar items
        // In a real implementation, a custom CellFactory would be used.

        sidebar.getChildren().addAll(sidebarHeader, navigationList);

        // --- Detail View (Main Content Area) ---
        // This area would dynamically swap content based on the sidebar selection.
        // For demonstration, we load the conceptual HIGChannelConfigurationEditor.
        BorderPane detailView = new BorderPane();
        detailView.setStyle("-fx-background-color: #FFFFFF;"); // HIG Default Background

        io.github.dsheirer.gui.playlist.channel.HIGChannelConfigurationEditor channelEditor = new io.github.dsheirer.gui.playlist.channel.HIGChannelConfigurationEditor();
        detailView.setCenter(channelEditor);

        splitPane.getItems().addAll(sidebar, detailView);

        setCenter(splitPane);
    }
}
