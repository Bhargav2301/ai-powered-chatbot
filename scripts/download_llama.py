"""Install the verified Linux x64 CPU llama.cpp runtime used by the model smoke test."""
import argparse
import hashlib
from pathlib import Path
import tarfile
import urllib.request

VERSION = "b10834"
SHA256 = "3850c750a2f3280beb7c96cc5dad4ada5b53287e978ad2ffc998dace4975f3ba"

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path(".tools/llama"))
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    archive = args.output / "llama.tar.gz"
    url = f"https://github.com/ggml-org/llama.cpp/releases/download/{VERSION}/llama-{VERSION}-bin-ubuntu-x64.tar.gz"
    with urllib.request.urlopen(url, timeout=60) as response, archive.open("wb") as output:
        while block := response.read(1024 * 1024):
            output.write(block)
    with archive.open("rb") as source:
        if hashlib.file_digest(source, "sha256").hexdigest() != SHA256:
            archive.unlink()
            raise RuntimeError("Runtime checksum mismatch")
    with tarfile.open(archive) as package:
        package.extractall(args.output, filter="data")
    (args.output / f"llama-{VERSION}/llama-server").chmod(0o755)
    archive.unlink()
    print(f"Verified llama.cpp {VERSION} installed in {args.output}")
