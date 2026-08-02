# AVIV — Player di rumore bianco e musica

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**Un player Android di rumore bianco e musica costruito attorno a un motore audio C++ in tempo reale, che integra una dozzina di effetti professionali, audio spaziale, AutoEQ e streaming online.**

[Funzionalità](#-funzionalità) · [Motore audio](#-motore-audio) · [Architettura](#-architettura) · [Compilazione](#-compila-dai-sorgenti) · [Dipendenze e licenze](#-dipendenze-open-source--licenze)

[English](../README.md) · [中文](README_ZH.md) · [日本語](README_JA.md) · [한국어](README_KO.md) · [Русский](README_RU.md) · [Français](README_FR.md) · [Deutsch](README_DE.md) · [Español](README_ES.md) · **Italiano(questo file)** · [Português](README_PT.md)

</div>

---

## 📖 Informazioni

AVIV fonde riproduzione di rumore bianco, musica locale/online e un motore professionale di elaborazione audio in tempo reale. Ogni effetto è calcolato in tempo reale nel livello C++ sui flussi audio a bassa latenza Oboe — senza pre-rendering, le modifiche dei parametri si applicano istantaneamente. Adatto per aiuto al sonno, concentrazione, meditazione, atmosfera e miglioramento musicale.

> ⚠️ **Nota:** al primo avvio la cattura dei log è attiva per impostazione predefinita (per raccogliere log di crash) e può essere disattivata in Impostazioni.

---

## ✨ Funzionalità

### 🌊 Rumore bianco
- Più sorgenti integrate organizzate per categoria, locali e online
- **Modalità Scatter**: riproduzione casuale con intervallo e range personalizzabili, simula un campo sonoro naturale
- **Catena di effetti indipendente** per traccia (EQ / reverb / spaziale / creativi), senza interferenze
- Range di guadagno volume 0–300 %

### 🎵 Riproduzione musicale
- Multi-formato (MP3 / WAV / FLAC / AAC, ecc.)
- Gestione playlist
- Streaming online tramite il **motore di script QuickJS**, rilevamento automatico multi-motore e failover automatico al timeout

### 🎛️ Effetti audio in tempo reale (motore C++)
| Effetto | Descrizione |
|------|------|
| **Equalizzatore (EQ)** | Filtri BiQuad multibanda manuali, curve indipendenti per traccia, con Bypass |
| **AutoEQ compensazione altoparlante** | 12 preset di dispositivi, 16 parametri regolabili, tutti i parametri di filtro (guadagno/frequenza/Q) editabili per banda e persistiti |
| **Audio spaziale** | Posizionamento 3D della sorgente, attenuazione di distanza, traiettoria surround (0,25–10 s/giro), dispersione casuale, offset fisso |
| **Reverb** | Dimensione stanza, tempo di decadimento, pre-delay, mix wet/dry, isolamento |
| **Limitatore** | Limitazione brick-wall, curva della funzione di trasferimento + meter VU + meter GR, visualizzazione in tempo reale |
| **Effetti creativi** | Lo-Fi / 8-bit / Sottomarino / Segnale alieno / Megafono / Distorsione |
| **Pseudo-ripristino** | Miglioramento HiFi (transiente soft-knee, blocco DC, smussamento del guadagno) |
| **Compressore multibanda** | MultibandCompressor |
| **Allargatore stereo / Basso virtuale / Isolamento** | StereoWidener / VirtualBass / Insulation |
| **Velocità & intonazione** | Con **SoundTouch**, velocità 0,1–5,0×, intonazione ±12 semitoni, regolazione indipendente |

### 🎛️ Pannello mixer
- Controllo effetti in un punto: intensità, EQ, AutoEQ, audio spaziale, reverb, limitatore, velocità/intonazione
- Anteprima in tempo reale, applicazione istantanea dei parametri
- Reset con un tocco

### ⏰ Timer
- Timer di spegnimento (fino a 23h 59m)
- Modalità snooze
- Tocca la palla del timer per avviare il conto

### 🐾 Animale fluttuante
- Animale fluttuante sul desktop, trascinabile / ridimensionabile / nascondibile
- Scalatura ancorata al centro, risveglio dallo stato nascosto

### 🎨 Personalizzazione
- **Temi personalizzati**: personalizzazione colore HSV + preset
- **Effetti vetro smerigliato / vetro liquido**
- Persistenza automatica dei parametri degli effetti

### 🔔 Sistema di notifiche
- **Notifiche in-app**: 5 tipi (Info/Successo/Avviso/Errore/Caricamento), livelli di priorità, swipe per chiudere, pulsanti azione
- **Notifiche multimediali di sistema**: integrazione MediaSession, controllo da schermata di blocco

### 🛡️ Stabilità e mantenimento
- **Mantenimento del flusso audio**: strategia AudioFocus, tolleranza alle disconnessioni e retry, ottimizzazione buffer, recupero graceful degli errori Oboe, fallback MediaPlayer
- **Guida al mantenimento in background**: whitelist ottimizzazione batteria + guida impostazioni autostart vendor
- **Diagnostica blocco memoria**: monitoraggio eccezioni (riavvio del motore audio / underrun buffer / blocco thread principale / avvisi memoria) + rapporti diagnostici
- **Sistema di log**: processo indipendente che cattura Logcat e log di crash

### 🌍 Multilingua
10 lingue: cinese, inglese, giapponese, coreano, russo, francese, tedesco, spagnolo, italiano, portoghese

---

## 🔊 Motore audio

### Pipeline di elaborazione

```
Decoder ──► Volume/Fade ──► Isolamento ──► Audio spaziale ──► Reverb
        ──► EQ / AutoEQ ──► Creativi ──► Pseudo-ripristino ──► Limitatore
        ──► SoundTouch(Velocità/Intonazione) ──► Clip duro ──► Output Oboe
```

### Struttura dei moduli C++

| Directory | Responsabilità |
|------|------|
| `cpp/AudioPlayFunc/` | `AudioEngine` (gestione flussi Oboe, mixing multipista), `AudioTrack` (decodifica e catena effetti per traccia) |
| `cpp/AudioEffect/` | 12 processori di effetti + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | Implementazione filtri BiQuad |
| `cpp/reverb/` | Algoritmo reverb |
| `cpp/spatial_audio/` | HRTF audio spaziale |
| `cpp/oboe/` | Libreria audio a bassa latenza Google Oboe |
| `cpp/ffmpeg/` | Decodifica FFmpeg (LGPL) |

### Interfaccia JNI
`JniInterface.cpp` espone tutte le capacità audio (controllo riproduzione, parametri effetti, curve EQ, audio spaziale, AutoEQ, limitatore, velocità/intonazione SoundTouch, ecc.); il lato Kotlin è incapsulato da `OboeAudioEngine`.

---

## 🏗️ Architettura

| Livello | Tecnologia |
|----|------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Audio | C++ + Oboe + SoundTouch + FFmpeg (JNI) |
| Rete | OkHttp |
| Script | QuickJS-kt (motore di script sorgenti online) |
| Archiviazione | Gson (config JSON) + EncryptedSharedPreferences |
| Media | AndroidX Media (MediaSession) |
| Altro | Lottie / Coil / Biometria / MTDataFilesProvider |

### Principali package Kotlin

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine, audio spaziale, reverb, gestione scatter
├── music/          # Controllo riproduzione musicale
├── onlinemusic/    # Streaming online e motore di script
├── storage/        # Archiviazione config/rumore bianco/musica/temi (JSON)
├── ui/             # UI Compose e componenti
├── servies/        # MusicService (servizio multimediale in primo piano)
├── service/        # MemoryLockService, LogCaptureService (processo :log)
├── floatingpet/    # Servizio animale fluttuante
├── timer/          # Timer
├── equalizer/      # Logica equalizzatore
└── ...             # data / security / utils / playlist ecc.
```

---

## 📥 Download e installazione

### Requisiti di sistema
- Android 7.0 (API 24) o superiore
- Almeno 100 MB di spazio libero

### Installazione
1. Scarica l'ultimo APK da [Releases](https://github.com/byxixiaoshao/AVIV/releases)
2. Attiva "Installa app da fonti sconosciute" sul dispositivo
3. Apri l'APK per installare
4. Avvia l'app

---

## 🔧 Compila dai sorgenti

### Requisiti
- Android Studio (che supporti la versione AGP attuale)
- JDK 11
- Android SDK, compileSdk 36
- NDK 27.0.12077973, CMake 4.3.0
- ABI supportate: `arm64-v8a`, `armeabi-v7a`

### Passaggi
```bash
git clone <url-del-repo>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> La compilazione richiede una config di firma `keystore.properties`. I sorgenti SoundTouch si trovano in `soundtouch/` nella root del progetto e CMake li compila automaticamente come libreria statica.

---

## 📖 Guida all'uso

### Rumore bianco
1. Nella schermata principale scegli categoria e sorgente
2. Tocca una scheda sorgente per avviare la riproduzione
3. Premi a lungo una scheda per configurare effetti indipendenti
4. Modalità Scatter: riproduci più sorgenti casualmente

### Musica
1. Scorri verso il basso la barra di riproduzione superiore per entrare nel player
2. Aggiungi musica tramite il selettore file
3. Regola gli effetti nel pannello mixer

### Regolazione effetti
Apri il **pannello mixer**:
- **Equalizzatore** → Equalizer Panel
- **Compensazione AutoEQ** → Speaker Compensation (editabile per banda)
- **Audio spaziale** → Spatial Audio Panel
- **Reverb** → Reverb Panel
- **Limitatore** → Limiter (con visualizzazione funzione di trasferimento)
- **Velocità/intonazione** → Regolazione velocità (SoundTouch)

---

## 📦 Dipendenze open source & licenze

| Dipendenza | Licenza | Uso |
|------|------|------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | Motore audio a bassa latenza |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | Regolazione indipendente velocità/intonazione |
| FFmpeg | LGPL v2.1+ | Decodifica audio |
| Jetpack Compose / AndroidX | Apache 2.0 | Framework UI |
| Material Components | Apache 2.0 | Componenti UI |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | Archiviazione preferenze |
| AndroidX Security | Apache 2.0 | Preferenze cifrate |
| AndroidX Biometric | Apache 2.0 | Autenticazione biometrica |
| Gson | Apache 2.0 | Serializzazione JSON |
| OkHttp | Apache 2.0 | Rete HTTP |
| Lottie | Apache 2.0 | Animazione |
| Coil | Apache 2.0 | Caricamento immagini |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | Motore di script sorgenti |
| Liquid Glass Android | Apache 2.0 | Effetti glassmorphism |
| MTDataFilesProvider | Apache 2.0 | Provider dati del media store |

> Le sorgenti sonore provengono da [Pixabay](https://pixabay.com/) sotto la Pixabay License.

---

## ⚠️ Licenza

Concesso in licenza sotto **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)**.

Vedi il file [LICENSE](../LICENSE) per il testo completo.

> Le librerie di terze parti mantengono le loro licenze originali (es. SoundTouch — LGPL v2.1, FFmpeg — LGPL v2.1+, QuickJS-kt — MIT).

---

## 📧 Contatti

- **Issues**: [GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email**: [3139105039@qq.com](mailto:3139105039@qq.com)
- **Autore**: byxixiaoshao / Bicy Studio

---

## 🙏 Ringraziamenti speciali

### Sorgente sonora
Tutti i suoni di rumore bianco provengono da **[Pixabay](https://pixabay.com/)**. Premi a lungo un'opzione sonora per vedere le informazioni sull'autore — supporta i creatori originali se possibile.

### Test software
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### Supporto artistico
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

Grazie a tutti i progetti open source che contribuiscono alle tecnologie di elaborazione audio.

---

<div align="center">

**⭐ Se questo progetto ti aiuta, lascia una Star ⭐**

Made with ❤️ by Bicy Studio

</div>
