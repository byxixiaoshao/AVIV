# 添空 AVIV — 白噪音 & 音乐播放器

<div align="center">

![Platform](https://img.shields.io/badge/platform-Android%207.0%2B-green.svg)
![Language](https://img.shields.io/badge/Kotlin%20%2B%20C%2B%2B-Compose%20%2F%20Oboe-blue.svg)
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)

**一款以实时 C++ 音频引擎为核心的 Android 白噪音与音乐播放器，集成十余种专业效果器、空间音频与AutoEQ**

[功能特性](#-功能特性) · [音频引擎](#-音频引擎) · [技术架构](#-技术架构) · [构建](#-从源码构建) · [开源依赖与许可](#-开源依赖与许可)

[English](../README.md) · **中文（本文件）** · [日本語](README_JA.md) · [한국어](README_KO.md) · [Русский](README_RU.md) · [Français](README_FR.md) · [Deutsch](README_DE.md) · [Español](README_ES.md) · [Italiano](README_IT.md) · [Português](README_PT.md)

</div>

---

## 📖 关于

添空 AVIV 将白噪音播放、本地/在线音乐与一套专业级实时音频处理引擎融合于一体。所有效果均在 C++ 层基于 Oboe 低延迟音频流实时计算，不依赖预渲染，参数调节即时生效。适用于助眠、专注、冥想、氛围营造与音乐增强等场景。

> ⚠️ **使用须知**：首次启动时日志捕获默认开启（用于收集崩溃日志），可在设置中关闭。

---

## ✨ 功能特性

### 🌊 白噪音
- 内置多种音源，按分类组织，支持本地音乐播放
- **散布模式（Scatter）**：随机播放，可自定义间隔与范围，模拟自然声场
- 每个音轨**独立配置效果**（EQ / 混响 / 空间 / 创意效果），互不干扰
- 音量增益范围 0–300%

### 🎵 音乐播放
- 多格式支持（MP3 / WAV / FLAC / AAC 等）
- 播放列表管理

### 🎛️ 实时音频效果（C++ 引擎）
| 效果 | 说明 |
|------|------|
| **均衡器 EQ** | 手动多频段 BiQuad 滤波器，每音轨独立曲线，支持 Bypass |
| **AutoEQ 扬声器补偿** | 12 种设备预设（手机/耳机/蓝牙/车载…），16 个可调参数，全部滤波器参数（增益/频率/Q）可逐频段编辑并持久化 |
| **空间音频** | 3D 声源定位、距离衰减、环绕轨迹（0.25–10 秒/圈）、随机散布、固定偏移 |
| **混响 Reverb** | 房间大小、衰减时间、预延迟、干湿混合、隔音 |
| **限幅器 Limiter** | 砖墙限幅，传递函数曲线 + VU 电平表 + 增益削减量实时可视化 |
| **创意效果** | Lo-Fi / 8-bit / 水下 / 外星信号 / 扩音器 / 失真 |
| **伪还原二次处理** | HiFi 音质增强（软膝瞬态、DC 阻挡、增益平滑） |
| **多段压缩** | MultibandCompressor |
| **立体声加宽 / 虚拟低音 / 隔音** | StereoWidener / VirtualBass / Insulation |
| **速率 & 音调** | 基于 **SoundTouch**，速率 0.1–5.0×、音调 ±12 半音，独立解耦调节 |

### 🎛️ 调音台面板
- 一站式效果控制：效果强度、EQ、AutoEQ、空间音频、混响、限幅器、速率/音调
- 实时预览，参数即时生效
- 一键重置

### ⏰ 定时器
- 定时停止（最长 23h 59m）
- 贪睡模式
- 点击定时球即可开始计时

### 🐾 悬浮萌宠
- 桌面悬浮宠物，可拖拽、缩放、隐藏
- 中心锚点缩放，隐藏状态可唤醒

### 🎨 个性化
- **自定义主题**：HSV 色彩定制 + 预设主题
- **毛玻璃 / 液态玻璃**效果
- 效果参数自动持久化

### 🔔 通知系统
- **应用内通知**：5 种类型（信息/成功/警告/错误/加载），优先级层级，滑动消除，操作按钮
- **系统媒体通知**：MediaSession 集成，锁屏控制

### 🛡️ 稳定性与保活
- **音频流维持**：AudioFocus 策略、流断开容错重试、缓冲区优化、Oboe 错误优雅恢复、MediaPlayer 降级兜底
- **后台保活引导**：电池优化白名单 + 厂商自启动设置引导
- **内存锁诊断**：异常监控（音频引擎重启 / 缓冲欠载 / 主线程卡顿 / 内存警告）+ 诊断报告
- **日志系统**：独立进程捕获 Logcat 与崩溃日志

### 🌍 多语言
支持 10 种语言：中文、英语、日语、韩语、俄语、法语、德语、西班牙语、意大利语、葡萄牙语

---

## 🔊 音频引擎

### 处理管线

```
解码器 ──► 音量/淡入淡出 ──► 隔音 ──► 空间音频 ──► 混响
       ──► EQ / AutoEQ ──► 创意效果 ──► 伪还原 ──► 限幅器
       ──► SoundTouch(速率/音调) ──► 硬钳位 ──► Oboe 输出
```

### C++ 模块结构

| 目录 | 职责 |
|------|------|
| `cpp/AudioPlayFunc/` | `AudioEngine`（Oboe 流管理、多轨混音）、`AudioTrack`（单轨解码与效果链） |
| `cpp/AudioEffect/` | 12 个效果器 + `AudioEffectManager` + `SoundTouchProcessor` |
| `cpp/equalizer/` | BiQuad 滤波器实现 |
| `cpp/reverb/` | 混响算法 |
| `cpp/spatial_audio/` | 空间音频 HRTF |
| `cpp/oboe/` | Google Oboe 低延迟音频库 |
| `cpp/ffmpeg/` | FFmpeg 解码（LGPL） |

### JNI 接口
`JniInterface.cpp` 暴露全部音频能力（播放控制、效果参数、EQ 曲线、空间音频、AutoEQ、限幅器、SoundTouch 速率音调等），Kotlin 侧由 `OboeAudioEngine` 封装。

---

## 🏗️ 技术架构

| 层 | 技术 |
|----|------|
| UI | Kotlin + Jetpack Compose + Material 3 |
| 音频 | C++ + Oboe + SoundTouch + FFmpeg（JNI） |
| 网络 | OkHttp |
| 脚本 | QuickJS-kt（在线音源脚本引擎） |
| 存储 | Gson（JSON 配置）+ EncryptedSharedPreferences |
| 媒体 | AndroidX Media（MediaSession） |
| 其他 | Lottie / Coil / 生物识别 / MTDataFilesProvider |

### 主要 Kotlin 包

```
com.bicy.whitenoise
├── audio/          # OboeAudioEngine、空间音频、混响、散布播放管理
├── music/          # 音乐播放控制
├── onlinemusic/    # 在线流媒体与音源脚本引擎
├── storage/        # 配置/白噪音/音乐/主题存储（JSON）
├── ui/             # Compose 界面与组件
├── servies/        # MusicService（前台媒体服务）
├── service/        # MemoryLockService、LogCaptureService（:log 进程）
├── floatingpet/    # 悬浮萌宠服务
├── timer/          # 定时器
├── equalizer/      # 均衡器逻辑
└── ...             # data / security / utils / playlist 等
```

---

## 📥 下载与安装

### 系统要求
- Android 10.0（API 29）或更高
- 至少 100MB 可用存储

### 安装
1. 从 [Releases](https://github.com/byxixiaoshao/AVIV/releases) 下载最新 APK
2. 在设备上启用"安装未知来源应用"
3. 打开 APK 安装
4. 启动应用

---

## 🔧 从源码构建

### 环境要求
- Android Studio（支持 AGP 当前版本）
- JDK 11
- Android SDK，compileSdk 36
- NDK 27.0.12077973，CMake 4.3.0
- 支持 ABI：`arm64-v8a`、`armeabi-v7a`

### 步骤
```bash
git clone <仓库地址>
cd AVIV/Android
./gradlew :app:assembleRelease
```

> 构建需要 `keystore.properties` 签名配置文件。SoundTouch 源码请自行获取。

---

## 📖 使用指南

### 白噪音
1. 主界面选择白噪音分类与音源
2. 点击音源卡片开始播放
3. 长按卡片配置独立效果
4. 散布模式：随机播放多个音源

### 音乐
1. 下滑顶部播放栏进入播放界面
2. 通过文件选择器添加音乐
3. 在调音台面板调节效果

### 效果调节
打开**调音台面板**（Mixer）：
- **均衡器** → Equalizer Panel
- **AutoEQ 补偿** → Speaker Compensation（可逐频段编辑）
- **空间音频** → Spatial Audio Panel
- **混响** → Reverb Panel
- **限幅器** → Limiter（含传递函数可视化）
- **速率/音调** → 速度调整（SoundTouch）

---

## 📦 开源依赖与许可

| 依赖 | 许可 | 用途 |
|------|------|------|
| [Oboe](https://github.com/google/oboe) | Apache 2.0 | 低延迟音频引擎 |
| [SoundTouch](https://gitlab.com/soundtouch/soundtouch) | LGPL v2.1 | 速率/音调独立调节 |
| FFmpeg | LGPL v2.1+ | 音频解码 |
| Jetpack Compose / AndroidX | Apache 2.0 | UI 框架 |
| Material Components | Apache 2.0 | UI 组件 |
| AndroidX Media | Apache 2.0 | MediaSession |
| AndroidX DataStore | Apache 2.0 | 偏好存储 |
| AndroidX Security | Apache 2.0 | 加密偏好 |
| AndroidX Biometric | Apache 2.0 | 生物识别 |
| Gson | Apache 2.0 | JSON 序列化 |
| OkHttp | Apache 2.0 | 网络请求 |
| Lottie | Apache 2.0 | 动画 |
| Coil | Apache 2.0 | 图片加载 |
| [QuickJS-kt](https://github.com/dokar3/quickjs-kt) | MIT | 音源脚本引擎 |
| Liquid Glass Android | Apache 2.0 | 毛玻璃效果 |
| MTDataFilesProvider | Apache 2.0 | 媒体库数据提供者 |

> 音源来自 [Pixabay](https://pixabay.com/)，遵循 Pixabay 许可。

---

## ⚠️ 许可

本项目源码采用 **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)** 许可。

详见 [LICENSE](../LICENSE) 文件。

> 第三方库保留其原始许可（如 SoundTouch — LGPL v2.1，FFmpeg — LGPL v2.1+，QuickJS-kt — MIT）。

---

## 📧 联系

- **Issues**：[GitHub Issues](https://github.com/byxixiaoshao/AVIV/issues)
- **Email**：[3139105039@qq.com](mailto:3139105039@qq.com)
- **作者**：byxixiaoshao / Bicy Studio

---

## 🙏 特别鸣谢

### 音源
本项目使用的白噪音音源均来自 **[Pixabay](https://pixabay.com/)**。长按声音选项可查看音频作者信息，若条件允许，请支持原创作者。

### 软件测试
- 条纹哦里GHT
- 土豆仙人
- AAA哈密瓜批发星见雅

### 美术支持
- AAA哈密瓜批发星见雅
- ☆雨の日が好き☔

感谢所有为音频处理技术贡献的开源项目。

---

<div align="center">

**⭐ 如果本项目对您有帮助，请给个 Star ⭐**

Made with ❤️ by Bicy Studio

</div>
