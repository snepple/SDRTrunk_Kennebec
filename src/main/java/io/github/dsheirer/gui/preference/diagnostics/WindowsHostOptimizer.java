package io.github.dsheirer.gui.preference.diagnostics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
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

    public static String getOptimizationScriptContent() {
        String currentDir = System.getProperty("user.dir");
        String sdrTrunkDir = Paths.get(System.getProperty("user.home"), "SDRTrunk").toString();
        
        return String.format(
            "$ErrorActionPreference = 'SilentlyContinue'; " +
            "try { " +
            "powercfg /SETACVALUEINDEX SCHEME_CURRENT 2a737441-1930-4402-8d77-b2bea12814ab 48e6b7a6-50f5-4782-a5d4-53bb8f07e226 0; " +
            "powercfg /SETDCVALUEINDEX SCHEME_CURRENT 2a737441-1930-4402-8d77-b2bea12814ab 48e6b7a6-50f5-4782-a5d4-53bb8f07e226 0; " +
            "powercfg /SETACTIVE SCHEME_CURRENT; " +
            "Add-MpPreference -ExclusionPath '%s'; " +
            "Add-MpPreference -ExclusionPath '%s'; " +
            "Stop-Service -Name '*DPTF*' -ErrorAction SilentlyContinue; " +
            "Set-Service -Name '*DPTF*' -StartupType Disabled -ErrorAction SilentlyContinue; " +
            "} catch { } ",
            currentDir, sdrTrunkDir
        );
    }

    public static CompletableFuture<Boolean> runOptimizationScript() {
        return CompletableFuture.supplyAsync(() -> {
            String innerScript = getOptimizationScriptContent() + "exit 0;";
            String innerEncoded = Base64.getEncoder().encodeToString(innerScript.getBytes(StandardCharsets.UTF_16LE));

            //Outer command elevates the inner (optimization) script via UAC (-Verb RunAs), waits for the
            //elevated child, and exits with the child's real exit code. The inner payload is passed as an
            //argument-array element so its length/characters can't break command-line quoting, and the
            //outer command is itself base64-encoded to avoid any quoting pitfalls. If the user declines
            //the UAC prompt, Start-Process throws and we return a non-zero exit so the UI reports failure.
            String outerScript =
                "$ErrorActionPreference='Stop'; " +
                "try { " +
                  "$p = Start-Process powershell -ArgumentList @('-NoProfile','-WindowStyle','Hidden'," +
                  "'-ExecutionPolicy','Bypass','-EncodedCommand','" + innerEncoded + "') -Verb RunAs -Wait -PassThru; " +
                  "exit $p.ExitCode " +
                "} catch { exit 1 }";
            String outerEncoded = Base64.getEncoder().encodeToString(outerScript.getBytes(StandardCharsets.UTF_16LE));

            try {
                ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-EncodedCommand", outerEncoded);
                Process p = pb.start();
                int exitCode = p.waitFor();
                mLog.info("Optimization script exited with code {}", exitCode);
                return exitCode == 0;
            } catch (Exception e) {
                mLog.error("Error launching elevated PowerShell script", e);
                return false;
            }
        });
    }
}
