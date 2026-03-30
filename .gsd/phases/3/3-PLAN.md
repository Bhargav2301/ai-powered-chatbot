---
phase: 3
plan: 3
wave: 3
---

# Plan 3.3: Interactive Scenarios & Multi-turn Conversational Stories

## Objective
Establish contextual dialogue graphs explicitly mapping the 3-turn and 4-turn behaviors using the slot properties defined in domain.yml.

## Context
- .gsd/ROADMAP.md (REQ-04, REQ-06)
- domain.yml

## Tasks

<task type="auto">
  <name>Create stories.yml</name>
  <files>data/stories.yml</files>
  <action>
    Create a new file `data/stories.yml`. Include the `stories` block.
    
    Implement at least 2 key flows for movies and 2 flows for music.
    
    FLOW 1: Movie context
    `intent: ask_movie_recommendation` -> `action: action_recommend_movie`
    Follow up: `intent: ask_movie_details` -> `action: action_get_movie_details`
    Follow up: `intent: ask_movie_director` -> `action: action_get_movie_director`
    
    FLOW 2: Movies logic ambiguity
    `intent: ask_movie_by_genre` -> `action: utter_ask_for_genre_movies` 
    Wait, `ask_movie_by_genre` already implies a genre constraint. Let's do:
    `intent: ask_movie_recommendation` (vague) -> `action: utter_ask_for_genre_movies` -> user enters `intent: ask_movie_by_genre` (supplying genre entity) -> `action: action_recommend_movie`.
    
    FLOW 3: Music logic
    `intent: ask_song_recommendation` -> `action: utter_ask_for_genre_music` -> user enters `intent: ask_song_by_genre` -> `action: action_recommend_song`.
    Follow up -> `intent: ask_artist_info` -> `action: action_get_artist_info`.

    Keep paths linear and simple, using the exact action names matched to the actions array in `domain.yml`.
  </action>
  <verify>Get-Content data/stories.yml</verify>
  <done>Multi-turn conversational paths correctly utilizing explicit custom action names exists</done>
</task>
