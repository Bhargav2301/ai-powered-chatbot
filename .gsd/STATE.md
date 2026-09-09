# Polymath migration state

## Completed implementation and verification

- Original HEAD `cf66c820319cc07679c075fa189f5e8de837dd19` preserved remotely on `deprecated/chatbot-2026-09-07`; only DEPRECATED.md added.
- Native Polymath 0.2, dataset-scoped RAG, MiniLM/Qwen service, images, expanded topics and Room upgrade implemented.
- 42 Android tests and 16 service tests passed; real Qwen HTTP smoke passed; Android lint has zero errors.
- Debug APK built and signature checked. Model/runtime checksums and setup documented.
- Automatic approval review rejected public keystore upload. Signing keys are excluded from Git; local APK continuity is retained and fresh checkouts use generated debug keys.
- Four migration/feature/documentation commits preserve old main ancestry. Final ref update and remote CI are reported at handoff.

## Remaining product work

See docs/ROADMAP.md for physical-device release gates, model quality/efficiency qualification, export/restore, PDF/OCR and AI plan drafting. No production deployment or device certification is claimed.

## Public testing download — preparation (2026-09-09)

- Confirmed the repository rename and successful run `34100208781`; Android artifact `10010341760` is available and not expired.
- Added a fixed-version prerelease publisher, release notes, and checksum/build provenance generation. It checks the original commit, artifact digest and unchanged application source, and never overwrites published assets.
- Verification: workflow YAML parsed; embedded Bash and Python syntax checks passed; `python3 scripts/validate_project.py` and `git diff --check` passed.
- Pending: execute the publisher, verify the anonymous public download, and add its link to the README. The CI key differs from the earlier locally shared APK; installation guidance documents the data implications.

## Public testing download — verified (2026-09-09)

- Published prerelease `v0.2.0-initial` (release ID `385299452`) with `Polymath_0.2.0_debug.apk`, `SHA256SUMS` and `BUILD_INFO.json`.
- Publisher run `34323245072` passed, including anonymous download and checksum verification. APK: 67,290,631 bytes; SHA-256 `bce40f2aa38058ae02d5d10c04dde9016a85b9e964538098428ff4c70731027e`.
- Added the verified public download near the top of the root README, installation steps, service setup and feedback links, and CI-signature/data-loss guidance. Root README repository links use the renamed repository.
- `python3 scripts/validate_project.py` and `git diff --check` passed. Application source and the previously verified CI APK were unchanged.

## On-device AI — implementation and qualification (2026-09-09)

- Qwen3-0.6B Q4_K_M selected with pinned llama.cpp b10834; model identity, license and runtime source are locked. Gemma/LiteRT-LM is a conditional benchmark alternative, not an automatic remote fallback.
- Implemented verified download/import/optional APK weights, permissionless isolated JNI worker, scoped local BM25 RAG, response validation, bounded generation and resource/cancellation controls.
- Native real-Q4 smoke passed in run `34327997083`. Initial Android APK compiled, but the airplane-mode integration found that reopening an app-private model path failed. Replaced that with the runtime's borrowed FILE-pointer API. Android lint also identified use of a restricted factory; replaced it with the public framework factory.
- Final Android build/lint, real-model airplane-mode integration and 16 KB ELF/ZIP validation are pending the correction build. No physical-phone latency, thermal, battery or memory qualification is claimed.
- Detailed decision record and Gradle/NDK instructions: `docs/ON_DEVICE_AI.md`. Published `v0.2.0-initial` remains the earlier server-based test build.

### Android qualification follow-up

- Commit `1a0be60ad2d1b4669bdfa41d2cf6a63c4a357d8d`: verification run `34329472498` passed with 53 Android unit/Robolectric tests, zero lint errors (32 advisories), 12 native libraries passing 16 KB ELF/ZIP checks and 14 service tests (2 explicit model-dependent skips).
- In run `34329472546`, native Q4 inference passed. Android airplane-mode inference loaded and generated successfully through the granted descriptor; cancellation passed. The citation acceptance assertion failed because the model returned an empty citation list, which the client correctly treated as insufficient evidence.
- Commit `dce92f042a9363887af4d3e8734e9fd1c3988d96` clarifies supported-answer citation IDs in the production prompt and preserves strict verification. The synthetic instrumentation fixture now includes original JSON in failure messages. Reverification is in progress.

### Final local-inference verification

- Tested source `dce92f042a9363887af4d3e8734e9fd1c3988d96`: run `34330582170` passed Android build, 53 tests, lint (0 errors/32 advisories), 12-library 16 KB ELF/ZIP checks and service checks (14 passed/2 explicit skips).
- Run `34330582133` passed actual Q4 host generation and both Android airplane-mode tests, including supported source answer and cancellation with the foreground app intact.
- Detailed evidence is recorded in docs/VERIFICATION.md, docs/local-verification.json and android/BUILD_STATUS.md. docs/ON_DEVICE_AI.md is the decision record, implementation plan and Gradle/NDK guide.
- Physical ARM64 performance, actual 16 KB page runtime, broad grounded-answer quality and optional bundled-model APK qualification remain release gates. The public 0.2 APK was not replaced.

## Offline APK handoff correction

- User reports HTTPS/API-key setup after installing the linked 0.3 ZIP. Exact device state is unknown; the source defaults to local mode but keeps a server-mode preference, combines both choices in a connection sheet, and the linked build omitted model weights.
- Implemented explicit offline-first setup, visible 0.3.1 identity, primary bundled-model preparation, local mode selection after installation, and advanced-only server settings.
- The offline preview uses `com.polymath.app.offline` and launcher label Polymath Offline so existing app data survives differing debug certificates.
- Added an APK-content audit and a full UI integration test: first launch, prepare model from APK assets, import example through UI and require a cited answer in airplane mode with no configured service. Verification is pending on branch offline-apk-fix.
