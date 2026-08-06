#!/usr/bin/env bash
# Linux / macOS. Run this before building:
#
#     source env.sh
#
# Two traps this fixes:
#   1. The default `java` may be JDK 11, which cannot build Android (needs 17).
#   2. Ubuntu's android-tools-adb package puts adb 1.0.39 at /usr/bin/adb, which
#      refuses to talk to the SDK's 1.0.41 server.
#
# Override either path by exporting it before sourcing, e.g.
#     JAVA_HOME=/opt/jdk17 source env.sh

# --- JDK 17 -----------------------------------------------------------------
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/javac" ]; then
    for candidate in \
        "$HOME/toolchain/jdk17" \
        "/usr/lib/jvm/java-17-openjdk-amd64" \
        "/usr/lib/jvm/temurin-17-jdk-amd64" \
        "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
    do
        [ -x "$candidate/bin/javac" ] && export JAVA_HOME="$candidate" && break
    done
fi

# --- Android SDK ------------------------------------------------------------
if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME/platform-tools" ]; then
    for candidate in \
        "$HOME/Android/Sdk" \
        "$HOME/Library/Android/sdk" \
        "/opt/android-sdk"
    do
        [ -d "$candidate/platform-tools" ] && export ANDROID_HOME="$candidate" && break
    done
fi

export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# --- Report -----------------------------------------------------------------
if [ -x "$JAVA_HOME/bin/java" ]; then
    echo "java : $(java -version 2>&1 | head -1)"
else
    echo "java : NOT FOUND - install JDK 17, then: export JAVA_HOME=/path/to/jdk17"
fi

if [ -x "$ANDROID_HOME/platform-tools/adb" ]; then
    echo "adb  : $(adb version 2>&1 | head -1)"
    echo "sdk  : $ANDROID_HOME"
else
    echo "adb  : NOT FOUND - install the Android SDK, then: export ANDROID_HOME=/path/to/Sdk"
fi
