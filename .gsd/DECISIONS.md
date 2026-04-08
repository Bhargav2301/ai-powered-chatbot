# 📓 DECISIONS.md — Architectural Decision Log
## AI-Powered Music & Movies Chatbot

---

## ✅ DECISION-001 — No `rasa init` Scaffolding
**Date**: 2026-03-30 | **Status**: 🔒 Locked
**Decision**: All Rasa files created manually, one milestone at a time.

---

## ✅ DECISION-002 — Separate NLU Files per Domain
**Date**: 2026-03-30 | **Status**: 🔒 Locked
**Decision**: `movies_nlu.yml` + `music_nlu.yml` + `general_nlu.yml`

---

## ✅ DECISION-003 — File Creation Order for Milestone 2
**Date**: 2026-03-30 | **Status**: 🔒 Locked
**Decision**: `config.yml` → `domain.yml` → NLU files → `rasa train`

---

## ✅ DECISION-004 — NLU Pipeline Components
**Date**: 2026-03-30 | **Status**: 🔒 Locked
**Decision**: WhitespaceTokenizer + DIETClassifier (100 epochs) + FallbackClassifier.
No SpaCy, no BERT. Rasa 3.6.20 / Python 3.10 / Windows confirmed.

---

## ✅ DECISION-005 — Windows Long Path Fix
**Date**: 2026-03-30 | **Status**: 🔒 Locked
**Decision**: Registry edit to enable paths > 260 chars before first `rasa train`.

---

## ✅ DECISION-006 — Python Version
**Date**: 2026-03-30 | **Status**: 🔒 Locked
**Decision**: Python 3.10.x (3.14 incompatible with Rasa 3.6.x).

---

## ✅ DECISION-007 — Data Source ⚠️ REVISED (v3 — FINAL)
**Date**: 2026-03-31 | **Status**: 🔒 Locked

**Previous**: Static JSON → Live TMDB API → **Now: Hybrid (local SQLite + live Last.fm)**

### Movies — Kaggle TMDB Dataset (Local SQLite)
**Source**: https://www.kaggle.com/datasets/asaniczka/tmdb-movies-dataset-2023-930k-movies

**Why not the live TMDB API?** API key access was unavailable.
**Why not the raw CSV?** 930k rows — loading on every action call is too slow.
**Why SQLite?** Millisecond queries, no network dependency, portable single file.

**Setup flow**:
```
1. Download TMDB_all_movies.csv from Kaggle (manually)
2. Place it at: data/TMDB_all_movies.csv
3. Run: python scripts/prepare_movies_db.py
4. This creates: data/movies.db (SQLite, gitignored)
5. actions.py queries movies.db using sqlite3
```

**SQLite schema** (created by prep script from CSV columns):
```sql
CREATE TABLE movies (
  id INTEGER PRIMARY KEY,
  title TEXT,
  overview TEXT,
  genres TEXT,
  release_date TEXT,
  vote_average REAL,
  vote_count INTEGER,
  popularity REAL,
  runtime INTEGER,
  status TEXT,
  tagline TEXT,
  original_language TEXT,
  production_companies TEXT,
  keywords TEXT
);
```

**Queries used in actions.py**:
```sql
-- Genre search (genres column contains comma-separated genre names)
SELECT title, overview, vote_average, release_date
FROM movies WHERE genres LIKE '%thriller%'
ORDER BY popularity DESC LIMIT 3;

-- Title search
SELECT * FROM movies
WHERE title LIKE '%Inception%'
ORDER BY popularity DESC LIMIT 1;

-- Now playing (approximate: released in last 6 months)
SELECT title, release_date FROM movies
WHERE release_date >= date('now', '-6 months')
AND status = 'Released'
ORDER BY popularity DESC LIMIT 4;
```

### Music — Last.fm Live API
**Auth**: API Key only (no OAuth) — key obtained ✅
**Base URL**: `https://ws.audioscrobbler.com/2.0`
**Endpoints**:
```
artist.getInfo       → Bio, listeners, play count
artist.getTopTracks  → Top tracks by artist
artist.getSimilar    → Similar artists
track.getInfo        → Track details and wiki
chart.getTopTracks   → Global trending tracks
tag.getTopTracks     → Tracks by genre tag
```

### Files gitignored (never committed):
- `data/TMDB_all_movies.csv` (too large — 200MB+)
- `data/movies.db` (regenerated from CSV)
- `.env` (contains Last.fm API key)

### Files committed:
- `scripts/prepare_movies_db.py` (the conversion script)
- `.env.example` (template, no real keys)

**New dependencies**:
```
pandas==2.0.3        ← reads the CSV during prep
requests==2.31.0     ← Last.fm API calls
python-dotenv==1.0.0 ← loads .env
```
SQLite3 is built into Python — no extra install needed.

**Impact**: `actions/actions.py`, `scripts/prepare_movies_db.py`,
`requirements.txt`, `.gitignore`, `data/movies.db`

---

## ✅ DECISION-008 — Conversation Memory Scope
**Date**: 2026-03-30 | **Status**: 🔒 Locked
**Decision**: Session-only memory. No persistent DB across sessions.

---

## ✅ DECISION-009 — API Key Management
**Date**: 2026-03-31 | **Status**: 🔒 Locked
**Decision**: `.env` file only. Never hardcoded. Loaded via `python-dotenv`.

**`.env` structure**:
```
LASTFM_API_KEY=your_lastfm_key_here
```
No TMDB key needed anymore — movies use local SQLite.

---

## ✅ DECISION-010 — HTTP Error Handling Strategy
**Date**: 2026-03-31 | **Status**: 🔒 Locked
**Decision**: All API calls wrapped in try/except, timeout=5s.
All SQLite queries wrapped in try/except with graceful fallback messages.

---

## ✅ DECISION-011 — Frontend Channel
**Date**: 2026-03-31 | **Status**: 🔒 Locked
**Decision**: Rasa REST channel + plain HTML/CSS/JS using fetch(). No SocketIO.

---

## 📋 Agent Instructions for Phase 4

> **⚠️ READ BEFORE PLANNING OR EXECUTING PHASE 4**

1. **Movies use LOCAL SQLite** — `data/movies.db` queried with `sqlite3` (built-in)
2. **Music uses Last.fm LIVE API** — key loaded from `.env` via `python-dotenv`
3. **NO TMDB API calls** — no TMDB_API_KEY needed anywhere
4. **Run prep script FIRST**: `python scripts/prepare_movies_db.py` before testing actions
5. **Install deps**: `pip install pandas==2.0.3 requests==2.31.0 python-dotenv==1.0.0`
6. **Update `.gitignore`**: add `data/TMDB_all_movies.csv` and `data/movies.db`
7. **All 9 action classes** implemented in `actions/actions.py`
8. **Every action**: docstring + try/except + SlotSet for context
9. **Test sequence**:
   ```
   Terminal 1: rasa run actions
   Terminal 2: rasa shell
   ```

---

## 🔲 PENDING DECISIONS
| ID | Question | Needed By |
|---|---|---|
| DECISION-013 | Deploy to cloud or local only? | Post-Milestone 6 |

---

## ✅ DECISION-012 — Relaxing Intent Accuracy Threshold
**Date**: 2026-04-07 | **Status**: 🔒 Locked
**Decision**: Relaxed `REQ-07` requirement from strict `≥ 90%` to simply evaluating and setting a baseline. We will note accuracy results but won't iteratively retrain the NLU dataset in Phase 6 if it falls short.

*Update this file before every GSD Discuss Phase.*
