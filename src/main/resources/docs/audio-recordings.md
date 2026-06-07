# Browse, Filter, and Play Back Audio Recordings

SDRTrunk Kennebec can save decoded audio to disk as individual files, one file per call or transmission. The **Audio Recordings** panel gives you a searchable, sortable table of every recording in your recordings directory, with built-in playback so you can review calls without leaving the application.

## Open the Audio Recordings panel

Select **View → Audio Recordings** from the main menu bar. The panel opens as a tab or docked pane depending on your layout settings.

---

## Enable recording for a talkgroup or radio

Recording is controlled at the alias level. A call is recorded when the talkgroup or radio ID matches an alias that has recording enabled.

**1. Open the Alias Editor**

In the Playlist Editor, click **Aliases** in the sidebar.

**2. Select the alias to record**

Find the alias for the talkgroup or radio ID you want to record and select it.

**3. Add a Record identifier**

In the alias detail editor, click **Add ID** and select **Record**. This marks the alias as recordable. Click **Save**.


> [!TIP]
> Use the **Record** view mode in the Alias Editor (the toggle button at the top) to see all aliases that currently have recording enabled and manage them in bulk.

> [!NOTE]
> Recording only captures calls on channels that are actively decoding. Make sure the channel is started (or set to Auto-Start) before expecting recordings to appear.

---

## Set the recordings directory

SDRTrunk Kennebec writes recording files to the directory you specify in **User Preferences**.

**4. Open User Preferences**

Select **View → User Preferences** from the menu bar, or use the keyboard shortcut **Alt+U**.

**5. Navigate to Directory settings**

In the preferences panel, select **File Storage → Directories**.

**6. Set the recording path**

Click the folder icon next to the **Recordings** path field and choose the directory where you want recording files saved. Click **OK** to confirm.


---

## Recording formats

SDRTrunk Kennebec supports two audio recording formats. Select your preferred format in **User Preferences → Audio → Record**.

| Format | Description | Default |
| --- | --- | --- |
| **MP3** | Compressed audio. Significantly smaller files at the cost of a small reduction in audio quality. Extension: `.mp3` | Yes |
| **WAV** | Uncompressed PCM audio. Universally compatible and requires no decoding overhead, but files are larger. Extension: `.wav` | No |

---

## Recording filename format

Each recording file is named using call metadata so you can identify a recording without opening it:

```
YYYYMMDD_HHMMSS_<system>_<site>_<channel>_TO_<to-alias>_FROM_<from-alias>.<ext>
```

For example:

```
20260430_143022_County_EMS_North_Site_Dispatch_TO_Engine_7_FROM_Mobile_12.wav
```

The Audio Recordings panel parses this filename to populate the **Date**, **Time**, **Channel**, **To Alias**, and **From Alias** columns in the table automatically.

---

## The recordings table

The table shows all recording files found in your configured recordings directory. Each row represents one file.

| Column | Description |
| --- | --- |
| **Date** | Recording date in `YYYY-MM-DD` format, parsed from the filename. |
| **Time** | Recording start time in `HH:MM:SS` format, parsed from the filename. |
| **Channel** | System and channel name, parsed from the filename. |
| **To Alias** | The talkgroup or destination alias name, parsed from the filename. |
| **From Alias** | The transmitting radio alias name, parsed from the filename. |
| **Size** | File size in MB. |
| **Action** | A **Play** button to start in-app playback of this recording. |

Click any column header to sort the table by that column. Click again to reverse the sort direction. Sort order and column widths persist across sessions.

---

## Filter recordings

The filter bar above the table lets you narrow the list without deleting any files.

**Date range filter**

Use the **Start Date** and **End Date** date pickers to show only recordings from a specific date range. Leave either field blank to apply no lower or upper bound on the date.

**Time range filter**

Use the **Start** and **End** hour and minute spinners to filter by time of day. The time range can wrap midnight — for example, setting Start to `22:00` and End to `06:00` shows overnight recordings.

**Alias filter**

Open the **Filter Alias** dropdown to show only recordings where the To Alias or From Alias matches the selected alias name. Select **All** to clear the alias filter.

**Channel filter**

Open the **Filter Channel** dropdown to show only recordings from a specific channel. Select **All** to show recordings from all channels.


---

## Play back a recording

**7. Locate the recording**

Use the date, time, alias, and channel filters to find the recording you want to review.

**8. Click Play**

Click the **Play** button in the **Action** column for that row. SDRTrunk Kennebec starts playback through your default audio output device. The **Stop Playback** button in the filter bar becomes active.

**9. Stop playback**

Click **Stop Playback** to end playback before the file finishes, or wait for the file to play to completion.


> [!NOTE]
> Only one recording can play at a time. Starting playback of a new file automatically stops any recording currently playing.

---

## Delete recordings

Select one or more rows in the table and click **Delete Selected** to permanently remove those files. To remove all recordings currently visible in the filtered view, click **Delete All**. Both actions display a confirmation dialog before deleting.

> [!WARNING]
> Deletions are permanent and cannot be undone. Use filters to narrow the table before clicking **Delete All** to avoid removing recordings you want to keep.

---

## Refresh the recordings list

Click **Refresh** in the filter bar to rescan the recordings directory and reload the table. Use this after SDRTrunk Kennebec has been running and capturing new recordings, or after you have added files outside the application.
