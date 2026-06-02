package io.github.dsheirer.preference.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryScriptUpdater {
    private final static Logger mLog = LoggerFactory.getLogger(MemoryScriptUpdater.class);

    /**
     * Updates the -Xmx setting in sdr-trunk.bat and sdr-trunk shell scripts.
     * @param gb memory in gigabytes
     */
    public static void updateMemoryLimit(int gb) {
        String xmxParam = "-Xmx" + gb + "g";
        
        // Attempt to locate bin/sdr-trunk.bat and bin/sdr-trunk relative to the current working directory
        Path baseDir = Paths.get(System.getProperty("user.dir"));
        Path batPath = baseDir.resolve("bin").resolve("sdr-trunk.bat");
        Path shPath = baseDir.resolve("bin").resolve("sdr-trunk");
        
        // Also check if we are in build/install/sdr-trunk (development)
        if (!Files.exists(batPath)) {
            batPath = baseDir.resolve("build").resolve("install").resolve("sdr-trunk").resolve("bin").resolve("sdr-trunk.bat");
            shPath = baseDir.resolve("build").resolve("install").resolve("sdr-trunk").resolve("bin").resolve("sdr-trunk");
        }

        updateScript(batPath, xmxParam);
        updateScript(shPath, xmxParam);
    }

    private static void updateScript(Path scriptPath, String newParam) {
        if (!Files.exists(scriptPath)) {
            mLog.debug("Script not found at: {}", scriptPath);
            return;
        }

        try {
            String content = Files.readString(scriptPath);
            // Replace -Xmx followed by digits and g/G/m/M with the new param
            Pattern pattern = Pattern.compile("-Xmx\\d+[gGmM]");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String updatedContent = matcher.replaceAll(newParam);
                Files.writeString(scriptPath, updatedContent);
                mLog.info("Updated memory limit in {} to {}", scriptPath, newParam);
            } else {
                // If not found, we might want to append it to DEFAULT_JVM_OPTS
                Pattern optsPattern = Pattern.compile("DEFAULT_JVM_OPTS=([\"'])(.*?)([\"'])");
                Matcher optsMatcher = optsPattern.matcher(content);
                if (optsMatcher.find()) {
                    String existingOpts = optsMatcher.group(2);
                    String updatedOpts = existingOpts.isEmpty() ? newParam : existingOpts + " " + newParam;
                    String updatedContent = optsMatcher.replaceFirst("DEFAULT_JVM_OPTS=$1" + updatedOpts + "$3");
                    Files.writeString(scriptPath, updatedContent);
                    mLog.info("Added memory limit in {} to {}", scriptPath, newParam);
                } else {
                    mLog.warn("Could not find DEFAULT_JVM_OPTS or -Xmx in {}", scriptPath);
                }
            }
        } catch (IOException e) {
            mLog.error("Failed to update memory script: {}", scriptPath, e);
        }
    }
}
