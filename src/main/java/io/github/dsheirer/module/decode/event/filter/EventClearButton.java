


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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Slider;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.CustomMenuItem;






public class EventClearButton extends SplitMenuButton
    {
        private static final long serialVersionUID = 1L;
        private EventClearHandler mEventClearHandler;

        public EventClearButton(int maxHistoryCount)
        {
            setText("Clear");

            VBox historyPanel = new VBox();

            historyPanel.getChildren().add(new Label("History Size:"));
            final Slider historySlider = initializeHistorySlider();
            Label valueLabel = new Label(String.valueOf(maxHistoryCount));

            historySlider.setValue(maxHistoryCount);
            historySlider.valueProperty().addListener((obs, oldV, arg0) -> {
                if (mEventClearHandler != null)
                {
                    mEventClearHandler.onHistoryLimitChanged(arg0.intValue());
                }

                valueLabel.setText(String.valueOf(historySlider.getValue()));
            });

            historyPanel.getChildren().add(historySlider);
            historyPanel.getChildren().add(valueLabel);
            CustomMenuItem historyItem = new CustomMenuItem(historyPanel);
            historyItem.setHideOnClick(false);
            getItems().add(historyItem);

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
            slider.setShowTickMarks(true);
            slider.setShowTickLabels(true);

            slider.setOnMouseClicked(e -> {
                if(e.getButton() == javafx.scene.input.MouseButton.PRIMARY && e.getClickCount() == 2)
                {
                    slider.setValue(500); //default
                }
            });
            return slider;
        }
    }