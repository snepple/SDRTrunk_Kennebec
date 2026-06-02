package io.github.dsheirer.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class UsbMonitorScriptTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testScriptExtractionAndExecution() throws Exception {
        // 1. Assert that the script copies/unpacks itself from resources
        Path tempDir = Files.createTempDirectory("usb_monitor_test");
        Path scriptPath = tempDir.resolve("usb_monitor.ps1");
        
        try (InputStream is = getClass().getResourceAsStream("/scripts/usb_monitor.ps1")) {
            assertNotNull(is, "usb_monitor.ps1 should exist in resources");
            Files.copy(is, scriptPath, StandardCopyOption.REPLACE_EXISTING);
            // Remove the #Requires -RunAsAdministrator so we can test the loop logic without elevation
            String content = Files.readString(scriptPath);
            content = content.replace("#Requires -RunAsAdministrator", "");
            Files.writeString(scriptPath, content);
        }
        
        assertTrue(Files.exists(scriptPath), "Script should be copied to the local directory");
        assertTrue(Files.size(scriptPath) > 0, "Script should not be empty");
        
        // 2. Mock starting SDRTrunk JVM using a simple sleeping Java process
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        
        Path mockJvmClass = tempDir.resolve("MockJVM.java");
        Files.writeString(mockJvmClass, "public class MockJVM { public static void main(String[] args) throws Exception { Thread.sleep(60000); } }");
        
        ProcessBuilder jvmBuilder = new ProcessBuilder(javaBin, mockJvmClass.toAbsolutePath().toString());
        Process mockJvm = jvmBuilder.start();
        long mockPid = mockJvm.pid();
        
        assertTrue(mockJvm.isAlive(), "Mock JVM should be running");
        
        // 3. Execute the script in the background
        Path logFile = tempDir.resolve("monitor.log");
        ProcessBuilder psBuilder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", scriptPath.toAbsolutePath().toString(),
                "-ProcessId", String.valueOf(mockPid),
                "-LogFile", logFile.toAbsolutePath().toString()
        );
        
        psBuilder.redirectErrorStream(true);
        Path psOutFile = tempDir.resolve("ps_out.txt");
        psBuilder.redirectOutput(psOutFile.toFile());
        
        Process scriptProcess = psBuilder.start();
        
        // Wait for script to initialize and write logs
        Thread.sleep(3000);
        
        assertTrue(scriptProcess.isAlive(), "Script should execute in the background and remain alive while Mock JVM runs. PS Output: " + Files.readString(psOutFile));
        
        // Assert that the script runs successfully without execution policy issues
        // Triggered by verifying it actually wrote to the log file (i.e., wasn't blocked from executing)
        assertTrue(Files.exists(logFile), "Log file should be created by the script");
        String logContent = Files.readString(logFile);
        assertTrue(logContent.contains("Starting Universal SDR USB Monitor"), "Script should initialize successfully");
        
        // 4. Terminate the mock JVM and assert script is killed cleanly without zombies
        mockJvm.destroy();
        assertTrue(mockJvm.waitFor(5, TimeUnit.SECONDS), "Mock JVM should terminate");
        
        // Script polls every 5 seconds. Wait up to 10 seconds for it to exit
        boolean scriptExited = scriptProcess.waitFor(10, TimeUnit.SECONDS);
        
        if (!scriptExited) {
            scriptProcess.destroyForcibly();
            fail("Script did not exit cleanly after mock JVM terminated; left a zombie process.");
        }
        
        assertFalse(scriptProcess.isAlive(), "Script should be cleanly killed");
        
        // Check log to see that it detected parent termination
        String finalLogContent = Files.readString(logFile);
        assertTrue(finalLogContent.contains("Parent Java process (" + mockPid + ") is no longer running"), 
                "Script should log that parent process exited");
    }
}
