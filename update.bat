@echo off
REM One-tap updater for Pop & Grow.
REM Builds the app and installs it on the connected phone, then relaunches it.
REM
REM How to use: plug the phone in (USB debugging on), then either
REM   - double-click update.bat in Explorer, or
REM   - open Command Prompt in this folder and run:  update.bat

setlocal enableextensions

REM --- Find JDK 17 (any Adoptium build, no hardcoded version) ---------------
if "%JAVA_HOME%"=="" (
    for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%~fJ"
)
if "%JAVA_HOME%"=="" (
    echo [X] JDK 17 not found. Install it from https://adoptium.net then re-run.
    pause
    exit /b 1
)

REM --- Android SDK ----------------------------------------------------------
if "%ANDROID_HOME%"=="" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

echo.
echo === Bubble Learn Kids updater ===
echo JDK: %JAVA_HOME%
echo SDK: %ANDROID_HOME%
echo.

REM --- Is a phone connected and authorized? ---------------------------------
adb start-server >nul 2>&1
set "DEVICE_OK="
for /f "skip=1 tokens=1,2" %%A in ('adb devices') do (
    if "%%B"=="device" set "DEVICE_OK=1"
    if "%%B"=="unauthorized" echo [!] Phone found but UNAUTHORIZED - tap "Allow" on the phone, then re-run.
)
if not defined DEVICE_OK (
    echo [X] No phone ready. Plug it in with USB debugging on, tap Allow, then re-run.
    pause
    exit /b 1
)

REM --- Build and install ----------------------------------------------------
cd /d "%~dp0"
call gradlew.bat installDebug
if errorlevel 1 (
    echo.
    echo [X] Build or install failed - read the error above.
    pause
    exit /b 1
)

REM --- Restart the app fresh on the phone -----------------------------------
adb shell am force-stop com.meetpatel.popgrow.debug
adb shell am start -n com.meetpatel.popgrow.debug/com.meetpatel.popgrow.MainActivity >nul

echo.
echo [OK] Updated and launched on the phone. Have fun!
pause
endlocal
