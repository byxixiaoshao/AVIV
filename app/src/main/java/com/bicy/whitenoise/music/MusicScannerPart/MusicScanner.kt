package com.bicy.whitenoise.music.MusicScannerPart

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.bicy.whitenoise.storage.music.MusicStorage
import com.bicy.whitenoise.utils.AudioMetadataReader
import java.io.File

object MusicScanner {
    
    private const val TAG = "MusicScanner"
    
    private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma", "opus", "ape", "tta")
    
    private lateinit var appContext: Context
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    fun hasPersistedUriPermission(uri: Uri): Boolean {
        val persistedUris = appContext.contentResolver.persistedUriPermissions
        return persistedUris.any { it.uri == uri && it.isReadPermission }
    }
    
    fun scanAudioFiles(): List<ScannedAudioFile> {
        val startTime = System.currentTimeMillis()
        val audioFiles = mutableListOf<ScannedAudioFile>()
        val enabledDirs = MusicStorage.getEnabledDirectories()
        Log.d(TAG, "Scanning ${enabledDirs.size} directories")
        for (dir in enabledDirs) {
            val uri = dir.uri
            if (uri != null && hasPersistedUriPermission(uri)) {
                scanDirectoryWithDocumentFile(uri, audioFiles)
            } else {
                // 没有 SAF URI 的目录（如默认 music 目录），使用 File API 回退扫描
                scanDirectoryWithFileApi(dir.path, audioFiles)
            }
        }
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Scan found ${audioFiles.size} audio files in ${elapsed}ms")
        return audioFiles
    }
    
    fun scanAudioFilesBasic(): List<ScannedAudioFile> {
        val startTime = System.currentTimeMillis()
        val audioFiles = mutableListOf<ScannedAudioFile>()
        val enabledDirs = MusicStorage.getEnabledDirectories()
        for (dir in enabledDirs) {
            val uri = dir.uri
            if (uri != null && hasPersistedUriPermission(uri)) {
                scanDirectoryBasic(uri, audioFiles)
            } else {
                scanDirectoryBasicWithFileApi(dir.path, audioFiles)
            }
        }
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Basic scan found ${audioFiles.size} audio files in ${elapsed}ms")
        return audioFiles
    }
    
    /** 使用 File API 回退扫描（用于无 SAF URI 的目录，如默认 Music 目录） */
    private fun scanDirectoryWithFileApi(dirPath: String, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                Log.w(TAG, "File API: directory not found or not a directory: $dirPath")
                return
            }
            scanFileRecursive(dir, audioFiles, withMetadata = true)
        } catch (e: Exception) {
            Log.e(TAG, "File API: error scanning $dirPath", e)
        }
    }
    
    private fun scanDirectoryBasicWithFileApi(dirPath: String, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) return
            scanFileRecursive(dir, audioFiles, withMetadata = false)
        } catch (e: Exception) {
            Log.e(TAG, "File API: error basic scanning $dirPath", e)
        }
    }
    
    private fun scanFileRecursive(dir: File, audioFiles: MutableList<ScannedAudioFile>, withMetadata: Boolean) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanFileRecursive(file, audioFiles, withMetadata)
            } else if (file.isFile) {
                val extension = file.extension.lowercase()
                if (extension in SUPPORTED_EXTENSIONS) {
                    val uri = Uri.fromFile(file)
                    val name = file.name
                    if (withMetadata) {
                        val metadata = getAudioMetadata(uri)
                        audioFiles.add(ScannedAudioFile(
                            uri = uri, path = file.absolutePath,
                            name = name.substringBeforeLast('.'),
                            extension = extension, length = file.length(),
                            lastModified = file.lastModified(),
                            duration = metadata?.duration ?: 0,
                            title = metadata?.title, artist = metadata?.artist,
                            album = metadata?.album
                        ))
                    } else {
                        audioFiles.add(ScannedAudioFile(
                            uri = uri, path = file.absolutePath,
                            name = name.substringBeforeLast('.'),
                            extension = extension, length = file.length(),
                            lastModified = file.lastModified()
                        ))
                    }
                }
            }
        }
    }
    
    fun readAudioMetadata(uri: Uri): com.bicy.whitenoise.utils.AudioMetadata? {
        return try { AudioMetadataReader.readFromUri(appContext, uri) } catch (e: Exception) { Log.w(TAG, "Failed to get metadata for $uri: ${e.message}"); null }
    }
    
    private fun scanDirectoryWithDocumentFile(treeUri: Uri, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val pickedDir = DocumentFile.fromTreeUri(appContext, treeUri)
            if (pickedDir == null || !pickedDir.exists()) { Log.w(TAG, "DocumentFile does not exist: $treeUri"); return }
            scanDocumentFileRecursive(pickedDir, audioFiles)
        } catch (e: Exception) { Log.e(TAG, "Error scanning directory with DocumentFile: $treeUri", e) }
    }
    
    private fun scanDirectoryBasic(treeUri: Uri, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val pickedDir = DocumentFile.fromTreeUri(appContext, treeUri)
            if (pickedDir == null || !pickedDir.exists()) return
            scanDocumentFileBasicRecursive(pickedDir, audioFiles)
        } catch (e: Exception) { Log.e(TAG, "Error scanning directory basic: $treeUri", e) }
    }
    
    private fun scanDocumentFileBasicRecursive(documentFile: DocumentFile, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val files = documentFile.listFiles()
            for (file in files) {
                if (file.isDirectory) { scanDocumentFileBasicRecursive(file, audioFiles) }
                else if (file.isFile) {
                    val name = file.name ?: continue
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (extension in SUPPORTED_EXTENSIONS) {
                        audioFiles.add(ScannedAudioFile(uri = file.uri, path = getFilePathFromDocumentUri(file.uri, name),
                            name = name.substringBeforeLast('.'), extension = extension, length = file.length(), lastModified = file.lastModified()))
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Error scanning DocumentFile basic: ${documentFile.uri}", e) }
    }
    
    private fun scanDocumentFileRecursive(documentFile: DocumentFile, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val files = documentFile.listFiles()
            for (file in files) {
                if (file.isDirectory) { scanDocumentFileRecursive(file, audioFiles) }
                else if (file.isFile) {
                    val name = file.name ?: continue
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (extension in SUPPORTED_EXTENSIONS) {
                        val uri = file.uri
                        val path = getFilePathFromDocumentUri(uri, name)
                        val metadata = getAudioMetadata(uri)
                        val title = metadata?.title; val artist = metadata?.artist; val album = metadata?.album
                        if (title != null || artist != null || album != null) Log.d(TAG, "Metadata for $name: title=$title, artist=$artist, album=$album")
                        audioFiles.add(ScannedAudioFile(uri = uri, path = path, name = name.substringBeforeLast('.'),
                            extension = extension, length = file.length(), lastModified = file.lastModified(),
                            duration = metadata?.duration ?: 0, title = title, artist = artist, album = album))
                    }
                }
            }
        } catch (e: Exception) { Log.e(TAG, "Error scanning DocumentFile: ${documentFile.uri}", e) }
    }
    
    private fun getAudioMetadata(uri: Uri): com.bicy.whitenoise.utils.AudioMetadata? {
        return try { AudioMetadataReader.readFromUri(appContext, uri) } catch (e: Exception) { Log.w(TAG, "Failed to get metadata for $uri: ${e.message}"); null }
    }
    
    private fun getFilePathFromDocumentUri(uri: Uri, fileName: String): String {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            val parts = docId.split(":")
            if (parts.size >= 2) {
                val volumeId = parts[0]; val path = parts[1]
                when (volumeId) { "primary" -> "/storage/emulated/0/$path"; else -> "/storage/$volumeId/$path" }
            } else uri.toString()
        } catch (e: Exception) { Log.e(TAG, "Error getting file path from uri: $uri", e); uri.toString() }
    }
}
