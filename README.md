# 🎬🎵 AI-Powered Music & Movies Chatbot

> A context-aware conversational AI that understands natural language queries about movies and music — built from scratch with Rasa Open Source, a 50k-movie SQLite database, and the Last.fm live API.

[![Python 3.10](https://img.shields.io/badge/Python-3.10.8-blue?logo=python&logoColor=white)](https://www.python.org/downloads/release/python-3108/)
[![Rasa 3.6](https://img.shields.io/badge/Rasa-3.6.20-5A17EE?logo=rasa&logoColor=white)](https://rasa.com)
[![Last.fm API](https://img.shields.io/badge/Last.fm-API-D51007?logo=last.fm&logoColor=white)](https://www.last.fm/api)
[![SQLite](https://img.shields.io/badge/SQLite-3-003B57?logo=sqlite&logoColor=white)](https://sqlite.org)

---

## 📖 Table of Contents

- [What It Does](#-what-it-does)
- [Conversation Examples](#-conversation-examples)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Quick Start](#-quick-start)
- [Development Milestones](#-development-milestones)
- [NLU Design](#-nlu-design)
- [Custom Actions](#-custom-actions)
- [Data Pipeline](#-data-pipeline)
- [Key Decisions](#-key-decisions)
- [What I Learned](#-what-i-learned)

---

## 🤖 What It Does

This chatbot handles intelligent, multi-turn conversations about **movies** and **music**:

| Capability | Example |
|---|---|
| 🎬 Movie recommendations by genre | *"Recommend a thriller"* → 3 films with ratings |
| 🎵 Music recommendations by genre | *"I want jazz music"* → top Last.fm tracks |
| 🧠 Context-aware follow-ups | *"Who directed it?"* — remembers the last movie discussed |
| 🎤 Artist deep dives | *"Tell me about Taylor Swift"* → bio + listener stats |
| 📈 Live charts | *"What are the top songs?"* → real Last.fm chart data |
| 🎞️ Movie details | *"Tell me about Parasite"* → overview, rating, runtime |
| 📅 Recent releases | *"What movies are out now?"* → sorted by recency |

---

## 💬 Conversation Examples

```
You: Recommend a zombie movie
Bot: Here are some great horror movies:
     🎬 The Nun II (2023) ⭐ 6.5
     🎬 Talk to Me (2022) ⭐ 7.2
     🎬 Scream VI (2023) ⭐ 7.1
     Want details on any of these?

You: Tell me about the second one
Bot: 🎬 Talk to Me (2022)
     ⭐ 7.2/10 · 95 min · Horror
     When a group of friends discover how to conjure spirits using an
     embalmed hand, they become hooked on the new thrill...
     Want to know who made it?

You: Who made it?
Bot: 🎬 Talk to Me was produced by: Causeway Films.
     For the director, search: https://www.imdb.com/find?q=Talk+to+Me
```

```
You: Tell me about Taylor Swift
Bot: 🎵 Taylor Swift
     👥 7,234,891 listeners · ▶️ 2,891,234,099 plays
     Taylor Alison Swift is an American singer-songwriter...
     Want to hear their top tracks?

You: Recommend pop music
Bot: Here are some great pop tracks:
     🎵 Anti-Hero — Taylor Swift
     🎵 As It Was — Harry Styles
     🎵 Flowers — Miley Cyrus
```

---

## 🏗️ Architecture

```
┌───────────────────────────────────────────────────┐
│           Web Chat UI  (frontend/)                 │
│        HTML + CSS + JavaScript (fetch)             │
└─────────────────────┬─────────────────────────────┘
                      │  POST /webhooks/rest/webhook
                      ▼
┌───────────────────────────────────────────────────┐
│         Rasa Server  :5005                         │
│  NLU Pipeline (DIETClassifier, 18 intents)         │
│  Dialogue Manager (TEDPolicy, RulePolicy)          │
└─────────────────────┬─────────────────────────────┘
                      │  HTTP POST (custom action)
                      ▼
┌───────────────────────────────────────────────────┐
│       Action Server  :5055  (actions.py)           │
│         │                        │                 │
│   SQLite query             Last.fm API             │
│   data/movies.db           Real-time music         │
│   50k movies, <10ms        Charts · Artists        │
└───────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology | Version |
|---|---|---|
| NLU + Dialogue | Rasa Open Source | 3.6.20 |
| Action Server | Rasa SDK | 3.6.2 |
| Language | Python | 3.10.8 |
| Movie Database | SQLite (Kaggle TMDB) | Built-in |
| Music | Last.fm REST API | v2.0 |
| Data Pipeline | pandas | 2.0.3 |
| HTTP Client | requests | 2.31.0 |
| Secrets | python-dotenv | 1.0.0 |
| Frontend | HTML + CSS + JS | — |
| IDE | Google Antigravity | 1.0 Preview |

---

## 📁 Project Structure

```
ai-powered-chatbot/
├── PRD.md                      Product Requirements Document
├── README.md                   This file
├── DECISIONS.md                11 architectural decisions (locked)
├── requirements.txt            Pinned Python dependencies
├── config.yml                  NLU pipeline + dialogue policies
├── domain.yml                  Intents, entities, slots, responses
├── endpoints.yml               Action server config
├── .env.example                API key template (safe to commit)
├── data/
│   ├── nlu/
│   │   ├── general_nlu.yml     Universal intents
│   │   ├── movies_nlu.yml      Movie intents + entity annotations
│   │   └── music_nlu.yml       Music intents + entity annotations
│   ├── stories/
│   │   ├── movies_stories.yml  15 multi-turn movie conversation flows
│   │   └── music_stories.yml   15 multi-turn music conversation flows
│   ├── rules/
│   │   └── rules.yml           7 deterministic rules
│   └── movies.db               SQLite database (gitignored)
├── actions/
│   ├── __init__.py
│   └── actions.py              9 custom action classes
├── scripts/
│   └── prepare_movies_db.py    Kaggle CSV → SQLite ETL pipeline
├── tests/
│   └── test_stories.yml        End-to-end tests
└── frontend/
    ├── index.html
    ├── style.css
    └── script.js
```

---

## 🚀 Quick Start

### Prerequisites

- Python 3.10.x — **not 3.11+** → [Download](https://www.python.org/downloads/release/python-3108/)
- Last.fm API key (free) → [Get one](https://www.last.fm/api/account/create)
- Kaggle TMDB CSV → [Download](https://www.kaggle.com/datasets/asaniczka/tmdb-movies-dataset-2023-930k-movies)

### Setup

```bash
# 1. Clone and enter project
git clone https://github.com/Bhargav2301/ai-powered-chatbot.git
cd ai-powered-chatbot

# 2. Create virtual environment (Python 3.10 required)
py -3.10 -m venv venv
.\venv\Scripts\activate

# 3. Install dependencies
pip install -r requirements.txt

# 4. Configure API key
copy .env.example .env
# Edit .env: LASTFM_API_KEY=your_actual_key_here

# 5. Build movie database (place TMDB_all_movies.csv in data/ first)
python scripts/prepare_movies_db.py

# 6. Train the model (~5-10 mins)
rasa train

# 7. Start both servers

# Terminal 1 — Action Server
rasa run actions

# Terminal 2 — Rasa Server
rasa run --enable-api --cors "*"

# 8. Open frontend/index.html in your browser
```

### Test in CLI

```bash
rasa shell
```

---

## 🏁 Development Milestones

| # | Branch | Description | Status |
|---|---|---|---|
| 1 | `milestone-1/environment-setup` | Python 3.10, Rasa 3.6.20, venv | ✅ Complete |
| 2 | `milestone-2/nlu-training-data` | 18 intents, entities, config, domain | ✅ Complete |
| 3 | `milestone-3/domain-and-dialogue` | 30 stories, 7 rules, model trained | ✅ Complete |
| 4 | `milestone-4/custom-actions` | 9 actions, SQLite pipeline, Last.fm | ✅ Complete |
| 5 | `milestone-5/web-frontend` | HTML/CSS/JS chat widget | 🔄 In Progress |
| 6 | `milestone-6/testing-and-polish` | Tests, cross-validation, cleanup | ⏳ Pending |

---

## 🧠 NLU Design

### Intents (18 total)

**Universal:** `greet` · `goodbye` · `affirm` · `deny` · `bot_challenge` · `thank_you` · `out_of_scope`

**Movies:** `ask_movie_recommendation` · `ask_movie_by_genre` · `ask_movie_details` · `ask_movie_director` · `ask_movie_cast` · `ask_now_playing`

**Music:** `ask_song_recommendation` · `ask_song_by_genre` · `ask_artist_info` · `ask_song_details` · `ask_top_songs`

### Entities

| Entity | Example |
|---|---|
| `genre` | `I want a [thriller](genre)` |
| `movie_title` | `Tell me about [Inception](movie_title)` |
| `artist_name` | `Who is [Taylor Swift](artist_name)?` |
| `song_title` | `What is [Blinding Lights](song_title) about?` |

### Context Slots

| Slot | Enables |
|---|---|
| `current_movie` | *"Who directed it?"* without repeating the title |
| `current_artist` | *"Tell me about them"* after a song recommendation |
| `preferred_genre` | Remembers genre preference across the session |

---

## ⚡ Custom Actions

| Action | Source | Returns |
|---|---|---|
| `ActionRecommendMovie` | SQLite | 3 movies by genre or popularity |
| `ActionGetMovieDetails` | SQLite | Overview, rating, runtime, tagline |
| `ActionGetMovieDirector` | SQLite | Production company + IMDb link |
| `ActionGetMovieCast` | SQLite | IMDb redirect |
| `ActionGetNowPlaying` | SQLite | Recent releases |
| `ActionRecommendSong` | Last.fm | Tracks by genre or global chart |
| `ActionGetArtistInfo` | Last.fm | Bio, listeners, playcount |
| `ActionGetSongDetails` | Last.fm | Track stats and wiki |
| `ActionGetTopSongs` | Last.fm | Live global top 5 |

All actions: `timeout=5s` · graceful `try/except` · `SlotSet` for context memory

---

## 🗄️ Data Pipeline

```
Kaggle CSV (930k rows, ~200MB)
      │
      ├─ Filter: remove adult content
      ├─ Filter: vote_count > 50, status = Released
      ├─ Sort by popularity → keep top 50,000
      ├─ Normalise genres to lowercase
      └─ Write SQLite with 4 indexes
             │
         movies.db (~15MB)
         Genre query: <10ms (was 60s from raw CSV)
```

Run: `python scripts/prepare_movies_db.py`

---

## 📋 Key Decisions

Full log in `DECISIONS.md`. Highlights:

| Decision | Why |
|---|---|
| No `rasa init` | Understand every file before using it |
| Python 3.10 strictly | Only compatible version with Rasa 3.6.x |
| Kaggle CSV + SQLite over TMDB API | API unavailable; SQLite is faster and offline |
| Last.fm over Spotify | No OAuth needed — API key only |
| Session-only memory | Sufficient for portfolio scope |
| `python-dotenv` for secrets | Industry standard; no keys in Git |

---

## 🎓 What I Learned

**NLP:** Intent vs entity extraction · Slot-based context memory · Rules vs ML stories · Training data annotation requirements

**Engineering:** Python version pinning for ML · ETL pipelines for large datasets · Defensive error handling · API key security

**Workflow:** Plan-Discuss-Execute prevents rework · DECISIONS.md keeps AI agents consistent · Milestone branching creates readable Git history

---

## 👤 Author

**Bhargav DVS** — M.S. Engineering Science (Data Science), University at Buffalo

[![LinkedIn](https://img.shields.io/badge/LinkedIn-dvs--bhargav-0077B5?logo=linkedin)](https://linkedin.com/in/dvs-bhargav)
[![GitHub](https://img.shields.io/badge/GitHub-Bhargav2301-181717?logo=github)](https://github.com/Bhargav2301)

---

*Built as a portfolio project demonstrating NLU, dialogue management, data engineering, and API integration.*
