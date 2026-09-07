# Repository migration record

| Item | Identity |
|---|---|
| Repository | `Bhargav2301/ai-powered-chatbot` |
| Original main HEAD | `cf66c820319cc07679c075fa189f5e8de837dd19` |
| Original tree | `f92bea4dc342357fd32cc073533c3b506a5fd1c4` |
| Preserved branch | `deprecated/chatbot-2026-09-07` |
| Deprecation commit | `c91dcd1e112db147717711ebae769c502294e642` |
| Archive change | Adds only `DEPRECATED.md`; all 248 original tracked files are unchanged |
| Replacement | Polymath Android under `android/`, private inference under `services/rag/`, examples under `datasets/` |

The archive branch was created from the exact old HEAD and pushed before restructuring. A Git branch already preserves the entire repository tree and ancestry; nesting another copy of the repository inside itself is unnecessary. The deprecation marker explains the new main project while retaining the original README unchanged for historical accuracy.

Main changes are ordinary descendants of the original HEAD. No force push, history rewrite or archive-file replacement is part of the migration. Repository-level methodology rules remain at the root; `.gsd/` records the finalized migration specification and current state.

## Inspect or run the old version

```bash
git fetch origin
git worktree add ../chatbot-legacy origin/deprecated/chatbot-2026-09-07
```

Read the old README inside that isolated checkout for its Rasa setup. Restoring the old application to main should be a new reviewed revert/restoration commit, preserving both histories. Do not force-reset the remote main branch.

The Android database upgrade is independent of the Git migration. Version 0.2 uses the same application ID and development certificate as 0.1 and migrates Room v1 to v2. Downgrading the APK is not a supported rollback strategy for a v2 database; retain a device/data backup before deployment experiments.

Signing material is intentionally excluded from Git. Automatic approval review blocked uploading the development keystore. The supplied APK was built with the retained local key; fresh checkouts/CI use their own generated Android debug key.
