---
phase: 2
plan: 2
wave: 2
---

# Plan 2.2: Scaffolding Domain and Movies NLU

## Objective
Establish the intent and entity definitions within the domain file and populate the Movies NLU training data file.

## Context
- .gsd/DECISIONS.md (001, 002, 003)
- .gsd/SPEC.md

## Tasks

<task type="auto">
  <name>Create domain.yml (Base structure)</name>
  <files>domain.yml</files>
  <action>
    Create domain.yml with `intents` (including movie intents, music intents, bot meta intents) and `entities` (genre, movie_title, artist_name, song_title).
    Include inline comments explaining the intent and entity structure.
  </action>
  <verify>Get-Content domain.yml</verify>
  <done>domain.yml exists with all intents and entities listed</done>
</task>

<task type="auto">
  <name>Create movies_nlu.yml</name>
  <files>data/nlu/movies_nlu.yml</files>
  <action>
    Create training data for the 6 movie intents defined in the PRD (ask_movie_recommendation, ask_movie_by_genre, ask_movie_details, ask_movie_director, ask_movie_cast, ask_now_playing).
    Provide EXACTLY or MORE than 10 examples per intent! This is a strict constraint.
    Annotate entities properly: [inception](movie_title), [thriller](genre).
  </action>
  <verify>Get-Content data/nlu/movies_nlu.yml</verify>
  <done>movies_nlu.yml contains ≥60 examples total, correctly annotated</done>
</task>
