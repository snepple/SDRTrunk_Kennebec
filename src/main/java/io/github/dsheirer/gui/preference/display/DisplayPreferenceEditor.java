package io.github.dsheirer.gui.preference.display;

import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import io.github.dsheirer.preference.display.DisplayPreference;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.geometry.Insets;
import java.util.Arrays;
import java.util.stream.Collectors;

public class DisplayPreferenceEditor extends VBox {

    private DisplayPreference mDisplayPreference;

    public DisplayPreferenceEditor(DisplayPreference displayPreference) {
        mDisplayPreference = displayPreference;
        
        getStyleClass().add("settings-page");
        setSpacing(20);

        getChildren().add(createSpectrumCard());
        getChildren().add(createOverlayCard());
    }

    private Node createSpectrumCard() {
        VBox container = new VBox(5);
        Label title = new Label("Spectrum & Waterfall");
        title.getStyleClass().add("kennebec-section-title");
        title.setPadding(new Insets(10, 0, 5, 0));
        SettingsCard card = new SettingsCard();

        // Waterfall Height
        Spinner<Integer> heightSpinner = new Spinner<>(100, 2000, mDisplayPreference.waterfallImageHeightProperty().get(), 10);
        heightSpinner.setEditable(true);
        heightSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                mDisplayPreference.waterfallImageHeightProperty().set(newV);
            }
        });
        card.getChildren().add(new SettingsRow("Waterfall Height (px)", heightSpinner));

        // Spectrum Inset
        Spinner<Double> insetSpinner = new Spinner<>(0.0, 100.0, mDisplayPreference.spectrumInsetProperty().get(), 1.0);
        insetSpinner.setEditable(true);
        insetSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                mDisplayPreference.spectrumInsetProperty().set(newV);
            }
        });
        card.getChildren().add(new SettingsRow("Spectrum Inset (px)", insetSpinner));

        // FFT Window Type
        ComboBox<String> windowTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
            Arrays.stream(WindowType.values()).map(Enum::name).collect(Collectors.toList())
        ));
        windowTypeCombo.setValue(mDisplayPreference.getFFTWindowType());
        windowTypeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                mDisplayPreference.setFFTWindowType(newV);
            }
        });
        card.getChildren().add(new SettingsRow("FFT Window Type", windowTypeCombo));

        container.getChildren().addAll(title, card);
        return container;
    }

    private Node createOverlayCard() {
        VBox container = new VBox(5);
        Label title = new Label("Overlay Layout");
        title.getStyleClass().add("kennebec-section-title");
        title.setPadding(new Insets(20, 0, 5, 0));
        SettingsCard card = new SettingsCard();

        // Label Width
        Spinner<Double> labelWidthSpinner = new Spinner<>(10.0, 200.0, mDisplayPreference.overlayLabelWidthProperty().get(), 1.0);
        labelWidthSpinner.setEditable(true);
        labelWidthSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                mDisplayPreference.overlayLabelWidthProperty().set(newV);
            }
        });
        card.getChildren().add(new SettingsRow("Label Width (px)", labelWidthSpinner));

        // Label Height
        Spinner<Double> labelHeightSpinner = new Spinner<>(5.0, 100.0, mDisplayPreference.overlayLabelHeightProperty().get(), 1.0);
        labelHeightSpinner.setEditable(true);
        labelHeightSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                mDisplayPreference.overlayLabelHeightProperty().set(newV);
            }
        });
        card.getChildren().add(new SettingsRow("Label Height (px)", labelHeightSpinner));

        container.getChildren().addAll(title, card);
        return container;
    }
}
