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

import javafx.geometry.Pos;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class EventClearButton extends SplitMenuButton
{
    private EventClearHandler mEventClearHandler;

    public EventClearButton(int maxHistoryCount)
    {
        super();
        setText("Clear");

        HBox historyPanel = new HBox(5);
        historyPanel.setAlignment(Pos.CENTER_LEFT);

        historyPanel.getChildren().add(new Label("History Size:"));
        final Slider historySlider = initializeHistorySlider();
        Label valueLabel = new Label(String.valueOf(maxHistoryCount));

        historySlider.setValue(maxHistoryCount);
        historySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mEventClearHandler != null)
            {
                mEventClearHandler.onHistoryLimitChanged(newValue.intValue());
            }
            valueLabel.setText(String.valueOf(newValue.intValue()));
        });

        historyPanel.getChildren().addAll(historySlider, valueLabel);

        CustomMenuItem customMenuItem = new CustomMenuItem(historyPanel);
        customMenuItem.setHideOnClick(false);
        getItems().add(customMenuItem);

        /* This handles the click action on the main button. Clear messages */
        setOnAction(e -> {
            if (mEventClearHandler != null)
            {
                mEventClearHandler.onClearHistoryClicked();
            }
        });
    }

    public void setEventClearHandler(EventClearHandler eventClearHandler) {
        this.mEventClearHandler = eventClearHandler;
    }

    private Slider initializeHistorySlider()
    {
        final Slider slider = new Slider();
        slider.setMin(0);
        slider.setMax(2000);
        slider.setMajorTickUnit(500);
        slider.setMinorTickCount(0);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setSnapToTicks(true);

        // Disable click to avoid eating double click handler
        slider.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                slider.setValue(500); // default
                e.consume();
            }
        });

        return slider;
    }
}
