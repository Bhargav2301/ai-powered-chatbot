# STATE.md

> **Purpose**: Maintain session context, memory, and important knowledge base items.

## Knowledge Base Entries (Crucial Agent Memory)
- **Environment**: This project uses Python 3.10.x in a venv at `.\venv`
- **Execution**: Rasa action server runs on port 5055, Rasa server on port 5005. Both must be started in separate terminals.
- **Coding Standards**: All YAML indentation must use 2 spaces, never tabs.
- **Educational Context**: The developer is a beginner — **always** explain concepts clearly via comments and text before implementing.
- **Agent Instructions**: 
  - Every Python function must include a docstring.
  - Every YAML file must include inline comments.
  - Use Windows-compatible commands only. Activate venv before any pip/rasa commands (`.\venv\Scripts\activate`).
  - Create files/milestones one at a time.
  - Generate at least 10 examples per intent when generating training data.

## Current Position
- **Phase**: 6
- **Task**: Planning complete
- **Status**: Ready for execution

## Last Session Summary
Phase 6 (Testing & Polish) plans rewritten to match user-locked scope:
- Plan 6.1: E2E test stories (5+ test conversations) + `rasa test` + `rasa test nlu --cross-validation`
- Plan 6.2: Comprehensive README.md (80+ lines) + merge all 6 milestone branches to main

## Next Steps
1. /execute 6
