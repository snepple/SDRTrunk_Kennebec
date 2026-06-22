# Vibrant Streaming Connection Console

## Goal

The streaming list console has been completely redesigned with high-contrast color indicators and advanced sorting tools, providing an elite overview of all active broadcast connections.

## UI Component Map

*   **Status Indicators**:
    *   `Connected`: Vibrant Apple-style green (`#34C759`)
    *   `Connecting`: Radiant system orange (`#FF9500`)
    *   `Errors & Warnings`: High-intensity warning red (`#FF3B30`)
    *   `Disabled`: Classic muted gray (`#8E8E93`)
*   **Show Enabled Streams Filter**: A "Show Enabled Only" checkbox on the top toolbar instantly hides disabled feeds, decluttering the list.
*   **Drag Column Reordering**: Click and drag any table header (like "Status" or "Name") horizontally to reorder the data columns to your preference.
*   **Native Column Sorting**: Click any header to instantly sort the list alphabetically or by connection status.

## Step-by-Step

1. Open the Streaming Console.
2. Right-click on any row to open a rich context menu with quick actions:

*   **Enable / Disable**: Instantly toggle the broadcast status without opening the configuration sheet.
*   **Reconnect**: Force an immediate manual reconnection attempt (useful for resolving transient server timeouts).
*   **Configure**: Jump directly into the feed's detail properties card.
