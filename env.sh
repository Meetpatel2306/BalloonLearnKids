#!/usr/bin/env bash
# Source this before building:  source env.sh
#
# This machine's default `java` is JDK 11 and its default `adb` is the old
# Ubuntu package (1.0.39), which refuses to talk to the SDK's adb server
# (1.0.41). Both are fixed here by putting the right tools first on PATH.
#
# To make it permanent, add these three lines to ~/.bashrc instead.

export JAVA_HOME=/home/netanalytic/toolchain/jdk17
export ANDROID_HOME=/home/netanalytic/Android/Sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

echo "java : $(java -version 2>&1 | head -1)"
echo "adb  : $(adb version 2>&1 | head -1)"
echo "sdk  : $ANDROID_HOME"
