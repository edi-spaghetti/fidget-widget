# Fidget Widget

A distraction app for Android: an app kids can play with on a parent's phone without wandering off into other apps.

- **Floating bubbles.** Soap bubbles drift up the screen. Tapping one pops it with a sound, a buzz, a ring and a burst of droplets. Bigger bubbles pop with a deeper sound.
- **Locked in.** Tapping Start pins the app using Android's built-in [screen pinning](https://support.google.com/android/answer/9455138). This blocks Home, Recents, notifications and Back.
- **Parent exit.** Hold the padlock in the top-right corner for 5 seconds, then answer a times-table question.

## Recommended phone setup

In Settings, search for **"pinning"** and turn on:

1. **App pinning**
2. **Ask for PIN before unpinning**

With both on, Android's own unpin gesture also needs your phone PIN.

## Building

Requires JDK 17 and the Android SDK (platform 34, build-tools 34). Put the SDK path in `local.properties` (`sdk.dir=...`).

```sh
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

`tools/make_pop.js` creates the pop sound (`app/src/main/res/raw/pop.wav`): `node tools/make_pop.js`.

## Installing

Sideload the APK by opening it on the phone and allowing installs from that source. Or install over USB with `adb install -r app-debug.apk`.
