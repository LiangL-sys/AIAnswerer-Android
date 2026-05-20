# AIAnswerer

[中文指南](README.md#中文使用指南) | [English Guide](#english-guide)

## English Guide

### Overview
AIAnswerer combines on-device OCR with large language models on Android. Capture questions through a floating overlay, let DeepSeek AI (OpenAI-compatible) analyze them, and receive instant answers—ideal for practice, review, or self-testing.

<img src="./image/main.png" width="300px"> <img src="./image/ai_setting.jpg" width="300px"> <img src="./image/answerer.jpg" width="300px"> <img src="./image/crop_demo.jpg" width="300px">

### Key Features
- 🖼️ Quick capture: Snap the current screen and auto-focus on the question area
- 📝 Smart OCR: Recognizes Chinese and English text with manual editing before submission
- 🤖 Instant AI answers: Generates explanations and copies the result to the clipboard
- 📋 Batch answering: Returns all answers at once when screenshot contains multiple questions
- 💬 Floating workflow: Control capture, preview, and submit without leaving your app
- 🔒 Full control: Bring your own API Key and pause network requests whenever needed
- 🌐 Bilingual: Supports Chinese and English interface

### Tech Stack
| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 |
| OCR | Google ML Kit (Chinese + Latin) |
| Network | OkHttp 4.12.0 |
| Storage | MMKV + EncryptedSharedPreferences |
| Build | Gradle (AGP 8.13.0) |

### Getting Ready
1. Use a device running Android 11 or later and ensure a stable internet connection.
2. Install the provided APK; enable "unknown sources" when prompted during the first install.
3. Prepare your LLM API Key
4. During the first launch, grant overlay, screenshot, and notification permissions as requested.

### Quick Start
1. Launch the app, tap "Enter answering mode," and confirm that all permissions are granted.  
   > Screenshot placeholder: Permission dialogs
2. Open the screen with your question and tap the floating button to capture it.  
   > Screenshot placeholder: Floating button guide
3. Review the recognized text in the confirmation view; adjust it if anything looks off.
4. Tap "Confirm & solve" to receive the AI-generated answer, which is also copied to your clipboard.
5. To exit, return to the main screen and tap "Stop service."

### Supported Question Types
- Multiple choice: Extracts question and options, highlights a recommended answer with reasoning
- Fill in the blank: Produces concise entries for each blank slot
- Short/long answer: Supplies structured explanations or outline-style responses

### Tips & Tricks
- Keep screenshots sharp and centered, and avoid busy backgrounds to improve OCR accuracy.
- Pause AI requests by toggling the setting in-app or briefly disconnecting from the network.
- Capture again at any time via the floating button to continue practicing new questions.

### FAQ
- **Missing permissions?** Open system settings, search for "overlay" or "screen capture," and enable the required entries manually.
- **Incorrect recognition?** Edit the text on the confirmation screen or retake the screenshot.
- **No AI response?** Verify connectivity, confirm your API Key, and make sure your DeepSeek balance covers the request.

### Project Structure

```
com.hwb.aianswerer/
├── BaseActivity.kt           # Unified language configuration base class
├── MyApplication.kt          # Application initialization
├── MainActivity.kt           # Main screen (permissions, settings)
├── FloatingWindowService.kt  # Core floating window service
├── ScreenCaptureManager.kt   # Screenshot management (MediaProjection)
├── TextRecognitionManager.kt # OCR text recognition
├── ConfirmTextActivity.kt    # Text confirmation/editing
├── ImageCropActivity.kt      # Image cropping (4-corner drag)
├── SettingsActivity.kt       # General settings
├── ModelSettingsActivity.kt  # API model configuration
├── AboutActivity.kt          # About page
├── Constants.kt              # Constants & system prompts
├── api/
│   └── OpenAIClient.kt       # OpenAI-compatible API client
├── config/
│   └── AppConfig.kt          # Configuration (MMKV + encrypted storage)
├── models/                   # Data models
├── ui/
│   ├── components/           # Shared Compose components
│   ├── dialogs/              # Dialog components
│   ├── icons/                # Local icon definitions
│   └── theme/                # Material3 theme
└── utils/
    ├── AppLog.kt             # Unified logging utility
    ├── ClipboardUtil.kt      # Clipboard utility
    ├── ImageCropUtil.kt      # Image crop utility
    └── LanguageUtil.kt       # Language switching utility
```

### Privacy & Disclaimer
- Recognized text is sent to your chosen AI provider; avoid sensitive or restricted content.
- DeepSeek usage may incur costs—monitor your API consumption responsibly.
- AIAnswerer is for learning and research purposes only. Respect exam rules and local regulations; you are accountable for any misuse.

### Update Instructions

#### v1.1 (Regex Filter & Thinking Mode)
* **Regex Filter Toggle**
  - New "Multi-question Regex Filter" toggle in web search settings (default: ON)
  - When OFF, Tavily search still runs even if multi-question pattern is detected
* **Thinking Mode Toggle**
  - New "Enable Thinking Mode" toggle in LLM model settings (default: OFF)
  - Sends `reasoning_effort: "medium"` parameter to API when enabled
  - For reasoning models like o1, DeepSeek-R1

#### v0.5 (Code Quality Optimization)
* **Security Enhancements**
  - API Key stored with EncryptedSharedPreferences, no longer plaintext
  - Release builds remove HTTP logs to prevent API Key leakage
  - Added OkHttp CertificatePinner for MITM protection
  - Authorization header redacted even in Debug mode
* **Architecture Improvements**
  - Extracted BaseActivity for unified language configuration, eliminated 6 duplicate code blocks
  - Floating window Composable moved to dedicated file, clearer Service responsibilities
  - Unified coroutine scope, fixed CancellationException being swallowed
* **Internationalization**
  - All floating window status messages support Chinese/English switching
  - Notification channel name, clipboard label use string resources
* **Network Enhancements**
  - Added network connectivity precheck, quick prompt when offline
  - API requests support automatic retry with exponential backoff
  - Service automatically cancels in-progress requests on destroy
* **Build Optimization**
  - Removed redundant ML Kit dependency (-10MB APK size)
  - All dependency versions unified in Version Catalog
  - Tightened ProGuard rules for better R8 obfuscation
* **Code Quality**
  - Unified AppLog utility, Release builds are silent
  - Eliminated all `e.printStackTrace()` calls
  - Fixed `savedCropRect!!` null safety risk
  - Added core unit tests (extractJsonPayload, isApiConfigValid)
* **JSON Parsing Optimization**
  - Batch answering: returns all answers when screenshot contains multiple questions
  - 5-level fallback parsing: direct parse → extract+fix → regex array → regex object → text extraction
  - Fixed JSON truncation caused by Chinese quotes
  - System prompt optimized: forces AI to fill answer field, never leave empty

#### v0.3
Added the pre-COR cropping function to improve the ability to recognize questions

#### v0.2
Fixed an issue where the release package could not request the AI API

#### v0.1
First edition

### License
This project is released under the [MIT License](/LICENSE)