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
- **Phase**: 4 (completed)
- **Task**: Execution verified
- **Status**: Ready for Phase 5

## Last Session Summary
Phase 4 (Database & Actions) has been fully executed! All 9 custom Action python scripts utilizing SQLite bindings and HTTP queries are operational inside `actions.py`. Endpoints have been connected.

## Next Steps
1. Run `rasa run actions` and `rasa shell` manually to verify the endpoints.
2. Advance toward Phase 5 (Web Frontend) using `/plan 5`.
