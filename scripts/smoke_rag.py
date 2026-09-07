"""Run real MiniLM + a running Qwen llama.cpp server through the authenticated RAG HTTP API."""
import argparse
import json
import os
from pathlib import Path
import time
from fastapi.testclient import TestClient
from polymath_rag.app import create_app

parser = argparse.ArgumentParser()
parser.add_argument("--output", type=Path, default=Path("docs/model-smoke.json"))
args = parser.parse_args()
key = "smoke-test-only-not-a-deployment-key"
request = {
    "question": "What voltage is needed to drive 2 amperes through a 6 ohm resistor?",
    "dataset_ids": ["physics"],
    "documents": [{"id": "ohm", "dataset_id": "physics", "revision": 1, "title": "Ohm's law worked example",
        "text": "For an ohmic resistor at fixed temperature, voltage equals current multiplied by resistance: V = I times R. A 6 ohm resistor carrying 2 amperes has a potential difference of 12 volts.",
        "source_url": "https://openstax.org/books/university-physics-volume-2/pages/9-4-ohms-law",
        "images": [{"url": "https://example.org/circuit.png", "alt": "Test source image metadata; not fetched by the model"}]}]
}
start = time.monotonic()
with TestClient(create_app(api_key=key)) as client:
    response = client.post("/v1/chat", json=request, headers={"Authorization": "Bearer " + key})
    response.raise_for_status()
    events = [json.loads(line) for line in response.iter_lines()]
    final = events[-1]
    assert final["type"] == "answer", final
    assert final["status"] == "answered", final
    assert "12" in final["answer"], final
    assert final["citations"][0]["document_id"] == "ohm"
    assert final["citations"][0]["images"][0]["url"] == "https://example.org/circuit.png"
    result = {"test": "real-model authenticated HTTP RAG", "models": ["all-MiniLM-L6-v2 INT8", "Qwen3-0.6B Q8_0"],
        "runtime": "llama.cpp b10834 CPU; ONNX Runtime 1.22.1", "elapsed_seconds": round(time.monotonic() - start, 2),
        "question": request["question"], "response": final, "image_check": "Metadata provenance only; model does not interpret images", "passed": True}
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(result, indent=2))
