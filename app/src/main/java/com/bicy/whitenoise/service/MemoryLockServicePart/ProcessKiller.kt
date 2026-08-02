package com.bicy.whitenoise.service.MemoryLockServicePart

import android.util.Log

object ProcessKiller {
    fun kill(reason: String) {
        Log.e("MemoryLock", "正在终止进程... 原因: $reason")
        android.os.Process.killProcess(android.os.Process.myPid())
        Thread.sleep(200)
        System.exit(0)
    }
}
