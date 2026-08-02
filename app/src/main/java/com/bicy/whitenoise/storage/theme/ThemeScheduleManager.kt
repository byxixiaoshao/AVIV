package com.bicy.whitenoise.storage.theme

import android.content.Context
import android.util.Log
import com.bicy.whitenoise.ui.theme.ThemeScheduleTask
import com.bicy.whitenoise.utils.AppInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

/**
 * 定时主题任务管理器
 * 负责管理定时主题切换任务
 */
object ThemeScheduleManager {
    
    private const val TAG = "ThemeScheduleManager"
    private const val FILE_NAME = "theme_schedules.json"
    
    private lateinit var context: Context
    private lateinit var file: File
    
    private val _tasks = MutableStateFlow<List<ThemeScheduleTask>>(emptyList())
    val tasks: StateFlow<List<ThemeScheduleTask>> = _tasks.asStateFlow()
    
    /**
     * 初始化
     */
    fun init(context: Context) {
        this.context = context
        file = File(context.filesDir, FILE_NAME)
        loadTasks()
        Log.d(TAG, "ThemeScheduleManager initialized with ${_tasks.value.size} tasks")
    }
    
    /**
     * 异步初始化
     */
    suspend fun initAsync(context: Context) {
        withContext(Dispatchers.IO) {
            init(context)
        }
    }
    
    /**
     * 加载定时任务
     */
    private fun loadTasks() {
        try {
            if (!file.exists()) {
                Log.d(TAG, "Theme schedules file not exists, creating empty list")
                _tasks.value = emptyList()
                saveTasks()
                return
            }
            
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val taskList = mutableListOf<ThemeScheduleTask>()
            
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val task = ThemeScheduleTask(
                    id = json.getString("id"),
                    startTime = json.getInt("startTime"),
                    endTime = json.getInt("endTime"),
                    themeId = json.getString("themeId"),
                    name = json.optString("name", "")
                )
                taskList.add(task)
            }
            
            _tasks.value = taskList.sortedBy { it.startTime }
            Log.d(TAG, "Loaded ${taskList.size} theme schedule tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load theme schedules", e)
            _tasks.value = emptyList()
        }
    }
    
    /**
     * 保存定时任务
     */
    private fun saveTasks() {
        try {
            val jsonArray = JSONArray()
            _tasks.value.forEach { task ->
                val json = JSONObject()
                json.put("id", task.id)
                json.put("startTime", task.startTime)
                json.put("endTime", task.endTime)
                json.put("themeId", task.themeId)
                json.put("name", task.name)
                jsonArray.put(json)
            }
            
            file.writeText(jsonArray.toString())
            Log.d(TAG, "Saved ${_tasks.value.size} theme schedule tasks")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save theme schedules", e)
        }
    }
    
    /**
     * 获取所有定时任务
     */
    fun getAllTasks(): List<ThemeScheduleTask> = _tasks.value
    
    /**
     * 通过 ID 获取定时任务
     */
    fun getTaskById(id: String): ThemeScheduleTask? {
        return _tasks.value.find { it.id == id }
    }
    
    /**
     * 添加定时任务
     * @return 是否添加成功
     */
    fun addTask(task: ThemeScheduleTask): Boolean {
        val currentList = _tasks.value.toMutableList()
        
        // 检查时间冲突
        val hasConflict = currentList.any { existingTask ->
            task.hasConflict(existingTask)
        }
        
        if (hasConflict) {
            Log.w(TAG, "New task has time conflict with existing tasks")
            return false
        }
        
        currentList.add(task)
        _tasks.value = currentList.sortedBy { it.startTime }
        saveTasks()
        Log.d(TAG, "Added theme schedule task: ${task.name}")
        return true
    }
    
    /**
     * 更新定时任务
     * @return 是否更新成功
     */
    fun updateTask(task: ThemeScheduleTask): Boolean {
        val currentList = _tasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == task.id }
        
        if (index == -1) {
            Log.w(TAG, "Task with id '${task.id}' not found")
            return false
        }
        
        // 检查更新后的时间冲突(排除自己)
        val hasConflict = currentList.filter { it.id != task.id }.any { existingTask ->
            task.hasConflict(existingTask)
        }
        
        if (hasConflict) {
            Log.w(TAG, "Updated task has time conflict with existing tasks")
            return false
        }
        
        currentList[index] = task
        _tasks.value = currentList.sortedBy { it.startTime }
        saveTasks()
        Log.d(TAG, "Updated theme schedule task: ${task.name}")
        return true
    }
    
    /**
     * 删除定时任务
     */
    fun deleteTask(id: String) {
        val currentList = _tasks.value.toMutableList()
        val removed = currentList.removeIf { it.id == id }
        
        if (removed) {
            _tasks.value = currentList
            saveTasks()
            Log.d(TAG, "Deleted theme schedule task: $id")
        } else {
            Log.w(TAG, "Task with id '$id' not found for deletion")
        }
    }
    
    /**
     * 检查新任务是否与现有任务冲突
     */
    fun checkTaskConflict(task: ThemeScheduleTask, excludeId: String? = null): Boolean {
        return _tasks.value
            .filter { it.id != excludeId }
            .any { existingTask -> task.hasConflict(existingTask) }
    }
    
    /**
     * 根据当前时间获取应该使用的主题 ID
     * @param currentTime 当前时间(分钟数),null 则使用系统当前时间
     * @return 主题 ID,如果没有匹配的任务则返回 null
     */
    fun getCurrentThemeId(currentTime: Int? = null): String? {
        val time = currentTime ?: getCurrentTimeInMinutes()
        
        // 遍历所有任务,找到当前时间匹配的任务
        // 按照起始时间排序,优先匹配最早的任务
        for (task in _tasks.value) {
            if (task.isTimeInRange(time)) {
                return task.themeId
            }
        }
        
        return null
    }
    
    /**
     * 获取当前时间的分钟数
     */
    fun getCurrentTimeInMinutes(): Int {
        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR_OF_DAY)
        val minutes = calendar.get(Calendar.MINUTE)
        return hours * 60 + minutes
    }
    
    /**
     * 获取当前匹配的任务
     */
    fun getCurrentTask(): ThemeScheduleTask? {
        val time = getCurrentTimeInMinutes()
        return _tasks.value.find { it.isTimeInRange(time) }
    }
    
    /**
     * 清空所有定时任务
     */
    fun clearAllTasks() {
        _tasks.value = emptyList()
        saveTasks()
        Log.d(TAG, "Cleared all theme schedule tasks")
    }
    
    /**
     * 获取任务摘要信息
     */
    fun getTaskSummary(task: ThemeScheduleTask): String {
        val themeName = CustomThemeLibrary.getThemeName(task.themeId)
        return "${task.getStartTimeString()} - ${task.getEndTimeString()} ($themeName)"
    }
}