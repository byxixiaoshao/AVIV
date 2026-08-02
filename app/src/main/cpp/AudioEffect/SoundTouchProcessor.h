#ifndef SOUNDTOUCH_PROCESSOR_H
#define SOUNDTOUCH_PROCESSOR_H

#include <SoundTouch.h>
#include <memory>

namespace audiofx {

/**
 * SoundTouchProcessor
 *
 * 对 soundtouch::SoundTouch 的轻量封装，提供交错的（interleaved）音频接口，
 * 便于直接接入现有 AudioTrack 管线。替代原 RubberBandWrapper。
 *
 * 设计要点：
 *  - SoundTouch 原生即按交错 float 工作，无需 de-interleave / interleave 转换。
 *  - FIFO 管道模型：调用方先 process() 喂入数据，再通过 available()/retrieve() 取出输出。
 *  - setTempo(speed)：1.0=原速，2.0=2 倍速（同调），0.5=半速。与 UI speed 直接对应。
 *  - setPitchSemiTones(semitones)：0=原调，+12=高八度，-12=低八度。与 UI pitch 直接对应。
 *  - tempo 与 pitch 独立可调（tempo 改速率不改音调，pitch 改音调不改速率）。
 *
 * 帧数语义：本封装所有 numFrames/maxFrames 均指"帧"（每帧 channels 个采样），
 * 与 SoundTouch 的 putSamples/receiveSamples 的 numSamples 语义一致（立体声单帧=1 sample）。
 *
 * 线程安全：与 SoundTouch 一致，单实例不可并发调用 process()。
 * 调用方应确保 setTempo/setPitchSemiTones 与 process() 同线程，或自行加锁。
 */
class SoundTouchProcessor {
public:
    SoundTouchProcessor() = default;
    ~SoundTouchProcessor() = default;

    SoundTouchProcessor(const SoundTouchProcessor&) = delete;
    SoundTouchProcessor& operator=(const SoundTouchProcessor&) = delete;

    /** 初始化处理器。sampleRate/channels 与实际音频管线一致。 */
    void init(int sampleRate, int channels) {
        sampleRate_ = sampleRate > 0 ? sampleRate : 44100;
        channels_ = channels > 0 ? channels : 2;

        pST_ = std::make_unique<soundtouch::SoundTouch>();
        pST_->setSampleRate(static_cast<uint>(sampleRate_));
        pST_->setChannels(static_cast<uint>(channels_));

        // WSOLA 参数优化：减小 sequence/seekwindow/overlap 以降低内部延迟和 CPU 开销。
        // 默认 sequence≈82ms seekwindow≈28ms overlap≈12ms → 预热延迟约 100-200ms，
        // 减速时 WSOLA 搜索开销大易导致 underrun 卡顿。
        // 减小到 sequence=40 seekwindow=15 overlap=8 → 延迟约 50ms，CPU 降低约 40%，
        // 质量轻微下降但实时性更好（适合实时播放而非离线处理）。
        pST_->setSetting(SETTING_SEQUENCE_MS, 40);
        pST_->setSetting(SETTING_SEEKWINDOW_MS, 15);
        pST_->setSetting(SETTING_OVERLAP_MS, 8);
    }

    /** 重置内部缓冲（clear），保留当前 tempo/pitch 设置。 */
    void reset() {
        if (pST_) pST_->clear();
    }

    /**
     * 设置播放速率（tempo）。1.0=原速，2.0=2 倍速（保持音调），0.5=半速。
     * 实时安全，可在播放中随时调用（勿与 process() 并发）。
     */
    void setTempo(float speed) {
        if (pST_) pST_->setTempo(static_cast<double>(speed));
    }

    /**
     * 设置音调偏移（半音）。0=原调，+12=高八度，-12=低八度（保持速率）。
     * 实时安全，可在播放中随时调用（勿与 process() 并发）。
     */
    void setPitchSemiTones(float semitones) {
        if (pST_) pST_->setPitchSemiTones(static_cast<double>(semitones));
    }

    /**
     * 喂入交错的音频数据（numFrames 个帧，每帧 channels 个采样）。
     * 不立即产出，需随后 retrieve()。SoundTouch 有初始延迟，首段输入可能无输出。
     */
    void process(const float* input, int numFrames) {
        if (!pST_ || numFrames <= 0) return;
        pST_->putSamples(input, static_cast<uint>(numFrames));
    }

    /** 当前可 retrieve 的输出帧数（可能为 0，尤其在初始预热期）。 */
    int available() const {
        return pST_ ? static_cast<int>(pST_->numSamples()) : 0;
    }

    /**
     * 取出交错输出，至多 maxFrames 帧。返回实际取出的帧数。
     * output 容量需 >= maxFrames * channels。
     */
    int retrieve(float* output, int maxFrames) {
        if (!pST_ || maxFrames <= 0) return 0;
        return static_cast<int>(pST_->receiveSamples(output, static_cast<uint>(maxFrames)));
    }

    /**
     * 刷新末尾剩余样本到输出管道。仅在流结束时调用以 drain 残留，
     * 中途调用会引入空白样本，不建议在播放中使用。
     */
    void flush() {
        if (pST_) pST_->flush();
    }

    int channels() const { return channels_; }
    int sampleRate() const { return sampleRate_; }

private:
    std::unique_ptr<soundtouch::SoundTouch> pST_;
    int sampleRate_ = 44100;
    int channels_ = 2;
};

} // namespace audiofx

#endif // SOUNDTOUCH_PROCESSOR_H
