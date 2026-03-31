---
phase: 4
plan: 1
wave: 1
---

# Plan 4.1: SQLite Movies Implementation

## Objective
Implement programmatic Python connection logic to the local `movies.db` SQLite database to facilitate extremely low latency responses for the 5 Movie action paths without relying on complex, external HTTP routing or rate-limited API keys.

## Context
- .gsd/DECISIONS.md (Rules 007 and 010 regarding Actions Structure)
- data/movies.db

## Tasks

<task type="auto">
  <name>Scaffold SQLite Boilerplate & Primary Movie Actions</name>
  <files>actions/actions.py</files>
  <action>
    - Import `sqlite3` and connect lazily to `data/movies.db` across class calls.
    - Implement `action_recommend_movie`. When users ask for a generic movie (or a specific genre), query the SQLite database mapping `SELECT * FROM movies WHERE genres LIKE '%<genre>%' ORDER BY popularity DESC LIMIT 3;`. Yield the result.
    - Set `current_movie` Slot using `SlotSet("current_movie", result)` so that subsequent turn flows possess the necessary context.
    - Implement `action_get_movie_details` to return the `overview` or `tagline` of the exact requested or implicit movie.
    - Include rigorous Try/Except blocks as instructed by DECISION-010.
  </action>
  <verify>python -m py_compile actions/actions.py</verify>
  <done>actions.py possesses syntactically accurate class instantiations for Base Movie Actions</done>
</task>

<task type="auto">
  <name>Implement Contextual Follow-up SQLite actions</name>
  <files>actions/actions.py</files>
  <action>
    - We must address `action_get_movie_cast`, `action_get_movie_director`, and `action_get_now_playing`.
    - However, the TMDB 930k Kaggle dataset has `title`, `genres`, `overview`, `popularity` and `release_date`, but it often lacks `cast` and `director` granularity (which requires separate credits tables).
    - As identified in the prep stages: implement an honest fallback for Director / Cast directing the user to search IMDb instead of crashing.
    - `action_get_now_playing` should query `SELECT * FROM movies ORDER BY release_date DESC LIMIT 5`.
  </action>
  <verify>python -m py_compile actions/actions.py</verify>
  <done>All 5 Movie actions exist within actions.py without syntax errors.</done>
</task>
