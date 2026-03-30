---
phase: 3
level: 2
researched_at: 2026-03-30
---

# Phase 3 Research: Domain & Dialogue Management

## Questions Investigated
1. How does Rasa 3.x handle slots for maintaining context (`current_movie`, `current_artist`) across multiple turns?
2. What is the best practice for separating Rules vs Stories in Rasa 3.x?
3. How does session-only memory (DECISION-008) affect the Tracker store?

## Findings

### Managing Context via Slots
Rasa 3.x uses explicit slot mappings in `domain.yml`. 
- **Type**: Slots like `current_movie` should use `type: text` with `influence_conversation: true` so the presence of a movie title alters dialogue policies (e.g. allowing the bot to answer "Who directed it?" vs "Please specify a movie").
- **Mappings**: We must define mappings such as `type: from_entity` mapping the `movie_title` entity directly to the `current_movie` slot. 

### Rules vs Stories Implementation
Rasa enforces a strict mental model for dialogue:
- **Rules**: Should be strictly used for short, deterministic interactions. In our bot: `greet`, `goodbye`, `bot_challenge`, and `out_of_scope`.
- **Stories**: Should handle the non-deterministic, multi-turn branches like:
  - User asks for recommendation → Bot recommends → User asks details about one.
  - User asks details → Bot gives details → User asks for cast/director.

### Tracker Store configurations
DECISION-008 specifies session-only memory. By default, Rasa core initializes an `InMemoryTrackerStore`. We do not need to configure Postgres or Redis in `endpoints.yml`, as the default fully satisfies this constraint. Context will reset whenever the Rasa server is restarted.

## Decisions Made
| Decision | Choice | Rationale |
|----------|--------|-----------|
| Slot Configuration | Explicit mappings with `influence_conversation: true` | Required for REQ-04 minimum 3-turn context awareness. |
| Rule limitations | Limit rules to single-turn meta intents | Keeps the TEDPolicy and RulePolicy strictly separated, preventing policy conflicts. |
| Tracker Store | Use Default `InMemoryTrackerStore` | Satisfies DECISION-008 perfectly with 0 overhead. |

## Patterns to Follow
- Follow the `utter_{intent}` naming convention strictly for Bot responses in `domain.yml` to keep actions predictable.
- Include explicit `action_listen` at the end of stories to give clarity to the dialogue graph.

## Anti-Patterns to Avoid
- **Story Contradiction**: Writing a Rule that contradicts a path deep inside a Story. Rules override Stories, which can prematurely terminate a multi-turn path.
- **Overusing Forms**: For this phase, standard slot extraction via entities is sufficient; do not build complex `forms` unless explicitly needed by the Custom Actions in Phase 4.

## Ready for Planning
- [x] Questions answered
- [x] Approach selected
- [x] Dependencies identified
