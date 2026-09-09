# Polymath Android — 0.3.1 development

On-device Qwen setup, model selection, resource limits and optional APK-bundled weights are documented in the [local LLM integration guide](../docs/ON_DEVICE_AI.md). The public offline preview includes the Qwen model and installs as **Polymath Offline**, alongside the original app. See [testing instructions](../docs/OFFLINE_PREVIEW.md).

Kotlin, Jetpack Compose and Material 3. Minimum Android 9 (API 28); compile/target API 36. JDK 17, Gradle 8.13, AGP 8.11.1, Kotlin 2.1.21 and Room 2.7.2 are pinned.

## Build and install

From the repository root, run `python scripts/prepare_local_llm.py`. Open this directory as the Android Studio project. Install Android SDK platform 36, build-tools 35.0.0, NDK 28.2.13676358 and CMake 3.22.1, then run:

```bash
bash gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Application ID: `com.polymath.app` by default; `-PpolymathOfflinePreview=true` selects `com.polymath.app.offline` and the Polymath Offline launcher label; version code: `4`. Signing keys are excluded from Git. Fresh checkouts and CI use the standard locally generated Android debug key. An existing local `dev/debug.keystore` is supported for development continuity; do not publish it. The public 0.2 APK uses the CI debug key; a fresh checkout or another CI runner can produce a different certificate and may not update that installation. Use a separate private key for production signing.

## Modules

| Module | Contents |
|---|---|
| `app` | Hilt application, Compose screens, ViewModel, image loading, widgets and UI integration test |
| `core:model` | Content types, 12-domain topic graph, logistic ranking, recall/EXP rules and plan templates |
| `core:data` | Room v2, repositories, RSS/Atom media, dataset import, local BM25/citations and the private-service RAG client |
| `core:local` | Verified Qwen model pack, resource policy, isolated Binder service and llama.cpp JNI runtime |

## Try the complete flow

1. Choose topics or explore all. Right swipe saves a card; left dismisses; Undo lasts five seconds.
2. Open a card to read its text, source link and source-image gallery. Use **Turn into idea** to retain the source association.
3. Save a Thought, Research topic or Idea in Notebook. Idea plans are editable four-step templates that require acceptance before task completion.
4. Practice recall for EXP; completed tasks require a reflection. Merely opening or saving a card earns no EXP.
5. Open Vault for offline keyword search. Tap its chat icon, or the chat icon at the top of Desk.
6. Open **AI settings → Offline AI** and prepare Qwen. The optional **Use a private server (advanced)** action exposes the [service](../services/rag/README.md) settings. Select My vault or a dataset, ask a question, and inspect citations.
7. In Chat → Datasets, add the built-in example or import a [JSON dataset](../datasets/README.md). Source cards join Discover immediately. Selecting a dataset in Chat queries all its imported documents, even those not saved to My vault.
8. Pin One pill or Quick capture from Desk on a compatible launcher.

## Persistence and integrity

Room v1 upgrades additively to v2: image metadata, dataset IDs, dataset records and chat history. Notes, saves, swipe events and EXP remain intact. Removing a dataset removes its source cards and saved copies; user-authored notes and EXP remain. Removing saved content, editing/deleting a note or deleting a dataset clears chat history to remove copied source excerpts. A source change cancels an in-flight answer.

Each API response is checked against the exact request corpus: dataset, source ID, revision, Unicode excerpt offsets, content and allowed citation IDs. Links, titles and images are reconstructed from local source records. No HTML from model output is executed.

The service API key is encrypted with an Android Keystore AES-GCM key. The Room database is app-private but not additionally encrypted. Android backup is disabled. Images use HTTPS and memory caching; disk image caching is disabled.

## Connection development

Production accepts HTTPS origins without embedded credentials, query strings or paths. Debug builds also accept `http://127.0.0.1:8000`, `http://localhost:8000` and `http://10.0.2.2:8000`.

For a USB-connected device with the service running on your computer:

```bash
adb reverse tcp:8000 tcp:8000
```

Then use `http://127.0.0.1:8000` in the debug app. Android Emulator may use `http://10.0.2.2:8000`. The API key and consent are still required. Arbitrary cleartext LAN origins are rejected. The release app does not inherit the debug network-security exceptions.

## Verification

```bash
bash gradlew :core:model:test :core:data:testDebugUnitTest :core:local:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The UI test runs the actual Hilt Activity and Room repositories under Robolectric API 35 and captures screen images. [Build status](BUILD_STATUS.md) records the actual results. [The shared workflow](../.github/workflows/verify.yml) runs from the repository root with this directory as its Android working directory.

## Current limits

The normal APK includes the local runtime but downloads/imports its Qwen weights separately; the public offline preview includes them. Local chat uses BM25 and Qwen; no embedding model is installed. Offline Vault search remains FTS4 keyword search, and semantic retrieval is available in explicit server mode. JSON imports are bounded to 150 sources, 300,000 text/title characters, 20,000 characters per source and six images per source. The app allows 20 imported datasets. Unsupported or oversized imports fail visibly; they are not silently truncated into an incomplete corpus.

RSS retains up to six HTTPS images per entry and bounded feed excerpts, not full article scraping. The topic graph has curated relationships rather than inferred prerequisites. Plans remain templates. Export/restore, autosave, full-text PDFs/OCR and production device profiling are pending. Uninstalling removes local notes and history.
