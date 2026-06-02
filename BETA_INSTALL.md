# Android beta — build, sign, and download

## One-time setup

```powershell
.\scripts\create-beta-keystore.ps1
```

Creates `keystore/invoicehammer-beta.jks` and adds signing lines to `local.properties` (gitignored).

## Build signed APK + AAB

```powershell
.\scripts\build-beta-download.ps1
```

Outputs in `dist/release/`:

- `androidApp-universal.apk` — install on any phone (Android 8.0+, all CPU types)
- `androidApp-release.aab` — Play Console upload
- `index.html` — simple download page

## Let testers download (same Wi‑Fi)

```powershell
.\scripts\serve-beta-download.ps1
```

Open the printed URL on the phone (e.g. `http://192.168.1.42:8765/`), tap **Download APK**.

Requires Python on PATH (`python -m http.server`). Firewall may prompt — allow private network.

## Direct install (USB)

```powershell
adb install -r dist\release\androidApp-universal.apk
```

## Notes

- Keep `keystore/invoicehammer-beta.jks` — same key required for updates over an existing install.
- Default beta passwords are set by `create-beta-keystore.ps1`; change them when you run the script with `-StorePassword` / `-KeyPassword` if you prefer.
- For production Play releases, use a separate production keystore and upload the `.aab`.
