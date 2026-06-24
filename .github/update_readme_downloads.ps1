# Regenerates the download-links block in README.md between the
# <!-- DOWNLOADS:START --> and <!-- DOWNLOADS:END --> markers.
#
# Invoked by build_and_release.bat after a GitHub release is published so the
# README always points at the current version's release assets. Kept as a
# committed script (rather than inline batch) so the markdown, markers and URLs
# need no batch-escaping.
#
# The link list is built from the assets that ACTUALLY exist in the published
# release (queried via the GitHub CLI), so a TEST build that only produced the
# Windows installer won't advertise non-existent macOS/Linux downloads, and a
# full release that produced ARM64 packages will list them too. If the CLI is
# unavailable (offline, not authenticated, gh missing) it falls back to the
# standard cross-platform link set so the README is still updated.
#
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File update_readme_downloads.ps1 -Version 00.095 -Repo snepple/SDRTrunk_Kennebec [-ReadmePath README.md]

param(
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$Repo,
    [string]$ReadmePath = "README.md"
)

$ErrorActionPreference = "Stop"

$base = "https://github.com/$Repo/releases/download/$Version"
$tag  = "https://github.com/$Repo/releases/tag/$Version"

$startMarker = "<!-- DOWNLOADS:START -->"
$endMarker   = "<!-- DOWNLOADS:END -->"

# ---------------------------------------------------------------------------
# Discover which assets actually exist in the published release (best effort).
# ---------------------------------------------------------------------------
$assetNames = @()
try {
    $raw = & gh release view $Version --repo $Repo --json assets 2>$null
    if ($LASTEXITCODE -eq 0 -and $raw) {
        $parsed = ($raw | Out-String | ConvertFrom-Json)
        if ($parsed.assets) {
            $assetNames = @($parsed.assets | ForEach-Object { $_.name })
        }
    }
}
catch {
    $assetNames = @()
}

function Test-Asset([string]$suffix) {
    return ($assetNames -contains "SDRTrunk-$Version-$suffix")
}

# ---------------------------------------------------------------------------
# Build the body. Prefer the real asset list; fall back to the standard set.
# ---------------------------------------------------------------------------
$body = New-Object System.Collections.Generic.List[string]
$body.Add("Not sure what to download? Here is a quick guide to getting started with **SDRTrunk Kennebec (v$Version)**:")
$body.Add("")

if ($assetNames.Count -gt 0) {
    # Windows
    if (Test-Asset "windows-installer.exe") {
        $body.Add("- **Windows Users (Recommended)**: Download the Native Windows Installer. This is the easiest way to install and manage SDRTrunk on Windows.")
        $body.Add("  - [Download Windows Installer (.exe)]($base/SDRTrunk-$Version-windows-installer.exe)")
        $body.Add("  - **Note**: Because this is an open-source project without a paid code signing certificate, Windows SmartScreen may show an ""Unknown Publisher"" warning. To proceed, click **""More info""**, then click **""Run anyway""**.")
    }
    if (Test-Asset "windows-x86_64.zip") {
        $body.Add("- **Advanced Windows Users**: Download the Portable ZIP if you prefer to run the application without installing it.")
        $body.Add("  - [Download Windows Portable ZIP (.zip)]($base/SDRTrunk-$Version-windows-x86_64.zip)")
    }
    # macOS
    if ((Test-Asset "macos-x86_64.zip") -or (Test-Asset "macos-aarch64.zip")) {
        $body.Add("- **Mac Users**:")
        if (Test-Asset "macos-x86_64.zip") {
            $body.Add("  - [Download macOS Portable ZIP - Intel x64 (.zip)]($base/SDRTrunk-$Version-macos-x86_64.zip)")
        }
        if (Test-Asset "macos-aarch64.zip") {
            $body.Add("  - [Download macOS Portable ZIP - Apple Silicon / ARM64 (.zip)]($base/SDRTrunk-$Version-macos-aarch64.zip)")
        }
    }
    # Linux
    if ((Test-Asset "linux-x86_64.zip") -or (Test-Asset "linux-aarch64.zip")) {
        $body.Add("- **Linux Users**:")
        if (Test-Asset "linux-x86_64.zip") {
            $body.Add("  - [Download Linux Portable ZIP - x64 (.zip)]($base/SDRTrunk-$Version-linux-x86_64.zip)")
        }
        if (Test-Asset "linux-aarch64.zip") {
            $body.Add("  - [Download Linux Portable ZIP - ARM64 (.zip)]($base/SDRTrunk-$Version-linux-aarch64.zip)")
        }
    }
}
else {
    # Fallback: CLI unavailable - emit the standard cross-platform set.
    $body.Add("- **Windows Users (Recommended)**: Download the Native Windows Installer. This is the easiest way to install and manage SDRTrunk on Windows.")
    $body.Add("  - [Download Windows Installer (.exe)]($base/SDRTrunk-$Version-windows-installer.exe)")
    $body.Add("  - **Note**: Because this is an open-source project without a paid code signing certificate, Windows SmartScreen may show an ""Unknown Publisher"" warning. To proceed, click **""More info""**, then click **""Run anyway""**.")
    $body.Add("- **Advanced Windows Users**: Download the Portable ZIP if you prefer to run the application without installing it.")
    $body.Add("  - [Download Windows Portable ZIP (.zip)]($base/SDRTrunk-$Version-windows-x86_64.zip)")
    $body.Add("- **Mac Users**:")
    $body.Add("  - [Download macOS Portable ZIP (.zip)]($base/SDRTrunk-$Version-macos-x86_64.zip)")
    $body.Add("- **Linux Users**:")
    $body.Add("  - [Download Linux Portable ZIP (.zip)]($base/SDRTrunk-$Version-linux-x86_64.zip)")
}

$body.Add("")
$body.Add("*(View all release assets and notes on the [Releases Page]($tag))*")

$block = @"
$startMarker
<!-- This section is regenerated automatically by build_and_release.bat after each GitHub release. Do not edit by hand. -->
## Download the Latest Release

$([string]::Join("`n", $body))
$endMarker
"@

if (-not (Test-Path -LiteralPath $ReadmePath)) {
    Write-Host "README not found at $ReadmePath - skipping download-link update."
    exit 0
}

$fullPath = (Resolve-Path -LiteralPath $ReadmePath).Path
# Windows PowerShell 5.1 reads UTF-8-without-BOM files as the system ANSI code
# page unless the encoding is explicit, which mojibakes em dashes/arrows.
$utf8 = New-Object System.Text.UTF8Encoding -ArgumentList $false, $true
$readme = [System.IO.File]::ReadAllText($fullPath, $utf8)

$startIdx = $readme.IndexOf($startMarker)
$endIdx   = $readme.IndexOf($endMarker)

if ($startIdx -lt 0 -or $endIdx -lt 0 -or $endIdx -lt $startIdx) {
    Write-Host "Download markers not found in $ReadmePath - skipping."
    exit 0
}

$pre  = $readme.Substring(0, $startIdx)
$post = $readme.Substring($endIdx + $endMarker.Length)

$newContent = $pre + $block + $post

if ($newContent -eq $readme) {
    Write-Host "README download links already current for $Version."
    exit 0
}

# Write UTF-8 without BOM to keep the file clean and stable.
[System.IO.File]::WriteAllText($fullPath, $newContent, $utf8)

Write-Host "README download links updated to $Version."
