---
phase: 2
plan: 3
wave: 2
---

# Plan 2.3: Scaffolding Music NLU and Base Rules

## Objective
Populate the Music NLU training data file and global rules.

## Context
- .gsd/DECISIONS.md (002)
- .gsd/SPEC.md

## Tasks

<task type="auto">
  <name>Create music_nlu.yml</name>
  <files>data/nlu/music_nlu.yml</files>
  <action>
    Create training data for the 5 music intents and 4 meta intents defined in the PRD.
    Provide EXACTLY or MORE than 10 examples per intent!
    Annotate entities properly: [taylor swift](artist_name), [pop](genre).
  </action>
  <verify>Get-Content data/nlu/music_nlu.yml</verify>
  <done>music_nlu.yml exists and has robust examples</done>
</task>
