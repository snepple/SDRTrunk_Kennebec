package io.github.dsheirer.gui.widget;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class Widget extends VBox {

    private static final Logger mLog = LoggerFactory.getLogger(Widget.class);

    private final String mId;
    private final String mTitle;
    private final JComponent mContentComponent;
    private final WidgetContainer mContainer;
    private final int mMinHeight;

    private boolean mMinimized = false;
    private WidgetController mController;
    private SwingNode mSwingNode;

    public Widget(String id, String title, JComponent contentComponent, WidgetContainer container, int minHeight) {
        mId = id;
        mTitle = title;
        mContentComponent = contentComponent;
        mContainer = container;
        mMinHeight = minHeight;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Widget.fxml"));
        try {
            loader.setRoot(this);
            loader.setController(new WidgetController());
            loader.load();
            mController = loader.getController();
        } catch (IOException e) {
            mLog.error("Error loading Widget.fxml", e);
        }

        mController.titleLabel.setText(title);
        mController.titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #808080;");

        mController.minimizeButton.setTooltip(new Tooltip("Minimize"));
        mController.minimizeButton.setOnAction(e -> toggleMinimized());

        mController.closeButton.setTooltip(new Tooltip("Close"));
        mController.closeButton.setOnAction(e -> closeWidget());

        int savedHeight = minHeight;
        if (container != null && container.getPreference() != null) {
            savedHeight = container.getPreference().getWidgetHeight(id, minHeight);
        }

        if (savedHeight > 0) {
            mContentComponent.setPreferredSize(new Dimension(0, savedHeight));
        }
        if (minHeight > 0) {
            mContentComponent.setMinimumSize(new Dimension(0, minHeight));
        }

        mSwingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> mSwingNode.setContent(mContentComponent));
        mController.contentContainer.getChildren().add(mSwingNode);

        // Styling for background and border
        setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-width: 1;");

        setupResizeHandle();
        updateIcons();
    }

    private void setupResizeHandle() {
        mController.resizeHandle.setOnMousePressed(e -> {
            e.consume();
        });

        mController.resizeHandle.setOnMouseDragged(e -> {
            double deltaY = e.getY(); // Local Y offset within resizeHandle
            double newHeight = Math.max(mMinHeight, mSwingNode.getBoundsInParent().getHeight() + deltaY);

            SwingUtilities.invokeLater(() -> {
                mContentComponent.setPreferredSize(new Dimension(mContentComponent.getWidth(), (int) newHeight));
                mContentComponent.revalidate();
            });
            e.consume();
        });

        mController.resizeHandle.setOnMouseReleased(e -> {
            if (mContainer != null && mContainer.getPreference() != null) {
                mContainer.getPreference().setWidgetHeight(mId, mContentComponent.getHeight());
            }
            e.consume();
        });
    }

    public void setCloseButtonVisible(boolean visible) {
        mController.closeButton.setVisible(visible);
        mController.closeButton.setManaged(visible);
    }

    public void setMinimizeButtonVisible(boolean visible) {
        mController.minimizeButton.setVisible(visible);
        mController.minimizeButton.setManaged(visible);
    }

    public boolean isMinimizeButtonVisible() {
        return mController.minimizeButton.isVisible();
    }

    public String getWidgetId() {
        return mId;
    }

    public HBox getHeaderPanel() {
        return mController.headerBox;
    }

    public void ensureContentComponentParent() {
        // Automatically handled by JavaFX/SwingNode embedding unless reparented
        SwingUtilities.invokeLater(() -> {
            if (mSwingNode.getContent() != mContentComponent) {
                mSwingNode.setContent(mContentComponent);
            }
        });
    }

    public void setDragging(boolean dragging) {
        if (dragging) {
            mController.headerBox.setStyle("-fx-background-color: #dcdcdc; -fx-background-radius: 12 12 0 0;");
        } else {
            mController.headerBox.setStyle("-fx-background-color: transparent;");
        }
    }

    public void setMinimized(boolean minimized) {
        mMinimized = minimized;
        mController.contentContainer.setVisible(!minimized);
        mController.contentContainer.setManaged(!minimized);
        mController.resizeHandle.setVisible(!minimized);
        mController.resizeHandle.setManaged(!minimized);
        updateIcons();
        mController.minimizeButton.setTooltip(new Tooltip(minimized ? "Expand" : "Minimize"));
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
        // Use simple text mapping for FontAwesome or simple unicode characters for now
        // In a real migration we'd use a JavaFX icon font library
        mController.minimizeButton.setText(mMinimized ? "+" : "-");
        mController.closeButton.setText("x");
    }
}
