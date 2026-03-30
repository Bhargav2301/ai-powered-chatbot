---
phase: 3
plan: 2
wave: 2
---

# Plan 3.2: Universal Rules Setup

## Objective
Define the strictly-deterministic Rule interactions for universal intents.

## Context
- .gsd/phases/3/RESEARCH.md
- domain.yml

## Tasks

<task type="auto">
  <name>Create rules.yml</name>
  <files>data/rules.yml</files>
  <action>
    Create a new file `data/rules.yml` under the `rules:` mapping.
    Define universal single-turn rules for the bot:
    - greet -> utter_greet
    - goodbye -> utter_goodbye
    - bot_challenge -> utter_bot_challenge
    - out_of_scope -> utter_out_of_scope
    - thank_you -> utter_thank_you
    
    ALSO define the default Fallback rule:
    When `nlu_fallback` intent occurs, trigger `action_default_fallback`.
  </action>
  <verify>Get-Content data/rules.yml</verify>
  <done>Rules exist managing deterministic responses and falbacks.</done>
</task>
