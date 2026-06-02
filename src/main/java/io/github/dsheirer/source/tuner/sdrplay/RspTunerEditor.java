/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.geometry.Orientation;


import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import io.github.dsheirer.util.ThreadPool;

import javafx.application.Platform;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;

/**
 * Abstract RSP tuner editor
 */
public abstract class RspTunerEditor<C extends RspTunerConfiguration> extends TunerEditor<RspTuner,C> implements ITunerStatusListener
{
    private Logger mLog = LoggerFactory.getLogger(RspTunerEditor.class);
    protected static final String MANUAL = "Manual";
    protected static final String AUTOMATIC = "Automatic";
    private ToggleButton mAgcButton;
    private Label mGainValueLabel;
    private LnaSlider mLNASlider;
    private IfGainSlider mIfGainSlider;
    private Button mGainOverloadButton;
    private VBox mGainPanel;
    private AtomicBoolean mGainOverloadAlert = new AtomicBoolean();

    /**
     * Constructs an instance
     * @param userPreferences for preference settings
     * @param tunerManager to notify for state changes
     * @param discoveredTuner to be edited.
     */
    public RspTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
    }

    private RspTunerController getTunerController()
    {
        return (RspTunerController) getTuner().getTunerController();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return RspTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return RspTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Gain controls panel
     * @return gain panel
     */
    protected VBox getGainPanel()
    {
        if(mGainPanel == null)
        {
            mGainPanel = new VBox();
            mGainPanel.getChildren().add(getGainValueLabel());
            mGainPanel.getChildren().add(getGainOverloadButton());
            mGainPanel.getChildren().add(new Label()); //empty label to grow to fill space
            mGainPanel.getChildren().add(new Label("IF Gain Mode:"));
            mGainPanel.getChildren().add(getAgcButton());
        }

        return mGainPanel;
    }

    /**
     * Label for displaying current gain index value
     */
    protected Label getGainValueLabel()
    {
        if(mGainValueLabel == null)
        {
            mGainValueLabel = new Label("0");
            mGainValueLabel.setDisable(!(false));
        }

        return mGainValueLabel;
    }

    /**
     * IF AGC mode (enable/disable) toggle button
     */
    protected ToggleButton getAgcButton()
    {
        if(mAgcButton == null)
        {
            mAgcButton = new ToggleButton(MANUAL);
            mAgcButton.setDisable(!(false));
            mAgcButton.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setAgcMode(mAgcButton.isSelected() ? AgcMode.ENABLE : AgcMode.DISABLE);
                        updateGainLabel();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting AGC mode on RSP device");
                    }
                    save();
                }

                getIfGainSlider().setDisable(!getAgcButton().isSelected());

                if(mAgcButton.isSelected())
                {
                    getAgcButton().setText(AUTOMATIC);
                }
                else
                {
                    getAgcButton().setText(MANUAL);
                }
            });
        }

        return mAgcButton;
    }

    /**
     * Gain overload button.  Used in a disabled state to indicate (e.g. flashing color) that gain overload is detected.
     */
    protected Button getGainOverloadButton()
    {
        if(mGainOverloadButton == null)
        {
            mGainOverloadButton = new Button("Gain Overload");
            mGainOverloadButton.setTooltip(new javafx.scene.control.Tooltip("Notification that manual gain is set too high and causing power overload.  Reduce manual gain when this flashes."));
            mGainOverloadButton.setDisable(!(false));
        }

        return mGainOverloadButton;
    }

    @Override
    public void notifyGainOverload(TunerSelect tunerSelect)
    {
        if(hasTuner() && getTuner().getRspTunerController().getTunerSelect() == tunerSelect)
        {
            //Set overload alert
            // Platform.runLater(() -> setGainOverloadAlert(true));

            //Schedule a reset to happen 1 second later
            // ThreadPool.SCHEDULED.schedule((Runnable) () -> setGainOverloadAlert(false), 600, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void frequencyUpdated()
    {
        updateLnaSlider();
    }

    /**
     * Toggles the alert styling of the disabled gain overload button to indicate an alert condition or a normal
     * operating condition.
     * @param alert true to apply alert styling or false to reset.
     */
    // private void // setGainOverloadAlert(boolean alert)
//     // {
//         if(alert && mGainOverloadAlert.compareAndSet(false, true))
//         {
//             getGainOverloadButton().setDisable(!(true));
//             getGainOverloadButton().setForeground(Color.YELLOW);
//             getGainOverloadButton().setBackground(Color.RED);
//         }
//         else if(!alert && mGainOverloadAlert.compareAndSet(true, false))
//         {
//             getGainOverloadButton().setDisable(!(false));
//             getGainOverloadButton().setForeground(getForeground());
//             getGainOverloadButton().setBackground(getBackground());
//         }
//     }
// 
    /**
     * Updates the LNA slider with the number of LNA states available for the current frequency.
     */
    protected void updateLnaSlider()
    {
        if(hasTuner())
        {
            int max = getTunerController().getControlRsp().getMaximumLNASetting();
            if(max != getLNASlider().getMax())
            {
                Platform.runLater(() -> {
                    setLoading(true);
                    //Adjust the value if it's less than max
                    getLNASlider().setValue(Math.min(getLNASlider().getValue(), max));
                    getLNASlider().setMax(max);
                    setLoading(false);
                });
            }
        }
    }

    /**
     * LNA Gain Slider
     */
    protected LnaSlider getLNASlider()
    {
        if(mLNASlider == null)
        {
            mLNASlider = new LnaSlider();
            mLNASlider.setDisable(!(true));
            mLNASlider.setMajorTickUnit(1);
            mLNASlider.setShowTickMarks(true);
            mLNASlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                Slider source = (Slider) ((javafx.beans.property.ReadOnlyProperty)obs).getBean();

                if(!source.isValueChanging())
                {
                    updateGain();
                }
            });
        }

        return mLNASlider;
    }

    /**
     * Updates the gain settings when the LNA or Baseband gain value is changed by the user.
     */
    private void updateGain()
    {
        int lna = getLNASlider().getLNA();
        int gr = getIfGainSlider().getGR();

        if(hasTuner() && !isLoading())
        {
            try
            {
                getTunerController().getControlRsp().setGain(lna, gr);
                save();
                updateGainLabel();
            }
            catch(Exception e)
            {
                mLog.error("Couldn't set RSP gain to LNA:" + lna + " Gain Reduction:" + gr, e);
                Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setContentText(String.valueOf("Couldn't set RSP gain value to LNA:" + lna + " Gain Reduction:" + gr)); alert.showAndWait(); });
            }
        }
    }

    protected void updateGainLabel()
    {
        try
        {
            float currentGain = getTunerController().getControlRsp().getCurrentGain();
            getGainValueLabel().setText((int)currentGain + " dB");
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error accessing current gain value from RSP tuner", se);
        }
    }

    /**
     * Creates a standardized help icon button with the provided tooltip text.
     */
    protected Button createHelpIcon(String text) {
        Button button = new Button(text);
        // Style as a subtle flat button
        // button.setPadding(new javafx.geometry.Insets(0, 4, 0, 4));
        // button.setFocusPainted(false);
        // button.setContentAreaFilled(false);
        button.setBackground(javafx.scene.layout.Background.EMPTY);
        return button;
    }

    /**
     * IF Gain Slider
     */
    protected IfGainSlider getIfGainSlider()
    {
        if(mIfGainSlider == null)
        {
            mIfGainSlider = new IfGainSlider();
            mIfGainSlider.setDisable(!(true));
            mIfGainSlider.setMajorTickUnit(1);
            mIfGainSlider.setShowTickMarks(true);
            mIfGainSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                Slider source = (Slider) ((javafx.beans.property.ReadOnlyProperty)obs).getBean();

                if(!source.isValueChanging())
                {
                    updateGain();
                }
            });
        }

        return mIfGainSlider;
    }

    /**
     * Slider implementation that inverts the scale to support LNA values.
     */
    public class LnaSlider extends Slider
    {
        /**
         * Constructs an instance
         */
        public LnaSlider()
        {
            super( 0,9,9);
        }

        /**
         * Slider value converted to the LNA scale.
         * @return lna value.
         */
        public int getLNA()
        {
             return (int)((int)(getMax() - getValue()));
        }

        /**
         * Sets the slider with the value converted from the provided LNA value.
         * @param lna value to apply
         */
        public void setLNA(int lna)
        {
            setValue(getMax() - lna);
        }
    }

    /**
     * Slider implementation that inverts the scale to support IF Gain (aka Gain Reduction) values.
     */
    public class IfGainSlider extends Slider
    {
        /**
         * Constructs an instance
         */
        public IfGainSlider()
        {
            super( 0, 39, 30);
        }

        /**
         * Slider value converted to the gain reduction scale.
         * @return gain reduction value.
         */
        public int getGR()
        {
            return (int)((int)getIfGainSlider().getMax() - (int)getIfGainSlider().getValue() + 20);
        }

        /**
         * Sets the slider with the value converted from the provided gain reduction value.
         * @param gainReduction value to apply
         */
        public void setGR(int gainReduction)
        {
            setValue(getMax() - (gainReduction - 20));
        }
    }
}
