# Android implementation plan — 0.3 development

The [on-device architecture and build guide](../../docs/ON_DEVICE_AI.md) specifies model choice, Qwen/Gemma fallback criteria, memory budgets, native process lifecycle, distribution options, Gradle/NDK setup and acceptance gates.

Implemented: Qwen3-0.6B Q4_K_M, pinned llama.cpp JNI, permissionless isolated worker, verified model download/import and optional bundled APK, explicit local/server mode, local BM25 retrieval, source citations and cancellation. The private MiniLM/Qwen service remains available by explicit selection.

Pending: physical-device qualification, local semantic embeddings and incremental indexing, encrypted export/restore, PDF/OCR, AI plan drafting and distribution hardening. Native/emulator tests do not establish phone performance. See the [shared roadmap](../../docs/ROADMAP.md).
