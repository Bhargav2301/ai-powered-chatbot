# Polymath

Native Android learning and knowledge management, with optional private AI chat. Discover news and knowledge pills, save sources, connect ideas, practice recall, and turn notes into projects.

**Current release: 0.2.0 (development).** The Android app is Kotlin/Jetpack Compose. AI uses a self-hosted, open-source **MiniLM + Qwen** service. Reading, notes, keyword search, recommendations and EXP work without the service. There is no proprietary-model fallback or mandatory account.

## Repository migration

The previous Rasa music/movie chatbot is preserved on [deprecated/chatbot-2026-09-07](https://github.com/Bhargav2301/ai-powered-chatbot/tree/deprecated/chatbot-2026-09-07). Its original commit is `cf66c820319cc07679c075fa189f5e8de837dd19`; the archive adds only `DEPRECATED.md`. All original tracked files and Git history remain available. Main now contains Polymath; this migration does not rewrite history.

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
- Hybrid retrieval: MiniLM embeddings, exact cosine similarity, BM25 and reciprocal-rank fusion. Qwen generates an answer from retrieved passages; both service and Android validate citation identities. Android also checks excerpt text and document revisions.
- RSS/Atom and JSON imports preserve associated images. Cards show a cover image; readers show the source gallery, captions and alternate text. Missing images have a fallback. The model reads source text, not image pixels.
- Twelve topic domains, including STEM, history, philosophy, literature, economics and professional certification concepts. Fourteen original starter lessons plus an importable example dataset. This is not an official exam-preparation syllabus.
- Thoughts, Research and Ideas; immutable note revisions; template project plans, dependencies and reflections.
- Room persistence, offline FTS4 keyword search, recall scheduling, an EXP ledger, a topic graph and native home-screen widgets.
- Explicit Room v1 → v2 migration, preserving existing local data.

## Run Android

Open `android/` in Android Studio with JDK 17 and Android SDK 36. Or:

```bash
cd android
bash gradlew :app:assembleDebug
```

Install `android/app/build/outputs/apk/debug/app-debug.apk` on Android 9+. The APK delivered with this migration uses the retained local development key and can update the previous 0.1 build. Fresh checkouts and CI generate their own debug key; signing material is excluded from Git. No build here is signed for a production store release.

## Enable AI chat

1. Follow the [service setup](services/rag/README.md) to download pinned MiniLM/Qwen models and start the service on a computer you control.
2. Open **Desk → chat icon → Connection** in Android.
3. Enter your service origin and API key, and enable the consent checkbox. Release connections require HTTPS. Debug builds additionally support localhost/Android emulator loopback.
4. Save a source or note, select **My vault**, and ask a question. For a separate dataset, open **Datasets → Add example dataset** or **Import JSON**.
5. Inspect the cited passages. Imported example cards also appear in Discover; the circuit example includes an original illustration.

No hosted endpoint or service key is bundled. Open-source models have no model-license fee; compute, electricity, network access and hosting are not promised free. AI generation currently runs on the configured server, **not on the phone**.

## Verify

```bash
cd android
bash gradlew :app:assembleDebug :core:model:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
cd ..
python -m pip install -r services/rag/requirements-dev.txt
PYTHONPATH=services/rag python -m pytest services/rag/tests -q
python scripts/validate_project.py
```

See [verification](docs/VERIFICATION.md) for measured results and the real-model test receipt. Unit tests do not require downloading models; two real-embedding tests run when `POLYMATH_MODEL_DIR` is set.

## Boundaries

The AI service is opt-in and stateless, but it receives selected source text and questions while processing. The shipped client stores its service key with Android Keystore encryption. Source images are fetched from their original HTTPS hosts. No analytics are included.

Citation validation detects unknown sources, modified excerpts and stale revisions; it does not prove every generated claim. Plans still use editable templates. Fully on-device generation, semantic search without a service, PDF/OCR import, image understanding, encrypted export/restore and automated web research remain on the [roadmap](docs/ROADMAP.md). Lock-screen widgets depend on the device host. Physical-device performance and production deployment have not yet been certified.
