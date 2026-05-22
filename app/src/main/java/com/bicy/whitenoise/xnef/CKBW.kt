package com.bicy.whitenoise.xnef

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.bicy.whitenoise.JwJY.EY9i.MusicStorage
import com.bicy.whitenoise.y10p.AudioMetadataReader

data class ScannedAudioFile(
    val uri: Uri,
    val path: String,
    val name: String,
    val extension: String,
    val length: Long,
    val lastModified: Long,
    val duration: Long = 0,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null
)

object MusicScanner {
    
    private const val TAG = "MusicScanner"
    
    private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma", "opus", "ape", "tta")
    
    private lateinit var appContext: Context
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    
    fun hasPersistedUriPermission(uri: Uri): Boolean {
        val persistedUris = appContext.contentResolver.persistedUriPermissions
        return persistedUris.any { 
            it.uri == uri && it.isReadPermission 
        }
    }
    
    fun scanAudioFiles(): List<ScannedAudioFile> {
        val startTime = System.currentTimeMillis()
        val audioFiles = mutableListOf<ScannedAudioFile>()
        val enabledDirs = MusicStorage.getEnabledDirectories()
        
        Log.d(TAG, "Scanning ${enabledDirs.size} directories using DocumentFile")
        
        for (dir in enabledDirs) {
            val uri = dir.uri
            if (uri != null && hasPersistedUriPermission(uri)) {
                scanDirectoryWithDocumentFile(uri, audioFiles)
            } else {
                Log.w(TAG, "No persisted permission for directory: ${dir.path}")
            }
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "DocumentFile scan found ${audioFiles.size} audio files in ${elapsed}ms")
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
            }
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Basic scan found ${audioFiles.size} audio files in ${elapsed}ms")
        return audioFiles
    }
    
    fun readAudioMetadata(uri: Uri): com.bicy.whitenoise.y10p.AudioMetadata? {
        return try {
            AudioMetadataReader.readFromUri(appContext, uri)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get metadata for $uri: ${e.message}")
            null
        }
    }
    
    private fun scanDirectoryWithDocumentFile(treeUri: Uri, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val pickedDir = DocumentFile.fromTreeUri(appContext, treeUri)
            if (pickedDir == null || !pickedDir.exists()) {
                Log.w(TAG, "DocumentFile does not exist: $treeUri")
                return
            }
            
            scanDocumentFileRecursive(pickedDir, audioFiles)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory with DocumentFile: $treeUri", e)
        }
    }
    
    private fun scanDirectoryBasic(treeUri: Uri, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val pickedDir = DocumentFile.fromTreeUri(appContext, treeUri)
            if (pickedDir == null || !pickedDir.exists()) {
                return
            }
            
            scanDocumentFileBasicRecursive(pickedDir, audioFiles)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory basic: $treeUri", e)
        }
    }
    
    private fun scanDocumentFileBasicRecursive(documentFile: DocumentFile, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val files = documentFile.listFiles()
            
            for (file in files) {
                if (file.isDirectory) {
                    scanDocumentFileBasicRecursive(file, audioFiles)
                } else if (file.isFile) {
                    val name = file.name ?: continue
                    val extension = name.substringAfterLast('.', "").lowercase()
                    
                    if (extension in SUPPORTED_EXTENSIONS) {
                        audioFiles.add(
                            ScannedAudioFile(
                                uri = file.uri,
                                path = getFilePathFromDocumentUri(file.uri, name),
                                name = name.substringBeforeLast('.'),
                                extension = extension,
                                length = file.length(),
                                lastModified = file.lastModified()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning DocumentFile basic: ${documentFile.uri}", e)
        }
    }
    
    private fun scanDocumentFileRecursive(documentFile: DocumentFile, audioFiles: MutableList<ScannedAudioFile>) {
        try {
            val files = documentFile.listFiles()
            
            for (file in files) {
                if (file.isDirectory) {
                    scanDocumentFileRecursive(file, audioFiles)
                } else if (file.isFile) {
                    val name = file.name ?: continue
                    val extension = name.substringAfterLast('.', "").lowercase()
                    
                    if (extension in SUPPORTED_EXTENSIONS) {
                        val uri = file.uri
                        val path = getFilePathFromDocumentUri(uri, name)
                        val metadata = getAudioMetadata(uri)
                        
                        val title = metadata?.title
                        val artist = metadata?.artist
                        val album = metadata?.album
                        
                        if (title != null || artist != null || album != null) {
                            Log.d(TAG, "Metadata for $name: title=$title, artist=$artist, album=$album")
                        }
                        
                        audioFiles.add(
                            ScannedAudioFile(
                                uri = uri,
                                path = path,
                                name = name.substringBeforeLast('.'),
                                extension = extension,
                                length = file.length(),
                                lastModified = file.lastModified(),
                                duration = metadata?.duration ?: 0,
                                title = title,
                                artist = artist,
                                album = album
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning DocumentFile: ${documentFile.uri}", e)
        }
    }
    
    private fun getAudioMetadata(uri: Uri): com.bicy.whitenoise.y10p.AudioMetadata? {
        return try {
            AudioMetadataReader.readFromUri(appContext, uri)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get metadata for $uri: ${e.message}")
            null
        }
    }
    
    private fun getFilePathFromDocumentUri(uri: Uri, fileName: String): String {
        return try {
            val docId = DocumentsContract.getDocumentId(uri)
            
            val parts = docId.split(":")
            if (parts.size >= 2) {
                val volumeId = parts[0]
                val path = parts[1]
                when (volumeId) {
                    "primary" -> "/storage/emulated/0/$path"
                    else -> "/storage/$volumeId/$path"
                }
            } else {
                uri.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file path from uri: $uri", e)
            uri.toString()
        }
    }
}
