package com.bicy.whitenoise.ui.screens.SettingScreenPart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialogSimple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.storage.theme.CustomThemeLibrary
import com.bicy.whitenoise.storage.theme.ThemeScheduleManager
import com.bicy.whitenoise.ui.components.InteractiveSlider
import com.bicy.whitenoise.ui.theme.CustomTheme
import com.bicy.whitenoise.ui.theme.ThemeColorManager
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorPresets
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorScheme
import com.bicy.whitenoise.ui.theme.ThemeMode
import com.bicy.whitenoise.ui.theme.ThemeScheduleTask

/**
 * 主题模式选择对话框
 */
@Composable
fun ThemeModeSelectionDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onConfirm: (ThemeMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    
    val modes = listOf(
        Triple(ThemeMode.OFF, stringResource(R.string.theme_mode_off), stringResource(R.string.theme_mode_off_desc)),
        Triple(ThemeMode.FOLLOW_SYSTEM, stringResource(R.string.theme_mode_follow_system), stringResource(R.string.theme_mode_follow_system_desc)),
        Triple(ThemeMode.SCHEDULED, stringResource(R.string.theme_mode_scheduled), stringResource(R.string.theme_mode_scheduled_desc))
    )
    
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss,
        scrollableContent = {
            Text(
                text = stringResource(R.string.theme_mode_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column {
                modes.forEach { (mode, label, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = mode }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedMode == mode)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedMode == mode) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedMode == mode)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                            
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    if (mode != ThemeMode.SCHEDULED) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        bottomContent = {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                TextButton(
                    onClick = {
                        onConfirm(selectedMode)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

/**
 * 定时任务配置对话框
 */
@Composable
fun ScheduleConfigDialog(
    defaultThemeId: String,
    onDismiss: () -> Unit,
    onDefaultThemeChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val tasks by ThemeScheduleManager.tasks.collectAsState()
    val allThemes = CustomThemeLibrary.getAllThemesIncludingPresets()
    
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.schedule_config),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 默认主题选择
            Text(
                text = stringResource(R.string.schedule_default_theme),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(R.string.schedule_default_theme_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val defaultTheme = allThemes.find { it.id == defaultThemeId }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable {
                        // 打开主题选择对话框（这里简化处理，直接显示一个简单的选择）
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(defaultTheme?.primary ?: Color(0xFFB8A07A))
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = defaultTheme?.name ?: "Default",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 定时任务列表
            Text(
                text = stringResource(R.string.schedule_tasks),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.schedule_no_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        ScheduleTaskItem(
                            task = task,
                            onDelete = { onDeleteTask(task.id) }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 添加任务按钮
            Button(
                onClick = onAddTask,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.schedule_add_task))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 关闭按钮
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.close),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 定时任务项组件
 */
@Composable
fun ScheduleTaskItem(
    task: ThemeScheduleTask,
    onDelete: () -> Unit
) {
    val themeName = CustomThemeLibrary.getThemeName(task.themeId)
    val theme = CustomThemeLibrary.getThemeByIdIncludingPresets(task.themeId)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间范围
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${task.getStartTimeString()} - ${task.getEndTimeString()}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            if (task.isCrossDay()) {
                Text(
                    text = stringResource(R.string.schedule_cross_day),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
            
            if (task.name.isNotEmpty()) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 主题色预览
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(theme?.primary ?: Color(0xFFB8A07A))
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // 主题名称
        Text(
            text = themeName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp)
        )
        
        // 删除按钮
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 添加定时任务对话框
 */
@Composable
fun AddScheduleTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (ThemeScheduleTask) -> Unit
) {
    // 时间选择 (小时和分钟)
    var startHour by remember { mutableIntStateOf(22) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(6) }
    var endMinute by remember { mutableIntStateOf(0) }
    
    var selectedThemeId by remember { mutableStateOf(ThemeColorPresets.DefaultDark.id) }
    var hasConflict by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    
    val existingTasks by ThemeScheduleManager.tasks.collectAsState()
    
    // 计算时间分钟数
    val startTimeMinutes = startHour * 60 + startMinute
    val endTimeMinutes = endHour * 60 + endMinute
    
    // 检查时间冲突（不传递任务名称）
    val tempTask = remember(startTimeMinutes, endTimeMinutes, selectedThemeId) {
        ThemeScheduleTask(
            startTime = startTimeMinutes,
            endTime = endTimeMinutes,
            themeId = selectedThemeId,
            name = ""  // 任务不需要命名
        )
    }
    
    hasConflict = existingTasks.any { it.hasConflict(tempTask) }
    
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.add_schedule_task),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 起始时间选择
                Text(
                    text = stringResource(R.string.schedule_start_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 小时选择器
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hour",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { startHour = (startHour - 1).coerceIn(0, 23) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 16.sp)
                            }
                            
                            Text(
                                text = startHour.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Button(
                                onClick = { startHour = (startHour + 1).coerceIn(0, 23) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 16.sp)
                            }
                        }
                    }
                    
                    // 分钟选择器
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Minute",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { startMinute = (startMinute - 15).coerceIn(0, 59) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 16.sp)
                            }
                            
                            Text(
                                text = startMinute.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Button(
                                onClick = { startMinute = (startMinute + 15).coerceIn(0, 59) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 16.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 结束时间选择
                Text(
                    text = stringResource(R.string.schedule_end_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 小时选择器
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hour",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { endHour = (endHour - 1).coerceIn(0, 23) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 16.sp)
                            }
                            
                            Text(
                                text = endHour.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Button(
                                onClick = { endHour = (endHour + 1).coerceIn(0, 23) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 16.sp)
                            }
                        }
                    }
                    
                    // 分钟选择器
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Minute",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { endMinute = (endMinute - 15).coerceIn(0, 59) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 16.sp)
                            }
                            
                            Text(
                                text = endMinute.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Button(
                                onClick = { endMinute = (endMinute + 15).coerceIn(0, 59) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 16.sp)
                            }
                        }
                    }
                }
                
                // 跨天提示
                if (endTimeMinutes < startTimeMinutes) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = stringResource(R.string.schedule_cross_day_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 主题选择按钮
                Text(
                    text = stringResource(R.string.schedule_select_theme),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val selectedTheme = CustomThemeLibrary.getThemeByIdIncludingPresets(selectedThemeId)
                
                Button(
                    onClick = { showThemeColorDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(selectedTheme?.primary ?: Color(0xFFB8A07A))
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = selectedTheme?.name ?: "Default",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 时间冲突提示
                if (hasConflict) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = stringResource(R.string.schedule_time_conflict_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                TextButton(
                    onClick = {
                        if (!hasConflict) {
                            onConfirm(tempTask)
                            onDismiss()
                        }
                    },
                    enabled = !hasConflict,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = if (hasConflict)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // 主题色选择对话框
    if (showThemeColorDialog) {
        ThemeColorDialog(
            currentColorId = selectedThemeId,
            onDismiss = { showThemeColorDialog = false },
            onConfirm = { newThemeId -> selectedThemeId = newThemeId },
            onCustomColorSelected = { _, _, _, _ -> }
        )
    }
}

/**
 * 创建自定义主题对话框
 */
@Composable
fun CreateCustomThemeDialog(
    initialTheme: CustomTheme? = null,
    onDismiss: () -> Unit,
    onConfirm: (CustomTheme) -> Unit
) {
    var themeName by remember { mutableStateOf(initialTheme?.name ?: "") }
    var nameExists by remember { mutableStateOf(false) }
    
    var accentColor by remember { mutableStateOf(initialTheme?.accent?.let { Color(it) } ?: Color(0xFFB8A07A)) }
    var primaryColor by remember { mutableStateOf(initialTheme?.primary?.let { Color(it) } ?: Color(0xFFB8A07A)) }
    var backgroundColor by remember { mutableStateOf(initialTheme?.background?.let { Color(it) } ?: Color(0xFFFAF6F0)) }
    var textColor by remember { mutableStateOf(initialTheme?.text?.let { Color(it) } ?: Color(0xFF3D3A35)) }
    
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerType by remember { mutableStateOf("accent") }
    
    val colorTypeLabels = listOf(
        "accent" to stringResource(R.string.accent_color),
        "primary" to stringResource(R.string.primary_color),
        "background" to stringResource(R.string.background_color),
        "text" to stringResource(R.string.text_color)
    )
    
    // 检查名称是否已存在
    nameExists = if (themeName.isNotEmpty()) {
        CustomThemeLibrary.isThemeNameExists(themeName, initialTheme?.id)
    } else {
        false
    }
    
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = if (initialTheme != null) stringResource(R.string.edit_custom_theme)
                       else stringResource(R.string.create_custom_theme),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 主题名称输入
            OutlinedTextField(
                value = themeName,
                onValueChange = { themeName = it },
                label = { Text(stringResource(R.string.custom_theme_name)) },
                placeholder = { Text(stringResource(R.string.custom_theme_name_hint)) },
                isError = nameExists,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (nameExists) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (nameExists) MaterialTheme.colorScheme.error
                                           else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                supportingText = if (nameExists) {
                    { Text(stringResource(R.string.custom_theme_name_exists)) }
                } else null
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 预览区域
            Text(
                text = stringResource(R.string.custom_theme_preview),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.preview_text),
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 颜色类型选择
            Text(
                text = stringResource(R.string.custom_theme_colors),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                colorTypeLabels.forEach { (type, label) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                colorPickerType = type
                                showColorPicker = true
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when (type) {
                                        "accent" -> accentColor
                                        "primary" -> primaryColor
                                        "background" -> backgroundColor
                                        "text" -> textColor
                                        else -> accentColor
                                    }
                                )
                                .border(1.dp, Color.Gray, CircleShape)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                TextButton(
                    onClick = {
                        if (themeName.isNotEmpty() && !nameExists) {
                            val customTheme = CustomTheme(
                                id = initialTheme?.id ?: java.util.UUID.randomUUID().toString(),
                                name = themeName,
                                accent = accentColor.toArgb(),
                                primary = primaryColor.toArgb(),
                                background = backgroundColor.toArgb(),
                                text = textColor.toArgb(),
                                createdAt = initialTheme?.createdAt ?: System.currentTimeMillis()
                            )
                            onConfirm(customTheme)
                            onDismiss()
                        }
                    },
                    enabled = themeName.isNotEmpty() && !nameExists,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = if (themeName.isNotEmpty() && !nameExists)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
    
    // 颜色选择器对话框
    if (showColorPicker) {
        val currentColor = when (colorPickerType) {
            "accent" -> accentColor
            "primary" -> primaryColor
            "background" -> backgroundColor
            "text" -> textColor
            else -> accentColor
        }
        
        val colorLabel = colorTypeLabels.find { it.first == colorPickerType }?.second ?: "Color"
        
        SingleColorPickerDialog(
            title = colorLabel,
            initialColor = currentColor,
            onDismiss = { showColorPicker = false },
            onConfirm = { newColor ->
                when (colorPickerType) {
                    "accent" -> accentColor = Color(newColor)
                    "primary" -> primaryColor = Color(newColor)
                    "background" -> backgroundColor = Color(newColor)
                    "text" -> textColor = Color(newColor)
                }
                showColorPicker = false
            }
        )
    }
}

// 辅助函数
private fun getHue(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    return hsv[0]
}

private fun getSaturation(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    return hsv[1]
}

private fun getValue(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    return hsv[2]
}