# AiWatch

An OpenRouter assistant for Wear OS, with a companion phone app that provisions it.

You paste your OpenRouter key and pick models on your phone; the phone pushes them to the
watch over the Wear Data Layer. The watch then talks to OpenRouter **directly** over Wi-Fi
or LTE, so replies do not wait on your phone and conversations keep working when the phone
app is closed.

```
┌──────────────────┐   key + model list    ┌──────────────────┐
│      Phone       │ ─── Data Layer ─────► │      Watch       │
│  com.aiwatch     │   (Google Play svcs)  │  com.aiwatch     │
│  .phone          │ ◄── "send config" ─── │  .wear           │
└────────┬─────────┘                       └────────┬─────────┘
         │                                          │
         │ GET /models, /key                        │ POST /chat/completions (stream)
         └──────────────► openrouter.ai ◄───────────┘
```

## What each app does

**Phone (`:app`)** — three tabs:

| Tab | What it does |
| --- | --- |
| **Key** | Paste and test your key against `GET /api/v1/key`, see spend and tier, remove it. Also sets temperature, max tokens and an optional system prompt. |
| **Models** | Add slugs by hand (`deepseek/deepseek-v4-flash-0731`) or pick from the live catalogue via `GET /api/v1/models`. Reorder to choose the default; delete to drop one. |
| **Watch** | Shows paired watches, a readiness checklist, and **Send to watch**. |

**Watch (`:wear`)** — three screens:

- **Home** — the synced models, one tap to start a chat. If the watch has no config it asks
  the phone for one.
- **Chat** — streaming replies rendered token by token, with a stop control, voice input
  (`ACTION_RECOGNIZE_SPEECH`) and an on-screen keyboard.
- **History** — every saved conversation, reopen or delete. Persisted as JSON in the app's
  private storage.

`:core` holds everything both sides share: the sync contract, the OpenRouter client, the
streaming SSE parser and the Data Layer bridge.

## Getting started

1. Build both APKs (see [CI](#continuous-integration) for the automatic route) or run from
   Android Studio.
2. Install `app-*.apk` on the phone and `wear-*.apk` on the watch.
3. Pair the watch with the phone in the **Wear OS** app.
4. On the phone: paste a key, tap **Test**, add models, then **Send to watch**.
5. On the watch: tap a model and talk.

> The watch needs its own internet connection (Wi-Fi or LTE) to reach OpenRouter. The phone
> is required once, to hand over the key.

### Signing

`assembleRelease` signs with `$AIWATCH_KEYSTORE_PATH` when that environment variable is set,
and otherwise falls back to the debug keystore so the output is still installable. To ship a
real build, add four repository secrets — `AIWATCH_KEYSTORE_BASE64`, `AIWATCH_KEYSTORE_PASSWORD`,
`AIWATCH_KEY_ALIAS`, `AIWATCH_KEY_PASSWORD` — and the workflow picks them up automatically.

## Continuous integration

`.github/workflows/android.yml` runs on every push, pull request and manual dispatch:

1. **verify** — `scripts/verify_project.py`, then `./gradlew test`.
2. **build** — a two-entry matrix (`app`, `wear`) running `assembleRelease`.

Each module declares ABI splits, so one build produces five APKs per app — `armeabi-v7a`,
`arm64-v8a`, `x86`, `x86_64` and a `universal` fallback. They are uploaded as the
`aiwatch-phone-apks` and `aiwatch-wear-apks` artifacts (14-day retention), downloadable from
the run's **Summary** page.

`armeabi-v7a` and `arm64-v8a` cover real watches; `x86`/`x86_64` cover emulators.

### Local checks

```bash
python3 scripts/verify_project.py   # no JVM or Android SDK required
./gradlew test                      # JVM unit tests for :core
./gradlew assembleRelease           # all APKs
```

`scripts/verify_project.py` parses every Kotlin file with tree-sitter, validates all XML, and
cross-checks the version catalog, manifest components, resource references and internal
imports. It is **not** a substitute for a Gradle build — it cannot resolve androidx symbols
or type-check Kotlin — but it catches the mistakes that are cheap to make and expensive to
discover in CI.

## Design

Graphite and sage: one desaturated accent, warm near-neutral surfaces, hairline borders
instead of drop shadows. The watch is near-black with off-white text, following Wear OS
convention. No saturated colour is used anywhere.

## Notable decisions

- **No Room, no annotation processors.** Watch history is a JSON document written behind a
  mutex with a temp-file-then-rename, so a crash mid-write cannot truncate it. Bounded at 100
  conversations. Swap in Room in `ChatRepository` if that ever changes.
- **Explicit serializers everywhere.** Every `Json.encodeToString`/`decodeFromString` passes
  a `serializer()` explicitly, so R8 cannot strip something reflection needed.
- **Streaming over an unbounded channel.** OkHttp delivers deltas on its own thread; a
  bounded buffer would silently drop text when the UI is briefly slow. Back-pressure happens
  at `emit`, which suspends rather than losing tokens.
- **Payload budgeting.** The Data Layer caps items at ~100 KB. Pasting a few hundred models
  could exceed that, so `PayloadBudget` binary-searches the longest model prefix that fits
  and the phone reports what it dropped.
- **Watch navigation is an explicit stack**, not `SwipeDismissableNavHost`, so `BackHandler`
  is enabled only when there is something to pop and the system back-swipe still exits.

## Project layout

```
core/   shared sync contract, OpenRouter client, SSE parser, Data Layer bridge
app/    phone companion (Compose Material 3)
wear/   watch app (Compose for Wear OS)
scripts/verify_project.py
```

| | Phone | Watch |
| --- | --- | --- |
| `minSdk` | 26 | 30 |
| `targetSdk` / `compileSdk` | 35 | 35 |
| UI | Compose Material 3 | Compose for Wear OS |

Versions live in `gradle/libs.versions.toml`. Bump `agp` together with
`gradle/wrapper/gradle-wrapper.properties`, then `kotlin`.

## Security

The key is stored in each app's private storage and is only ever sent to `openrouter.ai`.
Removing it on the phone also pushes the removal to the watch, so a lost phone does not leave
a usable key on the wrist. Nothing is logged and there is no third-party analytics.

## Known limitations

- Rotary scrolling is not wired up; lists scroll by touch. Adding
  `Modifier.onRotaryScrollEvent` requires opting into an experimental Wear Foundation API.
- Chat history is not synced back to the phone.
- Conversations are plain text; Markdown from the model is shown verbatim.

## Licence

Mozilla Public License 2.0 — see [LICENSE](LICENSE).
