# Monitor channels for silence and inactivity

> Set up SDRTrunk Kennebec to alert you via Telegram or Email when a monitored channel stays silent beyond a configurable time threshold.

Inactivity monitoring watches a channel for audio activity and fires an alert — via Telegram or Email — when the channel has been silent for longer than a threshold you define. This is useful for detecting a feed that has gone down, a control channel that has stopped decoding, or simply any channel you expect to be regularly active.

## How inactivity monitoring works

SDRTrunk Kennebec tracks the time since the last decoded audio event on each monitored channel. When that elapsed time exceeds the configured silence threshold, the application sends the alert through whichever delivery method you have enabled (Telegram, Email, or both). Once audio activity resumes, the timer resets automatically — you will not receive a repeat alert until the channel goes silent again for the full threshold duration.

> **Note:**
  Inactivity monitoring relies on the notification delivery methods (Telegram and/or Email) you have already configured. Set up at least one delivery method in **User Preferences → Notifications** before enabling inactivity monitoring.

## Use cases

  ### Detecting a down feed
    If you stream a channel to Broadcastify or another platform, inactivity monitoring alerts you when the source goes quiet — so you can investigate whether the feed is genuinely down or the radio system itself is inactive.


  ### Watching a high-priority talkgroup
    For talkgroups you expect to hear regularly (e.g., a primary dispatch channel), a silence alert tells you if decoding has stalled or if there is an upstream problem with the radio system.


  ### Overnight and unattended monitoring
    When running SDRTrunk Kennebec unattended, inactivity monitoring acts as a lightweight watchdog, notifying you on your phone via Telegram if something stops working without requiring you to check the application manually.


## Enable inactivity monitoring

  ### Open User Preferences
    Go to **View** → **User Preferences** in the menu bar.


  ### Navigate to Inactivity Monitoring
    Select **Inactivity Monitoring** in the left sidebar of the User Preferences panel.


  ### Enable the feature
    Turn on the **Enable Inactivity Monitoring** toggle.


  ### Set the silence threshold
    Enter your desired silence duration in the **Silence Threshold** field. The value is in minutes. For example, entering `15` means an alert fires if a channel produces no audio for 15 consecutive minutes.


  ### Choose channels to monitor
    Under **Monitored Channels**, select which channels or talkgroups to watch. You can apply monitoring globally to all active channels or limit it to specific entries from your playlist.


  ### Select alert delivery methods
    Enable **Telegram**, **Email**, or both, depending on how you want to receive silence alerts. These toggle independently from your error notification delivery settings, so you can use a different combination if needed.


> **Tip:**
  Start with a threshold of 30 minutes or longer to reduce false positives on channels that naturally experience quiet periods, such as low-traffic talkgroups during overnight hours. Adjust downward once you understand the typical activity pattern of the channel.

## Setting the silence threshold

Choose a threshold that reflects the expected activity level of the channel:

| Channel type                     | Suggested starting threshold |
| -------------------------------- | ---------------------------- |
| High-traffic dispatch (24/7)     | 10–15 minutes                |
| Moderate-traffic talkgroup       | 20–30 minutes                |
| Low-traffic or overnight channel | 60 minutes or more           |

> **Warning:**
  Very short thresholds (under 5 minutes) can generate frequent alerts on channels that have natural lulls in traffic. Consider the typical usage pattern of a channel before setting a low value.

## Alert message content

When a silence alert fires, the message includes:

* The name of the silent channel or talkgroup
* The duration of silence at the time the alert was sent
* The configured threshold that was exceeded

This information is delivered identically whether you receive the alert via Telegram or Email.

## Troubleshooting

  ### Alerts not firing when channel goes silent
    Confirm that **Enable Inactivity Monitoring** is toggled on and that at least one delivery method (Telegram or Email) is enabled in both the Inactivity Monitoring settings and the Notifications settings. Also verify that the channel is listed under Monitored Channels.


  ### Receiving too many alerts
    Increase the silence threshold or reduce the number of monitored channels. Channels with naturally long gaps between transmissions are poor candidates for tight thresholds.


  ### Alert not clearing after audio resumes
    The timer resets automatically when the channel produces a new decoded audio event. If you believe activity has resumed but the status has not updated, check whether the decoder for that channel is still running in the **Now Playing** view.