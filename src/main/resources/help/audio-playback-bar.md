# Audio Playback Bar

## Goal
Learn how to use the always-visible sticky Audio Playback Bar to control master volume, manage active calls, and mute audio instantly without losing your place in the app.

The **Audio Playback Bar** is locked to the top right of the SDRTrunk Kennebec window. It acts like an iTunes-style controller, letting you manage what you are hearing regardless of which settings page or tab you are currently viewing.

## Component Map

* **Master Volume Slider:** Controls the overall volume of all decoded radio traffic.
* **Master Mute Pill:** A bold, pill-shaped capsule (`Mute` / `Muted`). Click to instantly silence all audio. It turns warning-red when active.
* **Dynamic Channel Cards:** Translucent glassmorphism boxes that appear automatically when a channel starts receiving a call.
* **Channel Artwork:** A squircle graphic on the channel card that displays your uploaded custom artwork or a default waveform when active.

## How to manage active calls

1. **Monitor Activity:** When a call begins, a new translucent channel card appears in the bar.
2. **Identify the Caller:** The card displays the Talkgroup or Radio ID, along with the channel name and artwork.
3. **Mute Instantly:** If a call is too loud or unwanted, click the **Mute** pill to silence everything globally without needing to stop the channel.
4. **Auto-Hide:** When the call ends, the channel card gracefully disappears, keeping your interface clean.
