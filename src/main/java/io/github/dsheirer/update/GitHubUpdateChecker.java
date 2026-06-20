package io.github.dsheirer.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

/**
 * Checks GitHub for a newer SDRTrunk_Kennebec release and, when available, downloads/launches the
 * Windows installer (or opens the release page on other platforms).
 */
public class GitHubUpdateChecker
{
    private static final Logger mLog = LoggerFactory.getLogger(GitHubUpdateChecker.class);
    private static final String LATEST_RELEASE_URL =
        "https://api.github.com/repos/snepple/SDRTrunk_Kennebec/releases/latest";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    /**
     * Details of a GitHub release.
     */
    public static class Release
    {
        public final String tag;
        public final String htmlUrl;
        public final String installerUrl;   //Windows .exe/.msi asset download URL, or null
        public final String installerName;

        public Release(String tag, String htmlUrl, String installerUrl, String installerName)
        {
            this.tag = tag;
            this.htmlUrl = htmlUrl;
            this.installerUrl = installerUrl;
            this.installerName = installerName;
        }
    }

    /**
     * Fetches the latest release from GitHub, or null if it can't be retrieved.
     */
    public static Release fetchLatestRelease()
    {
        HttpURLConnection connection = null;

        try
        {
            connection = (HttpURLConnection) new URL(LATEST_RELEASE_URL).openConnection();
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            if(connection.getResponseCode() != 200)
            {
                return null;
            }

            String body = readStream(connection.getInputStream());
            if(body == null)
            {
                return null;
            }

            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            String tag = obj.has("tag_name") && !obj.get("tag_name").isJsonNull() ? obj.get("tag_name").getAsString() : null;
            String htmlUrl = obj.has("html_url") && !obj.get("html_url").isJsonNull() ? obj.get("html_url").getAsString() : null;
            String installerUrl = null;
            String installerName = null;

            if(obj.has("assets") && obj.get("assets").isJsonArray())
            {
                JsonArray assets = obj.getAsJsonArray("assets");
                for(JsonElement element : assets)
                {
                    JsonObject asset = element.getAsJsonObject();
                    String name = asset.has("name") ? asset.get("name").getAsString() : "";
                    String lower = name.toLowerCase();
                    if(lower.endsWith(".exe") || lower.endsWith(".msi"))
                    {
                        installerUrl = asset.get("browser_download_url").getAsString();
                        installerName = name;
                        break;
                    }
                }
            }

            return new Release(tag, htmlUrl, installerUrl, installerName);
        }
        catch(Exception e)
        {
            mLog.error("Error checking GitHub for the latest release", e);
            return null;
        }
        finally
        {
            if(connection != null)
            {
                connection.disconnect();
            }
        }
    }

    /**
     * Indicates whether the latest release tag is newer than the current version.  Both are normalized to
     * numeric components (e.g. "v00.108" and "00.107") and compared.
     */
    public static boolean isNewer(String latestTag, String current)
    {
        long latest = toComparable(latestTag);
        long currentValue = toComparable(current);
        return latest >= 0 && currentValue >= 0 && latest > currentValue;
    }

    private static long toComparable(String version)
    {
        if(version == null)
        {
            return -1;
        }

        String cleaned = version.replaceAll("[^0-9.]", "");
        if(cleaned.isEmpty())
        {
            return -1;
        }

        long value = 0;
        for(String part : cleaned.split("\\."))
        {
            if(part.isEmpty())
            {
                continue;
            }
            try
            {
                value = value * 1000 + Long.parseLong(part);
            }
            catch(NumberFormatException e)
            {
                return -1;
            }
        }
        return value;
    }

    /**
     * On Windows with an installer asset, downloads the installer to a temp file and launches it
     * (returns true).  Otherwise opens the release page in the browser (returns false).
     */
    public static boolean downloadAndLaunch(Release release)
    {
        if(release == null)
        {
            return false;
        }

        try
        {
            String os = System.getProperty("os.name", "").toLowerCase();
            if(os.contains("win") && release.installerUrl != null && !release.installerUrl.isEmpty())
            {
                String suffix = release.installerName != null ? release.installerName : "installer.exe";
                Path temp = Files.createTempFile("sdrtrunk-update-", "-" + suffix.replaceAll("[^A-Za-z0-9._-]", "_"));

                try(InputStream in = new URL(release.installerUrl).openStream())
                {
                    Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
                }

                new ProcessBuilder(temp.toString()).start();
                return true;
            }
        }
        catch(Exception e)
        {
            mLog.error("Error downloading/launching the update installer", e);
        }

        //Fallback: open the release page so the user can download manually.
        openBrowser(release.htmlUrl);
        return false;
    }

    private static void openBrowser(String url)
    {
        if(url == null || url.isEmpty())
        {
            return;
        }

        try
        {
            if(java.awt.Desktop.isDesktopSupported() &&
               java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE))
            {
                java.awt.Desktop.getDesktop().browse(new URI(url));
            }
        }
        catch(Exception e)
        {
            mLog.error("Error opening release page in browser", e);
        }
    }

    private static String readStream(InputStream stream)
    {
        if(stream == null)
        {
            return null;
        }

        try(Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A"))
        {
            return scanner.hasNext() ? scanner.next() : null;
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
