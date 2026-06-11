package io.github.dsheirer.gui.sidebar;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import io.github.dsheirer.gui.SidebarPanel.SidebarListener;
import io.github.dsheirer.gui.theme.ThemeManager;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class SidebarController implements Initializable {
    @FXML private VBox root;
    @FXML private Button toggleBtn;
    @FXML private VBox menuContainer;

    private boolean collapsed = false;
    private SidebarListener listener;
    private List<SidebarItemModel> items = new ArrayList<>();
    private String activeId;

    // Section separator indices — items after these indices get a divider before them
    private static final String SECTION_MAIN = "main";
    private static final String SECTION_TOOLS = "tools";
    private static final String SECTION_SYSTEM = "system";

    public void setListener(SidebarListener listener) {
        this.listener = listener;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.setPrefWidth(250);

        // ── MAIN VIEWS ──
        items.add(new SidebarItemModel("Now Playing", "M8 5v14l11-7z", "now_playing", true, SECTION_MAIN));

        // Renamed from "Playlist Editor" — these sub-pages manage channels, aliases,
        // streaming destinations, etc., which is really system configuration.
        SidebarItemModel config = new SidebarItemModel("Channel Manager",
            "M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z",
            "playlist_editor", false, SECTION_MAIN);
        config.addSubItem("Channels",         "playlist_channels",         "M17 3H7c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H7V5h10v14z");
        config.addSubItem("Radio Reference",  "playlist_radioreference",  "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z");
        config.addSubItem("Aliases",          "playlist_aliases",          "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z");
        config.addSubItem("Streaming",        "playlist_streaming",        "M3.24 6.15C2.51 6.43 2 7.17 2 8v12c0 1.1.9 2 2 2h16c1.1 0 2-.89 2-2V8c0-.83-.51-1.57-1.24-1.85L12 2 3.24 6.15zM12 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z");
        config.addSubItem("Two Tones",        "playlist_twotones",        "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z");
        config.addSubItem("Playlists",        "playlist_playlists",       "M4 10h12v2H4zm0-4h12v2H4zm0 8h8v2H4zm10 0v6l5-3z");
        config.expanded = true;
        items.add(config);

        items.add(new SidebarItemModel("Tuners",
            "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z",
            "tuners", true, SECTION_MAIN));

        // ── TOOLS ──
        items.add(new SidebarItemModel("Performance & Logs",
            "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
            "logs", true, SECTION_TOOLS));
        items.add(new SidebarItemModel("Audio Recordings",
            "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z",
            "audio_recordings", true, SECTION_TOOLS));
        items.add(new SidebarItemModel(".bits Viewer",
            "M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z",
            "msg_viewer", true, SECTION_TOOLS));

        // ── SYSTEM ──
        items.add(new SidebarItemModel("User Preferences",
            "M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.06-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.73,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.06,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.49-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z",
            "user_prefs", true, SECTION_SYSTEM));
        items.add(new SidebarItemModel("Help & Docs",
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z",
            "help_viewer", true, SECTION_SYSTEM));
        items.add(new SidebarItemModel("Toggle Theme",
            "M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z",
            "toggle_theme", false, SECTION_SYSTEM));
        items.add(new SidebarItemModel("Exit",
            "M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2 .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z",
            "exit", true, SECTION_SYSTEM));

        render();
    }

    @FXML
    private void handleToggle() {
        collapsed = !collapsed;
        
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        javafx.animation.KeyValue kv = new javafx.animation.KeyValue(root.prefWidthProperty(), collapsed ? 60 : 250, javafx.animation.Interpolator.EASE_BOTH);
        javafx.animation.KeyFrame kf = new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), kv);
        timeline.getKeyFrames().add(kf);
        
        if (!collapsed) {
            render();
            timeline.play();
        } else {
            timeline.setOnFinished(e -> render());
            timeline.play();
        }
    }

    public void setActive(String id) {
        this.activeId = id;
        render();
    }

    private void render() {
        menuContainer.getChildren().clear();

        String lastSection = null;

        for (SidebarItemModel item : items) {
            // Insert section divider when section changes
            if (lastSection != null && !item.section.equals(lastSection)) {
                addSectionDivider();
            }
            lastSection = item.section;

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

    private void addSectionDivider() {
        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 6 10 6 10; -fx-opacity: 0.3;");
        menuContainer.getChildren().add(sep);
    }

    private VBox createItemView(SidebarItemModel item) {
        VBox wrapper = new VBox();
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 10, 8, 10));
        box.setCursor(Cursor.HAND);

        boolean isActive = item.id.equals(activeId) || item.subItems.stream().anyMatch(s -> s.id.equals(activeId));
        boolean isDark = ThemeManager.isNightModeEnabled();

        box.getStyleClass().add("sidebar-item");
        if (isActive) {
            box.getStyleClass().add("sidebar-item-active");
        }

        Color inactiveColor = isDark ? Color.web("#e0e0e0") : Color.web("#1C1C1E");
        Color inactiveIconColor = isDark ? Color.web("#b0b0b0") : Color.web("#636366");

        SVGPath icon = new SVGPath();
        icon.setContent(item.iconPath);
        icon.setFill(isActive ? Color.WHITE : inactiveIconColor);
        icon.setScaleX(0.75);
        icon.setScaleY(0.75);

        HBox iconBox = new HBox(icon);
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefWidth(24);
        iconBox.setMinWidth(24);

        box.getChildren().add(iconBox);

        if (!collapsed) {
            Label label = new Label(item.label);
            label.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 13));
            label.setTextFill(isActive ? Color.WHITE : inactiveColor);
            box.getChildren().add(label);

            // Show expand/collapse chevron for items with sub-items
            if (!item.subItems.isEmpty()) {
                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                box.getChildren().add(spacer);

                SVGPath chevron = new SVGPath();
                if (item.expanded) {
                    chevron.setContent("M7 10l5 5 5-5z"); // Down chevron
                } else {
                    chevron.setContent("M10 17l5-5-5-5z"); // Right chevron
                }
                chevron.setFill(isActive ? Color.web("#ffffff80") : Color.web("#8E8E93"));
                chevron.setScaleX(0.7);
                chevron.setScaleY(0.7);
                box.getChildren().add(chevron);
            }
        } else {
            Tooltip tooltip = new Tooltip(item.label);
            tooltip.setShowDelay(javafx.util.Duration.millis(200));
            Tooltip.install(box, tooltip);
        }

        box.setOnMouseEntered(e -> {
            if (!isActive) box.getStyleClass().add("sidebar-item-hover");
        });
        box.setOnMouseExited(e -> {
            box.getStyleClass().remove("sidebar-item-hover");
        });

        box.setOnMouseClicked(e -> {
            if (item.id.equals("toggle_theme")) {
                ThemeManager.toggleNightMode();
                render();
                return;
            }

            if (item.selectable) {
                if (listener != null) {
                    Platform.runLater(() -> listener.onItemSelected(item.id));
                }
            } else if (!item.subItems.isEmpty()) {
                if (collapsed) {
                    handleToggle();
                } else {
                    item.expanded = !item.expanded;
                    if (item.expanded && !item.subItems.isEmpty() && listener != null) {
                        Platform.runLater(() -> listener.onItemSelected(item.subItems.get(0).id));
                    }
                    render();
                }
            } else {
                if (listener != null) {
                    Platform.runLater(() -> listener.onActionRequested(item.id));
                }
            }
        });

        wrapper.getChildren().add(box);
        return wrapper;
    }

    private HBox createSubItemView(SidebarItemModel.SubItem sub) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6, 10, 6, 20));
        box.setCursor(Cursor.HAND);

        boolean isActive = sub.id.equals(activeId);
        boolean isDark = ThemeManager.isNightModeEnabled();
        box.getStyleClass().add("sidebar-item");
        if (isActive) {
            box.getStyleClass().add("sidebar-item-active");
        }

        Color inactiveColor = isDark ? Color.web("#d0d0d0") : Color.web("#3A3A3C");
        Color inactiveIconColor = isDark ? Color.web("#909090") : Color.web("#8E8E93");

        if (!collapsed) {
            // Sub-item icon (smaller)
            if (sub.iconPath != null && !sub.iconPath.isEmpty()) {
                SVGPath subIcon = new SVGPath();
                subIcon.setContent(sub.iconPath);
                subIcon.setFill(isActive ? Color.WHITE : inactiveIconColor);
                subIcon.setScaleX(0.6);
                subIcon.setScaleY(0.6);
                HBox subIconBox = new HBox(subIcon);
                subIconBox.setAlignment(Pos.CENTER);
                subIconBox.setPrefWidth(20);
                subIconBox.setMinWidth(20);
                box.getChildren().add(subIconBox);
            } else {
                // Dot indicator as fallback
                Circle dot = new Circle(3);
                dot.setFill(isActive ? Color.WHITE : inactiveIconColor);
                HBox dotBox = new HBox(dot);
                dotBox.setAlignment(Pos.CENTER);
                dotBox.setPrefWidth(20);
                dotBox.setMinWidth(20);
                box.getChildren().add(dotBox);
            }

            Label label = new Label(sub.label);
            label.setFont(Font.font("SansSerif", FontWeight.NORMAL, 13));
            label.setTextFill(isActive ? Color.WHITE : inactiveColor);
            box.getChildren().add(label);
        } else {
            Tooltip tooltip = new Tooltip(sub.label);
            tooltip.setShowDelay(javafx.util.Duration.millis(200));
            Tooltip.install(box, tooltip);
        }

        box.setOnMouseEntered(e -> {
            if (!isActive) box.getStyleClass().add("sidebar-item-hover");
        });
        box.setOnMouseExited(e -> {
            box.getStyleClass().remove("sidebar-item-hover");
        });

        box.setOnMouseClicked(e -> {
            if (sub.id.startsWith("vis_")) {
                if (listener != null) {
                    Platform.runLater(() -> listener.onActionRequested(sub.id));
                }
            } else {
                if (listener != null) {
                    Platform.runLater(() -> listener.onItemSelected(sub.id));
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
        String section;
        List<SubItem> subItems = new ArrayList<>();

        public SidebarItemModel(String label, String iconPath, String id, boolean selectable, String section) {
            this.label = label;
            this.iconPath = iconPath;
            this.id = id;
            this.selectable = selectable;
            this.section = section;
        }

        public void addSubItem(String label, String id) {
            subItems.add(new SubItem(label, id, null));
        }

        public void addSubItem(String label, String id, String iconPath) {
            subItems.add(new SubItem(label, id, iconPath));
        }

        public static class SubItem {
            String label;
            String id;
            String iconPath;
            public SubItem(String label, String id, String iconPath) {
                this.label = label;
                this.id = id;
                this.iconPath = iconPath;
            }
        }
    }
}
