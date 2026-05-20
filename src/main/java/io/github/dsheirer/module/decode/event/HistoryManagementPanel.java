/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.filter.FilterEditor;
import io.github.dsheirer.filter.FilterSet;
import javafx.application.Platform;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.LoggerFactory;

// import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * History management panel with controls for managing item histories.
 */
public class HistoryManagementPanel<T> extends javafx.scene.layout.VBox
{
    private ClearableHistoryModel mModel;
    private FilterSet<T> mFilterSet;
    private FilterEditor<T> mFilterEditor;
    private String mFilterEditorTitle;

    /**
     * Optional callback invoked (on the slider's change listener) each time
     * the user moves the slider, so the caller can persist the new value.
     */
    private Consumer<Integer> mHistorySizeChangedCallback;

    private HistoryManagementPanelController mController;

    // For Swing interoperability when showing the filter editor
    private java.awt.Component mDummyAnchor;

    /**
     * Constructs an instance using the model's current history size as the
     * initial slider position.  No persistence callback is installed.
     *
     * @param model             to manage
     * @param filterEditorTitle title for the filter editor dialog
     */
    public HistoryManagementPanel(ClearableHistoryModel model, String filterEditorTitle)
    {
        this(model, filterEditorTitle, model.getHistorySize(), null);
    }

    /**
     * Constructs an instance with a caller-supplied initial slider value and
     * an optional callback for persistence.
     *
     * @param model                      to manage
     * @param filterEditorTitle          title for the filter editor dialog
     * @param initialHistorySize         slider starting position (from saved prefs)
     * @param historySizeChangedCallback called with the new value every time
     *                                   the slider moves; may be {@code null}
     */
    public HistoryManagementPanel(ClearableHistoryModel model, String filterEditorTitle,
                                  int initialHistorySize, Consumer<Integer> historySizeChangedCallback)
    {
        mModel = model;
        mFilterEditorTitle = filterEditorTitle;
        mHistorySizeChangedCallback = historySizeChangedCallback;

        if(initialHistorySize != model.getHistorySize())
        {
            model.setHistorySize(initialHistorySize);
        }

        mDummyAnchor = new java.awt.Panel();
        // Since we are now a JFXPanel, we don't add the dummy anchor directly.
        // It's just used as a component reference for the FilterEditor dialog location.

        // setPreferredSize(new Dimension(300, 38));

        Platform.runLater(this::initJavaFX);

        setDisable(true);
    }

    private void initJavaFX() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/module/decode/event/HistoryManagementPanel.fxml"));
            Parent root = loader.load();

            mController = loader.getController();
            mController.setCallbacks(this::handleFilterClick, this::handleClearClick, this::handleHistorySizeChanged);
            mController.setInitialHistorySize(mModel.getHistorySize());
            mController.setDisable(isDisabled());

            java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
            if (cssUrl != null) {
                root.getStylesheets().add(cssUrl.toExternalForm());
            }
            getChildren().add(root);
        } catch (Exception e) {
            LoggerFactory.getLogger(HistoryManagementPanel.class).error("Error loading HistoryManagementPanel FXML", e);
        }
    }

    private void handleFilterClick() {
        SwingUtilities.invokeLater(() -> javafx.application.Platform.runLater(() -> getFilterEditor().show()));
    }

    private void handleClearClick() {
        SwingUtilities.invokeLater(() -> mModel.clear());
    }

    private void handleHistorySizeChanged(int size) {
        SwingUtilities.invokeLater(() -> {
            mModel.setHistorySize(size);
            if (mHistorySizeChangedCallback != null) {
                mHistorySizeChangedCallback.accept(size);
            }
        });
    }

    /**
     * Updates the filter set
     * @param filterSet to use
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        mFilterSet = filterSet;

        if(mFilterEditor != null)
        {
            mFilterEditor.updateFilterSet(filterSet);
        }
    }

    /**
     * Overrides the panel method to also set the enabled state for the child controls.
     * @param enabled true if this component should be enabled, false otherwise
     */
    public void setEnabled(boolean enabled) {
        setDisable(!enabled);
        if (mController != null) {
            Platform.runLater(() -> mController.setDisable(!enabled));
        } else {
            // If view is not yet initialized, enqueue the disable update
            Platform.runLater(() -> {
                if (mController != null) {
                    mController.setDisable(!enabled);
                }
            });
        }
    }

    /**
     * Filter editor
     * @return editor lazily constructed
     */
    private FilterEditor<T> getFilterEditor()
    {
        if(mFilterEditor == null)
        {
            mFilterEditor = new FilterEditor<>(mFilterEditorTitle, mDummyAnchor, mFilterSet);
        }

        return mFilterEditor;
    }
}
