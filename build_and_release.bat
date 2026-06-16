@echo off
setlocal EnableDelayedExpansion

:: ============================================================================
::  SDR Trunk Kennebec - Multi-Platform Build and Release Script
:: ============================================================================

:: Persistence Guard - keeps terminal open on failure
if not defined TERMINAL_FIXED (
    set TERMINAL_FIXED=1
    start cmd /k "%~f0"
    exit
)

cd /d "%~dp0"
title SDR Trunk Kennebec - Multi-Platform Builder

:: ---- GITHUB CONFIG ----
set "GH_REPO=snepple/SDRTrunk_Kennebec"
set "REPO_URL=https://github.com/%GH_REPO%.git"
set "FOLDER_NAME=SDRTrunk_Kennebec_Build"
set "ROOT_DIR=%CD%"
set "LOG_FILE=%ROOT_DIR%\build_log.txt"
set "VOLK_BASE=C:\SDR_Deps\include"

:: ---- API CONFIG ----
:: AI build triage uses Anthropic's Claude. Put your key in api_keys.bat (created on first run).
if not exist api_keys.bat (
    echo @echo off > api_keys.bat
    echo set "ANTHROPIC_API_KEY=YOUR_ANTHROPIC_KEY_HERE" >> api_keys.bat
    echo set "CLAUDE_MODEL=claude-opus-4-8" >> api_keys.bat
)
call api_keys.bat
:: Default the model when an older api_keys.bat does not define it.
if "%CLAUDE_MODEL%"=="" set "CLAUDE_MODEL=claude-opus-4-8"

if exist "%LOG_FILE%" del "%LOG_FILE%"

echo.
echo ==============================================================
echo   SDR Trunk Kennebec - Multi-Platform Build and Release
echo ==============================================================
echo.

:: ============================================================================
:: STEP 1: Environment Check
:: ============================================================================
call :drawProgressBar 3 "Checking environment..."
where git >nul 2>&1 || (echo [ERROR] Git is not installed. & pause & exit)
where gh >nul 2>&1 || (echo [ERROR] GitHub CLI is not installed. & pause & exit)
where cmake >nul 2>&1 || (echo [ERROR] CMake is not installed. & pause & exit)
where g++ >nul 2>&1 || (echo [ERROR] MinGW/g++ is not installed. & pause & exit)
if "!JAVA_HOME!"=="" (echo [ERROR] JAVA_HOME is not set. & pause & exit)
for %%I in ("!JAVA_HOME!") do set "JH=%%~sI"

:: Check for jpackage
set "JPACKAGE_EXE=!JAVA_HOME!\bin\jpackage.exe"
if not exist "!JPACKAGE_EXE!" (
    echo [WARNING] jpackage.exe not found in JAVA_HOME. Native installer step will be skipped.
    set "HAS_JPACKAGE=0"
) else (
    echo [OK] jpackage found at !JPACKAGE_EXE!
    set "HAS_JPACKAGE=1"
)

:: Check for WiX Toolset
if exist "%USERPROFILE%\wix311\light.exe" (
    set "PATH=%USERPROFILE%\wix311;!PATH!"
    echo [OK] WiX Toolset found locally and added to PATH.
) else if exist "C:\Program Files (x86)\WiX Toolset v3.11\bin\light.exe" (
    set "PATH=C:\Program Files (x86)\WiX Toolset v3.11\bin;!PATH!"
    echo [OK] WiX Toolset found globally and added to PATH.
) else (
    echo [WARNING] WiX Toolset not found in standard paths. Native installer creation may fail.
)

:: ============================================================================
:: STEP 2: Libvolk Dependency Check
:: ============================================================================
call :drawProgressBar 6 "Checking libvolk dependency..."
if not exist "%VOLK_BASE%\volk\volk.h" (
    echo [INFO] libvolk missing. Cloning v3.3.0 with submodules...
    git clone --branch v3.3.0 --recursive https://github.com/gnuradio/volk.git volk_src >nul 2>&1
    if not exist "volk_src" (
        echo [ERROR] Failed to clone libvolk.
        pause
        goto ai_triage
    )

    echo [INFO] Compiling libvolk...
    python -m pip install mako >nul 2>&1
    cd /d "volk_src"
    if not exist build mkdir build
    cd build
    cmake -G "MinGW Makefiles" -DCMAKE_INSTALL_PREFIX="%VOLK_BASE%\.." .. > cmake_out.log 2>&1
    cmake --build . --target install >> cmake_out.log 2>&1
    cd /d "%ROOT_DIR%"

    if not exist "%VOLK_BASE%\volk\volk.h" (
        echo [ERROR] libvolk compilation or installation failed. Check volk_src\build\cmake_out.log
        pause
        goto ai_triage
    )
    rmdir /s /q volk_src >nul 2>&1
)

:: ============================================================================
:: STEP 3 & 4: Workspace Cleanup & Clone/Update Repository
:: ============================================================================
call :drawProgressBar 10 "Updating Kennebec Fork..."
:: Kill SDRTrunk window if open
taskkill /F /FI "WINDOWTITLE eq SDRTrunk*" /T >nul 2>&1

:: Gracefully stop Gradle daemon first
if exist "%FOLDER_NAME%\gradlew.bat" (
    cd /d "%ROOT_DIR%\%FOLDER_NAME%" 2>nul
    call gradlew.bat --stop >nul 2>&1
    cd /d "%ROOT_DIR%" 2>nul
)

:: Aggressively kill any remaining Java processes to ensure no file locks interfere with compilation
taskkill /F /IM java.exe /T >nul 2>&1
taskkill /F /IM javaw.exe /T >nul 2>&1
taskkill /F /IM SDRTrunk.exe /T >nul 2>&1
taskkill /F /IM sdr-trunk.exe /T >nul 2>&1

:: OPTIMIZATION: Fetch and reset instead of re-cloning to save time & keep Gradle cache
if not exist "%FOLDER_NAME%" (
    git clone --progress %REPO_URL% "%FOLDER_NAME%" 2> "%LOG_FILE%"
    if !ERRORLEVEL! NEQ 0 goto ai_triage
) else (
    cd /d "%ROOT_DIR%\%FOLDER_NAME%" 2>nul
    git fetch origin master >nul 2>&1

    :: Sync the build workspace to origin/master when it is behind. Use 'git reset --hard', which
    :: advances HEAD - the previous code only 'git checkout'-ed the build script into the working tree
    :: without moving HEAD, so the 'git diff HEAD origin/master' check below stayed dirty forever and
    :: the script restarted itself in an endless close/reopen loop. No restart is needed: the running
    :: script is the outer copy (%~f0), not this clone, so resetting the clone cannot corrupt execution.
    git diff --quiet HEAD origin/master 2>nul
    if errorlevel 1 (
        echo [INFO] Updates found on origin/master - syncing the build workspace...
        git reset --hard origin/master >nul 2>&1
    )
    cd /d "%ROOT_DIR%" 2>nul
)

cd /d "%ROOT_DIR%\%FOLDER_NAME%"

:: Absolute path to the cloned project + its Gradle wrapper. Every Gradle step below re-asserts this
:: working directory and invokes the wrapper by absolute path, so a restart, a standalone launch folder
:: (where ROOT_DIR has no gradlew.bat), or a stray cwd change can no longer cause
:: "'gradlew.bat' is not recognized as an internal or external command".
set "PROJ_DIR=%ROOT_DIR%\%FOLDER_NAME%"
set "GRADLEW=%PROJ_DIR%\gradlew.bat"
if not exist "%GRADLEW%" (
    echo [ERROR] Gradle wrapper not found at "%GRADLEW%".
    echo [ERROR] The repository clone appears incomplete - delete the "%FOLDER_NAME%" folder and re-run.
    goto ai_triage
)

echo [INFO] Performing case-sensitivity sweep...
powershell -Command "Get-ChildItem -Path . -Recurse | Group-Object {$_.FullName.ToLower()} | Where-Object {$_.Count -gt 1} | ForEach-Object { $_.Group[1] | Remove-Item -Force }" >nul 2>&1

:: ============================================================================
:: STEP 5: Read Project Version
:: ============================================================================
for /f "tokens=2 delims==" %%A in ('findstr /I "^projectVersion" gradle.properties') do set "PROJ_VER=%%A"
for /f "tokens=* delims= " %%B in ("!PROJ_VER!") do set "PROJ_VER=%%B"
echo [INFO] Project Version: !PROJ_VER!

set "APP_VER=!PROJ_VER!"
for /f "tokens=* delims=ABCDEFGHIJKLMNOPQRSTUVWXYZ." %%V in ("!APP_VER!") do set "APP_VER=%%V"
if "!APP_VER!"=="" set "APP_VER=0.0.1"

set "RELEASE_DIR=%ROOT_DIR%\%FOLDER_NAME%\build\releases"

:: ============================================================================
:: STEP 6: Gradle Init (Compile)
:: ============================================================================
call :drawProgressBar 25 "Initializing Gradle (compile)..."

:: Best-effort: exclude the build workspace from Windows Defender real-time scanning. Defender
:: (or another AV) holding freshly-written jars open is the most common cause of 'gradle clean'
:: failing with "New files were found ... a process is still writing to the target directory".
:: Requires admin; ignored silently if it cannot be applied.
powershell -NoProfile -Command "try { Add-MpPreference -ExclusionPath '%ROOT_DIR%\%FOLDER_NAME%' -ErrorAction Stop; Write-Host '[OK] Added Windows Defender exclusion for the build workspace.' } catch { Write-Host '[INFO] Could not add a Defender exclusion (run as administrator to enable) - continuing.' }" 2>nul

:: Clean + compile with automatic retry. If 'clean' cannot delete build\install\sdr-trunk\lib
:: jars because a process or antivirus is locking them, release the locks and retry.
:: OPTIMIZATION: Removed --no-daemon for much faster consecutive builds
set "CLEAN_ATTEMPT=0"
:clean_retry
set /a CLEAN_ATTEMPT+=1
echo [INFO] Clean/compile attempt !CLEAN_ATTEMPT! of 3...
cd /d "%PROJ_DIR%"
call "%GRADLEW%" clean classes --console=plain > gradle_out.log 2>&1
findstr /C:"BUILD SUCCESSFUL" gradle_out.log >nul
if !ERRORLEVEL! EQU 0 goto clean_ok
if !CLEAN_ATTEMPT! GEQ 3 (
    type gradle_out.log >> "%LOG_FILE%"
    echo [ERROR] Clean/compile failed after !CLEAN_ATTEMPT! attempts.
    goto ai_triage
)
echo [WARNING] Attempt !CLEAN_ATTEMPT! failed - releasing file locks on the build directory and retrying...
call :releaseBuildLocks
goto clean_retry
:clean_ok
type gradle_out.log >> "%LOG_FILE%"

if not exist "!RELEASE_DIR!" mkdir "!RELEASE_DIR!"

:: ============================================================================
:: STEP 7: C++ Native Library Compilation
:: ============================================================================
call :drawProgressBar 35 "Compiling Native C++ Library..."
if not exist "src\main\resources\native" mkdir "src\main\resources\native"

set "JNI_RAW=%ROOT_DIR%\%FOLDER_NAME%\build\generated\sources\headers\java\main"
if not exist "!JNI_RAW!" mkdir "!JNI_RAW!"
for %%I in ("!JNI_RAW!") do set "JNI_GEN=%%~sI"
for %%I in ("%VOLK_BASE%") do set "V_INC=%%~sI"
for %%I in ("%VOLK_BASE%\..\lib") do set "V_LIB=%%~sI"

g++ -shared -fPIC -I"!JH!\include" -I"!JH!\include\win32" -I"!JNI_GEN!" -I"src\main\cpp" -I"!V_INC!" -L"!V_LIB!" src\main\cpp\library.cpp -lvolk -o src\main\resources\native\library.dll 2> cpp_error.log
if !ERRORLEVEL! NEQ 0 (
    type cpp_error.log >> "%LOG_FILE%"
    echo [WARNING] C++ compilation failed. Using Java fallback.
)

:: ============================================================================
:: STEP 8: Build Windows Runtime Package
:: ============================================================================
call :drawProgressBar 45 "Building Windows runtime package..."
cd /d "%PROJ_DIR%"
call "%GRADLEW%" runtimeZipCurrent createExe -x test -x javadoc -x compileJni --console=plain > build_win.log 2>&1
type build_win.log >> "%LOG_FILE%"
findstr /C:"BUILD SUCCESSFUL" build_win.log >nul
if !ERRORLEVEL! EQU 0 (
    echo [OK] Windows runtime package built.
    for %%F in (build\image\*.zip) do (
        copy "%%F" "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-windows-x86_64.zip" >nul
        echo [OK] Staged: SDRTrunk-!PROJ_VER!-windows-x86_64.zip
    )
) else (
    echo [WARNING] Windows runtime build failed. Falling back to distZip...
    cd /d "%PROJ_DIR%"
    call "%GRADLEW%" build distZip -x test -x javadoc -x compileJni --console=plain > build_fallback.log 2>&1
    type build_fallback.log >> "%LOG_FILE%"
    findstr /C:"BUILD SUCCESSFUL" build_fallback.log >nul || goto ai_triage
    for %%F in (build\distributions\*.zip) do (
        copy "%%F" "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-windows-x86_64.zip" >nul
    )
)

:: ============================================================================
:: STEP 9: Build Windows Native Installer (.exe)
:: ============================================================================
if "!HAS_JPACKAGE!"=="1" (
    call :drawProgressBar 55 "Creating Windows native installer..."
    cd /d "%PROJ_DIR%"
    call "%GRADLEW%" createInstaller -x test -x javadoc -x compileJni --console=plain > build_installer.log 2>&1
    type build_installer.log >> "%LOG_FILE%"
    findstr /C:"BUILD SUCCESSFUL" build_installer.log >nul
    if !ERRORLEVEL! EQU 0 (
        for %%F in (build\installer\*.exe) do (
            copy "%%F" "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-windows-installer.exe" >nul
            echo [OK] Staged: SDRTrunk-!PROJ_VER!-windows-installer.exe
        )
        for %%F in (build\installer\*.msi) do (
            copy "%%F" "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-windows-installer.msi" >nul
            echo [OK] Staged: SDRTrunk-!PROJ_VER!-windows-installer.msi
        )
    ) else (
        echo [WARNING] Native installer creation failed. Portable zip is still available.
    )
) else (
    call :drawProgressBar 55 "Skipping native installer (jpackage not available)..."
)

:: ============================================================================
:: STEP 10: Build Cross-Platform Runtime Packages (Linux + macOS)
:: ============================================================================
call :drawProgressBar 65 "Building Linux and macOS runtime packages..."
cd /d "%PROJ_DIR%"
call "%GRADLEW%" runtimeZipOthers -x test -x javadoc -x compileJni --console=plain > build_others.log 2>&1
type build_others.log >> "%LOG_FILE%"
findstr /C:"BUILD SUCCESSFUL" build_others.log >nul
if !ERRORLEVEL! EQU 0 (
    echo [OK] Cross-platform runtime packages built.
    for %%F in (build\image\*.zip) do (
        set "ZIPNAME=%%~nxF"
        echo !ZIPNAME! | findstr /I "linux.*aarch64" >nul
        if !ERRORLEVEL! EQU 0 (
            copy "%%F" "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-linux-aarch64.zip" >nul
            echo [OK] Staged: SDRTrunk-!PROJ_VER!-linux-aarch64.zip
        )
        echo !ZIPNAME! | findstr /I "linux.*x86_64 linux.*amd64" >nul
        if !ERRORLEVEL! EQU 0 (
            copy "%%F" "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-linux-x86_64.zip" >nul
            echo [OK] Staged: SDRTrunk-!PROJ_VER!-linux-x86_64.zip
        )
        echo !ZIPNAME! | findstr /I "osx.*aarch64" >nul
        if !ERRORLEVEL! EQU 0 (
            copy "%%F" "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-macos-aarch64.zip" >nul
            echo [OK] Staged: SDRTrunk-!PROJ_VER!-macos-aarch64.zip
        )
        echo !ZIPNAME! | findstr /I "osx.*x86_64 osx.*amd64" >nul
        if !ERRORLEVEL! EQU 0 (
            copy "%%F" "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-macos-x86_64.zip" >nul
            echo [OK] Staged: SDRTrunk-!PROJ_VER!-macos-x86_64.zip
        )
    )
) else (
    echo [WARNING] Cross-platform builds failed. Only Windows package will be released.
)

:: ============================================================================
:: STEP 11: Local Installation (Windows)
:: ============================================================================
call :drawProgressBar 80 "Installing locally..."
cd /d "%PROJ_DIR%"
call "%GRADLEW%" installDist -x test -x javadoc -x compileJni --console=plain >> "%LOG_FILE%" 2>&1
if !ERRORLEVEL! NEQ 0 goto ai_triage

:: ============================================================================
:: STEP 12: Upload to GitHub Release
:: ============================================================================
call :drawProgressBar 90 "Creating GitHub Release..."
set "ASSET_COUNT=0"
set "ASSET_LIST="
for %%F in ("!RELEASE_DIR!\*.*") do (
    set /a ASSET_COUNT+=1
    set "ASSET_LIST=!ASSET_LIST! "%%F""
)

echo.
echo ==============================================================
echo   Release Artifacts for !PROJ_VER! - !ASSET_COUNT! file(s)
echo ==============================================================
for %%F in ("!RELEASE_DIR!\*.*") do (
    echo   * %%~nxF
)
echo ==============================================================
echo.

if !ASSET_COUNT! GTR 0 (
    > "!RELEASE_DIR!\release_notes.md" echo ## SDRTrunk Kennebec !PROJ_VER!
    >> "!RELEASE_DIR!\release_notes.md" echo.
    >> "!RELEASE_DIR!\release_notes.md" echo ### Downloads
    >> "!RELEASE_DIR!\release_notes.md" echo.
    >> "!RELEASE_DIR!\release_notes.md" echo ^| Platform ^| Type ^| File ^|
    >> "!RELEASE_DIR!\release_notes.md" echo ^| ---------- ^| ------ ^| ------ ^|
    if exist "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-windows-installer.exe" (
        >> "!RELEASE_DIR!\release_notes.md" echo ^| Windows x64 ^| Installer ^(.exe^) ^| SDRTrunk-!PROJ_VER!-windows-installer.exe ^|
    )
    if exist "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-windows-x86_64.zip" (
        >> "!RELEASE_DIR!\release_notes.md" echo ^| Windows x64 ^| Portable ^(.zip^) ^| SDRTrunk-!PROJ_VER!-windows-x86_64.zip ^|
    )
    if exist "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-linux-x86_64.zip" (
        >> "!RELEASE_DIR!\release_notes.md" echo ^| Linux x64 ^| Portable ^(.zip^) ^| SDRTrunk-!PROJ_VER!-linux-x86_64.zip ^|
    )
    if exist "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-linux-aarch64.zip" (
        >> "!RELEASE_DIR!\release_notes.md" echo ^| Linux ARM64 ^| Portable ^(.zip^) ^| SDRTrunk-!PROJ_VER!-linux-aarch64.zip ^|
    )
    if exist "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-macos-x86_64.zip" (
        >> "!RELEASE_DIR!\release_notes.md" echo ^| macOS x64 ^| Portable ^(.zip^) ^| SDRTrunk-!PROJ_VER!-macos-x86_64.zip ^|
    )
    if exist "!RELEASE_DIR!\SDRTrunk-!PROJ_VER!-macos-aarch64.zip" (
        >> "!RELEASE_DIR!\release_notes.md" echo ^| macOS ARM64 ^(Apple Silicon^) ^| Portable ^(.zip^) ^| SDRTrunk-!PROJ_VER!-macos-aarch64.zip ^|
    )
    >> "!RELEASE_DIR!\release_notes.md" echo.
    >> "!RELEASE_DIR!\release_notes.md" echo ### Installation
    >> "!RELEASE_DIR!\release_notes.md" echo.
    >> "!RELEASE_DIR!\release_notes.md" echo - **Windows Installer**: Download the .exe installer and double-click to install.
    >> "!RELEASE_DIR!\release_notes.md" echo   - ^> **Note**: Because this is an open-source project without a paid code signing certificate, Windows SmartScreen may show an "Unknown Publisher" warning. To proceed, click **"More info"**, then click **"Run anyway"**.
    >> "!RELEASE_DIR!\release_notes.md" echo - **Portable Zip**: Extract the zip and run `bin\sdr-trunk.bat` ^(Windows^) or `bin/sdr-trunk` ^(Linux/macOS^).
    >> "!RELEASE_DIR!\release_notes.md" echo - **Windows Portable**: You can also launch via `SDRTrunk.exe` in the extracted folder.
    >> "!RELEASE_DIR!\release_notes.md" echo.
    >> "!RELEASE_DIR!\release_notes.md" echo Bundled with a complete Java runtime - no separate JDK installation required.

    gh release delete !PROJ_VER! --repo %GH_REPO% --yes >nul 2>&1
    git tag -d !PROJ_VER! >nul 2>&1
    git push origin :refs/tags/!PROJ_VER! >nul 2>&1

    echo [INFO] Creating GitHub Release !PROJ_VER! with !ASSET_COUNT! assets...
    gh release create !PROJ_VER! !ASSET_LIST! --repo %GH_REPO% --title "SDRTrunk Kennebec !PROJ_VER!" --notes-file "!RELEASE_DIR!\release_notes.md"

    if !ERRORLEVEL! EQU 0 (
        echo [OK] GitHub Release created successfully!
    ) else (
        echo [WARNING] GitHub Release creation failed. Assets are in: !RELEASE_DIR!
    )
    del "!RELEASE_DIR!\release_notes.md" >nul 2>&1
) else (
    echo [WARNING] No release artifacts found. Skipping GitHub release.
)

:: ============================================================================
:: STEP 13: Update README.md
:: ============================================================================
call :drawProgressBar 95 "Updating README..."
cd /d "%ROOT_DIR%\%FOLDER_NAME%"
powershell -Command "(Get-Content README.md) -replace 'K\.\d{2}\.\d{3}', '!PROJ_VER!' | Set-Content README.md"
git add README.md
git commit -m "Update README.md with release !PROJ_VER!" >nul 2>&1
git push origin HEAD >nul 2>&1

:: ============================================================================
:: STEP 14: SUCCESS
:: ============================================================================
call :drawProgressBar 100 "Build Complete!"
echo.
echo ==============================================================
echo   BUILD SUCCESSFUL - SDRTrunk Kennebec !PROJ_VER!
echo ==============================================================
echo.
echo   Release: https://github.com/%GH_REPO%/releases/tag/!PROJ_VER!
echo.
echo   Local artifacts:
for %%F in ("!RELEASE_DIR!\*.*") do (
    echo     * %%~nxF
)
echo.
echo   Local install: %ROOT_DIR%\%FOLDER_NAME%\build\install\sdr-trunk\
echo ==============================================================
echo.

cd /d "%ROOT_DIR%\%FOLDER_NAME%"
for /d %%D in (build\install\*) do (
    if exist "%%D\bin\sdr-trunk.bat" (
        echo [INFO] Launching SDRTrunk...
        start "" "%%D\bin\sdr-trunk.bat"
    )
)
pause
exit /b 0

:: ============================================================================
:: UTILITY: Progress Bar
:: ============================================================================
:drawProgressBar
set "pc=%~1" & set "status=%~2"
set /a "filled=pc/5" & set /a "empty=20-filled"
set "bar="
for /l %%i in (1,1,%filled%) do set "bar=!bar!#"
for /l %%i in (1,1,%empty%) do set "bar=!bar!-"
echo.
echo ==========================================
echo Progress: [!bar!] %pc%%%
echo Status: !status!
echo ==========================================
exit /b

:: ============================================================================
:: UTILITY: Release file locks on the build directory
:: ============================================================================
:releaseBuildLocks
:: Stop the Gradle daemon and kill any process that may hold build\install jars open
:: (a previously launched build, a stray JVM, the packaged app, or an indexer).
if defined GRADLEW (call "%GRADLEW%" --stop >nul 2>&1) else (call gradlew.bat --stop >nul 2>&1)
taskkill /F /IM java.exe /T >nul 2>&1
taskkill /F /IM javaw.exe /T >nul 2>&1
taskkill /F /IM sdr-trunk.exe /T >nul 2>&1
taskkill /F /IM SDRTrunk.exe /T >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq SDRTrunk*" /T >nul 2>&1
:: Give the OS / antivirus a moment to release file handles
powershell -NoProfile -Command "Start-Sleep -Seconds 3" >nul 2>&1
:: Force-remove the directories that hold the locked, previously-built application jars so the
:: next 'gradle clean' has less to delete.
for %%D in (build\install build\image build\installer build\distributions) do (
    if exist "%%D" rmdir /s /q "%%D" >nul 2>&1
)
exit /b

:: ============================================================================
:: ERROR HANDLER: AI Triage
:: ============================================================================
:ai_triage
cd /d "%ROOT_DIR%"
echo ==========================================
echo BUILD FAILED - VIEWING RECENT LOGS
echo ==========================================
powershell -Command "if (Test-Path 'build_log.txt') { Get-Content 'build_log.txt' -Tail 20 }"
echo ==========================================
echo Attempting AI Triage (Claude)...
echo ==========================================

if "%ANTHROPIC_API_KEY%"=="" (
    echo [INFO] ANTHROPIC_API_KEY is not set - skipping AI triage.
    echo [INFO] Add  set "ANTHROPIC_API_KEY=sk-ant-..."  to api_keys.bat to enable Claude triage.
    pause
    exit /b 1
)
if "%ANTHROPIC_API_KEY%"=="YOUR_ANTHROPIC_KEY_HERE" (
    echo [INFO] ANTHROPIC_API_KEY is still the placeholder - skipping AI triage.
    echo [INFO] Edit api_keys.bat and set your real Anthropic API key to enable Claude triage.
    pause
    exit /b 1
)

:: Build a PowerShell script that asks Claude (Anthropic Messages API) to triage the build log.
> triage.ps1 echo [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
>> triage.ps1 echo $log = if (Test-Path 'build_log.txt') { Get-Content 'build_log.txt' -Tail 60 ^| Out-String } else { 'No log available.' }
>> triage.ps1 echo $log = $log -replace '[^^^\x20-\x7E\r\n\t]', ''
>> triage.ps1 echo $prompt = "You are a senior build engineer for the SDRTrunk Kennebec project (Java 23 / Gradle / JavaFX, built on Windows with jpackage and the Beryx runtime plugin). A build run failed. Analyze the tail of the build log below and respond with a concise, numbered, actionable diagnosis and fix. State clearly whether the root cause is the repository code or the local build environment/script. Build log:`n`n" + $log
>> triage.ps1 echo $body = @{ model = '%CLAUDE_MODEL%'; max_tokens = 1024; messages = @( @{ role = 'user'; content = $prompt } ) } ^| ConvertTo-Json -Depth 10
>> triage.ps1 echo $headers = @{ 'x-api-key' = '%ANTHROPIC_API_KEY%'; 'anthropic-version' = '2023-06-01' }
>> triage.ps1 echo try {
>> triage.ps1 echo   $res = Invoke-RestMethod -Method Post -Uri 'https://api.anthropic.com/v1/messages' -Headers $headers -ContentType 'application/json' -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
>> triage.ps1 echo   $text = ($res.content ^| Where-Object { $_.type -eq 'text' } ^| ForEach-Object { $_.text }) -join "`n"
>> triage.ps1 echo   Write-Host ''
>> triage.ps1 echo   Write-Host '===== CLAUDE BUILD TRIAGE =====' -ForegroundColor Cyan
>> triage.ps1 echo   Write-Host $text
>> triage.ps1 echo   Write-Host '===============================' -ForegroundColor Cyan
>> triage.ps1 echo } catch {
>> triage.ps1 echo   Write-Host "AI Error: $($_.Exception.Message)" -ForegroundColor Red
>> triage.ps1 echo   if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message -ForegroundColor DarkGray }
>> triage.ps1 echo }

powershell -NoProfile -ExecutionPolicy Bypass -File triage.ps1
if exist triage.ps1 del triage.ps1
pause
exit /b 1
