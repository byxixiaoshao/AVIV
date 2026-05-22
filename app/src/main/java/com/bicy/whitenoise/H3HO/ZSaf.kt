package com.bicy.whitenoise.H3HO

import android.util.Log

class ReverbEffect {
    
    companion object {
        private const val TAG = "ReverbEffect"
        
        init {
            try {
                System.loadLibrary("whitenoise")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }
    
    private var nativePtr: Long = 0
    
    init {
        nativePtr = nativeCreate()
        if (nativePtr == 0L) {
            Log.e(TAG, "Failed to create native reverb effect")
        } else {
            Log.d(TAG, "Native reverb effect created: $nativePtr")
        }
    }
    
    fun setRoomSize(roomSize: Float) {
        if (nativePtr != 0L) {
            nativeSetRoomSize(nativePtr, roomSize)
        }
    }
    
    fun setDecayTime(decayTime: Float) {
        if (nativePtr != 0L) {
            nativeSetDecayTime(nativePtr, decayTime)
        }
    }
    
    fun setDamping(damping: Float) {
        if (nativePtr != 0L) {
            nativeSetDamping(nativePtr, damping)
        }
    }
    
    fun setWetLevel(wetLevel: Float) {
        if (nativePtr != 0L) {
            nativeSetWetLevel(nativePtr, wetLevel)
        }
    }
    
    fun setDryLevel(dryLevel: Float) {
        if (nativePtr != 0L) {
            nativeSetDryLevel(nativePtr, dryLevel)
        }
    }
    
    fun applyConfig(config: ReverbConfig) {
        setRoomSize(config.roomSize)
        setDecayTime(config.decayTime)
        setDamping(config.damping)
        setWetLevel(config.wetLevel)
        setDryLevel(config.dryLevel)
    }
    
    fun release() {
        if (nativePtr != 0L) {
            nativeDestroy(nativePtr)
            Log.d(TAG, "Native reverb effect destroyed: $nativePtr")
            nativePtr = 0
        }
    }
    
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(nativePtr: Long)
    private external fun nativeSetRoomSize(nativePtr: Long, roomSize: Float)
    private external fun nativeSetDecayTime(nativePtr: Long, decayTime: Float)
    private external fun nativeSetDamping(nativePtr: Long, damping: Float)
    private external fun nativeSetWetLevel(nativePtr: Long, wetLevel: Float)
    private external fun nativeSetDryLevel(nativePtr: Long, dryLevel: Float)
}
