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
    Create data/nlu/ directories if needed.
    Read the exact content of `.gsd/phases/2/templates/music_nlu.yml` and copy it exactly into `data/nlu/music_nlu.yml`.
    Do NOT modify any contents or remove any comments.
  </action>
  <verify>Get-Content data/nlu/music_nlu.yml</verify>
  <done>music_nlu.yml exists and has robust examples</done>
</task>
