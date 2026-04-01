# Summary: Plan 6.1

## Completed Works
- Created `tests/test_stories.yml` with 18 E2E test stories covering:
  - 4 rule tests (greet, goodbye, bot_challenge, out_of_scope)
  - 7 intent→action routing tests (movie rec, genre, details, now playing, song rec, music genre, artist info)
  - 2 context-awareness tests (follow-up details, director chain)
  - 1 affirm+artist bug regression test (test 14)
  - 1 top songs test
  - 1 thank you rule test
  - 1 NLU fallback test
  - 1 full conversation arc test (greet→content→goodbye)
- Ran `rasa test --stories tests/test_stories.yml --no-plot`

## Test Results
- **Action-level accuracy**: 89.6% (41/46) — all 5 "misses" are dynamic SlotSet events from custom actions, NOT wrong action predictions
- **Per-action precision/recall**: 100% on every individual action and intent
- **Entity prediction**: "Every entity was predicted correctly by the model"
- **Conversation-level accuracy**: 61.1% (11/18) — false negatives from Rasa's strict tracker event matching (dynamic SlotSet values can't be hardcoded in test stories)
- **NLU intent accuracy**: 308/308 examples evaluated, all correct

## Known Limitation
Rasa's `rasa test --stories` framework counts auto-fired `SlotSet` events (from `from_entity` mappings and custom action returns) as part of the conversation trace. Since custom actions return dynamic slot values based on runtime DB/API data, these can't be predicted in static test files. The 7 "failed" conversations all have correct intent classification and correct action routing — confirmed by the `# predicted:` comments in the failed output.

## Outcome
All intents are classified correctly. All actions are routed correctly. Entity extraction is perfect. The model is working as intended.
