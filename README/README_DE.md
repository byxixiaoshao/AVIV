# AVIV — Weißes Rauschen & Musik-Player

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**Ein Android-Player für weißes Rauschen und Musik, gebaut um eine C++-Echtzeit-Audio-Engine, der ein Dutzend professionelle Effekte, räumliches Audio, AutoEQ und Online-Streaming integriert.**

[Funktionen](#-funktionen) · [Audio-Engine](#-audio-engine) · [Architektur](#-architektur) · [Build](#-aus-quellcode-kompilieren) · [Abhängigkeiten & Lizenzen](#-open-source-abhängigkeiten--lizenzen)

[English](../README.md) · [中文](README_ZH.md) · [日本語](README_JA.md) · [한국어](README_KO.md) · [Русский](README_RU.md) · [Français](README_FR.md) · **Deutsch(diese Datei)** · [Español](README_ES.md) · [Italiano](README_IT.md) · [Português](README_PT.md)

</div>

---

## 📖 Über

AVIV vereint Wiedergabe von weißem Rauschen, lokale/Online-Musik und eine professionelle Echtzeit-Audio-Verarbeitungs-Engine in einer App. Alle Effekte werden in Echtzeit in der C++-Schicht über Oboe-Niedriglatenz-Audioströmen berechnet — kein Pre-Rendering, Parameteränderungen werden sofort wirksam. Geeignet für Schlafhilfe, Konzentration, Meditation, Atmosphäre und Musikverbesserung.

> ⚠️ **Hinweis:** Beim ersten Start ist die Log-Erfassung standardmäßig aktiviert (zum Sammeln von Crash-Logs) und kann in den Einstellungen deaktiviert werden.

---

## ✨ Funktionen

### 🌊 Weißes Rauschen
- Mehrere eingebaute Quellen, nach Kategorie organisiert, lokal und online
- **Scatter-Modus**: zufällige Wiedergabe mit anpassbarem Intervall und Bereich, simuliert ein natürliches Klangfeld
- **Unabhängige Effektkette** pro Spur (EQ / Reverb / Räumlich / Kreativ), ohne gegenseitige Störung
- Lautstärkeverstärkung 0–300 %

### 🎵 Musikwiedergabe
- Multi-Format (MP3 / WAV / FLAC / AAC usw.)
- Playlist-Verwaltung
- Online-Streaming über die **QuickJS-Quellskript-Engine**, automatische Multi-Engine-Erkennung & Failover bei Timeout

### 🎛️ Echtzeit-Audioeffekte (C++-Engine)
| Effekt | Beschreibung |
|------|------|
| **Equalizer (EQ)** | Manuelle Multiband-BiQuad-Filter, spurunabhängige Kurven, mit Bypass |
| **AutoEQ-Lautsprecherkompensation** | 12 Geräte-Presets, 16 einstellbare Parameter, alle Filterparameter (Verstärkung/Frequenz/Q) pro Band editierbar und gespeichert |
| **Räumliches Audio** | 3D-Quellpositionierung, Distanzdämpfung, Surround-Bahn (0,25–10 s/Umdrehung), zufällige Streuung, fester Offset |
| **Reverb** | Raumgröße, Abklingzeit, Pre-Delay, Wet/Dry-Mix, Isolation |
| **Limiter** | Brick-Wall-Begrenzung, Übertragungsfunktionskurve + VU-Meter + GR-Meter, Echtzeit-Visualisierung |
| **Kreative Effekte** | Lo-Fi / 8-bit / Unterwasser / Alien-Signal / Megafon / Verzerrung |
| **Pseudo-Restaurierung** | HiFi-Verbesserung (Soft-Knee-Transiente, DC-Block, Gain-Glättung) |
| **Multiband-Kompressor** | MultibandCompressor |
| **Stereo-Verbreiterer / Virtueller Bass / Isolation** | StereoWidener / VirtualBass / Insulation |
| **Tempo & Tonhöhe** | Angetrieben von **SoundTouch**, Tempo 0,1–5,0×, Tonhöhe ±12 Halbtöne, unabhängig einstellbar |

### 🎛️ Mixer-Panel
- One-Stop-Effektsteuerung: Effektintensität, EQ, AutoEQ, räumliches Audio, Reverb, Limiter, Tempo/Tonhöhe
- Echtzeit-Vorschau, sofortige Parameteranwendung
- Ein-Tap-Zurücksetzen

### ⏰ Timer
- Sleep-Timer (bis zu 23 Std. 59 Min.)
 Snooze-Modus
- Timer-Ball antippen, um die Messung zu starten

### 🐾 Schwebendes Haustier
- Schwebendes Haustier auf dem Desktop, verschiebbar / skalierbar / ausblendbar
- Zentriert skaliert, Aufwecken aus verborgenem Zustand

### 🎨 Personalisierung
- **Benutzerdefinierte Themen**: HSV-Farbanpassung + Preset-Themen
- **Frosted-Glass / Liquid-Glass**-Effekte
- Automatische Persistenz von Effektparametern

### 🔔 Benachrichtigungssystem
- **In-App-Benachrichtigungen**: 5 Typen (Info/Erfolg/Warnung/Fehler/Laden), Prioritätsstufen, Wischen zum Schließen, Aktions-Buttons
- **System-Medienbenachrichtigungen**: MediaSession-Integration, Sperrbildschirm-Steuerung

### 🛡️ Stabilität & Keep-Alive
- **Audiostrom-Wartung**: AudioFocus-Strategie, fehlertolerante Wiederholung bei Stromabbruch, Buffer-Optimierung, graceful Oboe-Fehlerwiederherstellung, MediaPlayer-Fallback
- **Keep-Alive-Hintergrundanleitung**: Batterieoptimierungs-Whitelist + Hersteller-Autostart-Einstellungsanleitung
- **Speichersperren-Diagnose**: Ausnahmeüberwachung (Audio-Engine-Neustart / Buffer-Underrun / Main-Thread-Hänger / Speicherwarnungen) + Diagnoseberichte
- **Log-System**: unabhängiger Prozess erfasst Logcat & Crash-Logs

### 🌍 Mehrsprachig
10 Sprachen: Chinesisch, Englisch, Japanisch, Koreanisch, Russisch, Französisch, Deutsch, Spanisch, Italienisch, Portugiesisch

---

## 🔊 Audio-Engine

### Verarbeitungspipeline

```
Decoder ──► Lautstärke/Fade ──► Isolation ──► Räumliches Audio ──► Reverb
        ──► EQ / AutoEQ ──► Kreativ ──► Pseudo-Restaurierung ──► Limiter
        ──► SoundTouch(Tempo/Tonhöhe) ──► Hartes Clipping ──► Oboe-Ausgabe
```

### C++-Modulstruktur

| Verzeichnis | Verantwortung |
|------|------|
| `cpp/AudioPlayFunc/` | `AudioEngine` (Oboe-Stromverwaltung, Multitrack-Mixing), `AudioTrack` (Decodierung & Effektkette pro Spur) |
| `cpp/AudioEffect/` | 12 Effektprozessoren + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | BiQuad-Filter-Implementierung |
| `cpp/reverb/` | Reverb-Algorithmus |
| `cpp/spatial_audio/` | Räumliches-Audio-HRTF |
| `cpp/oboe/` | Google Oboe-Niedriglatenz-Audiobibliothek |
| `cpp/ffmpeg/` | FFmpeg-Decodierung (LGPL) |

### JNI-Schnittstelle
`JniInterface.cpp` stellt alle Audiofähigkeiten bereit (Wiedergabesteuerung, Effektparameter, EQ-Kurven, räumliches Audio, AutoEQ, Limiter, SoundTouch-Tempo/Tonhöhe usw.); Kotlin-Seite gewrappt von `OboeAudioEngine`.

---

## 🏗️ Architektur

| Schicht | Technologie |
|----|------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Audio | C++ + Oboe + SoundTouch + FFmpeg (JNI) |
| Netzwerk | OkHttp |
| Skript | QuickJS-kt (Online-Quellskript-Engine) |
| Speicher | Gson (JSON-Config) + EncryptedSharedPreferences |
| Medien | AndroidX Media (MediaSession) |
| Sonstiges | Lottie / Coil / Biometrie / MTDataFilesProvider |

### Wichtige Kotlin-Pakete

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine, räumliches Audio, Reverb, Scatter-Verwaltung
├── music/          # Musikwiedergabesteuerung
├── onlinemusic/    # Online-Streaming & Quellskript-Engine
├── storage/        # Config/Weißes-Rauschen/Musik/Themen-Speicher (JSON)
├── ui/             # Compose-UI & Komponenten
├── servies/        # MusicService (Vordergrund-Mediadienst)
├── service/        # MemoryLockService, LogCaptureService (:log-Prozess)
├── floatingpet/    # Schwebendes-Haustier-Dienst
├── timer/          # Timer
├── equalizer/      # Equalizer-Logik
└── ...             # data / security / utils / playlist usw.
```

---

## 📥 Download & Installation

### Systemanforderungen
- Android 7.0 (API 24) oder höher
- Mindestens 100 MB freier Speicher

### Installation
1. Neueste APK von [Releases](https://github.com/byxixiaoshao/AVIV/releases) herunterladen
2. „Installation unbekannter Apps" auf dem Gerät aktivieren
3. APK öffnen zum Installieren
4. App starten

---

## 🔧 Aus Quellcode kompilieren

### Voraussetzungen
- Android Studio (unterstützt aktuelle AGP-Version)
- JDK 11
- Android SDK, compileSdk 36
- NDK 27.0.12077973, CMake 4.3.0
- Unterstützte ABIs: `arm64-v8a`, `armeabi-v7a`

### Schritte
```bash
git clone <Repo-URL>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> Der Build erfordert eine `keystore.properties`-Signaturkonfiguration. SoundTouch-Quellen liegen im Projekt-Root `soundtouch/` und werden von CMake automatisch als statische Bibliothek kompiliert und gelinkt.

---

## 📖 Bedienungsanleitung

### Weißes Rauschen
1. Auf dem Hauptbildschirm Kategorie und Quelle wählen
2. Quellkarte antippen, um Wiedergabe zu starten
3. Karte lange drücken, um unabhängige Effekte zu konfigurieren
4. Scatter-Modus: mehrere Quellen zufällig wiedergeben

### Musik
1. Obere Wiedergabeleiste nach unten wischen, um den Player zu öffnen
2. Musik über Dateiauswahl hinzufügen
3. Effekte im Mixer-Panel anpassen

### Effektanpassung
**Mixer-Panel** öffnen:
- **Equalizer** → Equalizer Panel
- **AutoEQ-Kompensation** → Speaker Compensation (pro Band editierbar)
- **Räumliches Audio** → Spatial Audio Panel
- **Reverb** → Reverb Panel
- **Limiter** → Limiter (mit Übertragungsfunktions-Visualisierung)
- **Tempo/Tonhöhe** → Geschwindigkeitsanpassung (SoundTouch)

---

## 📦 Open-Source-Abhängigkeiten & Lizenzen

| Abhängigkeit | Lizenz | Verwendung |
|------|------|------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | Niedriglatenz-Audio-Engine |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | Unabhängige Tempo/Tonhöhen-Anpassung |
| FFmpeg | LGPL v2.1+ | Audio-Decodierung |
| Jetpack Compose / AndroidX | Apache 2.0 | UI-Framework |
| Material Components | Apache 2.0 | UI-Komponenten |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | Einstellungsspeicher |
| AndroidX Security | Apache 2.0 | Verschlüsselte Einstellungen |
| AndroidX Biometric | Apache 2.0 | Biometrische Authentifizierung |
| Gson | Apache 2.0 | JSON-Serialisierung |
| OkHttp | Apache 2.0 | HTTP-Netzwerk |
| Lottie | Apache 2.0 | Animation |
| Coil | Apache 2.0 | Bildladung |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | Quellskript-Engine |
| Liquid Glass Android | Apache 2.0 | Glassmorphism-Effekte |
| MTDataFilesProvider | Apache 2.0 | Medien-Store-Datenanbieter |

> Tonquellen stammen von [Pixabay](https://pixabay.com/) unter der Pixabay License.

---

## ⚠️ Lizenz

Lizenziert unter der **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)**.

Siehe die [LICENSE](../LICENSE)-Datei für den vollständigen Lizenztext.

> Drittanbieter-Bibliotheken behalten ihre ursprünglichen Lizenzen (z. B. SoundTouch — LGPL v2.1, FFmpeg — LGPL v2.1+, QuickJS-kt — MIT).

---

## 📧 Kontakt

- **Issues**: [GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email**: [3139105039@qq.com](mailto:3139105039@qq.com)
- **Autor**: byxixiaoshao / Bicy Studio

---

## 🙏 Besonderer Dank

### Tonquelle
Alle weißes-Rauschen-Klänge stammen von **[Pixabay](https://pixabay.com/)**. Lange auf eine Klangoption drücken, um Autoreninfos zu sehen — unterstützen Sie die Originalautoren, wenn möglich.

### Software-Tests
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### Kunst-Support
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

Danke an alle Open-Source-Projekte, die zu Audio-Verarbeitungstechnologien beitragen.

---

<div align="center">

**⭐ Wenn dieses Projekt Ihnen hilft, geben Sie ihm einen Star ⭐**

Made with ❤️ by Bicy Studio

</div>
