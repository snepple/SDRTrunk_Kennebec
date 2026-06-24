package io.github.dsheirer.preference.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the user's allocated-memory preference to whatever launcher actually starts SDRTrunk.
 *
 * SDRTrunk can be started several ways, each of which gets its {@code -Xmx} heap setting from a different place:
 * <ul>
 *     <li><b>Gradle/runtime-zip scripts</b> ({@code bin/sdr-trunk.bat}, {@code bin/sdr-trunk}) - these already read
 *         {@code ~/SDRTrunk/SDRTrunk.memory} at launch (see build.gradle), but we also rewrite the literal
 *         {@code -Xmx} for completeness.</li>
 *     <li><b>jpackage native installer</b> - heap is baked into {@code app/SDRTrunk.cfg} ({@code java-options=-Xmx...}).
 *         The native launcher re-reads this file each launch, so rewriting it makes the setting take effect.</li>
 *     <li><b>launch4j {@code SDRTrunk.exe}</b> (portable zip) - the exe appends JVM options from a sibling
 *         {@code SDRTrunk.l4j.ini}; a {@code -Xmx} there overrides the embedded default.</li>
 * </ul>
 *
 * Files under a protected install location (e.g. {@code C:\Program Files}) may not be writable without elevation; in
 * that case the update is logged as skipped and the script-based {@code SDRTrunk.memory} mechanism remains the
 * fallback for the script launchers.
 */
public class MemoryScriptUpdater
{
    private final static Logger mLog = LoggerFactory.getLogger(MemoryScriptUpdater.class);
    private static final Pattern XMX_PATTERN = Pattern.compile("-Xmx\\d+[gGmM]");

    /**
     * Updates every launcher configuration that can be located and written so the requested heap size takes effect
     * on the next launch.
     * @param gb memory in gigabytes
     */
    public static void updateMemoryLimit(int gb)
    {
        String xmxParam = "-Xmx" + gb + "g";
        int updated = 0;

        //Always write the user-home fallback file the launcher start scripts read at runtime (build.gradle appends
        //logic that applies -Xmx<value>g from <user.home>/SDRTrunk/SDRTrunk.memory). This lives under the user's
        //home, which is writable without elevation, so the setting still takes effect when the install directory
        //(e.g. C:\Program Files) is not writable for the jpackage .cfg / launch4j ini below.
        boolean fallbackWritten = writeMemoryFallbackFile(gb);

        for(Path base : candidateBaseDirectories())
        {
            //Gradle / runtime-zip start scripts
            updated += updateXmxInFile(base.resolve("bin").resolve("sdr-trunk.bat"), xmxParam, false);
            updated += updateXmxInFile(base.resolve("bin").resolve("sdr-trunk"), xmxParam, false);

            //jpackage native-launcher configuration
            updated += updateXmxInFile(base.resolve("app").resolve("SDRTrunk.cfg"), xmxParam, false);
            updated += updateXmxInFile(base.resolve("SDRTrunk.cfg"), xmxParam, false);

            //launch4j portable exe - create the ini next to the exe if the exe is present
            if(Files.exists(base.resolve("SDRTrunk.exe")))
            {
                updated += updateLaunch4jIni(base.resolve("SDRTrunk.l4j.ini"), xmxParam);
            }
        }

        if(updated == 0)
        {
            if(fallbackWritten)
            {
                mLog.info("Allocated-memory preference set to {} GB. No in-place launcher configuration was writable " +
                        "(e.g. an install under Program Files needing elevation), but the value was written to the " +
                        "user-home fallback file the start scripts read; it will apply on next launch via the start " +
                        "scripts. If you launch the native installer/portable .exe from a protected folder, run it " +
                        "from a writable location or as administrator for the change to take effect.", gb);
            }
            else
            {
                mLog.warn("Allocated-memory preference set to {} GB but no launcher configuration could be written " +
                        "and the user-home fallback file could not be created. The new heap size may not take effect.", gb);
            }
        }
        else
        {
            mLog.info("Applied allocated-memory preference of {} GB to {} launcher configuration file(s){}.", gb,
                    updated, fallbackWritten ? " and the user-home fallback file" : "");
        }
    }

    /**
     * Writes the heap size (in GB) to {@code <user.home>/SDRTrunk/SDRTrunk.memory}. The launcher start scripts read
     * this file at runtime and apply {@code -Xmx<value>g}, so this makes the preference take effect even when the
     * install directory's launcher configuration is not writable.
     *
     * @param gb memory in gigabytes
     * @return true if the file was written
     */
    private static boolean writeMemoryFallbackFile(int gb)
    {
        try
        {
            Path dir = Paths.get(System.getProperty("user.home"), "SDRTrunk");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("SDRTrunk.memory"), Integer.toString(gb));
            return true;
        }
        catch(IOException e)
        {
            mLog.warn("Could not write memory fallback file <user.home>/SDRTrunk/SDRTrunk.memory: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Candidate directories that may contain a launcher configuration, relative to where the JVM is running.
     */
    private static List<Path> candidateBaseDirectories()
    {
        List<Path> bases = new ArrayList<>();
        Path userDir = Paths.get(System.getProperty("user.dir"));

        addCandidate(bases, userDir);
        addCandidate(bases, userDir.getParent());                       //app/ -> install root (jpackage)
        addCandidate(bases, userDir.resolve("build").resolve("install").resolve("sdr-trunk")); //dev installDist

        return bases;
    }

    private static void addCandidate(List<Path> bases, Path path)
    {
        if(path != null && !bases.contains(path))
        {
            bases.add(path);
        }
    }

    /**
     * Replaces the {@code -Xmx} value in a file when present.
     * @return 1 if the file was found and updated, 0 otherwise.
     */
    private static int updateXmxInFile(Path path, String newParam, boolean createIfMissing)
    {
        if(!Files.exists(path))
        {
            return 0;
        }

        try
        {
            String content = Files.readString(path);
            Matcher matcher = XMX_PATTERN.matcher(content);

            if(matcher.find())
            {
                String updated = matcher.replaceAll(newParam);

                if(!updated.equals(content))
                {
                    Files.writeString(path, updated);
                    mLog.info("Updated heap setting in {} to {}", path, newParam);
                }
                return 1;
            }

            //Fallback for gradle scripts that define DEFAULT_JVM_OPTS but no explicit -Xmx
            Pattern optsPattern = Pattern.compile("DEFAULT_JVM_OPTS=([\"'])(.*?)([\"'])");
            Matcher optsMatcher = optsPattern.matcher(content);

            if(optsMatcher.find())
            {
                String existingOpts = optsMatcher.group(2);
                String updatedOpts = existingOpts.isEmpty() ? newParam : existingOpts + " " + newParam;
                String updated = optsMatcher.replaceFirst("DEFAULT_JVM_OPTS=$1" + Matcher.quoteReplacement(updatedOpts) + "$3");
                Files.writeString(path, updated);
                mLog.info("Added heap setting in {} as {}", path, newParam);
                return 1;
            }

            mLog.debug("No -Xmx or DEFAULT_JVM_OPTS found in {}", path);
            return 0;
        }
        catch(IOException e)
        {
            mLog.warn("Could not update heap setting in {} (the file may require elevated permissions): {}", path,
                    e.getMessage());
            return 0;
        }
    }

    /**
     * Writes (or rewrites) a launch4j ini so the portable {@code SDRTrunk.exe} uses the requested heap size.  The
     * exe appends each line of this ini to its JVM options, and a later {@code -Xmx} overrides the embedded default.
     * @return 1 if written, 0 on error.
     */
    private static int updateLaunch4jIni(Path iniPath, String xmxParam)
    {
        try
        {
            String content = "";

            if(Files.exists(iniPath))
            {
                content = Files.readString(iniPath);
            }

            String updated;
            Matcher matcher = XMX_PATTERN.matcher(content);

            if(matcher.find())
            {
                updated = matcher.replaceAll(xmxParam);
            }
            else
            {
                updated = content.isBlank() ? xmxParam + System.lineSeparator()
                        : content + System.lineSeparator() + xmxParam + System.lineSeparator();
            }

            Files.writeString(iniPath, updated);
            mLog.info("Updated launch4j heap setting in {} to {}", iniPath, xmxParam);
            return 1;
        }
        catch(IOException e)
        {
            mLog.warn("Could not write launch4j ini {} (the install location may require elevated permissions): {}",
                    iniPath, e.getMessage());
            return 0;
        }
    }
}
