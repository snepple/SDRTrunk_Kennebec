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
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * History management panel with controls for managing item histories.
 */
public class HistoryManagementPanel<T> extends JPanel
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

    private JFXPanel mJfxPanel;
    private HistoryManagementView mView;

    // For Swing interoperability when showing the filter editor
    private JPanel mDummyAnchor;

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

        setLayout(new BorderLayout());

        mDummyAnchor = new JPanel();
        add(mDummyAnchor, BorderLayout.EAST);

        mJfxPanel = new JFXPanel();
        mJfxPanel.setPreferredSize(new Dimension(300, 38));
        add(mJfxPanel, BorderLayout.CENTER);

        Platform.runLater(this::initJavaFX);

        setEnabled(false);
    }

    private void initJavaFX() {
        mView = new HistoryManagementView(mModel.getHistorySize(), this::handleFilterClick, this::handleClearClick, this::handleHistorySizeChanged);

        Scene scene = new Scene(mView.getRoot());

        java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        mJfxPanel.setScene(scene);
    }

    private void handleFilterClick() {
        Platform.runLater(() -> {
            getFilterEditor().show();
            getFilterEditor().toFront();
        });
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
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        if (mView != null) {
            Platform.runLater(() -> mView.setDisable(!enabled));
        } else {
            // If view is not yet initialized, enqueue the disable update
            Platform.runLater(() -> {
                if (mView != null) {
                    mView.setDisable(!enabled);
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
            mFilterEditor = new FilterEditor<>(mFilterEditorTitle, mFilterSet);
        }

        return mFilterEditor;
    }
}
