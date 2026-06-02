# What's New in SDRTrunk Kennebec

Welcome to the **SDRTrunk Kennebec** release! This major update brings a stunning premium design overhaul, a professional image upload pipeline, and automated hardware self-healing features. We have fine-tuned every layout, scroll pane, and color tone according to professional **Apple Human Interface Guidelines (HIG)** to make SDRTrunk Kennebec look and feel like a modern, native, state-of-the-art desktop app.

---

## 1. Always-Visible Sticky Audio Control Header
*   **Sticky Playback Bar**: The top iTunes-style playback controller (`AudioPanel`) is now completely locked to the upper boundary of the right content pane. It remains anchor-visible at all times, ensuring you can adjust volume, mute, or inspect active calls regardless of which settings page or editor tab you are browsing.
*   **Translucent glassmorphism**: Individual channel card boxes (`AudioChannelPanel`) now use a beautiful translucent dark style (`rgba(255,255,255,0.06)`) with subtle white borders and rounded corners, blending perfectly into the primary dark system background.
*   **Elite Mute Pill Capsule**: The master mute button has been redesigned into a bold, highly visible pill-shaped capsule (`🔊 Mute` in translucent gray / `🔇 Muted` in warning-red). Standard mute indicators have been removed from individual channels to eliminate redundant UI noise.

---

## 2. Channel Image & Custom Artwork Pipeline
*   **Unique Channel Graphics**: You can now upload custom images (logos, emblem badges, dispatcher photos) for any of your active channels!
*   **Smart Upload & Validation**:
    *   **Resolution Safety Check**: The upload dialogue verifies image dimensions. Any image smaller than `64x64` pixels is cleanly rejected with a helpful dialog error explaining that it does not meet the minimum resolution required for a sharp display.
    *   **Automatic Optimization**: High-resolution source images are automatically processed on a fast background thread, scaled down to `128x128` pixels using high-quality bilinear filtering, optimized as clean PNGs, and saved to a local persistent folder.
*   **Dynamic Playback Artwork**: When a channel is actively decoding, its custom image is displayed inside a beautiful circular squircle on the sticky top playback bar. When the channel goes idle, the squircle elegantly resets to a default waveform gradient.

---

## 3. Natural Scrolling & Viewport Layout Bounds
*   **Scroll-Lock Elimination**: Fixed common viewport cropping issues where forms would expand beyond the screen edge on lower-resolution monitors.
*   **Adaptive Sidebars**: The main category list under the User Preferences panel is now shrinkable and automatically shows standard scrollbars if the window height is constrained.
*   **ScrollPane Ingestion**: Every single preference editor panel now resides inside a transparent scrolling viewport, guaranteeing that long configuration sheets (such as audio, network, and directory settings) are fully accessible and never cropped.
*   **Records Table Elasticity**: The recordings catalog table in `AudioRecordingsPanel` now scales naturally to fill the bottom viewport and expands dynamically when window dimensions are adjusted.

---

## 4. Reorganized Two-Tone Alerting Panel
*   **Tabbed Base Configuration**: The Two-Tone alert dispatcher configurations have been grouped into four dedicated tabs:
    1.  **General Setup** (Frequencies, Tone Fills, Aliases)
    2.  **Zello Integration**
    3.  **MQTT Integration**
    4.  **Aliases**
*   **Persistent Top Toolbar**: The "Save Settings" button has been elevated to a persistent top header bar, keeping it visible and accessible at all times while browsing across integration tabs.
*   **DoubleProperty Live Bindings**: Tone frequencies in the Two-Tone table are now bound to the underlying model using JavaFX `DoubleProperty` wrappers. Changing A or B tone frequencies updates the parent overview table instantly.
*   **Combobox Auto-filtering**: Re-engineered Tone dropdown selection lists to prevent autocompletion programmatic text insertion from clearing the default list models, eliminating the blank dropdown bug.

---

## 5. Vibrant Streaming Connection Console
*   **High-Contrast Color Indicators**: Replaced hard-to-read, disabled-looking gray text cells in the streaming list. Active streams now feature vibrant status indicators:
    *   `Connected`: Vibrant Apple-style green (`#34C759`)
    *   `Connecting`: Radiant system orange (`#FF9500`)
    *   `Errors & Warnings`: High-intensity warning red (`#FF3B30`)
    *   `Disabled`: Classic muted gray (`#8E8E93`)
*   **Show Enabled Streams Filter**: Added a quick "Show Enabled Only" checkbox to the streaming toolbar. Toggle it to instantly filter the list and focus on active feeds.
*   **Rich Right-Click Context Menus**: Row context menus let you quickly toggle the stream status (`Enable` / `Disable`), trigger a manual `Reconnect`, or click `Configure` to jump directly into the feed properties card.
*   **SortedList Sorting & drag Column Reordering**: Table columns can now be reordered by dragging them horizontally. In addition, the filtered list supports standard column header click-to-sort natively.

---

## 6. Tuner Waterfall Collapse & Watchdog Self-Healing
*   **Zero-Height Tuner Collapse**: In the tuners panel, disabling the spectrum waterfall completely collapses the layout's upper space (`height = 0`, `visible = false`, `managed = false`). The bottom channels table automatically expands to fill the entire remaining vertical space, optimizing your monitor's real estate.
*   **USB Recovery Watchdog script (`usb_monitor.ps1`)**:
    *   An elite, highly-privileged system task that actively monitors USB hub connection states for RTL-SDR and HackRF controllers.
    *   If a hardware lockup or thread crash is detected, the script uses Windows highest execution parameters to silently reset the physical USB ports, reviving frozen tuners automatically in the background without needing a system reboot.
