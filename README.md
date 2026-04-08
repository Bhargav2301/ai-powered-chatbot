# 🎬🎵 AI-Powered Music & Movies Chatbot

![Python](https://img.shields.io/badge/Python-3.10-3776AB?logo=python&logoColor=white)
![Rasa](https://img.shields.io/badge/Rasa-3.6.20-5A17EE?logo=rasa&logoColor=white)
![Last.fm](https://img.shields.io/badge/Last.fm-API-D51007?logo=lastdotfm&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-Database-003B57?logo=sqlite&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

A context-aware conversational AI chatbot built with **Rasa Open Source** that can hold intelligent, multi-turn conversations about music and movies. It recognizes user intents, remembers context across turns, and responds using custom actions backed by live APIs and a local database.

---

## ✨ Features

- **🎬 Movie Recommendations** — Genre-filtered, randomized results from a local SQLite database (50,000+ movies from TMDB)
- **🎵 Music Recommendations** — Live song recommendations via the Last.fm API
- **🎤 Artist Biographies** — Listener counts, play counts, and artist bios from Last.fm
- **🧠 Multi-Turn Context** — 3+ turn conversations using slot memory (e.g., "recommend a thriller" → "tell me about the first one" → "who directed it?")
- **🔄 Dynamic Discovery** — Backend intuitively loops and randomizes selections when you ask for "more" or "other options"
- **🔒 Adult Content Filtering** — Explicit content filtered from both movie results and Last.fm bios
- **💬 Web Chat UI** — Glassmorphic dark-mode interface with typing indicators, Markdown formatting, and a clear-chat button
- **🤖 15+ Intents** — Covers greetings, genre requests, follow-ups, affirmations, denials, and out-of-scope handling
- **⚡ NLU Fallback** — Graceful handling of unrecognized input

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                  WEB CHAT UI                         │
│            HTML / CSS / JavaScript                   │
│              (frontend/)                             │
└────────────────────┬────────────────────────────────┘
                     │ HTTP POST /webhooks/rest/webhook
                     ▼
┌─────────────────────────────────────────────────────┐
│              RASA SERVER (Port 5005)                  │
│  ┌───────────────┐      ┌────────────────────────┐  │
│  │  NLU Pipeline │      │  Dialogue Management   │  │
│  │  ───────────  │      │  ──────────────────    │  │
│  │  Tokenizer    │ ───► │  Stories / Rules        │  │
│  │  Featurizer   │      │  Slots & Entities       │  │
│  │  DIET Clf.    │      │  TED Policy             │  │
│  └───────────────┘      └────────────────────────┘  │
│                                    │                 │
│                                    ▼                 │
│                      HTTP POST to Action Server      │
└────────────────────────────────────┬────────────────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │  RASA ACTION SERVER (Port 5055)  │
                    │  actions/actions.py               │
                    └────────────────┬────────────────┘
                                     │
              ┌──────────────────────┴──────────────────┐
              │                                         │
     ┌────────┴────────┐                    ┌───────────┴──────────┐
     │  SQLite Database │                    │   Last.fm Live API   │
     │  data/movies.db  │                    │  (music data)        │
     │  (TMDB movies)   │                    │                      │
     └─────────────────┘                    └──────────────────────┘
```

---

## 📋 Prerequisites

- **Python 3.10** (required for Rasa 3.x compatibility)
- **pip** and **virtualenv**
- **Last.fm API Key** — [Get one here](https://www.last.fm/api/account/create) (free)
- **Git**

---

## 🚀 Quick Start

### 1. Clone the repository
```bash
git clone https://github.com/Bhargav2301/ai-powered-chatbot.git
cd ai-powered-chatbot
```

### 2. Create and activate virtual environment
```bash
python -m venv venv

# Windows
.\venv\Scripts\activate

# macOS/Linux
source venv/bin/activate
```

### 3. Install dependencies
```bash
pip install rasa==3.6.20
pip install rasa-sdk requests python-dotenv
```

### 4. Set up your Last.fm API key
Create a `.env` file in the project root:
```
LASTFM_API_KEY=your_api_key_here
```

### 5. Prepare the movie database
```bash
python scripts/prepare_movies_db.py
```
This converts the TMDB CSV into a SQLite database at `data/movies.db`.

### 6. Train the model
```bash
rasa train
```

### 7. Run the chatbot

Open **two terminals** (both with venv activated):

**Terminal 1 — Action Server:**
```bash
rasa run actions
```

**Terminal 2 — Rasa Server (for web UI):**
```bash
rasa run --enable-api --cors "*"
```

Then open `frontend/index.html` in your browser.

**Or use the CLI:**
```bash
rasa shell
```

---

## 📁 Project Structure

```
ai-powered-chatbot/
├── actions/
│   └── actions.py            # Custom actions (SQLite + Last.fm API)
├── data/
│   ├── nlu/
│   │   ├── general_nlu.yml   # Greet, goodbye, affirm, deny intents
│   │   ├── movies_nlu.yml    # Movie-related intents & entities
│   │   └── music_nlu.yml     # Music-related intents & entities
│   ├── stories/
│   │   ├── movies_stories.yml # Movie conversation flows
│   │   └── music_stories.yml  # Music conversation flows
│   ├── rules/
│   │   └── rules.yml         # Deterministic rules (greet, fallback)
│   └── movies.db             # SQLite database (generated)
├── frontend/
│   ├── index.html            # Chat UI shell
│   ├── style.css             # Glassmorphic dark theme
│   └── script.js             # REST API client logic
├── scripts/
│   └── prepare_movies_db.py  # CSV → SQLite converter
├── tests/
│   └── test_stories.yml      # 18 E2E test stories
├── config.yml                # NLU pipeline & policy config
├── credentials.yml           # REST webhook channel config
├── domain.yml                # Intents, entities, slots, responses
├── endpoints.yml             # Action server endpoint
└── .env                      # Last.fm API key (not committed)
```

---

## 🏁 Milestone Tracker

| # | Milestone | Status | Branch |
|---|-----------|--------|--------|
| 1 | Environment Setup | ✅ Complete | `milestone-1/environment-setup` |
| 2 | NLU Training Data | ✅ Complete | `milestone-2/nlu-training-data` |
| 3 | Domain & Dialogue | ✅ Complete | `milestone-3/domain-and-dialogue` |
| 4 | Custom Actions | ✅ Complete | `milestone-4/custom-actions` |
| 5 | Web Frontend | ✅ Complete | `milestone-5/web-frontend` |
| 6 | Testing & Polish | ✅ Complete | `milestone-6/testing-and-polish` |

---

## 🧪 Running Tests

```bash
# Activate venv
.\venv\Scripts\activate

# Start action server (required for custom action tests)
rasa run actions

# In another terminal, run E2E story tests
rasa test --stories tests/test_stories.yml --no-plot

# Run NLU cross-validation
rasa test nlu --nlu data --cross-validation --folds 3 --no-plot
```

### Test Coverage
- 18 end-to-end test stories covering rules, intent routing, context awareness, and fallback
- Action-level accuracy: **89.6%** (100% per-action precision/recall)
- Entity prediction: **100% correct**

---

## ⚠️ Known Limitations

- **No director/cast data** — The TMDB CSV doesn't include credits; the bot provides IMDb links for these queries
- **Dataset age** — Movie data reflects the CSV snapshot date, not real-time releases
- **No authentication** — The web UI uses a fixed sender ID; session resets on page refresh
- **Last.fm rate limits** — Heavy usage may hit the free API tier limits
- **No voice input** — Text-only interface

---

## 🔑 Getting a Last.fm API Key

1. Go to [Last.fm API Account Creation](https://www.last.fm/api/account/create)
2. Sign in with your Last.fm account (or create one)
3. Fill in the application name (e.g., "AI Chatbot")
4. Copy the **API Key** (not the shared secret)
5. Paste it into your `.env` file

---

## 📄 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

## 🙏 Credits

- **[Rasa Open Source](https://rasa.com/)** — NLU and dialogue management framework
- **[Last.fm API](https://www.last.fm/api)** — Live music data
- **[TMDB / Kaggle](https://www.kaggle.com/)** — Movie dataset
- **[DiceBear](https://dicebear.com/)** — Bot avatar generation
- **[Google Fonts](https://fonts.google.com/)** — Outfit typography

Built with ❤️ by [Bhargav](https://github.com/Bhargav2301)
