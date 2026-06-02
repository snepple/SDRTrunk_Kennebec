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

package io.github.dsheirer.audio.broadcast.webhook;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Sends HTTP POST webhook notifications for critical system events.
 * Payloads are JSON-encoded and include event type, message, timestamp, and optional metadata.
 */
public class WebhookAlertService
{
    private static final Logger mLog = LoggerFactory.getLogger(WebhookAlertService.class);
    private static final Gson GSON = new Gson();

    private final HttpClient mClient;
    private final ExecutorService mExecutor;
    private String mWebhookUrl;
    private boolean mEnabled = false;

    /**
     * Constructs the webhook alert service with an HTTP client and a single-threaded executor
     * for asynchronous alert delivery.
     */
    public WebhookAlertService()
    {
        mClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        mExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WebhookAlert");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Configures the webhook URL and enabled state.
     *
     * @param url the webhook endpoint URL
     * @param enabled true to enable webhook alerts
     */
    public void configure(String url, boolean enabled)
    {
        mWebhookUrl = url;
        mEnabled = enabled;
    }

    /**
     * Sends an alert to the configured webhook endpoint asynchronously.
     *
     * @param eventType the type of event (e.g. "STUCK_MIC", "STREAM_DOWN")
     * @param message a human-readable description of the event
     * @param metadata optional additional key-value data to include in the payload
     */
    public void sendAlert(String eventType, String message, Map<String, Object> metadata)
    {
        if(!mEnabled || mWebhookUrl == null || mWebhookUrl.isEmpty())
        {
            return;
        }

        mExecutor.submit(() -> {
            try
            {
                Map<String, Object> payload = new ConcurrentHashMap<>();
                payload.put("event", eventType);
                payload.put("message", message);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("source", "SDRTrunk_Kennebec");

                if(metadata != null)
                {
                    payload.put("metadata", metadata);
                }

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mWebhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                    .timeout(Duration.ofSeconds(10))
                    .build();

                HttpResponse<String> response = mClient.send(request, HttpResponse.BodyHandlers.ofString());

                if(response.statusCode() >= 200 && response.statusCode() < 300)
                {
                    mLog.debug("Webhook alert sent: {}", eventType);
                }
                else
                {
                    mLog.warn("Webhook alert failed (HTTP {}): {}", response.statusCode(), eventType);
                }
            }
            catch(Exception e)
            {
                mLog.error("Webhook alert error for event: {}", eventType, e);
            }
        });
    }

    /**
     * Stops the webhook executor service.
     */
    public void stop()
    {
        mExecutor.shutdownNow();
    }
}
