package com.bicy.whitenoise.y10p

import android.content.Context
import android.content.res.Configuration
import com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundsManifest
import com.google.gson.Gson
import java.util.Locale

object LanguageManager {
    
    private var manifestCache: SoundsManifest? = null
    private val gson = Gson()
    
    private var currentLanguage: String = "zh"
    
    fun init(context: Context) {
        currentLanguage = getSystemLanguage(context)
        loadManifest(context)
    }
    
    private fun getSystemLanguage(context: Context): String {
        val config = context.resources.configuration
        val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            config.locale
        }
        
        return when (locale.language) {
            "zh" -> "zh"
            "ja" -> "jp"
            else -> "en"
        }
    }
    
    private fun loadManifest(context: Context) {
        try {
            val json = context.assets.open("sounds_remote.json").use { 
                it.bufferedReader().use { reader -> reader.readText() }
            }
            manifestCache = gson.fromJson(json, SoundsManifest::class.java)
        } catch (e: Exception) {
            android.util.Log.e("LanguageManager", "加载语言清单失败: ${e.message}")
        }
    }
    
    fun setLanguage(language: String) {
        currentLanguage = when (language) {
            "zh_CN", "zh_TW", "zh" -> "zh"
            "ja", "jp" -> "jp"
            else -> "en"
        }
    }
    
    fun translate(key: String): String {
        val translations = manifestCache?.Language?.get(currentLanguage)
        return translations?.get(key) ?: key
    }
    
    fun translate(key: String, translations: Map<String, String>?): String {
        if (translations != null) {
            val translation = translations[currentLanguage]
            if (!translation.isNullOrEmpty()) {
                return translation
            }
            val zhTranslation = translations["zh"]
            if (!zhTranslation.isNullOrEmpty()) {
                return zhTranslation
            }
            val enTranslation = translations["en"]
            if (!enTranslation.isNullOrEmpty()) {
                return enTranslation
            }
        }
        return translate(key)
    }
    
    fun translate(key: String, context: Context): String {
        if (manifestCache == null) {
            loadManifest(context)
        }
        return translate(key)
    }
    
    fun getCurrentLanguage(): String = currentLanguage
}
