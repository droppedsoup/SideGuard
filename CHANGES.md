# SideGuard changes

## 1.0
- APK vetting: SHA-256 hash, signing certificate, and a plain-language list of high-risk permissions
- VirusTotal hash lookup (bring your own free API key; the file itself is never uploaded)
- Signature pinning: warns if an app update is signed by a different key than before
- Shortcuts to Samsung Auto Blocker, "install unknown apps" and Play Protect
- Fixed the Auto Blocker shortcut so it opens the right screen on One UI
- Quick Settings tile for Auto Blocker
- Opens `.apk` files directly, so you can check an app before installing it
- VPN status indicator (read-only; never interferes with your VPN app)

## Publishing note
This public repo starts from a single clean commit of 1.0. Earlier development history was kept private.
