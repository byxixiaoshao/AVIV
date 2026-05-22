#include "FFmpegDecoder.h"
#include <android/log.h>
#include <unistd.h>

#define LOG_TAG "FFmpegDecoder"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace ffmpeg {

static int fdRead(void* opaque, uint8_t* buf, int bufSize) {
    FdContext* ctx = static_cast<FdContext*>(opaque);
    if (ctx->fd < 0) {
        return AVERROR_EOF;
    }
    
    int64_t remaining = ctx->length - ctx->position;
    if (ctx->length >= 0 && remaining <= 0) {
        return AVERROR_EOF;
    }
    
    int toRead = bufSize;
    if (ctx->length >= 0 && toRead > remaining) {
        toRead = static_cast<int>(remaining);
    }
    
    ssize_t bytesRead = read(ctx->fd, buf, toRead);
    if (bytesRead < 0) {
        return AVERROR(errno);
    }
    if (bytesRead == 0) {
        return AVERROR_EOF;
    }
    
    ctx->position += bytesRead;
    return static_cast<int>(bytesRead);
}

static int64_t fdSeek(void* opaque, int64_t offset, int whence) {
    FdContext* ctx = static_cast<FdContext*>(opaque);
    if (ctx->fd < 0) {
        return AVERROR(EINVAL);
    }
    
    int64_t newPos = 0;
    switch (whence) {
        case SEEK_SET:
            newPos = offset;
            break;
        case SEEK_CUR:
            newPos = ctx->position + offset;
            break;
        case SEEK_END:
            if (ctx->length >= 0) {
                newPos = ctx->length + offset;
            } else {
                off_t result = lseek(ctx->fd, offset, SEEK_END);
                if (result < 0) {
                    return AVERROR(errno);
                }
                ctx->position = result;
                return ctx->position;
            }
            break;
        case AVSEEK_SIZE:
            if (ctx->length >= 0) {
                return ctx->length;
            }
            return AVERROR(ENOSYS);
        default:
            return AVERROR(EINVAL);
    }
    
    if (newPos < 0) {
        newPos = 0;
    }
    if (ctx->length >= 0 && newPos > ctx->length) {
        newPos = ctx->length;
    }
    
    off_t result = lseek(ctx->fd, ctx->offset + newPos, SEEK_SET);
    if (result < 0) {
        return AVERROR(errno);
    }
    
    ctx->position = newPos;
    return ctx->position;
}

FFmpegDecoder::FFmpegDecoder() = default;

FFmpegDecoder::~FFmpegDecoder() {
    close();
}

bool FFmpegDecoder::open(const std::string& filePath) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (isOpen_) {
        close();
    }

    useFd_ = false;
    
    int ret = avformat_open_input(&formatCtx_, filePath.c_str(), nullptr, nullptr);
    if (ret != 0) {
        char errBuf[256];
        av_strerror(ret, errBuf, sizeof(errBuf));
        LOGE("Failed to open file: %s, error: %s", filePath.c_str(), errBuf);
        return false;
    }

    ret = avformat_find_stream_info(formatCtx_, nullptr);
    if (ret < 0) {
        char errBuf[256];
        av_strerror(ret, errBuf, sizeof(errBuf));
        LOGE("Failed to find stream info: %s", errBuf);
        avformat_close_input(&formatCtx_);
        return false;
    }

    audioStreamIndex_ = -1;
    for (unsigned int i = 0; i < formatCtx_->nb_streams; i++) {
        if (formatCtx_->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioStreamIndex_ = i;
            break;
        }
    }

    if (audioStreamIndex_ == -1) {
        LOGE("No audio stream found");
        avformat_close_input(&formatCtx_);
        return false;
    }

    if (!initDecoder()) {
        cleanup();
        return false;
    }

    isOpen_ = true;
    LOGI("Opened file: %s, sample rate: %d, channels: %d", 
         filePath.c_str(), codecCtx_->sample_rate, codecCtx_->ch_layout.nb_channels);
    
    return true;
}

bool FFmpegDecoder::openFromFd(int fd, int64_t offset, int64_t length) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (isOpen_) {
        close();
    }

    if (fd < 0) {
        LOGE("Invalid file descriptor");
        return false;
    }

    useFd_ = true;
    fdContext_.fd = fd;
    fdContext_.offset = offset;
    fdContext_.length = length;
    fdContext_.position = 0;

    if (lseek(fd, offset, SEEK_SET) < 0) {
        LOGE("Failed to seek to offset: %lld", (long long)offset);
        return false;
    }

    if (!initCustomIO()) {
        cleanup();
        return false;
    }

    int ret = avformat_open_input(&formatCtx_, "", nullptr, nullptr);
    if (ret != 0) {
        char errBuf[256];
        av_strerror(ret, errBuf, sizeof(errBuf));
        LOGE("Failed to open fd: %s", errBuf);
        cleanup();
        return false;
    }

    ret = avformat_find_stream_info(formatCtx_, nullptr);
    if (ret < 0) {
        char errBuf[256];
        av_strerror(ret, errBuf, sizeof(errBuf));
        LOGE("Failed to find stream info: %s", errBuf);
        cleanup();
        return false;
    }

    audioStreamIndex_ = -1;
    for (unsigned int i = 0; i < formatCtx_->nb_streams; i++) {
        if (formatCtx_->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioStreamIndex_ = i;
            break;
        }
    }

    if (audioStreamIndex_ == -1) {
        LOGE("No audio stream found");
        cleanup();
        return false;
    }

    if (!initDecoder()) {
        cleanup();
        return false;
    }

    isOpen_ = true;
    LOGI("Opened fd: %d (offset=%lld, length=%lld), sample rate: %d, channels: %d", 
         fd, (long long)offset, (long long)length,
         codecCtx_->sample_rate, codecCtx_->ch_layout.nb_channels);
    
    return true;
}

bool FFmpegDecoder::initCustomIO() {
    const int bufferSize = 8192;
    avioBuffer_ = static_cast<unsigned char*>(av_malloc(bufferSize));
    if (!avioBuffer_) {
        LOGE("Failed to allocate AVIO buffer");
        return false;
    }

    avioCtx_ = avio_alloc_context(avioBuffer_, bufferSize, 0, &fdContext_, fdRead, nullptr, fdSeek);
    if (!avioCtx_) {
        LOGE("Failed to allocate AVIO context");
        av_free(avioBuffer_);
        avioBuffer_ = nullptr;
        return false;
    }

    formatCtx_ = avformat_alloc_context();
    if (!formatCtx_) {
        LOGE("Failed to allocate format context");
        return false;
    }

    formatCtx_->pb = avioCtx_;
    return true;
}

bool FFmpegDecoder::initDecoder() {
    AVCodecParameters* codecPar = formatCtx_->streams[audioStreamIndex_]->codecpar;
    const AVCodec* codec = avcodec_find_decoder(codecPar->codec_id);
    
    if (!codec) {
        LOGE("Decoder not found for codec id: %d", codecPar->codec_id);
        return false;
    }

    codecCtx_ = avcodec_alloc_context3(codec);
    if (!codecCtx_) {
        LOGE("Failed to allocate codec context");
        return false;
    }

    int ret = avcodec_parameters_to_context(codecCtx_, codecPar);
    if (ret < 0) {
        LOGE("Failed to copy codec parameters");
        return false;
    }

    ret = avcodec_open2(codecCtx_, codec, nullptr);
    if (ret < 0) {
        char errBuf[256];
        av_strerror(ret, errBuf, sizeof(errBuf));
        LOGE("Failed to open codec: %s", errBuf);
        return false;
    }

    return true;
}

bool FFmpegDecoder::initResampler() {
    AVChannelLayout outLayout;
    if (outputChannels_ == 1) {
        outLayout = AV_CHANNEL_LAYOUT_MONO;
    } else if (outputChannels_ == 2) {
        outLayout = AV_CHANNEL_LAYOUT_STEREO;
    } else {
        av_channel_layout_default(&outLayout, outputChannels_);
    }

    int ret = swr_alloc_set_opts2(&swrCtx_, 
                                   &outLayout,
                                   AV_SAMPLE_FMT_FLT,
                                   outputSampleRate_,
                                   &codecCtx_->ch_layout,
                                   codecCtx_->sample_fmt,
                                   codecCtx_->sample_rate,
                                   0, nullptr);
    if (ret < 0) {
        LOGE("Failed to allocate resampler");
        return false;
    }

    ret = swr_init(swrCtx_);
    if (ret < 0) {
        LOGE("Failed to initialize resampler");
        return false;
    }

    LOGI("Resampler initialized: %dHz %dch -> %dHz %dch",
         codecCtx_->sample_rate, codecCtx_->ch_layout.nb_channels,
         outputSampleRate_, outputChannels_);
    return true;
}

void FFmpegDecoder::close() {
    std::lock_guard<std::mutex> lock(mutex_);
    cleanup();
}

void FFmpegDecoder::cleanup() {
    if (swrCtx_) {
        swr_free(&swrCtx_);
        swrCtx_ = nullptr;
    }
    
    if (codecCtx_) {
        avcodec_free_context(&codecCtx_);
        codecCtx_ = nullptr;
    }
    
    sampleBuffer_.clear();
    
    if (formatCtx_) {
        avformat_close_input(&formatCtx_);
        formatCtx_ = nullptr;
    }
    
    if (avioCtx_) {
        if (avioCtx_->buffer) {
            av_free(avioCtx_->buffer);
        }
        avio_context_free(&avioCtx_);
        avioCtx_ = nullptr;
        avioBuffer_ = nullptr;
    }
    
    if (useFd_ && fdContext_.fd >= 0) {
        ::close(fdContext_.fd);
        fdContext_.fd = -1;
    }
    
    audioStreamIndex_ = -1;
    isOpen_ = false;
    useFd_ = false;
}

bool FFmpegDecoder::isOpen() const {
    return isOpen_;
}

AudioInfo FFmpegDecoder::getAudioInfo() const {
    AudioInfo info;
    
    if (!isOpen_ || !formatCtx_ || audioStreamIndex_ == -1) {
        return info;
    }

    AVStream* stream = formatCtx_->streams[audioStreamIndex_];
    info.sampleRate = codecCtx_ ? codecCtx_->sample_rate : 0;
    info.channels = codecCtx_ ? codecCtx_->ch_layout.nb_channels : 0;
    info.duration = formatCtx_->duration;
    info.bitRate = formatCtx_->bit_rate;
    
    if (codecCtx_ && codecCtx_->codec) {
        info.codecName = codecCtx_->codec->name;
    }
    info.formatName = formatCtx_->iformat ? formatCtx_->iformat->name : "";
    
    return info;
}

bool FFmpegDecoder::decodeAll(DecodedAudio& output) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (!isOpen_) {
        LOGE("Decoder not open");
        return false;
    }

    output.sampleRate = outputSampleRate_;
    output.channels = outputChannels_;
    output.samples.clear();

    AVFrame* frame = av_frame_alloc();
    AVPacket* packet = av_packet_alloc();
    
    if (!frame || !packet) {
        av_frame_free(&frame);
        av_packet_free(&packet);
        return false;
    }

    bool success = true;
    int64_t totalSamples = 0;

    while (av_read_frame(formatCtx_, packet) >= 0) {
        if (packet->stream_index != audioStreamIndex_) {
            av_packet_unref(packet);
            continue;
        }

        int ret = avcodec_send_packet(codecCtx_, packet);
        if (ret < 0) {
            char errBuf[256];
            av_strerror(ret, errBuf, sizeof(errBuf));
            LOGE("Error sending packet: %s", errBuf);
            av_packet_unref(packet);
            continue;
        }

        while (ret >= 0) {
            ret = avcodec_receive_frame(codecCtx_, frame);
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                break;
            } else if (ret < 0) {
                char errBuf[256];
                av_strerror(ret, errBuf, sizeof(errBuf));
                LOGE("Error receiving frame: %s", errBuf);
                success = false;
                break;
            }

            if (!resampleFrame(frame, output.samples)) {
                success = false;
                break;
            }
            totalSamples += frame->nb_samples;
        }
        
        av_packet_unref(packet);
        if (!success) break;
    }

    avcodec_send_packet(codecCtx_, nullptr);
    while (true) {
        int ret = avcodec_receive_frame(codecCtx_, frame);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            break;
        } else if (ret < 0) {
            break;
        }
        
        if (!resampleFrame(frame, output.samples)) {
            success = false;
            break;
        }
        totalSamples += frame->nb_samples;
    }

    av_frame_free(&frame);
    av_packet_free(&packet);

    if (output.sampleRate > 0) {
        output.durationMs = (int64_t)(totalSamples * 1000.0 / output.sampleRate);
    }

    LOGI("Decoded %zu samples, duration: %ld ms", output.samples.size(), output.durationMs);
    return success;
}

bool FFmpegDecoder::decodeChunk(std::vector<float>& output, int maxFrames) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (!isOpen_) {
        return false;
    }

    if (!swrCtx_) {
        if (!initResampler()) {
            LOGE("Failed to initialize resampler in decodeChunk");
            return false;
        }
    }

    output.clear();
    int maxSamples = maxFrames * outputChannels_;
    
    if (!sampleBuffer_.empty()) {
        int samplesToTake = std::min((int)sampleBuffer_.size(), maxSamples);
        output.insert(output.end(), sampleBuffer_.begin(), sampleBuffer_.begin() + samplesToTake);
        sampleBuffer_.erase(sampleBuffer_.begin(), sampleBuffer_.begin() + samplesToTake);
    }

    if (output.size() >= (size_t)maxSamples) {
        return true;
    }

    AVFrame* frame = av_frame_alloc();
    AVPacket* packet = av_packet_alloc();
    
    if (!frame || !packet) {
        av_frame_free(&frame);
        av_packet_free(&packet);
        return !output.empty();
    }

    bool eof = false;
    int framesDecoded = 0;

    while (output.size() < (size_t)maxSamples && !eof) {
        int ret = av_read_frame(formatCtx_, packet);
        if (ret < 0) {
            if (ret == AVERROR_EOF) {
                avcodec_send_packet(codecCtx_, nullptr);
            }
            eof = true;
        }

        if (!eof && packet->stream_index != audioStreamIndex_) {
            av_packet_unref(packet);
            continue;
        }

        if (!eof) {
            ret = avcodec_send_packet(codecCtx_, packet);
            av_packet_unref(packet);
            if (ret < 0 && ret != AVERROR(EAGAIN)) {
                break;
            }
        }

        while (output.size() < (size_t)maxSamples) {
            ret = avcodec_receive_frame(codecCtx_, frame);
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                break;
            } else if (ret < 0) {
                av_frame_free(&frame);
                av_packet_free(&packet);
                return !output.empty();
            }

            std::vector<float> resampled;
            resampleFrame(frame, resampled);
            framesDecoded += frame->nb_samples;
            
            int samplesNeeded = maxSamples - output.size();
            int samplesToAdd = resampled.size();
            
            if (samplesToAdd <= samplesNeeded) {
                output.insert(output.end(), resampled.begin(), resampled.end());
            } else {
                output.insert(output.end(), resampled.begin(), resampled.begin() + samplesNeeded);
                sampleBuffer_.insert(sampleBuffer_.end(), resampled.begin() + samplesNeeded, resampled.end());
            }
        }
    }

    av_frame_free(&frame);
    av_packet_free(&packet);
    
    static int logCounter = 0;
    if (++logCounter >= 50) {
        logCounter = 0;
        int outputFrames = output.size() / outputChannels_;
        LOGI("decodeChunk: requested=%d frames, output=%d frames, buffered=%zu samples, inputFrames=%d",
             maxFrames, outputFrames, sampleBuffer_.size(), framesDecoded);
    }
    
    return !output.empty();
}

bool FFmpegDecoder::seekTo(int64_t positionMs) {
    std::lock_guard<std::mutex> lock(mutex_);
    
    if (!isOpen_) {
        return false;
    }

    sampleBuffer_.clear();

    int64_t timestamp = positionMs * AV_TIME_BASE / 1000;
    int ret = av_seek_frame(formatCtx_, -1, timestamp, AVSEEK_FLAG_BACKWARD);
    
    if (ret < 0) {
        char errBuf[256];
        av_strerror(ret, errBuf, sizeof(errBuf));
        LOGE("Seek failed: %s", errBuf);
        return false;
    }

    avcodec_flush_buffers(codecCtx_);
    return true;
}

void FFmpegDecoder::setOutputFormat(int sampleRate, int channels) {
    outputSampleRate_ = sampleRate;
    outputChannels_ = channels;
    
    if (isOpen_ && codecCtx_) {
        if (swrCtx_) {
            swr_free(&swrCtx_);
        }
        initResampler();
    }
}

void FFmpegDecoder::setDecodeRange(int64_t startMs, int64_t endMs) {
    rangeStart_ = startMs;
    rangeEnd_ = endMs;
}

bool FFmpegDecoder::resampleFrame(AVFrame* frame, std::vector<float>& output) {
    if (!swrCtx_) {
        return false;
    }

    int outSamples = swr_get_out_samples(swrCtx_, frame->nb_samples);
    
    double ratio = (double)outputSampleRate_ / codecCtx_->sample_rate;
    int expectedSamples = (int)(frame->nb_samples * ratio + 0.5);
    
    static int logCounter = 0;
    if (++logCounter >= 50) {
        logCounter = 0;
        LOGI("resampleFrame: inRate=%d, outRate=%d, ratio=%.4f, inSamples=%d, expectedOut=%d, swrOutSamples=%d",
             codecCtx_->sample_rate, outputSampleRate_, ratio,
             frame->nb_samples, expectedSamples, outSamples);
    }
    
    size_t currentSize = output.size();
    output.resize(currentSize + outSamples * outputChannels_);

    uint8_t* outData = reinterpret_cast<uint8_t*>(output.data() + currentSize);
    int converted = swr_convert(swrCtx_, &outData, outSamples,
                                 const_cast<const uint8_t**>(frame->data), frame->nb_samples);
    
    if (converted < 0) {
        output.resize(currentSize);
        return false;
    }

    output.resize(currentSize + converted * outputChannels_);
    return true;
}

}
