# Engineering roadmap after 0.2

The 0.2 scope delivers native dataset-scoped RAG chat, open-source inference, domain expansion and source-associated images. The items below are not marked shipped.

| Priority / slice | Work | Acceptance gate |
|---|---|---|
| P0 · Android release hardening | Profile startup, gestures, chat cancellation, memory, large fonts and TalkBack on API 28/35/36 devices; verify update from 0.1 | Named-device results; no data loss or critical accessibility failures |
| P0 · Recoverable private data | Versioned encrypted export/restore of notes, datasets, projects, sources and EXP | Round-trip and wrong-key/corrupt-file tests; documented recovery |
| P1 · On-device semantic retrieval | Port pinned MiniLM tokenizer/ONNX runtime; persist revisioned chunks and embeddings in Room; incremental indexing jobs | Tokenizer/vector parity, deletion and revision checks; measured p95 retrieval on 10k chunks |
| P1 · On-device generation pack | Opt-in signed/checksummed Qwen GGUF pack and llama.cpp Android runtime; memory/thermal-aware cancellation | API28 fallback and tested device tiers; same evidence/citation contract; no proprietary fallback |
| P1 · Evaluated content quality | Domain-specific query sets; tune relevance threshold; verify source links, quizzes, summaries and certification versions | Published retrieval/grounding evaluation per domain; editorial approval of factual content |
| P2 · Richer datasets | PDF extraction, OCR and embedded-image association; file attachment import with provenance | Page/figure provenance, malicious-file and wrong-image association tests |
| P2 · Research and planning | Cited AI plan drafts linked to note revision, explicit acceptance and editable tasks; opt-in web research | No automatic task execution; unsupported assumptions labeled; revision conflicts handled |
| P2 · Feed quality | Configurable sources, conditional fetches, retention, story deduplication and durable daily decks | Offline/refresh regression tests and publisher/source review |
| P2 · Graph and widgets | Concept-level graph, spaced-recall widgets, host-compatible lock-screen views | Accessible list equivalent; unlock/privacy behavior verified on named hosts |

Keep feature slices separate from infrastructure migration. Do not treat topic EXP as mastery or certification. Keep datasets bounded until an incremental index is shipped. Existing v1/v2 migrations and downgrade/backup behavior must be covered before any schema change.
