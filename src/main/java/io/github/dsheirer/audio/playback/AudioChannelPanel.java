/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.audio.playback;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.dsp.tone.TwoToneDetectedEvent;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashSet;
import java.util.Set;

/**
 * UI to wrap an audio channel and provide display of metadata and playback state information.
 */
public class AudioChannelPanel extends HBox implements Listener<AudioEvent>, SettingChangeListener
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioChannelPanel.class);

    private final AudioChannel mAudioChannel;
    private final AliasModel mAliasModel;
    private final IconModel mIconModel;
    private final SettingsManager mSettingsManager;
    private final UserPreferences mUserPreferences;
    private final BroadcastModel mBroadcastModel;
    private final TalkgroupFormatPreference mTalkgroupFormatPreference;
    private Identifier mIdentifier;
    private List<Alias> mAliases = Collections.emptyList();
    private final Lock mLock = new ReentrantLock();

    private final Font mFont = Font.font("System", 13);
    private final Font mBoldFont = Font.font("System", FontWeight.BOLD, 13);

    private final Label mMutedLabel = new Label("M");
    private final Label mChannelName;
    private final Label mIdentifierLabel = new Label("-----");
    private final Label mTwoToneAlertLabel = new Label("");
    private final ImageView mIconView = new ImageView();
    private final HBox mStreamIconsPanel = new HBox(2);

    private PauseTransition mTwoToneClearTimer;

    // MVC Properties
    private final BooleanProperty mMutedProperty = new SimpleBooleanProperty(false);
    private final StringProperty mIdentifierTextProperty = new SimpleStringProperty("-----");
    private final ObjectProperty<Image> mIconImageProperty = new SimpleObjectProperty<>();
    private final StringProperty mTwoToneAlertTextProperty = new SimpleStringProperty("");
    private final BooleanProperty mTwoToneAlertActiveProperty = new SimpleBooleanProperty(false);

    public AudioChannelPanel(AudioChannel audioChannel, AliasModel aliasModel, IconModel iconModel,
                             SettingsManager settingsManager, UserPreferences userPreferences, BroadcastModel broadcastModel)
    {
        mIconModel = iconModel;
        mSettingsManager = settingsManager;
        mSettingsManager.getSettingsModel().addListener(this);
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
        mBroadcastModel = broadcastModel;
        mTalkgroupFormatPreference = mUserPreferences.getTalkgroupFormatPreference();
        mAudioChannel = audioChannel;

        mChannelName = new Label(mAudioChannel != null ? mAudioChannel.getChannelName() : " ");

        if(mAudioChannel != null)
        {
            mAudioChannel.addAudioEventListener(this);
            mAudioChannel.setIdentifierCollectionListener(new AudioMetadataProcessor());
        }

        init();
        setupBindings();
    }

    private void setupBindings() {
        mMutedLabel.visibleProperty().bind(mMutedProperty);
        mMutedLabel.managedProperty().bind(mMutedProperty);

        mIdentifierLabel.textProperty().bind(mIdentifierTextProperty);
        mIconView.imageProperty().bind(mIconImageProperty);

        mTwoToneAlertLabel.textProperty().bind(mTwoToneAlertTextProperty);
        mTwoToneAlertLabel.visibleProperty().bind(mTwoToneAlertActiveProperty);
        mTwoToneAlertLabel.managedProperty().bind(mTwoToneAlertActiveProperty);

        mIdentifierLabel.visibleProperty().bind(mTwoToneAlertActiveProperty.not());
        mIdentifierLabel.managedProperty().bind(mTwoToneAlertActiveProperty.not());

        mIconView.visibleProperty().bind(mTwoToneAlertActiveProperty.not());
        mIconView.managedProperty().bind(mTwoToneAlertActiveProperty.not());
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            updateLabels();
        }
    }

    @Subscribe
    public void onTwoToneDetected(TwoToneDetectedEvent event) {
        if (mAudioChannel != null && event.getChannel() != null && event.getChannel().equals(mAudioChannel.getChannelName())) {
            Platform.runLater(() -> {
                mTwoToneAlertTextProperty.set(event.getMessage());
                mTwoToneAlertActiveProperty.set(true);

                if (mTwoToneClearTimer != null) {
                    mTwoToneClearTimer.stop();
                } else {
                    mTwoToneClearTimer = new PauseTransition(Duration.seconds(10));
                    mTwoToneClearTimer.setOnFinished(e -> mTwoToneAlertActiveProperty.set(false));
                }
                mTwoToneClearTimer.playFromStart();
            });
        }
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);

        if(mTwoToneClearTimer != null) {
            mTwoToneClearTimer.stop();
        }

        if(mAudioChannel != null)
        {
            mAudioChannel.removeAudioEventListener(this);
            mAudioChannel.removeAudioMetadataListener();
        }
    }

    private void init()
    {
        MyEventBus.getGlobalEventBus().register(this);

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(5);
        setPadding(new Insets(2, 5, 2, 5));
        getStyleClass().add("audio-channel-panel");

        mMutedLabel.setFont(mFont);
        mMutedLabel.setTextFill(Color.RED);

        mChannelName.setFont(mBoldFont);

        mIconView.setFitHeight(18);
        mIconView.setFitWidth(18);
        mIconView.setPreserveRatio(true);

        mIdentifierLabel.setFont(mBoldFont);

        mStreamIconsPanel.setAlignment(Pos.CENTER_LEFT);

        mTwoToneAlertLabel.setFont(mBoldFont);
        mTwoToneAlertLabel.setTextFill(Color.RED);

        getChildren().addAll(mMutedLabel, mChannelName, mIconView, mIdentifierLabel, mStreamIconsPanel, mTwoToneAlertLabel);
    }

    @Override
    public void receive(final AudioEvent audioEvent)
    {
        switch(audioEvent.getType())
        {
            case AUDIO_STOPPED:
                resetLabels();
                break;
            case AUDIO_MUTED:
            case AUDIO_UNMUTED:
                Platform.runLater(() -> mMutedProperty.set(mAudioChannel.isMuted()));
                break;
            default:
                break;
        }
    }

    private void resetLabels()
    {
        mLock.lock();
        try
        {
            boolean updated = mIdentifier != null;
            mIdentifier = null;
            mAliases = Collections.emptyList();

            if(updated)
            {
                updateLabels();
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    private void updateIdentifiers(IdentifierCollection identifierCollection)
    {
        if(identifierCollection == null || identifierCollection.isEmpty())
        {
            resetLabels();
            return;
        }

        List<Identifier> toIds = identifierCollection.getIdentifiers(IdentifierClass.USER, Role.TO);

        if(toIds.isEmpty())
        {
            resetLabels();
            return;
        }

        boolean updated = false;
        mLock.lock();
        try
        {
            if(toIds.size() == 1)
            {
                Identifier currentIdentifier = mIdentifier;

                if(currentIdentifier == null || currentIdentifier != toIds.get(0))
                {
                    mIdentifier = toIds.get(0);
                    AliasList aliasList = mAliasModel.getAliasList(identifierCollection);

                    if(aliasList != null)
                    {
                        mAliases = aliasList.getAliases(mIdentifier);
                    }
                    updated = true;
                }
            }
            else
            {
                mIdentifier = toIds.get(0);
                AliasList aliasList = mAliasModel.getAliasList(identifierCollection);

                if(aliasList != null)
                {
                    mAliases = aliasList.getAliases(mIdentifier);
                }
                updated = true;
            }

            if(updated)
            {
                updateLabels();
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    private void updateLabels()
    {
        String identifier = null;
        String iconName = null;

        mLock.lock();
        try
        {
            if(mAliases.size() == 1)
            {
                identifier = mAliases.get(0).getName();
                iconName = mAliases.get(0).getIconName();
            }
            else if(mAliases.size() > 1)
            {
                identifier = Joiner.on(", ").skipNulls().join(mAliases);
            }

            if(identifier == null && mIdentifier != null)
            {
                identifier = mTalkgroupFormatPreference.format(mIdentifier);
            }

            if(identifier == null)
            {
                identifier = "-----";
            }

            final String finalIdentifier = identifier;
            final String finalIconName = iconName;

            // Collect stream info
            final Set<AbstractAudioBroadcaster<?>> activeStreams = new HashSet<>();
            if (mAliases != null) {
                Set<String> processedNames = new HashSet<>();
                for (Alias alias : mAliases) {
                    for (BroadcastChannel bc : alias.getBroadcastChannels()) {
                        AbstractAudioBroadcaster<?> broadcaster = mBroadcastModel.getBroadcaster(bc.getChannelName());
                        if (broadcaster != null && broadcaster.getBroadcastConfiguration().isEnabled()) {
                            if (!processedNames.contains(bc.getChannelName())) {
                                processedNames.add(bc.getChannelName());
                                activeStreams.add(broadcaster);
                            }
                        }
                    }
                }
            }

            Platform.runLater(() -> {
                mIdentifierTextProperty.set(finalIdentifier);

                if (finalIconName != null) {
                    io.github.dsheirer.icon.Icon icon = mIconModel.getIcon(finalIconName);
                    if (icon != null && icon.getFxImage() != null) {
                        mIconImageProperty.set(icon.getFxImage());
                    } else {
                        mIconImageProperty.set(null);
                    }
                } else {
                    mIconImageProperty.set(null);
                }

                mStreamIconsPanel.getChildren().clear();
                for (AbstractAudioBroadcaster<?> broadcaster : activeStreams) {
                    io.github.dsheirer.icon.Icon streamIconDef = mIconModel.getIcon(broadcaster.getBroadcastConfiguration().getBroadcastServerType().getIconPath());
                    if (streamIconDef != null && streamIconDef.getFxImage() != null) {
                        ImageView streamIconView = new ImageView(streamIconDef.getFxImage());
                        streamIconView.setFitHeight(14);
                        streamIconView.setFitWidth(14);
                        streamIconView.setPreserveRatio(true);
                        Tooltip tooltip = new Tooltip(broadcaster.getBroadcastConfiguration().getName());
                        Tooltip.install(streamIconView, tooltip);
                        mStreamIconsPanel.getChildren().add(streamIconView);
                    }
                }
            });
        }
        finally
        {
            mLock.unlock();
        }
    }

    public class AudioMetadataProcessor implements Listener<IdentifierCollection>
    {
        @Override
        public void receive(final IdentifierCollection identifierCollection)
        {
            updateIdentifiers(identifierCollection);
        }
    }

    @Override
    public void settingChanged(Setting setting)
    {
        if(setting instanceof ColorSetting)
        {
            // Do nothing for color settings as we're migrating to CSS
        }
    }

    @Override
    public void settingDeleted(Setting setting)
    {
    }
}
