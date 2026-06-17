# Regenerates the download-links block in README.md between the
# <!-- DOWNLOADS:START --> and <!-- DOWNLOADS:END --> markers.
#
# Invoked by build_and_release.bat after a GitHub release is published so the
# README always points at the current version's release assets. Kept as a
# committed script (rather than inline batch) so the markdown, markers and URLs
# need no batch-escaping.
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

$block = @"
$startMarker
<!-- This section is regenerated automatically by build_and_release.bat after each GitHub release. Do not edit by hand. -->
## Download the Latest Release

Not sure what to download? Here is a quick guide to getting started with **SDRTrunk Kennebec (v$Version)**:

- **Windows Users (Recommended)**: Download the Native Windows Installer. This is the easiest way to install and manage SDRTrunk on Windows.
  - [Download Windows Installer (.exe)]($base/SDRTrunk-$Version-windows-installer.exe)
  - **Note**: Because this is an open-source project without a paid code signing certificate, Windows SmartScreen may show an "Unknown Publisher" warning. To proceed, click **"More info"**, then click **"Run anyway"**.
- **Advanced Windows Users**: Download the Portable ZIP if you prefer to run the application without installing it.
  - [Download Windows Portable ZIP (.zip)]($base/SDRTrunk-$Version-windows-x86_64.zip)
- **Mac Users**:
  - [Download macOS Portable ZIP (.zip)]($base/SDRTrunk-$Version-macos-x86_64.zip)
- **Linux Users**:
  - [Download Linux Portable ZIP (.zip)]($base/SDRTrunk-$Version-linux-x86_64.zip)

*(View all release assets and notes on the [Releases Page]($tag))*
$endMarker
"@

if (-not (Test-Path -LiteralPath $ReadmePath)) {
    Write-Host "README not found at $ReadmePath - skipping download-link update."
    exit 0
}

$readme = Get-Content -Raw -LiteralPath $ReadmePath

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
$utf8 = New-Object System.Text.UTF8Encoding($false)
$fullPath = (Resolve-Path -LiteralPath $ReadmePath).Path
[System.IO.File]::WriteAllText($fullPath, $newContent, $utf8)

Write-Host "README download links updated to $Version."
