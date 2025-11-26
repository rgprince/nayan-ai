# Nayan AI

<div align="center">

![Nayan AI Logo](https://img.shields.io/badge/Nayan-AI-blue?style=for-the-badge)

**Break the Cloud Leash - Run AI Models Entirely on Your Phone**

[![Build APK](https://github.com/rgprince/nayan-ai/actions/workflows/build-apk.yml/badge.svg)](https://github.com/rgprince/nayan-ai/actions/workflows/build-apk.yml)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Private-red)](LICENSE)

</div>

---

## 🚀 What is Nayan AI?

**Nayan AI** is a revolutionary Android application that enables you to convert PyTorch models (`.pt`) to ONNX format **entirely on your device** and run AI inference **100% offline**. No cloud, no APIs, no data leaks—just pure, private, on-device AI.

### The Problem
- Running custom AI models requires heavy PC setups or expensive cloud servers
- Mobile AI apps rely on APIs, sacrificing privacy and offline capability
- Converting models requires technical expertise and multiple tools

### The Solution
Nayan AI is a **self-contained Mobile AI Studio** that:
- ✅ Converts PyTorch models to ONNX **on your phone**
- ✅ Runs AI inference **entirely offline**
- ✅ Provides a **modern chat interface** like ChatGPT/Claude
- ✅ Stores **chat history locally** with Room database
- ✅ Uses **Material You design** for Android 12+

---

## ✨ Features

### 🔄 On-Device Model Conversion
- Import `.pt` PyTorch checkpoint files directly from your phone
- Automatic conversion to optimized ONNX format using embedded Python (Chaquopy)
- Real-time progress tracking with visual feedback

### 💬 Modern Chat Interface
- **Multiple chat sessions** with persistent history
- **Auto-generated chat titles** from first message
- **Material You dynamic theming** from wallpaper
- **Glassmorphism UI effects** for premium feel
- **Typing indicators** and smooth animations

### 🔒 Privacy First
- **100% offline** - no internet connection required after model import
- **Zero data collection** - all data stays on your device
- **No APIs** - no third-party services involved

### ⚡ Performance
- **ONNX Runtime** with hardware acceleration (NNAPI/GPU delegate)
- **Temperature-based sampling** with Top-K filtering
- **Efficient tokenization** for fast inference
- **Multi-threaded processing** for responsiveness

---

## 🏗️ Architecture

```
┌─────────────────────────────────────┐
│        Material You UI              │
│  (Jetpack Compose + Material3)      │
└──────────────┬──────────────────────┘
               │
       ┌───────┴───────┐
       │               │
┌──────▼────┐   ┌─────▼─────┐
│  Factory  │   │  Engine   │
│ (Python)  │   │ (Kotlin)  │
│           │   │           │
│ PyTorch   │   │   ONNX    │
│ Converter │   │  Runtime  │
└───────────┘   └───────────┘
       │               │
       └───────┬───────┘
               │
        ┌──────▼──────┐
        │    Room     │
        │  Database   │
        └─────────────┘
```

**Components:**
1. **The "Factory" (Python/Chaquopy)**: Background conversion engine using PyTorch
2. **The "Engine" (Kotlin/ONNX Runtime)**: High-performance inference with ONNX
3. **The "Studio" (Compose UI)**: Modern Material You interface
4. **Room Database**: Persistent chat session and message storage

---

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Language** | Kotlin | 2.0.21 |
| **UI Framework** | Jetpack Compose | 2024.10.00 |
| **Design System** | Material3 (Material You) | Latest |
| **Python Integration** | Chaquopy | 15.0.1 |
| **ML Framework** | PyTorch (CPU) | 2.0.1 |
| **Inference Engine** | ONNX Runtime | 1.16.3 |
| **Database** | Room | 2.6.1 |
| **Navigation** | Navigation Compose | 2.8.4 |
| **Build Tool** | Gradle | 8.7 |
| **Min SDK** | Android 8.0 (API 26) | - |
| **Target SDK** | Android 15 (API 35) | - |

---

## 📦 Installation

### Download Pre-built APK
1. Go to [GitHub Actions](https://github.com/rgprince/nayan-ai/actions)
2. Click on the latest successful build
3. Download `nayan-ai-debug` artifact
4. Install the APK on your Android device

### Build from Source
```bash
# Clone the repository
git clone https://github.com/rgprince/nayan-ai.git
cd nayan-ai

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🚦 Usage

### 1. Import a Model
- Tap **"Import Model"** on the Model Manager screen
- Select a `.pt` PyTorch checkpoint file
- Wait for conversion to complete (30-60 seconds)

### 2. Start Chatting
- Tap **"New Chat"** to create a conversation
- Type your message and hit send
- AI responses are generated entirely on-device

### 3. Manage Conversations
- View all chat sessions on the home screen
- Delete unwanted conversations
- Chat history persists across app restarts

---

## 🧠 Model Specifications

**Current Architecture: Nayan GPT-2 Custom**
- **Layers**: 6
- **Attention Heads**: 6
- **Embedding Dimension**: 384
- **Context Length**: 1024 tokens
- **Vocabulary Size**: 50,304
- **Model Size**: ~50MB (ONNX)

*Note: The app is designed for small GPT-2 models optimized for mobile. Larger models may require more RAM and will be slower.*

---

## 🔧 Development

### Project Structure
```
nayan-ai/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/rgprince/nayanai/
│   │   │   ├── data/           # Room database, repositories
│   │   │   ├── model/          # Model conversion, inference
│   │   │   ├── ui/
│   │   │   │   ├── components/ # Reusable UI components
│   │   │   │   ├── screens/    # App screens
│   │   │   │   └── theme/      # Material theme
│   │   │   └── MainActivity.kt
│   │   ├── python/
│   │   │   └── converter.py    # PyTorch to ONNX converter
│   │   └── res/                # Android resources
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

### Key Files
- **converter.py**: Python script for model conversion
- **ModelConverter.kt**: Kotlin bridge to Python
- **OnnxInference.kt**: ONNX Runtime inference engine
- **ChatDatabase.kt**: Room database schema
- **ChatRepository.kt**: Data access layer

---

## ⚠️ Known Limitations

1. **Tokenizer**: Currently uses simplified character-level tokenization. For production, replace with actual GPT-2 BPE vocabulary.
2. **Model Size**: Large models (>500MB) may cause OOM errors on devices with <4GB RAM.
3. **Conversion Time**: PyTorch conversion on mobile CPU takes 30-60 seconds.
4. **APK Size**: ~180MB due to PyTorch and ONNX Runtime dependencies.

---

## 🛣️ Roadmap

- [ ] Implement real GPT-2 BPE tokenizer
- [ ] Add model quantization support (INT8/FP16)
- [ ] Support for multiple model architectures
- [ ] Export/import chat history
- [ ] Voice input/output
- [ ] Multi-language support

---

## 📄 License

This project is **private** and proprietary. All rights reserved.

---

## 👨‍💻 Developer

Built with ❤️ by **rgprince**

- GitHub: [@rgprince](https://github.com/rgprince)

---

## 🙏 Acknowledgments

- **Microsoft ONNX Runtime** for mobile inference
- **Chaquopy** for Python-on-Android integration
- **PyTorch** for the ML framework
- **Material You** design guidelines

---

<div align="center">

**Break the Cloud Leash. Own Your AI.**

</div>
