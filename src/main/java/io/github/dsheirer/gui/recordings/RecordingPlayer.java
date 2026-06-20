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

package io.github.dsheirer.gui.recordings;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Mixer;
import java.nio.file.Path;

/**
 * Abstraction over playback of a single recording so the recordings transport bar can play both PCM WAV files
 * (via the Java Sound API) and MP3 files (via JavaFX MediaPlayer).
 *
 * JavaFX MediaPlayer cannot reliably play SDRTrunk's 8 kHz mono PCM WAV recordings - it advances the playback
 * clock but outputs silence - so WAV playback uses javax.sound.sampled (the same stack used for live audio),
 * while MP3 (which JavaFX handles well and which the Java Sound API can't decode without an extra SPI) continues
 * to use MediaPlayer.
 *
 * All Listener callbacks are delivered on the JavaFX Application Thread.
 */
interface RecordingPlayer
{
    /**
     * Loads the recording, firing {@link Listener#onReady(double)} once the duration is known and, when
     * {@code autoPlay} is true, beginning playback.
     */
    void prepare(boolean autoPlay);

    /** Starts (or resumes) playback. */
    void play();

    /** Pauses playback, retaining the current position. */
    void pause();

    /** Stops playback and resets the position to the start. */
    void stop();

    /** Seeks to the given position in seconds. */
    void seekSeconds(double seconds);

    /** @return current playback position in seconds. */
    double getPositionSeconds();

    /** @return total duration in seconds, or 0 if not yet known. */
    double getDurationSeconds();

    /** @return true if playback is currently running. */
    boolean isPlaying();

    /** Releases all playback resources. */
    void dispose();

    void setListener(Listener listener);

    interface Listener
    {
        void onReady(double durationSeconds);
        void onEndOfMedia();
        void onError(String message);
        void onPlayingChanged(boolean playing);
    }

    /**
     * Creates the appropriate player for the recording based on its file extension.
     * @param path of the recording
     * @param mixerInfo of the audio device to play WAV recordings through (the same device used for live audio),
     *                  or null to use the system default.  Ignored for MP3 (JavaFX MediaPlayer uses the default).
     */
    static RecordingPlayer create(Path path, Mixer.Info mixerInfo)
    {
        String name = path.getFileName().toString().toLowerCase();

        if(name.endsWith(".mp3"))
        {
            return new MediaRecordingPlayer(path);
        }

        return new ClipRecordingPlayer(path, mixerInfo);
    }
}

/**
 * Java Sound (javax.sound.sampled) player for PCM WAV recordings.
 */
class ClipRecordingPlayer implements RecordingPlayer
{
    private static final Logger mLog = LoggerFactory.getLogger(ClipRecordingPlayer.class);
    private final Path mPath;
    private final Mixer.Info mMixerInfo;
    private Clip mClip;
    private RecordingPlayer.Listener mListener;

    ClipRecordingPlayer(Path path, Mixer.Info mixerInfo)
    {
        mPath = path;
        mMixerInfo = mixerInfo;
    }

    @Override
    public void setListener(RecordingPlayer.Listener listener)
    {
        mListener = listener;
    }

    @Override
    public void prepare(boolean autoPlay)
    {
        try
        {
            mClip = openClip();
            mClip.addLineListener(event ->
            {
                if(event.getType() == LineEvent.Type.START)
                {
                    notifyPlaying(true);
                }
                else if(event.getType() == LineEvent.Type.STOP)
                {
                    notifyPlaying(false);

                    //A clip that has played all the way through sits at its frame length; treat that as end of media.
                    if(mClip != null && mClip.getFramePosition() >= mClip.getFrameLength())
                    {
                        notifyEnd();
                    }
                }
            });

            final double duration = getDurationSeconds();
            if(mListener != null)
            {
                Platform.runLater(() -> mListener.onReady(duration));
            }

            if(autoPlay)
            {
                play();
            }
        }
        catch(Exception e)
        {
            mLog.error("Unable to open WAV recording for playback: " + mPath, e);
            notifyError(e.getMessage());
        }
    }

    /**
     * Opens a clip on the configured live-audio mixer so recordings play through the same device as live audio.
     * Falls back to the system default mixer if the configured mixer can't provide a clip for this file's format.
     * A fresh audio input stream is used for each attempt because opening a clip consumes the stream.
     */
    private Clip openClip() throws Exception
    {
        if(mMixerInfo != null)
        {
            try(AudioInputStream in = AudioSystem.getAudioInputStream(mPath.toFile()))
            {
                Clip clip = AudioSystem.getClip(mMixerInfo);
                clip.open(in);
                return clip;
            }
            catch(Exception e)
            {
                mLog.warn("Could not open recording on configured audio device [{}], falling back to default mixer: {}",
                    mMixerInfo.getName(), e.getMessage());
            }
        }

        try(AudioInputStream in = AudioSystem.getAudioInputStream(mPath.toFile()))
        {
            Clip clip = AudioSystem.getClip();
            clip.open(in);
            return clip;
        }
    }

    private void notifyPlaying(boolean playing)
    {
        if(mListener != null)
        {
            Platform.runLater(() -> mListener.onPlayingChanged(playing));
        }
    }

    private void notifyEnd()
    {
        if(mListener != null)
        {
            Platform.runLater(() -> mListener.onEndOfMedia());
        }
    }

    private void notifyError(String message)
    {
        if(mListener != null)
        {
            Platform.runLater(() -> mListener.onError(message));
        }
    }

    @Override
    public void play()
    {
        if(mClip == null)
        {
            return;
        }

        //Rewind if sitting at the end so pressing play restarts from the beginning.
        if(mClip.getFramePosition() >= mClip.getFrameLength())
        {
            mClip.setFramePosition(0);
        }

        mClip.start();
    }

    @Override
    public void pause()
    {
        if(mClip != null)
        {
            mClip.stop();
        }
    }

    @Override
    public void stop()
    {
        if(mClip != null)
        {
            mClip.stop();
            mClip.setFramePosition(0);
        }
    }

    @Override
    public void seekSeconds(double seconds)
    {
        if(mClip != null)
        {
            long micros = (long)(seconds * 1_000_000L);
            micros = Math.max(0, Math.min(micros, mClip.getMicrosecondLength()));
            mClip.setMicrosecondPosition(micros);
        }
    }

    @Override
    public double getPositionSeconds()
    {
        return mClip != null ? mClip.getMicrosecondPosition() / 1_000_000.0 : 0;
    }

    @Override
    public double getDurationSeconds()
    {
        return mClip != null ? mClip.getMicrosecondLength() / 1_000_000.0 : 0;
    }

    @Override
    public boolean isPlaying()
    {
        return mClip != null && mClip.isRunning();
    }

    @Override
    public void dispose()
    {
        if(mClip != null)
        {
            mClip.stop();
            mClip.close();
            mClip = null;
        }
    }
}

/**
 * JavaFX MediaPlayer-backed player for MP3 recordings.
 */
class MediaRecordingPlayer implements RecordingPlayer
{
    private static final Logger mLog = LoggerFactory.getLogger(MediaRecordingPlayer.class);
    private final Path mPath;
    private MediaPlayer mMediaPlayer;
    private RecordingPlayer.Listener mListener;

    MediaRecordingPlayer(Path path)
    {
        mPath = path;
    }

    @Override
    public void setListener(RecordingPlayer.Listener listener)
    {
        mListener = listener;
    }

    @Override
    public void prepare(boolean autoPlay)
    {
        try
        {
            Media media = new Media(mPath.toUri().toString());
            mMediaPlayer = new MediaPlayer(media);

            mMediaPlayer.setOnError(() ->
            {
                String msg = mMediaPlayer != null && mMediaPlayer.getError() != null
                    ? mMediaPlayer.getError().getMessage() : "unknown error";
                mLog.error("Media player error for {}: {}", mPath, msg);

                if(mListener != null)
                {
                    mListener.onError(msg);
                }
            });

            mMediaPlayer.setOnReady(() ->
            {
                if(mListener != null)
                {
                    mListener.onReady(getDurationSeconds());
                }

                if(autoPlay)
                {
                    play();
                }
            });

            mMediaPlayer.statusProperty().addListener((obs, o, n) ->
            {
                if(mListener != null)
                {
                    mListener.onPlayingChanged(n == MediaPlayer.Status.PLAYING);
                }
            });

            mMediaPlayer.setOnEndOfMedia(() ->
            {
                if(mListener != null)
                {
                    mListener.onEndOfMedia();
                }
            });
        }
        catch(Exception e)
        {
            mLog.error("Unable to open MP3 recording for playback: " + mPath, e);

            if(mListener != null)
            {
                mListener.onError(e.getMessage());
            }
        }
    }

    @Override
    public void play()
    {
        if(mMediaPlayer != null)
        {
            mMediaPlayer.play();
        }
    }

    @Override
    public void pause()
    {
        if(mMediaPlayer != null)
        {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void stop()
    {
        if(mMediaPlayer != null)
        {
            mMediaPlayer.stop();
            mMediaPlayer.seek(Duration.ZERO);
        }
    }

    @Override
    public void seekSeconds(double seconds)
    {
        if(mMediaPlayer != null)
        {
            mMediaPlayer.seek(Duration.seconds(seconds));
        }
    }

    @Override
    public double getPositionSeconds()
    {
        if(mMediaPlayer != null && mMediaPlayer.getCurrentTime() != null)
        {
            return mMediaPlayer.getCurrentTime().toSeconds();
        }

        return 0;
    }

    @Override
    public double getDurationSeconds()
    {
        if(mMediaPlayer != null)
        {
            Duration total = mMediaPlayer.getTotalDuration();

            if(total != null && !total.isUnknown() && !total.isIndefinite())
            {
                return total.toSeconds();
            }
        }

        return 0;
    }

    @Override
    public boolean isPlaying()
    {
        return mMediaPlayer != null && mMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    @Override
    public void dispose()
    {
        if(mMediaPlayer != null)
        {
            mMediaPlayer.stop();
            mMediaPlayer.dispose();
            mMediaPlayer = null;
        }
    }
}
