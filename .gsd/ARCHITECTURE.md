# ARCHITECTURE.md

## System Diagram
```text
┌─────────────────────────────────────────────────────────┐
│                     USER INTERFACE                       │
│              HTML/CSS/JS Chat Widget                     │
│                  (index.html)                            │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP POST /webhooks/rest/webhook
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   RASA SERVER (Port 5005)                │
│  ┌─────────────────┐      ┌──────────────────────────┐  │
│  │   NLU Pipeline  │      │   Dialogue Management    │  │
│  │  ─────────────  │      │  ────────────────────    │  │
│  │  Tokenizer      │ ───► │  Stories / Rules         │  │
│  │  Featurizer     │      │  Slots & Forms           │  │
│  │  Intent Clf.    │      │  Entity Extrac.          │  │
│  └─────────────────┘      └──────────────────────────┘  │
│                                      │                   │
│                                      ▼                   │
│                        HTTP POST to Action Server        │
└─────────────────────────────────────┬───────────────────┘
                                      │
                     ┌────────────────┴──────────────┐
                     │   RASA ACTION SERVER (Port 5055)│
                     │   actions/actions.py            │
                     └────────────────────────────────┘
                                      │
                     ┌────────────────┴──────────────┐
                     │         DATA LAYER             │
                     │   data/movies.json             │
                     │   data/music.json              │
                     └────────────────────────────────┘
```

## Tech Stack
- NLU Framework: Rasa Open Source 3.x
- Language: Python 3.9 (run exclusively in `venv`)
- Frontend: Vanilla HTML/CSS/JS 
- Static Data: JSON
