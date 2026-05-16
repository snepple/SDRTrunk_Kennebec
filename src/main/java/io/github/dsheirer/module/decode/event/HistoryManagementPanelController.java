package io.github.dsheirer.module.decode.event;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import java.util.function.Consumer;

public class HistoryManagementPanelController {
    @FXML
    private HBox root;
    @FXML
    private Button filterButton;
    @FXML
    private Button clearButton;
    @FXML
    private Slider historySlider;
    @FXML
    private Label historyValueLabel;

    private Runnable onFilterClick;
    private Runnable onClearClick;
    private Consumer<Integer> onHistorySizeChanged;

    public void initialize() {
        historySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int size = newVal.intValue();
            historyValueLabel.setText(String.valueOf(size));
            if (onHistorySizeChanged != null) {
                onHistorySizeChanged.accept(size);
            }
        });
    }

    public void setCallbacks(Runnable onFilterClick, Runnable onClearClick, Consumer<Integer> onHistorySizeChanged) {
        this.onFilterClick = onFilterClick;
        this.onClearClick = onClearClick;
        this.onHistorySizeChanged = onHistorySizeChanged;
    }

    public void setInitialHistorySize(int initialHistorySize) {
        historySlider.setValue(initialHistorySize);
        historyValueLabel.setText(String.valueOf(initialHistorySize));
    }

    @FXML
    private void handleFilterClick() {
        if (onFilterClick != null) {
            onFilterClick.run();
        }
    }

    @FXML
    private void handleClearClick() {
        if (onClearClick != null) {
            onClearClick.run();
        }
    }

    @FXML
    private void handleSliderMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            historySlider.setValue(ClearableHistoryModel.DEFAULT_HISTORY_SIZE);
        }
    }

    public void setDisable(boolean disable) {
        root.setDisable(disable);
    }
}