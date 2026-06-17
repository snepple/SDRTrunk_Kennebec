# Increments projectVersion in gradle.properties (format XX.YYY, where YYY rolls over into XX at 1000,
# e.g. 00.999 -> 01.000), writes it back, and prints the new version to stdout.
#
# Invoked by build_and_release.bat each run so every build gets a new, monotonically increasing version.
#
# IMPORTANT - monotonic across runs without depending on a push:
# The build workspace is 'git reset --hard origin/master' on every run, which would discard a version
# bump that could not be pushed to origin/master (e.g. when the build machine has no push credentials,
# or master is protected). That is what previously froze the version. To stay monotonic regardless, the
# new version is computed as ONE MORE THAN THE HIGHER of:
#   (a) the version currently in gradle.properties (i.e. origin/master after the sync), and
#   (b) the version recorded in -CounterFile, a small file kept OUTSIDE the reset workspace.
# The new version is written to BOTH, so the next run always continues forward even if the push fails.
#
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File bump_version.ps1 `
#       -GradleProperties gradle.properties -CounterFile C:\path\sdrtrunk_build_version.txt [-PushToMaster]

param(
    [string]$GradleProperties = "gradle.properties",
    [string]$CounterFile = "",
    [switch]$PushToMaster
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $GradleProperties)) {
    Write-Error "gradle.properties not found at $GradleProperties"
    exit 1
}

$content = Get-Content -Raw -LiteralPath $GradleProperties

# Match the value only (no end-of-line anchor) so trailing whitespace, a CR, or a comment cannot block it.
$gMatch = [regex]::Match($content, '(?m)^projectVersion=(\d+)\.(\d+)')
if (-not $gMatch.Success) {
    Write-Error "projectVersion (format XX.YYY) not found in $GradleProperties"
    exit 1
}

$major = [int]$gMatch.Groups[1].Value
$minor = [int]$gMatch.Groups[2].Value

# Take the higher of gradle.properties and the persistent counter so the version never goes backwards
# after a 'git reset --hard origin/master' discards an un-pushed bump.
if ($CounterFile -and (Test-Path -LiteralPath $CounterFile)) {
    $counterMatch = [regex]::Match((Get-Content -Raw -LiteralPath $CounterFile), '(\d+)\.(\d+)')
    if ($counterMatch.Success) {
        $cMajor = [int]$counterMatch.Groups[1].Value
        $cMinor = [int]$counterMatch.Groups[2].Value
        if (($cMajor -gt $major) -or (($cMajor -eq $major) -and ($cMinor -gt $minor))) {
            $major = $cMajor
            $minor = $cMinor
        }
    }
}

$minor++
if ($minor -gt 999) {
    $major++
    $minor = 0
}

$newVersion = ('{0:D2}.{1:D3}' -f $major, $minor)

# Replace only the value (no end-of-line anchor) so trailing whitespace/CR/comments are preserved.
$newContent = [regex]::Replace($content, '(?m)^projectVersion=\d+\.\d+', "projectVersion=$newVersion")

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path -LiteralPath $GradleProperties).Path, $newContent, $utf8)

# Persist the new version OUTSIDE the reset workspace so the next run continues forward even when the
# push below fails. This is what makes the increment reliable run-to-run.
if ($CounterFile) {
    $counterDir = Split-Path -Parent $CounterFile
    if ($counterDir -and -not (Test-Path -LiteralPath $counterDir)) {
        New-Item -ItemType Directory -Path $counterDir -Force | Out-Null
    }
    [System.IO.File]::WriteAllText($CounterFile, $newVersion, $utf8)
}

# Best-effort publish of the bump to origin/master. Failure here is NON-FATAL: the counter file above
# already guarantees the local build version advances, so a missing push credential no longer freezes
# the version the way it used to.
if ($PushToMaster) {
    try {
        & git add -- $GradleProperties 2>$null | Out-Null
        & git commit -m "Bump version to $newVersion" 2>$null | Out-Null
        & git push origin HEAD:master 2>$null | Out-Null
    } catch {
        # Non-fatal: building with the bumped local version is still fine.
    }
}

# Print the new version so the caller can use it if desired.
Write-Output $newVersion
