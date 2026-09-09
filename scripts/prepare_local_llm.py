#!/usr/bin/env python3
"""Prepare the pinned native source. Optionally download the verified model / APK asset directory."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
LOCK = json.loads((ROOT / "docs/local-model-lock.json").read_text())

def prepare_source():
    source = ROOT / ".native/llama.cpp"
    if not (source / ".git").exists():
        source.mkdir(parents=True, exist_ok=True)
        subprocess.run(["git", "init", str(source)], check=True)
        subprocess.run(["git", "-C", str(source), "remote", "add", "origin", "https://github.com/ggml-org/llama.cpp.git"], check=True)
        subprocess.run(["git", "-C", str(source), "fetch", "--depth", "1", "origin", LOCK["llama_commit"]], check=True)
        subprocess.run(["git", "-C", str(source), "checkout", "--detach", LOCK["llama_commit"]], check=True)
    actual = subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip()
    if actual != LOCK["llama_commit"]:
        raise SystemExit("Existing native source is not the pinned commit. Use a separate checkout; it was not modified.")
    if subprocess.check_output(["git", "-C", str(source), "status", "--porcelain"], text=True).strip():
        raise SystemExit("Native source has changes. Refusing an unverified runtime build.")
    print("Native source verified:", actual)

def verify(path):
    digest = hashlib.sha256()
    with path.open("rb") as data:
        for chunk in iter(lambda: data.read(1024 * 1024), b""):
            digest.update(chunk)
    return path.stat().st_size == LOCK["bytes"] and digest.hexdigest() == LOCK["sha256"]

def download_model(directory):
    directory.mkdir(parents=True, exist_ok=True)
    target = directory / LOCK["file"]
    if target.exists() and verify(target):
        print("Verified model:", target)
        return
    partial = target.with_suffix(".part")
    url = f'https://huggingface.co/{LOCK["repository"]}/resolve/{LOCK["revision"]}/{LOCK["file"]}'
    try:
        with urllib.request.urlopen(url, timeout=60) as response, partial.open("wb") as output:
            total = 0
            while chunk := response.read(1024 * 1024):
                total += len(chunk)
                if total > LOCK["bytes"]:
                    raise ValueError("Download exceeds the pinned model size")
                output.write(chunk)
            output.flush()
            os.fsync(output.fileno())
        if not verify(partial):
            raise ValueError("Model SHA-256 or length does not match the lock file")
        partial.replace(target)
    finally:
        partial.unlink(missing_ok=True)
    print("Verified model:", target)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--model-dir", type=Path, help="Download the approved GGUF into this directory")
    parser.add_argument("--bundle-assets", type=Path, help="Download into <directory>/models for -PpolymathModelAssets")
    args = parser.parse_args()
    prepare_source()
    if args.model_dir:
        download_model(args.model_dir)
    if args.bundle_assets:
        download_model(args.bundle_assets / "models")
