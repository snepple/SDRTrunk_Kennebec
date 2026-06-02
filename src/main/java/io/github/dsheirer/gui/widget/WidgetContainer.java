

package io.github.dsheirer.gui.widget;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;


import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import io.github.dsheirer.preference.NowPlayingPreference;



import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WidgetContainer extends VBox {

    private final NowPlayingPreference mPreference;
    private final List<Widget> mWidgets = new ArrayList<>();

    private Widget mDraggingWidget = null;
    private int mDragStartY;
    private int mDropIndex = -1;

    public WidgetContainer(NowPlayingPreference preference) {
        mPreference = preference;
        // setLayout(new javafx.scene.layout.HBox(4));
        // addComponentListener(this);
    }

    public NowPlayingPreference getPreference() {
        return mPreference;
    }

    public void removeAll() {
        super.getChildren().clear();
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
            if (w1.getWidgetId().equals(pinnedWidgetId) && w2.getWidgetId().equals(pinnedWidgetId)) return 0;
            if (w1.getWidgetId().equals(pinnedWidgetId)) return 1;
            if (w2.getWidgetId().equals(pinnedWidgetId)) return -1;

            int order1 = mPreference.getWidgetOrder(w1.getWidgetId(), 999);
            int order2 = mPreference.getWidgetOrder(w2.getWidgetId(), 999);
            return Integer.compare(order1, order2);
        });

        rebuildLayout();
    }

    private void rebuildLayout() {
        super.getChildren().clear();
        for (int i = 0; i < mWidgets.size(); i++) {
            Widget w = mWidgets.get(i);

            if (mDraggingWidget != null && mDropIndex == i) {
                // Add a drop indicator placeholder
                // // indicator init removed
            }

            getChildren().add(w);
        }

        if (mDraggingWidget != null && mDropIndex == mWidgets.size()) {
            // indicator init removed
        }

        requestLayout();
        requestLayout();
    }

    public void setWidgetVisible(String id, boolean visible) {
        for (Widget w : mWidgets) {
            if (w.getWidgetId().equals(id)) {
                w.setVisible(visible);
                w.setManaged(visible);
                mPreference.setWidgetVisible(id, visible);
                requestLayout();
                requestLayout();
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
        javafx.scene.layout.HBox header = widget.getHeaderPanel();

        // drag adapter removed
    }


}
