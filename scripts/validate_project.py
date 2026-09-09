"""Validate shipped examples and local Markdown links without fetching remote URLs."""
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
TOPICS = {"systems", "ai", "design", "science", "math", "craft", "history", "philosophy", "literature", "economics", "engineering", "certifications"}

example = ROOT / "datasets/foundations.json"
assert example.read_bytes() == (ROOT / "android/app/src/main/assets/foundations.json").read_bytes(), "Example asset drift"
value = json.loads(example.read_text())
assert value["schema_version"] == 1
for document in value["documents"]:
    assert document["topic_id"] in TOPICS
    assert document["source_url"].startswith("https://")
    for image in document.get("images", []):
        assert image["url"].startswith("https://") and image["alt"]
        if "/main/datasets/assets/" in image["url"]:
            assert (ROOT / "datasets/assets" / image["url"].rsplit("/", 1)[-1]).is_file()
    quiz = document.get("quiz")
    if quiz:
        assert 0 <= quiz["correct_answer"] < len(quiz["answers"])

for file in ROOT.rglob("*.md"):
    if any(part in {".git", ".native", "build", ".gradle", ".tools", ".venv"} for part in file.parts):
        continue
    for link in re.findall(r"\]\(([^)]+)\)", file.read_text()):
        if "://" in link or link.startswith("#") or "{" in link:
            continue
        target = file.parent / link.split("#", 1)[0]
        assert target.exists(), f"Broken local link in {file.relative_to(ROOT)}: {link}"
print("Dataset, source image, bundled asset and Markdown link checks passed.")
