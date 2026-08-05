# AVIV — Reproductor de ruido blanco y música

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**Un reproductor Android de ruido blanco y música construido en torno a un motor de audio en tiempo real en C++, que integra una docena de efectos profesionales, audio espacial y AutoEQ.**

[Características](#-características) · [Motor de audio](#-motor-de-audio) · [Arquitectura](#-arquitectura) · [Compilación](#-compilar-desde-el-código-fuente) · [Dependencias y licencias](#-dependencias-open-source--licencias)

[English](../README.md) · [中文](README_ZH.md) · [日本語](README_JA.md) · [한국어](README_KO.md) · [Русский](README_RU.md) · [Français](README_FR.md) · [Deutsch](README_DE.md) · **Español(este archivo)** · [Italiano](README_IT.md) · [Português](README_PT.md)

</div>

---

## 📖 Acerca de

AVIV fusiona reproducción de ruido blanco, música local/en línea y un motor profesional de procesamiento de audio en tiempo real. Todos los efectos se calculan en tiempo real en la capa C++ sobre flujos de audio de baja latencia Oboe — sin pre-renderizado, los cambios de parámetros se aplican al instante. Adecuado para ayuda al sueño, concentración, meditación, ambiente y mejora musical.

> ⚠️ **Nota:** en el primer inicio, la captura de registros está activada por defecto (para recopilar registros de fallos) y puede desactivarse en Ajustes.

---

## ✨ Características

### 🌊 Ruido blanco
- Múltiples fuentes integradas organizadas por categoría, admite reproducción de música local
- **Modo Scatter**: reproducción aleatoria con intervalo y rango personalizables, simulando un campo sonoro natural
- **Cadena de efectos independiente** por pista (EQ / reverb / espacial / creativos), sin interferencias
- Rango de ganancia de volumen 0–300 %

### 🎵 Reproducción de música
- Multi-formato (MP3 / WAV / FLAC / AAC, etc.)
- Gestión de listas de reproducción

### 🎛️ Efectos de audio en tiempo real (motor C++)
| Efecto | Descripción |
|------|------|
| **Ecualizador (EQ)** | Filtros BiQuad multibanda manuales, curvas independientes por pista, con Bypass |
| **AutoEQ compensación de altavoz** | 12 presets de dispositivos, 16 parámetros ajustables, todos los parámetros de filtro (ganancia/frecuencia/Q) editables por banda y persistidos |
| **Audio espacial** | Posicionamiento 3D de fuente, atenuación por distancia, trayectoria envolvente (0,25–10 s/vuelta), dispersión aleatoria, offset fijo |
| **Reverb** | Tamaño de sala, tiempo de decaimiento, pre-delay, mix wet/dry, aislamiento |
| **Limitador** | Limitación brick-wall, curva de función de transferencia + medidor VU + medidor GR, visualización en tiempo real |
| **Efectos creativos** | Lo-Fi / 8-bit / Submarino / Señal alien / Megáfono / Distorsión |
| **Pseudo-restauración** | Mejora HiFi (transiente soft-knee, bloqueo DC, suavizado de ganancia) |
| **Compresor multibanda** | MultibandCompressor |
| **Ensanchador estéreo / Bajo virtual / Aislamiento** | StereoWidener / VirtualBass / Insulation |
| **Velocidad & tono** | Con **SoundTouch**, velocidad 0,1–5,0×, tono ±12 semitonos, ajuste independiente |

### 🎛️ Panel mezclador
- Control de efectos en un punto: intensidad, EQ, AutoEQ, audio espacial, reverb, limitador, velocidad/tono
- Vista previa en tiempo real, aplicación instantánea de parámetros
- Reinicio con un toque

### ⏰ Temporizador
- Temporizador de sueño (hasta 23h 59m)
- Modo pospuesta
- Toca la bola del temporizador para iniciar la cuenta

### 🐾 Mascota flotante
- Mascota flotante en el escritorio, arrastrable / redimensionable / ocultable
- Escalado anclado al centro, despertar desde estado oculto

### 🎨 Personalización
- **Temas personalizados**: personalización de color HSV + presets
- **Efectos de cristal esmerilado / cristal líquido**
- Persistencia automática de parámetros de efectos

### 🔔 Sistema de notificaciones
- **Notificaciones in-app**: 5 tipos (Info/Éxito/Aviso/Error/Carga), niveles de prioridad, deslizar para cerrar, botones de acción
- **Notificaciones multimedia del sistema**: integración MediaSession, control en pantalla de bloqueo

### 🛡️ Estabilidad y mantenimiento
- **Mantenimiento de flujo de audio**: estrategia AudioFocus, tolerancia a desconexiones y reintento, optimización de buffer, recuperación graceful de errores Oboe, respaldo MediaPlayer
- **Guía de mantenimiento en segundo plano**: lista blanca de optimización de batería + guía de ajustes de auto-inicio del fabricante
- **Diagnóstico de bloqueo de memoria**: monitorización de excepciones (reinicio del motor de audio / underrun de buffer / bloqueo de hilo principal / avisos de memoria) + informes de diagnóstico
- **Sistema de registros**: proceso independiente capturando Logcat y registros de fallos

### 🌍 Multiidioma
10 idiomas: chino, inglés, japonés, coreano, ruso, francés, alemán, español, italiano, portugués

---

## 🔊 Motor de audio

### Pipeline de procesamiento

```
Decodificador ──► Volumen/Fade ──► Aislamiento ──► Audio espacial ──► Reverb
              ──► EQ / AutoEQ ──► Creativos ──► Pseudo-restauración ──► Limitador
              ──► SoundTouch(Velocidad/Tono) ──► Clip duro ──► Salida Oboe
```

### Estructura de módulos C++

| Directorio | Responsabilidad |
|------|------|
| `cpp/AudioPlayFunc/` | `AudioEngine` (gestión de flujos Oboe, mezcla multipista), `AudioTrack` (decodificación y cadena de efectos por pista) |
| `cpp/AudioEffect/` | 12 procesadores de efectos + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | Implementación de filtros BiQuad |
| `cpp/reverb/` | Algoritmo de reverb |
| `cpp/spatial_audio/` | HRTF de audio espacial |
| `cpp/oboe/` | Biblioteca de audio de baja latencia Google Oboe |
| `cpp/ffmpeg/` | Decodificación FFmpeg (LGPL) |

### Interfaz JNI
`JniInterface.cpp` expone todas las capacidades de audio (control de reproducción, parámetros de efectos, curvas EQ, audio espacial, AutoEQ, limitador, velocidad/tono SoundTouch, etc.); el lado Kotlin lo envuelve `OboeAudioEngine`.

---

## 🏗️ Arquitectura

| Capa | Tecnología |
|----|------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Audio | C++ + Oboe + SoundTouch + FFmpeg (JNI) |
| Red | OkHttp |
| Script | QuickJS-kt (motor de scripts de fuentes en línea) |
| Almacenamiento | Gson (config JSON) + EncryptedSharedPreferences |
| Multimedia | AndroidX Media (MediaSession) |
| Otros | Lottie / Coil / Biometría / MTDataFilesProvider |

### Principales paquetes Kotlin

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine, audio espacial, reverb, gestión scatter
├── music/          # Control de reproducción musical
├── onlinemusic/    # Streaming en línea y motor de scripts
├── storage/        # Almacenamiento config/ruido blanco/música/temas (JSON)
├── ui/             # UI Compose y componentes
├── servies/        # MusicService (servicio multimedia en primer plano)
├── service/        # MemoryLockService, LogCaptureService (proceso :log)
├── floatingpet/    # Servicio mascota flotante
├── timer/          # Temporizador
├── equalizer/      # Lógica del ecualizador
└── ...             # data / security / utils / playlist etc.
```

---

## 📥 Descarga e instalación

### Requisitos del sistema
- Android 10.0 (API 29) o superior
- Al menos 100 MB de almacenamiento libre

### Instalación
1. Descarga el último APK desde [Releases](https://github.com/byxixiaoshao/AVIV/releases)
2. Activa "Instalar aplicaciones desconocidas" en tu dispositivo
3. Abre el APK para instalar
4. Inicia la app

---

## 🔧 Compilar desde el código fuente

### Requisitos
- Android Studio (que soporte la versión actual de AGP)
- JDK 11
- Android SDK, compileSdk 36
- NDK 27.0.12077973, CMake 4.3.0
- ABI soportadas: `arm64-v8a`, `armeabi-v7a`

### Pasos
```bash
git clone <url-del-repo>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> La compilación requiere una configuración de firma `keystore.properties`. Las fuentes de SoundTouch deben obtenerse por separado.

---

## 📖 Guía de uso

### Ruido blanco
1. En la pantalla principal elige categoría y fuente
2. Toca una tarjeta de fuente para iniciar la reproducción
3. Mantén pulsada una tarjeta para configurar efectos independientes
4. Modo Scatter: reproduce varias fuentes aleatoriamente

### Música
1. Desliza hacia abajo la barra de reproducción superior para entrar al reproductor
2. Añade música vía selector de archivos
3. Ajusta efectos en el panel mezclador

### Ajuste de efectos
Abre el **panel mezclador**:
- **Ecualizador** → Equalizer Panel
- **Compensación AutoEQ** → Speaker Compensation (editable por banda)
- **Audio espacial** → Spatial Audio Panel
- **Reverb** → Reverb Panel
- **Limitador** → Limiter (con visualización de función de transferencia)
- **Velocidad/tono** → Ajuste de velocidad (SoundTouch)

---

## 📦 Dependencias open source & licencias

| Dependencia | Licencia | Uso |
|------|------|------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | Motor de audio de baja latencia |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | Ajuste independiente velocidad/tono |
| FFmpeg | LGPL v2.1+ | Decodificación de audio |
| Jetpack Compose / AndroidX | Apache 2.0 | Framework UI |
| Material Components | Apache 2.0 | Componentes UI |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | Almacenamiento de preferencias |
| AndroidX Security | Apache 2.0 | Preferencias cifradas |
| AndroidX Biometric | Apache 2.0 | Autenticación biométrica |
| Gson | Apache 2.0 | Serialización JSON |
| OkHttp | Apache 2.0 | Red HTTP |
| Lottie | Apache 2.0 | Animación |
| Coil | Apache 2.0 | Carga de imágenes |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | Motor de scripts de fuentes |
| Liquid Glass Android | Apache 2.0 | Efectos glassmorphism |
| MTDataFilesProvider | Apache 2.0 | Proveedor de datos del almacén multimedia |

> Las fuentes de sonido provienen de [Pixabay](https://pixabay.com/) bajo la Pixabay License.

---

## ⚠️ Licencia

Licenciado bajo **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)**.

Consulta el archivo [LICENSE](../LICENSE) para el texto completo.

> Las bibliotecas de terceros conservan sus licencias originales (p. ej. SoundTouch — LGPL v2.1, FFmpeg — LGPL v2.1+, QuickJS-kt — MIT).

---

## 📧 Contacto

- **Issues**: [GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email**: [3139105039@qq.com](mailto:3139105039@qq.com)
- **Autor**: byxixiaoshao / Bicy Studio

---

## 🙏 Agradecimientos especiales

### Fuente de sonido
Todos los sonidos de ruido blanco provienen de **[Pixabay](https://pixabay.com/)**. Mantén pulsada una opción de sonido para ver la información del autor — apoya a los creadores originales si puedes.

### Pruebas de software
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### Soporte artístico
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

Gracias a todos los proyectos open source que contribuyen a las tecnologías de procesamiento de audio.

---

<div align="center">

**⭐ Si este proyecto te ayuda, dale una Star ⭐**

Made with ❤️ by Bicy Studio

</div>
