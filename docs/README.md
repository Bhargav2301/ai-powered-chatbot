# Polymath engineering documentation

- [Offline preview setup and troubleshooting](OFFLINE_PREVIEW.md).
- [On-device AI implementation and build guide](ON_DEVICE_AI.md): Qwen/Gemma evaluation, quantization, memory, isolated inference, model distribution, Gradle/NDK setup and acceptance gates.
- [Local model lock](local-model-lock.json): exact Q4 artifact revision, length, checksum and native runtime commit.
- [Architecture](ARCHITECTURE.md): data flows, RAG boundaries and media provenance.
- [Verification](VERIFICATION.md): actual checks and remaining device gates; [0.3.1 offline preview receipt](offline-preview-verification.json) and [historical 0.3 local integration receipt](local-verification.json).
- [Roadmap](ROADMAP.md): actionable follow-up work beyond the current development build.
- [Migration](MIGRATION.md): preserved legacy branch, history and rollback procedure.
- [Model lock](model-lock.json): pinned official model artifacts and SHA-256 hashes.
- [Real-model smoke receipt](model-smoke.json): actual MiniLM/Qwen response and citation check.
- [Service test receipt](rag-tests.xml): machine-readable JUnit results.
- [Android build status](../android/BUILD_STATUS.md) and [screen captures](../android/docs/screenshots/).

Use the root [README](../README.md) for setup and the [service README](../services/rag/README.md) for operations. Legacy Rasa documentation lives only on the deprecated branch.
