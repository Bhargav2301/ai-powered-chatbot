---
phase: 6
plan: 2
wave: 2
---

# Plan 6.2: README & Documentation Polish

## Objective
Replace the stub README with a comprehensive, professional project README that covers setup, architecture, usage, and screenshots. Update .gitignore and clean up any stray files.

## Context
- README.md
- .gitignore
- .gsd/DECISIONS.md
- .gsd/SPEC.md

## Tasks

<task type="auto">
  <name>Write comprehensive README.md</name>
  <files>README.md</files>
  <action>
    - Overwrite the existing 3-line README with a full project README containing:
      - Project title with emoji + description
      - Features list (movies via SQLite, music via Last.fm, multi-turn context, web UI)
      - Architecture diagram (text-based: Frontend → Rasa REST → Action Server → SQLite/Last.fm)
      - Prerequisites (Python 3.10, venv, Rasa 3.6.20)
      - Quick Start guide (clone, venv, install, prepare DB, train, run)
      - Project structure tree
      - How to get a Last.fm API key
      - Known limitations (dataset cutoff date, no cast/director data)
      - License and credits
    - Use proper markdown with badges, code blocks, and tables
    - Avoid placeholder text — every section should be complete
  </action>
  <verify>Get-Content README.md | Measure-Object -Line returns > 50 lines</verify>
  <done>README has all sections listed above with no placeholders</done>
</task>

<task type="auto">
  <name>Audit and update .gitignore</name>
  <files>.gitignore</files>
  <action>
    - Verify .gitignore contains entries for:
      - `venv/`
      - `.env`
      - `data/movies.db`
      - `data/TMDB_all_movies.csv`
      - `models/`
      - `__pycache__/`
      - `.rasa/`
    - Add any missing entries
    - Do NOT add `frontend/` or any committed source files
  </action>
  <verify>Get-Content .gitignore</verify>
  <done>.gitignore covers all generated/secret files, no source files excluded</done>
</task>

## Success Criteria
- [ ] README.md is comprehensive (50+ lines) with setup guide, architecture, and features
- [ ] .gitignore covers all generated artifacts and secrets
