package io.github.dsheirer.gui.widget;

import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;

public class Widget extends VBox {

    @FXML private VBox root;
    @FXML private HBox headerBox;
    @FXML private Label titleLabel;
    @FXML private Button minimizeButton;
    @FXML private Button closeButton;
    @FXML private VBox contentBox;
    @FXML private Region resizeHandle;

    private String mId;
    private String mTitle;
    private JComponent mContentComponent;
    private SwingNode mSwingNode;
    private WidgetContainer mContainer;
    private int mMinHeight;

    private boolean mMinimized = false;
    private double mSavedHeight;

    public Widget() {
        // Required for FXML
    }

    public Widget(String id, String title, JComponent contentComponent, WidgetContainer container, int minHeight) {
        mId = id;
        mTitle = title;
        mContentComponent = contentComponent;
        mContainer = container;
        mMinHeight = minHeight;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Widget.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        titleLabel.setText(title);

        IconNode minimizeIcon = new IconNode(FontAwesome.CARET_DOWN);
        minimizeIcon.setIconSize(12);
        minimizeButton.setGraphic(minimizeIcon);
        minimizeButton.setTooltip(new Tooltip("Minimize"));
        minimizeButton.setOnAction(e -> toggleMinimized());

        IconNode closeIcon = new IconNode(FontAwesome.TIMES);
        closeIcon.setIconSize(12);
        closeButton.setGraphic(closeIcon);
        closeButton.setTooltip(new Tooltip("Close"));
        closeButton.setOnAction(e -> closeWidget());
        closeButton.setVisible(false);
        closeButton.setManaged(false);

        mSwingNode = new SwingNode();
        mSwingNode.setContent(contentComponent);
        contentBox.getChildren().add(mSwingNode);

        int savedHeight = minHeight;
        if (container != null && container.getPreference() != null) {
            savedHeight = container.getPreference().getWidgetHeight(id, minHeight);
        }
        if (savedHeight > 0) {
            contentBox.setPrefHeight(savedHeight);
            mSavedHeight = savedHeight;
        } else {
            mSavedHeight = minHeight;
        }

        if (minHeight > 0) {
            contentBox.setMinHeight(minHeight);
        }

        resizeHandle.setOnMousePressed(event -> {
            resizeHandle.setUserData(event.getSceneY());
        });

        resizeHandle.setOnMouseDragged(event -> {
            double startY = (double) resizeHandle.getUserData();
            double deltaY = event.getSceneY() - startY;
            double newHeight = contentBox.getHeight() + deltaY;

            if (newHeight >= mMinHeight) {
                contentBox.setPrefHeight(newHeight);
                resizeHandle.setUserData(event.getSceneY());
            }
        });

        resizeHandle.setOnMouseReleased(event -> {
            if (mContainer != null && mContainer.getPreference() != null) {
                mContainer.getPreference().setWidgetHeight(mId, (int) contentBox.getHeight());
            }
            mSavedHeight = contentBox.getHeight();
        });
    }

    public String getWidgetId() {
        return mId;
    }

    public void setMinimized(boolean minimized) {
        if (mMinimized != minimized) {
            mMinimized = minimized;
            updateMinimizedState();
        }
    }

    public boolean isMinimized() {
        return mMinimized;
    }

    public void setMinimizeButtonVisible(boolean visible) {
        minimizeButton.setVisible(visible);
        minimizeButton.setManaged(visible);
    }

    public boolean isMinimizeButtonVisible() {
        return minimizeButton.isVisible();
    }

    public void setCloseButtonVisible(boolean visible) {
        closeButton.setVisible(visible);
        closeButton.setManaged(visible);
    }

    public boolean isCloseButtonVisible() {
        return closeButton.isVisible();
    }

    private void toggleMinimized() {
        mMinimized = !mMinimized;
        updateMinimizedState();
        if (mContainer != null) {
            mContainer.onWidgetStateChanged(this);
        }
    }

    private void updateMinimizedState() {
        contentBox.setVisible(!mMinimized);
        contentBox.setManaged(!mMinimized);
        resizeHandle.setVisible(!mMinimized);
        resizeHandle.setManaged(!mMinimized);

        IconNode icon = new IconNode(mMinimized ? FontAwesome.CARET_RIGHT : FontAwesome.CARET_DOWN);
        icon.setIconSize(12);
        minimizeButton.setGraphic(icon);
    }

    private void closeWidget() {
        if (mContainer != null) {
            mContainer.hideWidget(mId);
        }
    }

    public boolean containsComponent(JComponent component) {
        return mContentComponent == component || (mContentComponent instanceof JPanel && isChildOf((JPanel)mContentComponent, component));
    }

    private boolean isChildOf(Container parent, Component child) {
        for (Component c : parent.getComponents()) {
            if (c == child) {
                return true;
            }
            if (c instanceof Container) {
                if (isChildOf((Container) c, child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
