



package io.github.dsheirer.gui.widget;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.layout.Region;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;


import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class Widget extends VBox {

    private final String mId;
    private final String mTitle;
    private final Region mContentComponent;
    private final HBox mHeaderPanel;
    private final Label mTitleLabel;
    private final Button mMinimizeButton;
    private final Button mCloseButton;
    private final WidgetContainer mContainer;
    private final int mMinHeight;
    private final VBox mResizeHandle;

    private boolean mMinimized = false;

    public Widget(String id, String title, Region contentComponent, WidgetContainer container, int minHeight) {
        mId = id;
        setId(id);
        mTitle = title;
        mContentComponent = contentComponent;
        mContainer = container;
        mMinHeight = minHeight;

        this.setMinWidth(0);

        // Apply card styling from the design system
        getStyleClass().add("kennebec-card");
        setStyle("-fx-padding: 0;");

        // Header toolbar
        mHeaderPanel = new HBox(5);
        mHeaderPanel.setAlignment(Pos.CENTER_LEFT);
        mHeaderPanel.setPadding(new javafx.geometry.Insets(6, 10, 6, 10));
        mHeaderPanel.setStyle("-fx-background-color: #F9F9FB; -fx-border-color: transparent transparent #E5E5EA transparent; -fx-border-width: 0 0 1 0; -fx-background-radius: 10 10 0 0;");

        // Drag handle indicator
        Label dragHandle = new Label("\u22EE\u22EE");
        dragHandle.setStyle("-fx-text-fill: #C7C7CC; -fx-cursor: move; -fx-font-size: 12px; -fx-padding: 0 4 0 0;");
        dragHandle.setTooltip(new javafx.scene.control.Tooltip("Drag to reorder"));
        mHeaderPanel.getChildren().add(dragHandle);

        mTitleLabel = new Label(title);
        mTitleLabel.setFont(javafx.scene.text.Font.font("SansSerif", javafx.scene.text.FontWeight.BOLD, 13));
        mTitleLabel.setTextFill(javafx.scene.paint.Color.web("#48484A"));
        mHeaderPanel.getChildren().add(mTitleLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        mHeaderPanel.getChildren().add(spacer);

        mMinimizeButton = createHeaderButton();
        mMinimizeButton.setTooltip(new javafx.scene.control.Tooltip("Minimize"));
        mMinimizeButton.accessibleTextProperty().set("Minimize Widget");
        mMinimizeButton.accessibleHelpProperty().set("Minimizes or expands the widget");
        mMinimizeButton.setOnAction(e -> toggleMinimized());
        mHeaderPanel.getChildren().add(mMinimizeButton);

        mCloseButton = createHeaderButton();
        mCloseButton.setTooltip(new javafx.scene.control.Tooltip("Close"));
        mCloseButton.accessibleTextProperty().set("Close Widget");
        mCloseButton.accessibleHelpProperty().set("Closes the widget");
        mCloseButton.setOnAction(e -> closeWidget());
        mHeaderPanel.getChildren().add(mCloseButton);

        updateIcons();

        getChildren().add(mHeaderPanel);

        int savedHeight = minHeight;
        if (container != null && container.getPreference() != null) {
            savedHeight = container.getPreference().getWidgetHeight(id, minHeight);
        }
        if (savedHeight > 0) {
            mContentComponent.setPrefHeight(savedHeight);
        }
        if (minHeight > 0) {
            mContentComponent.setMinHeight(minHeight);
        }
        mContentComponent.setMaxWidth(Double.MAX_VALUE);
        mContentComponent.setMinWidth(0);
        mHeaderPanel.setMinWidth(0);
        VBox.setVgrow(mContentComponent, Priority.ALWAYS);
        getChildren().add(mContentComponent);

        // Resize handle with hover feedback
        mResizeHandle = new VBox();
        mResizeHandle.setCursor(javafx.scene.Cursor.S_RESIZE);
        mResizeHandle.setStyle("-fx-background-color: #E5E5EA; -fx-background-radius: 0 0 10 10;");
        mResizeHandle.setPrefSize(0, 4);
        mResizeHandle.setOnMouseEntered(e -> mResizeHandle.setStyle("-fx-background-color: #007AFF; -fx-background-radius: 0 0 10 10;"));
        mResizeHandle.setOnMouseExited(e -> mResizeHandle.setStyle("-fx-background-color: #E5E5EA; -fx-background-radius: 0 0 10 10;"));
        mResizeHandle.setOnMousePressed(e -> {
            mResizeHandle.getProperties().put("startY", e.getScreenY());
            mResizeHandle.getProperties().put("startHeight", mContentComponent.getHeight());
        });
        mResizeHandle.setOnMouseDragged(e -> {
            double startY = (double) mResizeHandle.getProperties().get("startY");
            double startHeight = (double) mResizeHandle.getProperties().get("startHeight");
            double delta = e.getScreenY() - startY;
            double newHeight = Math.max(mMinHeight, startHeight + delta);
            mContentComponent.setPrefSize(mContentComponent.getWidth(), newHeight);
            requestLayout();
            if (mContainer != null) mContainer.requestLayout();
        });
        mResizeHandle.setOnMouseReleased(e -> {
            if (mContainer != null && mContainer.getPreference() != null) {
                mContainer.getPreference().setWidgetHeight(mId, (int)mContentComponent.getHeight());
            }
        });
        getChildren().add(mResizeHandle);
    }

    private Button createHeaderButton() {
        Button button = new Button();
        button.setPadding(new javafx.geometry.Insets(0, 4, 0, 4));
        button.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        return button;
    }

    public void setCloseButtonVisible(boolean visible) {
        mCloseButton.setVisible(visible);
    }

    public void setMinimizeButtonVisible(boolean visible) {
        mMinimizeButton.setVisible(visible);
    }

    public boolean isMinimizeButtonVisible() {
        return mMinimizeButton.isVisible();
    }

    public String getWidgetId() {
        return mId;
    }

    public void setHeaderVisible(boolean visible) {
        mHeaderPanel.setVisible(visible);
        mHeaderPanel.setManaged(visible);
    }

    public HBox getHeaderPanel() {
        return mHeaderPanel;
    }

    public void ensureContentComponentParent() {
        if (mContentComponent != null && mContentComponent.getParent() != this) {
            getChildren().add(1, mContentComponent);
            requestLayout();
            requestLayout();
        }
    }

    public void setDragging(boolean dragging) {
        if (dragging) {
            setStyle("-fx-padding: 0; -fx-effect: dropshadow(three-pass-box, rgba(0,122,255,0.3), 8, 0, 0, 2);");
        } else {
            setStyle("-fx-padding: 0;");
        }
        requestLayout();
    }

    public void setMinimized(boolean minimized) {
        mMinimized = minimized;
        mContentComponent.setVisible(!minimized);
        mContentComponent.setManaged(!minimized);
        mResizeHandle.setVisible(!minimized);
        mResizeHandle.setManaged(!minimized);
        updateIcons();
        mMinimizeButton.setTooltip(new javafx.scene.control.Tooltip(minimized ? "Expand" : "Minimize"));
        mMinimizeButton.accessibleTextProperty().set(minimized ? "Expand Widget" : "Minimize Widget");
        requestLayout();
    }

    public boolean isMinimized() {
        return mMinimized;
    }

    private void toggleMinimized() {
        setMinimized(!mMinimized);
        if (mContainer != null) {
            mContainer.onWidgetStateChanged(this);
        }
    }

    private void closeWidget() {
        if (mContainer != null) {
            mContainer.hideWidget(mId);
        }
    }



    private void updateIcons() {
        if (mMinimizeButton != null) {
            jiconfont.javafx.IconNode icon = new jiconfont.javafx.IconNode(mMinimized ? FontAwesome.PLUS_SQUARE_O : FontAwesome.MINUS_SQUARE_O);
            icon.setIconSize(14);
            icon.setFill(javafx.scene.paint.Color.web("#8E8E93"));
            mMinimizeButton.setGraphic(icon);
        }
        if (mCloseButton != null) {
            jiconfont.javafx.IconNode icon = new jiconfont.javafx.IconNode(FontAwesome.TIMES);
            icon.setIconSize(14);
            icon.setFill(javafx.scene.paint.Color.web("#8E8E93"));
            mCloseButton.setGraphic(icon);
        }
    }
}
