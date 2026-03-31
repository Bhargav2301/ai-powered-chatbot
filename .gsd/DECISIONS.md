# 📓 DECISIONS.md — Architectural Decision Log
## AI-Powered Music & Movies Chatbot

> This file captures every key decision made during the project.
> It exists so the Antigravity GSD agent has a single source of truth
> before executing any phase. Update this file before each milestone.

---

## ✅ DECISION-001 — No `rasa init` Scaffolding
**Date**: 2026-03-30 | **Status**: 🔒 Locked

**Decision**: Do NOT run `rasa init`. All Rasa files created manually, one milestone at a time.

**Impact**: Milestone 2, 3, 4 — all files created from scratch

---

## ✅ DECISION-002 — Separate NLU Files per Domain
**Date**: 2026-03-30 | **Status**: 🔒 Locked

**Decision**: `data/nlu/movies_nlu.yml` + `data/nlu/music_nlu.yml` + `data/nlu/general_nlu.yml`

**Impact**: `data/nlu/` directory structure

---

## ✅ DECISION-003 — File Creation Order for Milestone 2
**Date**: 2026-03-30 | **Status**: 🔒 Locked

**Decision**: `config.yml` → `domain.yml` → NLU files → `rasa train`

---

## ✅ DECISION-004 — NLU Pipeline Components
**Date**: 2026-03-30 | **Status**: 🔒 Locked

**Decision**: `WhitespaceTokenizer` + dual `CountVectorsFeaturizer` + `DIETClassifier` (100 epochs) + `EntitySynonymMapper` + `FallbackClassifier`. No SpaCy, no BERT. Confirmed compatible with Rasa 3.6.20 / Python 3.10 / Windows.

---

## ✅ DECISION-005 — Windows Long Path Fix
**Date**: 2026-03-30 | **Status**: 🔒 Locked

**Decision**: Enable Windows long path support via PowerShell registry edit before first `rasa train`.

---

## ✅ DECISION-006 — Python Version
**Date**: 2026-03-30 | **Status**: 🔒 Locked

**Decision**: Python 3.10.x (confirmed after failed install on Python 3.14).

---

## ✅ DECISION-007 — Data Source ⚠️ REVISED
**Date**: 2026-03-31 | **Previous**: Static JSON files | **Status**: 🔒 Locked (UPDATED)

**Decision**: Use **live external APIs** for all movie and music data.

| Domain | API | Base URL | Auth |
|---|---|---|---|
| 🎬 Movies | TMDB (The Movie Database) | `https://api.themoviedb.org/3` | API Key (query param) |
| 🎵 Music | Last.fm | `https://ws.audioscrobbler.com/2.0` | API Key (query param) |

**API Keys stored in**: `.env` file (gitignored — NEVER committed)
**Loaded via**: `python-dotenv` in `actions/actions.py`

**Rationale**:
- Static JSON limits the bot to manually curated, stale data
- Live APIs provide real-time, comprehensive, and accurate responses
- Both TMDB and Last.fm use simple API key auth — no OAuth complexity
- TMDB is the industry standard (used by Netflix, HBO, Apple TV)
- Last.fm has 40M+ tracks with artist bios, top tracks, and similar artist recommendations
- Significantly stronger portfolio signal than a hardcoded JSON lookup

**Alternatives Rejected**:
- Static JSON: Too limited and stale for a functional chatbot
- Spotify API: Requires OAuth 2.0 — significant added complexity
- MusicBrainz: No API key needed but lacks popularity/chart data
- OMDB: Smaller and less reliable than TMDB

**TMDB Endpoints Used**:
```
GET /search/movie?query={title}          → Search movie by title
GET /movie/{id}                          → Full movie details
GET /movie/{id}/credits                  → Cast and director
GET /discover/movie?with_genres={id}     → Movies by genre
GET /genre/movie/list                    → Genre name → ID mapping
GET /movie/now_playing                   → Currently in cinemas
```

**Last.fm Endpoints Used**:
```
artist.getInfo?artist={name}             → Artist bio and stats
artist.getTopTracks?artist={name}        → Top tracks by artist
artist.getSimilar?artist={name}          → Similar artists
track.getInfo?artist={a}&track={t}       → Track details
chart.getTopTracks                       → Global trending tracks
tag.getTopTracks?tag={genre}             → Tracks by genre tag
```

**New dependencies**:
```
requests==2.31.0
python-dotenv==1.0.0
```

**Impact**: `actions/actions.py`, `requirements.txt`, `.env`, `.env.example`
**No** `data/movies.json` or `data/music.json` files needed.

---

## ✅ DECISION-008 — Conversation Memory Scope
**Date**: 2026-03-30 | **Status**: 🔒 Locked

**Decision**: Session-only memory. No persistent database across sessions.

**Impact**: `domain.yml` slots, `endpoints.yml`

---

## ✅ DECISION-009 — API Key Management
**Date**: 2026-03-31 | **Status**: 🔒 Locked

**Decision**: API keys stored in `.env` only. Loaded via `python-dotenv`. Never hardcoded.

**`.env` structure** (create manually, never commit):
```
TMDB_API_KEY=your_tmdb_key_here
LASTFM_API_KEY=your_lastfm_key_here
```

**`.env.example`** (safe to commit — placeholder values only):
```
TMDB_API_KEY=your_tmdb_api_key_here
LASTFM_API_KEY=your_lastfm_api_key_here
```

**Loading pattern**:
```python
from dotenv import load_dotenv
import os
load_dotenv()
TMDB_API_KEY = os.getenv("TMDB_API_KEY")
LASTFM_API_KEY = os.getenv("LASTFM_API_KEY")
```

**Impact**: `actions/actions.py`, `.gitignore` (`.env` already listed), new `.env.example`

---

## ✅ DECISION-010 — HTTP Error Handling Strategy
**Date**: 2026-03-31 | **Status**: 🔒 Locked

**Decision**: Every API call wrapped in try/except with graceful fallback. Timeout = 5 seconds on all requests. Action server must never crash on network failure.

**Standard pattern**:
```python
try:
    response = requests.get(url, params=params, timeout=5)
    response.raise_for_status()
    data = response.json()
except requests.exceptions.Timeout:
    dispatcher.utter_message("Sorry, the request timed out. Try again!")
    return []
except requests.exceptions.RequestException:
    dispatcher.utter_message("I couldn't fetch that right now. Try again later!")
    return []
```

**Impact**: All 9 action classes in `actions/actions.py`

---

## ✅ DECISION-011 — Frontend Channel
**Date**: 2026-03-31 | **Status**: 🔒 Locked

**Decision**: Rasa REST channel (`/webhooks/rest/webhook`) + plain HTML/CSS/JS using `fetch()`. No SocketIO.

**Impact**: Milestone 5, `credentials.yml`, `frontend/script.js`

---

## 📋 Agent Instructions for Phase 4

> **⚠️ READ BEFORE PLANNING OR EXECUTING PHASE 4**

1. **No static JSON** — all data from TMDB and Last.fm live APIs
2. **Install new deps first**: `pip install requests==2.31.0 python-dotenv==1.0.0`
3. **Update `requirements.txt`** to include `requests` and `python-dotenv`
4. **Create `.env.example`** (commit this) and `.env` (never commit — gitignored)
5. **Implement all 9 action classes** in `actions/actions.py`
6. **Every action must have**: docstring, try/except with timeout=5, slot updates
7. **Create `endpoints.yml`** pointing action server to `http://localhost:5055/webhook`
8. **Test sequence after implementation**:
   ```
   Terminal 1 (venv active): rasa run actions
   Terminal 2 (venv active): rasa shell
   ```
9. **Developer is a beginner** — comment every API call explaining what it fetches and why

---

## 🔲 PENDING DECISIONS

| ID | Question | Needed By |
|---|---|---|
| DECISION-012 | Deploy to cloud or local only? | Post-Milestone 6 |

---

*Update this file before every GSD Discuss Phase.*
