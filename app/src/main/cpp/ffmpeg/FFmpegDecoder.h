#ifndef FFMPEG_DECODER_H
#define FFMPEG_DECODER_H

#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <condition_variable>

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

class FFmpegDecoder {
public:
    FFmpegDecoder();
    ~FFmpegDecoder();

    bool open(const std::string& filePath);
    bool openFromFd(int fd, int64_t offset = 0, int64_t length = -1);
    void close();
    bool isOpen() const;

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
};

}

#endif
