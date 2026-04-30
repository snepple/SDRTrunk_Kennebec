# SDRTrunk Kennebec user preferences complete reference

> Complete guide to the consolidated User Preferences panel in SDRTrunk Kennebec, covering all settings categories, options, and defaults.

SDRTrunk Kennebec consolidates every application setting into a single **User Preferences** panel so you never have to hunt across multiple menus to configure the application. From memory allocation to talkgroup display formats, all options live in one place and take effect without restarting the application unless otherwise noted.

## Opening User Preferences

  ### Open the menu
    In the main application window, open the **View** menu in the menu bar.


  ### Select Preferences
    Click **User Preferences**. The preferences panel opens in a new window.


  ### Choose a category
    Select any item in the left sidebar to display that category's settings in the right pane. Section headers (such as **Application**, **Audio**, and **Decoder**) are not clickable — they are grouping labels only.


> **Tip:**
  The preferences panel remembers which category you last viewed and opens to it automatically on subsequent launches.

***

## Sidebar categories

The sidebar is organized into labeled sections. Each section contains one or more clickable preference pages.

  ### Application
    The **Application** section contains three preference pages.

    **Application** — General application settings:

    | Setting                         | Description                                                                                                                                                    |
    | ------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
    | Channel auto-start timeout      | Countdown in seconds before a channel starts automatically. Default: `10` seconds.                                                                             |
    | Automatic diagnostic monitoring | When enabled, the application continuously monitors channel health and flags issues. Default: on.                                                              |
    | Allocated memory                | Sets the JVM heap size in GB. The value is written to `~/SDRTrunk/SDRTrunk.memory` and takes effect the next time you launch the application. Default: `6` GB. |

    > **Note:**
      The **Allocated memory** field lets you change the JVM heap directly from the GUI without editing launch scripts. The new value takes effect on the next application restart.


    **Diagnostics (Logging)** — Runtime per-category DEBUG log toggles. See [Enable diagnostics and debug logging](/configuration/diagnostics) for full details.

    **MQTT** — Publish radio events to an MQTT broker for home automation or monitoring. See [Connect SDRTrunk Kennebec to an MQTT broker](/configuration/mqtt) for full details.


  ### Audio
    **Call Management** — Duplicate call detection and how the application handles overlapping or repeated transmissions.

    **MP3** — Controls the quality and bit rate settings used when encoding audio to MP3 format for recordings or streaming.

    **Playback / Tones** — Configures the audio output device, volume, and alert tones used for local monitoring playback.

    **Record** — Sets the file format, sample rate, and storage behavior for audio recordings.


  ### CPU
    **Vector Calibration** — Runs a benchmark to detect the fastest SIMD instruction set (AVX-512, AVX2, SSE, or scalar) available on your CPU and stores the result so the DSP engine uses it automatically. You can re-run calibration after a hardware change.


  ### Decoder
    **JMBE Audio Library** — Points the application to your JMBE library JAR file. JMBE is required to decode IMBE and AMBE+ vocoder audio used in P25 Phase 1, DMR, and other digital protocols. Without JMBE, voice traffic is decoded structurally but produces no audio output.


  ### Display
    **Channel Events** — Controls which event types appear in the channel events table and how they are formatted.

    **Talkgroup & Radio ID** — Sets the display format for talkgroup and radio identifiers throughout the application. Options include decimal, hexadecimal, and formatted representations. See [Talkgroup format](#talkgroup--radio-id-format) below for details.


  ### File Storage
    **Directories** — Overrides the default storage locations for recordings, playlists, and event logs. By default, all files go under `~/SDRTrunk/`. Change any path here to redirect storage to a different drive or folder.


  ### Source
    **Tuners** — Tuner-specific settings including PPM correction, bandwidth, and gain configuration for each connected SDR device.


  ### Icons
    **Icon Manager** — Manage the icon sets used to represent talkgroups and aliases in the Now Playing view and channel tables.


  ### AI
    **AI Settings** — Configures the optional Gemini AI integration. When enabled, AI can auto-set channel filters, review logs, monitor application performance, assess audio quality, and alert you if audio becomes unintelligible. Requires a valid Gemini API key.


***

## Talkgroup & Radio ID format

The **Talkgroup & Radio ID** preference page controls how numeric identifiers appear everywhere in the application — in the Now Playing view, channel event tables, and alias lists.

  ### Decimal
    Displays talkgroup and radio IDs as plain integers. This is the most common format and matches the values shown on most RadioReference system pages.

    Example: `1234`


  ### Hexadecimal
    Displays identifiers in base-16. Useful when working with systems where IDs are typically expressed in hex (some DMR and P25 Phase 2 systems).

    Example: `0x4D2`


  ### Formatted
    Applies a system-specific display format where the protocol defines one. P25 talkgroup IDs, for example, can be displayed with agency and group separators.


***

## Allocated memory

SDRTrunk Kennebec is a Java application and requires you to pre-allocate the maximum JVM heap at startup. The **Allocated memory** field in **Application** preferences lets you set this value in gigabytes without editing any launch scripts or configuration files.

When you save a new value, the application writes it to `~/SDRTrunk/SDRTrunk.memory`. The launcher reads this file at startup to set the `-Xmx` flag.

> **Warning:**
  Memory changes do not take effect until you restart the application. Set the value before starting a monitoring session.

Recommended starting points:

| Usage                                              | Suggested allocation |
| -------------------------------------------------- | -------------------- |
| Light (1–2 channels, no streaming)                 | 4 GB                 |
| Typical (5–10 channels, 1–2 streams)               | 6 GB (default)       |
| Heavy (20+ channels, multiple streams, AI enabled) | 8–12 GB              |

***

## OS theme syncing

SDRTrunk Kennebec uses JNA-based desktop integration to detect your operating system's light or dark theme and apply a matching appearance automatically. No preference setting is required — the application reads the OS theme at startup and adjusts accordingly.

> **Info:**
  If you change your OS theme while the application is running, restart SDRTrunk Kennebec to pick up the new appearance.