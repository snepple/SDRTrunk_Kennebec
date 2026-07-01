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

package io.github.dsheirer.audio.broadcast.zello;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.health.SystemHealthAlertEvent;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.IRealTimeAudioBroadcaster;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-time audio broadcaster for Zello Work channels via WebSocket.
 *
 * Implements IRealTimeAudioBroadcaster to receive 8kHz mono float audio buffers
 * in real-time. Audio is resampled to 16kHz, Opus-encoded, and streamed via
 * the Zello Channel API WebSocket protocol.
 *
 * Each audio segment maps to one Zello push-to-talk voice message:
 * - startRealTimeStream() -> start_stream command
 * - receiveRealTimeAudio() -> accumulate, resample, Opus encode, send packets
 * - stopRealTimeStream() -> stop_stream command
 */
public class ZelloBroadcaster extends AbstractAudioBroadcaster<ZelloConfiguration>
    implements IRealTimeAudioBroadcaster
{
    private static final Logger mLog = LoggerFactory.getLogger(ZelloBroadcaster.class);

    private static final int ZELLO_SAMPLE_RATE = 16000;
    private static final int ZELLO_CHANNELS = 1;
    private static final int ZELLO_FRAME_SIZE_MS = 60;
    private static final int ZELLO_FRAME_SIZE_SAMPLES = ZELLO_SAMPLE_RATE * ZELLO_FRAME_SIZE_MS / 1000; // 960
    private static final int OPUS_BITRATE = 28000;

    // codec_header: {sample_rate_hz(16LE), frames_per_packet(8), frame_size_ms(8)}
    private static final byte[] CODEC_HEADER = {(byte)0x80, (byte)0x3E, 0x01, 0x3C};
    private static final String CODEC_HEADER_B64 = Base64.getEncoder().encodeToString(CODEC_HEADER);

    private static final long RECONNECT_INTERVAL_MS = 15000;
    private static final long KICKED_BACKOFF_MS = 60000;
    private static final int MAX_KICKED_RETRIES = 5;

    /** Client-side keepalive interval — sends a keepalive command to detect dead connections */
    private static final long KEEPALIVE_INTERVAL_MS = 30000;
    /** Consecutive missed keepalive acks before declaring the connection dead */
    private static final int KEEPALIVE_MISSED_ACK_THRESHOLD = 3;

    /** Default minimum gap (ms) between stop_stream and next start_stream. */
    private static final long DEFAULT_STREAM_GUARD_MS = 500;

    /**
     * Maximum consecutive WebSocket handshake failures before marking as CONFIGURATION_ERROR
     * and stopping reconnect attempts. Prevents misconfigured channels from endlessly leaking
     * memory via failed TLS handshake buffers.
     */
    private static final int MAX_HANDSHAKE_FAILURES = 10;

    /**
     * Shared HttpClient across ALL ZelloBroadcaster instances.
     * Using a single shared instance eliminates the N*thread-pool and N*SSL-context
     * overhead that was causing memory to triple on each reconnect cycle when many
     * Zello channels were configured.
     */
    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(15))
        .build();
    private final Gson mGson = new Gson();
    private final AliasModel mAliasModel;

    private WebSocket mWebSocket;
    private final AtomicBoolean mConnected = new AtomicBoolean(false);
    private final AtomicBoolean mChannelOnline = new AtomicBoolean(false);
    private final AtomicInteger mUsersOnline = new AtomicInteger(0);
    private final AtomicBoolean mKicked = new AtomicBoolean(false);
    private final AtomicBoolean mReconnecting = new AtomicBoolean(false);
    private final AtomicBoolean mStopped = new AtomicBoolean(false);
    private final AtomicInteger mSequence = new AtomicInteger(1);
    private final AtomicInteger mKickedCount = new AtomicInteger(0);
    /** Counts consecutive WebSocket handshake failures (reset on successful connection) */
    private final AtomicInteger mHandshakeFailureCount = new AtomicInteger(0);
    private ScheduledFuture<?> mReconnectFuture;
    private ScheduledFuture<?> mKeepaliveFuture;
    private volatile boolean mKeepaliveAwaitingAck = false;
    private volatile int mKeepaliveMissedAcks = 0;

    /**
     * Session epoch — increments on every WebSocket reconnect. Stream operations
     * capture the epoch at start and abort if the epoch changes (meaning the
     * underlying WebSocket was replaced). This prevents sending start_stream or
     * audio packets on a new connection using stale session state.
     */
    private final AtomicInteger mSessionEpoch = new AtomicInteger(0);

    /**
     * Maps seq numbers to the command that sent them, so error responses can be
     * correlated with the originating command (e.g. "start_stream" or "stop_stream").
     * Entries are removed once the response is processed.
     */
    private final ConcurrentHashMap<Integer, String> mPendingCommands = new ConcurrentHashMap<>();

    private final AtomicBoolean mStreamActive = new AtomicBoolean(false);
    private final AtomicLong mCurrentStreamId = new AtomicLong(-1);
    private volatile long mLastStreamStopTime = 0; // System.currentTimeMillis() when last stream ended
    private volatile int mStreamSessionEpoch = -1; // epoch captured when stream started
    private final LinkedTransferQueue<float[]> mAudioQueue = new LinkedTransferQueue<>();
    private ScheduledFuture<?> mEncoderFuture;
    private ScheduledFuture<?> mRelaxationFuture; // delayed stop for relaxation_time hold-over
    //Opus frames encoded after start_stream but before the server assigns the stream_id are held here and flushed
    //once the id arrives, instead of being dropped - otherwise the first ~100-300ms of every call is clipped on the
    //Zello stream (local playback is a separate path and is unaffected, which matched the reported symptom).
    //Bounded so a start_stream that is never acknowledged cannot grow memory without limit. Accessed only under the
    //instance monitor (processAudioQueue/encodeAndSendFrame/start/stop are all synchronized on this).
    private static final int MAX_PENDING_OPUS_FRAMES = 50; // ~3s at 60ms frames
    private final java.util.ArrayDeque<byte[]> mPendingOpusFrames = new java.util.ArrayDeque<>();
    private volatile long mLastAudioReceivedTime = 0;
    private final AtomicBoolean mInjectingPreDispatch = new AtomicBoolean(false);

    //Two-tone alerts (text and/or pre-dispatch tone) that fired while the channel was not yet online are held here
    //and flushed once it comes online, so a tone-out during a reconnect/kick is not silently dropped (the reliability
    //problem where Zello text/audio alerts only fired sometimes).  Bounded, and each entry has a short TTL so a stale
    //alert never fires long after the event.
    private static final long PENDING_ALERT_TTL_MS = 45_000;
    private static final int MAX_PENDING_ALERTS = 20;
    private record PendingZelloAlert(String text, String audioFile, long expiresAtMs) {}
    private final java.util.concurrent.ConcurrentLinkedQueue<PendingZelloAlert> mPendingAlerts =
            new java.util.concurrent.ConcurrentLinkedQueue<>();

    private RealResampler mResampler;
    private OpusEncoder mOpusEncoder;
    private short[] mResampleBuffer = new short[ZELLO_FRAME_SIZE_SAMPLES];
    private int mResampleBufferPos = 0;
    private byte[] mOpusOutputBuffer = new byte[1275];
    private final AtomicInteger mStreamedCount = new AtomicInteger(0);

    public ZelloBroadcaster(ZelloConfiguration configuration, InputAudioFormat inputAudioFormat,
                            MP3Setting mp3Setting, AliasModel aliasModel)
    {
        super(configuration);
        mAliasModel = aliasModel;
        // Use the shared HttpClient — see SHARED_HTTP_CLIENT field comment.
    }

    /** Returns the configured channel name for log identification */
    private String ch()
    {
        String c = channelName();
        return (c != null && !c.isEmpty()) ? "[" + c + "] " : "";
    }

    /**
     * The configured Zello channel name, trimmed of accidental leading/trailing whitespace.  A trailing space (easy
     * to introduce when typing/pasting the channel) makes the Zello server reject the channel with 3003 "channel is
     * not ready", so the channel never comes online and NOTHING streams.  All channel names sent to the server go
     * through here so such whitespace can never silently break streaming.
     */
    private String channelName()
    {
        ZelloConfiguration config = getBroadcastConfiguration();
        String c = (config != null) ? config.getChannel() : null;
        return (c != null) ? c.trim() : null;
    }

    @Override
    public void start()
    {
        mStopped.set(false);
        setBroadcastState(BroadcastState.CONNECTING);
        try
        {
            initOpusEncoder();
            connectWebSocket();
        }
        catch(Exception e)
        {
            mLog.error("{}Error starting Zello broadcaster", ch(), e);
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            scheduleReconnect();
        }
    }

    @Override
    public void stop()
    {
        mStopped.set(true);
        stopKeepalive();
        if(mRelaxationFuture != null) { mRelaxationFuture.cancel(false); mRelaxationFuture = null; }
        if(mStreamActive.get()) doStopRealTimeStream();
        if(mReconnectFuture != null) { mReconnectFuture.cancel(true); mReconnectFuture = null; }
        mKicked.set(false);
        mKickedCount.set(0);
        mReconnecting.set(false);
        disconnectWebSocket();
        setBroadcastState(BroadcastState.DISCONNECTED);
    }

    @Override
    public void dispose() { stop(); }

    @Override
    public int getAudioQueueSize() { return mAudioQueue.size(); }

    /** Standard recording receive — discarded since we use real-time streaming */
    @Override
    public void receive(AudioRecording audioRecording)
    {
        if(audioRecording != null) audioRecording.removePendingReplay();
    }

    // ========================================================================
    // IRealTimeAudioBroadcaster
    // ========================================================================

    @Override
    public boolean isRealTimeReady()
    {
        return mConnected.get() && mChannelOnline.get() && !mStreamActive.get();
    }

    @Override
    public int getUsersOnline()
    {
        return mUsersOnline.get();
    }

    /**
     * Zello tracks completed streams in its own counter rather than the base-class field, so the
     * streaming status table reads the count through this override.
     */
    @Override
    public int getStreamedAudioCount()
    {
        return mStreamedCount.get();
    }

    @Override
    public synchronized void startRealTimeStream(IdentifierCollection identifiers)
    {
        if(!mConnected.get() || !mChannelOnline.get())
        {
            mLog.warn("{}Cannot start Zello stream - not connected", ch());
            return;
        }
        // If a relaxation timer is pending, the stream is still active — cancel the
        // timer and continue the existing stream instead of stopping and restarting.
        if(mRelaxationFuture != null)
        {
            mRelaxationFuture.cancel(false);
            mRelaxationFuture = null;
            if(mStreamActive.get())
            {
                mLog.debug("{}Relaxation hold-over: continuing existing stream", ch());
                return;
            }
        }
        if(mStreamActive.get())
        {
            doStopRealTimeStream();
        }

        // Enforce minimum gap between streams (like Bridge's stream_guard_timeout_ms).
        // On busy channels, the server may not have fully released the previous stream.
        // A value of 0 disables the guard entirely.
        long guardMs = getBroadcastConfiguration().getStreamGuardMs();
        long elapsed = System.currentTimeMillis() - mLastStreamStopTime;
        if(guardMs > 0 && mLastStreamStopTime > 0 && elapsed < guardMs)
        {
            long waitMs = guardMs - elapsed;
            mLog.debug("{}Stream guard: waiting {}ms before starting new stream", ch(), waitMs);
            try { Thread.sleep(waitMs); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }

        int epoch = mSessionEpoch.get();
        mStreamActive.set(true);
        mStreamSessionEpoch = epoch;
        mCurrentStreamId.set(-1);
        mResampleBufferPos = 0;
        mAudioQueue.clear();
        mPendingOpusFrames.clear();

        mResampler = new RealResampler(8000.0, ZELLO_SAMPLE_RATE, 16000, ZELLO_FRAME_SIZE_SAMPLES);
        mResampler.setListener(resampled -> {
            for(int i = 0; i < resampled.length; i++) {
                mResampleBuffer[i] = (short)(resampled[i] * 32767.0f);
            }
            mResampleBufferPos = resampled.length;
            encodeAndSendFrame();
            mResampleBufferPos = 0;
        });

        sendStartStream();

        if(mEncoderFuture == null || mEncoderFuture.isDone())
        {
            mEncoderFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(
                this::processAudioQueue, 10, 10, TimeUnit.MILLISECONDS);
        }

        mLog.debug("{}Zello stream started", ch());
    }

    @Override
    public void receiveRealTimeAudio(float[] audioBuffer)
    {
        if(mStreamActive.get() && !mInjectingPreDispatch.get())
        {
            mLastAudioReceivedTime = System.currentTimeMillis();
            mAudioQueue.offer(audioBuffer);
        }
    }

    @Override
    public synchronized void stopRealTimeStream()
    {
        if(!mStreamActive.get()) return;

        // Relaxation time: hold the stream open for a configured period after
        // the last audio, allowing back-to-back transmissions to merge into a
        // single Zello voice message (like Bridge's relaxation_time).
        int relaxMs = getBroadcastConfiguration().getRelaxationTimeMs();
        if(relaxMs > 0)
        {
            // Cancel any previous relaxation timer
            if(mRelaxationFuture != null) mRelaxationFuture.cancel(false);
            mRelaxationFuture = ThreadPool.SCHEDULED.schedule(() ->
            {
                synchronized(this) { doStopRealTimeStream(); }
            }, relaxMs, TimeUnit.MILLISECONDS);
            return;
        }

        doStopRealTimeStream();
    }

    /** Internal stop logic — called directly or after relaxation timer expires. */
    private synchronized void doStopRealTimeStream()
    {
        if(!mStreamActive.get()) return;

        mStreamActive.set(false);

        // Cancel relaxation timer if still pending
        if(mRelaxationFuture != null)
        {
            mRelaxationFuture.cancel(false);
            mRelaxationFuture = null;
        }

        // Cancel the encoder future and wait for it to finish to avoid
        // concurrent access to mResampleBuffer and the Opus encoder
        if(mEncoderFuture != null)
        {
            mEncoderFuture.cancel(false);
            try { Thread.sleep(15); } // Allow in-flight execution to complete
            catch(InterruptedException ignored) { Thread.currentThread().interrupt(); }
            mEncoderFuture = null;
        }

        // Now safe to drain remaining audio and flush
        try
        {
            processAudioQueue();
            if(mResampleBufferPos > 0) flushResampleBuffer();
        }
        catch(Exception e)
        {
            mLog.debug("{}Error flushing audio on stream stop: {}", ch(), e.getMessage());
        }

        long streamId = mCurrentStreamId.get();
        if(streamId > 0)
        {
            sendStopStream(streamId);
            mStreamedCount.incrementAndGet();
            mKickedCount.set(0); // Successful stream proves connection is healthy
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_STREAMED_COUNT_CHANGE));
        }

        //Discard any frames still buffered from an unacknowledged handshake so they can't leak into the next stream.
        mPendingOpusFrames.clear();
        mCurrentStreamId.set(-1);
        mResampleBufferPos = 0;
        mAudioQueue.clear();

        // Pause time: additional delay after stop_stream before the broadcaster
        // is ready for a new stream (like Bridge's pause_time).
        int pauseMs = getBroadcastConfiguration().getPauseTimeMs();
        if(pauseMs > 0)
        {
            try { Thread.sleep(pauseMs); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        mLastStreamStopTime = System.currentTimeMillis();
        mLog.debug("{}Zello stream stopped", ch());
    }

    // ========================================================================
    // Audio Processing
    // ========================================================================

    public void sendTextMessage(String text)
    {
        if(mWebSocket == null || !mConnected.get())
        {
            //Not connected (connecting/reconnecting/kicked): hold the alert and flush it when the channel comes
            //online, rather than silently dropping a tone-out alert during connection churn.
            queuePendingAlert(new PendingZelloAlert(text, null, System.currentTimeMillis() + PENDING_ALERT_TTL_MS));
            return;
        }
        try
        {
            JsonObject cmd = new JsonObject();
            cmd.addProperty("command", "send_text_message");
            int seq = mSequence.getAndIncrement();
            cmd.addProperty("seq", seq);
            mPendingCommands.put(seq, "send_text_message");
            cmd.addProperty("channel", channelName());
            cmd.addProperty("text", text);
            mWebSocket.sendText(mGson.toJson(cmd), true);
            mLog.info("{}Sent Zello text message", ch());
        }
        catch(Exception e)
        {
            mLog.error("{}Error sending Zello text message", ch(), e);
        }
    }

    /**
     * Sends a Zello Channel Alert — a high-priority notification that triggers audible beeps and
     * persistent notifications on all channel members' devices.  Uses the Zello Work WebSocket
     * {@code send_alert} command with the same JSON structure as {@link #sendTextMessage(String)}.
     *
     * @param text the alert text to display in the channel alert notification
     */
    public void sendChannelAlert(String text)
    {
        if(mWebSocket == null || !mConnected.get())
        {
            mLog.warn("{}Cannot send Zello channel alert — not connected", ch());
            return;
        }
        try
        {
            JsonObject cmd = new JsonObject();
            cmd.addProperty("command", "send_alert");
            int seq = mSequence.getAndIncrement();
            cmd.addProperty("seq", seq);
            mPendingCommands.put(seq, "send_alert");
            cmd.addProperty("channel", channelName());
            cmd.addProperty("text", text);
            mWebSocket.sendText(mGson.toJson(cmd), true);
            mLog.info("{}Sent Zello channel alert: {}", ch(), text);
        }
        catch(Exception e)
        {
            mLog.error("{}Error sending Zello channel alert", ch(), e);
        }
    }

    public void injectPreDispatchAudio(String filename)
    {
        if (filename == null || filename.isEmpty()) return;

        if(!mConnected.get() || !mChannelOnline.get())
        {
            //Channel not ready: starting a stream would bail, so hold the tone and flush it when online.
            queuePendingAlert(new PendingZelloAlert(null, filename, System.currentTimeMillis() + PENDING_ALERT_TTL_MS));
            return;
        }

        ThreadPool.CACHED.submit(() -> {
            if (!mStreamActive.get()) {
                startRealTimeStream(null);
            }

            mInjectingPreDispatch.set(true);
            try {
                // Built-in alert sounds are in /audio/thinline/; try there first, then /audio/ as fallback.
                java.net.URL resource = ZelloBroadcaster.class.getResource("/audio/thinline/" + filename);
                if (resource == null) {
                    resource = ZelloBroadcaster.class.getResource("/audio/" + filename);
                }
                if (resource == null) {
                    mLog.error("{}Could not find audio resource: /audio/thinline/{} or /audio/{}", ch(), filename, filename);
                    return;
                }

                javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(resource);
                javax.sound.sampled.AudioFormat targetFormat = new javax.sound.sampled.AudioFormat(
                    javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                    8000.0f,
                    16,
                    1,
                    2,
                    8000.0f,
                    false
                );

                javax.sound.sampled.AudioInputStream convertedAis = javax.sound.sampled.AudioSystem.getAudioInputStream(targetFormat, ais);

                byte[] buffer = new byte[160]; // 10ms at 8kHz 16-bit
                int bytesRead;
                while ((bytesRead = convertedAis.read(buffer)) != -1) {
                    float[] floatBuf = new float[bytesRead / 2];
                    for (int i = 0; i < floatBuf.length; i++) {
                        short sample = (short) ((buffer[i * 2 + 1] << 8) | (buffer[i * 2] & 0xFF));
                        floatBuf[i] = sample / 32768.0f;
                    }
                    mAudioQueue.offer(floatBuf);
                }
                convertedAis.close();
                ais.close();

                while(!mAudioQueue.isEmpty() && mStreamActive.get()) {
                    Thread.sleep(10);
                }
                mLog.info("{}Injected pre-dispatch audio: {}", ch(), filename);
            } catch (Exception e) {
                mLog.error("{}Error injecting pre-dispatch audio: {}", ch(), filename, e.getMessage());
            } finally {
                mInjectingPreDispatch.set(false);
            }
        });
    }

    /**
     * Holds a two-tone alert that could not be sent because the channel was not yet online, to be flushed when it
     * comes online.  Bounded (oldest dropped past the cap) so a long outage cannot accumulate alerts without limit.
     */
    private void queuePendingAlert(PendingZelloAlert alert)
    {
        while(mPendingAlerts.size() >= MAX_PENDING_ALERTS)
        {
            mPendingAlerts.poll();
        }
        mPendingAlerts.add(alert);
        mLog.info("{}Zello not ready - queued two-tone alert to send when the channel comes online", ch());
    }

    /**
     * Sends any queued two-tone alerts now that the channel is online, discarding any that have exceeded their TTL.
     * Invoked on the channel-online transition.  Safe because send paths now succeed (connected + online), so they
     * do not re-queue.
     */
    private void flushPendingAlerts()
    {
        long now = System.currentTimeMillis();
        PendingZelloAlert alert;

        while((alert = mPendingAlerts.poll()) != null)
        {
            if(alert.expiresAtMs() < now)
            {
                mLog.info("{}Discarding stale queued two-tone alert (older than {}s)", ch(), PENDING_ALERT_TTL_MS / 1000);
                continue;
            }

            if(alert.text() != null)
            {
                sendTextMessage(alert.text());
            }
            if(alert.audioFile() != null)
            {
                injectPreDispatchAudio(alert.audioFile());
            }
        }
    }

    private synchronized void processAudioQueue()
    {
        try
        {
            float[] buffer;
            while((buffer = mAudioQueue.poll()) != null)
            {
                processAudioBuffer(buffer);
            }
        }
        catch(Exception | AssertionError e)
        {
            mLog.debug("{}Error processing audio queue (non-fatal): {}", ch(), describeThrowable(e));
        }
    }

    /** Resample float 8kHz -> short 16kHz using RealResampler, encode when frame full */
    private void processAudioBuffer(float[] audio8k)
    {
        if (mResampler != null)
        {
            mResampler.resample(audio8k);
        }
    }

    private void encodeAndSendFrame()
    {
        if(mOpusEncoder == null) return;

        // Reject sends if the WebSocket session has changed since this stream started
        if(mStreamSessionEpoch != mSessionEpoch.get())
        {
            mLog.debug("{}Dropping audio frame — session epoch changed (stream={}, current={})",
                ch(), mStreamSessionEpoch, mSessionEpoch.get());
            mStreamActive.set(false);
            return;
        }

        try
        {
            int encoded = mOpusEncoder.encode(mResampleBuffer, 0, ZELLO_FRAME_SIZE_SAMPLES,
                mOpusOutputBuffer, 0, mOpusOutputBuffer.length);

            if(encoded > 0)
            {
                byte[] opusFrame = new byte[encoded];
                System.arraycopy(mOpusOutputBuffer, 0, opusFrame, 0, encoded);

                long streamId = mCurrentStreamId.get();

                if(streamId > 0)
                {
                    //Flush any frames captured during the start_stream handshake first (in order) so the start of
                    //the call is preserved, then send the current frame.
                    if(!mPendingOpusFrames.isEmpty())
                    {
                        byte[] pending;
                        while((pending = mPendingOpusFrames.poll()) != null)
                        {
                            sendAudioPacket(streamId, pending);
                        }
                    }
                    sendAudioPacket(streamId, opusFrame);
                }
                else if(streamId == -1)
                {
                    //start_stream handshake still in flight (stream_id not yet assigned). Buffer the encoded frames
                    //rather than dropping them so the beginning of the call is not clipped on the Zello stream.
                    if(mPendingOpusFrames.size() < MAX_PENDING_OPUS_FRAMES)
                    {
                        mPendingOpusFrames.add(opusFrame);
                    }
                }
                //streamId == -2 (start_stream failed) or 0: drop the frame.
            }
        }
        catch(Exception | AssertionError e)
        {
            mLog.debug("{}Opus encoding error (non-fatal): {}", ch(), describeThrowable(e));

            // Re-initialize the encoder to prevent cascading failures on subsequent frames
            try
            {
                initOpusEncoder();
                mLog.debug("{}Opus encoder re-initialized after error", ch());
            }
            catch(Exception reinitEx)
            {
                mLog.warn("{}Failed to re-initialize Opus encoder: {}", ch(), reinitEx.getMessage());
                mOpusEncoder = null;
            }
        }
    }

    private void flushResampleBuffer()
    {
        try
        {
            if (mResampler != null)
            {
                mResampler.resample(new float[0], true);
            }
        }
        catch(Exception | AssertionError e)
        {
            mLog.debug("{}Opus flush error (non-fatal): {}", ch(), describeThrowable(e));
        }
        finally
        {
            mResampleBufferPos = 0;
        }
    }

    /**
     * Describes a throwable for logging.  Many encoder failures are NullPointerExceptions or AssertionErrors with no
     * message, which logged as a bare "null"; fall back to the exception type so the log identifies what actually
     * failed.
     */
    private static String describeThrowable(Throwable t)
    {
        if(t == null)
        {
            return "unknown";
        }

        String message = t.getMessage();
        return (message != null && !message.isEmpty()) ? (t.getClass().getSimpleName() + ": " + message)
                : t.getClass().getSimpleName();
    }

    private void initOpusEncoder() throws Exception
    {
        mOpusEncoder = new OpusEncoder(ZELLO_SAMPLE_RATE, ZELLO_CHANNELS, OpusApplication.OPUS_APPLICATION_VOIP);
        mOpusEncoder.setBitrate(OPUS_BITRATE);
        mOpusEncoder.setSignalType(OpusSignal.OPUS_SIGNAL_VOICE);
        mOpusEncoder.setComplexity(8);
        mLog.debug("{}Opus encoder initialized: {}Hz, {}ch, {}kbps, {}ms frames",
            ch(), ZELLO_SAMPLE_RATE, ZELLO_CHANNELS, OPUS_BITRATE / 1000, ZELLO_FRAME_SIZE_MS);
    }

    // ========================================================================
    // WebSocket
    // ========================================================================

    private void connectWebSocket()
    {
        if(!mReconnecting.compareAndSet(false, true))
        {
            return; // Another reconnect is already in progress
        }

        // Don't reconnect if we've been stopped
        if(mStopped.get())
        {
            mReconnecting.set(false);
            return;
        }

        // Clean up any existing connection — send a proper close frame so the
        // server releases session state (abort() skips the close handshake which
        // can leave a stale session on Zello's side, causing auth failures on the
        // next logon attempt with the same credentials).
        if(mWebSocket != null)
        {
            try { mWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "reconnecting"); }
            catch(Exception e) { /* ignore — connection may already be dead */ }
            mWebSocket = null;
        }
        mConnected.set(false);
        mChannelOnline.set(false);
        if (mUsersOnline.getAndSet(0) != 0) {
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_USERS_ONLINE_CHANGE));
        }
        mPendingCommands.clear();

        String wsUrl = getBroadcastConfiguration().getWebSocketUrl();
        if(wsUrl == null)
        {
            mLog.error("Zello WebSocket URL is null");
            setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
            mReconnecting.set(false);
            return;
        }

        mLog.debug("{}Connecting to Zello Work: {}", ch(), wsUrl);
        try
        {
            SHARED_HTTP_CLIENT.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new ZelloWebSocketListener())
                .thenAccept(ws -> {
                    mWebSocket = ws;
                    mSessionEpoch.incrementAndGet();
                    mReconnecting.set(false);
                    mHandshakeFailureCount.set(0); // reset on successful connection
                    setLastErrorDetail(null);
                    sendLogon();
                })
                .exceptionally(ex -> {
                    int failures = mHandshakeFailureCount.incrementAndGet();
                    mLog.error("{}WebSocket connection failed (attempt {}): {}", ch(), failures, ex.getMessage());
                    setLastErrorDetail("WebSocket handshake failed");
                    setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                    mReconnecting.set(false);
                    if(failures >= MAX_HANDSHAKE_FAILURES)
                    {
                        mLog.error("{}WebSocket handshake failed {} consecutive times - stopping reconnect. " +
                            "Check Zello workspace URL and credentials.", ch(), failures);
                        setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
                        // Don't schedule another reconnect — this channel is misconfigured
                    }
                    else
                    {
                        // Exponential backoff: 15s, 30s, 60s, 120s, 240s, then capped at 300s
                        long backoffMs = Math.min(RECONNECT_INTERVAL_MS * (1L << Math.min(failures - 1, 4)), 300_000L);
                        scheduleReconnectWithDelay(backoffMs);
                    }
                    return null;
                });
        }
        catch(Exception e)
        {
            mLog.error("Error creating WebSocket connection", e);
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            mReconnecting.set(false);
            scheduleReconnect();
        }
    }

    private void disconnectWebSocket()
    {
        mConnected.set(false);
        mChannelOnline.set(false);
        if (mUsersOnline.getAndSet(0) != 0) {
            broadcast(new BroadcastEvent(this, BroadcastEvent.Event.BROADCASTER_USERS_ONLINE_CHANGE));
        }
        if(mWebSocket != null)
        {
            try { mWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Shutting down"); }
            catch(Exception e) { /* ignore */ }
            mWebSocket = null;
        }
    }

    private void scheduleReconnect()
    {
        // Don't reconnect if we've been stopped
        if(mStopped.get())
        {
            return;
        }

        if(mKicked.get())
        {
            int kickCount = mKickedCount.get();
            if(kickCount >= MAX_KICKED_RETRIES)
            {
                mLog.error("{}Zello kicked {} times - stopping reconnect attempts. Check channel permissions.", ch(), kickCount);
                setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
                return;
            }
            long backoff = KICKED_BACKOFF_MS * (1L << Math.min(kickCount, 4)); // exponential: 60s, 120s, 240s...
            mLog.warn("{}Zello 'kicked' by the server (attempt {}/{}) - backing off {}s. This almost always means the " +
                    "SAME Zello account is logged in somewhere else (another device/app, or two streams configured with " +
                    "the same account). While kicked, calls to this channel are NOT streamed. Fix: use a unique Zello " +
                    "account per stream.", ch(), kickCount + 1, MAX_KICKED_RETRIES, backoff / 1000);

            //Surface a user-visible health alert (rate-limited by SystemHealthMonitor) so the cause of missed/
            //non-streaming calls is obvious rather than buried in the log.
            ZelloConfiguration cfg = getBroadcastConfiguration();
            String streamName = (cfg != null && cfg.getChannel() != null) ? cfg.getChannel() : "(unknown)";
            MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                    SystemHealthAlertEvent.AlertType.INTEGRATION,
                    "Zello Account Kicked",
                    "Zello stream [" + streamName + "] was kicked - the same Zello account is most likely logged in " +
                    "elsewhere. Calls are not being streamed to this channel until it reconnects. Use a unique Zello " +
                    "account per stream to resolve."));

            scheduleReconnectWithDelay(backoff);
        }
        else
        {
            scheduleReconnectWithDelay(RECONNECT_INTERVAL_MS);
        }
    }

    private void scheduleReconnectWithDelay(long delayMs)
    {
        // Cancel any existing reconnect to prevent overlapping timers
        if(mReconnectFuture != null && !mReconnectFuture.isDone())
        {
            return; // A reconnect is already pending
        }

        mReconnectFuture = ThreadPool.SCHEDULED.schedule(() -> {
            if(!mConnected.get() && !mStopped.get())
            {
                mLog.debug("{}Zello reconnecting...", ch());
                connectWebSocket();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    // ========================================================================
    // Client-Side Keepalive
    // ========================================================================

    /**
     * Starts the client-side keepalive timer. Sends a keepalive command every
     * {@link #KEEPALIVE_INTERVAL_MS} to proactively detect dead connections
     * (e.g. silent NAT timeout, network change without TCP RST). If the server
     * fails to ack {@link #KEEPALIVE_MISSED_ACK_THRESHOLD} consecutive keepalives,
     * the connection is declared dead and reconnection is triggered.
     *
     * This mirrors the approach used by the official Zello JS SDK.
     */
    private void startKeepalive()
    {
        stopKeepalive();
        mKeepaliveAwaitingAck = false;
        mKeepaliveMissedAcks = 0;
        mKeepaliveFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(
            this::keepaliveTick, KEEPALIVE_INTERVAL_MS, KEEPALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopKeepalive()
    {
        if(mKeepaliveFuture != null)
        {
            mKeepaliveFuture.cancel(false);
            mKeepaliveFuture = null;
        }
    }

    private void keepaliveTick()
    {
        if(mWebSocket == null || !mConnected.get())
        {
            return;
        }

        if(mKeepaliveAwaitingAck)
        {
            mKeepaliveMissedAcks++;
            mLog.debug("{}Keepalive ack missed ({}/{})", ch(), mKeepaliveMissedAcks, KEEPALIVE_MISSED_ACK_THRESHOLD);
        }

        if(mKeepaliveMissedAcks >= KEEPALIVE_MISSED_ACK_THRESHOLD)
        {
            mLog.warn("{}Keepalive timeout — {} consecutive missed acks, reconnecting", ch(), mKeepaliveMissedAcks);
            stopKeepalive();
            mConnected.set(false);
            mChannelOnline.set(false);
            if (mUsersOnline.getAndSet(0) != 0) {
                broadcast(new BroadcastEvent(ZelloBroadcaster.this, BroadcastEvent.Event.BROADCASTER_USERS_ONLINE_CHANGE));
            }
            mStreamActive.set(false);
            mCurrentStreamId.set(-1);
            // Abort the dead WebSocket so connectWebSocket() starts fresh
            if(mWebSocket != null)
            {
                try { mWebSocket.abort(); } catch(Exception e) { /* ignore */ }
                mWebSocket = null;
            }
            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            setLastErrorDetail("Keepalive timeout — connection dead");
            scheduleReconnect();
            return;
        }

        // Send keepalive command
        try
        {
            mKeepaliveAwaitingAck = true;
            JsonObject cmd = new JsonObject();
            cmd.addProperty("command", "keepalive");
            int seq = mSequence.getAndIncrement();
            cmd.addProperty("seq", seq);
            mPendingCommands.put(seq, "keepalive");
            mWebSocket.sendText(mGson.toJson(cmd), true);
        }
        catch(Exception e)
        {
            mLog.warn("{}Keepalive send failed: {}", ch(), e.getMessage());
            mKeepaliveMissedAcks++;
        }
    }

    /**
     * Called when a keepalive ack is received from the server. Resets the
     * missed-ack counter so the connection is considered healthy.
     */
    private void handleKeepaliveAck()
    {
        mKeepaliveAwaitingAck = false;
        mKeepaliveMissedAcks = 0;
    }

    // ========================================================================
    // Zello Protocol
    // ========================================================================

    /**
     * Validates a Zello Work configuration by opening a short-lived WebSocket, performing a logon with the
     * configured network/username/password, and waiting for the channel-status response.  This exercises the exact
     * same streaming-API logon path used during normal operation, so it catches the common configuration mistakes:
     * a wrong network name or credentials (logon rejected) and — most usefully — a mistyped channel name or stray
     * whitespace (logon succeeds but the channel never reports "online").  No admin API key is required.
     *
     * Intended to be called off the UI thread (it blocks up to ~12 seconds).  The probe connection is always closed
     * before returning and never streams audio, so it has no effect on any live broadcaster.
     *
     * @param config to test
     * @return a human-readable result; a successful result starts with "OK".
     */
    public static String testConnection(ZelloConfiguration config)
    {
        if(config == null)
        {
            return "No configuration to test.";
        }

        final String channel = config.getChannel() != null ? config.getChannel().trim() : "";
        String network = config.getNetworkName() != null ? config.getNetworkName().trim() : "";
        String username = config.getUsername() != null ? config.getUsername().trim() : "";

        if(network.isEmpty())
        {
            return "Zello Work Network is required.";
        }
        if(username.isEmpty())
        {
            return "Username is required.";
        }
        if(channel.isEmpty())
        {
            return "Channel is required.";
        }

        String wsUrl = config.getWebSocketUrl();
        if(wsUrl == null)
        {
            return "Could not build the Zello Work URL from the network name [" + network + "].";
        }

        final Gson gson = new Gson();
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<String> result = new java.util.concurrent.atomic.AtomicReference<>(null);
        final AtomicBoolean loggedOn = new AtomicBoolean(false);

        WebSocket.Listener listener = new WebSocket.Listener()
        {
            private final StringBuilder mBuffer = new StringBuilder();

            @Override
            public void onOpen(WebSocket webSocket)
            {
                JsonObject logon = new JsonObject();
                logon.addProperty("command", "logon");
                logon.addProperty("seq", 1);
                com.google.gson.JsonArray channels = new com.google.gson.JsonArray();
                channels.add(channel);
                logon.add("channels", channels);
                logon.addProperty("username", config.getUsername());
                logon.addProperty("password", config.getPassword());
                logon.addProperty("platform_name", "Gateway");
                webSocket.sendText(gson.toJson(logon), true);
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last)
            {
                mBuffer.append(data);
                if(last)
                {
                    String message = mBuffer.toString();
                    mBuffer.setLength(0);
                    try
                    {
                        JsonObject json = JsonParser.parseString(message).getAsJsonObject();

                        if(json.has("error") && !json.has("command"))
                        {
                            finish("Login failed: " + json.get("error").getAsString() +
                                ". Verify the network, username and password.");
                        }
                        else if(json.has("command") && "on_channel_status".equals(json.get("command").getAsString()))
                        {
                            String status = json.has("status") ? json.get("status").getAsString() : "";
                            if("online".equals(status))
                            {
                                int users = json.has("users_online") ? json.get("users_online").getAsInt() : -1;
                                finish("OK — logged in and channel '" + channel + "' is online" +
                                    (users >= 0 ? " (" + users + " user" + (users == 1 ? "" : "s") + " online)." : "."));
                            }
                            else
                            {
                                finish("Logged in, but channel '" + channel + "' is " +
                                    (status.isEmpty() ? "not online" : status) +
                                    ". Check the channel name for exact spelling and stray spaces.");
                            }
                        }
                        else if(json.has("refresh_token") ||
                            (json.has("success") && json.get("success").getAsBoolean() && !json.has("stream_id")))
                        {
                            //Logon accepted; keep waiting for the channel-status message before declaring success.
                            loggedOn.set(true);
                        }
                    }
                    catch(Exception e)
                    {
                        //Ignore unparseable frames and keep waiting.
                    }
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error)
            {
                finish("Connection error: " + (error != null ? error.getMessage() : "unknown"));
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason)
            {
                finish(loggedOn.get()
                    ? "Logged in, but the channel never reported status — verify the channel name."
                    : "Connection closed before login completed (code " + statusCode + ").");
                return null;
            }

            private void finish(String r)
            {
                if(result.compareAndSet(null, r))
                {
                    done.countDown();
                }
            }
        };

        WebSocket webSocket = null;
        try
        {
            webSocket = SHARED_HTTP_CLIENT.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), listener)
                .join();

            if(!done.await(12, TimeUnit.SECONDS))
            {
                result.compareAndSet(null, loggedOn.get()
                    ? "Logged in, but channel '" + channel + "' did not report status within 12s — verify the channel name."
                    : "Timed out waiting for a Zello response — check the network name and your connection.");
            }
        }
        catch(Exception e)
        {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            result.compareAndSet(null, "Could not connect: " + cause.getMessage() +
                ". Verify the Zello Work network name.");
        }
        finally
        {
            if(webSocket != null)
            {
                try { webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "test complete"); } catch(Exception e) { /* ignore */ }
                try { webSocket.abort(); } catch(Exception e) { /* ignore */ }
            }
        }

        return result.get();
    }

    private void sendLogon()
    {
        if(mWebSocket == null) return;
        ZelloConfiguration config = getBroadcastConfiguration();
        JsonObject logon = new JsonObject();
        logon.addProperty("command", "logon");
        int seq = mSequence.getAndIncrement();
        logon.addProperty("seq", seq);
        mPendingCommands.put(seq, "logon");
        com.google.gson.JsonArray channels = new com.google.gson.JsonArray();
        channels.add(channelName());
        logon.add("channels", channels);
        logon.addProperty("username", config.getUsername());
        logon.addProperty("password", config.getPassword());
        logon.addProperty("platform_name", "Gateway");
        mWebSocket.sendText(mGson.toJson(logon), true);
    }

    private void sendStartStream()
    {
        if(mWebSocket == null) return;
        // Don't send start_stream if the session has already changed
        if(mStreamSessionEpoch != mSessionEpoch.get())
        {
            mLog.warn("{}Aborting start_stream — session epoch changed during setup", ch());
            mStreamActive.set(false);
            return;
        }
        ZelloConfiguration config = getBroadcastConfiguration();
        JsonObject cmd = new JsonObject();
        cmd.addProperty("command", "start_stream");
        int seq = mSequence.getAndIncrement();
        cmd.addProperty("seq", seq);
        mPendingCommands.put(seq, "start_stream");
        cmd.addProperty("channel", channelName());
        cmd.addProperty("type", "audio");
        cmd.addProperty("codec", "opus");
        cmd.addProperty("codec_header", CODEC_HEADER_B64);
        cmd.addProperty("packet_duration", ZELLO_FRAME_SIZE_MS);
        mWebSocket.sendText(mGson.toJson(cmd), true);
    }

    private void sendStopStream(long streamId)
    {
        if(mWebSocket == null) return;
        ZelloConfiguration config = getBroadcastConfiguration();
        JsonObject cmd = new JsonObject();
        cmd.addProperty("command", "stop_stream");
        int seq = mSequence.getAndIncrement();
        cmd.addProperty("seq", seq);
        mPendingCommands.put(seq, "stop_stream(id=" + streamId + ")");
        cmd.addProperty("stream_id", streamId);
        cmd.addProperty("channel", channelName());
        mWebSocket.sendText(mGson.toJson(cmd), true);
    }

    private void sendAudioPacket(long streamId, byte[] opusData)
    {
        if(mWebSocket == null) return;
        ByteBuffer packet = ByteBuffer.allocate(1 + 4 + 4 + opusData.length);
        packet.order(ByteOrder.BIG_ENDIAN);
        packet.put((byte)0x01);
        packet.putInt((int)streamId);
        packet.putInt(0);
        packet.put(opusData);
        packet.flip();
        mWebSocket.sendBinary(packet, true);
    }

    /**
     * Sets stream-level error detail only when the connection is not healthy.
     * While CONNECTED, stream errors (channel busy, on_stream_stop, invalid stream id) are
     * transient — clear any stale error rather than showing a misleading one in the table.
     */
    private void updateStreamErrorDetail(String detail)
    {
        if(getBroadcastState() == BroadcastState.CONNECTED)
        {
            setLastErrorDetail(null);
        }
        else if(detail != null)
        {
            setLastErrorDetail(detail);
        }
    }

    /**
     * Maps Zello Channel API error strings to Zello Bridge error codes (3001-3009)
     * for consistent diagnostics. See Zello Bridge documentation.
     */
    private static int mapBridgeErrorCode(String error)
    {
        if(error == null) return 3008;
        switch(error)
        {
            case "not connected":           return 3001;
            case "invalid credentials":     return 3002;
            case "not authorized":          return 3002;
            case "channel is not ready":    return 3003;
            case "failed to start stream":  return 3006;
            case "invalid stream id":       return 3007;
            case "failed to stop stream":   return 3007;
            case "kicked":                  return 3009;
            default:                        return 3008; // generic "error received"
        }
    }

    // ========================================================================
    // WebSocket Listener
    // ========================================================================

    private class ZelloWebSocketListener implements WebSocket.Listener
    {
        private StringBuilder mTextBuffer = new StringBuilder();

        @Override public void onOpen(WebSocket ws) { mLog.debug("{}WebSocket opened", ch()); ws.request(1); }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last)
        {
            mTextBuffer.append(data);
            if(last) { handleTextMessage(mTextBuffer.toString()); mTextBuffer.setLength(0); }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last)
        {
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer msg)
        {
            ws.sendPong(msg);
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int code, String reason)
        {
            mLog.info("{}Zello disconnected (code={} {})", ch(), code, reason);
            stopKeepalive();
            mConnected.set(false);
            mChannelOnline.set(false);
            if (mUsersOnline.getAndSet(0) != 0) {
                broadcast(new BroadcastEvent(ZelloBroadcaster.this, BroadcastEvent.Event.BROADCASTER_USERS_ONLINE_CHANGE));
            }
            // Always reset stream state on disconnect — prevents stale stream IDs
            // from surviving into the next session regardless of mStreamActive state
            mStreamActive.set(false);
            mCurrentStreamId.set(-1);

            // If kicked error already handled the reconnect, don't double-schedule
            if(mKicked.get())
            {
                return null;
            }

            // If an auth error already set CONFIGURATION_ERROR, don't override it
            // and don't schedule a reconnect — the credentials won't change by retrying
            if(getBroadcastState() == BroadcastState.CONFIGURATION_ERROR)
            {
                return null;
            }

            setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error)
        {
            mLog.error("{}Zello WebSocket error: {}", ch(), error.getMessage());
            stopKeepalive();
            mConnected.set(false);
            mChannelOnline.set(false);
            if (mUsersOnline.getAndSet(0) != 0) {
                broadcast(new BroadcastEvent(ZelloBroadcaster.this, BroadcastEvent.Event.BROADCASTER_USERS_ONLINE_CHANGE));
            }
            // Reset stream state on error — same as onClose
            mStreamActive.set(false);
            mCurrentStreamId.set(-1);

            // Don't override CONFIGURATION_ERROR or double-schedule after kicked
            if(!mKicked.get() && getBroadcastState() != BroadcastState.CONFIGURATION_ERROR)
            {
                setBroadcastState(BroadcastState.TEMPORARY_BROADCAST_ERROR);
                scheduleReconnect();
            }
        }

        private void handleTextMessage(String message)
        {
            try
            {
                JsonObject json = JsonParser.parseString(message).getAsJsonObject();

                if(json.has("refresh_token") ||
                    (json.has("success") && json.get("success").getAsBoolean() && !json.has("stream_id")))
                {
                    if(!mConnected.get())
                    {
                        mLog.debug("{}Zello logon accepted", ch());
                        mConnected.set(true);
                        // Reset kicked flag so next kick can be detected, but keep the
                        // kickedCount so exponential backoff continues to escalate.
                        // Count only resets when a stream succeeds or on manual stop/restart.
                        mKicked.set(false);
                    }
                    // else: refresh_token while already connected — ignore silently
                }
                else if(json.has("error") && !json.has("command"))
                {
                    String errorMsg = json.get("error").getAsString();
                    int seq = json.has("seq") ? json.get("seq").getAsInt() : -1;
                    String originCmd = seq > 0 ? mPendingCommands.remove(seq) : null;
                    int bridgeCode = mapBridgeErrorCode(errorMsg);

                    // Stream-related errors (3003/3006/3007): the Zello server expired or
                    // closed the stream, another user interrupted our transmission, the
                    // server refused a brand-new stream attempt, or the channel was not
                    // ready at the moment we tried to transmit. These are all transient —
                    // clean up, stay CONNECTED, and allow the next transmission.
                    //
                    // "channel is not ready" (3003) in particular is NOT an auth/config
                    // error: the WebSocket session and logon are valid, the target Zello
                    // channel just wasn't in the ready/online state when this call's audio
                    // arrived (e.g. no devices subscribed to that channel at that instant).
                    // Treating it as a CONFIGURATION_ERROR previously dropped the whole
                    // broadcaster on every such call, forcing a manual reconnect that then
                    // failed again the same way. Mark the channel offline so streaming
                    // resumes automatically once Zello sends the next on_channel_status
                    // "online" event for it.
                    if("channel is not ready".equals(errorMsg))
                    {
                        mChannelOnline.set(false);
                    }

                    if("invalid stream id".equals(errorMsg)
                        || "failed to stop stream".equals(errorMsg)
                        || "failed to start stream".equals(errorMsg)
                        || "failed to start sending message".equals(errorMsg)
                        || "failed to stop sending message".equals(errorMsg)
                        || "channel is not ready".equals(errorMsg))
                    {
                        mLog.debug("{}Zello [{}]: error=\"{}\" seq={} command={}",
                            ch(), bridgeCode, errorMsg, seq, originCmd != null ? originCmd : "unknown");
                        updateStreamErrorDetail("[" + bridgeCode + "] " + errorMsg +
                            (originCmd != null ? " — " + originCmd : ""));
                        mStreamActive.set(false);
                        mCurrentStreamId.set(-1);
                        mLastStreamStopTime = System.currentTimeMillis();
                        // Stay in CONNECTED state — the WebSocket session is still alive
                        return;
                    }

                    // Actual logon/authentication error (e.g. "invalid credentials")
                    mLog.error("{}Zello [{}]: error=\"{}\" seq={} command={}",
                        ch(), bridgeCode, errorMsg, seq, originCmd != null ? originCmd : "unknown");
                    setLastErrorDetail("[" + bridgeCode + "] " + errorMsg);
                    setBroadcastState(BroadcastState.CONFIGURATION_ERROR);
                    return;
                }

                if(json.has("command"))
                {
                    String command = json.get("command").getAsString();
                    if("on_channel_status".equals(command))
                    {
                        String status = json.has("status") ? json.get("status").getAsString() : "";
                        if("online".equals(status))
                        {
                            if (json.has("users_online")) {
                                int users = json.get("users_online").getAsInt();
                                if (mUsersOnline.getAndSet(users) != users) {
                                    broadcast(new BroadcastEvent(ZelloBroadcaster.this, BroadcastEvent.Event.BROADCASTER_USERS_ONLINE_CHANGE));
                                }
                            }
                            // getAndSet(true) returns the old value; only log/set state on first transition
                            if(!mChannelOnline.getAndSet(true))
                            {
                                setBroadcastState(BroadcastState.CONNECTED);
                                startKeepalive();
                                mLog.info("{}Zello connected", ch());
                                //Flush any two-tone alerts that fired while the channel was offline/reconnecting.
                                flushPendingAlerts();
                            }
                        }
                        else
                        {
                            mChannelOnline.set(false);
                            if (mUsersOnline.getAndSet(0) != 0) {
                                broadcast(new BroadcastEvent(ZelloBroadcaster.this, BroadcastEvent.Event.BROADCASTER_USERS_ONLINE_CHANGE));
                            }
                        }
                    }
                    else if("on_stream_stop".equals(command))
                    {
                        // Server-initiated stream termination. This happens when the server
                        // closes our outgoing stream (e.g. server-side timeout, audio gap,
                        // or channel policy). Proactively clean up so the subsequent
                        // stopRealTimeStream() call doesn't send a stale stop_stream.
                        long stoppedId = json.has("stream_id") ? json.get("stream_id").getAsLong() : -1;
                        if(stoppedId > 0 && stoppedId == mCurrentStreamId.get())
                        {
                            mLog.info("{}Zello server stopped our stream (id={})", ch(), stoppedId);
                            updateStreamErrorDetail("[3007] server stopped stream (id=" + stoppedId + ")");
                            mStreamActive.set(false);
                            mCurrentStreamId.set(-1);
                            mLastStreamStopTime = System.currentTimeMillis();
                        }
                        else
                        {
                            mLog.debug("{}Zello on_stream_stop for stream_id={} (not ours: {})",
                                ch(), stoppedId, mCurrentStreamId.get());
                        }
                    }
                    else if("on_error".equals(command))
                    {
                        String error = json.has("error") ? json.get("error").getAsString() : "";
                        mLog.error("{}Zello [{}]: {}", ch(), mapBridgeErrorCode(error), message);

                        if("kicked".equals(error))
                        {
                            setLastErrorDetail("[3009] kicked");
                            mKicked.set(true);
                            mKickedCount.incrementAndGet();
                            mConnected.set(false);
                            mChannelOnline.set(false);
                            if (mUsersOnline.getAndSet(0) != 0) {
                                broadcast(new BroadcastEvent(ZelloBroadcaster.this, BroadcastEvent.Event.BROADCASTER_USERS_ONLINE_CHANGE));
                            }
                            // Close the WebSocket ourselves to prevent onClose from also scheduling
                            if(mWebSocket != null)
                            {
                                try { mWebSocket.abort(); } catch(Exception e) { /* ignore */ }
                                mWebSocket = null;
                            }
                            scheduleReconnect();
                            return; // Don't process further messages on this connection
                        }
                    }
                }

                // Clean up pending command tracking on any successful response with seq
                if(json.has("seq") && json.has("success") && json.get("success").getAsBoolean())
                {
                    int ackSeq = json.get("seq").getAsInt();
                    String ackCmd = mPendingCommands.remove(ackSeq);
                    // Handle keepalive ack — reset missed-ack counter
                    if("keepalive".equals(ackCmd))
                    {
                        handleKeepaliveAck();
                    }
                }

                if(json.has("stream_id") && json.has("success"))
                {
                    if(json.get("success").getAsBoolean())
                    {
                        long streamId = json.get("stream_id").getAsLong();
                        mCurrentStreamId.set(streamId);
                        setLastErrorDetail(null);
                        mLog.debug("{}Zello stream_id={}", ch(), streamId);
                    }
                    else
                    {
                        int seq = json.has("seq") ? json.get("seq").getAsInt() : -1;
                        String originCmd = seq > 0 ? mPendingCommands.remove(seq) : null;
                        String error = json.has("error") ? json.get("error").getAsString() : "unknown";
                        mLog.error("{}Zello start_stream failed: error=\"{}\" seq={} command={}",
                            ch(), error, seq, originCmd != null ? originCmd : "start_stream");
                        updateStreamErrorDetail("[3006] " + error);
                        mCurrentStreamId.set(-2);
                        mStreamActive.set(false);
                        // Apply the stream guard after a failed start so "channel busy" and similar
                        // transient rejections don't trigger back-to-back start_stream attempts.
                        mLastStreamStopTime = System.currentTimeMillis();
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("Error parsing Zello message: {}", message, e);
            }
        }
    }
}
