---
phase: 3
plan: 4
wave: 4
---

# Plan 3.4: Re-Train and Evaluate Pipeline Core

## Objective
Train the entire pipeline (NLU + Core Policies) verifying that there are no contradictions between the RulePolicy and TEDPolicy logic graphs.

## Context
- .gsd/phases/3/1-PLAN.md
- ./venv/Scripts/activate

## Tasks

<task type="auto">
  <name>Train Rasa Core and NLU Model</name>
  <action>
    Run `rasa train` without constraints so it inherently rebuilds both the NLU parsing array and the dialogue Policy engine with the new `general_nlu.yml`, `rules.yml` and `stories.yml` files.
    Ensure to activate the python virtual environment.
  </action>
  <verify>Get-ChildItem models/</verify>
  <done>A new model archive incorporates the successfully compiled dialogue graphs.</done>
</task>

<task type="auto">
  <name>Test Policy Graph</name>
  <action>
    Run `rasa test core` (or `rasa test`) to test the dialogue paths internally. 
  </action>
  <verify>Test-Path results/story_report.json</verify>
  <done>story_report json metrics exist showing successfully traversed story tests</done>
</task>
