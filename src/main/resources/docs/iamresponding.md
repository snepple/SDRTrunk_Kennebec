# 🚒 IAmResponding Integration

> Send live paging audio from SDRTrunk Kennebec directly to the IAmResponding app for first responders!

If you use IAmResponding (IAR) for your fire department or EMS squad, you can link SDRTrunk Kennebec directly to it! When a dispatch page goes out over the radio, Kennebec can instantly send that audio snippet to the IAmResponding servers, so it pops up on everyone's phones.

## 🧠 How It Works

Think of this like a smart voice mailbox for your dispatchers.
1. SDRTrunk listens for the specific "beeps" (Two-Tone pages) that wake up the pagers.
2. When it hears the beeps, it starts recording the dispatcher's voice.
3. As soon as the dispatcher finishes, the recording is securely zipped over to IAmResponding.

> [!NOTE]
> This integration currently works best on **Windows** using a raw audio connection.

## 🛠️ Setup Instructions

### 1. Get Your IAR Information
You will need your special connection details from your IAmResponding dashboard. This usually includes a special email address or an API code. 

### 2. Configure the Broadcaster
1. Go to **View** > **Streaming**.
2. Click the **+** button and select **IAmResponding**.
3. Fill out the configuration form:

| 📝 Field | 💡 What to type |
|---|---|
| **Name** | Something simple like "Main Dispatch to IAR" |
| **IAR Target IP** | The server address given by IAmResponding |
| **Port** | The UDP port (usually a 4-digit number) |

### 3. Link to Your Channel
Make sure your dispatch channel in the **Playlist Editor** has **Two-Tone Detect** turned on! This tells Kennebec to listen for the "beeps".

> [!TIP]
> We recommend doing a "test page" with your dispatch center to make sure the audio shows up in the IAmResponding app loud and clear!
