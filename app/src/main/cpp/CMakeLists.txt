cmake_minimum_required(VERSION 3.22.1)
project("whitenoise")
# build identifier: registerNatives dynamic JNI

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_FLAGS "${CMAKE_CXX_STANDARD_FLAGS} -fexceptions -frtti")

set(CPP_DIR ${CMAKE_SOURCE_DIR})
set(PROJECT_ROOT ${CMAKE_SOURCE_DIR}/../../../../..)

set(FFMPEG_DIR ${CPP_DIR}/ffmpeg)
set(REVERB_DIR ${CPP_DIR}/reverb)
set(AUDIOPLAY_DIR ${CPP_DIR}/AudioPlayFunc)
set(AUDIOEFFECT_DIR ${CPP_DIR}/AudioEffect)
set(SPATIAL_DIR ${CPP_DIR}/spatial_audio)

# SoundTouch (LGPL v2.1) - time-stretch / pitch-shift，源码自包含编译
set(SOUNDTOUCH_DIR ${PROJECT_ROOT}/soundtouch)
set(SOUNDTOUCH_SRC_DIR ${SOUNDTOUCH_DIR}/source/SoundTouch)

add_library(oboe SHARED IMPORTED)
set_target_properties(oboe PROPERTIES
    IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/liboboe.so
)

set(FFMPEG_INCLUDE_DIR ${FFMPEG_DIR}/include)
set(FFMPEG_LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES
    IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libavcodec.so
)

add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES
    IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libavformat.so
)

add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES
    IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libavutil.so
)

add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES
    IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libswresample.so
)

add_library(whitenoise SHARED
    ${FFMPEG_DIR}/FFmpegDecoder.cpp
    ${REVERB_DIR}/ReverbEffect.cpp
    ${AUDIOPLAY_DIR}/AudioTrack.cpp
    ${AUDIOPLAY_DIR}/AudioEngine.cpp
    ${CPP_DIR}/JniInterface.cpp
    ${SPATIAL_DIR}/SpatialAudioProcessor.cpp
)

target_include_directories(whitenoise PRIVATE
    ${CPP_DIR}
    ${CPP_DIR}/oboe
    ${FFMPEG_INCLUDE_DIR}
    ${REVERB_DIR}
    ${AUDIOPLAY_DIR}
    ${AUDIOEFFECT_DIR}
    ${SPATIAL_DIR}
    ${SOUNDTOUCH_DIR}/include
)

# SoundTouch (LGPL v2.1) 静态库：自包含编译，仅纳入 time-stretch/pitch 所需源文件。
# 不编译 BPMDetect.cpp（不需要 BPM 检测）、mmx_optimized.cpp / sse_optimized.cpp
#（x86 专用，ARM 下由 SOUNDTOUCH_ALLOW_X86_OPTIMIZATIONS 宏守卫为空翻译单元）。
# 注意：cpu_detect_x86.cpp 必须编译——它提供 detectCPUextensions() 符号，
# TDStretch.cpp / FIRFilter.cpp 会引用；在 ARM 上该函数返回 0（无扩展）。
add_library(soundtouch STATIC
    ${SOUNDTOUCH_SRC_DIR}/AAFilter.cpp
    ${SOUNDTOUCH_SRC_DIR}/cpu_detect_x86.cpp
    ${SOUNDTOUCH_SRC_DIR}/FIFOSampleBuffer.cpp
    ${SOUNDTOUCH_SRC_DIR}/FIRFilter.cpp
    ${SOUNDTOUCH_SRC_DIR}/InterpolateCubic.cpp
    ${SOUNDTOUCH_SRC_DIR}/InterpolateLinear.cpp
    ${SOUNDTOUCH_SRC_DIR}/InterpolateShannon.cpp
    ${SOUNDTOUCH_SRC_DIR}/PeakFinder.cpp
    ${SOUNDTOUCH_SRC_DIR}/RateTransposer.cpp
    ${SOUNDTOUCH_SRC_DIR}/SoundTouch.cpp
    ${SOUNDTOUCH_SRC_DIR}/TDStretch.cpp
)
target_include_directories(soundtouch PUBLIC ${SOUNDTOUCH_DIR}/include)
target_compile_definitions(soundtouch PRIVATE SOUNDTOUCH_FLOAT_SAMPLES)
# ARM NEON 优化：arm64-v8a / armeabi-v7a 均启用 NEON SIMD
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(soundtouch PRIVATE SOUNDTOUCH_USE_NEON)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_definitions(soundtouch PRIVATE SOUNDTOUCH_USE_NEON)
    target_compile_options(soundtouch PRIVATE -mfpu=neon)
endif()
# 允许 SoundTouch 自身 autovectorization 优化
target_compile_options(soundtouch PRIVATE -O3 -ffast-math)

target_link_libraries(whitenoise
    android
    log
    oboe
    avcodec
    avformat
    avutil
    swresample
    soundtouch
)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(whitenoise PRIVATE -O3 -ffast-math)
    target_link_options(whitenoise PRIVATE 
        "-Wl,--no-warn-mismatch"
        "-Wl,-z,max-page-size=16384"
    )
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(whitenoise PRIVATE -O3 -ffast-math -mfpu=neon)
endif()
