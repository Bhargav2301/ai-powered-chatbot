import asyncio
from contextlib import asynccontextmanager
import json
import os
from pathlib import Path
import secrets
import anyio

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import StreamingResponse
from pydantic import ValidationError

from .generation import LlamaGenerator, answer
from .retrieval import MiniLM, retrieve
from .schema import ChatRequest


def create_app(embedder=None, generator=None, api_key=None):
    key = api_key if api_key is not None else os.environ.get("POLYMATH_API_KEY", "")
    if len(key) < 24:
        raise RuntimeError("Set POLYMATH_API_KEY to a random secret of at least 24 characters")
    inference_slot = asyncio.Semaphore(1)

    @asynccontextmanager
    async def lifespan(app):
        app.state.embedder = embedder or MiniLM(Path(os.environ.get("POLYMATH_MODEL_DIR", "models")))
        app.state.generator = generator or LlamaGenerator(os.environ.get("LLAMA_BASE_URL", "http://127.0.0.1:8081"))
        yield

    app = FastAPI(title="Polymath private RAG", version="0.2.0", lifespan=lifespan,
                  docs_url=None, redoc_url=None, openapi_url=None)

    @app.middleware("http")
    async def authenticate(request: Request, call_next):
        from fastapi.responses import JSONResponse
        expected = "Bearer " + key
        if not secrets.compare_digest(request.headers.get("authorization", "").encode(), expected.encode()):
            return JSONResponse({"detail": "Unauthorized"}, 401)
        return await call_next(request)

    @app.get("/health")
    async def health():
        return {"status": "ready", "version": "0.2.0", "storage": "stateless", "embedding": "all-MiniLM-L6-v2", "generation": "Qwen3-0.6B"}

    @app.post("/v1/chat")
    async def chat(request: Request):
        raw = bytearray()
        async for part in request.stream():
            raw.extend(part)
            if len(raw) > 2 * 1024 * 1024:
                raise HTTPException(413, "Request exceeds 2 MiB")
        try:
            payload = ChatRequest.model_validate_json(raw)
        except ValidationError:
            # Do not echo private documents or validation input in responses/logs.
            raise HTTPException(422, "Invalid dataset request or size limit exceeded") from None
        try:
            await asyncio.wait_for(inference_slot.acquire(), timeout=.05)
        except TimeoutError:
            raise HTTPException(429, "Inference is busy; retry shortly") from None

        async def events():
            retrieval_task = None
            try:
                yield line("status", {"message": "Retrieving selected evidence"})
                query = "\n".join(payload.previous_questions[-1:] + [payload.question])
                retrieval_task = asyncio.create_task(asyncio.to_thread(retrieve, query, payload.documents, app.state.embedder))
                chunks = await asyncio.shield(retrieval_task)
                if await request.is_disconnected():
                    return
                yield line("status", {"message": "Generating a cited answer", "passages": len(chunks)})
                result = await answer(payload, chunks, app.state.generator)
                yield line("answer", result.model_dump())
            except asyncio.CancelledError:
                raise
            except Exception:
                yield line("error", {"message": "Inference failed. Check the model service, or use a smaller dataset and retry."})
            finally:
                # Cancellation must not release the tokenizer/session slot while its CPU thread
                # is still running. The bounded request finishes without publishing its result.
                try:
                    if retrieval_task is not None and not retrieval_task.done():
                        with anyio.CancelScope(shield=True):
                            await asyncio.shield(retrieval_task)
                finally:
                    inference_slot.release()

        # No unvalidated token text is displayed: final answer arrives after citation checks.
        return StreamingResponse(events(), media_type="application/x-ndjson",
                                 headers={"Cache-Control": "no-store", "X-Accel-Buffering": "no"})

    return app


def line(kind, value):
    return json.dumps({"type": kind, **value}, ensure_ascii=False) + "\n"
