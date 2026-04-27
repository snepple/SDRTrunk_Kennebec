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

Write-Log "Starting Universal SDR USB Monitor..."
Write-Log "Monitoring for devices with Hardware IDs: $($HardwareIDs -join ', ')"
Write-Log "Ignoring MI_01 (Infrared) interfaces to prevent false alarms."

$RegexPattern = ($HardwareIDs | ForEach-Object { [regex]::Escape($_) }) -join '|'

while ($true) {
    # Check if parent Java process is still running
    if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
        Write-Log "Parent Java process ($ProcessId) is no longer running. Exiting USB Monitor."
        break
    }

    # Scan devices, filter by regex, and EXPLICITLY exclude the MI_01 interface
    $sdrDevices = Get-PnpDevice -PresentOnly | Where-Object {
        ($_.InstanceId -match $RegexPattern) -and ($_.InstanceId -notmatch "MI_01")
    }

    if ($sdrDevices) {
        foreach ($device in $sdrDevices) {
            if ($device.Status -ne "OK") {
                Write-LogError "Error detected on SDR: $($device.FriendlyName)"
                Write-Log "Instance ID: $($device.InstanceId)"
                Write-Log "Status: $($device.Status). Attempting to reset device..."

                try {
                    Restart-PnpDevice -InstanceId $device.InstanceId -Confirm:$false -ErrorAction Stop
                    Write-Log "Successfully sent reset command to SDR."
                } catch {
                    Write-LogError "Failed to reset device. Windows may require a physical replug."
                }
            }
        }
    }

    Start-Sleep -Seconds $PollInterval
}
