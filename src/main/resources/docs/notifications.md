# Set Up Telegram and Email Error Notifications

SDRTrunk Kennebec can reach you wherever you are — via Telegram or Email — the moment something goes wrong with your hardware or the application itself. Alerts are sent automatically after the built-in self-healing logic has already attempted a recovery, so you only hear about problems that require your attention.

## When notifications fire

Notifications trigger on two categories of events:

| Event category | What causes it |
| --- | --- |
| **Hardware alerts** | A connected SDR tuner encounters a fault, stops responding, or fails to recover after an automatic self-healing attempt |
| **System alerts** | An unhandled exception or critical failure occurs inside SDRTrunk Kennebec |
| **Signal alerts** | A signal or decoding condition requires your attention |
| **Integration alerts** | A streaming or external integration fails |
| **Channel inactivity** | A monitored channel exceeds its configured silence threshold (see [Inactivity Monitoring](/alerts/inactivity-monitoring)) |
| **AI audio monitoring** | An AI-flagged audio quality issue is detected on a channel |

> **Note:**
> SDRTrunk Kennebec attempts to self-heal a failed tuner before sending any alert. You will only receive a hardware notification if the automatic recovery fails.

## Open notification settings

**1. Open User Preferences**

In the menu bar, go to **View** → **User Preferences**.

**2. Navigate to Notifications**

Select **Notifications** in the left sidebar of the **User Preferences** panel.

## Configure Telegram alerts

To receive alerts through Telegram, you need a bot token and a chat ID. Both are free to create from the Telegram app.

**1. Create a Telegram bot**

Open Telegram and start a conversation with **@BotFather**. Send the `/newbot` command and follow the prompts. BotFather will give you a **bot token** — a string that looks like `123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11`.

**2. Find your chat ID**

Send any message to your new bot, then open a browser and visit:

```text
https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
```

Look for the `"id"` field inside the `"chat"` object. That number is your chat ID.

**3. Enter your credentials in SDRTrunk Kennebec**

In the **Notifications** panel of **User Preferences**:

- Paste your bot token into the **Telegram Bot Token** field.
- Paste your chat ID into the **Telegram Chat ID** field.
- Enable the **Telegram Notifications** toggle.

**4. Test the configuration**

Click **Send Test Message**. Your Telegram bot should deliver a test message within a few seconds.

> **Tip:**
> You can send alerts to a Telegram group or channel instead of a private chat. Add your bot to the group, then use the group's chat ID — which will be a negative number — in the **Telegram Chat ID** field.

## Configure Email alerts

SDRTrunk Kennebec sends email via SMTP, which works with Gmail, Outlook, or any standard mail provider.

**1. Enter SMTP settings**

In the **Notifications** panel of **User Preferences**, fill in the **Email / SMTP** section:

| Field | Description |
| --- | --- |
| **SMTP Host** | Your mail server hostname, e.g. `smtp.gmail.com` |
| **SMTP Port** | Typically `587` (STARTTLS) or `465` (SSL) |
| **Username** | Your email address or SMTP login |
| **Password** | Your email account password or app-specific password |
| **From Address** | The address that will appear as the sender |
| **To Address** | The address that will receive the alerts |

**2. Enable Email notifications**

Turn on the **Email Notifications** toggle.

**3. Test the configuration**

Click **Send Test Email**. Check the inbox of your **To Address** for a test message from SDRTrunk Kennebec.

> **Warning:**
> If you use Gmail, Google requires an **App Password** rather than your regular account password. Generate one at **Google Account → Security → App passwords**. Using your regular password with 2-Step Verification enabled will cause authentication to fail.

## Add notification recipients

Each recipient entry controls which events are delivered to a specific destination and via which channel. You can add multiple recipients — for example, one Telegram chat for hardware alerts and a separate email address for system alerts.

To add a recipient, click **Add Recipient** in the **Notifications** panel and configure the following fields:

| Field | Description |
| --- | --- |
| **Delivery Method** | `EMAIL` or `TELEGRAM` |
| **Destination** | Email address, or Telegram chat ID (use a negative number for groups) |
| **Hardware Alerts** | Receive tuner fault and self-healing failure alerts |
| **System Alerts** | Receive application error and critical failure alerts |
| **Signal Alerts** | Receive signal and decoding condition alerts |
| **Integration Alerts** | Receive streaming or external integration failure alerts |
| **Channel Inactivity** | Receive silence alerts from inactivity monitoring |
| **AI Audio Monitoring** | Receive AI-flagged audio quality alerts |

Each toggle is independent, so you can tailor exactly which events reach each recipient.

## Troubleshooting

**No Telegram message received**

Verify that your bot token is correct and that your bot has not been blocked. Re-run the `getUpdates` check in your browser to confirm the chat ID is current. If you added the bot to a group, confirm you are using the group's chat ID (a negative number), not your personal chat ID.

**Email delivery fails**

Check that your SMTP host and port are correct for your provider. For Gmail, confirm you are using an App Password. Review SDRTrunk's application log (**View → Logs**) for the exact SMTP error message.

**Notifications fire too frequently**

If tuner errors repeat persistently, the underlying hardware may need attention. Review the application log for the specific tuner error, and check connections and USB power for the affected device.
