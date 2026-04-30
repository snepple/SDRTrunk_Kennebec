# Review and manage audio recordings in the Recordings panel

> Access, play back, and manage recorded audio files in SDRTrunk Kennebec's Audio Recordings panel with date, alias, and channel filters.

SDRTrunk Kennebec can save decoded audio to disk as individual files, one file per call or transmission. The Audio Recordings panel gives you a searchable, sortable table of every recording in your recordings directory, with built-in playback so you can review calls without leaving the application.

## Enable recording

Recording is controlled at the alias level. A call is recorded when the talkgroup or radio ID matches an alias that has recording enabled.

  ### Open the Alias Editor
    In the Playlist Editor, click **Aliases** in the sidebar.


  ### Select the alias to record
    Find the alias for the talkgroup or radio ID you want to record and select it.


  ### Add a Record identifier
    In the alias detail editor, click **Add ID** and select **Record**. This marks the alias as recordable. Click **Save**.


> **Tip:**
  You can also use the **Record** view mode in the Alias Editor (the toggle button at the top) to see all aliases that currently have recording enabled and manage them in bulk.

> **Note:**
  Recording only captures calls on channels that are actively decoding. Make sure the channel is started (or set to Auto-Start) before expecting recordings to appear.

## Set the recordings directory

SDRTrunk Kennebec writes recording files to the directory you specify in User Preferences.

  ### Open User Preferences
    Select **View → User Preferences** from the Playlist Editor menu bar, or use the keyboard shortcut **Alt+U**.


  ### Navigate to the Directory settings
    In the preferences panel, select the **Directory** section.


  ### Set the recording path
    Click the folder icon next to the **Recordings** path field and choose the directory where you want recording files saved. Click **OK** to confirm.


## Recording formats

SDRTrunk Kennebec supports two audio recording formats.

  ### WAV
    Standard uncompressed PCM audio. WAV files are larger than MP3 but are universally compatible and require no decoding overhead. The file extension is `.wav`.


  ### MP3
    Compressed audio using the MP3 codec. MP3 files are significantly smaller than WAV at the cost of a small amount of audio quality. The file extension is `.mp3`.


Select your preferred format in **User Preferences → Audio → Recording Format**.

## Recording filename format

Each recording file is named using call metadata so you can identify a recording without opening it. The naming convention is:

```
YYYYMMDD_HHMMSS_<system>_<site>_<channel>_TO_<to-alias>_FROM_<from-alias>.<ext>
```

For example:

```
20260430_143022_County_EMS_North_Site_Dispatch_TO_Engine_7_FROM_Mobile_12.wav
```

The Audio Recordings panel parses this filename to populate the **Date**, **Time**, **Channel**, **To Alias**, and **From Alias** columns in the table.

## Open the Audio Recordings panel

The Audio Recordings panel is accessible from the main application window.

Select **View → Audio Recordings** from the main menu bar. The panel opens as a tab or docked pane depending on your layout settings.

## The Audio Recordings table

The table shows all recording files found in your configured recordings directory. Each row represents one file.

| Column         | Description                                                          |
| -------------- | -------------------------------------------------------------------- |
| **Date**       | Recording date in `YYYY-MM-DD` format, parsed from the filename.     |
| **Time**       | Recording start time in `HH:MM:SS` format, parsed from the filename. |
| **Channel**    | System and channel name, parsed from the filename.                   |
| **To Alias**   | The talkgroup or destination alias name, parsed from the filename.   |
| **From Alias** | The transmitting radio alias name, parsed from the filename.         |
| **Size**       | File size in MB.                                                     |
| **Action**     | A **Play** button to start in-app playback of this recording.        |

Click any column header to sort the table by that column. Click again to reverse the sort direction. The sort order and column widths persist across sessions.

## Filter recordings

The filter bar above the table lets you narrow the list without deleting any files.

  ### Date range filter
    Use the **Start Date** and **End Date** date pickers to show only recordings from a specific date range. Leave either field blank to apply no lower or upper bound on the date.


  ### Time range filter
    Use the **Start** and **End** hour and minute spinners to filter by time of day. The time range can wrap midnight — for example, setting Start to `22:00` and End to `06:00` shows overnight recordings.


  ### Alias filter
    Open the **Filter Alias** dropdown to show only recordings where the To Alias or From Alias matches the selected alias name. Select **All** to clear the alias filter.


  ### Channel filter
    Open the **Filter Channel** dropdown to show only recordings from a specific channel. Select **All** to show recordings from all channels.


## Play back a recording

  ### Locate the recording
    Use the date, time, alias, and channel filters to find the recording you want to review.


  ### Click Play
    Click the **Play** button in the **Action** column for that row. SDRTrunk Kennebec starts playback through your default audio output device. The **Stop Playback** button in the filter bar becomes active.


  ### Stop playback
    Click **Stop Playback** to end playback before the file finishes, or wait for the file to play to completion.


> **Note:**
  Only one recording can play at a time. Starting playback of a new file automatically stops any recording currently playing.

## Refresh the recordings list

Click **Refresh** in the filter bar to rescan the recordings directory and reload the table. Use this after SDRTrunk Kennebec has been running and capturing new recordings, or after you have added or removed files outside the application.

> **Warning:**
  Deleting recording files must be done outside SDRTrunk Kennebec using your operating system's file manager. The Audio Recordings panel does not have a built-in delete function.