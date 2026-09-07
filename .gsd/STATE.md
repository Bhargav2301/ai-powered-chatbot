# Polymath migration state

## Completed implementation and verification

- Original HEAD `cf66c820319cc07679c075fa189f5e8de837dd19` preserved remotely on `deprecated/chatbot-2026-09-07`; only DEPRECATED.md added.
- Native Polymath 0.2, dataset-scoped RAG, MiniLM/Qwen service, images, expanded topics and Room upgrade implemented.
- 42 Android tests and 16 service tests passed; real Qwen HTTP smoke passed; Android lint has zero errors.
- Debug APK built and signature checked. Model/runtime checksums and setup documented.
- Automatic approval review rejected public keystore upload. Signing keys are excluded from Git; local APK continuity is retained and fresh checkouts use generated debug keys.
- Four migration/feature/documentation commits preserve old main ancestry. Final ref update and remote CI are reported at handoff.

## Remaining product work

See docs/ROADMAP.md for physical-device release gates, on-device model packs, export/restore, PDF/OCR and AI plan drafting. No production deployment or device certification is claimed.
