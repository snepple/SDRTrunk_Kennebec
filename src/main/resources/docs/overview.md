# Streaming Audio to the Internet

> An easy-to-understand guide on how to send your radio calls from SDRTrunk Kennebec to the internet, so you can listen on your phone or share with friends!

SDRTrunk Kennebec can take the radio calls it hears and instantly send them to online platforms. Think of it like setting up your own private internet radio station!

## How Streaming Works

Whenever a radio call finishes, SDRTrunk quickly packages it into an MP3 file (like a tiny music track) and hands it off to your chosen streaming platform. 

You can even stream to multiple places at the exact same time without slowing down the application!

> **Note:**
> Each stream runs entirely on its own. If your connection to one platform drops, the others will keep playing without missing a beat!

## Supported Streaming Platforms

Here are the places you can send your audio:

| Platform | What it's best for |
|---|---|
| **Broadcastify** | Great for sharing your local public safety feeds with the world. |
| **Zello** | Perfect for a private push-to-talk group with your friends or team. |
| **Rdio Scanner** | Awesome for creating a searchable, private archive of all calls. |
| **OpenMHz** | Similar to Rdio Scanner, a great public call archive. |
| **ThinLine Radio** | A platform for logging and storing completed radio calls. |
| **IAmResponding** | Sends paging audio to first responders' phones. |

## How to Add a Stream

Setting up a new stream is as easy as filling out a short form!

1. **Open Streaming:** Go to **View** > **Streaming** at the top of the screen.
2. **Add New:** Click the **+** button and choose where you want to stream (e.g., Zello, Broadcastify).
3. **Fill in the Blanks:** Type in the login details or API keys provided by the platform.
4. **Turn it On:** Check the **Enabled** box and click **Save**. 

SDRTrunk Kennebec will immediately connect. If your internet blips, it will automatically try to reconnect for you!

## Checking Your Stream Health

Want to know if your stream is working? Go to **View** > **Broadcaster Status**. You'll see a simple status for every stream:

* **Connected:** Everything is running smoothly!
* **Disconnected:** It can't reach the internet. Check your wifi or passwords.
* **Error:** Something went wrong (check the logs for a clue).

> **Tip:**
> Setting up streaming for the first time? Check our specific guides for platforms like [ThinLine Radio](thinline-radio.md) or [IAmResponding](iamresponding.md) for more details!