package io.github.dsheirer.gui.widget;

import io.github.dsheirer.preference.NowPlayingPreference;
import javafx.geometry.Point2D;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class WidgetContainer extends VBox {

    private final NowPlayingPreference mPreference;
    private final List<Widget> mWidgets = new ArrayList<>();

    private Widget mDraggingWidget = null;
    private int mDropIndex = -1;

    public WidgetContainer(NowPlayingPreference preference) {
        mPreference = preference;
        setSpacing(2);
        setFillWidth(true);
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

        // Setup initial visibility and minimized states from prefs
        boolean isVisible = mPreference.isWidgetVisible(widget.getWidgetId(), true);
        widget.setVisible(isVisible);
        widget.setManaged(isVisible);

        if (widget.isMinimizeButtonVisible()) {
            boolean isMinimized = mPreference.isWidgetMinimized(widget.getWidgetId(), false);
            widget.setMinimized(isMinimized);
        }
    }

    // Allows the widget container to sort based on preferences once all are added
    public void layoutWidgets(String pinnedWidgetId) {
        // Sort widgets by order preference
        mWidgets.sort((w1, w2) -> {
            if (w1.getWidgetId().equals(pinnedWidgetId)) return 1;
            if (w2.getWidgetId().equals(pinnedWidgetId)) return -1;

            int order1 = mPreference.getWidgetOrder(w1.getWidgetId(), 999);
            int order2 = mPreference.getWidgetOrder(w2.getWidgetId(), 999);
            return Integer.compare(order1, order2);
        });

        rebuildLayout();
    }

    private void rebuildLayout() {
        getChildren().clear();
        for (int i = 0; i < mWidgets.size(); i++) {
            Widget w = mWidgets.get(i);

            if (mDraggingWidget != null && mDropIndex == i) {
                // Add a drop indicator placeholder
                Pane indicator = new Pane();
                indicator.setStyle("-fx-background-color: #007aff; -fx-border-color: #007aff; -fx-border-width: 2;");
                indicator.setPrefHeight(6);
                getChildren().add(indicator);
            }

            getChildren().add(w);
        }

        if (mDraggingWidget != null && mDropIndex == mWidgets.size()) {
            Pane indicator = new Pane();
            indicator.setStyle("-fx-background-color: #007aff;");
            indicator.setPrefHeight(4);
            getChildren().add(indicator);
        }
    }

    public void setWidgetVisible(String id, boolean visible) {
        for (Widget w : mWidgets) {
            if (w.getWidgetId().equals(id)) {
                w.setVisible(visible);
                w.setManaged(visible);
                mPreference.setWidgetVisible(id, visible);
                break;
            }
        }
    }

    public void hideWidget(String id) {
        setWidgetVisible(id, false);
    }

    public void onWidgetStateChanged(Widget widget) {
        if (widget.isMinimizeButtonVisible()) {
            mPreference.setWidgetMinimized(widget.getWidgetId(), widget.isMinimized());
        }
    }

    public void ensureComponentInWidget(String id) {
        for (Widget w : mWidgets) {
            if (w.getWidgetId().equals(id)) {
                w.ensureContentComponentParent();
                break;
            }
        }
    }

    private void setupDragAndDrop(Widget widget) {
        HBox header = widget.getHeaderPanel();

        header.setOnMousePressed(e -> {
            mDraggingWidget = widget;
            widget.setDragging(true);
            e.consume();
        });

        header.setOnMouseDragged(e -> {
            if (mDraggingWidget == null) return;

            // Convert mouse position to container coordinates
            Point2D pt = this.sceneToLocal(e.getSceneX(), e.getSceneY());

            // Find where we are dropping
            mDropIndex = -1;
            for (int i = 0; i < mWidgets.size(); i++) {
                Widget w = mWidgets.get(i);
                // Prevent dropping after the pinned widget (assumed to be last)
                if (i == mWidgets.size() - 1) { // Assume last is pinned Resource Status
                    if (pt.getY() < w.getBoundsInParent().getMinY()) {
                        mDropIndex = i;
                        break;
                    }
                } else if (w.isVisible() && pt.getY() < w.getBoundsInParent().getMinY() + w.getBoundsInParent().getHeight() / 2) {
                    mDropIndex = i;
                    break;
                }
            }

            if (mDropIndex == -1) {
                mDropIndex = mWidgets.size() - 1; // Before the pinned widget
            }

            rebuildLayout();
            e.consume();
        });

        header.setOnMouseReleased(e -> {
            if (mDraggingWidget != null && mDropIndex != -1) {
                // Reorder
                int originalIndex = mWidgets.indexOf(mDraggingWidget);
                mWidgets.remove(originalIndex);

                // Adjust drop index if we removed from before the drop point
                int insertIndex = mDropIndex;
                if (originalIndex < insertIndex) {
                    insertIndex--;
                }

                if (insertIndex > mWidgets.size() - 1) {
                     insertIndex = mWidgets.size() - 1; // Keep it before the pinned widget
                }

                mWidgets.add(insertIndex, mDraggingWidget);

                // Save new order
                for (int i = 0; i < mWidgets.size(); i++) {
                    mPreference.setWidgetOrder(mWidgets.get(i).getWidgetId(), i);
                }
            }

            if (mDraggingWidget != null) {
                mDraggingWidget.setDragging(false);
            }
            mDraggingWidget = null;
            mDropIndex = -1;
            rebuildLayout();
            e.consume();
        });
    }
}
