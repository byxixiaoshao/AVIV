# AVIV — ホワイトノイズ & 音楽プレイヤー

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**リアルタイム C++ オーディオエンジンを中心とした Android ホワイトノイズ & 音楽プレイヤー。十数種類のプロフェッショナルエフェクト、空間オーディオ、AutoEQ を統合。**

[機能](#-機能特性) · [オーディオエンジン](#-オーディオエンジン) · [アーキテクチャ](#-技術アーキテクチャ) · [ビルド](#-ソースからビルド) · [オープンソース依存関係とライセンス](#-オープンソース依存関係とライセンス)

[English](../README.md) · [中文](README_ZH.md) · **日本語（本ファイル）** · [한국어](README_KO.md) · [Русский](README_RU.md) · [Français](README_FR.md) · [Deutsch](README_DE.md) · [Español](README_ES.md) · [Italiano](README_IT.md) · [Português](README_PT.md)

</div>

---

## 📖 概要

AVIV は、ホワイトノイズ再生、ローカル/オンライン音楽、プロフェッショナル級のリアルタイムオーディオ処理エンジンを一つに融合したアプリです。すべてのエフェクトは Oboe 低レイテンシオーディオストリーム上で C++ レイヤーによりリアルタイム計算され、事前レンダリング不要、パラメータ変更は即座に反映されます。睡眠補助、集中、瞑想、雰囲気作り、音楽強化などに適しています。

> ⚠️ **注意**: 初回起動時、ログキャプチャがデフォルトで有効（クラッシュログ収集用）です。設定でオフにできます。

---

## ✨ 機能特性

### 🌊 ホワイトノイズ
- カテゴリ別に整理された複数の内蔵音源、ローカル音楽再生に対応
- **スキャッターモード**: カスタマイズ可能な間隔と範囲でランダム再生、自然な音場をシミュレート
- トラックごとに**独立したエフェクトチェーン**（EQ / リバーブ / 空間 / クリエイティブ）、相互干渉なし
- 音量ゲイン範囲 0–300%

### 🎵 音楽再生
- マルチフォーマット対応（MP3 / WAV / FLAC / AAC など）
- プレイリスト管理

### 🎛️ リアルタイムオーディオエフェクト（C++ エンジン）
| エフェクト | 説明 |
|------|------|
| **イコライザー (EQ)** | マニュアルマルチバンド BiQuad フィルター、トラックごとの独立カーブ、Bypass 対応 |
| **AutoEQ スピーカー補正** | 12 種のデバイスプリセット、16 の調整可能パラメータ、全フィルターパラメータ（ゲイン/周波数/Q）がバンドごとに編集・保存可能 |
| **空間オーディオ** | 3D 音源定位、距離減衰、サラウンド軌道（0.25–10 秒/周）、ランダム散布、固定オフセット |
| **リバーブ** | ルームサイズ、減衰時間、プリディレイ、ウェット/ドライミックス、遮音 |
| **リミッター** | ブリックウォール制限、伝達関数カーブ + VU メーター + GR メーターのリアルタイム可視化 |
| **クリエイティブエフェクト** | Lo-Fi / 8-bit / 水中 / エイリアンシグナル / メガホン / ディストーション |
| **疑似復元処理** | HiFi 音質強化（ソフトニートランジェント、DC ブロック、ゲインスムージング） |
| **マルチバンドコンプレッサー** | MultibandCompressor |
| **ステレオワイドナー / 仮想ベース / 遮音** | StereoWidener / VirtualBass / Insulation |
| **レート & ピッチ** | **SoundTouch** 駆動、レート 0.1–5.0×、ピッチ ±12 半音、独立して調整可能 |

### 🎛️ ミキサーパネル
- ワンストップエフェクト制御: エフェクト強度、EQ、AutoEQ、空間オーディオ、リバーブ、リミッター、レート/ピッチ
- リアルタイムプレビュー、パラメータ即時反映
- ワンタップリセット

### ⏰ タイマー
- スリープタイマー（最大 23 時間 59 分）
- スヌーズモード
- タイマーボールをタップして計測開始

### 🐾 フローティングペット
- デスクトップ浮遊ペット、ドラッグ / リサイズ / 非表示可能
- 中心アンカーのスケーリング、非表示状態からの復帰

### 🎨 パーソナライズ
- **カスタムテーマ**: HSV カラーカスタマイズ + プリセットテーマ
- **フロステッドグラス / リキッドグラス** エフェクト
- エフェクトパラメータの自動永続化

### 🔔 通知システム
- **アプリ内通知**: 5 種（情報/成功/警告/エラー/読込）、優先度レベル、スワイプ解除、アクションボタン
- **システムメディア通知**: MediaSession 統合、ロック画面制御

### 🛡️ 安定性 & 常駐
- **オーディオストリーム維持**: AudioFocus 戦略、ストリーム切断の耐障害リトライ、バッファ最適化、Oboe エラーの優雅な回復、MediaPlayer フォールバック
- **バックグラウンド常駐ガイド**: バッテリー最適化ホワイトリスト + ベンダー自動起動設定ガイド
- **メモリロック診断**: 異常監視（オーディオエンジン再起動 / バッファアンダーラン / メインスレッド停滞 / メモリ警告）+ 診断レポート
- **ログシステム**: 独立プロセスで Logcat & クラッシュログをキャプチャ

### 🌍 多言語
10 言語サポート: 中国語、英語、日本語、韓国語、ロシア語、フランス語、ドイツ語、スペイン語、イタリア語、ポルトガル語

---

## 🔊 オーディオエンジン

### 処理パイプライン

```
デコーダー ──► 音量/フェード ──► 遮音 ──► 空間オーディオ ──► リバーブ
           ──► EQ / AutoEQ ──► クリエイティブ ──► 疑似復元 ──► リミッター
           ──► SoundTouch(レート/ピッチ) ──► ハードクリップ ──► Oboe 出力
```

### C++ モジュール構成

| ディレクトリ | 責務 |
|------|------|
| `cpp/AudioPlayFunc/` | `AudioEngine`（Oboe ストリーム管理、マルチトラックミキシング）、`AudioTrack`（トラックごとのデコード & エフェクトチェーン） |
| `cpp/AudioEffect/` | 12 のエフェクトプロセッサ + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | BiQuad フィルター実装 |
| `cpp/reverb/` | リバーブアルゴリズム |
| `cpp/spatial_audio/` | 空間オーディオ HRTF |
| `cpp/oboe/` | Google Oboe 低レイテンシオーディオライブラリ |
| `cpp/ffmpeg/` | FFmpeg デコード（LGPL） |

### JNI インターフェース
`JniInterface.cpp` がすべてのオーディオ機能（再生制御、エフェクトパラメータ、EQ カーブ、空間オーディオ、AutoEQ、リミッター、SoundTouch レート/ピッチなど）を公開、Kotlin 側は `OboeAudioEngine` がラップ。

---

## 🏗️ 技術アーキテクチャ

| レイヤー | 技術 |
|----|------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| オーディオ | C++ + Oboe + SoundTouch + FFmpeg（JNI） |
| ネットワーク | OkHttp |
| スクリプト | QuickJS-kt（オンライン音源スクリプトエンジン） |
| ストレージ | Gson（JSON 設定）+ EncryptedSharedPreferences |
| メディア | AndroidX Media（MediaSession） |
| その他 | Lottie / Coil / 生体認証 / MTDataFilesProvider |

### 主要 Kotlin パッケージ

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine、空間オーディオ、リバーブ、スキャッター再生管理
├── music/          # 音楽再生制御
├── onlinemusic/    # オンラインストリーミング & 音源スクリプトエンジン
├── storage/        # 設定/ホワイトノイズ/音楽/テーマストレージ（JSON）
├── ui/             # Compose UI & コンポーネント
├── servies/        # MusicService（フォアグラウンドメディアサービス）
├── service/        # MemoryLockService、LogCaptureService（:log プロセス）
├── floatingpet/    # フローティングペットサービス
├── timer/          # タイマー
├── equalizer/      # イコライザーロジック
└── ...             # data / security / utils / playlist など
```

---

## 📥 ダウンロード & インストール

### システム要件
- Android 10.0（API 29）以上
- 100 MB 以上の空きストレージ

### インストール
1. [Releases](https://github.com/byxixiaoshao/AVIV/releases) から最新 APK をダウンロード
2. デバイスで「不明なアプリのインストール」を有効化
3. APK を開いてインストール
4. アプリを起動

---

## 🔧 ソースからビルド

### 要件
- Android Studio（現在の AGP バージョンをサポート）
- JDK 11
- Android SDK、compileSdk 36
- NDK 27.0.12077973、CMake 4.3.0
- サポート ABI: `arm64-v8a`、`armeabi-v7a`

### 手順
```bash
git clone <リポジトリURL>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> ビルドには `keystore.properties` 署名設定が必要です。SoundTouch のソースはご自身で入手してください。

---

## 📖 使い方

### ホワイトノイズ
1. メイン画面でカテゴリと音源を選択
2. 音源カードをタップして再生開始
3. カードを長押しして独立エフェクトを設定
4. スキャッターモード: 複数音源をランダム再生

### 音楽
1. トップ再生バーを下にスワイプして再生画面へ
2. ファイルピッカーで音楽を追加
3. ミキサーパネルでエフェクトを調整

### エフェクト調整
**ミキサーパネル**を開く:
- **イコライザー** → Equalizer Panel
- **AutoEQ 補正** → Speaker Compensation（バンドごとに編集可）
- **空間オーディオ** → Spatial Audio Panel
- **リバーブ** → Reverb Panel
- **リミッター** → Limiter（伝達関数可視化付き）
- **レート/ピッチ** → 速度調整（SoundTouch）

---

## 📦 オープンソース依存関係とライセンス

| 依存関係 | ライセンス | 用途 |
|------|------|------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | 低レイテンシオーディオエンジン |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | レート/ピッチ独立調整 |
| FFmpeg | LGPL v2.1+ | オーディオデコード |
| Jetpack Compose / AndroidX | Apache 2.0 | UI フレームワーク |
| Material Components | Apache 2.0 | UI コンポーネント |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | 設定ストレージ |
| AndroidX Security | Apache 2.0 | 暗号化設定 |
| AndroidX Biometric | Apache 2.0 | 生体認証 |
| Gson | Apache 2.0 | JSON シリアライズ |
| OkHttp | Apache 2.0 | HTTP ネットワーク |
| Lottie | Apache 2.0 | アニメーション |
| Coil | Apache 2.0 | 画像読み込み |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | 音源スクリプトエンジン |
| Liquid Glass Android | Apache 2.0 | グラスモーフィズム効果 |
| MTDataFilesProvider | Apache 2.0 | メディアストアデータプロバイダー |

> 音源は [Pixabay](https://pixabay.com/) から、Pixabay License に準拠。

---

## ⚠️ ライセンス

本プロジェクトは **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)** の下でライセンスされています。

詳細は [LICENSE](../LICENSE) ファイルを参照してください。

> サードパーティライブラリは各々の元のライセンスを保持します（例: SoundTouch — LGPL v2.1、FFmpeg — LGPL v2.1+、QuickJS-kt — MIT）。

---

## 📧 連絡先

- **Issues**: [GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email**: [3139105039@qq.com](mailto:3139105039@qq.com)
- **作者**: byxixiaoshao / Bicy Studio

---

## 🙏 謝辞

### 音源
すべてのホワイトノイズ音源は **[Pixabay](https://pixabay.com/)** から提供されています。サウンドオプションを長押しするとオーディオ作者情報を表示できます。可能であればオリジナルクリエイターをサポートしてください。

### ソフトウェアテスト
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### アートサポート
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

オーディオ処理技術に貢献するすべてのオープンソースプロジェクトに感謝します。

---

<div align="center">

**⭐ このプロジェクトが役に立ったら、Star をお願いします ⭐**

Made with ❤️ by Bicy Studio

</div>
