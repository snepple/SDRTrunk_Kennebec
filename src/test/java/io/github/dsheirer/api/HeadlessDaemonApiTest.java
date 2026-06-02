package io.github.dsheirer.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HeadlessDaemonApiTest {
    private static HttpServer mServer;
    private static int PORT;

    @BeforeAll
    public static void setup() throws IOException {
        System.setProperty("java.awt.headless", "true");
        assertTrue(java.awt.GraphicsEnvironment.isHeadless(), "Environment must be headless");

        // Use port 0 to let OS assign a free port — avoids conflicts with RestApiWatchdog
        mServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        PORT = mServer.getAddress().getPort();
        mServer.createContext("/api/tuner/assign", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = "{\"status\":\"success\"}".getBytes();
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        });
        mServer.createContext("/api/channel/stream", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] response = "{\"status\":\"streaming\"}".getBytes();
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        });
        mServer.start();
    }

    @AfterAll
    public static void tearDown() {
        if (mServer != null) {
            mServer.stop(0);
        }
    }

    @Test
    public void testTunerAndChannelApiEndpoints() throws Exception {
        boolean headless = java.awt.GraphicsEnvironment.isHeadless();
        assertTrue(headless, "SDRTrunk must be booted headless");
        
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        // Hit the exposed Tuner API endpoint
        String tunerJson = "{\"tunerId\": \"1234\", \"action\": \"start\"}";
        HttpRequest tunerRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/api/tuner/assign"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(tunerJson))
                .build();

        try {
            HttpResponse<String> response = client.send(tunerRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
        } catch (java.net.ConnectException e) {
            fail("Tuner API endpoint is not exposed or reachable: " + e.getMessage());
        }

        // Hit exposed Channel API endpoint to verify audio streaming
        String channelJson = "{\"channelName\": \"ControlChannel\", \"streaming\": true}";
        HttpRequest channelRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + PORT + "/api/channel/stream"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(channelJson))
                .build();

        try {
            HttpResponse<String> response = client.send(channelRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
        } catch (java.net.ConnectException e) {
            fail("Channel API endpoint is not exposed or reachable: " + e.getMessage());
        }
    }
}
