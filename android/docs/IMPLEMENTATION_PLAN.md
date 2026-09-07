# Android implementation status

Version 0.2 ships dataset-scoped AI chat through the optional private service, source-linked media, JSON dataset imports, twelve topic domains and a Room v1 → v2 migration. The local core retains swipe ranking, note revisions, project templates, recall and EXP.

The authoritative next slices and acceptance gates are in the [shared engineering roadmap](../../docs/ROADMAP.md). On-device MiniLM/Qwen runtime packs, incremental semantic indexing, export/restore, PDF/OCR, AI-generated plans, richer widgets and physical-device performance validation remain open.

`EmbeddingEngine` and `GroundedGenerator` in `core:model` remain extension boundaries for future on-device adapters. The shipped generation path is the concrete `RagClient` → private FastAPI service → MiniLM/llama.cpp pipeline. Do not describe the Kotlin interfaces alone as local inference.
