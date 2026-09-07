# Next engineering slices

The approved technical blueprint remains the target architecture. This document tracks the gap from the working 0.1 foundation, rather than treating unimplemented adapters as working features.

| Order | Slice | Acceptance gate |
| --- | --- | --- |
| 1 | Device validation and durability | Install on API 28, 33, and 36; exercise rotation, process death, airplane mode, large fonts, TalkBack, keyboard, back navigation, widget resize and locked-device entry. Confirm no private widget pixels. |
| 2 | Capture and recovery | Debounced transaction-safe autosave, draft restoration, Markdown/JSON export and verified restore, revisions browser, export deletion consistency. |
| 3 | Embeddings and semantic search | Integrate an Apache-2.0 MiniLM model pack through ONNX; validate tokenizer, attention-mask mean pooling, normalization, 384 dimensions, and parity fixtures. Index revisioned chunks in a durable queue. |
| 4 | Hybrid retrieval | Combine current FTS retrieval with exact cosine scoring and reciprocal-rank fusion. Filter current/nondeleted private documents before retrieval. Test deletion during indexing and query cancellation. Benchmark 10,000 chunks. |
| 5 | Local generation | Integrate the approved Qwen3-0.6B GGUF build with llama.cpp. Signed model manifests, optional downloads, cancellation, device capability gates, thermal and memory budgets. No proprietary fallback. |
| 6 | Grounded conversation | Retrieve bounded evidence, pack context, generate an answer, validate citations, preserve versioned sources, abstain when evidence is insufficient. Add explicit external-web scope only after private retrieval is reliable. |
| 7 | AI incubator | Research evidence and structured plan JSON; schema and dependency DAG validation; revision-aware acceptance; active-plan diff that preserves completed and edited tasks. |
| 8 | Content quality and recommendation | Source management, ETag/Last-Modified, retention policies, story deduplication, immutable daily decks and impressions, embedding features, recommendation explanations and evaluation fixtures. |
| 9 | Graph and review growth | Curated concept nodes, multi-topic EXP allocations, evidence-backed edges, accepted AI suggestions, review scheduling across timezone changes and clock adjustments. |
| 10 | Widgets and beta | Responsive public-pill layout, news-brief and learning-desk widgets, authenticated action receipts, per-host lock-screen testing, accessible light theme and tablet layouts. |

## Model interfaces already in the code

`EmbeddingEngine` and `GroundedGenerator` are explicit boundaries in `core:model`. No production implementation is installed yet. Add adapters in separate Android modules so low-memory devices retain the functional reader, notebook, reviews, and lexical vault without loading model binaries.

## Migration policy

Room version 1 is the first schema. Export its generated schema with the source. Add explicit migrations for subsequent changes and test restoration from every shipped version. Never add `fallbackToDestructiveMigration()` to a user's knowledge vault.

## Production release gates

Measure cold start, frame pacing, input-to-save latency, search latency, Room size, network volume, and inference memory on named devices. Gate a model pack on measured correctness and resource use. Before release, review feed permissions and starter content, validate all source links, run dependency/security scans, and establish a recoverable export path. These are engineering gates, not claims already established by the current build.
