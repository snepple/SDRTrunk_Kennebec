/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.event.filter;

import com.jidesoft.swing.JideButton;
import io.github.dsheirer.filter.FilterEditorPanel;
import io.github.dsheirer.filter.FilterSet;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javax.swing.JScrollPane;

/**
 * Event filter button that includes split button functionality to allow user to select filter items.
 *
 * @param <T> type of filter.
 */
public class EventFilterButton<T> extends JideButton
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an instance
     * @param dialogTitle for the dialog/panel that appears
     * @param filterSet to include in the editor panel.
     */
    public EventFilterButton(String dialogTitle, FilterSet<T> filterSet)
    {
        this("Filter", dialogTitle, filterSet);
    }

    /**
     * Constructs an instance
     * @param buttonLabel to use on the button
     * @param dialogTitle for the dialog/panel that appears
     * @param filterSet to include in the editor panel.
     */
    public EventFilterButton(String buttonLabel, String dialogTitle, FilterSet<T> filterSet)
    {
        super(buttonLabel);
        addActionListener(new EventFilterActionHandler(dialogTitle, filterSet));
    }

    /**
     * Action handler for the button
     */
    public class EventFilterActionHandler implements ActionListener
    {
        private String mTitle;
        private FilterSet<T> mFilterSet;

        /**
         * Constructs an instance
         * @param title for this panel
         * @param filterSet to edit
         */
        public EventFilterActionHandler(String title, FilterSet<T> filterSet)
        {
            mTitle = title;
            mFilterSet = filterSet;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            JPanel wrapperPanel = new JPanel();
            wrapperPanel.setLayout(new MigLayout("", "[grow,fill]", "[grow,fill][][]"));
            FilterEditorPanel<T> panel = new FilterEditorPanel<T>(mFilterSet);
        javafx.embed.swing.JFXPanel jfxPanel = new javafx.embed.swing.JFXPanel();
        javafx.application.Platform.runLater(() -> jfxPanel.setScene(new javafx.scene.Scene(panel)));
            javafx.embed.swing.SwingNode node = new javafx.embed.swing.SwingNode();
            javax.swing.SwingUtilities.invokeLater(() -> node.setContent(new javax.swing.JPanel())); // placeholder
            JScrollPane scroller = new JScrollPane(new javax.swing.JPanel());
            /* wrapped */
            wrapperPanel.add(scroller, "wrap");
            JButton close = new JButton("Close");
            close.setToolTipText("Close the filter editor");
            close.getAccessibleContext().setAccessibleName("Close Event Filter Editor");
            close.getAccessibleContext().setAccessibleDescription("Closes the event filter editor dialog");

            Platform.runLater(() -> {
                Stage stage = new Stage();
                stage.setTitle(mTitle);
                stage.setWidth(600);
                stage.setHeight(400);

                close.addActionListener(e1 -> Platform.runLater(() -> stage.close()));
                wrapperPanel.add(close);

                SwingNode swingNode = new SwingNode();
                SwingUtilities.invokeLater(() -> swingNode.setContent(wrapperPanel));

                Scene scene = new Scene(new javafx.scene.layout.StackPane(swingNode));
                stage.setScene(scene);
                stage.show();
            });
        }
    }
}