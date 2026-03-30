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
    Read the exact content of `.gsd/phases/2/templates/domain.yml` and copy it exactly into `domain.yml` in the root directory.
    Do NOT modify any contents or remove any comments.
  </action>
  <verify>Get-Content domain.yml</verify>
  <done>domain.yml exists with all intents and entities listed</done>
</task>

<task type="auto">
  <name>Create movies_nlu.yml</name>
  <files>data/nlu/movies_nlu.yml</files>
  <action>
    Create data/nlu/ directories if needed.
    Read the exact content of `.gsd/phases/2/templates/movies_nlu.yml` and copy it exactly into `data/nlu/movies_nlu.yml`.
    Do NOT modify any contents or remove any comments.
  </action>
  <verify>Get-Content data/nlu/movies_nlu.yml</verify>
  <done>movies_nlu.yml contains ≥60 examples total, correctly annotated</done>
</task>
