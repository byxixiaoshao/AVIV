# AVIV — Lecteur de bruit blanc & musique

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**Un lecteur Android de bruit blanc et de musique construit autour d'un moteur audio C++ en temps réel, intégrant une dizaine d'effets professionnels, l'audio spatial, AutoEQ et le streaming en ligne.**

[Fonctionnalités](#-fonctionnalités) · [Moteur audio](#-moteur-audio) · [Architecture](#-architecture) · [Compilation](#-compiler-depuis-les-sources) · [Dépendances & licences](#-dépendances-open-source--licences)

[English](../README.md) · [中文](README_ZH.md) · [日本語](README_JA.md) · [한국어](README_KO.md) · [Русский](README_RU.md) · **Français(ce fichier)** · [Deutsch](README_DE.md) · [Español](README_ES.md) · [Italiano](README_IT.md) · [Português](README_PT.md)

</div>

---

## 📖 À propos

AVIV fusionne lecture de bruit blanc, musique locale/en ligne et un moteur professionnel de traitement audio en temps réel. Chaque effet est calculé en temps réel dans la couche C++ sur les flux audio à faible latence Oboe — sans pré-rendu, les réglages sont appliqués instantanément. Idéal pour l'aide au sommeil, la concentration, la méditation, l'ambiance et l'amélioration musicale.

> ⚠️ **Note :** au premier lancement, la capture de logs est activée par défaut (pour collecter les logs de crash) et peut être désactivée dans les Paramètres.

---

## ✨ Fonctionnalités

### 🌊 Bruit blanc
- Plusieurs sources intégrées organisées par catégorie, locales et en ligne
- **Mode Scatter** : lecture aléatoire avec intervalle et plage personnalisables, simulant un champ sonore naturel
- **Chaîne d'effets indépendante** par piste (EQ / réverb / spatial / créatifs), sans interférence
- Plage de gain de volume 0–300 %

### 🎵 Lecture musicale
- Multi-format (MP3 / WAV / FLAC / AAC, etc.)
- Gestion des listes de lecture
- Streaming en ligne via le **moteur de scripts QuickJS**, détection automatique multi-moteurs et basculement automatique en cas de timeout

### 🎛️ Effets audio en temps réel (moteur C++)
| Effet | Description |
|------|------|
| **Égaliseur (EQ)** | Filtres BiQuad multibandes manuels, courbes indépendantes par piste, avec Bypass |
| **AutoEQ compensation haut-parleur** | 12 presets d'appareils, 16 paramètres réglables, tous les paramètres de filtre (gain/fréquence/Q) éditables par bande et persistés |
| **Audio spatial** | Positionnement 3D de la source, atténuation de distance, trajectoire surround (0,25–10 s/tr), dispersion aléatoire, offset fixe |
| **Réverb** | Taille de salle, temps de décroissance, pré-délai, mix wet/dry, isolation |
| **Limiteur** | Limitation brick-wall, courbe de fonction de transfert + VU-mètre + mètre GR, visualisation en temps réel |
| **Effets créatifs** | Lo-Fi / 8-bit / Sous-marin / Signal alien / Mégaphone / Distorsion |
| **Pseudo-restauration** | Amélioration HiFi (transitoire soft-knee, blocage DC, lissage du gain) |
| **Compresseur multibande** | MultibandCompressor |
| **Élargisseur stéréo / Basse virtuelle / Isolation** | StereoWidener / VirtualBass / Insulation |
| **Vitesse & hauteur** | Propulsé par **SoundTouch**, vitesse 0,1–5,0×, hauteur ±12 demi-tons, réglage indépendant |

### 🎛️ Panneau de mixage
- Contrôle des effets en un point : intensité, EQ, AutoEQ, audio spatial, réverb, limiteur, vitesse/hauteur
- Aperçu en temps réel, application instantanée des paramètres
- Réinitialisation en une touche

### ⏰ Minuteur
- Minuteur de sommeil (jusqu'à 23h 59m)
- Mode snooze
- Touchez la bille du minuteur pour démarrer

### 🐾 Animal flottant
- Animal flottant sur le bureau, déplaçable / redimensionnable / masquable
- Mise à l'échelle ancrée au centre, réveil depuis l'état masqué

### 🎨 Personnalisation
- **Thèmes personnalisés** : personnalisation couleur HSV + presets
- **Effets verre dépoli / verre liquide**
- Persistance automatique des paramètres d'effet

### 🔔 Système de notifications
- **Notifications in-app** : 5 types (Info/Succès/Avertissement/Erreur/Chargement), niveaux de priorité, balayage pour fermer, boutons d'action
- **Notifications média système** : intégration MediaSession, contrôle écran verrouillé

### 🛡️ Stabilité & maintien
- **Maintien du flux audio** : stratégie AudioFocus, tolérance aux déconnexions et retry, optimisation du buffer, récupération gracieuse des erreurs Oboe, repli MediaPlayer
- **Guide de maintien en arrière-plan** : liste blanche d'optimisation batterie + guide des paramètres de démarrage auto constructeur
- **Diagnostics de verrouillage mémoire** : surveillance d'exceptions (redémarrage du moteur audio / under-run buffer / blocage thread principal / avertissements mémoire) + rapports de diagnostic
- **Système de logs** : processus indépendant capturant Logcat & logs de crash

### 🌍 Multilingue
10 langues : chinois, anglais, japonais, coréen, russe, français, allemand, espagnol, italien, portugais

---

## 🔊 Moteur audio

### Pipeline de traitement

```
Décodeur ──► Volume/Fondu ──► Isolation ──► Audio spatial ──► Réverb
         ──► EQ / AutoEQ ──► Créatifs ──► Pseudo-restauration ──► Limiteur
         ──► SoundTouch(Vitesse/Hauteur) ──► Clip dur ──► Sortie Oboe
```

### Structure des modules C++

| Répertoire | Responsabilité |
|------|------|
| `cpp/AudioPlayFunc/` | `AudioEngine` (gestion flux Oboe, mixage multipiste), `AudioTrack` (décodage et chaîne d'effets par piste) |
| `cpp/AudioEffect/` | 12 processeurs d'effets + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | Implémentation des filtres BiQuad |
| `cpp/reverb/` | Algorithme de réverb |
| `cpp/spatial_audio/` | HRTF audio spatial |
| `cpp/oboe/` | Bibliothèque audio faible latence Google Oboe |
| `cpp/ffmpeg/` | Décodage FFmpeg (LGPL) |

### Interface JNI
`JniInterface.cpp` expose toutes les capacités audio (contrôle de lecture, paramètres d'effets, courbes EQ, audio spatial, AutoEQ, limiteur, vitesse/hauteur SoundTouch, etc.) ; côté Kotlin, encapsulé par `OboeAudioEngine`.

---

## 🏗️ Architecture

| Couche | Technologie |
|----|------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Audio | C++ + Oboe + SoundTouch + FFmpeg (JNI) |
| Réseau | OkHttp |
| Script | QuickJS-kt (moteur de scripts sources en ligne) |
| Stockage | Gson (config JSON) + EncryptedSharedPreferences |
| Média | AndroidX Media (MediaSession) |
| Autre | Lottie / Coil / Biométrie / MTDataFilesProvider |

### Principaux packages Kotlin

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine, audio spatial, réverb, gestion scatter
├── music/          # Contrôle de lecture musicale
├── onlinemusic/    # Streaming en ligne & moteur de scripts sources
├── storage/        # Stockage config/bruit blanc/musique/thèmes (JSON)
├── ui/             # UI Compose & composants
├── servies/        # MusicService (service média premier plan)
├── service/        # MemoryLockService, LogCaptureService (processus :log)
├── floatingpet/    # Service animal flottant
├── timer/          # Minuteur
├── equalizer/      # Logique égaliseur
└── ...             # data / security / utils / playlist etc.
```

---

## 📥 Téléchargement & installation

### Configuration requise
- Android 7.0 (API 24) ou supérieur
- Au moins 100 Mo de stockage libre

### Installation
1. Téléchargez le dernier APK depuis [Releases](https://github.com/byxixiaoshao/AVIV/releases)
2. Activez « Installer des applications inconnues » sur votre appareil
3. Ouvrez l'APK pour installer
4. Lancez l'application

---

## 🔧 Compiler depuis les sources

### Prérequis
- Android Studio (supportant la version AGP actuelle)
- JDK 11
- Android SDK, compileSdk 36
- NDK 27.0.12077973, CMake 4.3.0
- ABI supportés : `arm64-v8a`, `armeabi-v7a`

### Étapes
```bash
git clone <url-du-dépôt>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> La compilation nécessite une config de signature `keystore.properties`. Les sources SoundTouch sont dans `soundtouch/` à la racine du projet, compilées en bibliothèque statique par CMake automatiquement.

---

## 📖 Guide d'utilisation

### Bruit blanc
1. Sur l'écran principal, choisissez une catégorie et une source
2. Touchez une carte source pour lancer la lecture
3. Appui long sur une carte pour configurer ses effets indépendants
4. Mode Scatter : lecture aléatoire de plusieurs sources

### Musique
1. Glissez vers le bas la barre de lecture supérieure pour entrer dans le lecteur
2. Ajoutez de la musique via le sélecteur de fichiers
3. Ajustez les effets dans le panneau de mixage

### Réglage des effets
Ouvrez le **panneau de mixage** :
- **Égaliseur** → Equalizer Panel
- **Compensation AutoEQ** → Speaker Compensation (édition par bande)
- **Audio spatial** → Spatial Audio Panel
- **Réverb** → Reverb Panel
- **Limiteur** → Limiter (avec visualisation de la fonction de transfert)
- **Vitesse/hauteur** → Réglage de vitesse (SoundTouch)

---

## 📦 Dépendances open source & licences

| Dépendance | Licence | Usage |
|------|------|------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | Moteur audio faible latence |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | Réglage indépendant vitesse/hauteur |
| FFmpeg | LGPL v2.1+ | Décodage audio |
| Jetpack Compose / AndroidX | Apache 2.0 | Framework UI |
| Material Components | Apache 2.0 | Composants UI |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | Stockage préférences |
| AndroidX Security | Apache 2.0 | Préférences chiffrées |
| AndroidX Biometric | Apache 2.0 | Authentification biométrique |
| Gson | Apache 2.0 | Sérialisation JSON |
| OkHttp | Apache 2.0 | Réseau HTTP |
| Lottie | Apache 2.0 | Animation |
| Coil | Apache 2.0 | Chargement d'images |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | Moteur de scripts sources |
| Liquid Glass Android | Apache 2.0 | Effets glassmorphism |
| MTDataFilesProvider | Apache 2.0 | Fournisseur de données média |

> Les sources sonores proviennent de [Pixabay](https://pixabay.com/) sous licence Pixabay License.

---

## ⚠️ Licence

Sous licence **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)**.

Voir le fichier [LICENSE](../LICENSE) pour le texte complet.

> Les bibliothèques tierces conservent leurs licences d'origine (ex. SoundTouch — LGPL v2.1, FFmpeg — LGPL v2.1+, QuickJS-kt — MIT).

---

## 📧 Contact

- **Issues** : [GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email** : [3139105039@qq.com](mailto:3139105039@qq.com)
- **Auteur** : byxixiaoshao / Bicy Studio

---

## 🙏 Remerciements spéciaux

### Source sonore
Tous les sons de bruit blanc proviennent de **[Pixabay](https://pixabay.com/)**. Un appui long sur une option sonore affiche les informations de l'auteur — soutenez les créateurs originaux si possible.

### Tests logiciels
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### Support artistique
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

Merci à tous les projets open source qui contribuent aux technologies de traitement audio.

---

<div align="center">

**⭐ Si ce projet vous aide, merci de mettre une Star ⭐**

Made with ❤️ by Bicy Studio

</div>
