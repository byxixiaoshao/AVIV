# AVIV — Player de ruído branco e música

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**Um player Android de ruído branco e música construído em torno de um motor de áudio em tempo real em C++, integrando uma dúzia de efeitos profissionais, áudio espacial e AutoEQ.**

[Recursos](#-recursos) · [Motor de áudio](#-motor-de-áudio) · [Arquitetura](#-arquitetura) · [Compilação](#-compilar-a-partir-do-código-fonte) · [Dependências e licenças](#-dependências-open-source--licenças)

[English](../README.md) · [中文](README_ZH.md) · [日本語](README_JA.md) · [한국어](README_KO.md) · [Русский](README_RU.md) · [Français](README_FR.md) · [Deutsch](README_DE.md) · [Español](README_ES.md) · [Italiano](README_IT.md) · **Português(este arquivo)**

</div>

---

## 📖 Sobre

O AVIV funde reprodução de ruído branco, música local/online e um motor profissional de processamento de áudio em tempo real. Todos os efeitos são calculados em tempo real na camada C++ sobre fluxos de áudio de baixa latência Oboe — sem pré-renderização, as mudanças de parâmetro são aplicadas instantaneamente. Adequado para auxílio ao sono, concentração, meditação, ambiente e aprimoramento musical.

> ⚠️ **Nota:** no primeiro início, a captura de logs está ativada por padrão (para coletar logs de crash) e pode ser desativada nas Configurações.

---

## ✨ Recursos

### 🌊 Ruído branco
- Múltiplas fontes integradas organizadas por categoria, suporta reprodução de música local
- **Modo Scatter**: reprodução aleatória com intervalo e intervalo personalizáveis, simulando um campo sonoro natural
- **Cadeia de efeitos independente** por faixa (EQ / reverb / espacial / criativos), sem interferência
- Faixa de ganho de volume 0–300 %

### 🎵 Reprodução de música
- Multi-formato (MP3 / WAV / FLAC / AAC, etc.)
- Gestão de playlists

### 🎛️ Efeitos de áudio em tempo real (motor C++)
| Efeito | Descrição |
|------|------|
| **Equalizador (EQ)** | Filtros BiQuad multibanda manuais, curvas independentes por faixa, com Bypass |
| **AutoEQ compensação de alto-falante** | 12 presets de dispositivos, 16 parâmetros ajustáveis, todos os parâmetros de filtro (ganho/frequência/Q) editáveis por banda e persistidos |
| **Áudio espacial** | Posicionamento 3D da fonte, atenuação por distância, trajetória surround (0,25–10 s/volta), dispersão aleatória, offset fixo |
| **Reverb** | Tamanho da sala, tempo de decaimento, pré-delay, mix wet/dry, isolamento |
| **Limitador** | Limitação brick-wall, curva da função de transferência + medidor VU + medidor GR, visualização em tempo real |
| **Efeitos criativos** | Lo-Fi / 8-bit / Subaquático / Sinal alien / Megafone / Distorção |
| **Pseudo-restauração** | Aprimoramento HiFi (transiente soft-knee, bloqueio DC, suavização de ganho) |
| **Compressor multibanda** | MultibandCompressor |
| **Amplificador estéreo / Baixo virtual / Isolamento** | StereoWidener / VirtualBass / Insulation |
| **Velocidade & tom** | Com **SoundTouch**, velocidade 0,1–5,0×, tom ±12 semitons, ajuste independente |

### 🎛️ Painel mixer
- Controle de efeitos num só ponto: intensidade, EQ, AutoEQ, áudio espacial, reverb, limitador, velocidade/tom
- Pré-visualização em tempo real, aplicação instantânea de parâmetros
- Reset com um toque

### ⏰ Temporizador
- Temporizador de sono (até 23h 59m)
- Modo soneca
- Toque na bola do temporizador para iniciar a contagem

### 🐾 Animal flutuante
- Animal flutuante na área de trabalho, arrastável / redimensionável / ocultável
- Escala ancorada ao centro, despertar do estado oculto

### 🎨 Personalização
- **Temas personalizados**: personalização de cor HSV + presets
- **Efeitos de vidro fosco / vidro líquido**
- Persistência automática dos parâmetros de efeito

### 🔔 Sistema de notificações
- **Notificações in-app**: 5 tipos (Info/Sucesso/Aviso/Erro/Carregamento), níveis de prioridade, deslizar para fechar, botões de ação
- **Notificações de mídia do sistema**: integração MediaSession, controle na tela de bloqueio

### 🛡️ Estabilidade e manutenção
- **Manutenção do fluxo de áudio**: estratégia AudioFocus, tolerância a desconexões e retry, otimização de buffer, recuperação graceful de erros Oboe, fallback MediaPlayer
- **Guia de manutenção em segundo plano**: lista branca de otimização de bateria + guia de configurações de autostart do fabricante
- **Diagnóstico de bloqueio de memória**: monitoramento de exceções (reinício do motor de áudio / underrun de buffer / travamento da thread principal / avisos de memória) + relatórios de diagnóstico
- **Sistema de logs**: processo independente capturando Logcat e logs de crash

### 🌍 Multilíngue
10 idiomas: chinês, inglês, japonês, coreano, russo, francês, alemão, espanhol, italiano, português

---

## 🔊 Motor de áudio

### Pipeline de processamento

```
Decodificador ──► Volume/Fade ──► Isolamento ──► Áudio espacial ──► Reverb
              ──► EQ / AutoEQ ──► Criativos ──► Pseudo-restauração ──► Limitador
              ──► SoundTouch(Velocidade/Tom) ──► Clip duro ──► Saída Oboe
```

### Estrutura dos módulos C++

| Diretório | Responsabilidade |
|------|------|
| `cpp/AudioPlayFunc/` | `AudioEngine` (gestão de fluxos Oboe, mixing multipista), `AudioTrack` (decodificação e cadeia de efeitos por faixa) |
| `cpp/AudioEffect/` | 12 processadores de efeitos + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | Implementação de filtros BiQuad |
| `cpp/reverb/` | Algoritmo de reverb |
| `cpp/spatial_audio/` | HRTF de áudio espacial |
| `cpp/oboe/` | Biblioteca de áudio de baixa latência Google Oboe |
| `cpp/ffmpeg/` | Decodificação FFmpeg (LGPL) |

### Interface JNI
`JniInterface.cpp` expõe todas as capacidades de áudio (controle de reprodução, parâmetros de efeitos, curvas EQ, áudio espacial, AutoEQ, limitador, velocidade/tom SoundTouch, etc.); o lado Kotlin é encapsulado por `OboeAudioEngine`.

---

## 🏗️ Arquitetura

| Camada | Tecnologia |
|----|------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Áudio | C++ + Oboe + SoundTouch + FFmpeg (JNI) |
| Rede | OkHttp |
| Script | QuickJS-kt (motor de scripts de fontes online) |
| Armazenamento | Gson (config JSON) + EncryptedSharedPreferences |
| Mídia | AndroidX Media (MediaSession) |
| Outros | Lottie / Coil / Biometria / MTDataFilesProvider |

### Principais pacotes Kotlin

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine, áudio espacial, reverb, gestão scatter
├── music/          # Controle de reprodução musical
├── onlinemusic/    # Streaming online e motor de scripts
├── storage/        # Armazenamento config/ruído branco/música/temas (JSON)
├── ui/             # UI Compose e componentes
├── servies/        # MusicService (serviço de mídia em primeiro plano)
├── service/        # MemoryLockService, LogCaptureService (processo :log)
├── floatingpet/    # Serviço de animal flutuante
├── timer/          # Temporizador
├── equalizer/      # Lógica do equalizador
└── ...             # data / security / utils / playlist etc.
```

---

## 📥 Download e instalação

### Requisitos do sistema
- Android 10.0 (API 29) ou superior
- Pelo menos 100 MB de armazenamento livre

### Instalação
1. Baixe o APK mais recente de [Releases](https://github.com/byxixiaoshao/AVIV/releases)
2. Ative "Instalar aplicativos de fontes desconhecidas" no dispositivo
3. Abra o APK para instalar
4. Inicie o app

---

## 🔧 Compilar a partir do código-fonte

### Requisitos
- Android Studio (que suporte a versão atual do AGP)
- JDK 11
- Android SDK, compileSdk 36
- NDK 27.0.12077973, CMake 4.3.0
- ABIs suportadas: `arm64-v8a`, `armeabi-v7a`

### Passos
```bash
git clone <url-do-repo>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> A compilação requer uma config de assinatura `keystore.properties`. As fontes do SoundTouch devem ser obtidas separadamente.

---

## 📖 Guia de uso

### Ruído branco
1. Na tela principal escolha categoria e fonte
2. Toque num cartão de fonte para iniciar a reprodução
3. Pressione e segure um cartão para configurar efeitos independentes
4. Modo Scatter: reproduza várias fontes aleatoriamente

### Música
1. Deslize para baixo a barra de reprodução superior para entrar no player
2. Adicione música via seletor de arquivos
3. Ajuste efeitos no painel mixer

### Ajuste de efeitos
Abra o **painel mixer**:
- **Equalizador** → Equalizer Panel
- **Compensação AutoEQ** → Speaker Compensation (editável por banda)
- **Áudio espacial** → Spatial Audio Panel
- **Reverb** → Reverb Panel
- **Limitador** → Limiter (com visualização da função de transferência)
- **Velocidade/tom** → Ajuste de velocidade (SoundTouch)

---

## 📦 Dependências open source & licenças

| Dependência | Licença | Uso |
|------|------|------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | Motor de áudio de baixa latência |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | Ajuste independente velocidade/tom |
| FFmpeg | LGPL v2.1+ | Decodificação de áudio |
| Jetpack Compose / AndroidX | Apache 2.0 | Framework UI |
| Material Components | Apache 2.0 | Componentes UI |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | Armazenamento de preferências |
| AndroidX Security | Apache 2.0 | Preferências criptografadas |
| AndroidX Biometric | Apache 2.0 | Autenticação biométrica |
| Gson | Apache 2.0 | Serialização JSON |
| OkHttp | Apache 2.0 | Rede HTTP |
| Lottie | Apache 2.0 | Animação |
| Coil | Apache 2.0 | Carregamento de imagens |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | Motor de scripts de fontes |
| Liquid Glass Android | Apache 2.0 | Efeitos glassmorphism |
| MTDataFilesProvider | Apache 2.0 | Provedor de dados da media store |

> As fontes sonoras vêm de [Pixabay](https://pixabay.com/) sob a Pixabay License.

---

## ⚠️ Licença

Licenciado sob a **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)**.

Veja o arquivo [LICENSE](../LICENSE) para o texto completo.

> As bibliotecas de terceiros mantêm suas licenças originais (ex. SoundTouch — LGPL v2.1, FFmpeg — LGPL v2.1+, QuickJS-kt — MIT).

---

## 📧 Contato

- **Issues**: [GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email**: [3139105039@qq.com](mailto:3139105039@qq.com)
- **Autor**: byxixiaoshao / Bicy Studio

---

## 🙏 Agradecimentos especiais

### Fonte sonora
Todos os sons de ruído branco vêm de **[Pixabay](https://pixabay.com/)**. Pressione e segure uma opção de som para ver as informações do autor — apoie os criadores originais se possível.

### Testes de software
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### Suporte artístico
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

Obrigado a todos os projetos open source que contribuem para as tecnologias de processamento de áudio.

---

<div align="center">

**⭐ Se este projeto te ajuda, deixe uma Star ⭐**

Made with ❤️ by Bicy Studio

</div>
