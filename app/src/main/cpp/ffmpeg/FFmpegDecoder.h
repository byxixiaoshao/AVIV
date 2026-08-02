#ifndef FFMPEG_DECODER_H
#define FFMPEG_DECODER_H

#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <condition_variable>
#include <atomic>
#include <cstring>

extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"
#include "libavutil/opt.h"
}

namespace ffmpeg {

struct AudioInfo {
    int sampleRate = 0;
    int channels = 0;
    int64_t duration = 0;
    int64_t bitRate = 0;
    std::string codecName;
    std::string formatName;
};

struct DecodedAudio {
    std::vector<float> samples;
    int sampleRate = 0;
    int channels = 0;
    int64_t durationMs = 0;
};

struct FdContext {
    int fd = -1;
    int64_t offset = 0;
    int64_t length = -1;
    int64_t position = 0;
};

/** 流式解码上下文：环形缓冲区，生产者（Kotlin下载线程）写入，消费者（FFmpeg AVIO）读取 */
struct StreamContext {
    static constexpr size_t DEFAULT_BUFFER_SIZE = 512 * 1024;  // 512KB 环形缓冲区

    std::vector<uint8_t> buffer;
    size_t readIdx = 0;
    size_t writeIdx = 0;
    bool eof = false;
    std::mutex mtx;
    std::condition_variable cv;

    explicit StreamContext(size_t size = DEFAULT_BUFFER_SIZE) : buffer(size) {}

    size_t available() const {
        if (writeIdx >= readIdx) return writeIdx - readIdx;
        return buffer.size() - readIdx + writeIdx;
    }

    size_t freeSpace() const {
        size_t avail = available();
        if (avail + 1 >= buffer.size()) return 0;
        return buffer.size() - avail - 1;
    }

    /** 生产者写入数据，返回实际写入字节数 */
    size_t write(const uint8_t* data, size_t len) {
        std::lock_guard<std::mutex> lock(mtx);
        size_t space = freeSpace();
        size_t toWrite = std::min(len, space);
        if (toWrite == 0) return 0;

        size_t firstPart = std::min(toWrite, buffer.size() - writeIdx);
        memcpy(buffer.data() + writeIdx, data, firstPart);
        if (toWrite > firstPart) {
            memcpy(buffer.data(), data + firstPart, toWrite - firstPart);
        }
        writeIdx = (writeIdx + toWrite) % buffer.size();
        cv.notify_one();
        return toWrite;
    }

    /** 消费者读取数据，返回实际读取字节数；无数据且未 eof 时阻塞等待 */
    int read(uint8_t* buf, int len) {
        std::unique_lock<std::mutex> lock(mtx);
        while (available() == 0 && !eof) {
            cv.wait(lock);
        }
        size_t avail = available();
        if (avail == 0 && eof) return AVERROR_EOF;
        if (avail == 0) return 0;

        size_t toRead = std::min(static_cast<size_t>(len), avail);
        size_t firstPart = std::min(toRead, buffer.size() - readIdx);
        memcpy(buf, buffer.data() + readIdx, firstPart);
        if (toRead > firstPart) {
            memcpy(buf + firstPart, buffer.data(), toRead - firstPart);
        }
        readIdx = (readIdx + toRead) % buffer.size();
        cv.notify_one();
        return static_cast<int>(toRead);
    }

    void markComplete() {
        std::lock_guard<std::mutex> lock(mtx);
        eof = true;
        cv.notify_one();
    }

    bool isComplete() const { return eof; }
};

class FFmpegDecoder {
public:
    FFmpegDecoder();
    ~FFmpegDecoder();

    bool open(const std::string& filePath);
    bool openFromFd(int fd, int64_t offset = 0, int64_t length = -1);
    bool openFromStream(StreamContext* streamCtx);
    void close();
    bool isOpen() const;
    bool isStreamActive() const;

    AudioInfo getAudioInfo() const;

    bool decodeAll(DecodedAudio& output);
    bool decodeChunk(std::vector<float>& output, int maxFrames);
    bool seekTo(int64_t positionMs);

    void setOutputFormat(int sampleRate, int channels);
    void setDecodeRange(int64_t startMs, int64_t endMs);

private:
    bool initDecoder();
    bool initResampler();
    bool decodeFrame(AVFrame* frame);
    bool resampleFrame(AVFrame* frame, std::vector<float>& output);
    void cleanup();
    bool readAndDecodeFrame();
    bool initCustomIO();

    AVFormatContext* formatCtx_ = nullptr;
    AVCodecContext* codecCtx_ = nullptr;
    SwrContext* swrCtx_ = nullptr;
    AVIOContext* avioCtx_ = nullptr;
    unsigned char* avioBuffer_ = nullptr;
    int audioStreamIndex_ = -1;

    int outputSampleRate_ = 44100;
    int outputChannels_ = 2;

    int64_t seekPosition_ = -1;
    int64_t rangeStart_ = -1;
    int64_t rangeEnd_ = -1;

    bool isOpen_ = false;
    std::mutex mutex_;
    
    std::vector<float> sampleBuffer_;
    
    FdContext fdContext_;
    bool useFd_ = false;

    StreamContext* streamCtx_ = nullptr;
    bool useStream_ = false;
};

}

#endif
