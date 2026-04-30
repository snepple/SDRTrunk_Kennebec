# Organize talkgroups and radio IDs with aliases and actions

> Create aliases in SDRTrunk Kennebec to label talkgroups and radio IDs with names, highlight colors, and automated actions for easier monitoring.

Aliases let you replace raw numeric talkgroup and radio IDs with meaningful names, colors, and automated actions. Instead of seeing `47001` scroll past in the Now Playing panel, you see `Fire Station 3 Dispatch` highlighted in red. Aliases are stored in named alias lists, and each channel configuration points to one alias list to resolve the IDs it receives.

## What is an alias list?

An alias list is a named collection of aliases. You create at least one alias list and then assign it to one or more channels in the Playlist Editor. When SDRTrunk Kennebec decodes activity on a channel, it looks up the talkgroup or radio ID in the alias list assigned to that channel to find the matching alias name, color, and actions.

You can create multiple alias lists — for example one list per agency or region — and assign the appropriate list to each channel.

## Open the Alias Editor

In the Playlist Editor, click **Aliases** in the left sidebar. The Alias Editor opens with three view modes selectable from the top toolbar: **Alias**, **Identifier**, and **Record**. Use **Alias** view to create and manage aliases directly. Use **Identifier** view to browse aliases sorted by their assigned identifiers.

## Create an alias list

  ### Open the Alias Editor
    Click **Aliases** in the Playlist Editor sidebar.


  ### Add a new alias list
    Click the **New List** button or use the alias list dropdown to type a new list name. Alias list names must be unique within a playlist.


  ### Select the list
    Choose your new list from the dropdown. All aliases you create from this point are added to the selected list.


## Map a talkgroup ID to a name

  ### Select the target alias list
    Use the alias list dropdown at the top of the Alias Editor to choose the list you want to add to.


  ### Create a new alias
    Click **New** in the alias table toolbar. A blank alias row appears.


  ### Set the alias name
    In the **Name** field of the detail editor below the table, type the display name for this talkgroup, such as `Engine 7`.


  ### Add a talkgroup identifier
    Click **Add ID** and select **Talkgroup** from the identifier type menu. Enter the numeric talkgroup ID in the field provided. For trunked systems that use a range, select **Talkgroup Range** and enter the start and end values.


  ### Set a color (optional)
    Click the color swatch to open the color picker and assign a highlight color. This color appears in the Now Playing panel when this alias is active.


  ### Save
    Click **Save**. The alias row in the table updates with the name and any identifier summary.


## Map a radio ID to a name

The process is identical to mapping a talkgroup, except you select **Radio ID** (or **Radio ID Range**) as the identifier type. Radio IDs identify individual subscriber units transmitting on a channel.

> **Note:**
  P25 systems may use **P25 Fully Qualified Radio ID** or **P25 Fully Qualified Talkgroup** identifiers for cross-system calls. Select the appropriate type when monitoring P25 Phase 2 or DFSI systems.

## Supported identifier types

Each alias can have one or more identifiers attached to it. SDRTrunk Kennebec matches an alias when any of its identifiers match the decoded activity.

  ### Talkgroup and Talkgroup Range
    Matches a single talkgroup ID or a contiguous range of talkgroup IDs. Use **Talkgroup Range** to create a single alias entry that covers a block of IDs, for example a fleet of units sharing a range.


  ### Radio ID and Radio ID Range
    Matches a single radio (subscriber unit) ID or a contiguous range. Use this to identify individual portable or mobile units.


  ### P25 Fully Qualified Radio ID / Talkgroup
    Matches P25 fully qualified identifiers that include both a WACN/System ID and a radio or talkgroup ID. Required for cross-system P25 calls where the raw ID alone is not unique.


  ### Audio Broadcast Channel
    Associates this alias with a streaming output channel. When audio for this alias is decoded, SDRTrunk Kennebec routes it to the named broadcast stream. See the Streaming section of the Playlist Editor.


  ### Audio Priority
    Sets a numeric playback priority for this alias. Lower numbers have higher priority. When multiple talkgroups are active simultaneously, SDRTrunk Kennebec uses priority to determine which audio to play on the local speaker.


  ### Record
    Marks audio for this alias for local recording. Any call matching this alias is saved to a WAV or MP3 file in your configured recordings directory.


  ### Two Tone Paging
    Links this alias to a Two Tone detector configuration by name. When the detector fires, the call is attributed to this alias.


  ### CTCSS / DCS
    Matches calls that include a specific Continuous Tone-Coded Squelch (CTCSS) or Digital Coded Squelch (DCS) code. Useful for conventional FM channels shared between multiple groups.


  ### User Status / Unit Status
    Matches P25 status update messages sent by subscriber units. Use these to label status codes with human-readable meanings.


## Alias actions

In addition to a name and color, each alias can trigger one or more automated actions when a matching call is decoded.

  ### Beep
    Plays a short audible beep through the system speaker when a call matching this alias begins. Useful as a heads-up alert for high-priority talkgroups.

    To add a beep action, click **Add Action** in the alias detail editor and select **Beep**.


  ### Play Clip
    Plays a custom audio file (WAV or MP3) when a call matching this alias begins. You specify the path to the clip file in the action editor. Use this to play a distinctive sound for specific talkgroups.

    To add a clip action, click **Add Action** and select **Play Clip**, then browse to the audio file.


  ### Run Script
    Executes an external script or command when a call matching this alias begins. The script receives call metadata as arguments. Use this for integrations such as home automation triggers or custom logging.

    To add a script action, click **Add Action** and select **Run Script**, then enter the full path to the script.


> **Warning:**
  The **Run Script** action executes with the same OS privileges as SDRTrunk Kennebec. Only point it at scripts you control and trust.

## Alias groups and icons

You can assign each alias an optional **Group** tag to categorize aliases within a list (for example, `Fire` or `Law Enforcement`). The group tag is a free-text string and is used for visual filtering in the Alias Editor.

You can also assign an **Icon** to each alias. SDRTrunk Kennebec displays the icon alongside the alias name in the Now Playing panel. Icons are managed through **View → Icon Manager** in the Playlist Editor.

## Additional alias properties

| Property                | Description                                                                                       |
| ----------------------- | ------------------------------------------------------------------------------------------------- |
| **Audio output device** | Route audio for this alias to a specific output device, overriding the global setting.            |
| **Stream as talkgroup** | When streaming, substitute a different talkgroup value in the metadata sent to the stream server. |