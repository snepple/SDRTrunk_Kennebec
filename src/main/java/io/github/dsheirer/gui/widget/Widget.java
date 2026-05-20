package io.github.dsheirer.gui.widget;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
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
    private final Object mContentObject;
    private final WidgetContainer mContainer;
    private final int mMinHeight;

    private boolean mMinimized = false;
    private WidgetController mController;
    private Node mContentNode;

    public Widget(String id, String title, Object contentObject, WidgetContainer container, int minHeight) {
        mId = id;
        mTitle = title;
        mContentObject = contentObject;
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

        if (contentObject instanceof JComponent) {
            JComponent swingComp = (JComponent) contentObject;
            if (savedHeight > 0) {
                swingComp.setPreferredSize(new Dimension(0, savedHeight));
            }
            if (minHeight > 0) {
                swingComp.setMinimumSize(new Dimension(0, minHeight));
            }
            SwingNode swingNode = new SwingNode();
            SwingUtilities.invokeLater(() -> swingNode.setContent(swingComp));
            mContentNode = swingNode;
        } else if (contentObject instanceof Node) {
            mContentNode = (Node) contentObject;
            if (savedHeight > 0) {
                if (mContentNode instanceof javafx.scene.layout.Region) {
                    ((javafx.scene.layout.Region)mContentNode).setPrefHeight(savedHeight);
                }
            }
        }

        if (mContentNode != null) {
            mController.contentContainer.getChildren().add(mContentNode);
        }

        setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #e0e0e0; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-width: 1;");

        setupResizeHandle();
        updateIcons();
    }

    private void setupResizeHandle() {
        mController.resizeHandle.setOnMousePressed(e -> {
            e.consume();
        });

        mController.resizeHandle.setOnMouseDragged(e -> {
            double deltaY = e.getY();
            double newHeight = Math.max(mMinHeight, mContentNode.getBoundsInParent().getHeight() + deltaY);

            if (mContentObject instanceof JComponent) {
                JComponent swingComp = (JComponent) mContentObject;
                SwingUtilities.invokeLater(() -> {
                    swingComp.setPreferredSize(new Dimension(swingComp.getWidth(), (int) newHeight));
                    swingComp.revalidate();
                });
            } else if (mContentObject instanceof javafx.scene.layout.Region) {
                ((javafx.scene.layout.Region)mContentObject).setPrefHeight(newHeight);
            }
            e.consume();
        });

        mController.resizeHandle.setOnMouseReleased(e -> {
            if (mContainer != null && mContainer.getPreference() != null) {
                double h = 0;
                if (mContentObject instanceof JComponent) {
                    h = ((JComponent)mContentObject).getHeight();
                } else if (mContentObject instanceof javafx.scene.layout.Region) {
                    h = ((javafx.scene.layout.Region)mContentObject).getHeight();
                }
                mContainer.getPreference().setWidgetHeight(mId, (int) h);
            }
            e.consume();
        });
    }

    public String getWidgetId() {
        return mId;
    }

    private void toggleMinimized() {
        mMinimized = !mMinimized;
        mController.contentContainer.setVisible(!mMinimized);
        mController.contentContainer.setManaged(!mMinimized);
        mController.resizeHandle.setVisible(!mMinimized);
        mController.resizeHandle.setManaged(!mMinimized);
        updateIcons();
        if (mContainer != null) {
            mContainer.layoutWidgets(mId);
        }
    }

    private void closeWidget() {
        if (mContainer != null) {
            mContainer.hideWidget(mId);
        }
    }

    private void updateIcons() {
        if(mMinimized) {
            mController.minimizeButton.setText("+");
        } else {
            mController.minimizeButton.setText("-");
        }
    }

    public void setMinimizeButtonVisible(boolean visible) {
        mController.minimizeButton.setVisible(visible);
        mController.minimizeButton.setManaged(visible);
    }

    public void setCloseButtonVisible(boolean visible) {
        mController.closeButton.setVisible(visible);
        mController.closeButton.setManaged(visible);
    }

    public boolean isMinimizeButtonVisible() {
        return mController.minimizeButton.isVisible();
    }

    public void setMinimized(boolean minimized) {
        if (mMinimized != minimized) {
            toggleMinimized();
        }
    }

    public boolean isMinimized() {
        return mMinimized;
    }

    public HBox getHeaderPanel() {
        return mController.headerBox;
    }

    public void setDragging(boolean dragging) {
    }

    public void ensureContentComponentParent() {
    }
}
