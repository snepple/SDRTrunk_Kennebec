package io.github.dsheirer.gui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class RestApiWatchdog {
    private static final Logger mLog = LoggerFactory.getLogger(RestApiWatchdog.class);
    private static HttpServer server;

    public static void start(SDRTrunk app) {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
            server.createContext("/health", new HealthHandler());
            server.createContext("/restart", new RestartHandler(app));
            server.setExecutor(null); // creates a default executor
            server.start();
            mLog.info("REST API Watchdog listening on http://127.0.0.1:8080");
        } catch (IOException e) {
            mLog.error("Failed to start REST API Watchdog on port 8080", e);
            // Try fallback port
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8081), 0);
                server.createContext("/health", new HealthHandler());
                server.createContext("/restart", new RestartHandler(app));
                server.setExecutor(null);
                server.start();
                mLog.info("REST API Watchdog listening on fallback port http://127.0.0.1:8081");
            } catch (IOException ex) {
                mLog.error("Failed to start REST API Watchdog on fallback port 8081", ex);
            }
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "{\"status\":\"UP\", \"message\":\"SDRTrunk is running\"}";
            t.getResponseHeaders().add("Content-Type", "application/json");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class RestartHandler implements HttpHandler {
        private final SDRTrunk app;

        public RestartHandler(SDRTrunk app) {
            this.app = app;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                mLog.warn("Watchdog received force-restart request!");
                
                String response = "{\"status\":\"RESTARTING\"}";
                t.getResponseHeaders().add("Content-Type", "application/json");
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();

                // Shutdown and exit — the wrapper script/service will restart
                new Thread(() -> {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    System.exit(1);
                }).start();
            } else {
                t.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }
}
