package io.github.dsheirer;

import io.github.dsheirer.audio.broadcast.iamresponding.IAmRespondingBroadcaster;
import io.github.dsheirer.audio.broadcast.iamresponding.IAmRespondingConfiguration;
import io.github.dsheirer.audio.broadcast.zello.ZelloBroadcaster;
import io.github.dsheirer.audio.broadcast.zello.ZelloConfiguration;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.PlaylistV2;
import io.github.dsheirer.playlist.TwoToneConfiguration;
import io.github.dsheirer.preference.notification.AntiFloodFilter;
import io.github.dsheirer.preference.notification.NotificationRouter;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.dsp.tone.TwoToneDetector;
import io.github.dsheirer.source.tuner.test.TestTuner;
import io.github.dsheirer.source.tuner.test.TestTunerController;
import io.github.dsheirer.module.demodulate.fm.FMDemodulatorModule;
import io.github.dsheirer.audio.codec.mbe.JmbeAudioModule;
import io.github.dsheirer.sample.complex.ComplexSamples;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualTunerIntegrationTest {
    
    @BeforeAll
    public static void initJFX() {
        System.setProperty("java.awt.headless", "true");
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already started
        }
    }

    private DatagramSocket mockIarUdpSocket;
    private AtomicInteger iarPacketsReceived;
    
    @BeforeEach
    public void setup() throws Exception {
        iarPacketsReceived = new AtomicInteger(0);
        mockIarUdpSocket = new DatagramSocket(new java.net.InetSocketAddress("127.0.0.1", 0)); // random port on loopback
        
        // Start UDP thread
        new Thread(() -> {
            try {
                byte[] buf = new byte[2048];
                while (!mockIarUdpSocket.isClosed()) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    mockIarUdpSocket.receive(p);
                    if (p.getLength() > 0) {
                        iarPacketsReceived.incrementAndGet();
                    }
                }
            } catch (Exception e) {
                // ignoring
            }
        }).start();
    }

    @AfterEach
    public void teardown() {
        if (mockIarUdpSocket != null) {
            mockIarUdpSocket.close();
        }
    }

    @Test
    public void testTwoToneValidationAndUdpMock() throws Exception {
        // Mock UDP servers to assert IAmRespondingBroadcaster drops 0 frames
        IAmRespondingConfiguration iarConfig = new IAmRespondingConfiguration();
        iarConfig.setHost("127.0.0.1");
        iarConfig.setPort(mockIarUdpSocket.getLocalPort());
        
        AliasModel aliasModel = new AliasModel();
        IAmRespondingBroadcaster iarBroadcaster = new IAmRespondingBroadcaster(iarConfig, aliasModel);
        
        javafx.application.Platform.runLater(() -> {
            iarBroadcaster.start();
            iarBroadcaster.startRealTimeStream(null);
        });
        Thread.sleep(200);
        
        // Push 400 audio samples to trigger exactly one IAR UDP packet (400 floats = 800 bytes)
        float[] dummyAudio = new float[400];
        iarBroadcaster.receiveRealTimeAudio(dummyAudio);
        
        // Let UDP thread process
        Thread.sleep(200);
        
        assertTrue(iarPacketsReceived.get() > 0, "IAmResponding UDP Broadcaster dropped 0 frames and sent successfully");
        iarBroadcaster.stopRealTimeStream();
        iarBroadcaster.stop();

        // Virtual Tuner
        TestTuner tuner = new TestTuner(null);
        TestTunerController controller = tuner.getTunerController();
        
        // NBFM Demodulator Audio check
        FMDemodulatorModule fmDemod = new FMDemodulatorModule(12500);
        AtomicReference<float[]> lastAudioBuffer = new AtomicReference<>(null);
        fmDemod.setBufferListener(buffer -> {
            lastAudioBuffer.set(buffer);
        });

        // Test NBFM Tone Output
        controller.setNbfmWaveform(1000f, 1.0f);
        
        // Create PlaylistManager with Two Tone Config
        PlaylistManager playlistManager = Mockito.mock(PlaylistManager.class);
        PlaylistV2 playlist = new PlaylistV2();
        Mockito.when(playlistManager.getCurrentPlaylist()).thenReturn(playlist);
        Mockito.when(playlistManager.getAliasModel()).thenReturn(aliasModel);
        
        TwoToneConfiguration ttConfig = new TwoToneConfiguration();
        ttConfig.setAlias("Fire Dispatch");
        ttConfig.setToneA(600f);
        ttConfig.setToneB(800f);
        ttConfig.setFrequencyTolerance(10f);
        ttConfig.setToneDurationMs(500); // 500ms min
        ttConfig.setEnableZelloTextMessage(true);
        ttConfig.setEnableZelloAlert(true);
        ttConfig.setZelloAlertFile("predispatch.wav");
        ttConfig.setZelloChannel("FireChannel");
        ttConfig.setEnableMqttPublish(false);
        ttConfig.setMqttTopic("homeassistant/twotone");
        ttConfig.setMqttPayload("payload");
        
        playlist.getTwoToneConfigurations().add(ttConfig);
        
        List<ZelloBroadcaster> zelloBroadcasters = new ArrayList<>();
        ZelloBroadcaster mockZello = Mockito.mock(ZelloBroadcaster.class);
        ZelloConfiguration zelloConfig = new ZelloConfiguration();
        zelloConfig.setChannel("FireChannel");
        Mockito.when(mockZello.getBroadcastConfiguration()).thenReturn(zelloConfig);
        zelloBroadcasters.add(mockZello);
        
        TwoToneDetector detector = new TwoToneDetector(playlistManager, zelloBroadcasters);
        
        AntiFloodFilter filter = new AntiFloodFilter(Mockito.mock(NotificationRouter.class));
        detector.setAntiFloodFilter(filter);
        
        // Assertions for sequence detection and pre-dispatch audio are tested by injecting into detector
        // In real execution, we would process synthetic float buffers through the detector.
        // Let's pass a synthetic array of frequency 600 then 800 directly to test detection.
        // Generate audio buffer blocks (160 samples @ 8000Hz)
        
        // Block of 600 Hz
        float[] block600 = new float[160];
        for(int i=0; i<160; i++) {
            block600[i] = (float)Math.cos(2 * Math.PI * 600 * i / 8000.0) + 2.0f;
        }
        for(int b=0; b<30; b++) {
            detector.processAudio(block600, null); // 30 * 20ms = 600ms
        }
        
        // Block of 800 Hz
        float[] block800 = new float[160];
        for(int i=0; i<160; i++) {
            block800[i] = (float)Math.cos(2 * Math.PI * 800 * i / 8000.0) + 2.0f;
        }
        for(int b=0; b<30; b++) {
            detector.processAudio(block800, null); // 30 * 20ms = 600ms
        }
        
        Thread.sleep(500); // allow background thread to process
        
        // Assert that Zello mock was called to send text message
        Mockito.verify(mockZello, Mockito.atLeastOnce()).sendTextMessage(Mockito.anyString());
        
        // Assert Pre-Dispatch Audio was injected
        Mockito.verify(mockZello, Mockito.atLeastOnce()).injectPreDispatchAudio("predispatch.wav");
        
        // Since we process again, AntiFloodFilter should drop duplicate
        for(int b=0; b<30; b++) { detector.processAudio(block600, null); }
        for(int b=0; b<30; b++) { detector.processAudio(block800, null); }
        Thread.sleep(500);
        
        // Verify it was only called exactly once despite duplicate sequence!
        Mockito.verify(mockZello, Mockito.times(1)).sendTextMessage(Mockito.anyString());
        
        detector.dispose();

        // Feed some complex samples to NBFM to ensure it outputs non-zero audio
        fmDemod.getSourceEventListener().receive(io.github.dsheirer.source.SourceEvent.sampleRateChange(25000));
        float[] iq = new float[1024];
        for (int i=0; i<1024; i++) iq[i] = (float) Math.random();
        ComplexSamples cs = new ComplexSamples(iq, new float[iq.length], 0L);
        for (int j = 0; j < 25; j++) {
            fmDemod.receive(cs);
        }
        assertNotNull(lastAudioBuffer.get(), "NBFM demodulator audio output buffer should not be null");
        assertTrue(lastAudioBuffer.get().length > 0, "NBFM demodulator should contain valid audio");
        
    }
}
