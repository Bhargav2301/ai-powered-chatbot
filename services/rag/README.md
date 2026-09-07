# Private RAG service — 0.2.0

An authenticated FastAPI service for the Android app. It uses [all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2) (INT8 ONNX, 384 dimensions) for retrieval and [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B-GGUF) (Q8_0 GGUF, llama.cpp) for generated answers. Both model families use Apache-2.0 licenses. The service does not store documents, conversations or private embeddings between requests. No proprietary API fallback is included.

## Local setup: Linux x64

Python 3.12, approximately 700 MB for model artifacts, and additional memory for model execution are required. Measure resource use on your own host; phone benchmarks are not implied by the server smoke test.

From the repository root:

```bash
python3.12 -m venv .venv
. .venv/bin/activate
pip install -r services/rag/requirements-dev.txt
python scripts/download_models.py --output models
python scripts/download_llama.py --output .tools/llama
```

The download helpers pin revisions/runtime and verify SHA-256. They do not execute downloaded Python model code.

Start the model in terminal 1:

```bash
.tools/llama/llama-b10834/llama-server \
  -m models/qwen3-0.6b.gguf --alias Qwen3-0.6B \
  --host 127.0.0.1 --port 8081 -c 4096 -t 4 --parallel 1 \
  --jinja --reasoning off --no-webui
```

Start the authenticated service in terminal 2, after activating the same environment:

```bash
export POLYMATH_API_KEY="$(python -c 'import secrets; print(secrets.token_urlsafe(32))')"
export POLYMATH_MODEL_DIR="$PWD/models"
export LLAMA_BASE_URL=http://127.0.0.1:8081
export PYTHONPATH=services/rag
uvicorn polymath_rag.app:create_app --factory --host 127.0.0.1 --port 8000 --no-access-log
```

Copy your generated API key into the Android Connection form through your local terminal or secret manager. The key must be at least 24 characters. Never commit it. If the key is lost, restart the service with a new random key and update the client.

For the debug Android app, use `adb reverse tcp:8000 tcp:8000` and connect to `http://127.0.0.1:8000`; the emulator can use `http://10.0.2.2:8000`. For other hosts, put the API behind an HTTPS reverse proxy and configure that origin in Android. Never expose the raw, unauthenticated llama port to the internet.

## Container configuration

After downloading `models/`, set `POLYMATH_API_KEY` in your shell and run from the repository root:

```bash
docker compose up --build -d
```

The API binds only `127.0.0.1:8000`. The inference container has no host port; an internal Docker network connects it to the API. Containers run without root, with read-only root filesystems and read-only model mounts. An HTTPS proxy is still required for remote release clients. This container configuration is supplied for deployment; see [verification](../../docs/VERIFICATION.md) for which deployment checks were actually run.

## API contract

All endpoints require `Authorization: Bearer <POLYMATH_API_KEY>`.

- `GET /health`: service metadata after the embedding runtime has loaded. It reports configured generation identity; a successful chat is the generation readiness check.
- `POST /v1/chat`: a question, selected dataset IDs, current source documents and up to three previous user questions. Returns newline-delimited JSON status events and a final validated answer or error. It does not stream unvalidated token text.

Example request:

```json
{
  "question": "What voltage does the example require?",
  "dataset_ids": ["physics"],
  "previous_questions": [],
  "documents": [{
    "id": "source:ohm", "dataset_id": "physics", "revision": 1,
    "title": "Ohm's law", "text": "A 6 ohm resistor carrying 2 amperes requires 12 volts.",
    "source_url": "https://example.org/physics", "images": []
  }]
}
```

The final `answer` event contains `status`, `answer`, `citations`, `model` and `embedding_model`. Each citation includes a source ID, dataset ID, revision, title, exact excerpt, Unicode code-point offsets, original URL and source image metadata. Status is `answered`, `insufficient_evidence` or `unverified`. Citation IDs supplied by the model are checked against retrieved sources; if the model supplies valid structured IDs without inline markers, the renderer appends those same IDs.

HTTP failures: 401 invalid key; 413 over 2 MiB; 422 invalid scope/schema/limits; 429 active inference request. Post-stream failures return an `error` event. Android displays failures and supports retry/stop.

## Retrieval and privacy boundaries

1. Validate the corpus: 150 unique sources, 300,000 title/text characters, 20,000 characters per source, at most six HTTPS images per source. Documents outside the selected dataset IDs are rejected.
2. Split source text into 192-wordpiece chunks with 40-wordpiece overlap. Disable tokenizer padding/truncation during splitting so the end of each document remains searchable.
3. Compute request-local MiniLM embeddings in batches of 16, attention-masked mean pooling and L2 normalization. No cross-user/document embedding cache is retained.
4. Rank by exact cosine similarity and BM25, fuse the first 20 results using RRF with constant 60, apply a configurable-in-code cosine relevance gate of 0.24, and select up to four passages.
5. Give Qwen only these passages and conversation questions. Documents are untrusted data, not tool instructions. The model has no browsing, file access or action tools.
6. Validate the answer and citation IDs. Android independently checks local source revisions and exact excerpts and reconstructs all source links/media from local data.

The 0.24 relevance threshold is a starting configuration, not a calibrated confidence score. A known citation can still accompany an unsupported claim. Users must be able to inspect evidence. New sources or a larger model may improve answer quality; neither removes that limitation.

The service receives the selected corpus for every question and recomputes embeddings. This favors isolation and simple deletion semantics over large-corpus throughput. Payloads and prompts are not logged by the supplied API; keep reverse-proxy request-body logging disabled. The raw llama runtime may retain its current KV state in memory; restart it when retiring a private session/host. No persistent prompt cache is configured. Use a dedicated trusted host, not an unreviewed shared public endpoint.

## Tests

```bash
PYTHONPATH=services/rag python -m pytest services/rag/tests -q
POLYMATH_MODEL_DIR="$PWD/models" PYTHONPATH=services/rag python -m pytest services/rag/tests -q
POLYMATH_MODEL_DIR="$PWD/models" PYTHONPATH=services/rag python scripts/smoke_rag.py
```

The final command requires the real llama.cpp server. It checks an actual generated answer and citation/image provenance. The model does not inspect image pixels. Standalone tests use controlled generators for error and isolation cases; two tests additionally exercise real MiniLM when model artifacts are configured.
