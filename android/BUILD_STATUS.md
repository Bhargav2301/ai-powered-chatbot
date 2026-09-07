# Verified build status — 0.1.0

Date: 7 September 2026. This is the first development milestone of the approved native Android architecture.

| Check | Result |
| --- | --- |
| Android debug APK | Built successfully |
| Application ID | `com.polymath.app` |
| Version | `0.1.0` / code `1` |
| Supported minimum | Android 9 / API 28 |
| Compile and target SDK | API 36 |
| Domain tests | 11 passed |
| Room/repository tests | 13 passed |
| RSS/Atom parser tests | 5 passed |
| Compose activity flow test | 1 passed |
| Total automated tests | **30 passed; 0 failed** |
| Android lint | **0 errors; 7 advisory warnings** |
| APK size | 66,792,515 bytes (63.7 MiB) |

The APK signature was checked with Android SDK `apksigner verify`. This build uses the retained development signing key, not a production release key.

APK SHA-256:

```text
2e5feceff5c1d66111e119dacc6f4605320fc1dc294f889e02ccb7209296b4cc
```

## What the tests establish

Tests exercise actual Kotlin rules, Room DAOs, repository transactions, RSS/Atom parsing, and the Hilt-backed Compose Activity under Robolectric API 35. They cover prefix search and deletion consistency, disk persistence across reopening, undo expiry and explicit-save ownership, note revision conflicts, duplicate recall submissions, review timing, task dependencies, EXP caps and reversal, and onboarding → save → vault → graph → note capture → activity recreation.

Rendered screens are included under `docs/screenshots`. These are captures of the real Activity view tree in Android framework simulation at 411 × 891 dp. They are not photographs of an installed APK. Visual review also checked and corrected the onboarding text contrast.

## Remaining verification

Physical-device installation and interaction, battery/thermal measurements, low-memory behavior, API 28/36 device coverage, large-font/TalkBack testing, and launcher/lock-screen widget compatibility have not been executed here. RSS parser fixtures passed; feed availability on the user's device/network is not guaranteed by these tests. No embedding or LLM inference benchmark has been run because model packs are not yet integrated.

Lint advisories concern API-31 widget sizing attributes with legacy minimum-size fallbacks and newer available dependency versions. The toolchain is pinned to a verified compatible set. Full details and sanitized JUnit results are in `docs/verification`.

## Build tools and command

JDK 17, Gradle 8.13, Android Gradle Plugin 8.11.1, Kotlin 2.1.21, Android SDK 36, Build Tools 35.0.0.

```sh
./gradlew :app:assembleDebug :core:model:test :core:data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
```

The GitHub Actions workflow is supplied for future repository use; it was not executed remotely during this handoff.

## Scope boundary

The working foundation includes on-device swipe ranking, a persisted reader and notebook, keyword search, template plans, recall/EXP, a topic graph, RSS fetching, and two public-content widgets. Semantic embeddings, local LLM/RAG, AI research/planning, export/restore, and the additional widget layouts remain in the implementation plan. The UI states these boundaries explicitly.
