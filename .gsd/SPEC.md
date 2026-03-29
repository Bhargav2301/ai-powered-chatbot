# SPEC.md — AI-Powered Music & Movies Chatbot using Rasa

> **Status**: `FINALIZED`
> **Version:** 1.0
> **Author:** Bhargav (dvs-bhargav)

## Vision
A context-aware, AI-powered conversational chatbot built with Rasa Open Source that can hold intelligent, multi-turn conversations about music and movies. It will recognize user intents, remember context across turns, respond using custom actions backed by data, and feature a web chat widget.

## Goals
1. Build a fully functional, locally-running chatbot that understands and responds to music and movie queries.
2. Maintain context over at least 3-turn conversations.
3. Understand what NLU, intents, entities, and slots are, along with story/rule building.
4. Interface cleanly via an HTML/CSS/JS frontend.

## Non-Goals (Out of Scope)
- Deployment to cloud servers (Phase 1 logic runs locally).
- Live API integrations (Phase 1 uses static JSON files for data).
- Complex auth or user accounts.
- Unrelated domains outside of movies and music. 

## Users
- Primary User: End-users seeking music or movie recommendations.
- Developer context: The developer (Bhargav) is a beginner learning NLP and Rasa concepts.

## Constraints
- **Language**: Python 3.9 in a `venv`.
- **NLU Framework**: Rasa Open Source 3.x.
- **Environment**: Windows 10/11.
- **Tools**: VS Code/Antigravity with specific extensions.

## Success Criteria
- [ ] Chatbot correctly classifies ≥ 10 distinct intents.
- [ ] Chatbot maintains context over at least 3-turn conversations.
- [ ] Custom actions fetch/return structured data (from JSON).
- [ ] Web UI renders and sends messages correctly.
- [ ] All milestones committed to GitHub with descriptive branch names and messages.
