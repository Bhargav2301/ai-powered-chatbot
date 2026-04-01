---
phase: 6
plan: 1
wave: 1
---

# Plan 6.1: E2E Test Stories & NLU Validation

## Objective
Create end-to-end test stories that validate the chatbot's core conversation flows, run Rasa's built-in test framework, and fix any failures. This proves the bot works correctly beyond manual testing.

## Context
- domain.yml — intent/action/slot definitions
- data/stories/movies_stories.yml — movie conversation flows
- data/stories/music_stories.yml — music conversation flows
- data/rules/rules.yml — deterministic rule paths
- .gsd/SPEC.md — success criteria requiring test validation

## Tasks

<task type="auto">
  <name>Create test_stories.yml with 5+ E2E test conversations</name>
  <files>tests/test_stories.yml</files>
  <action>
    - Create the `tests/` directory in the project root.
    - Create `tests/test_stories.yml` with at least 5 complete end-to-end test stories.
    - Each test story should use `## ` prefix to mark them as test conversation blocks.
    - Required test conversations:
      1. **Greet flow** — user greets, bot responds with utter_greet
      2. **Movie genre recommendation** — user asks for a thriller movie, bot calls action_recommend_movie
      3. **Movie context follow-up** — user asks for genre recommendation → then asks "tell me about the first one" (ask_movie_details) → bot calls action_get_movie_details
      4. **Music genre recommendation** — user asks for pop songs → bot calls action_recommend_song
      5. **Music affirm + artist info** — user asks for song recommendation → affirms → bot calls action_get_artist_info
      6. **Out of scope handling** — user asks unrelated question → bot responds with utter_out_of_scope
    - Use exact intent names and action names from domain.yml.
    - Include inline YAML comments explaining what each test validates.
    - Do NOT include entity annotations in test steps — only intent and action.
  </action>
  <verify>Get-Content tests/test_stories.yml | Measure-Object -Line | Select-Object -ExpandProperty Lines</verify>
  <done>test_stories.yml exists with at least 5 complete test stories totalling 30+ lines</done>
</task>

<task type="auto">
  <name>Run Rasa test suite and fix failures</name>
  <files>tests/test_stories.yml</files>
  <action>
    - Activate the venv: `.\venv\Scripts\activate`
    - Run: `rasa test --stories tests/test_stories.yml --no-plot`
    - This validates story paths against the trained model.
    - If any test story fails:
      - Read the error output to identify which story failed and why
      - Fix the test story to match actual model behaviour (NOT the other way around — do not retrain to fix tests)
      - Re-run until all tests pass
    - Run: `rasa test nlu --nlu data --cross-validation --folds 3 --no-plot`
    - This validates NLU intent classification accuracy.
    - Capture the overall intent accuracy percentage from output.
    - If intent accuracy < 80%, note it as a known limitation but do NOT retrain in Phase 6.
  </action>
  <verify>rasa test --stories tests/test_stories.yml --no-plot exits with code 0</verify>
  <done>All test stories pass. NLU cross-validation results captured.</done>
</task>

## Success Criteria
- [ ] `tests/test_stories.yml` contains 5+ test stories covering greet, movies, music, context, and out-of-scope
- [ ] `rasa test --stories` passes with 0 failures
- [ ] `rasa test nlu --cross-validation` runs and prints accuracy metrics
