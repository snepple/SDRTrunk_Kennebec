@echo off
setlocal EnableDelayedExpansion

:: Persistence Guard
if not defined TERMINAL_FIXED (
    set TERMINAL_FIXED=1
    start cmd /k "%~f0"
    exit
)

cd /d "%~dp0"
title SDR Trunk Kennebec Updater - Sam N.

:: API CONFIG
if not exist api_keys.bat (
    echo @echo off > api_keys.bat
    echo set "JULES_API_KEY=AQ.Ab8RN6IY3pZPoo14Q-HqZ_K8TvdS7ILeiJzpzkJAIL_PRnIMSA" >> api_keys.bat
    echo set "GEMINI_API_KEY=AIzaSyCV1CqyDZTjFH_E_FHljTnqTRxAZ91SD4s" >> api_keys.bat
    echo set "GEMINI_MODEL=gemini-1.5-flash" >> api_keys.bat
)
call api_keys.bat

:: GITHUB CONFIG
set "GH_REPO=snepple/SDRTrunk_Kennebec"
set "REPO_URL=https://github.com/%GH_REPO%.git"
set "FOLDER_NAME=SDRTrunk_Kennebec_Build"
set "ROOT_DIR=%CD%"
set "LOG_FILE=%ROOT_DIR%\build_log.txt"
set "VOLK_BASE=C:\SDR_Deps\include"

if exist "%LOG_FILE%" del "%LOG_FILE%"

echo ==========================================
echo SDR Trunk Kennebec Updater - AFD Station
echo ==========================================

:: Step 1: Env Check
call :drawProgressBar 5 "Checking environment..."
where gh >nul 2>&1 || (echo [ERROR] GitHub CLI missing. & pause & exit)
where g++ >nul 2>&1 || (echo [ERROR] MinGW/g++ missing. & pause & exit)
if "!JAVA_HOME!"=="" (echo [ERROR] JAVA_HOME not set. & pause & exit)
for %%I in ("!JAVA_HOME!") do set "JH=%%~sI"

:: Libvolk Dependency Check
call :drawProgressBar 10 "Checking libvolk dependency..."
if not exist "%VOLK_BASE%\volk\volk.h" (
    echo [INFO] libvolk missing. Downloading v3.3.0...
    powershell -Command "Invoke-WebRequest -Uri 'https://api.github.com/repos/gnuradio/volk/zipball/v3.3.0' -OutFile 'volk.zip'" >nul 2>&1
    if not exist "volk.zip" (
        echo [ERROR] Failed to download libvolk.
        pause
        goto ai_triage
    )
    echo [INFO] Extracting libvolk...
    powershell -Command "Expand-Archive -Path 'volk.zip' -DestinationPath 'volk_src' -Force" >nul 2>&1

    echo [INFO] Compiling libvolk...
    for /d %%D in (volk_src\*) do set "VOLK_DIR=%%D"
    cd /d "!VOLK_DIR!"
    if not exist build mkdir build
    cd build
    cmake -G "MinGW Makefiles" -DCMAKE_INSTALL_PREFIX="%VOLK_BASE%\.." .. >nul 2>&1
    cmake --build . --target install >nul 2>&1
    cd /d "%ROOT_DIR%"

    rmdir /s /q volk_src >nul 2>&1
    del volk.zip >nul 2>&1

    if not exist "%VOLK_BASE%\volk\volk.h" (
        echo [ERROR] libvolk installation failed.
        pause
        goto ai_triage
    )
)

:: Step 2: Cleanup
call :drawProgressBar 15 "Cleaning workspace..."
taskkill /F /FI "IMAGENAME eq java.exe" /T >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq SDRTrunk*" /T >nul 2>&1
if exist "%FOLDER_NAME%" rmdir /s /q "%FOLDER_NAME%"

:: Step 3: Clone & Deep Resource Fix
call :drawProgressBar 25 "Cloning Kennebec Fork..."
git clone --progress %REPO_URL% "%FOLDER_NAME%" 2> "%LOG_FILE%" || goto ai_triage

cd /d "%ROOT_DIR%\%FOLDER_NAME%"

echo [INFO] Performing case-sensitivity sweep...
powershell -Command "Get-ChildItem -Path . -Recurse | Group-Object {$_.FullName.ToLower()} | Where-Object {$_.Count -gt 1} | ForEach-Object { $_.Group[1] | Remove-Item -Force }" >nul 2>&1

:: Step 4: Gradle Init
call :drawProgressBar 40 "Initializing Gradle..."
call gradlew.bat clean classes --no-daemon --console=plain > gradle_out.log 2>&1
type gradle_out.log >> "%LOG_FILE%"
findstr /C:"BUILD SUCCESSFUL" gradle_out.log >nul || goto ai_triage

:: Step 5: C++ Compilation
call :drawProgressBar 60 "Compiling Native Library..."
if not exist "src\main\resources\native" mkdir "src\main\resources\native"








set "JNI_RAW=%ROOT_DIR%\%FOLDER_NAME%\build\generated\sources\headers\java\main"
if not exist "!JNI_RAW!" mkdir "!JNI_RAW!"
for %%I in ("!JNI_RAW!") do set "JNI_GEN=%%~sI"
for %%I in ("%VOLK_BASE%") do set "V_INC=%%~sI"
for %%I in ("%VOLK_BASE%\..\lib") do set "V_LIB=%%~sI"

g++ -shared -fPIC -I"!JH!\include" -I"!JH!\include\win32" -I"!JNI_GEN!" -I"src\main\cpp" -I"!V_INC!" -L"!V_LIB!" src\main\cpp\library.cpp -lvolk -o src\main\resources\native\library.dll 2> cpp_error.log
if !ERRORLEVEL! NEQ 0 (
    type cpp_error.log >> "%LOG_FILE%"
    goto ai_triage
)

:: Step 6: Final Packaging
call :drawProgressBar 75 "Packaging Distribution..."
call gradlew.bat build distZip -x test -x javadoc -x compileJni --no-daemon --console=plain > build_out.log 2>&1
type build_out.log >> "%LOG_FILE%"
findstr /C:"BUILD SUCCESSFUL" build_out.log >nul || goto ai_triage

:: Step 7: Local Installation
call :drawProgressBar 85 "Installing..."
:: Added -x compileJni here to ensure it doesn't re-trigger and fail
call gradlew.bat installDist -x test -x javadoc -x compileJni --no-daemon --console=plain >> "%LOG_FILE%" 2>&1 || goto ai_triage

:: Step 8: Success
call :drawProgressBar 100 "Build Complete!"
echo.
echo [SUCCESS] Sam, Kennebec build is ready.
for /d %%D in (build\install\*) do (if exist "%%D\bin\sdrtrunk.bat" start "" "%%D\bin\sdrtrunk.bat")
pause
exit /b 0

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

:ai_triage
cd /d "%ROOT_DIR%"
echo ==========================================
echo BUILD FAILED - VIEWING RECENT LOGS
echo ==========================================
powershell -Command "if (Test-Path 'build_log.txt') { Get-Content 'build_log.txt' -Tail 15 }"
echo ==========================================
echo Attempting AI Triage...
echo ==========================================

echo [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 > triage.ps1
echo $log = if (Test-Path 'build_log.txt') { Get-Content 'build_log.txt' -Tail 30 ^| Out-String } else { 'No log' } >> triage.ps1
:: Corrected the caret escape [^^^] to preserve the character in the PS1 script
echo $log = $log -replace '[^^^\x20-\x7E]', '' -replace [char]34, ' ' -replace '\\', '/' >> triage.ps1
echo $instr = 'SDRTrunk build failed. Log: ' + $log + '. Return JSON ONLY: {"target":"REPO|SCRIPT","content":"Fix advice"}' >> triage.ps1
echo $body = @{ contents = @( @{ parts = @( @{ text = $instr } ) } ) } ^| ConvertTo-Json -Depth 10 >> triage.ps1
echo try { >> triage.ps1
echo   $gUrl = "https://generativelanguage.googleapis.com/v1/models/%GEMINI_MODEL%:generateContent?key=%GEMINI_API_KEY%" >> triage.ps1
echo   $res = Invoke-RestMethod -Method Post -Uri $gUrl -ContentType 'application/json' -Body $body >> triage.ps1
echo   $json = $res.candidates[0].content.parts[0].text -replace '```json', '' -replace '```', '' >> triage.ps1
echo   $dec = $json ^| ConvertFrom-Json >> triage.ps1
echo   Write-Host "[AI DECISION] Target: $($dec.target)" -ForegroundColor Cyan >> triage.ps1
echo   if ($dec.target -eq 'REPO') { >> triage.ps1
echo     $jBody = @{ prompt=$dec.content; sourceContext=@{source="sources/github/%GH_REPO%"; githubRepoContext=@{startingBranch='master'}}; automationMode='AUTO_CREATE_PR'; title='Fix Build' } ^| ConvertTo-Json -Depth 10 >> triage.ps1
echo     $jRes = Invoke-RestMethod -Method Post -Uri 'https://jules.googleapis.com/v1alpha/sessions' -Headers @{'X-Goog-Api-Key'='%JULES_API_KEY%'; 'Content-Type'='application/json'} -Body $jBody >> triage.ps1
echo     Write-Host "[SUCCESS] Jules ID: $($jRes.id)" -ForegroundColor Green >> triage.ps1
echo   } else { Write-Host '[ADVICE]' -ForegroundColor Yellow; Write-Host $dec.content } >> triage.ps1
echo } catch { Write-Host "AI Error: $($_.Exception.Message)" } >> triage.ps1

powershell -NoProfile -ExecutionPolicy Bypass -File triage.ps1
if exist triage.ps1 del triage.ps1
pause
exit /b 1
