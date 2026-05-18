package io.github.dsheirer.gui.control;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.event.FocusListener;
import java.util.concurrent.CountDownLatch;
import java.util.function.UnaryOperator;

public class FrequencyTextField extends JFXPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrequencyTextField.class);
    private static final String REGEX = "^[0-9]{0,4}[.]?[0-9]{0,6}$";
    private static final String ZEROS_REGEX = "^0?([.]0{0,5})?$";
    private double mMinimum;
    private double mMaximum;

    private FrequencyTextFieldController controller;

    public FrequencyTextField(long minimum, long maximum, long current) {
        mMinimum = minimum / 1E6d;
        mMaximum = maximum / 1E6d;

        setPreferredSize(new Dimension(80, 26));

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/dsheirer/gui/control/FrequencyTextField.fxml"));
                StackPane root = loader.load();
                controller = loader.getController();

                UnaryOperator<TextFormatter.Change> filter = change -> {
                    String newText = change.getControlNewText();
                    if (isValid(newText)) {
                        return change;
                    }
                    return null;
                };

                controller.setupFilter(filter);

                double frequencyMHz = current / 1E6d;
                if (isValid(String.valueOf(frequencyMHz))) {
                    controller.fxTextField.setText(String.valueOf(frequencyMHz));
                }

                controller.fxTextField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        java.awt.event.FocusEvent event = new java.awt.event.FocusEvent(this, java.awt.event.FocusEvent.FOCUS_LOST);
                        for (FocusListener listener : getFocusListeners()) {
                            listener.focusLost(event);
                        }
                    } else {
                        java.awt.event.FocusEvent event = new java.awt.event.FocusEvent(this, java.awt.event.FocusEvent.FOCUS_GAINED);
                        for (FocusListener listener : getFocusListeners()) {
                            listener.focusGained(event);
                        }
                    }
                });

                Scene scene = new Scene(root);
                setScene(scene);
            } catch (Exception e) {
                LOGGER.error("Failed to load FrequencyTextField FXML", e);
            }
        });
    }

    public long getFrequency() {
        if (controller == null || controller.fxTextField == null) return 0;

        final String[] valueHolder = new String[1];
        if (Platform.isFxApplicationThread()) {
            valueHolder[0] = controller.fxTextField.getText();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                valueHolder[0] = controller.fxTextField.getText();
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            }
        }

        String value = valueHolder[0];
        if (value == null || value.isEmpty()) {
            return 0;
        }

        try {
            return (long)(Double.parseDouble(value) * 1E6d);
        } catch (Exception e) {
            LOGGER.error("Unable to parse frequency value from text [" + value + "] " + e.getLocalizedMessage());
        }

        return 0;
    }

    public void setFrequency(long frequency) {
        double frequencyMHz = frequency / 1E6d;

        if (isValid(String.valueOf(frequencyMHz))) {
            Platform.runLater(() -> {
                if (controller != null && controller.fxTextField != null) {
                    controller.fxTextField.setText(String.valueOf(frequencyMHz));
                }
            });
        } else {
            LOGGER.warn("Can't set frequency [" + frequency + "Hz / " + frequencyMHz + "MHz] with current minimum [" + mMinimum + "MHz] and maximum [" + mMaximum + "MHz] limits");
        }
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        Platform.runLater(() -> {
            if (controller != null && controller.fxTextField != null) {
                controller.fxTextField.setTooltip(new Tooltip(text));
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Platform.runLater(() -> {
            if (controller != null && controller.fxTextField != null) {
                controller.fxTextField.setDisable(!enabled);
            }
        });
    }

    public String getText() {
        if (controller == null || controller.fxTextField == null) return "";

        final String[] valueHolder = new String[1];
        if (Platform.isFxApplicationThread()) {
            valueHolder[0] = controller.fxTextField.getText();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                valueHolder[0] = controller.fxTextField.getText();
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
        }
        return valueHolder[0] != null ? valueHolder[0] : "";
    }

    private boolean isValid(String value) {
        if (value == null || value.isEmpty() || value.matches(ZEROS_REGEX)) {
            return true;
        }

        if (value.matches(REGEX)) {
            try {
                double frequency = Double.parseDouble(value);
                return mMinimum <= frequency && frequency <= mMaximum;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return false;
    }
}
