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
