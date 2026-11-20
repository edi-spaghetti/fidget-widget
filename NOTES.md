# Project notes

Where things stand, and the decisions made, so work can pick up later.

## Goal
A "fidget widget" Android app the kids can play with on my phone. It keeps them inside the app so they can't tap into anything they shouldn't. Ideally the phone stays locked until a parent unlocks it.

## Decisions so far
- **Platform:** Android only, native Kotlin. Views are drawn on a custom canvas (no Compose), which keeps the app small and dependency-free.
- **Locking:** screen pinning via `startLockTask()`, with **"Ask for PIN before unpinning"** turned on in phone Settings.
  - We considered the full kiosk / device-owner mode and rejected it, because it needs a factory-reset phone with no Google accounts.
- **Parent exit:** a visible padlock button in the top right. Hold it for 5 s (a ring fills, a countdown shows, the phone ticks each second, a "Keep holding…" hint appears), then answer a times-table question.
  - v1 had a hidden top-left corner held for 3 s. It didn't work in practice and was hard to find, so it was replaced.
- **Content:** v1 was a pop-it grid. v1.1 replaced it with **floating soap bubbles** that drift upward. Tapping one pops it with a sound (deeper for bigger bubbles), a buzz, a flash, a ring and a burst of droplets.
- **Distribution:** sideload the APK, which is signed with the debug key. New versions install over the old one. The Play Store pipeline has been explained but not started (see below).

## Status (2026-11-20)
- v1.1 (versionCode 2) is built and was sent to the phone. Installing v1 confirmed sideloading works. v1.1 hasn't been checked on the phone yet.
- The repo is at `git@github.com:edi-spaghetti/fidget-widget.git` (private).

## Ideas / next steps
- Get feedback on v1.1: bubble speed and size, sound, the unlock flow.
- More toys, picked from a menu by the parent before locking: fidget spinner, finger paint, switches and sliders. The original pop-it grid could come back as one of them.
- Play Store, if we ever want it:
  - Play Console account ($25)
  - release keystore (back it up!) and a signed `.aab`
  - closed test with 12 testers for 14 days (needed for new personal accounts)
  - store listing and privacy policy
  - Families policy compliance
- Optional: attach APKs to GitHub Releases (needs the `gh` CLI or a token).
