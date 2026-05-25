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

package io.github.dsheirer.gui.preference.call;

import io.github.dsheirer.audio.broadcast.PatchGroupStreamingOption;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.duplicate.CallManagementPreference;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Preference settings for audio call management for duplicate calls and patch group streaming.
 */
public class CallManagementPreferenceEditor extends VBox
{
    private final static Logger mLog = LoggerFactory.getLogger(CallManagementPreferenceEditor.class);
    private CallManagementPreference mPreference;
    private VBox mEditorPane;
    private ToggleSwitch mDetectDuplicateTalkgroups;
    private ToggleSwitch mDetectDuplicateRadios;
    private ToggleSwitch mSuppressDuplicateListening;
    private ToggleSwitch mSuppressDuplicateRecording;
    private ToggleSwitch mSuppressDuplicateStreaming;
    private ComboBox<PatchGroupStreamingOption> mPatchGroupStreamingOptionComboBox;

    /**
     * Constructs an instance
     */
    public CallManagementPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getCallManagementPreference();

        VBox.setVgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    private VBox getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new VBox();
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));
            mEditorPane.setSpacing(20);

            // Duplicate Call Detection Section
            Label detectionHeader = new Label("Duplicate Call Detection");
            detectionHeader.getStyleClass().add("hig-section-header");
            Label detectionDesc = new Label("Detect duplicate calls across channels that share a common System name.");
            detectionDesc.getStyleClass().add("kennebec-secondary-text");
            detectionDesc.setPadding(new Insets(0, 10, 5, 10));

            SettingsCard detectionCard = new SettingsCard();
            detectionCard.getChildren().add(new SettingsRow("Detect by Talkgroup", getDetectDuplicateTalkgroups()));
            detectionCard.getChildren().add(new SettingsRow("Detect by Radio ID", getDetectDuplicateRadios()));

            Label warningLabel = new Label("Note: Enabling Radio ID detection can cause unintended side effects (e.g., stopping recording on one talkgroup if a radio broadcasts to two simultaneously).");
            warningLabel.setWrapText(true);
            warningLabel.getStyleClass().add("kennebec-secondary-text");
            warningLabel.setPadding(new Insets(5, 10, 0, 10));

            VBox detectionBox = new VBox(detectionHeader, detectionDesc, detectionCard, warningLabel);

            // Duplicate Call Suppression Section
            Label suppressionHeader = new Label("Duplicate Call Suppression");
            suppressionHeader.getStyleClass().add("hig-section-header");
            Label suppressionDesc = new Label("When a duplicate call is detected, suppress the audio during:");
            suppressionDesc.getStyleClass().add("kennebec-secondary-text");
            suppressionDesc.setPadding(new Insets(0, 10, 5, 10));

            SettingsCard suppressionCard = new SettingsCard();
            suppressionCard.getChildren().add(new SettingsRow("Listening", getSuppressDuplicateListening()));
            suppressionCard.getChildren().add(new SettingsRow("Recording", getSuppressDuplicateRecording()));
            suppressionCard.getChildren().add(new SettingsRow("Streaming", getSuppressDuplicateStreaming()));

            VBox suppressionBox = new VBox(suppressionHeader, suppressionDesc, suppressionCard);

            // Patch Group Streaming Section
            Label patchHeader = new Label("Patch Group Streaming");
            patchHeader.getStyleClass().add("hig-section-header");

            SettingsCard patchCard = new SettingsCard();
            patchCard.getChildren().add(new SettingsRow("Stream patch group call as", getPatchGroupStreamingOptionComboBox()));

            VBox patchBox = new VBox(patchHeader, patchCard);

            mEditorPane.getChildren().addAll(detectionBox, suppressionBox, patchBox);
        }

        return mEditorPane;
    }

    private ToggleSwitch getDetectDuplicateTalkgroups()
    {
        if(mDetectDuplicateTalkgroups == null)
        {
            mDetectDuplicateTalkgroups = new ToggleSwitch();
            mDetectDuplicateTalkgroups.setSelected(mPreference.isDuplicateCallDetectionByTalkgroupEnabled());
            mDetectDuplicateTalkgroups.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateCallDetectionByTalkgroupEnabled(newValue));
            mDetectDuplicateTalkgroups.setTooltip(new Tooltip("Detect duplicate calls by matching talkgroup or patchgroup values"));
        }

        return mDetectDuplicateTalkgroups;
    }

    private ToggleSwitch getDetectDuplicateRadios()
    {
        if(mDetectDuplicateRadios == null)
        {
            mDetectDuplicateRadios = new ToggleSwitch();
            mDetectDuplicateRadios.setSelected(mPreference.isDuplicateCallDetectionByRadioEnabled());
            mDetectDuplicateRadios.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateCallDetectionByRadioEnabled(newValue));
            mDetectDuplicateRadios.setTooltip(new Tooltip("Detect duplicate calls by matching radio identifiers"));
        }

        return mDetectDuplicateRadios;
    }

    private ToggleSwitch getSuppressDuplicateListening()
    {
        if(mSuppressDuplicateListening == null)
        {
            mSuppressDuplicateListening = new ToggleSwitch();
            mSuppressDuplicateListening.disableProperty()
                .bind(Bindings.and(getDetectDuplicateTalkgroups().selectedProperty().not(),
                    getDetectDuplicateRadios().selectedProperty().not()));
            mSuppressDuplicateListening.setSelected(mPreference.isDuplicatePlaybackSuppressionEnabled());
            mSuppressDuplicateListening.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicatePlaybackSuppressionEnabled(newValue));
            mSuppressDuplicateListening.setTooltip(new Tooltip("Suppress playing audio for duplicate calls locally"));
        }

        return mSuppressDuplicateListening;
    }

    private ToggleSwitch getSuppressDuplicateRecording()
    {
        if(mSuppressDuplicateRecording == null)
        {
            mSuppressDuplicateRecording = new ToggleSwitch();
            mSuppressDuplicateRecording.disableProperty()
                .bind(Bindings.and(getDetectDuplicateTalkgroups().selectedProperty().not(),
                    getDetectDuplicateRadios().selectedProperty().not()));
            mSuppressDuplicateRecording.setSelected(mPreference.isDuplicateRecordingSuppressionEnabled());
            mSuppressDuplicateRecording.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateRecordingSuppressionEnabled(newValue));
            mSuppressDuplicateRecording.setTooltip(new Tooltip("Suppress saving audio for duplicate calls to disk"));
        }

        return mSuppressDuplicateRecording;
    }

    private ToggleSwitch getSuppressDuplicateStreaming()
    {
        if(mSuppressDuplicateStreaming == null)
        {
            mSuppressDuplicateStreaming = new ToggleSwitch();
            mSuppressDuplicateStreaming.disableProperty()
                .bind(Bindings.and(getDetectDuplicateTalkgroups().selectedProperty().not(),
                    getDetectDuplicateRadios().selectedProperty().not()));
            mSuppressDuplicateStreaming.setSelected(mPreference.isDuplicateStreamingSuppressionEnabled());
            mSuppressDuplicateStreaming.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateStreamingSuppressionEnabled(newValue));
            mSuppressDuplicateStreaming.setTooltip(new Tooltip("Suppress broadcasting audio for duplicate calls over the internet"));
        }

        return mSuppressDuplicateStreaming;
    }

    /**
     * Combo box for presenting the patch group streaming options.
     * @return combo box.
     */
    private ComboBox<PatchGroupStreamingOption> getPatchGroupStreamingOptionComboBox()
    {
        if(mPatchGroupStreamingOptionComboBox == null)
        {
            ObservableList<PatchGroupStreamingOption> options = FXCollections.observableArrayList();
            options.addAll(PatchGroupStreamingOption.values());
            mPatchGroupStreamingOptionComboBox = new ComboBox<>(options);
            mPatchGroupStreamingOptionComboBox.getSelectionModel().select(mPreference.getPatchGroupStreamingOption());
            mPatchGroupStreamingOptionComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> mPreference.setPatchGroupStreamingOption(newValue));
            mPatchGroupStreamingOptionComboBox.setTooltip(new Tooltip("Determine whether patch group calls stream via the Supergroup or the Member Sub-groups."));
        }

        return mPatchGroupStreamingOptionComboBox;
    }
}
