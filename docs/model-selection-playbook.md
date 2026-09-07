# Model selection for Polymath engineering

The repository methodology is model-agnostic. Agent effort settings are optional metadata and do not change application behavior. No development workflow requires a proprietary provider.

Application inference uses the pinned permissively licensed models described in the [service setup](../services/rag/README.md), with checksums in [the model lock](model-lock.json). Evaluate replacement models against the same scope, provenance and resource tests before changing that lock. Keep on-device and server measurements separate.
