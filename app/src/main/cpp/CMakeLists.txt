cmake_minimum_required(VERSION 3.22.1)
project("whitenoise")
# build identifier: registerNatives dynamic JNI

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_FLAGS "${CMAKE_CXX_STANDARD_FLAGS} -fexceptions -frtti")

set(CPP_DIR ${CMAKE_SOURCE_DIR})
set(PROJECT_ROOT ${CMAKE_SOURCE_DIR}/../../..)

set(FFMPEG_DIR ${CPP_DIR}/ffmpeg)
set(REVERB_DIR ${CPP_DIR}/reverb)
set(AUDIOPLAY_DIR ${CPP_DIR}/AudioPlayFunc)
set(AUDIOEFFECT_DIR ${CPP_DIR}/AudioEffect)
set(SPATIAL_DIR ${CPP_DIR}/spatial_audio)

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
)

target_link_libraries(whitenoise
    android
    log
    oboe
    avcodec
    avformat
    avutil
    swresample
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
