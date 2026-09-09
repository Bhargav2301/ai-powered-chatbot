# Build status — Polymath 0.3.0 development

Source commit `dce92f042a9363887af4d3e8734e9fd1c3988d96` passed [Android verification](https://github.com/Bhargav2301/polymath_ai_news_chat/actions/runs/34330582170) and [real-model offline integration](https://github.com/Bhargav2301/polymath_ai_news_chat/actions/runs/34330582133) on 9 September 2026.

- Debug APK built: `com.polymath.app`, version code 3, minimum API 28 and compile/target API 36.
- 53 unit/Robolectric tests passed, with no failures or skips. Lint: zero errors, 32 advisories.
- Two actual-Qwen instrumentation tests passed on an API 35 x86_64 emulator in airplane mode: cited source Q&A and cancellation without foreground-app termination.
- All 12 packaged native libraries passed 16 KB ELF/ZIP alignment checks. This is not a 16 KB runtime-device test.
- The standard APK includes the runtime; install the separately verified Q4 model pack before local inference. The optional model-included APK path is documented but has not been qualified end to end.

[Build guide](../docs/ON_DEVICE_AI.md), [complete verification record](../docs/VERIFICATION.md), and [debug APK/report artifact](https://github.com/Bhargav2301/polymath_ai_news_chat/actions/runs/34330582170/artifacts/10095846353). Physical ARM64 devices, production signing, battery/thermal behavior and broad answer-quality qualification remain open.

## Archived 0.2.0 build receipt

The following is the earlier locally built 0.2 artifact, not the 0.3 CI APK or the separately signed public 0.2 download.

| Check | Result |
|---|---|
| Debug APK | Built successfully; `com.polymath.app`, version code 2 |
| Android compatibility | Minimum API 28, compile/target API 36 |
| Kotlin domain tests | 11 passed |
| Room, migration, feed/media, dataset and RAG transport tests | 29 passed |
| Compose integration/rendering tests | 2 passed |
| Android lint | 0 errors, 7 advisories |
| Service tests | 16 passed, including two real MiniLM tests |
| Real-model RAG smoke | Passed: actual Qwen answer with correct source citation/media identity |
| Debug signature | Verified; same development certificate as 0.1 |

The final Android command was:

```bash
bash gradlew :core:model:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --max-workers=3 -Pkotlin.compiler.execution.strategy=in-process
```

It completed successfully. The seven lint advisories concern widget attributes with existing size fallbacks, available dependency upgrades and a version-catalog suggestion. No lint baseline hides errors.

APK SHA-256: `3b5c1f16ac5ee1db6d7702de61de916cb809e362b275530046a57c957972e69f`.

Development certificate SHA-256: `5b1bdebab5257361ff9edbdf4711bf5cc71dab93f1742d50beb5e5f3d621d0fc`.

The APK is a development build, not a store-signed release. Image assets and dependencies make the unminified debug package about 67.4 MB. Model weights are downloaded separately on the AI host.

## Evidence

[Sanitized test receipts](docs/verification/) and [native view captures](docs/screenshots/) are included. The UI flow covers onboarding, saving, Vault, Graph, note persistence after Activity recreation, chat entry, dataset import/scope selection and connection setup. A separate rendering test loads actual circuit-image pixels through a controlled Coil interceptor and checks source alt text and attribution. The migration test constructs the exported v1 SQLite schema, inserts original data and opens it through the real v2 Room migration.

[The shared verification record](../docs/VERIFICATION.md) and [real-model receipt](../docs/model-smoke.json) describe backend checks. The model smoke ran on CPU in the build environment, not on an Android phone. The API and Android transport are tested separately against their common NDJSON contract.

## Remaining release gates

No physical Android device or hardware emulator was available. Device performance, battery/thermal behavior, API 28/36 device coverage, large-font/TalkBack review and launcher/lock-screen widget compatibility remain unverified. Docker containers and public HTTPS deployment were not started here. The CI workflow exists at the repository root; its remote result is reported at handoff.
