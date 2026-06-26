


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

package io.github.dsheirer.gui.playlist.channel;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import javafx.scene.control.Button;
import io.github.dsheirer.record.AudioRecordingManager;
import java.nio.file.Path;
import io.github.dsheirer.controller.channel.Channel;

import io.github.dsheirer.gui.control.HexFormatter;
import io.github.dsheirer.gui.control.IntegerFormatter;
import io.github.dsheirer.gui.playlist.decoder.AuxDecoderConfigurationEditor;
import io.github.dsheirer.gui.playlist.eventlog.EventLogConfigurationEditor;
import io.github.dsheirer.gui.playlist.source.FrequencyEditor;
import io.github.dsheirer.gui.playlist.source.SourceConfigurationEditor;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.config.ChannelToneFilter;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.IntegerFormat;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfiguration;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tab;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Tab;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.ToggleSwitch;

import jiconfont.javafx.IconNode;
import jiconfont.icons.font_awesome.FontAwesome;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import io.github.dsheirer.module.decode.nbfm.ai.AIAudioOptimizer;
import io.github.dsheirer.module.decode.nbfm.ai.AIAnalysisResult;
import io.github.dsheirer.module.decode.nbfm.ai.AudioBufferManager;

/**
 * Narrow-Band FM channel configuration editor
 */
public class NBFMConfigurationEditor extends ChannelConfigurationEditor
{
    private javafx.scene.Node mAuxDecoderPane;
    private javafx.scene.Node mDecoderPane;
    private javafx.scene.Node mEventLogPane;
    private javafx.scene.Node mRecordPane;
    private javafx.scene.Node mSourcePane;
    private TextField mTalkgroupField;
    private ToggleSwitch mAudioFilterEnable;
    private TextFormatter<Integer> mTalkgroupTextFormatter;
    private ToggleSwitch mBasebandRecordSwitch;
    private SegmentedButton mBandwidthButton;

    // CTCSS/DCS Tone Filter UI
    private javafx.scene.Node mToneFilterPane;
    private ToggleSwitch mToneFilterEnabledSwitch;
    private ComboBox<ChannelToneFilter.ToneType> mToneTypeCombo;
    private Spinner<Integer> mToneMinCallDurationSpinner;
    private Spinner<Integer> mMinCallDurationSpinner;
    private ToggleSwitch mToneRequireNoiseSquelchSwitch;
    private ComboBox<CTCSSCode> mCtcssCodeCombo;
    private ComboBox<DCSCode> mDcsCodeCombo;

    // Squelch Tail Removal UI

    private ToggleSwitch mSquelchTailEnabledSwitch;
    private Spinner<Integer> mTailRemovalSpinner;
    private Spinner<Integer> mHeadRemovalSpinner;
    private Spinner<Integer> mAudioHangtimeSpinner;

    // Audio Filters UI
    private javafx.scene.Node mAudioFiltersPane;
    private Slider mInputGainSlider;
    private TextField mInputGainField;
    private ToggleSwitch mLowPassEnabledSwitch;
    private Slider mLowPassCutoffSlider;
    private TextField mLowPassCutoffField;
    // De-emphasis removed from UI — config fields preserved for backward compatibility
    private ToggleSwitch mVoiceEnhanceEnabledSwitch;
    private Slider mVoiceEnhanceSlider;
    private TextField mVoiceEnhanceField;
    private ToggleSwitch mBassBoostEnabledSwitch;
    private Slider mBassBoostSlider;
    private TextField mBassBoostField;
    private ToggleSwitch mHissReductionEnabledSwitch;
    private Slider mHissReductionDbSlider;
    private TextField mHissReductionDbField;
    private Slider mHissReductionCornerSlider;
    private TextField mHissReductionCornerField;
    private ToggleSwitch mSquelchEnabledSwitch;
    private Slider mSquelchThresholdSlider;
    private TextField mSquelchThresholdField;
    private Slider mSquelchReductionSlider;
    private TextField mSquelchReductionField;
    private Slider mHoldTimeSlider;
    private TextField mHoldTimeField;
    private javafx.scene.control.Button mAIOptimizeButton;
    private Label mAIOptimizeStatusLabel;

    private boolean mLoadingConfiguration = false;
    //True only while AI-suggested values are being applied programmatically, so that does not get
    //mistaken for a manual filter edit.
    private boolean mApplyingAiResult = false;
    //Notification shown in the Audio Filters pane when AI auto-optimization is paused due to manual edits.
    private HBox mAiLockoutBanner;

    private SourceConfigurationEditor mSourceConfigurationEditor;
    private AuxDecoderConfigurationEditor mAuxDecoderConfigurationEditor;
    private EventLogConfigurationEditor mEventLogConfigurationEditor;
    private final TalkgroupValueChangeListener mTalkgroupValueChangeListener = new TalkgroupValueChangeListener();
    private final IntegerFormatter mDecimalFormatter = new IntegerFormatter(1, -1);
    private final HexFormatter mHexFormatter = new HexFormatter(1, -1);

    /**
     * Constructs an instance
     * @param playlistManager for playlists
     * @param tunerManager for tuners
     * @param userPreferences for preferences
     */
    public NBFMConfigurationEditor(PlaylistManager playlistManager, TunerManager tunerManager,
                                   UserPreferences userPreferences, IFilterProcessor filterProcessor)
    {
        super(playlistManager, tunerManager, userPreferences, filterProcessor);
        // Could not find name for getSourcePane()
        addConfigurationPane("Source", getSourcePane());
        // Could not find name for getDecoderPane()
        addConfigurationPane("Decoder", getDecoderPane());
        // Could not find name for getToneFilterPane()
        addConfigurationPane("Tone Filter", getToneFilterPane());
        // Could not find name for getAudioFiltersPane()
        addConfigurationPane("Audio Filters", getAudioFiltersPane());
        // Could not find name for getAuxDecoderPane()
        addConfigurationPane("Additional Decoders", getAuxDecoderPane());
        // Could not find name for getEventLogPane()
        addConfigurationPane("Logging", getEventLogPane());
        // Could not find name for getRecordPane()
        addConfigurationPane("Recording", getRecordPane());
        setupAlertsPane();
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NBFM;
    }

    private javafx.scene.Node getSourcePane(){
        if(mSourcePane == null)
        {
            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(getSourceConfigurationEditor());
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mSourcePane = sp;

        }

        return mSourcePane;
    }

    private javafx.scene.Node getDecoderPane(){
        if(mDecoderPane == null)
        {
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            HBox talkgroupBox = new HBox(5);
            talkgroupBox.setAlignment(Pos.CENTER_LEFT);
            talkgroupBox.getChildren().add(getTalkgroupField());

            javafx.scene.control.Button generateIdButton = new javafx.scene.control.Button("Generate ID");
            generateIdButton.getStyleClass().add("flat-button");
            generateIdButton.setOnAction(event -> {
                GeographicSchemaGenerator generator = new GeographicSchemaGenerator(getItem(), mUserPreferences, getPlaylistManager());
                generator.showAndWait().ifPresent(id -> {
                    // Parse as unsigned int since 10-digit IDs can exceed Integer.MAX_VALUE
                    mTalkgroupTextFormatter.setValue(Integer.parseUnsignedInt(id));
                });
            });
            talkgroupBox.getChildren().add(generateIdButton);

            //General minimum call duration - applies to ALL calls (with or without tone filtering). Calls shorter
            //than this are discarded so sub-second static bursts never reach the Events table.
            mMinCallDurationSpinner = new Spinner<>(0, 5000, 0, 100);
            mMinCallDurationSpinner.setEditable(true);
            mMinCallDurationSpinner.setPrefWidth(110);
            mMinCallDurationSpinner.setTooltip(new Tooltip("Discard calls shorter than this (milliseconds). 0 = off."));
            mMinCallDurationSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });

            SettingsCard decoderCard = new SettingsCard();
            decoderCard.getChildren().addAll(
                new SettingsRow("Channel Bandwidth", createHelpIcon("NBFM (Narrow-Band FM) channel width determines how much radio spectrum is decoded.\n\u2022 12.5 kHz = Standard narrow-band (most modern radios)\n\u2022 25 kHz = Wide-band (older or commercial radios)\nIf you hear distorted or chopped audio, try the other setting."), getBandwidthButton()),
                new SettingsRow("Talkgroup To Assign", createHelpIcon("Talkgroup ID: A numeric address used to identify a specific group of radio users.\nFor NBFM channels without trunking, this manually assigns a talkgroup number\nso that alias rules (listen/record/stream) can be applied to this channel's audio.\nLeave blank to use the auto-detected talkgroup (if available)."), talkgroupBox),
                new SettingsRow("Min Call Duration (ms)", createHelpIcon("Discards any call shorter than this many milliseconds, so brief sub-second\nstatic bursts never appear in the Events table or get streamed. Applies to\nall calls (with or without a tone filter). This also keeps the limited Events\ntable from filling with static and evicting real calls before their\ntranscriptions arrive. 0 disables this (default)."), mMinCallDurationSpinner)
            );
            content.getChildren().add(decoderCard);

            javafx.scene.control.ScrollPane mDecoderPaneSp = new javafx.scene.control.ScrollPane(content);
            mDecoderPaneSp.setFitToWidth(true);
            mDecoderPaneSp.setFitToHeight(true);
            mDecoderPaneSp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mDecoderPane = mDecoderPaneSp;

            //Special handling - the pill button doesn't like to set a selected state if the pane is not expanded,
            //so detect when the pane is expanded and refresh the config view

        }

        return mDecoderPane;
    }

    // === Tone Filter (CTCSS / DCS) pane ===
    private javafx.scene.Node getToneFilterPane(){
        if(mToneFilterPane == null)
        {
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            mToneFilterEnabledSwitch = new ToggleSwitch();
            mToneFilterEnabledSwitch.selectedProperty().addListener((obs, ov, nv) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });

            mToneTypeCombo = new ComboBox<>();
            mToneTypeCombo.getItems().addAll(ChannelToneFilter.ToneType.CTCSS, ChannelToneFilter.ToneType.DCS);
            mToneTypeCombo.setValue(ChannelToneFilter.ToneType.CTCSS);
            mToneTypeCombo.valueProperty().addListener((obs, ov, nv) -> {
                updateToneCodeVisibility();
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });

            // CTCSS code selector
            mCtcssCodeCombo = new ComboBox<>();
            mCtcssCodeCombo.getItems().addAll(CTCSSCode.STANDARD_CODES);
            mCtcssCodeCombo.setPromptText("Select PL tone");
            mCtcssCodeCombo.setPrefWidth(200);
            mCtcssCodeCombo.valueProperty().addListener((obs, ov, nv) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });

            // DCS code selector (hidden by default)
            mDcsCodeCombo = new ComboBox<>();
            mDcsCodeCombo.getItems().addAll(DCSCode.STANDARD_CODES);
            mDcsCodeCombo.getItems().addAll(DCSCode.INVERTED_CODES);
            mDcsCodeCombo.setPromptText("Select DCS code");
            mDcsCodeCombo.setPrefWidth(200);
            mDcsCodeCombo.setVisible(false);
            mDcsCodeCombo.setManaged(false);
            mDcsCodeCombo.valueProperty().addListener((obs, ov, nv) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });

            // Code selector box stacks CTCSS and DCS combos (only one visible at a time)
            HBox codeBox = new HBox(5);
            codeBox.setAlignment(Pos.CENTER_LEFT);
            codeBox.getChildren().addAll(mCtcssCodeCombo, mDcsCodeCombo);

            // False-trigger suppression: minimum call duration (drops brief static bursts that carry the tone)
            mToneMinCallDurationSpinner = new Spinner<>(0, 5000, 0, 100);
            mToneMinCallDurationSpinner.setEditable(true);
            mToneMinCallDurationSpinner.setPrefWidth(110);
            mToneMinCallDurationSpinner.setTooltip(new Tooltip("Drop tone matches shorter than this (milliseconds). " +
                    "0 = off."));
            mToneMinCallDurationSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });

            // False-trigger suppression: also require the noise squelch open (tone AND carrier)
            mToneRequireNoiseSquelchSwitch = new ToggleSwitch();
            mToneRequireNoiseSquelchSwitch.selectedProperty().addListener((obs, ov, nv) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });

            SettingsCard toneCard = new SettingsCard();
            toneCard.getChildren().addAll(
                new SettingsRow("Enable Tone Filter", createHelpIcon("When enabled, this channel will only pass audio when the selected tone is detected.\nUse this to reduce false triggering on busy repeaters."), mToneFilterEnabledSwitch),
                new SettingsRow("Tone Type", createHelpIcon("CTCSS (Continuous Tone-Coded Squelch System):\n  A sub-audible tone below 300 Hz transmitted alongside voice.\n  Also called PL Tone (Private Line) or Sub-Tone.\n\nDCS (Digital-Coded Squelch):\n  A digital bit pattern used instead of a tone.\n  Also called DPL (Digital Private Line)."), mToneTypeCombo, codeBox),
                new SettingsRow("Min Call Duration (ms)", createHelpIcon("Drops tone matches shorter than this many milliseconds - e.g. brief static\nbursts that momentarily carry the correct tone (the Sidney Fire problem).\nBuffered lead-in audio is released once a call qualifies, so a real call's\nstart is not clipped. 0 disables this (default)."), mToneMinCallDurationSpinner),
                new SettingsRow("Require Noise Squelch", createHelpIcon("Also require the noise squelch to be open (tone AND carrier).\nWith this ON (default), noisy static that briefly fools the tone detector\ncan't open the channel because its high noise keeps the hysteresis-protected\nnoise squelch closed - this is what prevents sub-second static calls and\nmatches upstream behavior.\nTurn OFF only for pure tone-squelch if a mistuned noise squelch is\nsilencing a channel."), mToneRequireNoiseSquelchSwitch)
            );
            content.getChildren().add(toneCard);

            javafx.scene.control.ScrollPane mToneFilterPaneSp = new javafx.scene.control.ScrollPane(content);
            mToneFilterPaneSp.setFitToWidth(true);
            mToneFilterPaneSp.setFitToHeight(true);
            mToneFilterPaneSp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mToneFilterPane = mToneFilterPaneSp;
        }
        return mToneFilterPane;
    }

    private void updateToneCodeVisibility()
    {
        boolean isCTCSS = mToneTypeCombo.getValue() == ChannelToneFilter.ToneType.CTCSS;
        mCtcssCodeCombo.setVisible(isCTCSS);
        mCtcssCodeCombo.setManaged(isCTCSS);
        mDcsCodeCombo.setVisible(!isCTCSS);
        mDcsCodeCombo.setManaged(!isCTCSS);
    }

    // === Audio Filters pane ===
    private javafx.scene.Node getAudioFiltersPane(){
        if(mAudioFiltersPane == null)
        {
            VBox contentBox = new VBox(10);
            contentBox.setPadding(new Insets(10,10,10,10));

            //Notification banner shown when AI auto-optimization is paused because filters were edited.
            contentBox.getChildren().add(getAiLockoutBanner());

            // Add AI Optimization if enabled
            if (mUserPreferences.getAIPreference().isAIEnabled() &&
                !mUserPreferences.getAIPreference().getGeminiApiKey().trim().isEmpty()) {
                GridPane aiPane = new GridPane();
                aiPane.setHgap(10);
                aiPane.setVgap(5);

                javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
                col1.setHgrow(javafx.scene.layout.Priority.NEVER);
                col1.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

                javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
                col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                aiPane.getColumnConstraints().addAll(col1, col2);
                aiPane.setMaxWidth(Double.MAX_VALUE);

                mAIOptimizeButton = new javafx.scene.control.Button("AI Optimize Audio Filters");
                mAIOptimizeButton.setStyle("-fx-font-weight: bold;");
                mAIOptimizeButton.setOnAction(e -> handleAIOptimizeClick());
                mAIOptimizeButton.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                GridPane.setConstraints(mAIOptimizeButton, 0, 0);
                aiPane.getChildren().add(mAIOptimizeButton);
                mAIOptimizeStatusLabel = new Label("Click to run Gemini AI analysis on this channel's audio");
                mAIOptimizeStatusLabel.setWrapText(true);
                GridPane.setConstraints(mAIOptimizeStatusLabel, 1, 0);
                aiPane.getChildren().add(mAIOptimizeStatusLabel);
                contentBox.getChildren().addAll(aiPane, new Separator());
            }

            // 1. Low-pass filter (primary/most-used control - always visible)
            contentBox.getChildren().add(createLowPassSection());
            contentBox.getChildren().add(new Separator());

            // Progressive Disclosure: Advanced Settings
            // Collapsed by default to reduce cognitive load for new users.
            // The most useful control (Low-Pass) is always visible above.
            VBox advancedContent = new VBox(8);
            advancedContent.setVisible(false);
            advancedContent.setManaged(false);

            // 2. Hiss Reduction
            advancedContent.getChildren().add(createHissReductionSection());
            advancedContent.getChildren().add(new Separator());

            // 3. Bass Boost
            advancedContent.getChildren().add(createBassBoostSection());
            advancedContent.getChildren().add(new Separator());

            // 4. Voice Enhancement
            advancedContent.getChildren().add(createVoiceEnhanceSection());
            advancedContent.getChildren().add(new Separator());

            // 5. Squelch Tail
            advancedContent.getChildren().add(createSquelchTailSection());
            advancedContent.getChildren().add(new Separator());

            // 6. Intelligent Squelch
            advancedContent.getChildren().add(createSquelchSection());
            advancedContent.getChildren().add(new Separator());

            // 7. Output Gain
            advancedContent.getChildren().add(createInputGainSection());

            // Toggle button
            Button advancedToggle = new Button("\u25B6  Show Advanced Filters");
            advancedToggle.getStyleClass().add("flat-button");
            advancedToggle.setStyle("-fx-font-size: 12px; -fx-text-fill: #4f8ef7; -fx-cursor: hand; -fx-background-color: transparent; -fx-border-color: transparent;");
            advancedToggle.setTooltip(new Tooltip("Expand to access Hiss Reduction, Bass Boost, Voice Enhancement, Squelch, and Output Gain controls."));
            advancedToggle.setOnAction(ev -> {
                boolean nowVisible = !advancedContent.isVisible();
                advancedContent.setVisible(nowVisible);
                advancedContent.setManaged(nowVisible);
                advancedToggle.setText(nowVisible ? "\u25BC  Hide Advanced Filters" : "\u25B6  Show Advanced Filters");
            });

            contentBox.getChildren().addAll(advancedToggle, advancedContent);

            //All filter controls now exist; track manual edits to pause AI auto-optimization.
            installFilterEditTracking();

            javafx.scene.control.ScrollPane mAudioFiltersPaneSp = new javafx.scene.control.ScrollPane(contentBox);
            mAudioFiltersPaneSp.setFitToWidth(true);
            mAudioFiltersPaneSp.setFitToHeight(true);
            mAudioFiltersPaneSp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mAudioFiltersPane = mAudioFiltersPaneSp;
        }
        return mAudioFiltersPane;
    }

    /**
     * Notification banner shown in the Audio Filters pane when the user has manually edited a filter,
     * which pauses automatic AI filter optimization for this channel.  Hidden until that happens.
     */
    private HBox getAiLockoutBanner()
    {
        if(mAiLockoutBanner == null)
        {
            Label message = new Label("You manually edited the audio filters, so automatic AI filter " +
                    "optimization is paused for this channel — it will not change these (or any other) " +
                    "filter settings. You can re-enable automatic AI filter changes at any time.");
            message.setWrapText(true);
            HBox.setHgrow(message, javafx.scene.layout.Priority.ALWAYS);
            message.setMaxWidth(Double.MAX_VALUE);

            Button reenable = new Button("Re-enable AI Auto Filter Changes");
            reenable.setOnAction(e -> {
                if(getItem() != null && getItem().getDecodeConfiguration() instanceof DecodeConfigNBFM nbfm)
                {
                    nbfm.setAiAutoOptimizeOptedOut(false);
                    modifiedProperty().set(true);
                }
                updateAiLockoutBanner();
            });

            mAiLockoutBanner = new HBox(10, message, reenable);
            mAiLockoutBanner.setAlignment(Pos.CENTER_LEFT);
            mAiLockoutBanner.setPadding(new Insets(8));
            //Amber warning styling that reads in both light and dark themes.
            mAiLockoutBanner.setStyle("-fx-background-color: rgba(255,193,7,0.18); " +
                    "-fx-border-color: rgba(255,193,7,0.6); -fx-border-radius: 4; -fx-background-radius: 4;");
            mAiLockoutBanner.setVisible(false);
            mAiLockoutBanner.setManaged(false);
        }
        return mAiLockoutBanner;
    }

    /**
     * Shows or hides the AI lockout banner based on the current channel's opt-out state.
     */
    private void updateAiLockoutBanner()
    {
        if(mAiLockoutBanner == null)
        {
            return;
        }
        boolean optedOut = getItem() != null
                && getItem().getDecodeConfiguration() instanceof DecodeConfigNBFM nbfm
                && nbfm.isAiAutoOptimizeOptedOut();
        mAiLockoutBanner.setVisible(optedOut);
        mAiLockoutBanner.setManaged(optedOut);
    }

    /**
     * Attaches change listeners to every audio-filter control so that a manual edit pauses automatic
     * AI filter optimization (and shows the notification banner).  Programmatic changes during config
     * load or while applying AI suggestions are ignored.
     */
    private void installFilterEditTracking()
    {
        addManualEditHook(mLowPassEnabledSwitch.selectedProperty());
        addManualEditHook(mLowPassCutoffSlider.valueProperty());
        addManualEditHook(mHissReductionEnabledSwitch.selectedProperty());
        addManualEditHook(mHissReductionDbSlider.valueProperty());
        addManualEditHook(mHissReductionCornerSlider.valueProperty());
        addManualEditHook(mBassBoostEnabledSwitch.selectedProperty());
        addManualEditHook(mBassBoostSlider.valueProperty());
        addManualEditHook(mVoiceEnhanceEnabledSwitch.selectedProperty());
        addManualEditHook(mVoiceEnhanceSlider.valueProperty());
        addManualEditHook(mSquelchTailEnabledSwitch.selectedProperty());
        addManualEditHook(mTailRemovalSpinner.valueProperty());
        addManualEditHook(mHeadRemovalSpinner.valueProperty());
        addManualEditHook(mSquelchEnabledSwitch.selectedProperty());
        addManualEditHook(mSquelchThresholdSlider.valueProperty());
        addManualEditHook(mSquelchReductionSlider.valueProperty());
        addManualEditHook(mHoldTimeSlider.valueProperty());
        addManualEditHook(mInputGainSlider.valueProperty());
    }

    private void addManualEditHook(javafx.beans.value.ObservableValue<?> property)
    {
        if(property == null)
        {
            return;
        }
        property.addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration && !mApplyingAiResult)
            {
                markFiltersManuallyEdited();
            }
        });
    }

    /**
     * Records that the user manually edited a filter: opts this channel out of automatic AI filter
     * optimization and shows the notification banner.
     */
    private void markFiltersManuallyEdited()
    {
        if(getItem() != null && getItem().getDecodeConfiguration() instanceof DecodeConfigNBFM nbfm
                && !nbfm.isAiAutoOptimizeOptedOut())
        {
            nbfm.setAiAutoOptimizeOptedOut(true);
            modifiedProperty().set(true);
        }
        updateAiLockoutBanner();
    }

    private javafx.scene.Node getEventLogPane(){
        if(mEventLogPane == null)
        {
            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(getEventLogConfigurationEditor());
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mEventLogPane = sp;

        }

        return mEventLogPane;
    }

    private javafx.scene.Node getAuxDecoderPane(){
        if(mAuxDecoderPane == null)
        {
            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(getAuxDecoderConfigurationEditor());
            sp.setFitToWidth(true);
            sp.setFitToHeight(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mAuxDecoderPane = sp;

        }

        return mAuxDecoderPane;
    }

    private javafx.scene.Node getRecordPane(){
        if(mRecordPane == null)
        {
            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            SettingsCard recordCard = new SettingsCard();
            recordCard.getChildren().addAll(
                new SettingsRow("Channel (Baseband I&Q)", getBasebandRecordSwitch())
            );
            content.getChildren().add(recordCard);

            javafx.scene.control.ScrollPane mRecordPaneSp = new javafx.scene.control.ScrollPane(content);
            mRecordPaneSp.setFitToWidth(true);
            mRecordPaneSp.setFitToHeight(true);
            mRecordPaneSp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
            mRecordPane = mRecordPaneSp;
        }

        return mRecordPane;
    }

    /**
     * Creates a styled TextField that syncs bidirectionally with a Slider.
     * When the user presses Enter or the field loses focus, the parsed value updates the slider.
     *
     * @param slider The slider to sync with
     * @param defaultText Initial display text
     * @param suffix Unit suffix (e.g. " Hz", " dB", "%", " ms")
     * @param format printf format for the numeric value (e.g. "%.0f", "%.1f")
     */
    private TextField createSliderTextField(Slider slider, String defaultText, String suffix, String format)
    {
        TextField field = new TextField(defaultText);
        field.setPrefWidth(90);
        field.setMaxWidth(90);
        field.setStyle("-fx-font-size: 11px;");

        // Commit on Enter key
        field.setOnAction(event -> {
            commitTextFieldToSlider(field, slider, suffix, format);
        });

        // Commit on focus loss
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if(!isFocused)
            {
                commitTextFieldToSlider(field, slider, suffix, format);
            }
        });

        return field;
    }

    /**
     * Parses the numeric value from a text field and updates the associated slider.
     */
    private void commitTextFieldToSlider(TextField field, Slider slider, String suffix, String format)
    {
        try
        {
            String text = field.getText().trim();
            // Strip suffix and common symbols
            text = text.replace(suffix.trim(), "").replace("+", "").replace("x", "").replace("(", "").trim();
            // Handle "1.0x (0.0 dB)" format for gain field
            if(text.contains(" "))
            {
                text = text.split("\\s+")[0];
            }
            double value = Double.parseDouble(text);
            value = Math.max(slider.getMin(), Math.min(slider.getMax(), value));
            slider.setValue(value);
            // Re-format the display to be consistent
            field.setText(String.format(format, value) + suffix);
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
            }
        }
        catch(NumberFormatException e)
        {
            // Revert to current slider value on parse failure
            field.setText(String.format(format, slider.getValue()) + suffix);
        }
    }

    private SettingsCard createInputGainSection()
    {
        SettingsCard section = new SettingsCard();

        mInputGainSlider = new Slider(0.1, 5.0, 2.0);
        mInputGainSlider.setMajorTickUnit(1.0);
        mInputGainSlider.setMinorTickCount(4);
        mInputGainSlider.setShowTickMarks(true);
        mInputGainSlider.setShowTickLabels(true);
        mInputGainSlider.setPrefWidth(300);
        mInputGainSlider.setTooltip(new Tooltip("Output gain applied after all filters\n1.0 = unity, 2.0 = +6dB (default)"));
        mInputGainSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mInputGainField.setText(String.format("%.1fx (%.1f dB)", val.floatValue(),
                    20.0 * Math.log10(val.doubleValue())));
                modifiedProperty().set(true);
            }
        });

        mInputGainField = new TextField("2.0x (6.0 dB)");
        mInputGainField.setPrefWidth(120);
        mInputGainField.setMaxWidth(120);
        mInputGainField.setStyle("-fx-font-size: 11px;");
        mInputGainField.setOnAction(event -> {
            commitTextFieldToSlider(mInputGainField, mInputGainSlider, "x", "%.1f");
            double v = (int) mInputGainSlider.getValue();
            mInputGainField.setText(String.format("%.1fx (%.1f dB)", v, 20.0 * Math.log10(v)));
        });
        mInputGainField.focusedProperty().addListener((obs2, wasFocused, isFocused) -> {
            if(!isFocused)
            {
                commitTextFieldToSlider(mInputGainField, mInputGainSlider, "x", "%.1f");
                double v = (int) mInputGainSlider.getValue();
                mInputGainField.setText(String.format("%.1fx (%.1f dB)", v, 20.0 * Math.log10(v)));
            }
        });

        section.getChildren().add(new SettingsRow("Output Gain", createHelpIcon("Adjusts the final audio volume level after all filters have been applied. Use this to compensate for volume lost due to filtering or to boost quiet signals."), mInputGainSlider, mInputGainField));
        return section;
    }


    private SettingsCard createHighPassSection()
    {
        SettingsCard section = new SettingsCard();
        section.getChildren().add(new SettingsRow("High-Pass Filter", createHelpIcon("Removes DC offset and sub-audible signalling."), getAudioFilterEnable()));
        return section;
    }

    private SettingsCard createLowPassSection()
    {
        SettingsCard section = new SettingsCard();

        mLowPassEnabledSwitch = new ToggleSwitch("Enable");
        mLowPassEnabledSwitch.setTooltip(new Tooltip("Remove high-frequency hiss/noise"));
        mLowPassEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mLowPassCutoffSlider.setDisable(!val);
            }
        });

        mLowPassCutoffSlider = new Slider(2500, 4000, 2800);
        mLowPassCutoffSlider.setMajorTickUnit(500);
        mLowPassCutoffSlider.setMinorTickCount(4);
        mLowPassCutoffSlider.setShowTickMarks(true);
        mLowPassCutoffSlider.setShowTickLabels(true);
        mLowPassCutoffSlider.setPrefWidth(300);
        mLowPassCutoffSlider.setTooltip(new Tooltip("Higher = brighter\nLower = less noise\nDefault: 2800 Hz"));
        mLowPassCutoffSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mLowPassCutoffField.setText(val.intValue() + " Hz");
                modifiedProperty().set(true);
            }
        });

        mLowPassCutoffField = createSliderTextField(mLowPassCutoffSlider, "2800 Hz", " Hz", "%.0f");

        section.getChildren().addAll(
            new SettingsRow("Low-Pass Filter", createHelpIcon("Removes high frequencies above the cutoff. Use this to eliminate harsh high-pitched static and hiss from weak analog FM signals."), mLowPassEnabledSwitch),
            new SettingsRow("Cutoff", mLowPassCutoffSlider, mLowPassCutoffField)
        );
        return section;
    }

    private SettingsCard createVoiceEnhanceSection()
    {
        SettingsCard section = new SettingsCard();

        mVoiceEnhanceEnabledSwitch = new ToggleSwitch("Enable");
        mVoiceEnhanceEnabledSwitch.setTooltip(new Tooltip("Boost speech clarity (2-4 kHz presence)"));
        mVoiceEnhanceEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mVoiceEnhanceSlider.setDisable(!val);
            }
        });

        mVoiceEnhanceSlider = new Slider(0, 100, 0);
        mVoiceEnhanceSlider.setMajorTickUnit(25);
        mVoiceEnhanceSlider.setMinorTickCount(4);
        mVoiceEnhanceSlider.setShowTickMarks(true);
        mVoiceEnhanceSlider.setShowTickLabels(true);
        mVoiceEnhanceSlider.setPrefWidth(300);
        mVoiceEnhanceSlider.setTooltip(new Tooltip("Boost speech presence\n0% = off, 100% = max clarity\nDefault: 0%"));
        mVoiceEnhanceSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mVoiceEnhanceField.setText(val.intValue() + "%");
                modifiedProperty().set(true);
            }
        });

        mVoiceEnhanceField = createSliderTextField(mVoiceEnhanceSlider, "0%", "%", "%.0f");

        section.getChildren().addAll(
            new SettingsRow("Voice Enhancement", createHelpIcon("Boosts frequencies in the 2-4 kHz range (presence peak). Use this to improve speech intelligibility and make voices stand out, especially in noisy environments."), mVoiceEnhanceEnabledSwitch),
            new SettingsRow("Amount", mVoiceEnhanceSlider, mVoiceEnhanceField)
        );
        return section;
    }

    private SettingsCard createBassBoostSection()
    {
        SettingsCard section = new SettingsCard();

        mBassBoostEnabledSwitch = new ToggleSwitch("Enable");
        mBassBoostEnabledSwitch.setTooltip(new Tooltip("Boost low frequencies below 400 Hz for warmth"));
        mBassBoostEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mBassBoostSlider.setDisable(!val);
            }
        });

        mBassBoostSlider = new Slider(0, 12, 0);
        mBassBoostSlider.setMajorTickUnit(3);
        mBassBoostSlider.setMinorTickCount(2);
        mBassBoostSlider.setShowTickMarks(true);
        mBassBoostSlider.setShowTickLabels(true);
        mBassBoostSlider.setPrefWidth(300);
        mBassBoostSlider.setTooltip(new Tooltip("Low-shelf boost below 400 Hz\n0 dB = off, +12 dB = max bass\nDefault: 0 dB"));
        mBassBoostSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mBassBoostField.setText(String.format("+%.1f dB", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });

        mBassBoostField = createSliderTextField(mBassBoostSlider, "+0.0 dB", " dB", "+%.1f");

        section.getChildren().addAll(
            new SettingsRow("Bass Boost", createHelpIcon("Boosts low frequencies (low-shelf) below 400 Hz. Use this to add depth and richness to voices that sound thin or \'tinny\'."), mBassBoostEnabledSwitch),
            new SettingsRow("Boost Amount", mBassBoostSlider, mBassBoostField)
        );
        return section;
    }

    private SettingsCard createHissReductionSection()
    {
        SettingsCard section = new SettingsCard();

        mHissReductionEnabledSwitch = new ToggleSwitch("Enable");
        mHissReductionEnabledSwitch.setTooltip(new Tooltip(
                "High-shelf cut above corner frequency to reduce FM hiss.\n" +
                "Stacks with Low-Pass Filter."));
        mHissReductionEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mHissReductionDbSlider.setDisable(!val);
                mHissReductionCornerSlider.setDisable(!val);
            }
        });

        mHissReductionDbSlider = new Slider(-12, 0, -6);
        mHissReductionDbSlider.setMajorTickUnit(3);
        mHissReductionDbSlider.setMinorTickCount(2);
        mHissReductionDbSlider.setShowTickMarks(true);
        mHissReductionDbSlider.setShowTickLabels(true);
        mHissReductionDbSlider.setPrefWidth(300);
        mHissReductionDbSlider.setTooltip(new Tooltip(
                "High-shelf attenuation above corner frequency.\n" +
                "0 dB = off, -12 dB = max hiss cut\nDefault: -6 dB"));
        mHissReductionDbSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHissReductionDbField.setText(String.format("%.1f dB", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });

        mHissReductionDbField = createSliderTextField(mHissReductionDbSlider, "-6.0 dB", " dB", "%.1f");

        mHissReductionCornerSlider = new Slider(1000, 3500, 2000);
        mHissReductionCornerSlider.setMajorTickUnit(500);
        mHissReductionCornerSlider.setMinorTickCount(4);
        mHissReductionCornerSlider.setShowTickMarks(true);
        mHissReductionCornerSlider.setShowTickLabels(true);
        mHissReductionCornerSlider.setPrefWidth(300);
        mHissReductionCornerSlider.setTooltip(new Tooltip(
                "Shelf pivot frequency. Hiss above this is attenuated.\n" +
                "Lower = more hiss cut but slightly duller voice.\nDefault: 2000 Hz"));
        mHissReductionCornerSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHissReductionCornerField.setText(String.format("%.0f Hz", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });

        mHissReductionCornerField = createSliderTextField(mHissReductionCornerSlider, "2000 Hz", " Hz", "%.0f");

        section.getChildren().addAll(
            new SettingsRow("Hiss Reduction", createHelpIcon("Attenuates high frequencies (high-shelf cut) above the corner frequency. Use this to soften background hiss while preserving voice clarity better than a hard low-pass filter."), mHissReductionEnabledSwitch),
            new SettingsRow("Cut Amount", mHissReductionDbSlider, mHissReductionDbField),
            new SettingsRow("Corner Freq", mHissReductionCornerSlider, mHissReductionCornerField)
        );
        return section;
    }

    private SettingsCard createSquelchTailSection()
    {
        SettingsCard section = new SettingsCard();

        mSquelchTailEnabledSwitch = new ToggleSwitch("Enable");
        mSquelchTailEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
            }
        });

        mHeadRemovalSpinner = new Spinner<>(0, 150, 0, 10);
        mHeadRemovalSpinner.setEditable(true);
        mHeadRemovalSpinner.setPrefWidth(100);
        mHeadRemovalSpinner.setTooltip(new Tooltip("Milliseconds to trim from start of transmission (removes tone ramp-up)"));
        mHeadRemovalSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });

        mTailRemovalSpinner = new Spinner<>(0, 300, 100, 10);
        mTailRemovalSpinner.setEditable(true);
        mTailRemovalSpinner.setPrefWidth(100);
        mTailRemovalSpinner.setTooltip(new Tooltip("Milliseconds to trim from end of transmission (removes noise burst)"));
        mTailRemovalSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });

        mAudioHangtimeSpinner = new Spinner<>(0, 1000, 0, 50);
        mAudioHangtimeSpinner.setEditable(true);
        mAudioHangtimeSpinner.setPrefWidth(100);
        mAudioHangtimeSpinner.setTooltip(new Tooltip("Delay before closing audio segment after transmission ends.\nPrevents cutting off the end of audio in ThinLine/Zello streams.\n0 = immediate close (default), 100-300 = recommended for streaming"));
        mAudioHangtimeSpinner.getValueFactory().valueProperty().addListener((obs, ov, nv) -> {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
        });

        section.getChildren().addAll(
            new SettingsRow("Squelch Tail Removal", createHelpIcon("Trims the beginning or end of transmissions to remove noise bursts or tone ramp-ups. Hangtime adds a delay before closing audio."), mSquelchTailEnabledSwitch),
            new SettingsRow("Head Trim (ms)", mHeadRemovalSpinner),
            new SettingsRow("Tail Trim (ms)", mTailRemovalSpinner),
            new SettingsRow("Hangtime (ms)", mAudioHangtimeSpinner)
        );
        return section;
    }

    private SettingsCard createSquelchSection()
    {
        SettingsCard section = new SettingsCard();

        mSquelchEnabledSwitch = new ToggleSwitch("Enable");
        mSquelchEnabledSwitch.setTooltip(new Tooltip("Silence carrier/static between voice"));
        mSquelchEnabledSwitch.selectedProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                modifiedProperty().set(true);
                mSquelchThresholdSlider.setDisable(!val);
                mSquelchReductionSlider.setDisable(!val);
                mHoldTimeSlider.setDisable(!val);
            }
        });

        mSquelchThresholdSlider = new Slider(0, 100, 4.0);
        mSquelchThresholdSlider.setMajorTickUnit(25);
        mSquelchThresholdSlider.setMinorTickCount(4);
        mSquelchThresholdSlider.setShowTickMarks(true);
        mSquelchThresholdSlider.setShowTickLabels(true);
        mSquelchThresholdSlider.setPrefWidth(300);
        mSquelchThresholdSlider.setTooltip(new Tooltip("Gate opens when level > threshold\nLower = more sensitive\nDefault: 4%"));
        mSquelchThresholdSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mSquelchThresholdField.setText(String.format("%.1f%%", val.doubleValue()));
                modifiedProperty().set(true);
            }
        });

        mSquelchThresholdField = createSliderTextField(mSquelchThresholdSlider, "4.0%", "%", "%.1f");

        mSquelchReductionSlider = new Slider(0, 100, 80);
        mSquelchReductionSlider.setMajorTickUnit(25);
        mSquelchReductionSlider.setMinorTickCount(4);
        mSquelchReductionSlider.setShowTickMarks(true);
        mSquelchReductionSlider.setShowTickLabels(true);
        mSquelchReductionSlider.setPrefWidth(300);
        mSquelchReductionSlider.setTooltip(new Tooltip("How much to reduce carrier noise\n0% = no reduction, 100% = full mute\nDefault: 80%"));
        mSquelchReductionSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mSquelchReductionField.setText(val.intValue() + "%");
                modifiedProperty().set(true);
            }
        });

        mSquelchReductionField = createSliderTextField(mSquelchReductionSlider, "80%", "%", "%.0f");

        mHoldTimeSlider = new Slider(0, 1000, 500);
        mHoldTimeSlider.setMajorTickUnit(250);
        mHoldTimeSlider.setMinorTickCount(4);
        mHoldTimeSlider.setShowTickMarks(true);
        mHoldTimeSlider.setShowTickLabels(true);
        mHoldTimeSlider.setPrefWidth(300);
        mHoldTimeSlider.setTooltip(new Tooltip("Keep gate open after voice stops\nSilences carrier/static between voice\nDefault: 500ms"));
        mHoldTimeSlider.valueProperty().addListener((obs, old, val) -> {
            if(!mLoadingConfiguration)
            {
                mHoldTimeField.setText(val.intValue() + " ms");
                modifiedProperty().set(true);
            }
        });

        mHoldTimeField = createSliderTextField(mHoldTimeSlider, "500 ms", " ms", "%.0f");

        section.getChildren().addAll(
            new SettingsRow("Squelch / Noise Gate", createHelpIcon("Mutes the audio when the signal level drops below the threshold. Use this to silence background static and noise between active voice transmissions."), mSquelchEnabledSwitch),
            new SettingsRow("Threshold", mSquelchThresholdSlider, mSquelchThresholdField),
            new SettingsRow("Reduction", mSquelchReductionSlider, mSquelchReductionField),
            new SettingsRow("Delay (Hold Time)", mHoldTimeSlider, mHoldTimeField)
        );
        return section;
    }

    private SourceConfigurationEditor getSourceConfigurationEditor()
    {
        if(mSourceConfigurationEditor == null)
        {
            mSourceConfigurationEditor = new FrequencyEditor(mTunerManager);

            //Add a listener so that we can push change notifications up to this editor
            mSourceConfigurationEditor.modifiedProperty()
                .addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mSourceConfigurationEditor;
    }

    private EventLogConfigurationEditor getEventLogConfigurationEditor()
    {
        if(mEventLogConfigurationEditor == null)
        {
            List<EventLogType> types = new ArrayList<>();
            types.add(EventLogType.CALL_EVENT);
            types.add(EventLogType.DECODED_MESSAGE);

            mEventLogConfigurationEditor = new EventLogConfigurationEditor(types);
            mEventLogConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mEventLogConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mEventLogConfigurationEditor;
    }

    private AuxDecoderConfigurationEditor getAuxDecoderConfigurationEditor()
    {
        if(mAuxDecoderConfigurationEditor == null)
        {
            mAuxDecoderConfigurationEditor = new AuxDecoderConfigurationEditor(DecoderType.AUX_DECODERS);
            mAuxDecoderConfigurationEditor.setPadding(new Insets(5,5,5,5));
            mAuxDecoderConfigurationEditor.modifiedProperty().addListener((observable, oldValue, newValue) -> modifiedProperty().set(true));
        }

        return mAuxDecoderConfigurationEditor;
    }

    /**
     * Toggle switch for enable/disable the audio filtering in the audio module.
     * @return toggle switch.
     */
    private ToggleSwitch getAudioFilterEnable()
    {
        if(mAudioFilterEnable == null)
        {
            mAudioFilterEnable = new ToggleSwitch("Enable High-Pass Filter");
            mAudioFilterEnable.setTooltip(new Tooltip("High-pass filter to remove DC offset and sub-audible signalling"));
            mAudioFilterEnable.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });
        }

        return mAudioFilterEnable;
    }

    private SegmentedButton getBandwidthButton()
    {
        if(mBandwidthButton == null)
        {
            mBandwidthButton = new SegmentedButton();
            mBandwidthButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mBandwidthButton.setDisable(true);

            for(DecodeConfigNBFM.Bandwidth bandwidth : DecodeConfigNBFM.Bandwidth.FM_BANDWIDTHS)
            {
                ToggleButton toggleButton = new ToggleButton(bandwidth.toString());
                toggleButton.setUserData(bandwidth);
                mBandwidthButton.getButtons().add(toggleButton);
            }

            mBandwidthButton.getToggleGroup().selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });

            //Note: there is a weird timing bug with the segmented button where the toggles are not added to
            //the toggle group until well after the control is rendered.  We attempt to setItem() on the
            //decode configuration and we're unable to correctly set the bandwidth setting.  As a work
            //around, we'll listen for the toggles to be added and update them here.  This normally only
            //happens when we first instantiate the editor and load an item for editing the first time.
            mBandwidthButton.getToggleGroup().getToggles().addListener((ListChangeListener<Toggle>)c ->
            {
                //This change event happens when the toggles are added -- we don't need to inspect the change event
                if(getItem() != null && getItem().getDecodeConfiguration() instanceof DecodeConfigNBFM)
                {
                    //Capture current modified state so that we can reapply after adjusting control states
                    boolean modified = modifiedProperty().get();

                    DecodeConfigNBFM config = (DecodeConfigNBFM)getItem().getDecodeConfiguration();
                    DecodeConfigNBFM.Bandwidth bandwidth = config.getBandwidth();
                    if(bandwidth == null)
                    {
                        bandwidth = DecodeConfigNBFM.Bandwidth.BW_7_5;
                    }

                    for(Toggle toggle: getBandwidthButton().getToggleGroup().getToggles())
                    {
                        toggle.setSelected(toggle.getUserData() == bandwidth);
                    }

                    modifiedProperty().set(modified);
                }
            });
        }

        return mBandwidthButton;
    }

    private TextField getTalkgroupField()
    {
        if(mTalkgroupField == null)
        {
            mTalkgroupField = new TextField();
            mTalkgroupField.setTextFormatter(mTalkgroupTextFormatter);
        }

        return mTalkgroupField;
    }

    /**
     * Updates the talkgroup editor's text formatter.
     * @param value to set in the control.
     */
    private void updateTextFormatter(int value)
    {
        if(mTalkgroupTextFormatter != null)
        {
            mTalkgroupTextFormatter.valueProperty().removeListener(mTalkgroupValueChangeListener);
        }

        IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(Protocol.NBFM);

        if(format == null)
        {
            format = IntegerFormat.DECIMAL;
        }

        if(format == IntegerFormat.DECIMAL)
        {
            mTalkgroupTextFormatter = mDecimalFormatter;
            getTalkgroupField().setTooltip(new Tooltip("Valid range: 1 to 4,294,967,295"));
        }
        else
        {
            mTalkgroupTextFormatter = mHexFormatter;
            getTalkgroupField().setTooltip(new Tooltip("Valid range: 1 to FFFFFFFF"));
        }

        mTalkgroupTextFormatter.setValue(value);

        getTalkgroupField().setTextFormatter(mTalkgroupTextFormatter);
        mTalkgroupTextFormatter.valueProperty().addListener(mTalkgroupValueChangeListener);
    }

    /**
     * Change listener to detect when talkgroup value has changed and set modified property to true.
     */
    public class TalkgroupValueChangeListener implements ChangeListener<Integer>
    {
        @Override
        public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue)
        {
            if(!mLoadingConfiguration) {
                modifiedProperty().set(true);
            }
            Integer tg = getConfiguredTalkgroup();
            if (tg != null && tg > 0) {
                boolean conflict = false;
                for (io.github.dsheirer.controller.channel.Channel c : getPlaylistManager().getChannelModel().channelList()) {
                    if (c == getItem()) continue;
                    io.github.dsheirer.module.decode.config.DecodeConfiguration dc = c.getDecodeConfiguration();
                    if (dc instanceof io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM) {
                        if (tg.equals(((io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM) dc).getTalkgroup())) {
                            conflict = true;
                            break;
                        }
                    }
                }
                if (conflict) {
                    getTalkgroupField().setStyle("-fx-border-color: red; -fx-border-width: 2px; -fx-border-radius: 3px;");
                    if (mTalkgroupTextFormatter == mHexFormatter) {
                        getTalkgroupField().setTooltip(new Tooltip("Talkgroup ID already assigned to another channel (1 - FFFFFFFF)"));
                    } else {
                        getTalkgroupField().setTooltip(new Tooltip("Talkgroup ID already assigned to another channel (1 - 4,294,967,295)"));
                    }
                } else {
                    getTalkgroupField().setStyle("");
                    if (mTalkgroupTextFormatter == mHexFormatter) {
                        getTalkgroupField().setTooltip(new Tooltip("1 - FFFFFFFF"));
                    } else {
                        getTalkgroupField().setTooltip(new Tooltip("1 - 4,294,967,295"));
                    }
                }
            } else {
                getTalkgroupField().setStyle("");
                if (mTalkgroupTextFormatter == mHexFormatter) {
                    getTalkgroupField().setTooltip(new Tooltip("1 - FFFFFFFF"));
                } else {
                    getTalkgroupField().setTooltip(new Tooltip("1 - 4,294,967,295"));
                }
            }
        }
    }

    private ToggleSwitch getBasebandRecordSwitch()
    {
        if(mBasebandRecordSwitch == null)
        {
            mBasebandRecordSwitch = new ToggleSwitch();
            mBasebandRecordSwitch.setDisable(true);
            mBasebandRecordSwitch.setTextAlignment(TextAlignment.RIGHT);
            mBasebandRecordSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if(!mLoadingConfiguration) {
                    modifiedProperty().set(true);
                }
            });
        }

        return mBasebandRecordSwitch;
    }

    @Override
    protected void setDecoderConfiguration(DecodeConfiguration config)
    {
        mLoadingConfiguration = true;

        if(config instanceof DecodeConfigNBFM)
        {
            getBandwidthButton().setDisable(false);
            DecodeConfigNBFM decodeConfigNBFM = (DecodeConfigNBFM)config;
            final DecodeConfigNBFM.Bandwidth bandwidth = (decodeConfigNBFM.getBandwidth() != null ?
                    decodeConfigNBFM.getBandwidth() : DecodeConfigNBFM.Bandwidth.BW_7_5);

            for(Toggle toggle: getBandwidthButton().getToggleGroup().getToggles())
            {
                toggle.setSelected(toggle.getUserData() == bandwidth);
            }

            updateTextFormatter(decodeConfigNBFM.getTalkgroup());
            getAudioFilterEnable().setDisable(false);
            getAudioFilterEnable().setSelected(decodeConfigNBFM.isAudioFilter());

            // Load tone filter settings
            mToneFilterEnabledSwitch.setSelected(decodeConfigNBFM.isToneFilterEnabled());
            List<ChannelToneFilter> savedFilters = decodeConfigNBFM.getToneFilters();
            if(savedFilters != null && !savedFilters.isEmpty())
            {
                ChannelToneFilter filter = savedFilters.get(0);
                mToneTypeCombo.setValue(filter.getToneType());
                updateToneCodeVisibility();
                //Clear both code combos first so a stale value from a previously-edited channel doesn't
                //linger (the editor instance is reused across channels of the same decoder type).
                mCtcssCodeCombo.setValue(null);
                mDcsCodeCombo.setValue(null);
                if(filter.getToneType() == ChannelToneFilter.ToneType.CTCSS)
                {
                    CTCSSCode code = filter.getCTCSSCode();
                    if(code != null && code != CTCSSCode.UNKNOWN)
                    {
                        mCtcssCodeCombo.setValue(code);
                    }
                }
                else if(filter.getToneType() == ChannelToneFilter.ToneType.DCS)
                {
                    DCSCode code = filter.getDCSCode();
                    if(code != null)
                    {
                        mDcsCodeCombo.setValue(code);
                    }
                }
            }
            else
            {
                mToneTypeCombo.setValue(ChannelToneFilter.ToneType.CTCSS);
                mCtcssCodeCombo.setValue(null);
                mCtcssCodeCombo.setPromptText("Select PL tone");
                mDcsCodeCombo.setValue(null);
                mDcsCodeCombo.setPromptText("Select DCS code");
                updateToneCodeVisibility();
            }

            // Load tone-squelch false-trigger suppression settings
            mToneMinCallDurationSpinner.getValueFactory().setValue(decodeConfigNBFM.getToneMinCallDurationMs());
            mToneRequireNoiseSquelchSwitch.setSelected(decodeConfigNBFM.isToneRequireNoiseSquelch());
            mMinCallDurationSpinner.getValueFactory().setValue(decodeConfigNBFM.getMinCallDurationMs());

            // Load squelch tail settings
            mSquelchTailEnabledSwitch.setSelected(decodeConfigNBFM.isSquelchTailRemovalEnabled());
            mTailRemovalSpinner.getValueFactory().setValue(decodeConfigNBFM.getSquelchTailRemovalMs());
            mHeadRemovalSpinner.getValueFactory().setValue(decodeConfigNBFM.getSquelchHeadRemovalMs());
            mAudioHangtimeSpinner.getValueFactory().setValue(decodeConfigNBFM.getAudioHangtimeMs());

            // Load audio filter settings
            loadAudioFilterConfiguration(decodeConfigNBFM);
        }
        else
        {
            getBandwidthButton().setDisable(true);

            for(Toggle toggle: getBandwidthButton().getToggleGroup().getToggles())
            {
                toggle.setSelected(false);
            }

            updateTextFormatter(0);
            getTalkgroupField().setDisable(true);
            getAudioFilterEnable().setDisable(true);
            getAudioFilterEnable().setSelected(false);

            // Reset tone filter controls
            mToneFilterEnabledSwitch.setSelected(false);
            mToneTypeCombo.setValue(ChannelToneFilter.ToneType.CTCSS);
            mCtcssCodeCombo.setValue(null);
            mCtcssCodeCombo.setPromptText("Select PL tone");
            mDcsCodeCombo.setValue(null);
            mDcsCodeCombo.setPromptText("Select DCS code");
            updateToneCodeVisibility();
            mToneMinCallDurationSpinner.getValueFactory().setValue(0);
            mToneRequireNoiseSquelchSwitch.setSelected(true);
            mMinCallDurationSpinner.getValueFactory().setValue(0);

            // Reset squelch tail controls
            mSquelchTailEnabledSwitch.setSelected(false);
            mTailRemovalSpinner.getValueFactory().setValue(100);
            mHeadRemovalSpinner.getValueFactory().setValue(0);

            // Disable audio filter controls
            disableAudioFilterControls();
        }

        mLoadingConfiguration = false;
    }

    @Override
    protected Integer getConfiguredTalkgroup()
    {
        String tgText = getTalkgroupField().getText();
        if(tgText != null && !tgText.trim().isEmpty())
        {
            try
            {
                IntegerFormat format = mUserPreferences.getTalkgroupFormatPreference().getTalkgroupFormat(Protocol.NBFM);
                int tg;
                if(format == IntegerFormat.HEXADECIMAL)
                {
                    tg = Integer.parseUnsignedInt(tgText.trim(), 16);
                }
                else
                {
                    tg = Integer.parseUnsignedInt(tgText.trim());
                }
                if(io.github.dsheirer.alias.id.talkgroup.TalkgroupFormat.NBFM.isValid(tg))
                {
                    return tg;
                }
            }
            catch(NumberFormatException e)
            {
                return null;
            }
        }
        return null;
    }

    @Override
    protected void setConfiguredTalkgroup(int value)
    {
        //Set via the text formatter so the value (unsigned, possibly 10 digits) is parsed/displayed correctly.
        if(mTalkgroupTextFormatter != null)
        {
            mTalkgroupTextFormatter.setValue(value);
        }
        else
        {
            getTalkgroupField().setText(Integer.toUnsignedString(value));
        }
    }

    @Override
    protected void saveDecoderConfiguration()
    {
        DecodeConfigNBFM config;

        if(getItem().getDecodeConfiguration() instanceof DecodeConfigNBFM)
        {
            config = (DecodeConfigNBFM)getItem().getDecodeConfiguration();
        }
        else
        {
            config = new DecodeConfigNBFM();
        }

        DecodeConfigNBFM.Bandwidth bandwidth = DecodeConfigNBFM.Bandwidth.BW_7_5;

        if(getBandwidthButton().getToggleGroup().getSelectedToggle() != null)
        {
            bandwidth = (DecodeConfigNBFM.Bandwidth)getBandwidthButton().getToggleGroup().getSelectedToggle().getUserData();
        }

        config.setBandwidth(bandwidth);

        int originalTalkgroup = config.getTalkgroup();

        Integer talkgroup = mTalkgroupTextFormatter.getValue();

        if(talkgroup == null)
        {
            talkgroup = 1;
        }

        config.setTalkgroup(talkgroup);

        if(originalTalkgroup > 0 && originalTalkgroup != config.getTalkgroup())
        {
            getPlaylistManager().getAliasModel().updateTalkgroup(originalTalkgroup, config.getTalkgroup(), io.github.dsheirer.protocol.Protocol.NBFM);
        }
        config.setAudioFilter(getAudioFilterEnable().isSelected());

        // Save tone filter settings
        config.setToneFilterEnabled(mToneFilterEnabledSwitch.isSelected());
        List<ChannelToneFilter> filters = new ArrayList<>();
        ChannelToneFilter.ToneType selectedType = mToneTypeCombo.getValue();
        if(selectedType == ChannelToneFilter.ToneType.CTCSS)
        {
            CTCSSCode code = mCtcssCodeCombo.getValue();
            if(code != null && code != CTCSSCode.UNKNOWN)
            {
                filters.add(new ChannelToneFilter(selectedType, code.name(), ""));
            }
        }
        else if(selectedType == ChannelToneFilter.ToneType.DCS)
        {
            DCSCode code = mDcsCodeCombo.getValue();
            if(code != null)
            {
                filters.add(new ChannelToneFilter(selectedType, code.name(), ""));
            }
        }
        config.setToneFilters(filters);
        config.setToneMinCallDurationMs(mToneMinCallDurationSpinner.getValue());
        config.setToneRequireNoiseSquelch(mToneRequireNoiseSquelchSwitch.isSelected());
        config.setMinCallDurationMs(mMinCallDurationSpinner.getValue());

        // Save squelch tail settings
        config.setSquelchTailRemovalEnabled(mSquelchTailEnabledSwitch.isSelected());
        config.setSquelchTailRemovalMs(mTailRemovalSpinner.getValue());
        config.setSquelchHeadRemovalMs(mHeadRemovalSpinner.getValue());
        config.setAudioHangtimeMs(mAudioHangtimeSpinner.getValue());

        // Save audio filter settings
        saveAudioFilterConfiguration(config);

        getItem().setDecodeConfiguration(config);
    }

    private void loadAudioFilterConfiguration(DecodeConfigNBFM config)
    {
        // Input Gain (map from AGC max gain)
        float inputGain = (float)Math.pow(10.0, config.getAgcMaxGain() / 40.0);
        mInputGainSlider.setValue(inputGain);
        mInputGainField.setText(String.format("%.1fx (%.1f dB)", inputGain,
            20.0 * Math.log10(inputGain)));

        // Low-pass
        mLowPassEnabledSwitch.setSelected(config.isLowPassEnabled());
        mLowPassCutoffSlider.setValue(config.getLowPassCutoff());
        mLowPassCutoffField.setText((int)config.getLowPassCutoff() + " Hz");
        mLowPassCutoffSlider.setDisable(!config.isLowPassEnabled());

        // Voice Enhancement - load from AGC target level
        mVoiceEnhanceEnabledSwitch.setSelected(config.isAgcEnabled());
        // Map -30 to -6 dB range back to 0-100%
        float targetLevel = config.getAgcTargetLevel();
        float voiceAmount = ((targetLevel + 30.0f) / 24.0f) * 100.0f;
        voiceAmount = Math.max(0, Math.min(100, voiceAmount));
        mVoiceEnhanceSlider.setValue(voiceAmount);
        mVoiceEnhanceField.setText((int)voiceAmount + "%");
        mVoiceEnhanceSlider.setDisable(!config.isAgcEnabled());

        // Bass Boost
        mBassBoostEnabledSwitch.setSelected(config.isBassBoostEnabled());
        float bassBoostDb = config.getBassBoostDb();
        mBassBoostSlider.setValue(bassBoostDb);
        mBassBoostField.setText(String.format("+%.1f dB", bassBoostDb));
        mBassBoostSlider.setDisable(!config.isBassBoostEnabled());

        // Hiss Reduction
        mHissReductionEnabledSwitch.setSelected(config.isHissReductionEnabled());
        float hissDb = config.getHissReductionDb();
        mHissReductionDbSlider.setValue(hissDb);
        mHissReductionDbField.setText(String.format("%.1f dB", hissDb));
        double hissCorner = config.getHissReductionCornerHz();
        mHissReductionCornerSlider.setValue(hissCorner);
        mHissReductionCornerField.setText(String.format("%.0f Hz", hissCorner));
        mHissReductionDbSlider.setDisable(!config.isHissReductionEnabled());
        mHissReductionCornerSlider.setDisable(!config.isHissReductionEnabled());

        // Squelch / Noise Gate (Vox-Send style)
        mSquelchEnabledSwitch.setSelected(config.isNoiseGateEnabled());

        // Threshold is stored as percentage (0-100%)
        float thresholdPercent = config.getNoiseGateThreshold();
        mSquelchThresholdSlider.setValue(thresholdPercent);
        mSquelchThresholdField.setText(String.format("%.1f%%", thresholdPercent));

        // Reduction
        mSquelchReductionSlider.setValue(config.getNoiseGateReduction() * 100.0f);
        mSquelchReductionField.setText((int)(config.getNoiseGateReduction() * 100.0f) + "%");

        // Hold time
        int holdTime = config.getNoiseGateHoldTime();
        mHoldTimeSlider.setValue(holdTime);
        mHoldTimeField.setText(holdTime + " ms");

        // Disable controls if squelch is off
        boolean squelchEnabled = config.isNoiseGateEnabled();
        mSquelchThresholdSlider.setDisable(!squelchEnabled);
        mSquelchReductionSlider.setDisable(!squelchEnabled);
        mHoldTimeSlider.setDisable(!squelchEnabled);
    }

    private void disableAudioFilterControls()
    {
        mInputGainSlider.setValue(2.0);
        mLowPassEnabledSwitch.setSelected(false);
        mLowPassCutoffSlider.setDisable(true);
        mVoiceEnhanceEnabledSwitch.setSelected(false);
        mVoiceEnhanceSlider.setDisable(true);
        mHissReductionEnabledSwitch.setSelected(false);
        mHissReductionDbSlider.setDisable(true);
        mHissReductionCornerSlider.setDisable(true);
        mSquelchEnabledSwitch.setSelected(false);
        mSquelchThresholdSlider.setDisable(true);
        mSquelchReductionSlider.setDisable(true);
    }

    private void saveAudioFilterConfiguration(DecodeConfigNBFM config)
    {
        // Input Gain (store as AGC max gain for compatibility)
        float inputGain = (int) (float)mInputGainSlider.getValue();
        float maxGainDb = (float)(40.0 * Math.log10(inputGain));
        config.setAgcMaxGain(maxGainDb);
        config.setAgcEnabled(true);

        // Low-pass
        config.setLowPassEnabled(mLowPassEnabledSwitch.isSelected());
        config.setLowPassCutoff(mLowPassCutoffSlider.getValue());

        // De-emphasis
        // Voice Enhancement - store amount as AGC target level
        config.setAgcEnabled(mVoiceEnhanceEnabledSwitch.isSelected());
        float voiceAmount = (int) (float)mVoiceEnhanceSlider.getValue();
        // Map 0-100% to -30 to -6 dB range for storage
        float targetLevel = -30.0f + (voiceAmount / 100.0f * 24.0f);
        config.setAgcTargetLevel(targetLevel);

        // Bass Boost
        config.setBassBoostEnabled(mBassBoostEnabledSwitch.isSelected());
        config.setBassBoostDb((float)mBassBoostSlider.getValue());

        // Hiss Reduction
        config.setHissReductionEnabled(mHissReductionEnabledSwitch.isSelected());
        config.setHissReductionDb((float)mHissReductionDbSlider.getValue());
        config.setHissReductionCornerHz(mHissReductionCornerSlider.getValue());

        // Squelch / Noise Gate (Vox-Send style)
        config.setNoiseGateEnabled(mSquelchEnabledSwitch.isSelected());
        config.setNoiseGateThreshold((float)mSquelchThresholdSlider.getValue());  // Already percentage
        config.setNoiseGateReduction((float)mSquelchReductionSlider.getValue() / 100.0f);
        config.setNoiseGateHoldTime((int)mHoldTimeSlider.getValue());
    }

    private void showComparisonUI(DecodeConfigNBFM config, AIAnalysisResult result) {
        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("AI DSP Optimization");
        dialog.setHeaderText(result.getExplanation());

        javafx.scene.control.ButtonType acceptButtonType = new javafx.scene.control.ButtonType("Accept All", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(acceptButtonType, javafx.scene.control.ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Setting"), 0, 0);
        grid.add(new Label("Current"), 1, 0);
        grid.add(new Label("Suggested"), 2, 0);

        grid.add(new Label("Low Pass Enabled"), 0, 1);
        grid.add(new Label(String.valueOf(config.isLowPassEnabled())), 1, 1);
        grid.add(new Label(String.valueOf(result.isLowPassEnabled())), 2, 1);

        grid.add(new Label("Low Pass Cutoff"), 0, 2);
        grid.add(new Label(String.valueOf(config.getLowPassCutoff())), 1, 2);
        grid.add(new Label(String.valueOf(result.getLowPassCutoff())), 2, 2);

        grid.add(new Label("Hiss Reduction"), 0, 3);
        grid.add(new Label(String.valueOf(config.isHissReductionEnabled())), 1, 3);
        grid.add(new Label(String.valueOf(result.isHissReductionEnabled())), 2, 3);

        grid.add(new Label("Noise Gate Enabled"), 0, 4);
        grid.add(new Label(String.valueOf(config.isNoiseGateEnabled())), 1, 4);
        grid.add(new Label(String.valueOf(result.isNoiseGateEnabled())), 2, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == acceptButtonType) {
                return true;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(accepted -> {
            if (accepted) {
                //Applying AI suggestions from the manual button is not a manual filter edit.
                mApplyingAiResult = true;
                mLowPassEnabledSwitch.setSelected(result.isLowPassEnabled());
                mLowPassCutoffSlider.setValue(result.getLowPassCutoff());
                mHissReductionEnabledSwitch.setSelected(result.isHissReductionEnabled());
                mHissReductionDbSlider.setValue(result.getHissReductionDb());
                mHissReductionCornerSlider.setValue(result.getHissReductionCorner());
                mBassBoostEnabledSwitch.setSelected(result.isBassBoostEnabled());
                mBassBoostSlider.setValue(result.getBassBoostDb());
                mSquelchEnabledSwitch.setSelected(result.isNoiseGateEnabled());
                mSquelchThresholdSlider.setValue(result.getNoiseGateThreshold());
                mSquelchReductionSlider.setValue(result.getNoiseGateReduction() * 100.0);
                mSquelchTailEnabledSwitch.setSelected(result.isSquelchTailRemovalEnabled());
                mTailRemovalSpinner.getValueFactory().setValue(result.getSquelchTailRemovalMs());
                mHeadRemovalSpinner.getValueFactory().setValue(result.getSquelchHeadRemovalMs());
                mHoldTimeSlider.setValue(result.getNoiseGateHoldTime());
                modifiedProperty().set(true);
                mApplyingAiResult = false;

                //Record what changed and why + the time so the channel shows its last optimization and the
                //AI can learn from it on the next run.
                recordManualOptimizationSummary(result);
            }
        });
    }

    /**
     * Persists a human-readable summary (time, what changed, why) of an accepted manual optimization so the
     * channel's "last run" display can show it and the optimizer can learn from it on the next run.
     */
    private void recordManualOptimizationSummary(AIAnalysisResult result) {
        if(getItem() == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[MANUAL ")
          .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date()))
          .append("] ");
        if(result.getImprovements() != null && !result.getImprovements().isEmpty()) {
            sb.append("Changes: ").append(result.getImprovements());
        }
        if(result.getIssuesFound() != null && !result.getIssuesFound().isEmpty()) {
            sb.append("  Why: ").append(result.getIssuesFound());
        }
        String name = getItem().getName();
        mUserPreferences.getAIPreference().setNBFMLastOptimizeSummary(name, sb.toString());
        mUserPreferences.getAIPreference().setNBFMLastOptimizeMs(name, System.currentTimeMillis());
        updateLastRunDisplay();
    }

    /**
     * Shows, in the AI-optimize status label next to the manual run button, when this channel was last
     * optimized (auto or manual) with the full "what changed and why" summary available in the tooltip.
     * Does nothing if the channel has never been optimized (leaves the existing readiness message).
     */
    private void updateLastRunDisplay() {
        if(mAIOptimizeStatusLabel == null || getItem() == null) {
            return;
        }
        String name = getItem().getName();
        long lastMs = mUserPreferences.getAIPreference().getNBFMLastOptimizeMs(name);
        String summary = mUserPreferences.getAIPreference().getNBFMLastOptimizeSummary(name);
        if(lastMs <= 0 || summary == null || summary.isEmpty()) {
            return;
        }
        String when = new java.text.SimpleDateFormat("MMM d, HH:mm").format(new java.util.Date(lastMs));
        mAIOptimizeStatusLabel.setText("Last optimized " + when + " (hover for details)");
        mAIOptimizeStatusLabel.setStyle("-fx-text-fill: #444444;");
        mAIOptimizeStatusLabel.setTooltip(new javafx.scene.control.Tooltip(summary));
    }

    private void handleAIOptimizeClick() {
        //Guard against the feature being toggled off after this editor was opened.
        if (!mUserPreferences.getAIPreference().isNBFMAudioAutoOptimizeEnabled()) {
            mAIOptimizeStatusLabel.setText("Enable 'Auto-Optimize NBFM Audio Filters' in AI preferences first");
            mAIOptimizeStatusLabel.setStyle("-fx-text-fill: #cc0000;");
            return;
        }

        Channel channel = getItem();
        if(channel == null) {
            mAIOptimizeStatusLabel.setText("No channel selected");
            mAIOptimizeStatusLabel.setStyle("-fx-text-fill: #cc0000;");
            mAIOptimizeButton.setDisable(true);
            return;
        }

        String channelName = channel.getName();
        DecodeConfigNBFM config = (DecodeConfigNBFM)channel.getDecodeConfiguration();
        String priorSummary = mUserPreferences.getAIPreference().getNBFMLastOptimizeSummary(channelName);

        mAIOptimizeButton.setDisable(true);
        mAIOptimizeStatusLabel.setText("Analyzing... This can take up to 60 seconds.");
        mAIOptimizeStatusLabel.setStyle("-fx-text-fill: #0066cc;");

        Thread optimizerThread = new Thread(() -> {
            try {
                int eventCount = AudioBufferManager.getBufferedEventCount(mUserPreferences, channelName);
                if (eventCount < 5) {
                    updateAIOptimizeStatus(channel,
                            "Needs at least 5 audio events saved for this channel to optimize. Found: " + eventCount,
                            "-fx-text-fill: #cc0000;", false);
                    return;
                }

                AudioBufferManager bufferManager = new AudioBufferManager(mUserPreferences, channelName);
                java.util.List<java.util.List<float[]>> recent = bufferManager.getBufferedEvents();
                if(recent.size() < 5) {
                    updateAIOptimizeStatus(channel,
                            "Needs at least 5 readable audio events saved for this channel to optimize. Found: " + recent.size(),
                            "-fx-text-fill: #cc0000;", false);
                    return;
                }

                AIAudioOptimizer optimizer = new AIAudioOptimizer(mUserPreferences);
                AIAnalysisResult result = optimizer.analyzeRawAudio(config, recent, priorSummary);

                Platform.runLater(() -> showAIOptimizeResult(channel, config, result));
            } catch (Exception e) {
                updateAIOptimizeStatus(channel, "Analysis failed: " + formatAIOptimizeException(e),
                        "-fx-text-fill: #cc0000;", false);
            }
        }, "NBFM AI Audio Optimizer");
        optimizerThread.setDaemon(true);
        optimizerThread.start();
    }

    private void showAIOptimizeResult(Channel channel, DecodeConfigNBFM config, AIAnalysisResult result) {
        if(getItem() != channel) {
            return;
        }

        try {
            showComparisonUI(config, result);
            mAIOptimizeStatusLabel.setText("Analysis complete.");
            mAIOptimizeStatusLabel.setStyle("-fx-text-fill: #009900;");
        } catch(Exception e) {
            mAIOptimizeStatusLabel.setText("Analysis failed: " + formatAIOptimizeException(e));
            mAIOptimizeStatusLabel.setStyle("-fx-text-fill: #cc0000;");
        } finally {
            mAIOptimizeButton.setDisable(false);
        }
    }

    private void updateAIOptimizeStatus(Channel channel, String status, String style, boolean disableButton) {
        Platform.runLater(() -> {
            if(getItem() != channel) {
                return;
            }

            mAIOptimizeStatusLabel.setText(status);
            mAIOptimizeStatusLabel.setStyle(style);
            mAIOptimizeButton.setDisable(disableButton);
        });
    }

    private static String formatAIOptimizeException(Exception e) {
        String message = e.getMessage();
        if(message == null || message.isEmpty()) {
            return e.getClass().getSimpleName();
        }

        return message;
    }
    @Override
    public void setItem(Channel channel) {
        mLoadingConfiguration = true;
        super.setItem(channel);
        mLoadingConfiguration = false;
        //Reflect this channel's AI auto-optimize opt-out state in the notification banner.
        updateAiLockoutBanner();
        if (mAIOptimizeButton != null && mAIOptimizeStatusLabel != null) {
            if (channel == null) {
                mAIOptimizeButton.setDisable(true);
                mAIOptimizeStatusLabel.setText("No channel selected");
                return;
            }

            //Manual optimization requires the feature to be enabled (#9): the feature toggle means
            //"available for manual runs", and call audio is only buffered for analysis while it is on.
            if (!mUserPreferences.getAIPreference().isNBFMAudioAutoOptimizeEnabled()) {
                mAIOptimizeButton.setDisable(true);
                mAIOptimizeStatusLabel.setText("Enable 'Auto-Optimize NBFM Audio Filters' in AI preferences to use this");
                mAIOptimizeStatusLabel.setStyle("-fx-text-fill: #888888;");
                return;
            }

            mAIOptimizeButton.setDisable(true);
            mAIOptimizeStatusLabel.setText("Checking for buffered events...");

            new Thread(() -> {
                int eventCount = io.github.dsheirer.module.decode.nbfm.ai.AudioBufferManager.getBufferedEventCount(mUserPreferences, channel.getName());
                javafx.application.Platform.runLater(() -> {
                    // Prevent updates if the selected channel has changed while we were loading
                    if (getItem() != channel) return;

                    if (eventCount < 5) {
                        mAIOptimizeButton.setDisable(true);
                        mAIOptimizeStatusLabel.setText("Needs at least 5 audio events saved for this channel to optimize. Found: " + eventCount);
                        mAIOptimizeStatusLabel.setStyle("-fx-text-fill: #cc0000;");
                    } else {
                        mAIOptimizeButton.setDisable(false);
                        mAIOptimizeStatusLabel.setText("Click to run Gemini AI analysis on this channel's audio");
                        mAIOptimizeStatusLabel.setStyle("");
                        //If this channel has been optimized before (auto or manual), show that instead.
                        updateLastRunDisplay();
                    }
                });
            }).start();
        }
    }

    @Override
    protected void setEventLogConfiguration(EventLogConfiguration config)
    {
        getEventLogConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveEventLogConfiguration()
    {
        getEventLogConfigurationEditor().save();

        if(getEventLogConfigurationEditor().getItem().getLoggers().isEmpty())
        {
            getItem().setEventLogConfiguration(null);
        }
        else
        {
            getItem().setEventLogConfiguration(getEventLogConfigurationEditor().getItem());
        }
    }

    @Override
    protected void setAuxDecoderConfiguration(AuxDecodeConfiguration config)
    {
        getAuxDecoderConfigurationEditor().setItem(config);
    }

    @Override
    protected void saveAuxDecoderConfiguration()
    {
        getAuxDecoderConfigurationEditor().save();

        if(getAuxDecoderConfigurationEditor().getItem().getAuxDecoders().isEmpty())
        {
            getItem().setAuxDecodeConfiguration(null);
        }
        else
        {
            getItem().setAuxDecodeConfiguration(getAuxDecoderConfigurationEditor().getItem());
        }
    }

    @Override
    protected void setRecordConfiguration(RecordConfiguration config)
    {
        if(config != null)
        {
            getBasebandRecordSwitch().setDisable(false);
            getBasebandRecordSwitch().selectedProperty().set(config.contains(RecorderType.BASEBAND));
        }
        else
        {
            getBasebandRecordSwitch().selectedProperty().set(false);
            getBasebandRecordSwitch().setDisable(true);
        }
    }

    @Override
    protected void saveRecordConfiguration()
    {
        RecordConfiguration config = new RecordConfiguration();

        if(getBasebandRecordSwitch().selectedProperty().get())
        {
            config.addRecorder(RecorderType.BASEBAND);
        }

        getItem().setRecordConfiguration(config);
    }

    @Override
    protected void setSourceConfiguration(SourceConfiguration config)
    {
        getSourceConfigurationEditor().setSourceConfiguration(config);
    }

    @Override
    protected void saveSourceConfiguration()
    {
        getSourceConfigurationEditor().save();
        SourceConfiguration sourceConfiguration = getSourceConfigurationEditor().getSourceConfiguration();
        getItem().setSourceConfiguration(sourceConfiguration);
    }

    private Label createHelpIcon(String tooltipText) {
        IconNode iconNode = new IconNode(FontAwesome.INFO_CIRCLE);
        iconNode.setIconSize(14);
        iconNode.setFill(Color.GRAY);
        Label label = new Label("", iconNode);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(400);
        label.setTooltip(tooltip);
        return label;
    }
}
