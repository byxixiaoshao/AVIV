package com.bicy.whitenoise

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bicy.whitenoise.xnef.MusicLibrary
import com.bicy.whitenoise.xnef.MusicPlayerController
import com.bicy.whitenoise.rgRE.MusicService
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.yODW.MainScreen
import com.bicy.whitenoise.yODW.ZFNn.WhiteNoiseThemeWithPremium
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private var musicService: MusicService? = null
    private var isServiceBound = false
    private var isContentSet = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            isServiceBound = true
            restorePlaybackState()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isServiceBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupWindowInsets()
        
        SplashActivity.onReadyCallback = {
            runOnUiThread {
                loadContent()
                bindMusicService()
            }
        }
        
        val splashIntent = Intent(this, SplashActivity::class.java)
        startActivity(splashIntent)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
    
    private fun loadContent() {
        if (isContentSet) return
        isContentSet = true
        
        lifecycleScope.launch {
            MusicLibrary.performIncrementalScan()
        }
        
        setContent {
            val globalState by ConfigStorage.config.collectAsState()
            
            WhiteNoiseThemeWithPremium(isPremiumUser = globalState.isPremium) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    private fun setupWindowInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enableEdgeToEdge()
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        musicService?.onAppResume()
    }
    
    override fun onPause() {
        super.onPause()
        musicService?.onAppPause()
        MusicPlayerController.saveCurrentPlaybackState()
    }
    
    override fun onStop() {
        super.onStop()
        MusicPlayerController.saveCurrentPlaybackState()
    }
    
    private fun bindMusicService() {
        val serviceIntent = Intent(this, MusicService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun restorePlaybackState() {
        if (WhiteNoiseStorage.getPlaybackState().sounds.isNotEmpty()) {
            com.bicy.whitenoise.JwJY.sBYh.kcFp.PlaybackRestorer.restorePlaybackState()
        }
        
        lifecycleScope.launch {
            var retryCount = 0
            while (MusicLibrary.tracks.value.isEmpty() && retryCount < 50) {
                kotlinx.coroutines.delay(100)
                retryCount++
            }
            
            if (MusicLibrary.tracks.value.isNotEmpty()) {
                val restored = MusicPlayerController.restoreLastPlayback()
                Log.d("MainActivity", "音乐播放状态恢复: $restored")
            } else {
                Log.w("MainActivity", "音乐库为空，无法恢复播放状态")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        MusicPlayerController.saveCurrentPlaybackState()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }
}
