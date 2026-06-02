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
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.duplicate.CallManagementPreference;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        // Duplicate Call Detection section
        Label detectionHeader = new Label("Duplicate Call Detection");
        detectionHeader.getStyleClass().add("hig-section-header");
        getChildren().add(detectionHeader);

        Label detectionDescription = new Label("Detect duplicate calls across channels that share a common " +
            "System name in each channel configuration.");
        detectionDescription.setWrapText(true);
        detectionDescription.getStyleClass().add("kennebec-secondary-text");
        detectionDescription.setPadding(new Insets(0, 10, 0, 10));
        getChildren().add(detectionDescription);

        SettingsCard detectionCard = new SettingsCard();
        detectionCard.getChildren().add(new SettingsRow("Talkgroup", getDetectDuplicateTalkgroups()));
        detectionCard.getChildren().add(new SettingsRow("Radio ID", getDetectDuplicateRadios()));
        getChildren().add(detectionCard);

        Label warningLabel = new Label("Note: be careful when enabling duplicate call detection by Radio ID " +
            "because this can produce unintended side-effects.  For example, if you have two talkgroups with " +
            "talkgroup 1 set to record and talkgroup 2 set to stream and dispatch radio ID 1234 makes a " +
            "simultaneous call to both talkgroup 1 and talkgroup 2, there is no way to control which call audio " +
            "(talkgroup 1 or 2) gets flagged as the duplicate call and therefore either the audio for talkgroup 1 " +
            "doesn't record, or the audio for talkgroup 2 doesn't stream.");
        warningLabel.setWrapText(true);
        warningLabel.getStyleClass().add("kennebec-secondary-text");
        warningLabel.setPadding(new Insets(0, 10, 0, 10));
        getChildren().add(warningLabel);

        // Duplicate Call Suppression section
        Label suppressionHeader = new Label("Duplicate Call Suppression");
        suppressionHeader.getStyleClass().add("hig-section-header");
        getChildren().add(suppressionHeader);

        Label suppressionDescription = new Label("When duplicate call audio is detected, suppress the audio during:");
        suppressionDescription.setWrapText(true);
        suppressionDescription.getStyleClass().add("kennebec-secondary-text");
        suppressionDescription.setPadding(new Insets(0, 10, 0, 10));
        getChildren().add(suppressionDescription);

        SettingsCard suppressionCard = new SettingsCard();
        suppressionCard.getChildren().add(new SettingsRow("Listening", getSuppressDuplicateListening()));
        suppressionCard.getChildren().add(new SettingsRow("Recording", getSuppressDuplicateRecording()));
        suppressionCard.getChildren().add(new SettingsRow("Streaming", getSuppressDuplicateStreaming()));
        getChildren().add(suppressionCard);

        // Patch Group Streaming section
        Label patchGroupHeader = new Label("Patch Group Streaming");
        patchGroupHeader.getStyleClass().add("hig-section-header");
        getChildren().add(patchGroupHeader);

        Label patchGroupDescription = new Label("Stream a patch group call as:");
        patchGroupDescription.setWrapText(true);
        patchGroupDescription.getStyleClass().add("kennebec-secondary-text");
        patchGroupDescription.setPadding(new Insets(0, 10, 0, 10));
        getChildren().add(patchGroupDescription);

        SettingsCard patchGroupCard = new SettingsCard();
        patchGroupCard.getChildren().add(new SettingsRow("Streaming Option", getPatchGroupStreamingOptionComboBox()));
        getChildren().add(patchGroupCard);
    }

    private ToggleSwitch getDetectDuplicateTalkgroups()
    {
        if(mDetectDuplicateTalkgroups == null)
        {
            mDetectDuplicateTalkgroups = new ToggleSwitch();
            mDetectDuplicateTalkgroups.setSelected(mPreference.isDuplicateCallDetectionByTalkgroupEnabled());
            mDetectDuplicateTalkgroups.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mPreference.setDuplicateCallDetectionByTalkgroupEnabled(newValue));
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
        }

        return mPatchGroupStreamingOptionComboBox;
    }
}
