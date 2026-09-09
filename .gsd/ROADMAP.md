# Polymath execution roadmap

## Local inference integration

1. Pin Qwen3-0.6B Q4_K_M and llama.cpp; compare MLC, MediaPipe/LiteRT-LM and Gemma: complete.
2. Add a verified model pack, bounded offline retrieval, isolated native inference and explicit device/server selection: complete.
3. Verify build, retrieval, model integrity, JNI inference and lifecycle; publish implementation/build guide with physical-device gates: complete.

## Original migration

1. Preserve complete old repository on deprecated branch: complete.
2. Implement private RAG service and Android chat, imports, images, domains and migrations: complete.
3. Verify real models, API, persistence, UI rendering, build and lint: complete.
4. Synchronize docs and create migration commits: complete; final remote ref/CI confirmation at handoff.

Future engineering slices and acceptance gates are tracked in docs/ROADMAP.md.

## Public testing download (2026-09-09)

1. Publish the existing successful CI APK with a checksum and build provenance; verify anonymous download: complete.
2. Add the verified release link, Android installation steps and test guidance to the root README; update its renamed repository URL: complete.
