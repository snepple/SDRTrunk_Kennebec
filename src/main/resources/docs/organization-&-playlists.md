# Organize radio systems and channels with playlists

A playlist is the top-level organizational unit in SDRTrunk Kennebec. Everything you configure — channels, aliases, streaming outputs, and tone detectors — lives inside a playlist file. SDRTrunk loads one playlist at a time, and switching playlists instantly changes the entire set of channels and aliases in use. This makes playlists the right tool for separating unrelated monitoring setups, such as different geographic areas or different use cases.

## What a playlist contains

| Item | Description |
| --- | --- |
| **Channels** | Every channel you monitor, each with its protocol, frequency, and decoder settings. |
| **Aliases** | Named alias lists that map talkgroup IDs and radio IDs to display names, colors, and automated actions. |
| **Streaming** | Audio stream output configurations (Broadcastify, Icecast, etc.). |
| **Two Tones** | Two-tone sequential paging detector configurations. |

## Playlist file location

SDRTrunk Kennebec stores playlist files as XML on disk. The default location is inside the SDRTrunk application data directory:

```
~/.sdrtrunk/playlist/
```

You can place playlist files anywhere on your system — SDRTrunk tracks the file path, not a fixed location. Use descriptive filenames to distinguish playlists, for example `county-fire.xml` or `aviation-monitoring.xml`.

## Create a new playlist

**1. Open the Playlist Editor**

Select **View → Playlist Editor** from the main menu.

**2. Navigate to Playlists**

Click **Playlists** in the left sidebar. The right pane lists all playlist files SDRTrunk Kennebec currently knows about.

**3. Create the file**

Click **New** in the action bar at the bottom of the list. A file-save dialog appears. Choose a folder and filename, then click **Save**. The new playlist appears in the list.

**4. Set it as the active playlist**

Select the new playlist in the list and click **Select**. SDRTrunk Kennebec switches to this playlist immediately. The title bar of the editor reflects the active playlist filename.


> [!TIP]
> Use **Clone** to copy an existing playlist as a starting point for a new one. This is faster than rebuilding channels and aliases from scratch when you want a variation of an existing setup.

## Channel hierarchy within a playlist

SDRTrunk Kennebec organizes channels in a three-level hierarchy: **System → Site → Channel**.

- **System** — the radio network, for example `County P25` or `City Fire`. This is a free-text label you define.
- **Site** — a tower or repeater within the system, for example `North Tower` or `Site 001`. Leave blank for simple conventional channels.
- **Channel** — the individual frequency or logical traffic channel to decode.

Use consistent naming across channels that belong to the same logical system so they sort and filter together in the Channels table.

## Manage aliases within a playlist

Each playlist has its own set of alias lists. Alias lists are scoped to the playlist — they do not transfer automatically when you switch playlists or share a playlist file. When you assign a channel an alias list, it references the list by name within the same playlist.

See [Aliases & talkgroups](/decoding/aliases-talkgroups) for full details on creating and managing aliases.

## Manage streaming outputs

The **Streaming** section of the **Playlist Editor** lets you configure audio broadcast destinations such as Broadcastify or a local Icecast server. Each streaming configuration is stored in the playlist. Channels and aliases can route audio to a named stream via the **Audio Broadcast Channel** alias identifier.

## Import from RadioReference

If you have a RadioReference.com Premium subscription, you can import systems and talkgroups directly into the active playlist rather than entering them manually.

**5. Open the Radio Reference section**

Click **Radio Reference** in the **Playlist Editor** sidebar.

**6. Enter your credentials**

Enter your RadioReference.com username and password, then click **Login**.

**7. Browse and import**

Navigate to your state, county, and target system. Select the talkgroups or channels you want to import and click **Import**. SDRTrunk Kennebec creates the channels and aliases in the active playlist automatically.


> [!NOTE]
> A RadioReference.com **Premium** subscription is required to use the API import feature.

## Switch between playlists

To switch to a different playlist, open the **Playlist Editor**, click **Playlists** in the sidebar, select the target playlist file in the list, and click **Select**. SDRTrunk Kennebec stops all currently running channels, unloads the old playlist, and loads the selected one. You can then start channels from the new playlist manually or rely on their **Auto-Start** settings.

> [!WARNING]
> Switching playlists stops all active decoding. If you are recording or streaming, make sure those sessions are concluded before switching.
