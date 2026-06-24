# Streaming Connection Console

## Goal
Learn how to monitor, filter, and manage your live audio streams using the vibrant Streaming Connection Console.

The Streaming Connection Console is your dashboard for all outgoing audio integrations (like Zello, OpenMHz, or Rdio Scanner). It provides high-contrast visual feedback so you know exactly which streams are online and which need attention.

## Component Map

* **Status Indicators:** Color-coded cells showing the real-time state of each stream.
* **Show Enabled Only Checkbox:** A filter toggle in the top toolbar to hide disabled configurations.
* **Context Menu:** Right-click any stream to Enable/Disable, Reconnect, or Configure.
* **Draggable Column Headers:** Click and drag columns to rearrange the table layout to your preference.

## Understanding Status Colors

| Color | Status | What it means |
|---|---|---|
| 🟢 **Green** | `Connected` | The stream is live and successfully transmitting audio. |
| 🟠 **Orange** | `Connecting` | The application is currently trying to establish a connection to the server. |
| 🔴 **Red** | `Errors & Warnings` | The stream failed to connect (e.g., wrong password, server down). |
| ⚪ **Gray** | `Disabled` | The stream is manually turned off and not attempting to connect. |

## How to manage streams

1. **Filter the List:** Check the **Show Enabled Only** box in the toolbar to focus only on active streams.
2. **Force Reconnect:** If a stream is stuck in an error state, right-click the row and select **Reconnect**.
3. **Edit Settings:** Right-click a stream and select **Configure** to jump directly into its properties card to fix a typo or change an API key.
