# Polymath

Native Android learning and knowledge management, with optional private AI chat. Discover news and knowledge pills, save sources, connect ideas, practice recall, and turn notes into projects.

**Current source and offline preview: 0.3.1.** The Android app is Kotlin/Jetpack Compose. Version 0.3.1 includes **on-device Qwen3-0.6B**, with a verified 397 MB model pack and offline source-based chat. The self-hosted **MiniLM + Qwen** service remains an explicit alternative. Reading, notes, keyword search, recommendations and EXP need neither engine. There is no proprietary-model fallback or mandatory account.

See the [on-device architecture, model comparison and Android build guide](docs/ON_DEVICE_AI.md) for Qwen/Gemma research, memory budgets, NDK/Gradle setup, bundled weights and device acceptance gates.

## Download and test the Android app

**[Download Polymath Offline 0.3.1 APK](https://github.com/Bhargav2301/polymath_ai_news_chat/releases/download/v0.3.1-offline-preview/Polymath_0.3.1_offline_preview.apk)** — **657 MB download**, including the 397 MB Qwen model. No GitHub account, AI account, HTTPS endpoint or API key is needed.

This installs as **Polymath Offline**, alongside any existing Polymath app. It uses separate storage and does not import the old app's vault automatically. Keep your original app and its data.

1. Download and open the APK on Android. Allow installation from your browser/file manager if Android requests it.
2. Open **Polymath Offline**, choose topics, then open **Desk → chat icon**. The chat screen must show **Polymath 0.3.1 · Offline preview**.
3. Tap **Set up offline AI → Prepare included AI**. Wait for **Installed · ready for offline chat**, then tap **Done**. No additional model download is needed.
4. Open **Datasets → Add example dataset → Done**, then select **Polymath foundations**.
5. Ask: **What voltage is needed for 2 amps through 6 ohms?** Inspect the answer and its source citation. You can perform steps 2–5 with airplane mode enabled.
6. Save your own sources/notes and select **My vault** to ask about them. Chat answers from the selected sources; an empty vault has no evidence to answer from.

Requires Android 9 or newer and a supported 64-bit device, with roughly 4 GB RAM or more and sufficient available memory. Allow about **3 GB free storage** for the download, Android installation and unpacked model. This is an early debug preview; physical-phone performance and broad answer accuracy still need qualification.

[Release notes and checksums](https://github.com/Bhargav2301/polymath_ai_news_chat/releases/tag/v0.3.1-offline-preview) · [Setup and troubleshooting](docs/OFFLINE_PREVIEW.md) · [Report a bug](https://github.com/Bhargav2301/polymath_ai_news_chat/issues)

The earlier [0.2 service-based APK](https://github.com/Bhargav2301/polymath_ai_news_chat/releases/tag/v0.2.0-initial) remains archived. It is not the offline preview.

## Repository migration

The previous Rasa music/movie chatbot is preserved on [deprecated/chatbot-2026-09-07](https://github.com/Bhargav2301/polymath_ai_news_chat/tree/deprecated/chatbot-2026-09-07). Its original commit is `cf66c820319cc07679c075fa189f5e8de837dd19`; the archive adds only `DEPRECATED.md`. All original tracked files and Git history remain available. Main now contains Polymath; this migration does not rewrite history.

| Location | Responsibility |
|---|---|
| [android/](android/README.md) | Native app, Room database, recommendation rules, widgets and Android tests |
| [services/rag/](services/rag/README.md) | Authenticated, stateless semantic retrieval and grounded generation |
| [datasets/](datasets/README.md) | Import format, source-linked images and example learning material |
| [scripts/](scripts/README.md) | Pinned model/runtime downloads, real-model smoke test and project validation |
| [docs/](docs/README.md) | Architecture, verification, migration and engineering roadmap |
| `.github/workflows/verify.yml` | Android build/tests/lint and service API tests |

## Features shipped

- Swipe right to like/save, left to dismiss; tap to read. Undo replays the local recommendation model correctly, including old v1 learning events.
- AI chat with **My vault** or a selected imported dataset, question history, loading states, cancellation, retry and inspectable citations. Only the selected scope is sent.
- In 0.3.1: explicit **On device** mode with Qwen3-0.6B Q4_K_M, local BM25 retrieval, verified model download/import, an optional bundled-weight build, a permissionless native worker, resource gates and cancellation. It never falls back to a server automatically. Physical-device qualification remains pending.
- Hybrid retrieval: MiniLM embeddings, exact cosine similarity, BM25 and reciprocal-rank fusion. Qwen generates an answer from retrieved passages; both service and Android validate citation identities. Android also checks excerpt text and document revisions.
- RSS/Atom and JSON imports preserve associated images. Cards show a cover image; readers show the source gallery, captions and alternate text. Missing images have a fallback. The model reads source text, not image pixels.
- Twelve topic domains, including STEM, history, philosophy, literature, economics and professional certification concepts. Fourteen original starter lessons plus an importable example dataset. This is not an official exam-preparation syllabus.
- Thoughts, Research and Ideas; immutable note revisions; template project plans, dependencies and reflections.
- Room persistence, offline FTS4 keyword search, recall scheduling, an EXP ledger, a topic graph and native home-screen widgets.
- Explicit Room v1 → v2 migration, preserving existing local data.

## Run Android

Use JDK 17, Android SDK 36, NDK 28.2.13676358 and CMake 3.22.1. Prepare the pinned native source before opening `android/` in Android Studio:

```bash
python scripts/prepare_local_llm.py
cd android
bash gradlew :app:assembleDebug
```

Install `android/app/build/outputs/apk/debug/app-debug.apk` on Android 9+. Fresh checkouts and CI generate their own debug key; signing material is excluded from Git. Locally built APKs may have a different signature from the [public test APK](#download-and-test-the-android-app). No build here is signed for a production store release.

## Enable AI chat

**On-device mode:** open **AI settings → Offline AI**, prepare the included Qwen pack in the public offline preview, select saved sources or a dataset, and ask a question. A normal developer build without bundled weights offers a one-time model download/import instead. Local retrieval uses BM25; semantic embeddings remain server-only. Questions stay on the device. See the [integration guide](docs/ON_DEVICE_AI.md).

**Optional private-server mode (advanced):**

1. Follow the [service setup](services/rag/README.md) to download pinned MiniLM/Qwen models and start the service on a computer you control.
2. Open **Desk → chat icon → AI settings → Use a private server (advanced)**. This is optional and is not needed for offline AI.
3. Enter your service origin and API key, and enable the consent checkbox. Release connections require HTTPS. Debug builds additionally support localhost/Android emulator loopback.
4. Save a source or note, select **My vault**, and ask a question. For a separate dataset, open **Datasets → Add example dataset** or **Import JSON**.
5. Inspect the cited passages. Imported example cards also appear in Discover; the circuit example includes an original illustration.

No hosted endpoint or service key is bundled. Open-source models have no model-license fee; compute, electricity, network access and hosting are not promised free. In 0.3.1, the selected mode determines whether generation runs on the device or on your configured server.

## Verify

```bash
cd android
bash gradlew :app:assembleDebug :core:model:test :core:data:testDebugUnitTest :core:local:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug --max-workers=2
cd ..
python -m pip install -r services/rag/requirements-dev.txt
PYTHONPATH=services/rag python -m pytest services/rag/tests -q
python scripts/validate_project.py
```

See [verification](docs/VERIFICATION.md) for measured results and the real-model test receipt. Unit tests do not require downloading models; two real-embedding tests run when `POLYMATH_MODEL_DIR` is set.

## Boundaries

The AI service is opt-in and stateless, but it receives selected source text and questions while processing. On-device mode keeps inference local and disables automatic image loading inside chat. The client stores its service key with Android Keystore encryption. Feed images and user-opened original links still contact their original hosts. No analytics are included.

Citation validation detects unknown sources, modified excerpts and stale revisions; it does not prove every generated claim. Plans still use editable templates. On-device semantic embeddings, PDF/OCR import, image understanding, encrypted export/restore and automated web research remain on the [roadmap](docs/ROADMAP.md). Lock-screen widgets depend on the device host. Physical-device performance and production deployment have not yet been certified.
