package com.bicy.whitenoise.yODW.NvYq.BxAd

import android.content.Context
import android.net.Uri
import androidx.compose.ui.unit.dp

val ContentPaddingTop = 8.dp

fun getFullPathFromUri(context: Context, uri: Uri): String? {
    return try {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        val parts = docId.split(":")
        if (parts.size >= 2) {
            val volumeId = parts[0]
            val path = parts[1]
            when (volumeId) {
                "primary" -> "/storage/emulated/0/$path"
                else -> "/storage/$volumeId/$path"
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
