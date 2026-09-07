# Knowledge datasets

`foundations.json` is an importable example with engineering, history, philosophy and professional-security concepts. The same bytes ship in Android assets, accessible from **Chat → Datasets → Add example dataset**. `assets/ohms-law.png` is an original Polymath illustration; it is not copied from a publisher.

## Import schema v1

```json
{
  "schema_version": 1,
  "id": "my-course",
  "title": "My course",
  "documents": [{
    "id": "lesson-one",
    "title": "A useful concept",
    "topic_id": "engineering",
    "source_url": "https://example.org/lesson",
    "publisher": "Source publisher",
    "format": "text",
    "text": "The source material used to answer questions.",
    "summary": "A short flashcard face.",
    "images": [{"url": "https://example.org/figure.png", "alt": "Describe the figure", "caption": "Attribution and context"}],
    "quiz": {"question": "A recall question?", "answers": ["First", "Second"], "correct_answer": 0, "explanation": "Why the first answer is correct."}
  }]
}
```

`images`, `quiz`, `summary`, `publisher` and `format` are optional. `format: "html"` strips markup from source text while extracting `img` elements before stripping. Relative image URLs resolve against that document's `source_url`. Explicit image metadata and HTML image references are deduplicated. An image is associated only with its source document; images are never borrowed from a neighboring document.

Supported topic IDs: `systems`, `ai`, `design`, `science`, `math`, `craft`, `history`, `philosophy`, `literature`, `economics`, `engineering`, `certifications`.

IDs use 1–64 ASCII letters, digits, underscores or hyphens. Dataset IDs `vault` and `public` are reserved. Each import has 1–150 unique source IDs, at most 300,000 text/title characters, 20,000 characters per source, six images per source and 2 MiB total JSON. A repeated dataset ID is rejected; remove it before importing a replacement. Keep original source text and image attribution accurate. Only include material you are entitled to store and display.

The source URL and image URLs must be HTTPS without credentials. The app does not fetch the article to discover additional images; include them in `images` or supplied HTML. RSS/Atom supports per-entry Media RSS thumbnails/content, image enclosures, HTML images and Atom image enclosure links. Failed image loads retain alt text and source access. Images are displayed; the text model does not analyze their pixels.

Imported sources are available in Discover. Like/save adds one to My vault; selecting the named dataset in Chat queries that dataset directly. Deleting it removes its cards and saved copies and clears copied chat excerpts. User notes and earned EXP remain.
