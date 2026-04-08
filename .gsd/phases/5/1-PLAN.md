---
phase: 5
plan: 1
wave: 1
---

# Plan 5.1: Restore and Verify Web Frontend

## Objective
Restore the web chat interface files (index.html, script.js, style.css) to the `main` branch and verify their functionality with the Rasa API. This ensures the frontend is fully integrated and documented.

## Context
- .gsd/SPEC.md
- .gsd/ROADMAP.md
- frontend/index.html
- frontend/script.js
- frontend/style.css

## Tasks

<task type="auto">
  <name>Restore Frontend Files</name>
  <files>frontend/index.html, frontend/script.js, frontend/style.css</files>
  <action>
    Merge the missing frontend files from `origin/milestone-5/web-frontend` into the current `main` branch. 
    - Ensure `frontend/script.js` uses `sender: "user_bhargav"` as per DECISION-008.
    - Resolve any conflicts in ROADMAP.md or STATE.md that may arise from the merge.
  </action>
  <verify>git ls-tree -r HEAD frontend</verify>
  <done>All three frontend files are present in the HEAD of the main branch.</done>
</task>

<task type="auto">
  <name>Verify Frontend Connectivity</name>
  <files>frontend/script.js</files>
  <action>
    Validate that the frontend correctly communicates with the Rasa server (port 5005).
    - Check that the REST API call in `frontend/script.js` points to `http://localhost:5005/webhooks/rest/webhook`.
    - Verify that the action server is reachable.
  </action>
  <verify>Test-Path "frontend/index.html"</verify>
  <done>Frontend files are verified and connected to the backend API.</done>
</task>

## Success Criteria
- [ ] index.html, style.css, and script.js are present in the `main` branch.
- [ ] `frontend/script.js` uses `user_bhargav` as the sender ID.
- [ ] Phase 5 is marked as ✅ Complete in ROADMAP.md.
