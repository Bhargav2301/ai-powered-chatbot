---
phase: 2
plan: 4
wave: 3
---

# Plan 2.4: Rasa Train and Evaluate

## Objective
Train the resulting NLU model and verify accuracy.

## Context
- .gsd/DECISIONS.md (003)
- .gsd/REQUIREMENTS.md (REQ-07)

## Tasks

<task type="auto">
  <name>Train the NLU Model</name>
  <action>
    Run `rasa train nlu` in the activated virtual environment. 
    Ensure no YAML syntax errors exist before running.
  </action>
  <verify>Get-ChildItem models/</verify>
  <done>A compressed tar.gz model appears in the models folder</done>
</task>

<task type="auto">
  <name>Test the NLU Model</name>
  <action>
    Run `rasa test nlu --nlu data/nlu/ --cross-validation`
    Parse the confusion matrix and intent report to ensure >90% precision.
  </action>
  <verify>Get-Content results/intent_report.json</verify>
  <done>intent_report.json shows passing accuracy criteria</done>
</task>
