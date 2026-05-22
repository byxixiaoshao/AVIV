package com.bicy.whitenoise.y10p

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File

data class AudioMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val duration: Long = 0,
    val sampleRate: Int = 0,
    val bitrate: Int = 0,
    val channels: Int = 0,
    val genre: String? = null,
    val year: String? = null,
    val trackNumber: String? = null,
    val albumArt: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioMetadata

        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (duration != other.duration) return false
        if (sampleRate != other.sampleRate) return false
        if (bitrate != other.bitrate) return false
        if (channels != other.channels) return false
        if (genre != other.genre) return false
        if (year != other.year) return false
        if (trackNumber != other.trackNumber) return false
        if (albumArt != null) {
            if (other.albumArt == null) return false
            if (!albumArt.contentEquals(other.albumArt)) return false
        } else if (other.albumArt != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + duration.hashCode()
        result = 31 * result + sampleRate
        result = 31 * result + bitrate
        result = 31 * result + channels
        result = 31 * result + (genre?.hashCode() ?: 0)
        result = 31 * result + (year?.hashCode() ?: 0)
        result = 31 * result + (trackNumber?.hashCode() ?: 0)
        result = 31 * result + (albumArt?.contentHashCode() ?: 0)
        return result
    }
}

object AudioMetadataReader {
    
    private const val TAG = "AudioMetadataReader"
    
    fun readFromFile(file: File): AudioMetadata? {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: ${file.absolutePath}")
            return null
        }
        
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            extractMetadata(retriever)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read metadata from file: ${file.absolutePath}", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release retriever", e)
            }
        }
    }
    
    fun readFromUri(context: Context, uri: Uri): AudioMetadata? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            extractMetadata(retriever)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read metadata from uri: $uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release retriever", e)
            }
        }
    }
    
    private fun extractMetadata(retriever: MediaMetadataRetriever): AudioMetadata {
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
        val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        val channelsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)
        val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
        val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
        val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
        
        val duration = durationStr?.toLongOrNull() ?: 0L
        val sampleRate = sampleRateStr?.toIntOrNull() ?: 0
        val bitrate = bitrateStr?.toIntOrNull() ?: 0
        val channels = channelsStr?.toIntOrNull() ?: 2
        
        Log.d(TAG, "extractMetadata: title=$title, artist=$artist, album=$album, duration=$duration")
        
        return AudioMetadata(
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            sampleRate = sampleRate,
            bitrate = bitrate,
            channels = channels,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
            albumArt = null
        )
    }
    
    fun getDuration(file: File): Long {
        return readFromFile(file)?.duration ?: 0L
    }
    
    fun getTitle(file: File): String? {
        return readFromFile(file)?.title
    }
    
    fun getAlbumArt(file: File): ByteArray? {
        return readFromFile(file)?.albumArt
    }
    
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    fun formatDurationWithHours(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
