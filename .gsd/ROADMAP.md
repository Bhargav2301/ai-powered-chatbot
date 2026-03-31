# ROADMAP.md

> **Current Phase**: Not started
> **Milestone**: v1.0

## Must-Haves (from SPEC)
- [ ] 10+ Intent Classification
- [ ] Contextual multi-turn conversation (3+ turns)
- [ ] Custom actions with backend JSON data
- [ ] Web-based UI widget
- [ ] Testing and validation setup

## Phases

### Phase 1: Environment Setup
**Status**: ⬜ Not Started
**Mielstone Branch**: `milestone-1/environment-setup`
**Objective**: Install Rasa, set up the venv, initialize git repo, and extensions.
**Requirements**: Foundational setup.

### Phase 2: NLU Training Data
**Status**: ✅ Complete
**Mielstone Branch**: `milestone-2/nlu-training-data`
**Objective**: Define intents, entities, and train the initial NLU model.
**Requirements**: REQ-01, REQ-02

### Phase 3: Domain & Dialogue
**Status**: ✅ Complete
**Mielstone Branch**: `milestone-3/domain-and-dialogue`
**Objective**: Define slots, stories, and rules for managing dialog flow.
**Requirements**: REQ-04, REQ-06

### Phase 4: Custom Actions
**Status**: ✅ Complete
**Mielstone Branch**: `milestone-4/custom-actions`
**Objective**: Develop Python custom actions backed by Live APIs and SQLite Data.
**Requirements**: REQ-03

### Phase 5: Web Frontend
**Status**: ⬜ Not Started
**Mielstone Branch**: `milestone-5/web-frontend`
**Objective**: Build and style the web chat interface, connected to Rasa API.
**Requirements**: REQ-05

### Phase 6: Testing & Polish
**Status**: ⬜ Not Started
**Mielstone Branch**: `milestone-6/testing-and-polish`
**Objective**: Polish documentation, create e2e tests.
**Requirements**: REQ-07, REQ-08
