# AVIV — 화이트노이즈 & 음악 플레이어

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**실시간 C++ 오디오 엔진을 중심으로 구축된 Android 화이트노이즈 및 음악 플레이어. 10여 종의 전문 이펙트, 공간 오디오, AutoEQ, 온라인 스트리밍을 통합.**

[기능](#-기능-특징) · [오디오 엔진](#-오디오-엔진) · [아키텍처](#-기술-아키텍처) · [빌드](#-소스에서-빌드) · [오픈소스 의존성 및 라이선스](#-오픈소스-의존성-및-라이선스)

[English](../README.md) · [中文](README_ZH.md) · [日本語](README_JA.md) · **한국어(본 파일)** · [Русский](README_RU.md) · [Français](README_FR.md) · [Deutsch](README_DE.md) · [Español](README_ES.md) · [Italiano](README_IT.md) · [Português](README_PT.md)

</div>

---

## 📖 소개

AVIV은 화이트노이즈 재생, 로컬/온라인 음악, 전문급 실시간 오디오 처리 엔진을 하나로 융합한 앱입니다. 모든 이펙트는 Oboe 저지연 오디오 스트림 위에서 C++ 레이어를 통해 실시간으로 계산되며, 사전 렌더링 없이 파라미터 변경이 즉시 반영됩니다. 수면 보조, 집중, 명상, 분위기 조성, 음악 강화 등에 적합합니다.

> ⚠️ **참고**: 첫 실행 시 로그 캡처가 기본적으로 활성화되어 있으며(크래시 로그 수집용), 설정에서 끌 수 있습니다.

---

## ✨ 기능 특징

### 🌊 화이트노이즈
- 카테고리별로 정리된 다수의 내장 음원, 로컬 및 온라인 지원
- **스캐터 모드**: 사용자 정의 간격과 범위로 무작위 재생, 자연스러운 음장 시뮬레이션
- 트랙별 **독립 이펙트 체인**(EQ / 리버브 / 공간 / 크리에이티브), 상호 간섭 없음
- 볼륨 게인 범위 0–300%

### 🎵 음악 재생
- 멀티 포맷 지원(MP3 / WAV / FLAC / AAC 등)
- 재생목록 관리
- **QuickJS 음원 스크립트 엔진** 기반 온라인 스트리밍, 멀티 엔진 자동 탐지 및 타임아웃 시 자동 페일오버

### 🎛️ 실시간 오디오 이펙트(C++ 엔진)
| 이펙트 | 설명 |
|------|------|
| **이퀄라이저(EQ)** | 수동 멀티밴드 BiQuad 필터, 트랙별 독립 커브, Bypass 지원 |
| **AutoEQ 스피커 보정** | 12가지 기기 프리셋, 16개 조정 가능 파라미터, 모든 필터 파라미터(게인/주파수/Q)를 밴드별로 편집 및 저장 |
| **공간 오디오** | 3D 음원 위치, 거리 감쇠, 서라운드 궤도(0.25–10초/회), 무작위 분산, 고정 오프셋 |
| **리버브** | 룸 크기, 감쇠 시간, 프리딜레이, 웻/드라이 믹스, 차음 |
| **리미터** | 브릭월 제한, 전달 함수 곡선 + VU 미터 + GR 미터 실시간 시각화 |
| **크리에이티브 이펙트** | Lo-Fi / 8-bit / 수중 / 외계 신호 / 메가폰 / 디스토션 |
| **유사 복원 처리** | HiFi 음질 강화(소프트니 트랜지언트, DC 블록, 게인 스무딩) |
| **멀티밴드 컴프레서** | MultibandCompressor |
| **스테레오 와이드너 / 가상 베이스 / 차음** | StereoWidener / VirtualBass / Insulation |
| **속도 & 피치** | **SoundTouch** 기반, 속도 0.1–5.0×, 피치 ±12 반음, 독립 조정 |

### 🎛️ 믹서 패널
- 원스톱 이펙트 제어: 이펙트 강도, EQ, AutoEQ, 공간 오디오, 리버브, 리미터, 속도/피치
- 실시간 미리보기, 파라미터 즉시 적용
- 원탭 리셋

### ⏰ 타이머
- 슬립 타이머(최대 23시간 59분)
- 스누즈 모드
- 타이머 공을 탭하여 측정 시작

### 🐾 플로팅 펫
- 데스크톱 플로팅 펫, 드래그 / 리사이즈 / 숨기기 가능
- 중심 앵커 스케일링, 숨김 상태에서 복귀

### 🎨 개인화
- **커스텀 테마**: HSV 색상 커스터마이즈 + 프리셋 테마
- **프로스티드 글래스 / 리퀴드 글래스** 효과
- 이펙트 파라미터 자동 영속화

### 🔔 알림 시스템
- **인앱 알림**: 5종(정보/성공/경고/오류/로딩), 우선순위 레벨, 스와이프 해제, 액션 버튼
- **시스템 미디어 알림**: MediaSession 연동, 잠금 화면 제어

### 🛡️ 안정성 & 유지
- **오디오 스트림 유지**: AudioFocus 전략, 스트림 단결 내결함성 재시도, 버퍼 최적화, Oboe 오류 우아한 복구, MediaPlayer 폴백
- **백그라운드 유지 가이드**: 배터리 최적화 화이트리스트 + 벤더 자동 시작 설정 가이드
- **메모리 잠금 진단**: 예외 모니터링(오디오 엔진 재시작 / 버퍼 언더런 / 메인 스레드 지연 / 메모리 경고) + 진단 보고서
- **로그 시스템**: 독립 프로세스에서 Logcat & 크래시 로그 캡처

### 🌍 다국어
10개 언어 지원: 중국어, 영어, 일본어, 한국어, 러시아어, 프랑스어, 독일어, 스페인어, 이탈리아어, 포르투갈어

---

## 🔊 오디오 엔진

### 처리 파이프라인

```
디코더 ──► 볼륨/페이드 ──► 차음 ──► 공간 오디오 ──► 리버브
       ──► EQ / AutoEQ ──► 크리에이티브 ──► 유사 복원 ──► 리미터
       ──► SoundTouch(속도/피치) ──► 하드 클립 ──► Oboe 출력
```

### C++ 모듈 구조

| 디렉터리 | 책임 |
|------|------|
| `cpp/AudioPlayFunc/` | `AudioEngine`(Oboe 스트림 관리, 멀티트랙 믹싱), `AudioTrack`(트랙별 디코딩 & 이펙트 체인) |
| `cpp/AudioEffect/` | 12개 이펙트 프로세서 + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | BiQuad 필터 구현 |
| `cpp/reverb/` | 리버브 알고리즘 |
| `cpp/spatial_audio/` | 공간 오디오 HRTF |
| `cpp/oboe/` | Google Oboe 저지연 오디오 라이브러리 |
| `cpp/ffmpeg/` | FFmpeg 디코딩(LGPL) |

### JNI 인터페이스
`JniInterface.cpp`가 모든 오디오 기능(재생 제어, 이펙트 파라미터, EQ 커브, 공간 오디오, AutoEQ, 리미터, SoundTouch 속도/피치 등)을 노출하며, Kotlin 측은 `OboeAudioEngine`이 래핑.

---

## 🏗️ 기술 아키텍처

| 레이어 | 기술 |
|----|------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| 오디오 | C++ + Oboe + SoundTouch + FFmpeg(JNI) |
| 네트워크 | OkHttp |
| 스크립트 | QuickJS-kt(온라인 음원 스크립트 엔진) |
| 저장 | Gson(JSON 설정) + EncryptedSharedPreferences |
| 미디어 | AndroidX Media(MediaSession) |
| 기타 | Lottie / Coil / 생체 인식 / MTDataFilesProvider |

### 주요 Kotlin 패키지

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine, 공간 오디오, 리버브, 스캐터 재생 관리
├── music/          # 음악 재생 제어
├── onlinemusic/    # 온라인 스트리밍 & 음원 스크립트 엔진
├── storage/        # 설정/화이트노이즈/음악/테마 저장(JSON)
├── ui/             # Compose UI & 컴포넌트
├── servies/        # MusicService(포그라운드 미디어 서비스)
├── service/        # MemoryLockService, LogCaptureService(:log 프로세스)
├── floatingpet/    # 플로팅 펫 서비스
├── timer/          # 타이머
├── equalizer/      # 이퀄라이저 로직
└── ...             # data / security / utils / playlist 등
```

---

## 📥 다운로드 & 설치

### 시스템 요구사항
- Android 7.0(API 24) 이상
- 최소 100MB 사용 가능 저장공간

### 설치
1. [Releases](https://github.com/byxixiaoshao/AVIV/releases)에서 최신 APK 다운로드
2. 기기에서 "알 수 없는 앱 설치" 활성화
3. APK을 열어 설치
4. 앱 실행

---

## 🔧 소스에서 빌드

### 요구사항
- Android Studio(현재 AGP 버전 지원)
- JDK 11
- Android SDK, compileSdk 36
- NDK 27.0.12077973, CMake 4.3.0
- 지원 ABI: `arm64-v8a`, `armeabi-v7a`

### 단계
```bash
git clone <저장소 URL>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> 빌드에는 `keystore.properties` 서명 설정이 필요합니다. SoundTouch 소스는 프로젝트 루트의 `soundtouch/`에 있으며, CMake가 자동으로 정적 라이브러리로 컴파일하여 링크합니다.

---

## 📖 사용 가이드

### 화이트노이즈
1. 메인 화면에서 카테고리와 음원 선택
2. 음원 카드를 탭하여 재생 시작
3. 카드를 길게 눌러 독립 이펙트 설정
4. 스캐터 모드: 여러 음원 무작위 재생

### 음악
1. 상단 재생 바를 아래로 스와이프하여 재생 화면 진입
2. 파일 선택기로 음악 추가
3. 믹서 패널에서 이펙트 조정

### 이펙트 조정
**믹서 패널** 열기:
- **이퀄라이저** → Equalizer Panel
- **AutoEQ 보정** → Speaker Compensation(밴드별 편집 가능)
- **공간 오디오** → Spatial Audio Panel
- **리버브** → Reverb Panel
- **리미터** → Limiter(전달 함수 시각화 포함)
- **속도/피치** → 속도 조정(SoundTouch)

---

## 📦 오픈소스 의존성 및 라이선스

| 의존성 | 라이선스 | 용도 |
|------|------|------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | 저지연 오디오 엔진 |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | 속도/피치 독립 조정 |
| FFmpeg | LGPL v2.1+ | 오디오 디코딩 |
| Jetpack Compose / AndroidX | Apache 2.0 | UI 프레임워크 |
| Material Components | Apache 2.0 | UI 컴포넌트 |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | 설정 저장 |
| AndroidX Security | Apache 2.0 | 암호화 설정 |
| AndroidX Biometric | Apache 2.0 | 생체 인증 |
| Gson | Apache 2.0 | JSON 직렬화 |
| OkHttp | Apache 2.0 | HTTP 네트워킹 |
| Lottie | Apache 2.0 | 애니메이션 |
| Coil | Apache 2.0 | 이미지 로딩 |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | 음원 스크립트 엔진 |
| Liquid Glass Android | Apache 2.0 | 글래스모피즘 효과 |
| MTDataFilesProvider | Apache 2.0 | 미디어 스토어 데이터 프로바이더 |

> 음원은 [Pixabay](https://pixabay.com/)에서 제공되며, Pixabay License를 따릅니다.

---

## ⚠️ 라이선스

본 프로젝트는 **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)** 하에 라이선스됩니다.

자세한 내용은 [LICENSE](../LICENSE) 파일을 참조하세요.

> 서드파티 라이브러리는 각각의 원래 라이선스를 유지합니다(예: SoundTouch — LGPL v2.1, FFmpeg — LGPL v2.1+, QuickJS-kt — MIT).

---

## 📧 연락처

- **Issues**: [GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email**: [3139105039@qq.com](mailto:3139105039@qq.com)
- **저자**: byxixiaoshao / Bicy Studio

---

## 🙏 특별 감사

### 음원
모든 화이트노이즈 음원은 **[Pixabay](https://pixabay.com/)**에서 제공됩니다. 사운드 옵션을 길게 눌러 오디오 작성자 정보를 볼 수 있습니다. 가능하면 원작 크리에이터를 지원해 주세요.

### 소프트웨어 테스트
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### 아트 지원
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

오디오 처리 기술에 기여하는 모든 오픈소스 프로젝트에 감사드립니다.

---

<div align="center">

**⭐ 이 프로젝트가 도움이 되었다면 Star를 부탁드립니다 ⭐**

Made with ❤️ by Bicy Studio

</div>
