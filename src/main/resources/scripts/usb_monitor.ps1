#Requires -RunAsAdministrator

param(
    [Parameter(Mandatory=$true)]
    [int]$ProcessId,
    [Parameter(Mandatory=$true)]
    [string]$LogFile
)

$HardwareIDs = @(
    "VID_0BDA&PID_2838", # RTL-SDR
    "VID_1D50&PID_6089", # HackRF One
    "VID_1D50&PID_60A1"  # Airspy
)

$PollInterval = 5

function Write-Log($Message) {
    $Timestamp = (Get-Date).ToString("yyyyMMdd HHmmss.SSS")
    $LogMessage = "$Timestamp [UsbMonitor] INFO - $Message"
    try {
        Add-Content -Path $LogFile -Value $LogMessage -ErrorAction SilentlyContinue
    } catch {
        # Ignore write errors
    }
}

function Write-LogError($Message) {
    $Timestamp = (Get-Date).ToString("yyyyMMdd HHmmss.SSS")
    $LogMessage = "$Timestamp [UsbMonitor] ERROR - $Message"
    try {
        Add-Content -Path $LogFile -Value $LogMessage -ErrorAction SilentlyContinue
    } catch {
        # Ignore write errors
    }
}

$RegexPattern = ($HardwareIDs | ForEach-Object { [regex]::Escape($_) }) -join '|'

function Reset-SDRDevice($DeviceId, $Name) {
    Write-LogError "Error detected on SDR: $Name"
    Write-Log "Device ID: $DeviceId. Attempting to reset device..."

    try {
        Write-Log "Disabling device using pnputil..."
        $disableResult = pnputil /disable-device "$DeviceId" | Out-String
        Write-Log "Disable result: $($disableResult.Trim())"

        Start-Sleep -Seconds 2

        Write-Log "Enabling device using pnputil..."
        $enableResult = pnputil /enable-device "$DeviceId" | Out-String
        Write-Log "Enable result: $($enableResult.Trim())"

        Write-Log "Successfully sent reset commands to SDR."
    } catch {
        Write-LogError "Failed to reset device. Windows may require a physical replug. Error: $_"
    }
}

Write-Log "Starting Universal SDR USB Monitor..."
Write-Log "Monitoring for devices with Hardware IDs: $($HardwareIDs -join ', ')"
Write-Log "Ignoring MI_01 (Infrared) interfaces to prevent false alarms."

Write-Log "Performing initial scan for devices in error state..."
try {
    $initialDevices = Get-CimInstance -ClassName Win32_PnPEntity -Filter "ConfigManagerErrorCode <> 0" -ErrorAction SilentlyContinue
    if ($initialDevices) {
        foreach ($device in $initialDevices) {
            if (($device.PNPDeviceID -match $RegexPattern) -and ($device.PNPDeviceID -notmatch "MI_01")) {
                Reset-SDRDevice -DeviceId $device.PNPDeviceID -Name $device.Name
            }
        }
    }
} catch {
    Write-LogError "Failed to perform initial device scan: $_"
}

$MessageData = @{
    LogFile = $LogFile
    RegexPattern = $RegexPattern
}

$action = {
    $eventDevice = $Event.SourceEventArgs.NewEvent.TargetInstance
    $deviceId = $eventDevice.PNPDeviceID
    $deviceName = $eventDevice.Name
    $errorCode = $eventDevice.ConfigManagerErrorCode

    $LogFile = $Event.MessageData.LogFile
    $RegexPattern = $Event.MessageData.RegexPattern

    if (($deviceId -match $RegexPattern) -and ($deviceId -notmatch "MI_01")) {
        $Timestamp = (Get-Date).ToString("yyyyMMdd HHmmss.SSS")

        function Write-EventLog($Msg, $IsError=$false) {
            $level = if ($IsError) { "ERROR" } else { "INFO" }
            $LogMessage = "$Timestamp [UsbMonitor] $level - $Msg"
            try { Add-Content -Path $LogFile -Value $LogMessage -ErrorAction SilentlyContinue } catch {}
        }

        Write-EventLog "Error detected on SDR: $deviceName (ErrorCode: $errorCode)" $true
        Write-EventLog "Device ID: $deviceId. Attempting to reset device..."

        try {
            Write-EventLog "Disabling device using pnputil..."
            $disableResult = pnputil /disable-device "$deviceId" | Out-String
            Write-EventLog "Disable result: $($disableResult.Trim())"

            Start-Sleep -Seconds 2

            Write-EventLog "Enabling device using pnputil..."
            $enableResult = pnputil /enable-device "$deviceId" | Out-String
            Write-EventLog "Enable result: $($enableResult.Trim())"

            Write-EventLog "Successfully sent reset commands to SDR."
        } catch {
            Write-EventLog "Failed to reset device. Windows may require a physical replug. Error: $_" $true
        }
    }
}

Write-Log "Subscribing to WMI events for device state changes..."
try {
    $WmiQueryMod = "SELECT * FROM __InstanceModificationEvent WITHIN 5 WHERE TargetInstance ISA 'Win32_PnPEntity' AND TargetInstance.ConfigManagerErrorCode <> 0 AND PreviousInstance.ConfigManagerErrorCode = 0"
    Register-WmiEvent -Query $WmiQueryMod -SourceIdentifier "SDRDeviceErrorMonitorMod" -MessageData $MessageData -Action $action | Out-Null

    $WmiQueryCre = "SELECT * FROM __InstanceCreationEvent WITHIN 5 WHERE TargetInstance ISA 'Win32_PnPEntity' AND TargetInstance.ConfigManagerErrorCode <> 0"
    Register-WmiEvent -Query $WmiQueryCre -SourceIdentifier "SDRDeviceErrorMonitorCre" -MessageData $MessageData -Action $action | Out-Null
    Write-Log "Successfully subscribed to WMI events."
} catch {
    Write-LogError "Failed to subscribe to WMI events: $_"
}

Write-Log "Entering monitor loop. Waiting for parent Java process ($ProcessId) to exit..."

while ($true) {
    if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        Write-Log "Parent Java process ($ProcessId) is no longer running. Exiting USB Monitor."
        try {
            Unregister-Event -SourceIdentifier "SDRDeviceErrorMonitorMod" -ErrorAction SilentlyContinue
            Unregister-Event -SourceIdentifier "SDRDeviceErrorMonitorCre" -ErrorAction SilentlyContinue
        } catch {}
        break
    }
    Start-Sleep -Seconds 5
}
