"""Download pinned, official Apache-2.0 model artifacts; never execute remote code."""
from pathlib import Path
import argparse
import hashlib
import json
import urllib.request

ARTIFACTS = [
    ("sentence-transformers/all-MiniLM-L6-v2", "1110a243fdf4706b3f48f1d95db1a4f5529b4d41", "onnx/model_quint8_avx2.onnx", "minilm.onnx"),
    ("sentence-transformers/all-MiniLM-L6-v2", "1110a243fdf4706b3f48f1d95db1a4f5529b4d41", "tokenizer.json", "tokenizer.json"),
    ("Qwen/Qwen3-0.6B-GGUF", "23749fefcc72300e3a2ad315e1317431b06b590a", "Qwen3-0.6B-Q8_0.gguf", "qwen3-0.6b.gguf"),
]

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path("models"))
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    records = []
    for repo, revision, remote, local in ARTIFACTS:
        url = f"https://huggingface.co/{repo}/resolve/{revision}/{remote}"
        target = args.output / local
        partial = target.with_suffix(".partial")
        print(f"Downloading {repo}/{remote}", flush=True)
        with urllib.request.urlopen(url, timeout=60) as response, partial.open("wb") as output:
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
        digest = hashlib.file_digest(partial.open("rb"), "sha256").hexdigest()
        lock = json.loads((Path(__file__).resolve().parents[1] / "docs/model-lock.json").read_text())
        expected = next(item["sha256"] for item in lock if item["local"] == local)
        if digest != expected:
            partial.unlink()
            raise RuntimeError(f"Checksum mismatch for {local}; artifact not installed")
        partial.replace(target)
        records.append(dict(repo=repo, revision=revision, path=remote, local=local, sha256=digest))
    (args.output / "manifest.json").write_text(json.dumps(records, indent=2) + "\n")
    print("Model artifacts downloaded; SHA-256 receipts saved in manifest.json", flush=True)
