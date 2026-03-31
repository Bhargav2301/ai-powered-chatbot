---
phase: 5
plan: 1
wave: 1
---

# Plan 5.1: REST Pipeline & Frontend Structure

## Objective
Enable the Rasa REST webhook endpoint to accept asynchronous HTTP traffic and scaffold the HTML5 visual interface implementing the core Glassmorphic aesthetic mapped per REC-05.

## Context
- .gsd/SPEC.md
- .gsd/DECISIONS.md (REST + Vanilla UI)

## Tasks

<task type="auto">
  <name>Enable Async Rasa REST Endpoint</name>
  <files>credentials.yml</files>
  <action>
    - Create `credentials.yml` in the project root if it does not already exist.
    - Expose the exact YAML property `rest:` cleanly to ensure Rasa can intercept frontend POST commands.
  </action>
  <verify>Get-Content credentials.yml</verify>
  <done>File structurally contains the "rest:" array activation.</done>
</task>

<task type="auto">
  <name>Scaffold Main Interface HTML</name>
  <files>frontend/index.html</files>
  <action>
    - Create the `frontend` directory in the root and initialize `index.html`.
    - Inject modern HTML5 semantics defining an encapsulating `.chat-container`, a main `.messages` visualization window, and an `.input-area` referencing a text `input` component mapped via explicit `id` selectors for JS interaction.
    - Import the Google Font "Outfit" identically referencing a premium 400/600 web typography aesthetic.
  </action>
  <verify>Test-Path frontend/index.html</verify>
  <done>index.html exists containing chat bubbles, text inputs, and font linkings.</done>
</task>

## Success Criteria
- [ ] Rasa webhook listener logically configured.
- [ ] HTML scaffolding visually maps inputs/outputs natively.
