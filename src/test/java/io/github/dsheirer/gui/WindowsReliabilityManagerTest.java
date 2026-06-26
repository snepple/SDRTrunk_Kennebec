package io.github.dsheirer.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsReliabilityManagerTest
{
    @TempDir
    Path mTempDir;

    @Test
    void updaterExitMarkerSuppressesWatchdogRestart() throws Exception
    {
        String originalOsName = System.getProperty("os.name");
        Field markerDirField = WindowsReliabilityManager.class.getDeclaredField("mMarkerDir");
        markerDirField.setAccessible(true);
        Object originalMarkerDir = markerDirField.get(null);

        try
        {
            System.setProperty("os.name", "Windows 11");
            markerDirField.set(null, mTempDir.toString());

            WindowsReliabilityManager.markIntentionalExitForUpdate();

            Path marker = mTempDir.resolve("sdrtrunk_graceful_exit");
            assertTrue(Files.exists(marker));
            assertEquals("update", Files.readString(marker));
        }
        finally
        {
            if(originalOsName != null)
            {
                System.setProperty("os.name", originalOsName);
            }
            markerDirField.set(null, originalMarkerDir);
        }
    }
}
