package io.github.dsheirer.gui.widget;

import io.github.dsheirer.preference.NowPlayingPreference;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.geometry.Insets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WidgetContainer extends VBox {

    @FXML private VBox root;

    private final NowPlayingPreference mPreference;
    private final List<Widget> mWidgets = new ArrayList<>();

    private Widget mDraggingWidget = null;
    private int mDropIndex = -1;

    public WidgetContainer() {
        this(null);
    }

    public WidgetContainer(NowPlayingPreference preference) {
        mPreference = preference;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/WidgetContainer.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public NowPlayingPreference getPreference() {
        return mPreference;
    }

    public void removeAll() {
        getChildren().clear();
        mWidgets.clear();
    }

    public void addWidget(Widget widget, boolean pinned) {
        mWidgets.add(widget);

        if (!pinned) {
            setupDragAndDrop(widget);
        } else {
            widget.setCloseButtonVisible(true);
        }

        boolean isVisible = mPreference != null ? mPreference.isWidgetVisible(widget.getWidgetId(), true) : true;
        widget.setVisible(isVisible);
        widget.setManaged(isVisible);

        if (widget.isMinimizeButtonVisible()) {
            boolean isMinimized = mPreference != null ? mPreference.isWidgetMinimized(widget.getWidgetId(), false) : false;
            widget.setMinimized(isMinimized);
        }
    }

    public void layoutWidgets(String pinnedWidgetId) {
        mWidgets.sort((w1, w2) -> {
            if (w1.getWidgetId().equals(pinnedWidgetId)) return 1;
            if (w2.getWidgetId().equals(pinnedWidgetId)) return -1;

            int order1 = mPreference != null ? mPreference.getWidgetOrder(w1.getWidgetId(), 999) : 999;
            int order2 = mPreference != null ? mPreference.getWidgetOrder(w2.getWidgetId(), 999) : 999;
            return Integer.compare(order1, order2);
        });

        rebuildLayout();
    }

    private void rebuildLayout() {
        getChildren().clear();
        for (int i = 0; i < mWidgets.size(); i++) {
            Widget w = mWidgets.get(i);

            if (mDraggingWidget != null && mDropIndex == i) {
                Region indicator = new Region();
                indicator.setBackground(new Background(new BackgroundFill(Color.web("#0078D7"), CornerRadii.EMPTY, Insets.EMPTY)));
                indicator.setPrefHeight(2);
                getChildren().add(indicator);
            }

            if (w.isVisible()) {
                getChildren().add(w);
            }
        }

        if (mDraggingWidget != null && mDropIndex == mWidgets.size()) {
            Region indicator = new Region();
            indicator.setBackground(new Background(new BackgroundFill(Color.web("#0078D7"), CornerRadii.EMPTY, Insets.EMPTY)));
            indicator.setPrefHeight(2);
            getChildren().add(indicator);
        }
    }

    public void setWidgetVisible(String id, boolean visible) {
        for (Widget w : mWidgets) {
            if (w.getWidgetId().equals(id)) {
                w.setVisible(visible);
                w.setManaged(visible);
                if (mPreference != null) {
                    mPreference.setWidgetVisible(id, visible);
                }
                rebuildLayout();
                break;
            }
        }
    }

    public void hideWidget(String id) {
        setWidgetVisible(id, false);
    }

    public void onWidgetStateChanged(Widget widget) {
        if (mPreference != null) {
            mPreference.setWidgetMinimized(widget.getWidgetId(), widget.isMinimized());
        }
    }

    public void ensureComponentInWidget(String id) {
        // Handle logic via rebuildLayout for JavaFX
        rebuildLayout();
    }

    private void setupDragAndDrop(Widget widget) {
        widget.setOnDragDetected(event -> {
            // Setup simple drag logic within the VBox
            mDraggingWidget = widget;
            event.consume();
        });

        widget.setOnMouseDragged(event -> {
            if (mDraggingWidget != null) {
                int newIndex = -1;
                double y = event.getSceneY();
                for (int i = 0; i < mWidgets.size(); i++) {
                    Widget w = mWidgets.get(i);
                    if (y < w.getLocalToSceneTransform().getTy() + w.getHeight() / 2) {
                        newIndex = i;
                        break;
                    }
                }
                if (newIndex == -1) {
                    newIndex = mWidgets.size();
                }

                if (newIndex != mDropIndex) {
                    mDropIndex = newIndex;
                    rebuildLayout();
                }
            }
        });

        widget.setOnMouseReleased(event -> {
            if (mDraggingWidget != null) {
                if (mDropIndex >= 0 && mDropIndex <= mWidgets.size()) {
                    mWidgets.remove(mDraggingWidget);
                    if (mDropIndex > mWidgets.size()) {
                        mDropIndex = mWidgets.size();
                    }
                    mWidgets.add(mDropIndex, mDraggingWidget);

                    if (mPreference != null) {
                        for (int i = 0; i < mWidgets.size(); i++) {
                            mPreference.setWidgetOrder(mWidgets.get(i).getWidgetId(), i);
                        }
                    }
                }

                mDraggingWidget = null;
                mDropIndex = -1;
                rebuildLayout();
            }
        });
    }
}
