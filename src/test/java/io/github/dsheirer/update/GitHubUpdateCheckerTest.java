package io.github.dsheirer.update;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GitHubUpdateCheckerTest
{
    @Test
    void parseReleaseSelectsWindowsInstallerAsset()
    {
        String body = """
                {
                  "tag_name": "00.153",
                  "html_url": "https://github.com/snepple/SDRTrunk_Kennebec/releases/tag/00.153",
                  "assets": [
                    {
                      "name": "SDRTrunk-00.153-windows-x86_64.zip",
                      "browser_download_url": "https://example.invalid/portable.zip"
                    },
                    {
                      "name": "SDRTrunk-00.153-windows-installer.exe",
                      "browser_download_url": "https://example.invalid/installer.exe"
                    }
                  ]
                }
                """;

        GitHubUpdateChecker.Release release = GitHubUpdateChecker.parseRelease(body);

        assertNotNull(release);
        assertEquals("00.153", release.tag);
        assertEquals("https://github.com/snepple/SDRTrunk_Kennebec/releases/tag/00.153", release.htmlUrl);
        assertEquals("https://example.invalid/installer.exe", release.installerUrl);
        assertEquals("SDRTrunk-00.153-windows-installer.exe", release.installerName);
    }

    @Test
    void windowsInstallerLaunchUsesShellStartForUacPrompt()
    {
        Path installer = Path.of("C:/Users/Test User/AppData/Local/Temp/SDRTrunk-00.153-windows-installer.exe");

        List<String> command = GitHubUpdateChecker.getWindowsInstallerLaunchCommand(installer);

        assertEquals(List.of("cmd.exe", "/c",
                "start \"\" \"" + installer.toAbsolutePath() + "\""), command);
    }
}
