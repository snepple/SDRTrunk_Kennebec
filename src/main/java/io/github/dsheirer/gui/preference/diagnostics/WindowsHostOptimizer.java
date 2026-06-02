package io.github.dsheirer.gui.preference.diagnostics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowsHostOptimizer {
    private static final Logger mLog = LoggerFactory.getLogger(WindowsHostOptimizer.class);

    public static CompletableFuture<String> checkDiagnostics() {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                // Check Power Plan
                Process powerProcess = Runtime.getRuntime().exec("powercfg /getactivescheme");
                BufferedReader reader = new BufferedReader(new InputStreamReader(powerProcess.getInputStream()));
                String line;
                sb.append("Current Power Plan:\n");
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                
                // Check Defender Exclusions
                Process defenderProcess = Runtime.getRuntime().exec(new String[]{"powershell", "-Command", "Get-MpPreference | Select-Object -ExpandProperty ExclusionPath"});
                BufferedReader defenderReader = new BufferedReader(new InputStreamReader(defenderProcess.getInputStream()));
                sb.append("\nWindows Defender Exclusions:\n");
                boolean foundExclusions = false;
                while ((line = defenderReader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        sb.append(line).append("\n");
                        foundExclusions = true;
                    }
                }
                if (!foundExclusions) {
                    sb.append("None found.\n");
                }
            } catch (IOException e) {
                mLog.error("Error checking diagnostics", e);
                sb.append("Error retrieving diagnostics.\n");
            }
            return sb.toString();
        });
    }

    public static void runOptimizationScript() {
        String currentDir = System.getProperty("user.dir");
        String sdrTrunkDir = Paths.get(System.getProperty("user.home"), "SDRTrunk").toString();
        
        String script = String.format(
            "powercfg /SETACVALUEINDEX SCHEME_CURRENT 2a737441-1930-4402-8d77-b2bea12814ab 48e6b7a6-50f5-4782-a5d4-53bb8f07e226 0; " +
            "powercfg /SETDCVALUEINDEX SCHEME_CURRENT 2a737441-1930-4402-8d77-b2bea12814ab 48e6b7a6-50f5-4782-a5d4-53bb8f07e226 0; " +
            "powercfg /SETACTIVE SCHEME_CURRENT; " +
            "Add-MpPreference -ExclusionPath '%s'; " +
            "Add-MpPreference -ExclusionPath '%s'; " +
            "Stop-Service -Name '*DPTF*' -ErrorAction SilentlyContinue; " +
            "Set-Service -Name '*DPTF*' -StartupType Disabled -ErrorAction SilentlyContinue; " +
            "Write-Host 'Optimizations Applied! You may close this window.'; Start-Sleep -Seconds 3",
            currentDir, sdrTrunkDir
        );
        
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", "Start-Process", "powershell", "-ArgumentList", "\"-NoProfile -ExecutionPolicy Bypass -Command \\\"" + script + "\\\"\"", "-Verb", "RunAs");
            pb.start();
            mLog.info("Launched elevated optimization script.");
        } catch (IOException e) {
            mLog.error("Error launching elevated PowerShell script", e);
        }
    }
}
