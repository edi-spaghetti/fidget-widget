# Fidget Widget: working notes for Claude

Read `NOTES.md` first. It has the goals, the decisions made and the next steps.

## Build environment (this PC)
All of this was installed without admin rights. The system `java` is version 8, which is too old, so set `JAVA_HOME` explicitly:
- JDK 17: `%LOCALAPPDATA%\Android\jdk\jdk-17.0.21.1+2`
- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (platform 34, build-tools 34, platform-tools/adb)
- `local.properties` (gitignored) has `sdk.dir=C:/Users/Edward/AppData/Local/Android/Sdk`

Build from bash:
```sh
export JAVA_HOME="$LOCALAPPDATA/Android/jdk/jdk-17.0.21.1+2"
./gradlew.bat assembleDebug --console=plain -q
```

## Shipping a build to the phone
The user often works from their phone over Remote Control. For each release:
1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build.
3. Copy `app/build/outputs/apk/debug/app-debug.apk` to `FidgetWidget-v<version>.apk` in the project root. It's gitignored.
4. Send the APK with `SendUserFile` (display: attach). It installs over the previous version.

## Code map
- `MainActivity.kt`: start overlay, screen pinning (`startLockTask`), padlock placement, parent times-table gate, Back disabled, immersive mode
- `FloatingBubblesView.kt`: the bubble toy (spawning, drift, popping, particles, sound)
- `HoldUnlockButton.kt`: the 5 s hold-to-unlock padlock
- `Haptics.kt`: vibration helpers
- `tools/make_pop.js`: creates `res/raw/pop.wav`
