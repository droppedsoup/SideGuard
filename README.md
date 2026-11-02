# SideGuard

A lightweight, free, open-source Android app for safer sideloading (installing apps from outside the Play
Store), built for Samsung (One UI) phones. It is an original app, not a fork.

> **Status:** personal project, version 1.0. Small and simple. No warranty (see `LICENSE`).

## What it does
- **Vet an APK before install** — computes SHA-256, reads the signing certificate,
  and flags high-risk permissions (SMS, accessibility, device admin, overlays, etc.).
- **VirusTotal lookup** — checks the APK's hash against 70+ engines. Only the hash
  is sent, never the file. Needs a free API key (set it in-app).
- **Signature pinning** — remembers each app's signer and warns if a later update
  is signed by a different key (a common repackaging/tampering sign).
- **Quick shortcuts** — jump straight to Auto Blocker, "allow unknown installs",
  and Play Protect instead of digging through Settings.
- **Mullvad status** — shows whether a VPN tunnel is active. Does not interfere
  with Mullvad (Android allows only one active VPN).

- **Open APKs directly** — SideGuard registers as a handler for `.apk` files, so tapping a downloaded APK
  can send it to SideGuard for a check first.
- **Quick Settings tile** — a tile that jumps to the Auto Blocker screen.

### What the permission check looks for
SMS read/send/receive, call log, contacts, outgoing-call monitoring, placing calls, microphone, camera,
precise and background location, phone number/IMEI, drawing over other apps, accessibility services,
device admin, installing other apps, seeing all installed apps, auto-start on boot, file storage access,
notifications and persistent background services. Each one is explained in plain words in the report.

## Privacy
- The APK file never leaves your phone. Only its SHA-256 hash is sent, and only to VirusTotal
  (`virustotal.com/api/v3`), and only if you add your own API key.
- No analytics, ads, accounts or trackers. Remembered signatures are stored only on the phone.
- Permissions it asks for: internet (VirusTotal lookup), network state (VPN status), and permission to
  hand an APK on to the system installer.

## What it does NOT do (by design / platform limits)
- It cannot toggle Samsung Auto Blocker programmatically (protected system setting);
  it deep-links you to the screen instead.
- It is not an always-on antivirus — that needs system privileges. Play Protect
  fills that role. SideGuard vets apps at the moment you install them.
- It does not require root. Fully uninstallable, Knox stays intact.

## Install
Download the latest APK from the [Releases](../../releases) page. Requires Android 8.0+. Releases are
built automatically by GitHub Actions from this repo. They are signed with a CI debug key that can change,
so uninstall the old version first if an update is refused.

## Build
Local:
```
./gradlew assembleDebug
```
APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

Cloud: every push to `main` builds the APK via GitHub Actions and attaches it to a
release (see the Releases tab).

## Requirements
- minSdk 26 (Android 8.0), target 34.

See [CHANGES.md](CHANGES.md) for the version history.

## License
MIT. See `LICENSE`. Uses AndroidX and Material Components (Apache-2.0).
