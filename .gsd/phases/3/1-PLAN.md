---
phase: 3
plan: 1
wave: 1
---

# Plan 3.1: Version Control Sync & Universal NLU

## Objective
Safeguard Phase 2 by syncing with the remote repository on github, checkout the new Phase 3 branch, and integrate the user-provided Universal NLU structure that dictates domain-agnostic intents.

## Context
- .gsd/ROADMAP.md
- .gsd/phases/3/templates/general_nlu.yml

## Tasks

<task type="auto">
  <name>Sync to GitHub</name>
  <action>
    Run `git push origin main` (or the equivalent default remote branch) to push all current local Phase 2 commits.
    This fulfills the user's request to "update the github repository".
  </action>
  <verify>git log origin/main..HEAD</verify>
  <done>Zero unpushed commits showing, remote is synchronized</done>
</task>

<task type="auto">
  <name>Checkout Phase 3 Branch</name>
  <action>
    Run `git checkout -b milestone-3/domain-and-dialogue` to cleanly isolate Phase 3 development.
  </action>
  <verify>git branch --show-current</verify>
  <done>Branch is switched to milestone-3/domain-and-dialogue</done>
</task>

<task type="auto">
  <name>Copy Universal NLU Template</name>
  <files>data/nlu/general_nlu.yml</files>
  <action>
    Read the contents from `.gsd/phases/3/templates/general_nlu.yml` and copy them strictly to `data/nlu/general_nlu.yml`.
  </action>
  <verify>Get-Content data/nlu/general_nlu.yml</verify>
  <done>File exists containing universal intents</done>
</task>
