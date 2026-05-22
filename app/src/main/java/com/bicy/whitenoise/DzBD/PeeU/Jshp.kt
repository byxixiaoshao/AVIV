package com.bicy.whitenoise.DzBD.PeeU

import android.content.Context
import com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundCategory
import com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundMetadata
import com.bicy.whitenoise.y10p.LanguageManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ItemList {
    
    private val gson = Gson()
    private var manifestCache: com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundsManifest? = null
    
    fun loadManifest(context: Context, onLoaded: (List<SoundCategory>, List<SoundMetadata>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (manifestCache == null) {
                    val json = context.assets.open("sounds_remote.json").use { 
                        it.bufferedReader().use { reader -> reader.readText() }
                    }
                    manifestCache = gson.fromJson(json, com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundsManifest::class.java)
                }
                
                withContext(Dispatchers.Main) {
                    manifestCache?.let { manifest ->
                        onLoaded(manifest.categories, manifest.sounds)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ItemList", "加载音频清单失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    onLoaded(emptyList(), emptyList())
                }
            }
        }
    }
    
    fun getManifest(context: Context): com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundsManifest? {
        if (manifestCache == null) {
            try {
                val json = context.assets.open("sounds_remote.json").use { 
                    it.bufferedReader().use { reader -> reader.readText() }
                }
                manifestCache = gson.fromJson(json, com.bicy.whitenoise.DzBD.PeeU.Q9xg.SoundsManifest::class.java)
            } catch (e: Exception) {
                android.util.Log.e("ItemList", "加载音频清单失败: ${e.message}")
            }
        }
        return manifestCache
    }
}
