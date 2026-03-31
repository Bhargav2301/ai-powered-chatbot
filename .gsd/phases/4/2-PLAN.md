---
phase: 4
plan: 2
wave: 2
---

# Plan 4.2: Live Last.fm API Implementations

## Objective
Embed live real-time API queries utilizing Python's `requests` library to fetch active trends from the Last.fm backend.

## Context
- .gsd/DECISIONS.md (Live Integration Specifications)
- .env

## Tasks

<task type="auto">
  <name>Establish Python DotEnv & Base LastFM Search</name>
  <files>actions/actions.py</files>
  <action>
    - Inside `actions.py`, install the `load_dotenv` logic to fetch `LASTFM_API_KEY`.
    - Implement `action_recommend_song` (queries `tag.gettoptracks` using the supplied `.env` key). Yield the top 3 tracks formatted gracefully. Remember to update the `current_artist` slot so stories can be continued!
    - Implement an absolute 5-second `timeout=5` rule inside the `requests.get()` invocation to adhere to DECISION-010.
  </action>
  <verify>python -m py_compile actions/actions.py</verify>
  <done>LastFM API keys are actively accessed via os.getenv inside actions.py securely.</done>
</task>

<task type="auto">
  <name>Complete Remaining Live Music Endpoints</name>
  <files>actions/actions.py</files>
  <action>
    - Implement `action_get_song_details` and `action_get_artist_info` pointing to `artist.getInfo` via LastFM. 
    - Implement `action_get_top_songs` pointing to `chart.getTopTracks`.
    - Ensure all API invocations wrap neatly inside `Try/Except` with `Timeout` fallbacks uttering failure gracefully.
  </action>
  <verify>python -m py_compile actions/actions.py</verify>
  <done>All 9 total required Rasa Action implementations are instantiated properly in actions.py.</done>
</task>
