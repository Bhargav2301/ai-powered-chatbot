---
phase: 6
plan: 2
wave: 2
---

# Plan 6.2: Documentation Polish & Git Cleanup

## Objective
Replace the stub README with a comprehensive project README covering setup, architecture, milestones, and usage. Merge all milestone branches into main for a clean final state. No new features — documentation and housekeeping only.

## Context
- README.md — current 3-line stub
- .gitignore — verify completeness
- .gsd/SPEC.md — success criteria and project description
- .gsd/ARCHITECTURE.md — system diagram
- domain.yml — intent/entity reference for feature listing

## Tasks

<task type="auto">
  <name>Write comprehensive README.md</name>
  <files>README.md</files>
  <action>
    - Overwrite README.md with a full project README containing these sections:
      1. **Title & Badge row** — project name with emoji, Python version badge, Rasa version badge
      2. **Overview** — 2-3 sentence project description (AI chatbot for music & movies built with Rasa)
      3. **Features** — bullet list of what the bot can do:
         - Movie recommendations by genre from SQLite database
         - Music recommendations via Last.fm API
         - Artist biography and playcount from Last.fm
         - Multi-turn context (3+ turns using slot memory)
         - Glassmorphic dark-mode web chat UI
         - Adult content filtering on Last.fm bios
      4. **Architecture** — text-based diagram from ARCHITECTURE.md (Frontend → Rasa → Action Server → SQLite/Last.fm)
      5. **Prerequisites** — Python 3.10, pip, virtualenv, Last.fm API key
      6. **Quick Start** — step-by-step: clone, create venv, install deps, get API key, prepare DB, train, run
      7. **Project Structure** — tree view of key files/directories
      8. **Milestone Tracker** — table of 6 milestones, all marked Complete:
         | # | Milestone | Status | Branch |
         |---|-----------|--------|--------|
         | 1 | Environment Setup | ✅ | milestone-1/environment-setup |
         | 2 | NLU Training Data | ✅ | milestone-2/nlu-training-data |
         | 3 | Domain & Dialogue | ✅ | milestone-3/domain-and-dialogue |
         | 4 | Custom Actions | ✅ | milestone-4/custom-actions |
         | 5 | Web Frontend | ✅ | milestone-5/web-frontend |
         | 6 | Testing & Polish | ✅ | milestone-6/testing-and-polish |
      9. **Known Limitations** — no cast/director via Last.fm, dataset age, no auth
      10. **License** — MIT (or user's choice)
    - Every section must be complete — NO placeholders, NO TODO markers
    - Use proper markdown: headers, code blocks, tables, emoji
  </action>
  <verify>Get-Content README.md | Measure-Object -Line | Select-Object -ExpandProperty Lines</verify>
  <done>README.md is 80+ lines with all 10 sections, zero placeholders</done>
</task>

<task type="auto">
  <name>Final git cleanup — merge branches to main</name>
  <files>N/A (git operations)</files>
  <action>
    - First, ensure all Phase 6 work is committed on the current branch:
      ```
      git add tests/ README.md .gsd/
      git commit -m "feat(phase-6): E2E tests, README, documentation polish"
      ```
    - Create the milestone-6 branch if not already on it:
      ```
      git checkout -b milestone-6/testing-and-polish
      ```
      (If already on it, skip this step)
    - Push the branch:
      ```
      git push origin milestone-6/testing-and-polish
      ```
    - Merge all milestone branches into main sequentially:
      ```
      git checkout main
      git merge milestone-1/environment-setup --no-edit
      git merge milestone-2/nlu-training-data --no-edit
      git merge milestone-3/domain-and-dialogue --no-edit
      git merge milestone-4/custom-actions --no-edit
      git merge milestone-5/web-frontend --no-edit
      git merge milestone-6/testing-and-polish --no-edit
      git push origin main
      ```
    - If merge conflicts arise on any branch, resolve them by accepting the LATER branch's changes (they represent the latest state).
    - Update .gsd/ROADMAP.md: set Phase 6 status to ✅ Complete.
    - Final commit and push:
      ```
      git add .gsd/ROADMAP.md
      git commit -m "docs: mark all milestones complete, final merge to main"
      git push origin main
      ```
  </action>
  <verify>git log --oneline -10 on main shows all milestone merges</verify>
  <done>All 6 milestone branches merged into main. ROADMAP shows all phases ✅ Complete. main is pushed to origin.</done>
</task>

## Success Criteria
- [ ] README.md is comprehensive (80+ lines) with setup guide, architecture, milestone tracker, and features
- [ ] All 6 milestone branches are merged into main
- [ ] main is pushed to origin with clean history
- [ ] ROADMAP.md shows all 6 phases as ✅ Complete
