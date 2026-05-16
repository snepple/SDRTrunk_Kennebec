package io.github.dsheirer.module.decode.event.filter;

import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.control.CustomMenuItem;
import javafx.geometry.Pos;

public class EventClearButton extends SplitMenuButton {

    private EventClearHandler mEventClearHandler;

    public EventClearButton(int maxHistoryCount) {
        setText("Clear");

        HBox historyPanel = new HBox(5);
        historyPanel.setAlignment(Pos.CENTER_LEFT);

        historyPanel.getChildren().add(new Label("History Size:"));

        Slider historySlider = new Slider(0, 2000, maxHistoryCount);
        historySlider.setMajorTickUnit(500);
        historySlider.setShowTickMarks(true);
        historySlider.setShowTickLabels(true);

        Label valueLabel = new Label(String.valueOf(maxHistoryCount));

        historySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            if (mEventClearHandler != null) {
                mEventClearHandler.onHistoryLimitChanged(val);
            }
            valueLabel.setText(String.valueOf(val));
        });

        historySlider.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                historySlider.setValue(500); // default
            }
        });

        historyPanel.getChildren().addAll(historySlider, valueLabel);

        CustomMenuItem customMenuItem = new CustomMenuItem(historyPanel, false);
        getItems().add(customMenuItem);

        setOnAction(e -> {
            if (mEventClearHandler != null) {
                mEventClearHandler.onClearHistoryClicked();
            }
        });
    }

    public void setEventClearHandler(EventClearHandler eventClearHandler) {
        this.mEventClearHandler = eventClearHandler;
    }
}
