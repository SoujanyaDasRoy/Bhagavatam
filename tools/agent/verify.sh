#!/usr/bin/env bash
# Build, test, install, launch, screenshot and crash-check the app on the emulator.
#   tools/agent/verify.sh            debug build, unit tests, install, launch, screenshot of the first screen
#   tools/agent/verify.sh release    release build instead (installs the release APK)
#   tools/agent/verify.sh nobuild    skip building, only reinstall the APK already in apk/ and look at it
# Output: screenshots in $SHOTS (default: a "shots" folder in the temp directory) and a PASS/FAIL summary.
# It cannot judge how a screen looks: open the screenshot and look at it.
set -u
MODE="${1:-debug}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
export JAVA_HOME="${JAVA_HOME:-$HOME/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2}"
SDK="${ANDROID_SDK:-/c/Users/sdroy/AppData/Local/Android/Sdk}"
ADB="$SDK/platform-tools/adb.exe"
SHOTS="${SHOTS:-${TEMP:-/tmp}/shots}"
PKG=com.bhagavatam.app
mkdir -p "$SHOTS"
fail=0

if [ "$MODE" != "nobuild" ]; then
  task=":app:assembleDebug"; [ "$MODE" = "release" ] && task=":app:assembleRelease"
  echo "== build + unit tests"
  if ./gradlew --offline :app:testDebugUnitTest $task -Dorg.gradle.java.home="$JAVA_HOME" 2>&1 | tee "$SHOTS/build.log" | grep -E "^e: |FAILED|BUILD"; then :; fi
  if ! grep -q "BUILD SUCCESSFUL" "$SHOTS/build.log"; then echo "FAIL: build or tests failed (see $SHOTS/build.log)"; exit 1; fi
fi

# The APK name can carry a version (Bhagavatam-v1.1.0-debug.apk), so take the newest one of the right kind.
KIND=debug; [ "$MODE" = "release" ] && KIND=release
APK="$(ls -t apk/*-$KIND.apk 2>/dev/null | head -1)"
[ -n "$APK" ] && [ -f "$APK" ] || { echo "FAIL: no apk/*-$KIND.apk found"; exit 1; }
echo "apk: $APK"

echo "== device"
if ! "$ADB" devices | grep -q "device$"; then
  echo "No emulator or phone is connected. Connect a phone with USB debugging, or start an emulator (see AGENTS.md section 3), then run this again with 'nobuild'."
  if [ "$MODE" = "nobuild" ]; then echo "PARTIAL: nothing was built or tested on a device."; else echo "PARTIAL: build and unit tests passed; nothing was tested on a device."; fi
  exit 2
fi
"$ADB" logcat -c
"$ADB" install -r -t "$APK" | tail -1
"$ADB" shell am force-stop "$PKG"
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 10
"$ADB" exec-out screencap -p > "$SHOTS/verify_home.png"
echo "screenshot: $SHOTS/verify_home.png"

echo "== crash check"
if "$ADB" logcat -d -s AndroidRuntime:E | grep -q "FATAL"; then
  "$ADB" logcat -d -s AndroidRuntime:E | tail -20
  echo "FAIL: the app crashed"; fail=1
else
  echo "no crash in the log"
fi
[ $fail -eq 0 ] && echo "PASS (build, tests, install, launch). Now open the screenshot and check the screen you changed." || exit 1
