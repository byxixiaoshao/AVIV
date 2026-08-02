package com.bicy.whitenoise.floatingpet

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.storage.core.StorageManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FloatingPetInitializer {
    
    private const val TAG = "FloatingPetInitializer"
    private const val ASSETS_PATH = "Floating_Sprite_Frame"
    private const val MARKER_FILE = ".initialized"
    
    fun initialize(context: Context) {
        val petsDir = StorageManager.getFloatingPetsDir()
        if (petsDir == null) {
            Log.e(TAG, "Floating pets directory not available")
            return
        }
        
        val markerFile = File(petsDir, MARKER_FILE)
        if (markerFile.exists()) {
            Log.d(TAG, "Floating pets already initialized, skipping extraction")
            return
        }
        
        Log.d(TAG, "Initializing floating pets, extracting from assets...")
        
        try {
            val petDirs = context.assets.list(ASSETS_PATH) ?: emptyArray()
            
            for (petId in petDirs) {
                extractPet(context, petsDir, petId)
            }
            
            markerFile.createNewFile()
            markerFile.writeText("initialized_at=${System.currentTimeMillis()}")
            
            Log.d(TAG, "Floating pets initialization completed, extracted ${petDirs.size} pets")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize floating pets", e)
        }
    }
    
    private fun extractPet(context: Context, petsDir: File, petId: String) {
        val petSourcePath = "$ASSETS_PATH/$petId"
        val petDestDir = File(petsDir, petId)
        
        if (petDestDir.exists()) {
            Log.d(TAG, "Pet $petId already exists, skipping")
            return
        }
        
        petDestDir.mkdirs()
        
        try {
            val files = context.assets.list(petSourcePath) ?: emptyArray()
            
            for (fileName in files) {
                val sourcePath = "$petSourcePath/$fileName"
                
                if (isDirectory(context, sourcePath)) {
                    extractSubdirectory(context, petDestDir, sourcePath, fileName)
                } else {
                    extractFile(context, petDestDir, sourcePath, fileName)
                }
            }
            
            Log.d(TAG, "Extracted pet: $petId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract pet: $petId", e)
            petDestDir.deleteRecursively()
        }
    }
    
    private fun isDirectory(context: Context, path: String): Boolean {
        return try {
            val list = context.assets.list(path)
            list != null && list.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun extractSubdirectory(context: Context, petDestDir: File, sourcePath: String, dirName: String) {
        val subDir = File(petDestDir, dirName)
        subDir.mkdirs()
        
        try {
            val files = context.assets.list(sourcePath) ?: emptyArray()
            
            for (fileName in files) {
                val filePath = "$sourcePath/$fileName"
                extractFile(context, subDir, filePath, fileName)
            }
            
            Log.d(TAG, "Extracted subdirectory: $dirName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract subdirectory: $dirName", e)
        }
    }
    
    private fun extractFile(context: Context, destDir: File, sourcePath: String, fileName: String) {
        val destFile = File(destDir, fileName)
        
        try {
            val inputStream: InputStream = context.assets.open(sourcePath)
            val outputStream = FileOutputStream(destFile)
            
            inputStream.copyTo(outputStream)
            
            inputStream.close()
            outputStream.close()
            
            Log.d(TAG, "Extracted file: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract file: $fileName", e)
        }
    }
    
    fun isInitialized(): Boolean {
        val petsDir = StorageManager.getFloatingPetsDir()
        if (petsDir == null) return false
        
        val markerFile = File(petsDir, MARKER_FILE)
        return markerFile.exists()
    }
    
    fun forceReinitialize(context: Context) {
        val petsDir = StorageManager.getFloatingPetsDir()
        if (petsDir == null) return
        
        val markerFile = File(petsDir, MARKER_FILE)
        if (markerFile.exists()) {
            markerFile.delete()
        }
        
        initialize(context)
    }
}