# Manage channels and systems with the Playlist Editor

> Create and manage channels, sites, and systems in the SDRTrunk Kennebec Playlist Editor for trunked and conventional radio monitoring.

The Playlist Editor is the central hub for configuring everything SDRTrunk Kennebec monitors. From here you create and organize playlists, define radio systems and sites, add individual channels, and set each channel's decoding protocol and startup behavior. Changes you make in the editor are saved automatically to the active playlist file.

## Open the Playlist Editor

Select **View → Playlist Editor** from the main application menu bar. The editor opens in a split-pane window. The left sidebar lists the available sections: **Playlists**, **Channels**, **Aliases**, **Streaming**, **Radio Reference**, and **Two Tones**. Click any item in the sidebar to switch to that section.

## Create a new playlist

A playlist is an XML file that stores all your channel, alias, and streaming configurations. SDRTrunk Kennebec loads one playlist at a time.

  ### Open the Playlists section
    In the Playlist Editor sidebar, click **Playlists**. The right pane shows a list of all playlist files SDRTrunk Kennebec knows about.


  ### Create the playlist file
    Click **New** in the action bar at the bottom of the list. A file-save dialog appears. Choose a folder and filename, then click **Save**. The new playlist appears in the list.


  ### Set it as the active playlist
    Select the new playlist in the list and click **Select**. SDRTrunk Kennebec switches to this playlist immediately. The title bar of the editor reflects the active playlist filename.


> **Tip:**
  You can maintain separate playlists for different locations or use cases and switch between them without losing your configurations. Use **Clone** to copy an existing playlist as a starting point.

## Add systems, sites, and channels

SDRTrunk Kennebec organizes channels in a three-level hierarchy: **System → Site → Channel**. A system represents a radio network (for example, a county P25 trunking system). A site is a tower or repeater within that system. A channel is a specific frequency or logical traffic channel to decode.

  ### Navigate to the Channels section
    Click **Channels** in the sidebar. The upper pane shows a table of all configured channels across all systems and sites.


  ### Create a new channel
    Click **New** in the top-right toolbar. A dropdown menu appears listing the supported decoder types. Select the protocol that matches the system you want to monitor (for example, **P25 Phase 1**, **DMR**, or **NBFM**). A new channel entry appears in the table and the detail editor opens in the lower pane.


  ### Fill in the channel details
    In the detail editor, complete the fields described in the [channel configuration fields](#channel-configuration-fields) section below. At minimum, set the **System**, **Name**, and **Frequency** fields.


  ### Save the channel
    Click **Save** in the detail editor. The channel row in the table updates immediately.


> **Note:**
  The system and site fields are free-text labels. Use consistent naming across channels that belong to the same logical system so they sort and filter together in the Channels table.

## Channel configuration fields

The lower half of the Channels section is the detail editor. Its fields change depending on the decoder type you selected when creating the channel, but the following fields are present for all channel types.

  ### Name
    A human-readable label for this channel. The name appears in the Channels table, the Now Playing panel, and in recorded audio filenames. Choose a short, descriptive name such as `Fire Dispatch` or `PD Car-to-Car`.


  ### System
    The name of the radio system this channel belongs to. This is a free-text field used for grouping and filtering. Example: `County EMS` or `City P25`.


  ### Site
    The name of the site (tower or repeater) this channel is associated with. Example: `North Tower` or `Site 001`. For conventional channels with no site structure, you can leave this blank or set it to the same value as System.


  ### Frequency
    The receive frequency in MHz for this channel. For trunked systems this is the control channel frequency. For conventional channels this is the channel's fixed frequency.


  ### Protocol / Decoder
    The decoder type selected when you created the channel. This determines which signal processing pipeline SDRTrunk Kennebec applies. Common options include **P25 Phase 1**, **P25 Phase 2**, **DMR**, **LTR**, **MPT-1327**, **NBFM**, and **AM**. You cannot change the decoder type after creation; delete and recreate the channel if you need a different protocol.


  ### Alias list
    The alias list to use for this channel. Talkgroup and radio ID lookups use the aliases defined in the list you select here. See [Organize talkgroups and radio IDs with aliases](/channels/aliases-talkgroups) for details on creating alias lists.


  ### Auto-Start
    When enabled, SDRTrunk Kennebec starts decoding this channel automatically each time the application launches. Channels without Auto-Start enabled must be started manually. The Channels table shows a checkmark in the **Auto-Start** column for any channel with this setting on.


> **Warning:**
  Enabling Auto-Start on a large number of channels simultaneously increases CPU and SDR tuner load at startup. Start with the channels you monitor most frequently and enable Auto-Start selectively.

## Filter and sort the Channels table

The Channels table supports live filtering and persistent sort order.

**Filter by view:** The **All / Playing / Auto-Start** segmented control at the top of the Channels section narrows the list to all channels, only those currently decoding, or only those with Auto-Start enabled.

**Text search:** The **Search** field filters channels by system, site, name, or protocol as you type. Clear the field to restore the full list.

**Column sort:** Click any column header to sort by that column. Click again to reverse the sort direction. Your chosen sort column, sort direction, and column widths persist across application restarts.

> **Tip:**
  Double-click a channel row to immediately start or stop decoding that channel without opening its detail editor.

## Import from RadioReference

If you have a RadioReference.com account, you can import systems and talkgroups directly rather than entering them by hand.

  ### Open the Radio Reference section
    Click **Radio Reference** in the Playlist Editor sidebar.


  ### Enter your credentials
    Enter your RadioReference.com username and password in the fields provided. Click **Login**.


  ### Browse and import
    Navigate to your state, county, and system. Select the talkgroups or channels you want to import and click **Import**. SDRTrunk Kennebec creates the channels and aliases in your active playlist automatically.


> **Note:**
  A RadioReference.com Premium subscription is required to use the API import feature.