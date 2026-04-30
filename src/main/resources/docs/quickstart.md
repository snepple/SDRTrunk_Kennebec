# Quick start: install SDRTrunk Kennebec and scan radio

> Download or build SDRTrunk Kennebec, connect your SDR hardware, and decode your first radio channel — from installation to first audio in a few steps.

This guide walks you through getting SDRTrunk Kennebec running for the first time — from obtaining the application to hearing your first decoded channel. Before you begin, confirm that your system meets the [minimum requirements](/system-requirements) and that you have a compatible SDR device available.

> **Note:**
  SDRTrunk Kennebec is currently in **prerelease**. Prerelease builds are functional for most use cases. Check the [GitHub repository](https://github.com/snepple/SDRTrunk_Kennebec) for the latest release assets.

  ### Obtain SDRTrunk Kennebec
    You can either download a prebuilt release package or build from source using Gradle.

    **Option A — Download a release package**

    Download the prebuilt zip archive for your operating system from the [GitHub releases page](https://github.com/snepple/SDRTrunk_Kennebec). Release packages bundle a compatible JDK and do not require a separate Java installation.

    Extract the zip to a location of your choice, such as `C:\SDRTrunk` on Windows or `~/sdrtrunk` on Linux and macOS.

    **Option B — Build from source**

    Clone the repository and use the Gradle wrapper to build a runtime package for your current operating system:

    ```bash theme={null}
    git clone https://github.com/snepple/SDRTrunk_Kennebec.git
    cd SDRTrunk_Kennebec
    ./gradlew runtimeZipCurrent
    ```

    On Windows, use `gradlew.bat` instead of `./gradlew`. The built package is placed in the `build/image/` directory as a zip archive. Extract it before proceeding.

    > **Tip:**
      You can also run the application directly from source without packaging it: `./gradlew run`



  ### Confirm Java is available
    Prebuilt release packages ship with a bundled JDK — no separate Java installation is required. If you are running from source using `./gradlew run`, Gradle Toolchains automatically provisions Java 23 if it is not already present on your system.

    > **Info:**
      SDRTrunk Kennebec requires Java 23 or higher. If you are setting up a non-Gradle environment, install a JDK 23+ distribution such as [Bellsoft Liberica JDK](https://bell-sw.com/pages/downloads/) or [Azul Zulu](https://www.azul.com/downloads/).



  ### Launch the application
    Navigate to the extracted application directory and run the launcher for your operating system.

    **Windows:**

    ```text theme={null}
    bin\sdrtrunk.bat
    ```

    **Linux and macOS:**

    ```bash theme={null}
    ./bin/sdrtrunk
    ```

    SDRTrunk Kennebec opens to the main window. On first launch, the application creates a configuration directory:

    * **Windows:** `%USERPROFILE%\SDRTrunk\`
    * **Linux / macOS:** `~/SDRTrunk/`

    All playlists, recordings, and settings are stored here.


  ### Connect your SDR tuner
    Plug in your SDR device before or after launching the application. SDRTrunk automatically detects most supported hardware on startup.

    Supported hardware families include RTL-SDR dongles, Airspy devices, and HackRF. See [Supported tuners](/hardware/supported-tuners) for the full list.

    To verify your tuner is recognized:

    1. Open the **Tuners** panel from the main toolbar.
    2. Your device should appear in the tuner list with its model name.
    3. If it does not appear, check the [hardware setup guides](/hardware/supported-tuners) for driver and platform-specific instructions.

    > **Warning:**
      On Windows, most RTL-SDR devices require the WinUSB driver to be installed via Zadig before SDRTrunk can access them. See the [RTL-SDR setup guide](/hardware/rtl-sdr) for details.



  ### Open the Playlist Editor and add a channel
    The Playlist Editor is where you configure the radio systems and channels you want to monitor.

    1. Click **Playlist Editor** in the main toolbar to open the editor window.
    2. In the editor, click **New Playlist** and give it a name (for example, the name of your county or system).
    3. Inside your playlist, click **Add Channel**.
    4. Set the **Protocol** to match your radio system (for example, **P25 Phase 1** for a Phase 1 P25 system).
    5. Enter the **Frequency** of the system's control channel in Hz (for example, `851,012,500` for 851.0125 MHz).
    6. Assign your tuner to the channel under the **Source** configuration.
    7. Click **Save**.

    > **Tip:**
      If you are not sure which protocol or frequency your local system uses, check [RadioReference.com](https://www.radioreference.com). SDRTrunk also includes a RadioReference API integration that can import system configurations directly.



  ### Start the channel
    With your channel configured, you are ready to start decoding.

    1. In the Playlist Editor, select the channel you just created.
    2. Click **Enable** (or toggle the channel to the enabled state).
    3. Close the Playlist Editor and watch the **Now Playing** view in the main window.

    When the application locks onto the control channel, it begins tracking talkgroups and decoding voice traffic. Active calls appear in the Now Playing view as they are received.

    > **Info:**
      It may take a few seconds for the application to acquire and lock the control channel. A status indicator in the channel row shows the current decoding state.



## Next steps

Now that you have a channel running, you can expand your setup:

  ### Add talkgroup aliases

    Assign names to talkgroup IDs so calls are labelled with recognizable names instead of raw numbers.


  ### Set up streaming

    Route live audio to Broadcastify, Zello, Icecast, or IamResponding.


  ### Configure alerts

    Receive Telegram or Email notifications for tuner errors and channel inactivity.


  ### Explore hardware options

    Learn about supported SDR devices and how to get the best performance from each.