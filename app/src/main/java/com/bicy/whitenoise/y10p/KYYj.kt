package com.bicy.whitenoise.y10p

import android.util.Log

object AppLog {
    
    fun d(tag: String, message: String) {
        if (LogManager.isLogEnabled()) {
            LogManager.d(tag, message)
        } else {
            Log.d(tag, message)
        }
    }
    
    fun i(tag: String, message: String) {
        if (LogManager.isLogEnabled()) {
            LogManager.i(tag, message)
        } else {
            Log.i(tag, message)
        }
    }
    
    fun w(tag: String, message: String) {
        if (LogManager.isLogEnabled()) {
            LogManager.w(tag, message)
        } else {
            Log.w(tag, message)
        }
    }
    
    fun e(tag: String, message: String) {
        if (LogManager.isLogEnabled()) {
            LogManager.e(tag, message)
        } else {
            Log.e(tag, message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable) {
        if (LogManager.isLogEnabled()) {
            LogManager.e(tag, message, throwable)
        } else {
            Log.e(tag, message, throwable)
        }
    }
    
    fun v(tag: String, message: String) {
        if (LogManager.isLogEnabled()) {
            LogManager.v(tag, message)
        } else {
            Log.v(tag, message)
        }
    }
}
