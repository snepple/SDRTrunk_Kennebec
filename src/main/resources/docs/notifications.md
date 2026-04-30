# Set up error notifications via Telegram and Email

> Configure SDRTrunk Kennebec to send Telegram or Email alerts when application or tuner errors occur, so you never miss a critical failure.

SDRTrunk Kennebec can send you a notification — via Telegram or Email — the moment a tuner or application error occurs. This means you don't have to keep the application window in focus to know when something goes wrong; alerts reach you wherever you are.

## When notifications fire

Notifications trigger on two categories of events:

* **Tuner errors** — a connected SDR device encounters a hardware fault, stops responding, or fails to recover after a self-healing attempt.
* **Application errors** — an unhandled exception or critical failure occurs within SDRTrunk Kennebec itself.

> **Note:**
  SDRTrunk Kennebec also includes tuner self-healing logic that attempts to recover a failed device before firing a notification. You will only receive an alert if the automatic recovery fails.

## Open notification settings

  ### Open User Preferences
    In the menu bar, go to **View** → **User Preferences**. All notification settings are consolidated in this single panel.


  ### Navigate to Notifications
    In the left sidebar of User Preferences, select **Notifications**.


## Configure Telegram alerts

To receive alerts through Telegram, you need a bot token and a chat ID. Both are free to obtain from the Telegram app.

  ### Create a Telegram bot
    Open Telegram and start a conversation with **@BotFather**. Send the `/newbot` command and follow the prompts. BotFather will give you a **bot token** — a string that looks like `123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11`.


  ### Find your chat ID
    Send any message to your new bot, then open a browser and visit:

    ```text theme={null}
    https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
    ```

    Look for the `"id"` field inside the `"chat"` object. That number is your chat ID.


  ### Enter your credentials in SDRTrunk
    In the **Notifications** panel of User Preferences:

    * Paste your bot token into the **Telegram Bot Token** field.
    * Paste your chat ID into the **Telegram Chat ID** field.
    * Enable the **Telegram Notifications** toggle.


  ### Test the configuration
    Click **Send Test Message**. If everything is set up correctly, your Telegram bot will deliver a test message within a few seconds.


> **Tip:**
  You can send alerts to a Telegram group or channel instead of a private chat. Add your bot to the group, then use the group's chat ID (which will be a negative number) in the **Telegram Chat ID** field.

## Configure Email alerts

SDRTrunk Kennebec sends email via SMTP, which works with Gmail, Outlook, or any standard mail provider.

  ### Enter SMTP settings
    In the **Notifications** panel of User Preferences, fill in the **Email / SMTP** section:

    | Field            | Description                                          |
    | ---------------- | ---------------------------------------------------- |
    | **SMTP Host**    | Your mail server hostname, e.g. `smtp.gmail.com`     |
    | **SMTP Port**    | Typically `587` (STARTTLS) or `465` (SSL)            |
    | **Username**     | Your email address or SMTP login                     |
    | **Password**     | Your email account password or app-specific password |
    | **From Address** | The address that will appear as the sender           |
    | **To Address**   | The address that will receive the alerts             |


  ### Enable Email notifications
    Turn on the **Email Notifications** toggle.


  ### Test the configuration
    Click **Send Test Email**. Check the inbox of your **To Address** for a test message from SDRTrunk Kennebec.


> **Warning:**
  If you use Gmail, Google requires an **App Password** rather than your regular account password. Generate one at **Google Account → Security → App passwords**. Using your regular password with 2-Step Verification enabled will cause authentication to fail.

## Troubleshooting

  ### No Telegram message received
    Verify that your bot token is correct and that your bot has not been blocked. Re-run the `getUpdates` check to confirm the chat ID is current. If you added the bot to a group, make sure the group chat ID (negative number) is entered, not your personal chat ID.


  ### Email delivery fails
    Check that your SMTP host and port are correct for your provider. For Gmail, confirm you are using an App Password and that "Less secure app access" restrictions have not blocked the connection. Review SDRTrunk's application log (**View → Logs**) for the exact SMTP error message.


  ### Notifications fire too frequently
    If tuner errors are occurring repeatedly, the underlying tuner may need attention. See the [tuner self-healing](/hardware/tuner-self-healing) guide for steps to diagnose and resolve persistent hardware faults.