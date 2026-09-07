# Development scripts

Run from the repository root with Python 3.12.

| Script | Purpose |
|---|---|
| `download_models.py --output models` | Download pinned official MiniLM INT8 ONNX, tokenizer and Qwen3-0.6B Q8_0 GGUF; verify the committed SHA-256 lock before installation |
| `download_llama.py --output .tools/llama` | Download and verify the Linux x64 CPU llama.cpp b10834 runtime |
| `validate_project.py` | Check dataset/Android-asset parity, topic IDs, source images and documentation paths |
| `smoke_rag.py --output docs/model-smoke.json` | Exercise real embeddings and a running local Qwen server through the authenticated FastAPI route |

For the smoke test, install `services/rag/requirements-dev.txt`, start llama.cpp as described in the [service README](../services/rag/README.md), and set `POLYMATH_MODEL_DIR`, `LLAMA_BASE_URL` and `PYTHONPATH=services/rag`. The test uses synthetic source text and a test-only authentication key. It requires an actual model answer and checks source/media identity; it does not count a mocked response as success.

No model weights, local user data, API keys or runtime binaries belong in Git. The committed model lock records official repository revisions and checksums. Downloads fail on checksum mismatch. The llama download helper targets Linux x64; other operating systems require a compatible upstream build.
