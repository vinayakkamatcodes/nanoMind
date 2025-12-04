# NanoMInd 🤖

An on-device AI chat application for Android that runs LLM models completely offline using llama.cpp.

## Features

- ✅ **100% On-Device**: Your conversations stay private - no internet required
- ✅ **GGUF Model Support**: Compatible with any GGUF format models
- ✅ **Real-time Streaming**: Watch responses generate token-by-token
- ✅ **Modern UI**: Built with Jetpack Compose Material3
- ✅ **Arm-Optimized**: Leverages Arm CPU features for efficient inference

## Tech Stack

- **Language**: Kotlin 2.2.0
- **UI Framework**: Jetpack Compose
- **LLM Engine**: [kotlinllamacpp](https://github.com/ljcamargo/kotlinllamacpp) 0.2.0
- **Architecture**: MVVM with Kotlin Coroutines & Flow
- **Minimum SDK**: Android 24 (Nougat)
- **Target SDK**: Android 36

## Setup

### Prerequisites
- Android Studio (latest version recommended)
- Android device with arm64-v8a processor
- A GGUF model file (recommended: Q4 or Q5 quantized, < 3GB)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/NanoMInd.git
   cd NanoMInd
   ```

2. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on your device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
   
   Or use the quick install script:
   ```bash
   ./install.sh
   ```

### Adding a Model

1. Download a GGUF model (e.g., from [HuggingFace](https://huggingface.co/models?search=GGUF))
2. Rename it to `nanomind_model.gguf`
3. Place it in your device's **Downloads** folder
4. Grant storage permissions when the app requests them
5. The app will automatically load the model on startup

**Recommended Models:**
- [TinyLlama 1.1B Q4](https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF) - Fast, great for testing
- [Phi-2 Q4](https://huggingface.co/TheBloke/phi-2-GGUF) - Good balance of size and quality
- Any GGUF model under 3GB for smooth mobile performance

## Project Structure

```
app/src/main/java/com/example/nanomind/
├── MainActivity.kt          # Main activity with FileProvider setup
├── ChatViewModel.kt         # Chat logic and LLM integration
└── res/
    └── xml/file_paths.xml   # FileProvider configuration
```

## Key Implementation Details

### FileProvider for Model Access
Modern Android requires `content://` URIs for file access. This app uses FileProvider to convert file paths to proper URIs:

```kotlin
val contentUri = FileProvider.getUriForFile(
    context,
    "${packageName}.fileprovider",
    modelFile
)
```

### Streaming Token Generation
Real-time response updates using Kotlin Flow:

```kotlin
llmFlow.collect { event ->
    when (event) {
        is LlamaHelper.LLMEvent.Ongoing -> {
            accumulatedText += event.word
            updateMessage(accumulatedText)
        }
    }
}
```

## Performance Tips

- Use Q4 or Q5 quantized models for best mobile performance
- Adjust `contextLength` in `ChatViewModel.kt` based on available RAM (default: 2048)
- Smaller models (< 3B parameters) are recommended for phones
- First response may be slower as the model initializes

## Troubleshooting

**Model not loading?**
- Ensure file is named exactly `nanomind_model.gguf`
- Check it's in the Downloads folder (not a subfolder)
- Verify storage permissions are granted
- Check logcat: `adb logcat -s NanoMInd`

**App crashes on model load?**
- Model may be too large for available RAM
- Try a smaller or more quantized model (Q4_K_M recommended)

**Slow inference?**
- Normal for larger models on mobile devices
- Try a smaller model or higher quantization level
- Ensure your device has arm64-v8a architecture

## Build Issues Resolved

This project overcame several challenges during development:
- ✅ Kotlin 2.0+ Compose Compiler plugin configuration
- ✅ ContentResolver file access on modern Android
- ✅ FileProvider URI generation for external storage
- ✅ Flow collection lifecycle management for streaming responses

See [walkthrough.md](.gemini/antigravity/brain/17f1a699-7154-49a9-8302-c1a092c3320f/walkthrough.md) for detailed implementation notes.

## License

MIT License - feel free to use and modify!

## Acknowledgments

- [llama.cpp](https://github.com/ggerganov/llama.cpp) by Georgi Gerganov
- [kotlinllamacpp](https://github.com/ljcamargo/kotlinllamacpp) by ljcamargo
- Built with ❤️ for on-device AI

## Contributing

Contributions are welcome! Feel free to:
- Report bugs
- Suggest features
- Submit pull requests

---

**Note**: This is an offline AI assistant. No data leaves your device. All processing happens locally on your phone.
