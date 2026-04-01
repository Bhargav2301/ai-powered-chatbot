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
- **Phase**: 6 (completed)
- **Task**: All tasks complete
- **Status**: Verified

## Last Session Summary
Phase 6 executed successfully. 2 plans, 4 tasks completed:
- Plan 6.1: 18 E2E test stories created, `rasa test` + NLU evaluation run
- Plan 6.2: README rewritten (191 lines), ROADMAP updated, .gitignore verified

## Next Steps
1. Final git cleanup — merge all milestone branches to main
