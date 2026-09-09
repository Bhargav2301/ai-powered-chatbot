# Polymath Offline 0.3.1: testing guide

The offline preview includes both llama.cpp and the approved Qwen3-0.6B Q4_K_M model. It requires no inference endpoint, service account or API key. First-use preparation verifies and copies the included model into private storage; it does not download it.

## Identify the correct app

- APK: `Polymath_0.3.1_offline_preview.apk`.
- Launcher name: **Polymath Offline**.
- Chat version: **Polymath 0.3.1 · Offline preview**.
- Application ID: `com.polymath.app.offline`.

This is a separate preview installation. The old Polymath app and its data remain in place, and its vault is not automatically copied into the preview. There is no need to uninstall it to test offline AI. Different debug certificates can prevent normal in-place upgrades, which is one reason this preview has a distinct identity.

## First offline answer

1. Download and install the [offline preview APK](https://github.com/Bhargav2301/polymath_ai_news_chat/releases/download/v0.3.1-offline-preview/Polymath_0.3.1_offline_preview.apk).
2. Open Polymath Offline and complete topic selection. You may turn on airplane mode now.
3. Open **Desk → chat icon → Set up offline AI**.
4. Tap **Prepare included AI**. Wait for **Installed · ready for offline chat**, then **Done**.
5. Open **Datasets → Add example dataset → Done**. Select **Polymath foundations** in the chat scope chips.
6. Ask **What voltage is needed for 2 amps through 6 ohms?** The supplied example supports **12 volts**. Open its citation to inspect the original passage.

For your own material, save sources or write notes, then select **My vault**. Local chat answers from the selected text. It does not browse the live web or provide unrestricted general-purpose chat when no source supports an answer.

## If setup still asks for a server

| What you see | What to do |
|---|---|
| HTTPS/API-key fields | These belong to **Private server (advanced)**. Tap **Back to offline AI**. No credentials are needed for the local model. |
| Only the older Polymath icon or no 0.3.1 version label | Open the separately installed **Polymath Offline** app. If Android rejected an APK update, the older app may still be installed. |
| **Download Qwen** instead of **Prepare included AI** | This is a developer APK without bundled weights. Use the offline preview APK named above. |
| Insufficient free memory / device too hot | Follow the displayed resource message; let the device cool or free memory. The app will not upload the question as a fallback. |
| No matching evidence | Select the example dataset, or save relevant material and select My vault. An empty vault cannot support a sourced answer. |
| Incomplete or unvalidated answer | Inspect the passages and rephrase. The small model can fail; the app does not accept invalid citations as proof. |

## Storage and device requirements

The APK is 657,299,390 bytes (about 657 MB); the model is 396,705,472 bytes. This debug build also retains native debug symbols. Its APK copy remains installed while its verified working copy occupies private storage. Allow approximately 3 GB free for the download, installation staging and model preparation. Removing the working model frees that copy; the APK still includes the original pack.

Android 9 or newer, a supported 64-bit ABI, approximately 4 GB RAM or more and sufficient available memory are required. Resource checks remain active in the preview. Emulator results do not establish performance on a particular physical phone.

## Verification scope

The [preview workflow](https://github.com/Bhargav2301/polymath_ai_news_chat/actions/runs/34343013163) passed: it audited the previously linked 0.3 APK, verified the new APK version/application ID, checked the embedded model checksum and native alignment, and ran 53 unit/Robolectric tests plus lint. Its Android UI test prepared the model from APK assets, imported the example through the UI and received a cited answer with airplane mode enabled and no service configured. No model was injected into app storage by adb in this test. This ran on an API 35 x86_64 emulator with 6 GB RAM.

The [publisher](https://github.com/Bhargav2301/polymath_ai_news_chat/actions/runs/34344548170) independently downloaded the public APK without credentials and verified its APK and model checksums. The released APK SHA-256 is `bad63923c4486fb9eec45d2277071c615ab00b9c84fc6dff3e559758eae3bd30`.

See [verification records](VERIFICATION.md) for completed run evidence and [architecture/build instructions](ON_DEVICE_AI.md) for implementation details and remaining device gates.
