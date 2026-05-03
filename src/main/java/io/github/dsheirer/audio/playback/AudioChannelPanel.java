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
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.ColorSetting;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.SettingChangeListener;
import io.github.dsheirer.settings.SettingsManager;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import io.github.dsheirer.dsp.tone.TwoToneDetectedEvent;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * UI to wrap an audio channel and provide display of metadata and playback state information.
 */
public class AudioChannelPanel extends JPanel implements Listener<AudioEvent>, SettingChangeListener
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
    private final TalkgroupFormatPreference mTalkgroupFormatPreference;
    private Identifier mIdentifier;
    private List<Alias> mAliases = Collections.EMPTY_LIST;
    private final Lock mLock = new ReentrantLock();

    private final Font mFont = UIManager.getFont("Label.font") != null ? UIManager.getFont("Label.font").deriveFont(13f) : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private final Color mBackgroundColor;
    private final Color mLabelColor;
    private final Color mMutedColor;
    private final Color mValueColor;
    private final JLabel mMutedLabel = new JLabel("M");
    private JLabel mChannelName = new JLabel(" ");
    private final JLabel mIconLabel = new JLabel(" ");

    private final JLabel mIdentifierLabel = new JLabel("-----");

    private final JLabel mTwoToneAlertLabel = new JLabel("");
    private final JPanel mStreamIconsPanel = new JPanel(new MigLayout("insets 0, gap 2", "", ""));

    private Timer mTwoToneClearTimer;


    /**
     * Constructs an instance
     * @param audioChannel to wrap by this panel
     * @param aliasModel for alias lookup
     * @param iconModel for icon lookup
     * @param settingsManager for monitoring changes to tone insertion
     * @param userPreferences for lookup of tone and other preferences
     */
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

        if(mAudioChannel != null)
        {
            mAudioChannel.addAudioEventListener(this);
            mAudioChannel.setIdentifierCollectionListener(new AudioMetadataProcessor());
        }

        mBackgroundColor = SystemProperties.getInstance().get(PROPERTY_COLOR_BACKGROUND, UIManager.getColor("Panel.background"));
        mLabelColor = SystemProperties.getInstance().get(PROPERTY_COLOR_LABEL, UIManager.getColor("Label.foreground"));
        mMutedColor = SystemProperties.getInstance().get(PROPERTY_COLOR_MUTED, Color.RED);
        mValueColor = SystemProperties.getInstance().get(PROPERTY_COLOR_VALUE, UIManager.getColor("Label.foreground"));

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
            EventQueue.invokeLater(() -> {
                mTwoToneAlertLabel.setText(event.getMessage());
                mTwoToneAlertLabel.setVisible(true);
                mIdentifierLabel.setVisible(false);
                mIconLabel.setVisible(false);

                if (mTwoToneClearTimer != null && mTwoToneClearTimer.isRunning()) {
                    mTwoToneClearTimer.restart();
                } else {
                    mTwoToneClearTimer = new Timer(10000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            mTwoToneAlertLabel.setVisible(false);
                            mIdentifierLabel.setVisible(true);
                            mIconLabel.setVisible(true);
                        }
                    });
                    mTwoToneClearTimer.setRepeats(false);
                    mTwoToneClearTimer.start();
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

    private void init()
    {
        //Register to receive preference updates
        MyEventBus.getGlobalEventBus().register(this);

        setLayout(new MigLayout("align center center, insets 0 2 0 2",
            "[][][align right]5[grow,fill]", ""));
        setBackground(mBackgroundColor);

        mMutedLabel.setFont(mFont);
        mMutedLabel.setForeground(mMutedColor);
        mMutedLabel.setVisible(false);
        add(mMutedLabel);

        mChannelName = new JLabel(mAudioChannel != null ? mAudioChannel.getChannelName() : " ");
        mChannelName.setFont(mFont.deriveFont(Font.BOLD));
        mChannelName.setForeground(mLabelColor);
        add(mChannelName);

        mIconLabel.setFont(mFont);
        mIconLabel.setForeground(mValueColor);
        add(mIconLabel);


        mIdentifierLabel.setFont(mFont.deriveFont(Font.BOLD));
        mIdentifierLabel.setForeground(mValueColor);
        add(mIdentifierLabel, "wmin 10lp");

        mStreamIconsPanel.setOpaque(false);
        add(mStreamIconsPanel, "wmin 10lp, hidemode 3");


        mTwoToneAlertLabel.setFont(mFont.deriveFont(Font.BOLD));
        mTwoToneAlertLabel.setForeground(Color.RED);
        mTwoToneAlertLabel.setVisible(false);
        add(mTwoToneAlertLabel, "hidemode 3");

    }

    @Override
    public void receive(final AudioEvent audioEvent)
    {
        switch(audioEvent.getType())
        {
            case AUDIO_STOPPED:
                EventQueue.invokeLater(this::resetLabels);
                break;
            case AUDIO_MUTED:
            case AUDIO_UNMUTED:
                EventQueue.invokeLater(() -> mMutedLabel.setVisible(mAudioChannel.isMuted()));
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

            final ImageIcon icon = iconName != null ? mIconModel.getIcon(iconName, 18) : null;
            final String identifierText = identifier;

            EventQueue.invokeLater(() -> {
                mIdentifierLabel.setText(identifierText);
                mIconLabel.setIcon(icon);

                mStreamIconsPanel.removeAll();
                if (mAliases != null) {
                    java.util.Set<String> activeStreams = new java.util.HashSet<>();
                    for (Alias alias : mAliases) {
                        for (BroadcastChannel bc : alias.getBroadcastChannels()) {
                            AbstractAudioBroadcaster<?> broadcaster = mBroadcastModel.getBroadcaster(bc.getChannelName());
                            if (broadcaster != null && broadcaster.getBroadcastConfiguration().isEnabled()) {
                                if (!activeStreams.contains(bc.getChannelName())) {
                                    activeStreams.add(bc.getChannelName());
                                    JLabel streamLabel = new JLabel(mIconModel.getIcon(broadcaster.getBroadcastConfiguration().getBroadcastServerType().getIconPath(), 14));
                                    streamLabel.setToolTipText(broadcaster.getBroadcastConfiguration().getName());
                                    mStreamIconsPanel.add(streamLabel);
                                }
                            }
                        }
                    }
                }
                mStreamIconsPanel.revalidate();
                mStreamIconsPanel.repaint();
            });
        }
        finally
        {
            mLock.unlock();
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
                    EventQueue.invokeLater(() -> {
                        if(mIdentifierLabel != null)
                        {
                            mIdentifierLabel.setForeground(mLabelColor);
                        }
                        if(mIconLabel != null)
                        {
                            mIconLabel.setForeground(mLabelColor);
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
