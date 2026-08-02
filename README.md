# AVIV — White Noise & Music Player

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**An Android white-noise & music player built around a real-time C++ audio engine, integrating a dozen professional effects, spatial audio, AutoEQ and online streaming.**

[Features](#-features) · [Audio Engine](#-audio-engine) · [Architecture](#-architecture) · [Build](#-build-from-source) · [Open Source & Licenses](#-open-source-dependencies--licenses)

**English (this file)** · [中文](README/README_ZH.md) · [日本語](README/README_JA.md) · [한국어](README/README_KO.md) · [Русский](README/README_RU.md) · [Français](README/README_FR.md) · [Deutsch](README/README_DE.md) · [Español](README/README_ES.md) · [Italiano](README/README_IT.md) · [Português](README/README_PT.md)

</div>

---

## 📖 About

AVIV fuses white-noise playback, local/online music and a professional-grade real-time audio processing engine into a single app. Every effect is computed in real time in the C++ layer on top of Oboe low-latency audio streams — no pre-rendering, and every parameter change takes effect instantly. Suitable for sleep aid, focus, meditation, ambience and music enhancement.

> ⚠️ **Note:** On first launch, log capture is enabled by default (to collect crash logs) and can be turned off in Settings.

---

## ✨ Features

### 🌊 White Noise
- Multiple built-in sound sources organized by category, supporting both local and online sources
- **Scatter mode**: randomized playback with customizable interval and range, simulating a natural sound field
- Per-track **independent effect chain** (EQ / reverb / spatial / creative effects) — no interference between tracks
- Volume gain range 0–300%

### 🎵 Music Playback
- Multi-format support (MP3 / WAV / FLAC / AAC, etc.)
- Playlist management
- Online streaming powered by the **QuickJS source-script engine**, with automatic multi-engine detection & failover when a single engine times out

### 🎛️ Real-time Audio Effects (C++ Engine)
| Effect | Description |
|--------|-------------|
| **Equalizer (EQ)** | Manual multi-band BiQuad filters, per-track independent curves, with Bypass |
| **AutoEQ Speaker Compensation** | 12 device presets (phone / headphones / Bluetooth / car…), 16 tunable parameters; every filter parameter (gain / frequency / Q) editable per-band and persisted |
| **Spatial Audio** | 3D source positioning, distance attenuation, surround trajectory (0.25–10 s/rev), random scatter, fixed offset |
| **Reverb** | Room size, decay time, pre-delay, wet/dry mix, insulation |
| **Limiter** | Brick-wall limiting, transfer-function curve + VU meter + gain-reduction meter, real-time visualization |
| **Creative Effects** | Lo-Fi / 8-bit / Underwater / Alien Signal / Megaphone / Distortion |
| **Pseudo-Restoration** | HiFi enhancement (soft-knee transient, DC blocking, gain smoothing) |
| **Multiband Compressor** | MultibandCompressor |
| **Stereo Widener / Virtual Bass / Insulation** | StereoWidener / VirtualBass / Insulation |
| **Rate & Pitch** | Powered by **SoundTouch**, rate 0.1–5.0×, pitch ±12 semitones, independently decoupled |

### 🎛️ Mixer Panel
- One-stop effect control: effect intensity, EQ, AutoEQ, spatial audio, reverb, limiter, rate/pitch
- Real-time preview, instant parameter application
- One-tap reset

### ⏰ Timer
- Sleep timer (up to 23h 59m)
- Snooze mode
- Tap the timer ball to start timing

### 🐾 Floating Pet
- Desktop floating pet, draggable / resizable / hideable
- Center-anchored scaling, wake-up from hidden state

### 🎨 Personalization
- **Custom themes**: HSV color customization + preset themes
- **Frosted-glass / Liquid-glass** effects
- Automatic persistence of effect parameters

### 🔔 Notification System
- **In-app notifications**: 5 types (Info / Success / Warning / Error / Loading), priority levels, swipe-to-dismiss, action buttons
- **System media notifications**: MediaSession integration, lock-screen controls

### 🛡️ Stability & Keep-Alive
- **Audio-stream maintenance**: AudioFocus strategy, stream-disconnect fault tolerance & retry, buffer optimization, graceful Oboe error recovery, MediaPlayer fallback
- **Background keep-alive guidance**: battery-optimization whitelist + vendor auto-start settings guidance
- **Memory-lock diagnostics**: exception monitoring (audio-engine restart / buffer underrun / main-thread jank / memory warnings) + diagnostic reports
- **Logging system**: independent process capturing Logcat & crash logs

### 🌍 Multi-language
Supports 10 languages: Chinese, English, Japanese, Korean, Russian, French, German, Spanish, Italian, Portuguese

---

## 🔊 Audio Engine

### Processing Pipeline

```
Decoder ──► Volume/Fade ──► Insulation ──► Spatial Audio ──► Reverb
        ──► EQ / AutoEQ ──► Creative FX ──► Pseudo-Restoration ──► Limiter
        ──► SoundTouch(Rate/Pitch) ──► Hard-clip ──► Oboe Output
```

### C++ Module Structure

| Directory | Responsibility |
|-----------|----------------|
| `cpp/AudioPlayFunc/` | `AudioEngine` (Oboe stream management, multi-track mixing), `AudioTrack` (per-track decoding & effect chain) |
| `cpp/AudioEffect/` | 12 effect processors + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | BiQuad filter implementation |
| `cpp/reverb/` | Reverb algorithm |
| `cpp/spatial_audio/` | Spatial-audio HRTF |
| `cpp/oboe/` | Google Oboe low-latency audio library |
| `cpp/ffmpeg/` | FFmpeg decoding (LGPL) |

### JNI Interface
`JniInterface.cpp` exposes all audio capabilities (playback control, effect parameters, EQ curves, spatial audio, AutoEQ, limiter, SoundTouch rate/pitch, etc.); the Kotlin side is wrapped by `OboeAudioEngine`.

---

## 🏗️ Architecture

| Layer | Technology |
|-------|------------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Audio | C++ + Oboe + SoundTouch + FFmpeg (JNI) |
| Network | OkHttp |
| Scripting | QuickJS-kt (online source-script engine) |
| Storage | Gson (JSON config) + EncryptedSharedPreferences |
| Media | AndroidX Media (MediaSession) |
| Other | Lottie / Coil / Biometric / MTDataFilesProvider |

### Main Kotlin Packages

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine, spatial audio, reverb, scatter-playback manager
├── music/          # Music playback control
├── onlinemusic/    # Online streaming & source-script engine
├── storage/        # Config / white-noise / music / theme storage (JSON)
├── ui/             # Compose UI & components
├── servies/        # MusicService (foreground media service)
├── service/        # MemoryLockService, LogCaptureService (:log process)
├── floatingpet/    # Floating pet service
├── timer/          # Timer
├── equalizer/      # Equalizer logic
└── ...             # data / security / utils / playlist, etc.
```

---

## 📥 Download & Install

### System Requirements
- Android 7.0 (API 24) or higher
- At least 100 MB of free storage

### Installation
1. Download the latest APK from [Releases](https://github.com/byxixiaoshao/AVIV/releases)
2. Enable "Install unknown apps" on your device
3. Open the APK to install
4. Launch the app

---

## 🔧 Build from Source

### Requirements
- Android Studio (supporting the current AGP version)
- JDK 11
- Android SDK, compileSdk 36
- NDK 27.0.12077973, CMake 4.3.0
- Supported ABIs: `arm64-v8a`, `armeabi-v7a`

### Steps
```bash
git clone <repo-url>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> The build requires a `keystore.properties` signing config. SoundTouch sources live in the project root `soundtouch/` directory and are compiled into a static library by CMake automatically.

---

## 📖 Usage Guide

### White Noise
1. On the main screen, pick a white-noise category and source
2. Tap a source card to start playback
3. Long-press a card to configure its independent effects
4. Scatter mode: randomly play multiple sources

### Music
1. Swipe down on the top playback bar to enter the player screen
2. Add music via the file picker
3. Tweak effects in the mixer panel

### Effect Tuning
Open the **Mixer panel**:
- **Equalizer** → Equalizer Panel
- **AutoEQ Compensation** → Speaker Compensation (per-band editable)
- **Spatial Audio** → Spatial Audio Panel
- **Reverb** → Reverb Panel
- **Limiter** → Limiter (with transfer-function visualization)
- **Rate/Pitch** → Speed adjustment (SoundTouch)

---

## 📦 Open-Source Dependencies & Licenses

| Dependency | License | Usage |
|------------|---------|-------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | Low-latency audio engine |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | Independent rate / pitch adjustment |
| FFmpeg | LGPL v2.1+ | Audio decoding |
| Jetpack Compose / AndroidX | Apache 2.0 | UI framework |
| Material Components | Apache 2.0 | UI components |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | Preferences storage |
| AndroidX Security | Apache 2.0 | Encrypted preferences |
| AndroidX Biometric | Apache 2.0 | Biometric authentication |
| Gson | Apache 2.0 | JSON serialization |
| OkHttp | Apache 2.0 | HTTP networking |
| Lottie | Apache 2.0 | Animation |
| Coil | Apache 2.0 | Image loading |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | Online source-script engine |
| Liquid Glass Android | Apache 2.0 | Glassmorphism effects |
| MTDataFilesProvider | Apache 2.0 | Media-store data provider |

> Sound sources come from [Pixabay](https://pixabay.com/) under the Pixabay License.

---

## ⚠️ License

Licensed under the **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)**.

See the [LICENSE](LICENSE) file for the full license text.

> Third-party libraries retain their original licenses (e.g. SoundTouch — LGPL v2.1, FFmpeg — LGPL v2.1+, QuickJS-kt — MIT).

---

## 📧 Contact

- **Issues**: [GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email**: [3139105039@qq.com](mailto:3139105039@qq.com)
- **Author**: byxixiaoshao / Bicy Studio

---

## 🙏 Special Thanks

### Sound Source
All white-noise sounds are from **[Pixabay](https://pixabay.com/)**. Long-press a sound option to view the audio author details — support the original creators if you can.

### Software Testing
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### Art Support
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

Thanks to all the open-source projects that contribute to audio-processing technology.

---

<div align="center">

**⭐ If this project helps you, please give it a Star ⭐**

Made with ❤️ by Bicy Studio

</div>
