# DECISIONS.md

> Architecture Decision Records (ADRs) to track key changes and technical decisions.

## Phase 1 & 2 Decisions

**Date:** 2026-03-30

### Scope & Structure
- **DECISION-001**: NO `rasa init`. All files created manually, one milestone at a time.
- **DECISION-002**: Separate files for NLU data: `movies_nlu.yml` + `music_nlu.yml`
- **DECISION-003**: File order for Milestone 2: `config.yml` → `domain.yml` → nlu files → `rasa train`

### Technical Stack & Configuration
- **DECISION-004**: Pipeline locked to `WhitespaceTokenizer` + `DIETClassifier` stack (no SpaCy/BERT).
- **DECISION-005**: Windows long path registry fix required before first `rasa train`.
- **DECISION-006**: Python version updated to 3.10.x (3.14 was incompatible).
- **DECISION-007**: Static JSON data files for Phase 1. No live APIs.
- **DECISION-008**: Session-only memory. No persistent DB across sessions.

### Pending
- **PENDING**: DECISION-009 (frontend) and DECISION-010 (channel) deferred to Milestone 5.
