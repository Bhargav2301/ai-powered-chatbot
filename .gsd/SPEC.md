# Polymath migration specification
Status: FINALIZED

Approved extension (2026-09-09): implement optional on-device Qwen inference with a pinned 4-bit model, verified model installation, offline scoped retrieval and citations, process isolation, cancellation and resource limits. Preserve explicit opt-in server mode without automatic network fallback. Research Gemma/LiteRT-LM as a conditional fallback and document device acceptance gates and Gradle/NDK steps. Native/emulator checks establish integration; physical-device performance remains a separate release gate.

Approved by the user on 2026-09-07. Replace the Rasa chatbot with the native Android Polymath project while preserving the full old tree and history on deprecated/chatbot-2026-09-07.

Deliver real dataset-scoped AI chat using permissively licensed open-source models through an explicitly configured self-hosted service; retain local-first storage and offline features. Expand learning domains, retain source-associated images in imported facts and feed cards, migrate Room without data loss, update every README, test and push main without force.

Acceptance: Android build and tests, server API and retrieval tests, real model smoke test, source image association and UI checks, migration evidence, ordinary descendant Git history.

Approved extension (2026-09-09): use the renamed `Bhargav2301/polymath_ai_news_chat` repository and add a public initial-app APK download to the root README. Publish the existing verified CI APK as a versioned GitHub prerelease, include install/service/signing guidance, and verify anonymous download before linking it.

Approved correction (2026-09-09): deliver a clearly identifiable offline preview APK that includes the pinned Qwen weights, gives local setup priority without HTTPS/API-key fields, retains explicit advanced server choice, and tests the actual first-use chat UI with the bundled model in airplane mode. Publish the exact verified APK as a versioned prerelease and update the README download. Use a separate preview application ID to preserve existing installations and their data across differing debug certificates.
