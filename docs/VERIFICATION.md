# Verification record — Polymath 0.2

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
