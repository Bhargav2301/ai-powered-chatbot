# Verification record — Polymath 0.3 development

## On-device integration: 9 September 2026

Verified implementation commit: `dce92f042a9363887af4d3e8734e9fd1c3988d96`. Later documentation and ignore-rule changes do not change this tested application/runtime source. Machine-readable summary: [local-verification.json](local-verification.json).

| Check | Observed result | Evidence |
|---|---|---|
| Android APK, unit/Robolectric tests and lint | Build passed; 53 tests, zero failures/errors/skips; lint zero errors and 32 advisories | [Verification run](https://github.com/Bhargav2301/polymath_ai_news_chat/actions/runs/34330582170) |
| Native packaging | All 12 packaged libraries passed 16 KB ELF and stored-ZIP alignment checks | Same verification run |
| Native Q4 execution | Actual Qwen model returned `12 volts` and source ID `S1` through the shared C++ core | [Local inference run](https://github.com/Bhargav2301/polymath_ai_news_chat/actions/runs/34330582133) |
| Android offline inference | Two instrumentation tests passed on API 35 x86_64 with airplane mode set and Wi-Fi/mobile data disabled | Same local inference run |
| Existing RAG service | 14 tests passed; two MiniLM-artifact-dependent tests explicitly skipped | Verification run |
| Source/configuration | Dataset/media/asset/Markdown checks and Docker Compose configuration passed | Verification run |

The Android test requires the exact 396,705,472-byte Q4 artifact and verifies its SHA-256; missing weights fail the test. It runs the real isolated service and JNI engine, checks that the worker has no INTERNET permission, requires a supported answer containing **12** and **[S1]**, then confirms the app repository remains usable. The second test cancels inference and requires cancellation to complete without terminating the foreground app. This is real model execution, not a mocked response.

Initial verification found an isolated-UID path-reopening failure and a valid model response without citation IDs. The final source uses the runtime's borrowed FILE-pointer loader and clearer citation instructions. Output validation was retained; an empty or invalid citation list is not accepted as a supported answer.

The [Android verification artifact](https://github.com/Bhargav2301/polymath_ai_news_chat/actions/runs/34330582170/artifacts/10095846353) contains the 0.3 debug APK, test/lint reports and generated Compose screen captures. GitHub authentication and artifact-retention limits apply. The public `v0.2.0-initial` download remains the earlier server-based build.

These checks establish integration and one supported-answer example. They do not establish general factual accuracy, ARM64 phone latency, sustained thermal/battery behavior, API 28/36 runtime coverage or actual 16 KB page-runtime compatibility. The default APK was built; the optional model-included APK route has not been end-to-end qualified. These remaining gates are specified in the [on-device architecture/build guide](ON_DEVICE_AI.md). Lint advisories concern widget API fallbacks, dependency-version suggestions and version-catalog usage; no baseline suppresses lint errors.

## Historical verification — Polymath 0.2

The following records describe the previous implementation and its original build environment.

## Real AI execution

The [model smoke receipt](model-smoke.json) records a real MiniLM INT8 embedding pass and a Qwen3-0.6B Q8_0 answer through the authenticated FastAPI HTTP route. llama.cpp b10834 ran on CPU. Given an original source stating a 6 ohm resistor carries 2 amperes, the model answered **12 volts** and returned the correct source citation. The test also verified that source image metadata remained associated with that citation. This is one integration example, not a general factual-accuracy or phone-performance benchmark.

The [service test report](rag-tests.xml) covers authenticated API responses, dataset isolation, duplicate IDs, empty/deleted corpora, unrecognized citations, missing references, generated URLs, malformed output, prompt data separation, unsafe media URLs, request limits, actual semantic paraphrase retrieval and complete wordpiece chunk coverage. Sixteen tests passed with real model artifacts present. Without those artifacts, fourteen tests run and two are explicitly skipped.

## Android checks

The final Android build result and test counts are recorded in [BUILD_STATUS.md](../android/BUILD_STATUS.md). The suite covers legacy behavior, Room v1 → v2 preservation, v1 recommendation replay, source-image associations, dataset removal, scoped corpus selection, citation/excerpt/revision validation, HTTPS boundaries, NDJSON transport and cancellation. The UI checks exercise actual Compose/Hilt/Room flows and image rendering under Robolectric, with captures under [screenshots](../android/docs/screenshots/).

The sample image is supplied to the rendering test through a controlled image interceptor, so UI rendering can be verified without depending on a publisher's availability. Separate parser/import tests check that the image comes from the correct source. No test claims that every external image URL will remain available.

## Repository and configuration checks

The deprecated branch was fetched from GitHub and compared with the original main HEAD. The only difference is the new `DEPRECATED.md`; all original tracked files remain intact. Repository migration commits preserve the old main as their ancestor.

`python scripts/validate_project.py` checks bundled example parity, supported topics, image asset paths and local Markdown links. CI includes Android build/tests/lint, service tests, example validation and Docker Compose configuration validation.

## Not executed here

Physical-device installation, launcher/lock-screen widget compatibility, API 28/36 device behavior, battery/thermal profiling and production load testing remain open. Docker containers and an HTTPS reverse-proxy deployment were not started in this environment; the native Python/llama.cpp service path was exercised. Fully on-device embedding/generation is not implemented in this version. Remote CI status is reported separately at handoff rather than inferred from the presence of a workflow file.
