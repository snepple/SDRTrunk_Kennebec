# Increments projectVersion in gradle.properties by one patch (the YYY in XX.YYY), rolling YYY over
# into XX at 1000 (e.g. 00.999 -> 01.000), writes it back, and prints the new version to stdout.
#
# Invoked by build_and_release.bat each run so every build gets a new, monotonically increasing
# version. Kept as a committed script so the parsing/formatting needs no batch escaping.
#
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File bump_version.ps1 -GradleProperties gradle.properties

param(
    [string]$GradleProperties = "gradle.properties",
    [switch]$PushToMaster
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $GradleProperties)) {
    Write-Error "gradle.properties not found at $GradleProperties"
    exit 1
}

$content = Get-Content -Raw -LiteralPath $GradleProperties

$match = [regex]::Match($content, '(?m)^projectVersion=(\d+)\.(\d+)[ \t]*$')
if (-not $match.Success) {
    Write-Error "projectVersion (format XX.YYY) not found in $GradleProperties"
    exit 1
}

$major = [int]$match.Groups[1].Value
$minor = [int]$match.Groups[2].Value

$minor++
if ($minor -gt 999) {
    $major++
    $minor = 0
}

$newVersion = ('{0:D2}.{1:D3}' -f $major, $minor)

$newContent = [regex]::Replace($content, '(?m)^projectVersion=\d+\.\d+[ \t]*$', "projectVersion=$newVersion")

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path -LiteralPath $GradleProperties).Path, $newContent, $utf8)

# Optionally commit and push the bump straight to origin/master (best-effort).
if ($PushToMaster) {
    try {
        & git add gradle.properties 2>$null | Out-Null
        & git commit -m "Bump version to $newVersion" 2>$null | Out-Null
        & git push origin HEAD:master 2>$null | Out-Null
    } catch {
        # Non-fatal: building with the bumped local version is still fine.
    }
}

# Print the new version so the caller can use it if desired.
Write-Output $newVersion
