package io.github.dsheirer.gui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Local REST API for external health monitoring and watchdog restart support.
 *
 * Endpoints (bound to 127.0.0.1 only):
 *   GET  /health  - returns aggregated application health (tuners, channels, streams) as JSON.
 *                   Returns HTTP 200 when healthy, HTTP 503 when any component is degraded, so it
 *                   can be used directly by external monitors (systemd, uptime-kuma, healthchecks).
 *   POST /restart - exits the application with a non-zero code so a wrapper script/service restarts it.
 *
 * Configuration (system property, or environment variable in parentheses):
 *   sdrtrunk.api.port  (SDRTRUNK_API_PORT)  - port to bind, default 8080 with fallback to port+1
 *   sdrtrunk.api.token (SDRTRUNK_API_TOKEN) - when set, /restart requires header "Authorization: Bearer <token>"
 */
public class RestApiWatchdog {
    private static final Logger mLog = LoggerFactory.getLogger(RestApiWatchdog.class);
    private static final int DEFAULT_PORT = 8080;
    private static HttpServer server;

    private static TunerManager sTunerManager;
    private static ChannelProcessingManager sChannelProcessingManager;
    private static BroadcastModel sBroadcastModel;

    /**
     * Starts the REST API with deep health checks.
     * @param app application reference used for restart
     * @param tunerManager for tuner health (optional, may be null)
     * @param channelProcessingManager for channel health (optional, may be null)
     * @param broadcastModel for streaming health (optional, may be null)
     */
    public static void start(SDRTrunk app, TunerManager tunerManager,
                             ChannelProcessingManager channelProcessingManager, BroadcastModel broadcastModel) {
        sTunerManager = tunerManager;
        sChannelProcessingManager = channelProcessingManager;
        sBroadcastModel = broadcastModel;
        start(app);
    }

    public static void start(SDRTrunk app) {
        int port = getConfiguredPort();

        try {
            server = createServer(app, port);
            server.start();
            mLog.info("REST API Watchdog listening on http://127.0.0.1:" + port);
        } catch (IOException e) {
            mLog.error("Failed to start REST API Watchdog on port " + port, e);
            // Try fallback port
            try {
                server = createServer(app, port + 1);
                server.start();
                mLog.info("REST API Watchdog listening on fallback port http://127.0.0.1:" + (port + 1));
            } catch (IOException ex) {
                mLog.error("Failed to start REST API Watchdog on fallback port " + (port + 1), ex);
            }
        }

        if (getConfiguredToken() == null) {
            mLog.info("REST API /restart endpoint is unauthenticated - set SDRTRUNK_API_TOKEN " +
                "(or -Dsdrtrunk.api.token) to require an Authorization: Bearer token");
        }
    }

    private static HttpServer createServer(SDRTrunk app, int port) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        httpServer.createContext("/health", new HealthHandler());
        httpServer.createContext("/metrics", new MetricsHandler());
        httpServer.createContext("/restart", new RestartHandler(app));
        httpServer.setExecutor(null); // creates a default executor
        return httpServer;
    }

    public static int getConfiguredPort() {
        String value = System.getProperty("sdrtrunk.api.port", System.getenv("SDRTRUNK_API_PORT"));

        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                mLog.warn("Invalid REST API port [" + value + "] - using default " + DEFAULT_PORT);
            }
        }

        return DEFAULT_PORT;
    }

    private static String getConfiguredToken() {
        String token = System.getProperty("sdrtrunk.api.token", System.getenv("SDRTRUNK_API_TOKEN"));
        return (token == null || token.isBlank()) ? null : token.trim();
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Aggregates real application state: per-tuner status, channel processing count, and
     * per-stream broadcast state.  Reports 503 when any tuner is in an error/recovering state or
     * any configured stream is in an error state, so external monitors can react.
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                boolean degraded = false;
                StringBuilder json = new StringBuilder();
                json.append("{");

                //Tuners
                json.append("\"tuners\":[");
                if (sTunerManager != null) {
                    boolean first = true;
                    for (DiscoveredTuner tuner : sTunerManager.getAvailableTuners()) {
                        if (!first) {
                            json.append(",");
                        }
                        first = false;

                        TunerStatus status = tuner.getTunerStatus();
                        if (tuner.isEnabled() && (status == TunerStatus.ERROR || status == TunerStatus.RECOVERING)) {
                            degraded = true;
                        }

                        json.append("{\"id\":\"").append(escapeJson(tuner.getId()))
                            .append("\",\"status\":\"").append(status)
                            .append("\",\"error\":\"").append(escapeJson(tuner.getErrorMessage()))
                            .append("\"}");
                    }
                }
                json.append("],");

                //Channels
                int processing = sChannelProcessingManager != null ?
                    sChannelProcessingManager.getProcessingChannelCount() : -1;
                json.append("\"channelsProcessing\":").append(processing).append(",");

                //Streams
                json.append("\"streams\":[");
                if (sBroadcastModel != null) {
                    boolean first = true;
                    for (BroadcastConfiguration config : sBroadcastModel.getBroadcastConfigurations()) {
                        if (!config.isEnabled()) {
                            continue;
                        }

                        AbstractAudioBroadcaster<?> broadcaster = sBroadcastModel.getBroadcaster(config.getName());
                        BroadcastState state = broadcaster != null ? broadcaster.getBroadcastState() : null;

                        if (state != null && state.isErrorState()) {
                            degraded = true;
                        }

                        if (!first) {
                            json.append(",");
                        }
                        first = false;

                        json.append("{\"name\":\"").append(escapeJson(config.getName()))
                            .append("\",\"state\":\"").append(state != null ? state : "NOT_STARTED")
                            .append("\"}");
                    }
                }
                json.append("],");

                json.append("\"status\":\"").append(degraded ? "DEGRADED" : "UP").append("\"}");

                respond(t, degraded ? 503 : 200, json.toString());
            } catch (Exception e) {
                mLog.error("Error building health response", e);
                respond(t, 500, "{\"status\":\"ERROR\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * Prometheus text exposition format metrics for external monitoring/dashboards (e.g. Prometheus,
     * Grafana, uptime-kuma).  Exposes per-tuner status, channel processing count, and per-stream state
     * and throughput counters so slow degradations are visible before they become outages.
     */
    static class MetricsHandler implements HttpHandler {
        private static String label(String value) {
            return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            try {
                StringBuilder m = new StringBuilder();

                m.append("# HELP sdrtrunk_up Application liveness (always 1 when responding)\n");
                m.append("# TYPE sdrtrunk_up gauge\n");
                m.append("sdrtrunk_up 1\n");

                m.append("# HELP sdrtrunk_tuner_status Tuner status (1=reported state)\n");
                m.append("# TYPE sdrtrunk_tuner_status gauge\n");
                m.append("# HELP sdrtrunk_tuner_healthy Tuner is enabled and not in an error/recovering state\n");
                m.append("# TYPE sdrtrunk_tuner_healthy gauge\n");
                if (sTunerManager != null) {
                    for (DiscoveredTuner tuner : sTunerManager.getAvailableTuners()) {
                        TunerStatus status = tuner.getTunerStatus();
                        boolean healthy = !tuner.isEnabled() ||
                            (status != TunerStatus.ERROR && status != TunerStatus.RECOVERING);
                        m.append("sdrtrunk_tuner_status{id=\"").append(label(tuner.getId()))
                            .append("\",status=\"").append(status).append("\"} 1\n");
                        m.append("sdrtrunk_tuner_healthy{id=\"").append(label(tuner.getId()))
                            .append("\"} ").append(healthy ? 1 : 0).append("\n");
                    }
                }

                m.append("# HELP sdrtrunk_channels_processing Number of channels currently processing\n");
                m.append("# TYPE sdrtrunk_channels_processing gauge\n");
                m.append("sdrtrunk_channels_processing ").append(sChannelProcessingManager != null ?
                    sChannelProcessingManager.getProcessingChannelCount() : 0).append("\n");

                m.append("# HELP sdrtrunk_stream_connected Stream is in the CONNECTED state\n");
                m.append("# TYPE sdrtrunk_stream_connected gauge\n");
                m.append("# HELP sdrtrunk_stream_queue_size Audio recordings queued for streaming\n");
                m.append("# TYPE sdrtrunk_stream_queue_size gauge\n");
                m.append("# HELP sdrtrunk_stream_streamed_total Audio recordings streamed since startup\n");
                m.append("# TYPE sdrtrunk_stream_streamed_total counter\n");
                m.append("# HELP sdrtrunk_stream_aged_off_total Audio recordings aged off without streaming since startup\n");
                m.append("# TYPE sdrtrunk_stream_aged_off_total counter\n");
                if (sBroadcastModel != null) {
                    for (BroadcastConfiguration config : sBroadcastModel.getBroadcastConfigurations()) {
                        if (!config.isEnabled()) {
                            continue;
                        }

                        AbstractAudioBroadcaster<?> broadcaster = sBroadcastModel.getBroadcaster(config.getName());

                        if (broadcaster != null) {
                            String name = label(config.getName());
                            BroadcastState state = broadcaster.getBroadcastState();
                            m.append("sdrtrunk_stream_connected{name=\"").append(name).append("\"} ")
                                .append(state == BroadcastState.CONNECTED ? 1 : 0).append("\n");
                            m.append("sdrtrunk_stream_queue_size{name=\"").append(name).append("\"} ")
                                .append(broadcaster.getAudioQueueSize()).append("\n");
                            m.append("sdrtrunk_stream_streamed_total{name=\"").append(name).append("\"} ")
                                .append(broadcaster.getStreamedAudioCount()).append("\n");
                            m.append("sdrtrunk_stream_aged_off_total{name=\"").append(name).append("\"} ")
                                .append(broadcaster.getAgedOffAudioCount()).append("\n");
                        }
                    }
                }

                byte[] bytes = m.toString().getBytes(StandardCharsets.UTF_8);
                t.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
                t.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = t.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                mLog.error("Error building metrics response", e);
                respond(t, 500, "{\"status\":\"ERROR\"}");
            }
        }
    }

    static class RestartHandler implements HttpHandler {
        private final SDRTrunk app;

        public RestartHandler(SDRTrunk app) {
            this.app = app;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equalsIgnoreCase(t.getRequestMethod())) {
                t.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            String token = getConfiguredToken();

            if (token != null) {
                String auth = t.getRequestHeaders().getFirst("Authorization");

                if (auth == null || !auth.equals("Bearer " + token)) {
                    mLog.warn("Watchdog restart request rejected - missing or invalid Authorization token");
                    respond(t, 401, "{\"status\":\"UNAUTHORIZED\"}");
                    return;
                }
            }

            mLog.warn("Watchdog received force-restart request!");
            respond(t, 200, "{\"status\":\"RESTARTING\"}");

            // Shutdown and exit — the wrapper script/service will restart
            new Thread(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                System.exit(1);
            }).start();
        }
    }
}
