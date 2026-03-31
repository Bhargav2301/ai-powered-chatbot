---
phase: 5
plan: 2
wave: 2
---

# Plan 5.2: Async Connectivity & Glassmorphic Styling

## Objective
Provide the HTML structure mapped in 5.1 with a state-of-the-art interactive aesthetics package. Inject `fetch()` wrappers communicating with Rasa `localhost:5005` in real-time.

## Context
- `frontend/index.html`

## Tasks

<task type="auto">
  <name>Implement Glassmorphic CSS Aesthetic</name>
  <files>frontend/style.css</files>
  <action>
    - Inject premium styling arrays matching custom Dark Mode parameters.
    - Emulate modern web design gradients, blurred glassmorphism on UI elements, and sleek rounded elements to build the `.chat-container`.
    - Apply smooth transitional `@keyframes` ensuring incoming `.bot` chat messages spawn with a slight fade and slide-up animation.
  </action>
  <verify>Test-Path frontend/style.css</verify>
  <done>Frontend appearance matches a "wow-factor" visual benchmark.</done>
</task>

<task type="auto">
  <name>Implement Async UI Control Hooks</name>
  <files>frontend/script.js</files>
  <action>
    - Program logic listening to the "Enter" keyboard submission explicitly linking it alongside the "Send" DOM buttons.
    - Write a JS `async renderMessage(text, type)` method injecting elements synchronously alongside bot-side loading `.dots` indicators.
    - Write the main async `fetch()` function `POST`ing `{ sender: "user", message: input }` explicitly targeting `http://localhost:5005/webhooks/rest/webhook`. Map the looping array response text sequentially mimicking a genuine conversational flow over the `.messages` canvas.
  </action>
  <verify>Test-Path frontend/script.js</verify>
  <done>JavaScript contains standard fetching instructions binding Rasa strings physically into DOM elements.</done>
</task>

## Success Criteria
- [ ] CSS animations mapped gracefully scaling the layout effectively.
- [ ] Logic explicitly communicates utilizing standard unauthenticated Rest.
