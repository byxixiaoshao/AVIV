package com.bicy.whitenoise.rgRE.uY6e

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.bicy.whitenoise.H3HO.OboeAudioEngine
import com.bicy.whitenoise.H3HO.PlaybackStateManager
import com.bicy.whitenoise.rgRE.MusicService
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.xnef.MusicPlayerController

object mS7k {
    
    private const val TAG = "MediaSessionManager"
    
    private var Y7xK: MediaSessionCompat? = null
    private var Z2pL: Context? = null
    private var Q3mN = false
    
    fun initialize(context: Context) {
        if (Q3mN) return
        
        Log.d(TAG, "Initializing MediaSession")
        Z2pL = context.applicationContext
        
        val mediaButtonIntent = Intent(context, MusicService::class.java).apply {
            action = "android.intent.action.MEDIA_BUTTON"
        }
        val mediaButtonPendingIntent = PendingIntent.getForegroundService(
            context,
            0,
            mediaButtonIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        Y7xK = MediaSessionCompat(context, "AVIVMediaSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            
            setCallback(W4vB())
            
            setMediaButtonReceiver(mediaButtonPendingIntent)
            
            val initialState = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                .build()
            setPlaybackState(initialState)
            
            isActive = true
        }
        
        Q3mN = true
        Log.d(TAG, "MediaSession initialized and activated")
    }

    fun updatePlaybackState(
        isPlaying: Boolean,
        playingCount: Int
    ) {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, 0, if (isPlaying) 1.0f else 0f)
            .build()
        
        Y7xK?.setPlaybackState(playbackState)
    }

    fun updateMetadata(
        title: String,
        artist: String = "添空",
        album: String = "",
        artwork: Bitmap? = null
    ) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
        
        artwork?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
        }
        
        Y7xK?.setMetadata(metadataBuilder.build())
    }

    fun getSessionToken(): MediaSessionCompat.Token? {
        return Y7xK?.sessionToken
    }

    fun release() {
        Y7xK?.run {
            isActive = false
            release()
        }
        Y7xK = null
        Q3mN = false
    }
    
    fun handleExternalPlayPause(play: Boolean? = null) {
        val service = MusicService.getInstance() ?: return
        val target = getControlTarget()
        Log.d(TAG, "handleExternalPlayPause: play=$play, target=$target")
        
        fun shouldPlay(): Boolean {
            if (play != null) return play
            val wnIds = PlaybackStateManager.getAllSoundIds()
            val isWnFadingOut = wnIds.any { OboeAudioEngine.isFadingOut(it) }
            return when (target) {
                ControlTarget.WHITE_NOISE -> {
                    if (isWnFadingOut) true
                    else !wnIds.any { OboeAudioEngine.isPlaying(it) }
                }
                ControlTarget.MUSIC -> !MusicPlayerController.state.value.isPlaying
                ControlTarget.ALL -> {
                    val isWnPlaying = wnIds.any { OboeAudioEngine.isPlaying(it) }
                    val isMusicPlaying = MusicPlayerController.state.value.isPlaying
                    if (isWnFadingOut) true
                    else !(isWnPlaying || isMusicPlaying)
                }
            }
        }
        
        when {
            target == ControlTarget.MUSIC -> {
                if (shouldPlay()) MusicPlayerController.play() else MusicPlayerController.pause()
            }
            target == ControlTarget.WHITE_NOISE -> {
                if (shouldPlay()) service.resumeAllSounds() else service.pauseAllSounds()
            }
            else -> {
                if (shouldPlay()) {
                    service.resumeAllSounds()
                    MusicPlayerController.play()
                } else {
                    service.pauseAllSounds()
                    MusicPlayerController.pause()
                }
            }
        }
    }
    
    fun handleExternalNext() {
        val target = getControlTarget()
        Log.d(TAG, "handleExternalNext: target=$target")
        when (target) {
            ControlTarget.MUSIC, ControlTarget.ALL -> MusicPlayerController.next()
            else -> {}
        }
    }
    
    fun handleExternalPrevious() {
        val target = getControlTarget()
        Log.d(TAG, "handleExternalPrevious: target=$target")
        when (target) {
            ControlTarget.MUSIC, ControlTarget.ALL -> MusicPlayerController.previous()
            else -> {}
        }
    }
    
    fun handleExternalStop() {
        Log.d(TAG, "handleExternalStop")
        MusicService.getInstance()?.pauseAllSounds()
        MusicPlayerController.pause()
    }
    
    fun handleMediaButton(keyEvent: android.view.KeyEvent) {
        Log.d(TAG, "handleMediaButton: keyCode=${keyEvent.keyCode}")
        when (keyEvent.keyCode) {
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> handleExternalPlayPause(true)
            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> handleExternalPlayPause(false)
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> handleExternalPlayPause()
            android.view.KeyEvent.KEYCODE_MEDIA_STOP -> handleExternalStop()
            android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> handleExternalNext()
            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> handleExternalPrevious()
        }
    }
    
    private fun isWnActuallyPlaying(): Boolean {
        return PlaybackStateManager.getAllSoundIds().any { OboeAudioEngine.isPlaying(it) }
    }
    
    private fun hasWnLoaded(): Boolean {
        return PlaybackStateManager.getAllSoundIds().any { OboeAudioEngine.isLoaded(it) }
    }
    
    private fun getControlTarget(): ControlTarget {
        val priority = ConfigStorage.getMediaControlPriority()
        val musicState = MusicPlayerController.state.value
        
        val isWnPlaying = isWnActuallyPlaying()
        val hasWn = hasWnLoaded()
        val isMusicPlaying = musicState.isPlaying && musicState.currentTrack != null
        val hasMusic = musicState.currentTrack != null
        
        Log.d(TAG, "getControlTarget: priority=$priority, isWnPlaying=$isWnPlaying, hasWn=$hasWn, isMusicPlaying=$isMusicPlaying, hasMusic=$hasMusic")
        
        return when (priority) {
            "white_noise" -> ControlTarget.WHITE_NOISE
            "music" -> ControlTarget.MUSIC
            "all" -> ControlTarget.ALL
            else -> {
                when {
                    hasWn && !hasMusic -> ControlTarget.WHITE_NOISE
                    hasMusic && !hasWn -> ControlTarget.MUSIC
                    isWnPlaying && !isMusicPlaying -> ControlTarget.WHITE_NOISE
                    isMusicPlaying && !isWnPlaying -> ControlTarget.MUSIC
                    else -> ControlTarget.ALL
                }
            }
        }
    }
    
    private enum class ControlTarget {
        WHITE_NOISE,
        MUSIC,
        ALL
    }

    private class W4vB : MediaSessionCompat.Callback() {
        
        override fun onPlay() {
            Log.d(TAG, "onPlay called")
            handleExternalPlayPause(true)
        }

        override fun onPause() {
            Log.d(TAG, "onPause called")
            handleExternalPlayPause(false)
        }

        override fun onStop() {
            Log.d(TAG, "onStop called")
            handleExternalStop()
        }

        override fun onSkipToNext() {
            Log.d(TAG, "onSkipToNext called")
            handleExternalNext()
        }

        override fun onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious called")
            handleExternalPrevious()
        }
    }
}
