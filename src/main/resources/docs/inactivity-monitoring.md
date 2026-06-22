# Monitor channels for silence and poor audio quality

SDRTrunk Kennebec provides dual-layer channel telemetry: **inactivity monitoring** detects when a channel stops producing decoded calls, and **AI audio monitoring** verifies that recorded audio contains real voice or dispatch signaling rather than static or squelch noise.

Both layers route alerts through the notification system configured in **View → User Preferences → Notifications** (Telegram, SMTP email, and per-recipient routing). Configure delivery credentials and recipients before enabling channel alerts.

## Global notification configuration

Open **View → User Preferences → Notifications** to configure:

| Pane | Purpose |
| --- | --- |
| **Telegram** | Bot token from @BotFather for low-latency mobile push alerts |
| **SMTP Email** | Host, port (465 or 587), username, password, and from address for HTML email alerts |
| **Notification recipients & routing** | Per-recipient delivery method and category toggles, including **Channel Inactivity** and **AI Audio** |

Route high-priority trunk channels to Telegram and lower-priority channels to email by creating separate recipients with different category toggles.

## Per-channel configuration

Open the **Playlist Editor**, select a channel, and open the **Alerts** tab.

### Channel inactivity alerts

| Setting | Description |
| --- | --- |
| **Enable Alert** | Master toggle for inactivity monitoring on this channel |
| **Duration Threshold (minutes)** | Maximum silence before the monitor escalates (e.g. 10 minutes for dispatch, days for tactical channels) |
| **Auto-Restart Channel** | Automatically tear down and restart the channel before alerting a human (up to 2 attempts per episode) |

When a threshold is exceeded, the monitor enters a short grace period to absorb brief control-channel dropouts. If silence continues, auto-restart runs first when enabled. Persistent silence triggers Telegram/email alerts. Multiple inactive channels on the same tuner are bundled into a single correlated alert.

When audio resumes, a recovery notification is sent and timers reset.

### AI audio monitoring

| Setting | Description |
| --- | --- |
| **Enable Monitoring** | Master toggle for Gemini-based audio verification |
| **Check Interval (hours)** | How often the monitor sweeps new recordings for the channel |
| **Wait for New Audio on Failure** | After a failed verification, pause until the radio keys up again before re-checking (avoids re-analyzing the same static) |
| **Alert Threshold (failures)** | Consecutive invalid recordings required before an external alert is sent |

Gemini classifies each recording using structured output (`is_valid_transmission`, acoustic profile, confidence). Isolated static or empty key-ups are absorbed silently. Sustained failures trigger alerts with exponential backoff to prevent notification loops. Recovery requires consecutive valid verifications before a resolved notification is sent.

Requires a Gemini API key in **View → User Preferences → AI Settings**.

## Use cases

**Detecting a down feed**

Inactivity monitoring alerts you when a streamed channel goes quiet so you can determine whether the feed or the radio system is down.

**Watching a high-priority talkgroup**

A short inactivity threshold on a primary dispatch channel catches decoder stalls quickly.

**Catching false squelch breaks**

AI audio monitoring detects when SDRTrunk records static or noise that satisfies the inactivity timer but is not real traffic.

**Overnight unattended monitoring**

Telegram push alerts notify you on your phone without checking the application manually.

## Choosing thresholds

| Channel type | Suggested inactivity threshold |
| --- | --- |
| High-traffic dispatch (24/7) | 10–15 minutes |
| Moderate-traffic talkgroup | 20–30 minutes |
| Low-traffic or overnight channel | 60 minutes or more |

Start with longer thresholds and adjust downward once you understand normal activity patterns. Thresholds under 5 minutes can generate frequent alerts during natural quiet periods.

## Troubleshooting

**Alerts not firing when channel goes silent**

Confirm the channel's **Enable Alert** toggle is on, the channel is actively processing, and at least one notification recipient has **Channel Inactivity** enabled with Telegram or email configured.

**AI alerts not firing**

Confirm **Enable Monitoring** is on, a Gemini API key is set, recordings exist for the channel, and a recipient has **AI Audio** enabled.

**Receiving too many alerts**

Increase the inactivity threshold, raise the AI alert failure threshold, or reduce monitored channels. The system applies grace periods, auto-restart, wait-for-new-audio, exponential backoff, and tuner-level deduplication to limit alert fatigue.

**Alert not clearing after audio resumes**

The inactivity timer resets when the channel produces a new decoded audio event. AI recovery requires consecutive valid verifications before a resolved notification is sent.
