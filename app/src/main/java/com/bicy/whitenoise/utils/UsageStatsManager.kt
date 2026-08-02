package com.bicy.whitenoise.utils

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.storage.core.JsonStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 搴旂敤浣跨敤缁熻鏁版嵁绠＄悊绫? * 璁板綍骞剁粺璁″簲鐢ㄧ殑浣跨敤鎯呭喌
 */
data class UsageStatsEntity(
    val id: String = "main",
    val usedDatesJson: String = "{}",
    val startCount: Int = 0,
    val historyStatsJson: String = "{}",
    val lastSaveTime: Long = System.currentTimeMillis()
)

object UsageStatsManager {
    private const val TAG = "UsageStatsManager"
    private const val MAX_HISTORY_DAYS = 14

    private val _stats = MutableStateFlow<UsageStats>(UsageStats())
    val stats: StateFlow<UsageStats> = _stats.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.getDefault())

    private var startTime: Long = 0L
    private var whiteNoiseStartTime: Long = 0L
    private var musicStartTime: Long = 0L
    private var timerStartTime: Long = 0L
    private var isWhiteNoisePlaying = false
    private var isMusicPlaying = false
    private var isTimerRunning = false

    data class UsageStats(
        val usedDates: Set<String> = emptySet(),
        val firstLaunchTime: Long = 0L,
        val totalWhiteNoiseDuration: Long = 0L,
        val totalMusicDuration: Long = 0L,
        val totalTimerDuration: Long = 0L,
        val timerStartCount: Int = 0,
        val todayWhiteNoiseDuration: Long = 0L,
        val todayMusicDuration: Long = 0L,
        val todayTimerDuration: Long = 0L,
        val todayDate: String = "",
        val historyStats: List<DailyStats> = emptyList()
    )

    data class DailyStats(
        val date: String,
        val whiteNoiseDuration: Long,
        val musicDuration: Long,
        val timerDuration: Long
    )

    fun init(context: Context) {
        loadStats()
        checkAndUpdateDailyStats()
        startTime = System.currentTimeMillis()
        if (_stats.value.firstLaunchTime == 0L) {
            updateFirstLaunchTime(startTime)
        }
        recordUsedDate(dateFormat.format(Date(startTime)))
        Log.d(TAG, "UsageStatsManager initialized")
    }

    private fun loadStats() {
        try {
            val entity = runBlocking {
                JsonStorageManager.read("usage_stats.json", UsageStatsEntity::class.java)
            }
            if (entity != null) {
                val usedDates = mutableSetOf<String>()
                val usedDatesArray = JSONArray(entity.usedDatesJson)
                for (i in 0 until usedDatesArray.length()) {
                    usedDates.add(usedDatesArray.getString(i))
                }

                val extraJson = JSONObject(entity.historyStatsJson)
                val historyArray = extraJson.optJSONArray("historyStats") ?: JSONArray()
                val historyStats = mutableListOf<DailyStats>()
                for (i in 0 until historyArray.length()) {
                    val item = historyArray.getJSONObject(i)
                    historyStats.add(DailyStats(
                        date = item.getString("date"),
                        whiteNoiseDuration = item.getLong("whiteNoiseDuration"),
                        musicDuration = item.getLong("musicDuration"),
                        timerDuration = item.getLong("timerDuration")
                    ))
                }

                _stats.value = UsageStats(
                    usedDates = usedDates,
                    firstLaunchTime = extraJson.optLong("firstLaunchTime", 0L),
                    totalWhiteNoiseDuration = extraJson.optLong("totalWhiteNoiseDuration", 0L),
                    totalMusicDuration = extraJson.optLong("totalMusicDuration", 0L),
                    totalTimerDuration = extraJson.optLong("totalTimerDuration", 0L),
                    timerStartCount = entity.startCount,
                    todayWhiteNoiseDuration = extraJson.optLong("todayWhiteNoiseDuration", 0L),
                    todayMusicDuration = extraJson.optLong("todayMusicDuration", 0L),
                    todayTimerDuration = extraJson.optLong("todayTimerDuration", 0L),
                    todayDate = extraJson.optString("todayDate", ""),
                    historyStats = historyStats
                )
                Log.d(TAG, "Stats loaded: usedDays=${usedDates.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load stats", e)
        }
    }

    private fun saveStats() {
        try {
            val extraJson = JSONObject().apply {
                put("firstLaunchTime", _stats.value.firstLaunchTime)
                put("totalWhiteNoiseDuration", _stats.value.totalWhiteNoiseDuration)
                put("totalMusicDuration", _stats.value.totalMusicDuration)
                put("totalTimerDuration", _stats.value.totalTimerDuration)
                put("todayWhiteNoiseDuration", _stats.value.todayWhiteNoiseDuration)
                put("todayMusicDuration", _stats.value.todayMusicDuration)
                put("todayTimerDuration", _stats.value.todayTimerDuration)
                put("todayDate", _stats.value.todayDate)
                put("historyStats", JSONArray(_stats.value.historyStats.map { stat ->
                    JSONObject().apply {
                        put("date", stat.date)
                        put("whiteNoiseDuration", stat.whiteNoiseDuration)
                        put("musicDuration", stat.musicDuration)
                        put("timerDuration", stat.timerDuration)
                    }
                }))
            }.toString()
            
            val entity = UsageStatsEntity(
                id = "main",
                usedDatesJson = JSONArray(_stats.value.usedDates).toString(),
                startCount = _stats.value.timerStartCount,
                historyStatsJson = extraJson,
                lastSaveTime = System.currentTimeMillis()
            )
            runBlocking {
                JsonStorageManager.write("usage_stats.json", entity)
            }
            Log.d(TAG, "Stats saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save stats", e)
        }
    }

    private fun updateFirstLaunchTime(time: Long) {
        _stats.value = _stats.value.copy(firstLaunchTime = time)
        saveStats()
    }

    private fun recordUsedDate(date: String) {
        if (!_stats.value.usedDates.contains(date)) {
            _stats.value = _stats.value.copy(usedDates = _stats.value.usedDates + date)
            saveStats()
        }
    }

    private fun checkAndUpdateDailyStats() {
        val today = dateFormat.format(Date())
        val currentToday = _stats.value.todayDate

        if (currentToday.isEmpty() || currentToday != today) {
            if (currentToday.isNotEmpty()) {
                addToHistory(currentToday)
            }
            _stats.value = _stats.value.copy(
                todayDate = today,
                todayWhiteNoiseDuration = 0L,
                todayMusicDuration = 0L,
                todayTimerDuration = 0L
            )
            trimHistory()
            saveStats()
        }
    }

    private fun addToHistory(date: String) {
        val newHistory = _stats.value.historyStats.toMutableList()
        newHistory.add(DailyStats(
            date = date,
            whiteNoiseDuration = _stats.value.todayWhiteNoiseDuration,
            musicDuration = _stats.value.todayMusicDuration,
            timerDuration = _stats.value.todayTimerDuration
        ))
        _stats.value = _stats.value.copy(historyStats = newHistory)
    }

    private fun trimHistory() {
        if (_stats.value.historyStats.size > MAX_HISTORY_DAYS) {
            _stats.value = _stats.value.copy(
                historyStats = _stats.value.historyStats.takeLast(MAX_HISTORY_DAYS)
            )
        }
    }

    fun onWhiteNoiseStart() {
        whiteNoiseStartTime = System.currentTimeMillis()
        isWhiteNoisePlaying = true
        Log.d(TAG, "White noise started")
    }

    fun onWhiteNoiseStop() {
        if (isWhiteNoisePlaying && whiteNoiseStartTime > 0) {
            val duration = System.currentTimeMillis() - whiteNoiseStartTime
            _stats.value = _stats.value.copy(
                totalWhiteNoiseDuration = _stats.value.totalWhiteNoiseDuration + duration,
                todayWhiteNoiseDuration = _stats.value.todayWhiteNoiseDuration + duration
            )
            saveStats()
            Log.d(TAG, "White noise stopped, duration=${duration}ms")
        }
        isWhiteNoisePlaying = false
        whiteNoiseStartTime = 0L
    }

    fun onMusicStart() {
        musicStartTime = System.currentTimeMillis()
        isMusicPlaying = true
        Log.d(TAG, "Music started")
    }

    fun onMusicStop() {
        if (isMusicPlaying && musicStartTime > 0) {
            val duration = System.currentTimeMillis() - musicStartTime
            _stats.value = _stats.value.copy(
                totalMusicDuration = _stats.value.totalMusicDuration + duration,
                todayMusicDuration = _stats.value.todayMusicDuration + duration
            )
            saveStats()
            Log.d(TAG, "Music stopped, duration=${duration}ms")
        }
        isMusicPlaying = false
        musicStartTime = 0L
    }

    fun onTimerStart() {
        timerStartTime = System.currentTimeMillis()
        isTimerRunning = true
        _stats.value = _stats.value.copy(timerStartCount = _stats.value.timerStartCount + 1)
        saveStats()
        Log.d(TAG, "Timer started")
    }

    fun onTimerStop(isSnooze: Boolean = false) {
        if (isTimerRunning && timerStartTime > 0) {
            val duration = System.currentTimeMillis() - timerStartTime
            _stats.value = _stats.value.copy(
                totalTimerDuration = _stats.value.totalTimerDuration + duration,
                todayTimerDuration = _stats.value.todayTimerDuration + duration
            )
            saveStats()
            Log.d(TAG, "Timer stopped, duration=${duration}ms, isSnooze=$isSnooze")
        }
        isTimerRunning = false
        timerStartTime = 0L
    }

    fun onAppExit() {
        val exitTime = System.currentTimeMillis()
        if (isWhiteNoisePlaying) onWhiteNoiseStop()
        if (isMusicPlaying) onMusicStop()
        if (isTimerRunning) onTimerStop()
        recordUsedDate(dateFormat.format(Date(exitTime)))
        Log.d(TAG, "App exiting")
    }

    fun getUsedDaysCount(): Int = _stats.value.usedDates.size

    fun getTotalRunDuration(): Long {
        return if (_stats.value.firstLaunchTime > 0) {
            System.currentTimeMillis() - _stats.value.firstLaunchTime
        } else 0L
    }

    fun formatDuration(millis: Long): String {
        return timeFormat.format(Date(millis))
    }

    fun formatDurationFromStart(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d-%02d-%02d %02d:%02d:%02d", 
            days / 365, days % 365 / 30, days % 30, hours, minutes, seconds)
    }

    fun formatDurationSimple(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun getHistoryStats(): List<DailyStats> = _stats.value.historyStats

    fun getStatsSnapshot(): UsageStats = _stats.value
}
