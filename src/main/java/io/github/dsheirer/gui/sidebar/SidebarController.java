package io.github.dsheirer.gui.sidebar;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import io.github.dsheirer.gui.SidebarPanel.SidebarListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.SwingUtilities;

public class SidebarController implements Initializable {
    @FXML private VBox root;
    @FXML private Button toggleBtn;
    @FXML private VBox menuContainer;

    private boolean collapsed = false;
    private SidebarListener listener;
    private List<SidebarItemModel> items = new ArrayList<>();
    private String activeId;

    public void setListener(SidebarListener listener) {
        this.listener = listener;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.setPrefWidth(250);
        toggleBtn.setTooltip(new Tooltip("Collapse Sidebar"));

        // Define items (mimicking the original Swing setup)
        items.add(new SidebarItemModel("Now Playing", "M8 5v14l11-7z", "now_playing", true)); // Play icon

        SidebarItemModel playlist = new SidebarItemModel("Playlist Editor", "M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z", "playlist_editor", false);
        playlist.addSubItem("Channels", "playlist_channels");
        playlist.addSubItem("Aliases", "playlist_aliases");
        playlist.addSubItem("Streaming", "playlist_streaming");
        playlist.addSubItem("Radio Reference", "playlist_radioreference");
        playlist.addSubItem("Two Tones", "playlist_twotones");
        playlist.addSubItem("Playlists", "playlist_playlists");
        playlist.expanded = true;
        items.add(playlist);

        items.add(new SidebarItemModel("Tuners", "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z", "tuners", true)); // Sliders
        items.add(new SidebarItemModel("Performance & Logs", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z", "logs", true)); // File text
        items.add(new SidebarItemModel("Audio Recordings", "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z", "audio_recordings", true)); // Mic
        items.add(new SidebarItemModel(".bits Viewer", "M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z", "msg_viewer", true)); // Code
        items.add(new SidebarItemModel("User Preferences", "M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.06-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.73,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.06,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.49-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z", "user_prefs", true)); // Cogs
        items.add(new SidebarItemModel("Help & Docs", "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z", "help_viewer", true)); // Question
        items.add(new SidebarItemModel("Exit", "M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2 .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z", "exit", true)); // Exit

        render();
    }

    @FXML
    private void handleToggle() {
        collapsed = !collapsed;
        root.setPrefWidth(collapsed ? 60 : 250);
        toggleBtn.getTooltip().setText(collapsed ? "Expand Sidebar" : "Collapse Sidebar");
        render();
    }

    public void setActive(String id) {
        this.activeId = id;
        render();
    }

    private void render() {
        menuContainer.getChildren().clear();

        for (SidebarItemModel item : items) {
            VBox itemView = createItemView(item);
            menuContainer.getChildren().add(itemView);

            if (item.expanded && !collapsed && !item.subItems.isEmpty()) {
                for (SidebarItemModel.SubItem sub : item.subItems) {
                    HBox subView = createSubItemView(sub);
                    menuContainer.getChildren().add(subView);
                }
            }
        }
    }

    private VBox createItemView(SidebarItemModel item) {
        VBox wrapper = new VBox();
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setCursor(Cursor.HAND);

        boolean isActive = item.id.equals(activeId) || item.subItems.stream().anyMatch(s -> s.id.equals(activeId));

        String baseStyle = isActive ? "-fx-background-color: #c8c8c8;" : "-fx-background-color: transparent;";
        box.setStyle(baseStyle);

        SVGPath icon = new SVGPath();
        icon.setContent(item.iconPath);
        icon.setFill(Color.BLACK);

        // Scale icon slightly down to match font size
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);

        HBox iconBox = new HBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefWidth(24);

        box.getChildren().add(iconBox);

        if (!collapsed) {
            Label label = new Label(item.label);
            label.setFont(Font.font("System", FontWeight.BOLD, 12));
            label.setTextFill(Color.BLACK);
            box.getChildren().add(label);
        } else {
            Tooltip.install(box, new Tooltip(item.label));
        }

        box.setOnMouseEntered(e -> {
            if (!isActive) box.setStyle("-fx-background-color: #dcdcdc;");
        });
        box.setOnMouseExited(e -> {
            if (!isActive) box.setStyle(baseStyle);
        });

        box.setOnMouseClicked(e -> {
            if (item.selectable) {
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onItemSelected(item.id));
                }
            } else if (!item.subItems.isEmpty()) {
                if (collapsed) {
                    // We'd show a popup here in a fully featured version, but for simplicity we expand
                    handleToggle();
                } else {
                    item.expanded = !item.expanded;
                    if (item.expanded && !item.subItems.isEmpty() && listener != null) {
                        SwingUtilities.invokeLater(() -> listener.onItemSelected(item.subItems.get(0).id));
                    }
                    render();
                }
            } else {
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onActionRequested(item.id));
                }
            }
        });

        wrapper.getChildren().add(box);
        return wrapper;
    }

    private HBox createSubItemView(SidebarItemModel.SubItem sub) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6, 6, 6, 40)); // Indent
        box.setCursor(Cursor.HAND);

        boolean isActive = sub.id.equals(activeId);
        String baseStyle = isActive ? "-fx-background-color: #c8c8c8;" : "-fx-background-color: transparent;";
        box.setStyle(baseStyle);

        if (!collapsed) {
            Label label = new Label(sub.label);
            label.setTextFill(Color.BLACK);
            box.getChildren().add(label);
        } else {
            Tooltip.install(box, new Tooltip(sub.label));
        }

        box.setOnMouseEntered(e -> {
            if (!isActive) box.setStyle("-fx-background-color: #dcdcdc;");
        });
        box.setOnMouseExited(e -> {
            if (!isActive) box.setStyle(baseStyle);
        });

        box.setOnMouseClicked(e -> {
            if (sub.id.startsWith("vis_")) {
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onActionRequested(sub.id));
                }
            } else {
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.onItemSelected(sub.id));
                }
            }
        });

        return box;
    }

    public static class SidebarItemModel {
        String label;
        String iconPath;
        String id;
        boolean selectable;
        boolean expanded = false;
        List<SubItem> subItems = new ArrayList<>();

        public SidebarItemModel(String label, String iconPath, String id, boolean selectable) {
            this.label = label;
            this.iconPath = iconPath;
            this.id = id;
            this.selectable = selectable;
        }

        public void addSubItem(String label, String id) {
            subItems.add(new SubItem(label, id));
        }

        public static class SubItem {
            String label;
            String id;
            public SubItem(String label, String id) {
                this.label = label;
                this.id = id;
            }
        }
    }
}
