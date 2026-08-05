@echo off
REM Windows. Run this before building, from Command Prompt:
REM
REM     env.bat
REM
REM Edit JAVA_HOME below if your JDK 17 is somewhere else. Check with:
REM     dir "C:\Program Files\Eclipse Adoptium"
REM     dir "C:\Program Files\Java"
REM
REM If you installed Android Studio, the SDK is normally already at the
REM ANDROID_HOME path below. Verify in Studio under:
REM     Settings -> Languages ^& Frameworks -> Android SDK -> "Android SDK Location"

if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
if "%ANDROID_HOME%"=="" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"

set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\emulator;%ANDROID_HOME%\cmdline-tools\latest\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo java : NOT FOUND at "%JAVA_HOME%"
    echo         Install JDK 17, then edit JAVA_HOME at the top of env.bat
) else (
    for /f "tokens=*" %%i in ('java -version 2^>^&1 ^| findstr /i version') do echo java : %%i
)

if not exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo adb  : NOT FOUND at "%ANDROID_HOME%"
    echo         Install the Android SDK, then edit ANDROID_HOME at the top of env.bat
) else (
    for /f "tokens=*" %%i in ('adb version 2^>^&1 ^| findstr /i "Android Debug Bridge"') do echo adb  : %%i
    echo sdk  : %ANDROID_HOME%
)
