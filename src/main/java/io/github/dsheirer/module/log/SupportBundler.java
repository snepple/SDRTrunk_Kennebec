package io.github.dsheirer.module.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SupportBundler {
    private static final Logger mLog = LoggerFactory.getLogger(SupportBundler.class);

    public static File createDiagnosticBundle(Path playlistPath, Path logPath, Map<String, Object> stateJournal) {
        File zipFile = new File(System.getProperty("user.home"), "Desktop/sdrtrunk_support_bundle.zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Add Playlist
            if (playlistPath != null && Files.exists(playlistPath)) {
                addFileToZip(playlistPath.toFile(), zos);
            }

            // Add App Log
            if (logPath != null && Files.exists(logPath)) {
                addFileToZip(logPath.toFile(), zos);
            }

            // Add State Journal
            if (stateJournal != null) {
                ZipEntry entry = new ZipEntry("state_journal.txt");
                zos.putNextEntry(entry);
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Object> kv : stateJournal.entrySet()) {
                    sb.append(kv.getKey()).append("=").append(kv.getValue()).append("\n");
                }
                zos.write(sb.toString().getBytes());
                zos.closeEntry();
            }

            mLog.info("Support Bundle created at: " + zipFile.getAbsolutePath());
            return zipFile;
        } catch (IOException e) {
            mLog.error("Failed to create support bundle", e);
            return null;
        }
    }

    private static void addFileToZip(File file, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }
}
