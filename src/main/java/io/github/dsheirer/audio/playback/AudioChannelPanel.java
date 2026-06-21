

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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

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
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;

import javafx.application.Platform;
import javafx.scene.text.Font;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;






import io.github.dsheirer.dsp.tone.TwoToneDetectedEvent;

import javafx.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * UI to wrap an audio channel and provide display of metadata and playback state information.
 */
public class AudioChannelPanel extends HBox implements Listener<AudioEvent>, SettingChangeListener
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(AudioChannelPanel.class);

    public static final String PROPERTY_PREFIX = "audio.channel.panel.color.";
    public static final String PROPERTY_COLOR_BACKGROUND = PROPERTY_PREFIX + "background";
    public static final String PROPERTY_COLOR_LABEL = PROPERTY_PREFIX + "label";
    public static final String PROPERTY_COLOR_MUTED = PROPERTY_PREFIX + "muted";
    public static final String PROPERTY_COLOR_VALUE = PROPERTY_PREFIX + "value";

    private final AudioChannel mAudioChannel;
    private final AliasModel mAliasModel;
    private final IconModel mIconModel;
    private final SettingsManager mSettingsManager;
    private final UserPreferences mUserPreferences;
    private final BroadcastModel mBroadcastModel;
    private final io.github.dsheirer.playlist.PlaylistManager mPlaylistManager;
    private final TalkgroupFormatPreference mTalkgroupFormatPreference;
    private Identifier mIdentifier;
    private List<Alias> mAliases = Collections.EMPTY_LIST;
    private final Lock mLock = new ReentrantLock();

    private final javafx.scene.text.Font mFont = javafx.scene.text.Font.font("SansSerif", 13);
    private final Color mBackgroundColor;
    private final Color mLabelColor;
    private final Color mMutedColor;
    private final Color mValueColor;
    private final Label mMutedLabel = new Label("M");
    private Label mChannelName = new Label(" ");
    private final Label mIconLabel = new Label(" ");

    private final Label mIdentifierLabel = new Label("-----");

    private final Label mTwoToneAlertLabel = new Label("");
    private final HBox mStreamIconsPanel = new HBox(4);

    private javafx.animation.PauseTransition mTwoToneClearTimer;


    /**
     * Constructs an instance
     * @param audioChannel to wrap by this panel
     * @param aliasModel for alias lookup
     * @param iconModel for icon lookup
     * @param settingsManager for monitoring changes to tone insertion
     * @param userPreferences for lookup of tone and other preferences
     */
    public AudioChannelPanel(AudioChannel audioChannel, AliasModel aliasModel, IconModel iconModel,
                             SettingsManager settingsManager, UserPreferences userPreferences, BroadcastModel broadcastModel, io.github.dsheirer.playlist.PlaylistManager playlistManager)
    {
        mIconModel = iconModel;
        mSettingsManager = settingsManager;
        mSettingsManager.getSettingsModel().addListener(this);
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
        mBroadcastModel = broadcastModel;
        mPlaylistManager = playlistManager;
        mTalkgroupFormatPreference = mUserPreferences.getTalkgroupFormatPreference();
        mAudioChannel = audioChannel;

        if(mAudioChannel != null)
        {
            mAudioChannel.addAudioEventListener(this);
            mAudioChannel.setIdentifierCollectionListener(new AudioMetadataProcessor());
        }

        mBackgroundColor = SystemProperties.getInstance().get(PROPERTY_COLOR_BACKGROUND, javafx.scene.paint.Color.GRAY);
        mLabelColor = SystemProperties.getInstance().get(PROPERTY_COLOR_LABEL, javafx.scene.paint.Color.GRAY);
        mMutedColor = SystemProperties.getInstance().get(PROPERTY_COLOR_MUTED, Color.RED);
        mValueColor = SystemProperties.getInstance().get(PROPERTY_COLOR_VALUE, javafx.scene.paint.Color.GRAY);

        init();
    }

    /**
     * Receives preference update notifications via the event bus
     * @param preferenceType that was updated
     */
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
                mTwoToneAlertLabel.setText(event.getMessage());
                mTwoToneAlertLabel.setVisible(true);
                mIdentifierLabel.setVisible(false);
                mIconLabel.setVisible(false);

                if (mTwoToneClearTimer != null && mTwoToneClearTimer.getStatus() == javafx.animation.Animation.Status.RUNNING) {
                    mTwoToneClearTimer.playFromStart();
                } else {
                    mTwoToneClearTimer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(10000));
                    mTwoToneClearTimer.setOnFinished(e -> {
                        mTwoToneAlertLabel.setVisible(false);
                        mIdentifierLabel.setVisible(true);
                        mIconLabel.setVisible(true);
                    });
                    mTwoToneClearTimer.play();
                }
            });
        }
    }

    public void dispose()
    {
        //Deregister from receiving preference update notifications
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

    private javafx.scene.image.ImageView mArtworkView;
    private StackPane mArtworkContainer;

    private void init()
    {
        //Register to receive preference updates
        MyEventBus.getGlobalEventBus().register(this);

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(2, 6, 2, 6));

        // Artwork
        mArtworkView = new ImageView();
        mArtworkView.setFitWidth(56);
        mArtworkView.setFitHeight(56);
        mArtworkView.setPreserveRatio(false);
        mArtworkView.setSmooth(true);

        javafx.scene.shape.Rectangle artClip = new javafx.scene.shape.Rectangle(56, 56);
        artClip.setArcWidth(12);
        artClip.setArcHeight(12);
        mArtworkView.setClip(artClip);

        javafx.scene.effect.DropShadow artShadow = new javafx.scene.effect.DropShadow();
        artShadow.setRadius(8);
        artShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        artShadow.setOffsetY(2);
        mArtworkView.setEffect(artShadow);

        mArtworkContainer = new StackPane(mArtworkView);
        mArtworkContainer.setVisible(false);
        mArtworkContainer.setManaged(false);

        // Metadata VBox
        VBox metaBox = new VBox(1);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        mMutedLabel.setFont(mFont);
        mMutedLabel.setTextFill(mMutedColor);
        mMutedLabel.setVisible(false);

        String cName = mAudioChannel != null ? mAudioChannel.getChannelName() : " ";
        if ("MONO".equalsIgnoreCase(cName)) {
            cName = " ";
        }
        mChannelName = new Label(cName);
        mChannelName.getStyleClass().add("audio-channel-name");
        mChannelName.setFont(javafx.scene.text.Font.font(mFont.getFamily(), javafx.scene.text.FontWeight.BOLD, mFont.getSize()));
        mChannelName.setTextFill(mLabelColor);
        //Larger, prominent channel name - the header bar is 72px tall and the name is the primary info,
        //so it can be considerably bigger than the previous 20px without clipping the stacked rows.
        mChannelName.setStyle("-fx-font-size: 25px; -fx-font-weight: bold;");

        mIconLabel.setFont(mFont);
        mIconLabel.setTextFill(mValueColor);

        mIdentifierLabel.getStyleClass().add("audio-channel-identifier");
        mIdentifierLabel.setFont(javafx.scene.text.Font.font(mFont.getFamily(), javafx.scene.text.FontWeight.NORMAL, 18));
        mIdentifierLabel.setTextFill(mValueColor);

        mStreamIconsPanel.setBackground(javafx.scene.layout.Background.EMPTY);

        mTwoToneAlertLabel.setFont(javafx.scene.text.Font.font(mFont.getFamily(), javafx.scene.text.FontWeight.BOLD, mFont.getSize()));
        mTwoToneAlertLabel.setTextFill(javafx.scene.paint.Color.RED);
        mTwoToneAlertLabel.setVisible(false);

        //Mockup layout: channel artwork on the left; to its right the channel name on top, the live
        //talkgroup/alias identifier next, and the streaming-service icons in a horizontal row below.
        HBox identifierLine = new HBox(4, mIconLabel, mIdentifierLabel, mTwoToneAlertLabel);
        identifierLine.setAlignment(Pos.CENTER_LEFT);

        mStreamIconsPanel.setAlignment(Pos.CENTER_LEFT);

        metaBox.getChildren().addAll(mChannelName, identifierLine, mStreamIconsPanel);
        HBox.setHgrow(metaBox, Priority.ALWAYS);

        getChildren().addAll(mArtworkContainer, metaBox);
    }

    @Override
    public void receive(final AudioEvent audioEvent)
    {
        switch(audioEvent.getType())
        {
            case AUDIO_STOPPED:
                Platform.runLater(this::resetLabels);
                break;
            case AUDIO_MUTED:
            case AUDIO_UNMUTED:
                break;
            default:
                break;
        }
    }

    /**
     * Resets the from and to labels.
     */
    private void resetLabels()
    {
        //Protect access to mIdentifier and mAliases
        mLock.lock();

        try
        {
            boolean updated = mIdentifier != null;
            mIdentifier = null;
            mAliases = Collections.EMPTY_LIST;

            //Hold the lock through the label update
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

        //Protect access to mIdentifier and mAliases
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

            //Hold the lock through the label update
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

    /**
     * Updates the alias label with text and icon from the alias.
     */
    private void updateLabels()
    {
        String identifier = null;
        String iconName = null;

        //Protect access to mIdentifier and mAliases
        // mLock.lock();

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
                identifier = " ";
            }

            final javafx.scene.image.Image icon = iconName != null ? mIconModel.getIcon(iconName, 26) : null;
            final String identifierText = identifier;
            final boolean isIdle = (mIdentifier == null && mAliases.isEmpty());

            Platform.runLater(() -> {
                if (isIdle) {
                    getStyleClass().remove("audio-channel-panel");
                } else if (!getStyleClass().contains("audio-channel-panel")) {
                    getStyleClass().add("audio-channel-panel");
                }

                mIdentifierLabel.setText(identifierText);
                if (icon != null) { mIconLabel.setGraphic(new javafx.scene.image.ImageView(icon)); } else { mIconLabel.setGraphic(null); }

                mStreamIconsPanel.getChildren().clear();
                if (mAliases != null) {
                    java.util.Set<String> activeStreams = new java.util.HashSet<>();
                    for (Alias alias : mAliases) {
                        for (BroadcastChannel bc : alias.getBroadcastChannels()) {
                            AbstractAudioBroadcaster<?> broadcaster = mBroadcastModel.getBroadcaster(bc.getChannelName());
                            if (broadcaster != null && broadcaster.getBroadcastConfiguration().isEnabled()) {
                                if (!activeStreams.contains(bc.getChannelName())) {
                                    activeStreams.add(bc.getChannelName());
                                    Label streamLabel = new Label("", new javafx.scene.image.ImageView(mIconModel.getIcon(broadcaster.getBroadcastConfiguration().getBroadcastServerType().getIconPath(), 22)));
                                    streamLabel.setTooltip(new javafx.scene.control.Tooltip(broadcaster.getBroadcastConfiguration().getName()));
                                    mStreamIconsPanel.getChildren().add(streamLabel);
                                }
                            }
                        }
                    }
                }
                mStreamIconsPanel.requestLayout();
                mStreamIconsPanel.requestLayout();
                
                // Lookup channel and load artwork
                io.github.dsheirer.controller.channel.Channel chan = null;
                if (mAudioChannel != null && mAudioChannel.getChannelName() != null && !mAudioChannel.getChannelName().isEmpty() && mPlaylistManager != null && mPlaylistManager.getChannelModel() != null) {
                    for (io.github.dsheirer.controller.channel.Channel c : mPlaylistManager.getChannelModel().getChannels()) {
                        if (mAudioChannel.getChannelName().equals(c.getName())) {
                            chan = c;
                            break;
                        }
                    }
                }
                
                if (chan != null && chan.getImagePath() != null && !chan.getImagePath().isEmpty()) {
                    try {
                        java.io.File file = new java.io.File(chan.getImagePath());
                        if (file.exists()) {
                            javafx.scene.image.Image img = new javafx.scene.image.Image(file.toURI().toString(), 46, 46, false, true);
                            mArtworkView.setImage(img);
                            mArtworkContainer.setVisible(true);
                            mArtworkContainer.setManaged(true);
                        } else {
                            mArtworkView.setImage(null);
                            mArtworkContainer.setVisible(false);
                            mArtworkContainer.setManaged(false);
                        }
                    } catch (Exception ex) {
                        mArtworkView.setImage(null);
                        mArtworkContainer.setVisible(false);
                        mArtworkContainer.setManaged(false);
                    }
                } else {
                    mArtworkView.setImage(null);
                    mArtworkContainer.setVisible(false);
                    mArtworkContainer.setManaged(false);
                }
            });
        }
        finally
        {
            // mLock.unlock();
        }
    }

    /**
     * Processes audio metadata to update this panel's display values
     */
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
            ColorSetting colorSetting = (ColorSetting)setting;

            switch(colorSetting.getColorSettingName())
            {
                case CHANNEL_STATE_LABEL_DECODER:
                    Platform.runLater(() -> {
                        if(mIdentifierLabel != null)
                        {
                            mIdentifierLabel.setTextFill(mLabelColor);
                        }
                        if(mIconLabel != null)
                        {
                            mIconLabel.setTextFill(mLabelColor);
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void settingDeleted(Setting setting)
    {
    }

}
