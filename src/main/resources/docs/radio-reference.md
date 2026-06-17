# Import Radio Systems from RadioReference.com

RadioReference.com is the largest public database of radio system information in North America, containing frequencies, talkgroup IDs, site coordinates, and system metadata for thousands of public safety and government networks. SDRTrunk Kennebec's RadioReference integration lets you browse this database by location and pull a complete system configuration — channels, sites, and aliases — directly into your active playlist in seconds, without entering anything manually.

> **Note:**
> A RadioReference.com **Premium** subscription is required to use the API import feature. Free accounts cannot access the API.

---

## Quick start: import a system

**1. Open the RadioReference panel**

In the Playlist Editor, click **Radio Reference** in the left sidebar.

**2. Authenticate your account**

Enter your RadioReference.com username and password in the credentials section, then click **Login**.

**3. Browse for your system**

Use the cascading dropdown menus to locate the system you want to import:
1. Select your **Country**.
2. Select your **State/Province**.
3. Select your **County**.
4. Select the specific **System** you want to monitor.

**4. Select components and import**

Check the boxes next to the sites and talkgroup categories you want to include. Click **Import Selected**. SDRTrunk Kennebec automatically creates the necessary channels and alias lists in your active playlist.

---

## What gets imported

When you import a system, SDRTrunk Kennebec generates the following in your active playlist:

| Imported item | Description |
| --- | --- |
| **Channels** | One channel per selected site, pre-configured with the correct decoder protocol (P25 Phase 1, P25 Phase 2, etc.) and control channel frequency. |
| **Alias groups** | One alias group per talkgroup category selected during import (e.g., "Law Enforcement", "Fire / EMS"). |
| **Talkgroup aliases** | Individual aliases for each talkgroup ID in the selected categories, named from the RadioReference database. |
| **System metadata** | System name and site names are applied to channel names for easy identification. |

Encrypted talkgroups are imported with a **Do Not Monitor** priority by default, so they appear in your alias list but are not decoded. You can change this behavior in **User Preferences → RadioReference**.

---

## Post-import review

After importing, the generated channels and aliases behave exactly like manually created ones. Review them before starting a session.

### Verify channel settings

Navigate to the **Channels** tab in the Playlist Editor. You will see new channels created for each imported site.

- Confirm the **Decoder** protocol matches your system (P25 Phase 1 or Phase 2).
- Toggle **Auto-Start** on for your primary control channels so they begin decoding automatically when the application launches.

### Customize alias groups

Navigate to the **Aliases** tab. The import process groups talkgroups based on the categories provided by RadioReference.

- Assign colors to specific aliases to make them stand out in the Now Playing panel.
- Add automated actions — such as a notification beep or a script trigger — for high-priority talkgroups.
- Enable **Record** on any alias to automatically save audio for that talkgroup. See [Audio Recordings](/configuration/audio-recordings) for details.

---

## Credential storage

Your RadioReference username and password are saved in the application's Java preferences store. The **Store credentials** option (enabled by default) means you only need to log in once. To remove stored credentials, open the RadioReference panel and click the option to clear them.

> **Warning:**
> Credentials are stored in your operating system's user preferences store. Avoid using your RadioReference password on shared or publicly accessible systems.
