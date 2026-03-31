---
phase: 6
plan: 1
wave: 1
---

# Plan 6.1: End-to-End Validation & Browser Test

## Objective
Empirically verify that the full stack works end-to-end: Rasa action server, Rasa REST API with CORS, and the frontend UI. Catch any remaining integration bugs before polishing documentation.

## Context
- frontend/index.html
- frontend/script.js
- actions/actions.py
- credentials.yml

## Tasks

<task type="auto">
  <name>Validate Rasa REST endpoint accepts POST</name>
  <files>credentials.yml, endpoints.yml</files>
  <action>
    - Start Rasa with REST + CORS enabled: `rasa run --enable-api --cors "*"`
    - Start action server: `rasa run actions`
    - Send a test POST via PowerShell to confirm the REST webhook responds:
      `Invoke-RestMethod -Uri "http://localhost:5005/webhooks/rest/webhook" -Method POST -Body '{"sender":"test","message":"hello"}' -ContentType "application/json"`
    - Confirm the response JSON contains a bot reply with text
  </action>
  <verify>Invoke-RestMethod returns valid JSON with a "text" field</verify>
  <done>REST endpoint returns bot response to "hello" message</done>
</task>

<task type="checkpoint:human-verify">
  <name>Browser integration test</name>
  <files>frontend/index.html</files>
  <action>
    - Open frontend/index.html in the browser
    - Type "hello" and verify bot responds
    - Type "recommend a zombie movie" and verify genre-filtered results
    - Type "recommend jazz music" and verify Last.fm results (requires valid API key in .env)
    - Capture screenshots showing all three interactions
  </action>
  <verify>Visual confirmation of working chat UI with correct responses</verify>
  <done>All three test messages produce correct, genre-filtered responses in the browser UI</done>
</task>

## Success Criteria
- [ ] REST endpoint responds to POST requests with bot messages
- [ ] Frontend correctly renders user/bot messages in the browser
- [ ] Genre filtering works ("zombie" → horror movies)
