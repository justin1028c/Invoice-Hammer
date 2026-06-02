# Google Sign-In / Drive backup — Firebase setup

## Default beta build (works now)

`.\scripts\build-beta-download.ps1` builds the **`beta`** variant: same obfuscation as release, signed with the **debug** keystore that already matches `google-services.json`. Google Sign-In works without Firebase changes.

For production signing (`invoicehammer-beta.jks`), use:

```powershell
.\scripts\build-beta-download.ps1 -ProductionSigning
```

That requires the beta SHA-1 in Firebase (below).

## Why production-signed beta fails sign-in

`app/google-services.json` only registers the **debug** signing certificate:

- Debug SHA-1: `8c7b5a09a3fee0e4011d5371cc834f4b493527be`

The **beta APK** is signed with `keystore/invoicehammer-beta.jks`:

- Beta SHA-1: `5c64756a1cd541582faaf8ddb20481eeb4446e08`

Until the beta SHA-1 is added in Firebase, Google Sign-In returns error **code 10** on release/beta builds.

## Fix (one-time)

1. Run:
   ```powershell
   .\scripts\print-signing-sha1.ps1
   ```
2. Open [Firebase Console](https://console.firebase.google.com/) → project **toolbelt-3812f381**
3. **Project settings** → **Your apps** → Android `com.fordham.toolbelt`
4. **Add fingerprint** → paste beta **SHA-1** (no colons): `5c64756a1cd541582faaf8ddb20481eeb4446e08`
5. Optionally add beta **SHA-256**: `bc863dc597b3e105e0b1fd50a48d8a9674074819595c02d5d428ed7d963840ff`
6. Download the new `google-services.json` and replace `app/google-services.json`
7. Rebuild the APK: `.\scripts\build-beta-download.ps1`

## After sign-in

- Sign-in requests **Google Drive app data** scope for cloud backup.
- If **SYNC NOW** fails with Drive permission text: **Sign out**, **Sign in** again, and approve Drive access.
- Enable **Google Drive API** in [Google Cloud Console](https://console.cloud.google.com/) for the same project if uploads fail with API errors.
