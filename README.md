# AI Watch

A minimal, near‑monochrome AI chat assistant for Wear OS, configured from a
companion phone app.

- **Phone app** (`:app`) — enter your OpenRouter API key, add one or more
  model IDs (e.g. `deepseek/deepseek-v4-flash-0731`), and tap **Send to
  Watch**. The config is pushed to the paired watch over the Wearable Data
  Layer API.
- **Watch app** (`:wear`) — receives the config, lets you pick a model,
  start a chat, and keeps full chat history locally (Room database). Once
  configured, the watch calls the OpenRouter API directly — the phone
  doesn't need to stay in range for chatting, only for re-syncing config.
- **`:shared`** — the small data model (`WatchConfig`, `ModelEntry`) and
  JSON encode/decode shared by both apps so the wire format stays in sync.

## How the pieces talk to each other

```
Phone app  --(Wearable DataClient, path /aiwatch/config)-->  Watch app
                                                                 |
                                                        ConfigListenerService
                                                                 |
                                                            ConfigStore (SharedPreferences)
                                                                 |
                                                      ChatViewModel -> OpenRouter API
                                                                 |
                                                         Room DB (chats, messages)
```

## Project layout

```
app/     Phone (Jetpack Compose, Material 3)
wear/    Watch (Wear Compose, Room, OkHttp)
shared/  Common Kotlin data models + JSON
.github/workflows/build.yml   CI: builds debug APKs for both apps, per ABI
```

## Getting this into your repository

This was generated outside of GitHub (no push access from this session), so
push it yourself:

```bash
cd Ai-watch
git init
git add .
git commit -m "Initial commit: AI Watch phone + wear apps"
git branch -M main
git remote add origin https://github.com/bsod-coder/Ai-watch.git
git push -u origin main
```

Once pushed, go to the repo's **Actions** tab — the `Build APKs` workflow
runs automatically and, when it finishes, attaches two downloadable
artifacts to the run:

- `phone-apks` — `app-armeabi-v7a-debug.apk`, `app-arm64-v8a-debug.apk`,
  `app-x86-debug.apk`, `app-x86_64-debug.apk`, `app-universal-debug.apk`
- `watch-apks` — the same set for the watch app

You can also trigger it manually from **Actions → Build APKs → Run workflow**.

## Installing

- **Phone**: install `app-universal-debug.apk` (or the ABI matching your
  phone) via `adb install` or by sideloading.
- **Watch**: install the matching `wear-*-debug.apk` the same way —
  `adb -s <watch-serial> install app-universal-debug.apk` (enable ADB
  debugging over Wi‑Fi in the watch's Developer Options, since most watches
  have no USB port).

## Building locally

Requires JDK 17 and the Android SDK (API 34) installed, with `ANDROID_HOME`
set.

```bash
./gradlew :app:assembleDebug :wear:assembleDebug
```

(If you don't have a Gradle wrapper jar checked in, run `gradle wrapper`
once with a local Gradle install, or just use a system `gradle` — that's
what CI does.)

## Notes / things you may want to change

- **Debug builds only by default.** CI currently produces debug APKs
  (auto-signed with the debug keystore) so they install with zero setup.
  For a signed release build, add a keystore + `signingConfig` and switch
  the workflow to `assembleRelease`, storing the keystore as a GitHub
  Actions secret.
- **API key storage on the watch** uses plain `SharedPreferences` for
  simplicity. Swap `ConfigStore` to use
  `androidx.security:security-crypto`'s `EncryptedSharedPreferences` if you
  want it encrypted at rest.
- **Networking permissions**: the watch app requests `INTERNET` and calls
  OpenRouter directly — it does not proxy chat requests through the phone.
  This means the watch needs its own network path (Wi‑Fi, LTE, or Bluetooth
  companion proxying, depending on your watch).
- The UI intentionally uses a small, restrained palette (charcoal + off‑white
  + a single muted teal accent) rather than Material's default vivid colors.
