# Quickstart Guide

Let's get SDRTrunk Kennebec running and listening to your first channel! Don't worry if this is your first time — we'll take it step by step.

> **Important:**
> Make sure you have your USB radio antenna (SDR) plugged into your computer before we start!

## Step 1: Install the Software

You don't need to be a computer whiz to install Kennebec. It comes completely ready to go.

1. Download the latest "Release" zip file for your computer (Windows, Mac, or Linux) from our website.
2. Extract the zip folder to your Desktop or Documents folder.
3. Open the folder and double click the `sdrtrunk` icon (on Windows, it's `sdrtrunk.bat`).

> **Note:**
> The app has Java built right in, so you don't need to install any extra programming tools!

## Step 2: Check Your Antenna

Let's make sure the application can see your USB antenna.

1. Click the **Tuners** button at the top of the screen.
2. Look for your device (like "RTL-SDR" or "Airspy") in the list.
3. If you see it, you are ready to go!

> **Warning:**
> On Windows, you might need to install a special driver called **WinUSB** using a free tool called Zadig. If your antenna doesn't show up, check our hardware guides!

## Step 3: Tell it What to Listen To

Now we need to tell the app which "highway" (radio system) to tune into. We do this in the **Playlist Editor**.

1. Click **Playlist Editor** at the top of the screen.
2. Click **New Playlist** and type a name (like "My City Police").
3. Inside your new playlist, click **Add Channel**.
4. Choose the **Protocol** (the radio "language"). For most modern police, this is **P25 Phase 1**.
5. Type in the **Frequency** of the system. (You can find this number on RadioReference.com).
6. Pick your antenna from the **Source** dropdown menu.
7. Click **Save**.

## Step 4: Start Listening!

You're almost there! Let's turn it on.

1. In the **Playlist Editor**, click on the channel you just made.
2. Click the **Enable** button. 
3. Close the Playlist window.

Now, watch the **Now Playing** box on your main screen. It might take a few seconds to lock on. Once it connects, you will start seeing calls pop up, and you'll hear the audio!

> **Tip:**
> If you hear robot noises instead of voices, double check that you entered the right Frequency and Protocol!

## What's Next?

Congratulations! You are officially scanning. You can now try:
* Giving names to the people talking (Adding Aliases).
* Sending the audio to your phone (Streaming).
* Turning on AI features to clean up the sound.
