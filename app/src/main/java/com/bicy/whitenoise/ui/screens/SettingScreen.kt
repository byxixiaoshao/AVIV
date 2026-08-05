package com.bicy.whitenoise.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialogSimple
import com.bicy.whitenoise.ui.components.toast.ToastManager
import com.bicy.whitenoise.ui.components.ExpandableNavBarPart.BottomNavTotalHeight
import com.bicy.whitenoise.storage.config.GlassScopeConfig
import com.bicy.whitenoise.storage.config.FrostedGlassConfig
import com.bicy.whitenoise.storage.config.FrostedGlassScopeConfig
import com.bicy.whitenoise.storage.config.BlurBackdropConfig
import com.bicy.whitenoise.storage.config.BackgroundGlassConfig
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bicy.whitenoise.R
import com.bicy.whitenoise.storage.music.MusicStorage
import com.bicy.whitenoise.music.MusicLibraryPart.MusicLibrary
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.servies.MusicService
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.storage.config.LiquidGlassConfig
import com.bicy.whitenoise.storage.config.GlassRenderConfig
import com.bicy.whitenoise.storage.config.NavBackgroundConfig
import com.bicy.whitenoise.ui.components.glass.GlassMode
import com.bicy.whitenoise.ui.screens.SettingScreenPart.AboutDialog
import com.bicy.whitenoise.ui.PageTopPadding
import com.bicy.whitenoise.ui.screens.SettingScreenPart.EffectOrderDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.MediaControlPriorityDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.MusicDirectoryDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingCategorySection
import com.bicy.whitenoise.ui.screens.SettingScreenPart.VisualizationConfigDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingClickItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingClickItemWithIcon
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingSliderItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingFloatSliderItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingSliderWithCheckboxItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingSwitchItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingSegmentedControlItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SingleColorPickerDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.DonationDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.ThemeColorDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.UserLevelCard
import com.bicy.whitenoise.ui.screens.SettingScreenPart.getFullPathFromUri
import com.bicy.whitenoise.ui.screens.SettingScreenPart.GitHubRepoButton
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingTextItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingTextItemWithButton
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingTextFieldItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingSliderWithTextFieldItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.SettingSliderWithTextItem
import com.bicy.whitenoise.ui.screens.SettingScreenPart.UsageHistoryDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.HistoryType
import com.bicy.whitenoise.ui.screens.SettingScreenPart.ThemeModeSelectionDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.ScheduleConfigDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.AddScheduleTaskDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.CreateCustomThemeDialog
import com.bicy.whitenoise.ui.theme.ThemeColorManager
import com.bicy.whitenoise.ui.theme.ThemeMode
import com.bicy.whitenoise.storage.theme.CustomThemeLibrary
import com.bicy.whitenoise.storage.theme.ThemeScheduleManager
import com.bicy.whitenoise.ui.viewmodel.SettingsViewModel
import com.bicy.whitenoise.utils.LogManager
import com.bicy.whitenoise.utils.BatteryOptimizationHelper
import com.bicy.whitenoise.service.LogCaptureService
import com.bicy.whitenoise.ui.PageBottomPadding
import com.bicy.whitenoise.utils.UsageStatsManager
import com.bicy.whitenoise.onlinemusic.SourceScriptManager
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.service.AnomalyType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val autoPlayEnabled by viewModel.autoPlayEnabled.collectAsState()
    val musicDirectories by MusicStorage.directories.collectAsState()
    val scanProgress by MusicLibrary.scanProgress.collectAsState()
    val globalState by ConfigStorage.config.collectAsState()
    val currentThemeColor by ThemeColorManager.currentThemeColor.collectAsState()
    val customColors by ThemeColorManager.customColors.collectAsState()
    val currentThemeMode by ThemeColorManager.currentThemeMode.collectAsState()

    var showThemeColorDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showScheduleConfigDialog by remember { mutableStateOf(false) }
    var showAddScheduleTaskDialog by remember { mutableStateOf(false) }
    var showCreateCustomThemeDialog by remember { mutableStateOf(false) }
    // 编辑模式下的主题（非 null 表示正在编辑已有自定义主题）
    var editingTheme by remember { mutableStateOf<com.bicy.whitenoise.ui.theme.CustomTheme?>(null) }
    var showMusicDirDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showEffectOrderDialog by remember { mutableStateOf(false) }
    var showMediaControlPriorityDialog by remember { mutableStateOf(false) }
    var showWnHistoryDialog by remember { mutableStateOf(false) }
    var showMusicHistoryDialog by remember { mutableStateOf(false) }
    var showTimerHistoryDialog by remember { mutableStateOf(false) }
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }
    var editingColorType by remember { mutableStateOf<String?>(null) }
    var showFloatingPetSelectorDialog by remember { mutableStateOf(false) }
    var showGlassScopeDialog by remember { mutableStateOf(false) }
    var showFrostedScopeDialog by remember { mutableStateOf(false) }
    var showBgGlassTypeDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showEqUnlimitedWarning by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                Log.d("SettingScreen", "Persisted URI permission for: $it")
            } catch (e: Exception) {
                Log.e("SettingScreen", "Failed to persist URI permission", e)
            }
            
            val path = getFullPathFromUri(context, it)
            if (path != null) {
                MusicStorage.addDirectory(path, it)
                scope.launch {
                    MusicLibrary.scanLibrary(context)
                }
            }
        }
    }

    // 背景图片选择器
    val backgroundImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // 复制到内部存储，避免 URI 权限过期
            NavBackgroundConfig.setBackgroundImage(context, it)
        }
    }

    val scriptManager = remember { SourceScriptManager.getInstance(context.applicationContext) }
    var scriptCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        scriptManager.initAsync {
            scriptCount = scriptManager.getScriptCount()
        }
    }

    // 脚本文件选择器
    val scriptFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.readText() ?: ""
                inputStream?.close()
                if (content.isNotBlank()) {
                    scriptManager.importScript(content)
                    scriptCount = scriptManager.getScriptCount()
                    ToastManager.success("音源脚本已导入")
                }
            } catch (e: Exception) {
                ToastManager.error(e.message ?: "脚本导入失败")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        
        if (allGranted) {
            directoryPicker.launch(null)
        }
    }
    
    fun checkAndRequestPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            directoryPicker.launch(null)
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = PageBottomPadding
            )
        ) {
            item {
                Spacer(modifier = Modifier.height(PageTopPadding))
            }

            item {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.customize_experience),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                UserLevelCard(
                onUnlockClick = { showDonationDialog = true }
            )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingCategorySection(
                    title = stringResource(R.string.ai),
                    isExpanded = expandedSections["ai"] == true,
                    onToggle = {
                        expandedSections["ai"] = expandedSections["ai"] != true
                    }
                ) {
                    SettingSwitchItem(
                        title = stringResource(R.string.ai_enabled),
                        checked = globalState.aiEnabled,
                        onCheckedChange = { ConfigStorage.setAiEnabled(it) }
                    )

                    SettingTextFieldItem(
                        title = stringResource(R.string.ai_base_url),
                        value = globalState.aiBaseUrl,
                        placeholder = "https://api.openai.com/v1",
                        inline = true,
                        onValueChange = { ConfigStorage.setAiBaseUrl(it) }
                    )

                    SettingTextFieldItem(
                        title = stringResource(R.string.ai_api_key),
                        value = globalState.aiApiKey,
                        placeholder = "sk-...",
                        isPassword = true,
                        inline = true,
                        onValueChange = { ConfigStorage.setAiApiKey(it) }
                    )

                    SettingTextFieldItem(
                        title = stringResource(R.string.ai_model),
                        value = globalState.aiModel,
                        placeholder = "gpt-4o",
                        inline = true,
                        onValueChange = { ConfigStorage.setAiModel(it) }
                    )

                    // 温度滑块（float 0.0-2.0）
                    SettingFloatSliderItem(
                        title = stringResource(R.string.ai_temperature),
                        value = globalState.aiTemperature,
                        valueRange = 0f..2f,
                        onValueChange = { ConfigStorage.setAiTemperature(it) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSliderWithTextFieldItem(
                        title = stringResource(R.string.ai_max_tokens),
                        value = globalState.aiMaxTokens,
                        valueRange = 1024..65536,
                        onValueChange = { ConfigStorage.setAiMaxTokens(it) }
                    )

                    SettingSliderWithTextFieldItem(
                        title = stringResource(R.string.ai_max_tool_calls),
                        value = globalState.aiMaxToolCalls,
                        valueRange = 10..50,
                        onValueChange = { ConfigStorage.setAiMaxToolCalls(it) }
                    )

                    SettingSwitchItem(
                        title = stringResource(R.string.ai_enable_thinking),
                        checked = globalState.aiEnableThinking,
                        onCheckedChange = { ConfigStorage.setAiEnableThinking(it) }
                    )

                    SettingTextFieldItem(
                        title = stringResource(R.string.ai_system_prompt),
                        value = globalState.aiSystemPrompt,
                        placeholder = stringResource(R.string.ai_system_prompt_default),
                        multiline = true,
                        onValueChange = { ConfigStorage.setAiSystemPrompt(it) }
                    )

                    // 测试连接按钮
                    var isTestingConnection by remember { mutableStateOf(false) }
                    SettingClickItem(
                        title = stringResource(R.string.ai_test_connection),
                        value = if (isTestingConnection) "..." else "",
                        onClick = {
                            if (globalState.aiApiKey.isBlank() || globalState.aiBaseUrl.isBlank() || globalState.aiModel.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.ai_not_configured), Toast.LENGTH_SHORT).show()
                                return@SettingClickItem
                            }
                            isTestingConnection = true
                            scope.launch {
                                val aiService = com.bicy.whitenoise.data.ai.AIService(context)
                                aiService.initialize()
                                val result = aiService.testConnection()
                                isTestingConnection = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, context.getString(R.string.ai_connection_success), Toast.LENGTH_SHORT).show()
                                } else {
                                    val msg = result.exceptionOrNull()?.message ?: "unknown"
                                    Toast.makeText(context, context.getString(R.string.ai_connection_failed, msg), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingCategorySection(
                    title = stringResource(R.string.music),
                    isExpanded = expandedSections["music"] == true,
                    onToggle = {
                        expandedSections["music"] = expandedSections["music"] != true
                    }
                ) {
                    SettingClickItem(
                        title = stringResource(R.string.music_directory),
                        value = "${musicDirectories.size} ${stringResource(R.string.directories_count)}",
                        onClick = { showMusicDirDialog = true }
                    )
                    
                    SettingClickItem(
                        title = stringResource(R.string.source_script_management),
                        value = "$scriptCount ${stringResource(R.string.scripts_count)}",
                        onClick = { showScriptDialog = true }
                    )
                    
                    SettingSwitchItem(
                        title = stringResource(R.string.sync_compensation_to_manual_eq),
                        checked = globalState.autoEqSyncToManual,
                        onCheckedChange = { enabled -> ConfigStorage.setAutoEqSyncToManual(enabled) }
                    )
                    
                    SettingSwitchItem(
                        title = stringResource(R.string.eq_unlimited_points),
                        checked = globalState.eqUnlimitedPoints,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showEqUnlimitedWarning = true
                            } else {
                                ConfigStorage.setEqUnlimitedPoints(false)
                            }
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingCategorySection(
                    title = stringResource(R.string.personalization),
                    isExpanded = expandedSections["personalization"] == true,
                    onToggle = {
                        expandedSections["personalization"] = expandedSections["personalization"] != true
                    }
                ) {
                    // 主题模式选择
                    val modeDisplayName = when (currentThemeMode) {
                        ThemeMode.OFF -> stringResource(R.string.theme_mode_off)
                        ThemeMode.FOLLOW_SYSTEM -> stringResource(R.string.theme_mode_follow_system)
                        ThemeMode.SCHEDULED -> stringResource(R.string.theme_mode_scheduled)
                    }
                    
                    SettingClickItem(
                        title = stringResource(R.string.theme_mode),
                        value = modeDisplayName,
                        onClick = { showThemeModeDialog = true }
                    )
                    
                    // 根据模式显示不同的子项
                    when (currentThemeMode) {
                        ThemeMode.OFF -> {
                            // 关闭模式：只显示当前主题色选择
                            SettingClickItem(
                                title = stringResource(R.string.theme_color),
                                value = currentThemeColor.name,
                                onClick = { showThemeColorDialog = true }
                            )
                            
                            if (currentThemeColor.id == "custom") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.custom_colors),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            val colors = customColors
                                            listOf(
                                                stringResource(R.string.accent_color) to (colors?.accent ?: 0xFFB8A07A.toInt()),
                                                stringResource(R.string.primary_color) to (colors?.primary ?: 0xFFB8A07A.toInt()),
                                                stringResource(R.string.background_color) to (colors?.background ?: 0xFFFAF6F0.toInt()),
                                                stringResource(R.string.text_color) to (colors?.text ?: 0xFF3D3A35.toInt())
                                            ).forEach { (label, colorValue) ->
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(colorValue))
                                                            .clickable { editingColorType = label }
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        ThemeMode.FOLLOW_SYSTEM -> {
                            // 跟随系统模式：显示日间主题和夜间主题两个设置项
                            val dayThemeId = ConfigStorage.getDayThemeId()
                            val nightThemeId = ConfigStorage.getNightThemeId()
                            
                            val dayTheme = CustomThemeLibrary.getThemeByIdIncludingPresets(dayThemeId)
                            val nightTheme = CustomThemeLibrary.getThemeByIdIncludingPresets(nightThemeId)
                            
                            SettingClickItem(
                                title = stringResource(R.string.day_theme),
                                value = dayTheme?.name ?: "Default",
                                onClick = {
                                    // 可以通过 showThemeColorDialog 选择，然后设置日间主题
                                    showThemeColorDialog = true
                                }
                            )
                            
                            SettingClickItem(
                                title = stringResource(R.string.night_theme),
                                value = nightTheme?.name ?: "Default Night",
                                onClick = {
                                    // 可以通过 showThemeColorDialog 选择，然后设置夜间主题
                                    showThemeColorDialog = true
                                }
                            )
                        }
                        ThemeMode.SCHEDULED -> {
                            // 定时模式：显示"定时任务配置"按钮
                            SettingClickItem(
                                title = stringResource(R.string.schedule_config),
                                value = stringResource(R.string.customize),
                                onClick = { showScheduleConfigDialog = true }
                            )
                        }
                    }
                    // 背景图片设置
                    val backgroundUri by NavBackgroundConfig.backgroundUriFlow.collectAsState()

                    SettingClickItem(
                        title = stringResource(R.string.nav_background_image),
                        value = if (backgroundUri != null) {
                            stringResource(R.string.nav_background_image_set)
                        } else {
                            stringResource(R.string.nav_background_image_none)
                        },
                        onClick = {
                            if (backgroundUri != null) {
                                // 已设置背景图片，点击清除
                                NavBackgroundConfig.clearBackgroundUri()
                            } else {
                                // 未设置，点击选择
                                backgroundImagePicker.launch("image/*")
                            }
                        }
                    )

                    // 背景色透明度设置
                    val backgroundAlpha by NavBackgroundConfig.backgroundAlphaFlow.collectAsState()

                    SettingFloatSliderItem(
                        title = stringResource(R.string.nav_background_alpha),
                        value = backgroundAlpha,
                        valueRange = 0.1f..1.0f,
                        onValueChange = { alpha -> NavBackgroundConfig.setBackgroundAlpha(alpha) }
                    )

                    // 任务8：可视化配置按钮 + 弹窗（替代原 slider/switch）
                    var showVizConfigDialog by remember { mutableStateOf(false) }
                    val vizSummary = buildString {
                        if (globalState.vizWnEnabled) append("W ")
                        if (globalState.vizMusicEnabled) append("M ")
                        if (globalState.vizFlashEnabled) append("F")
                    }.ifBlank { "—" }
                    SettingClickItem(
                        title = stringResource(R.string.viz_config_title),
                        value = vizSummary,
                        onClick = { showVizConfigDialog = true }
                    )
                    if (showVizConfigDialog) {
                        VisualizationConfigDialog(onDismiss = { showVizConfigDialog = false })
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingSwitchItem(
                        title = stringResource(R.string.floating_pet),
                        checked = globalState.floatingPetEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                    ConfigStorage.setFloatingPetEnabled(false)
                                } else {
                                    ConfigStorage.setFloatingPetEnabled(true)
                                    com.bicy.whitenoise.floatingpet.FloatingPetService.getInstance(context).show()
                                }
                            } else {
                                ConfigStorage.setFloatingPetEnabled(false)
                                com.bicy.whitenoise.floatingpet.FloatingPetService.getInstance(context).hide()
                            }
                        }
                    )
                    
                    if (globalState.floatingPetEnabled) {
                        SettingClickItem(
                            title = stringResource(R.string.switch_floating_pet),
                            value = globalState.floatingPetId,
                            onClick = { showFloatingPetSelectorDialog = true }
                        )
                        
                        SettingSliderItem(
                            title = stringResource(R.string.floating_pet_size),
                            value = ((globalState.floatingPetScale - 0.5f) / 0.5f).toInt().coerceIn(0, 3),
                            valueRange = 0..3,
                            onValueChange = { scaleIndex ->
                                val scale = 0.5f + scaleIndex * 0.5f
                                ConfigStorage.setFloatingPetScale(scale)
                                com.bicy.whitenoise.floatingpet.FloatingPetService.getInstance(context).updateConfig()
                            }
                        )
                        
                        SettingSwitchItem(
                            title = stringResource(R.string.floating_pet_antialias),
                            checked = globalState.floatingPetAntiAlias,
                            onCheckedChange = { enabled ->
                                ConfigStorage.setFloatingPetAntiAlias(enabled)
                                com.bicy.whitenoise.floatingpet.FloatingPetService.getInstance(context).updateConfig()
                            }
                        )
                        
                        SettingSwitchItem(
                            title = stringResource(R.string.floating_pet_window_mode),
                            checked = globalState.floatingPetWindowMode,
                            onCheckedChange = { enabled ->
                                ConfigStorage.setFloatingPetWindowMode(enabled)
                            }
                        )
                        
                        SettingSliderWithTextItem(
                            title = stringResource(R.string.floating_pet_hide_delay),
                            value = globalState.floatingPetHideDelay,
                            valueRange = 3..10,
                            valueSuffix = "s",
                            onValueChange = { ConfigStorage.setFloatingPetHideDelay(it) }
                        )
                        
                        SettingSliderWithTextItem(
                            title = stringResource(R.string.floating_pet_alpha),
                            value = (globalState.floatingPetAlpha * 100).toInt(),
                            valueRange = 10..100,
                            valueSuffix = "%",
                            onValueChange = {
                                ConfigStorage.setFloatingPetAlpha(it / 100f)
                                com.bicy.whitenoise.floatingpet.FloatingPetService.getInstance(context).updateConfig()
                            }
                        )
                        
                        SettingSliderWithTextItem(
                            title = stringResource(R.string.floating_pet_hidden_alpha),
                            value = (globalState.floatingPetHiddenAlpha * 100).toInt(),
                            valueRange = 10..100,
                            valueSuffix = "%",
                            onValueChange = {
                                ConfigStorage.setFloatingPetHiddenAlpha(it / 100f)
                                com.bicy.whitenoise.floatingpet.FloatingPetService.getInstance(context).updateConfig()
                            }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingCategorySection(
                    title = stringResource(R.string.rendering_experimental),
                    isExpanded = expandedSections["rendering"] == true,
                    onToggle = {
                        expandedSections["rendering"] = expandedSections["rendering"] != true
                    }
                ) {
                    val glassMode by LiquidGlassConfig.modeFlow.collectAsState()
                    
                    // ── 液态玻璃组件 ──
                    val isGlassExpanded = expandedSections["liquid_glass"] == true
                    SettingCategorySection(
                        title = stringResource(R.string.liquid_glass_components),
                        isExpanded = isGlassExpanded,
                        onToggle = {
                            expandedSections["liquid_glass"] = expandedSections["liquid_glass"] != true
                        }
                    ) {
                        val isPerfectSupported = LiquidGlassConfig.isPerfectModeSupported()
                        SettingSegmentedControlItem(
                            title = stringResource(R.string.liquid_glass_mode),
                            options = listOf(
                                stringResource(R.string.liquid_glass_off),
                                stringResource(R.string.liquid_glass_compatible),
                                stringResource(R.string.liquid_glass_perfect)
                            ),
                            selectedOption = when (glassMode) {
                                GlassMode.OFF -> 0
                                GlassMode.COMPATIBLE -> 1
                                GlassMode.PERFECT -> 2
                            },
                            disabledOptions = if (!isPerfectSupported) listOf(2) else emptyList(),
                            onOptionSelected = { index ->
                                val newMode = when (index) {
                                    0 -> GlassMode.OFF
                                    1 -> GlassMode.COMPATIBLE
                                    2 -> GlassMode.PERFECT
                                    else -> GlassMode.COMPATIBLE
                                }
                                LiquidGlassConfig.setMode(newMode)
                            }
                        )

                        val currentGlassScope by GlassScopeConfig.scopeFlow.collectAsState()
                        val scopeLabel = when (currentGlassScope) {
                            GlassScopeConfig.SCOPE_BOTTOM_NAV -> stringResource(R.string.glass_scope_bottom_nav)
                            GlassScopeConfig.SCOPE_TOP_BAR -> stringResource(R.string.glass_scope_top_bar)
                            else -> stringResource(R.string.glass_scope_all)
                        }
                        SettingClickItem(
                            title = stringResource(R.string.glass_scope),
                            value = scopeLabel,
                            onClick = { showGlassScopeDialog = true }
                        )

                        // 调整参数（仅非关闭时显示）
                        if (glassMode != GlassMode.OFF) {
                            val isParamsExpanded = expandedSections["glass_params"] == true
                            SettingCategorySection(
                                title = stringResource(R.string.glass_adjust_params),
                                isExpanded = isParamsExpanded,
                                onToggle = {
                                    expandedSections["glass_params"] = expandedSections["glass_params"] != true
                                }
                            ) {
                                if (glassMode == GlassMode.COMPATIBLE) {
                                    // COMPATIBLE 参数
                                    val compatOpacity by GlassRenderConfig.compatOpacityFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.glass_compat_opacity),
                                        value = (compatOpacity * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> GlassRenderConfig.setCompatOpacity(v / 100f) }
                                    )
                                    val compatDarkness by GlassRenderConfig.compatDarknessFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.glass_compat_darkness),
                                        value = (compatDarkness * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> GlassRenderConfig.setCompatDarkness(v / 100f) }
                                    )
                                    val compatScale by GlassRenderConfig.compatScaleFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.glass_compat_scale),
                                        value = (compatScale * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> GlassRenderConfig.setCompatScale(v / 100f) }
                                    )
                                    // ── 阴影 ──
                                    val compatShadowEnabled by GlassRenderConfig.compatShadowEnabledFlow.collectAsState()
                                    SettingSwitchItem(
                                        title = stringResource(R.string.glass_shadow_enable),
                                        checked = compatShadowEnabled,
                                        onCheckedChange = { GlassRenderConfig.setCompatShadowEnabled(it) }
                                    )
                                    if (compatShadowEnabled) {
                                        val compatShadowStrength by GlassRenderConfig.compatShadowStrengthFlow.collectAsState()
                                        SettingSliderItem(
                                            title = stringResource(R.string.glass_shadow_strength),
                                            value = (compatShadowStrength * 100f).toInt(),
                                            valueRange = 0..100,
                                            onValueChange = { v -> GlassRenderConfig.setCompatShadowStrength(v / 100f) }
                                        )
                                        val compatShadowHeight by GlassRenderConfig.compatShadowHeightFlow.collectAsState()
                                        SettingSliderItem(
                                            title = stringResource(R.string.glass_shadow_height),
                                            value = compatShadowHeight.toInt(),
                                            valueRange = 0..50,
                                            onValueChange = { v -> GlassRenderConfig.setCompatShadowHeight(v.toFloat()) }
                                        )
                                    }
                                } else {
                                    // PERFECT 参数
                                    val perfBlur by GlassRenderConfig.perfBlurFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.glass_blur_strength),
                                        value = (perfBlur * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> GlassRenderConfig.setPerfBlur(v / 100f) }
                                    )
                                    val perfScale by GlassRenderConfig.perfScaleFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.glass_scale_effect),
                                        value = (perfScale * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> GlassRenderConfig.setPerfScale(v / 100f) }
                                    )
                                    val perfDistortion by GlassRenderConfig.perfDistortionFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.glass_center_distortion),
                                        value = (perfDistortion * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> GlassRenderConfig.setPerfDistortion(v / 100f) }
                                    )
                                    val perfDarkness by GlassRenderConfig.perfDarknessFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.glass_edge_darkness),
                                        value = (perfDarkness * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> GlassRenderConfig.setPerfDarkness(v / 100f) }
                                    )
                                    val perfWarp by GlassRenderConfig.perfWarpFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.glass_edge_warp),
                                        value = (perfWarp * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> GlassRenderConfig.setPerfWarp(v / 100f) }
                                    )
                                    // ── 阴影 ──
                                    val perfShadowEnabled by GlassRenderConfig.perfShadowEnabledFlow.collectAsState()
                                    SettingSwitchItem(
                                        title = stringResource(R.string.glass_shadow_enable),
                                        checked = perfShadowEnabled,
                                        onCheckedChange = { GlassRenderConfig.setPerfShadowEnabled(it) }
                                    )
                                    if (perfShadowEnabled) {
                                        val perfShadowStrength by GlassRenderConfig.perfShadowStrengthFlow.collectAsState()
                                        SettingSliderItem(
                                            title = stringResource(R.string.glass_shadow_strength),
                                            value = (perfShadowStrength * 100f).toInt(),
                                            valueRange = 0..100,
                                            onValueChange = { v -> GlassRenderConfig.setPerfShadowStrength(v / 100f) }
                                        )
                                        val perfElevation by GlassRenderConfig.perfElevationFlow.collectAsState()
                                        SettingSliderItem(
                                            title = stringResource(R.string.glass_shadow_height),
                                            value = perfElevation.toInt(),
                                            valueRange = 0..20,
                                            onValueChange = { v -> GlassRenderConfig.setPerfElevation(v.toFloat()) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 毛玻璃 ──
                    val isFrostedExpanded = expandedSections["frosted_params"] == true
                    SettingCategorySection(
                        title = stringResource(R.string.frosted_glass),
                        isExpanded = isFrostedExpanded,
                        onToggle = {
                            expandedSections["frosted_params"] = expandedSections["frosted_params"] != true
                        }
                    ) {
                        val frostedEnabled by FrostedGlassConfig.enabledFlow.collectAsState()
                        SettingSwitchItem(
                            title = stringResource(R.string.frosted_glass_enable),
                            checked = frostedEnabled,
                            onCheckedChange = { FrostedGlassConfig.setEnabled(it) }
                        )
                        if (frostedEnabled) {
                            val frostedBlur by FrostedGlassConfig.blurFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.frosted_blur),
                                value = (frostedBlur * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> FrostedGlassConfig.setBlur(v / 100f) }
                            )
                            val frostedOpacity by FrostedGlassConfig.opacityFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.frosted_opacity),
                                value = (frostedOpacity * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> FrostedGlassConfig.setOpacity(v / 100f) }
                            )
                            val frostedEdgeHighlight by FrostedGlassConfig.edgeHighlightFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.frosted_edge_highlight),
                                value = (frostedEdgeHighlight * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> FrostedGlassConfig.setEdgeHighlight(v / 100f) }
                            )
                            val frostedDarkness by FrostedGlassConfig.darknessFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.frosted_darkness),
                                value = (frostedDarkness * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> FrostedGlassConfig.setDarkness(v / 100f) }
                            )
                            val frostedScopeCount = FrostedGlassScopeConfig.ALL_SCOPES.count { FrostedGlassScopeConfig.flow(it.key).value }
                            SettingClickItem(
                                title = stringResource(R.string.glass_scope),
                                value = "$frostedScopeCount / ${FrostedGlassScopeConfig.ALL_SCOPES.size}",
                                onClick = { showFrostedScopeDialog = true }
                            )
                        }
                    }

                    // ── 弹窗模糊背板 ──
                    val isBlurBackdropExpanded = expandedSections["blur_backdrop"] == true
                    SettingCategorySection(
                        title = stringResource(R.string.blur_backdrop),
                        isExpanded = isBlurBackdropExpanded,
                        onToggle = {
                            expandedSections["blur_backdrop"] = expandedSections["blur_backdrop"] != true
                        }
                    ) {
                        val blurBackdropEnabled by BlurBackdropConfig.enabledFlow.collectAsState()
                        SettingSwitchItem(
                            title = stringResource(R.string.blur_backdrop_enable),
                            checked = blurBackdropEnabled,
                            onCheckedChange = { BlurBackdropConfig.setEnabled(it) }
                        )
                        if (blurBackdropEnabled) {
                            val blurBackdropBlur by BlurBackdropConfig.blurFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.blur_backdrop_blur),
                                value = (blurBackdropBlur * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> BlurBackdropConfig.setBlur(v / 100f) }
                            )
                            val blurBackdropDarkness by BlurBackdropConfig.darknessFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.blur_backdrop_darkness),
                                value = (blurBackdropDarkness * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> BlurBackdropConfig.setDarkness(v / 100f) }
                            )
                        }
                    }

                    // ── 背景玻璃模糊 ──
                    val isBgGlassExpanded = expandedSections["bg_glass"] == true
                    SettingCategorySection(
                        title = stringResource(R.string.background_glass),
                        isExpanded = isBgGlassExpanded,
                        onToggle = {
                            expandedSections["bg_glass"] = expandedSections["bg_glass"] != true
                        }
                    ) {
                        val bgGlassEnabled by BackgroundGlassConfig.enabledFlow.collectAsState()
                        SettingSwitchItem(
                            title = stringResource(R.string.background_glass_enable),
                            checked = bgGlassEnabled,
                            onCheckedChange = { BackgroundGlassConfig.setEnabled(it) }
                        )
                        if (bgGlassEnabled) {
                            // 玻璃类型选择器
                            val bgGlassType by BackgroundGlassConfig.typeFlow.collectAsState()
                            val typeLabel = when (bgGlassType) {
                                BackgroundGlassConfig.GlassType.SANDBLASTED -> stringResource(R.string.bg_glass_sandblasted)
                                BackgroundGlassConfig.GlassType.FROSTED -> stringResource(R.string.bg_glass_frosted)
                                BackgroundGlassConfig.GlassType.GRID -> stringResource(R.string.bg_glass_grid)
                                BackgroundGlassConfig.GlassType.MISTY -> stringResource(R.string.bg_glass_misty)
                                BackgroundGlassConfig.GlassType.SILK -> stringResource(R.string.bg_glass_silk)
                            }
                            SettingClickItem(
                                title = stringResource(R.string.bg_glass_type),
                                value = typeLabel,
                                onClick = { showBgGlassTypeDialog = true }
                            )
                            // 通用参数：模糊强度
                            val bgGlassBlur by BackgroundGlassConfig.blurFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.bg_glass_blur),
                                value = (bgGlassBlur * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> BackgroundGlassConfig.setBlur(v / 100f) }
                            )
                            // 通用参数：不透明度
                            val bgGlassOpacity by BackgroundGlassConfig.opacityFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.bg_glass_opacity),
                                value = (bgGlassOpacity * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> BackgroundGlassConfig.setOpacity(v / 100f) }
                            )
                            // 通用参数：暗度
                            val bgGlassDarkness by BackgroundGlassConfig.darknessFlow.collectAsState()
                            SettingSliderItem(
                                title = stringResource(R.string.bg_glass_darkness),
                                value = (bgGlassDarkness * 100f).toInt(),
                                valueRange = 0..100,
                                onValueChange = { v -> BackgroundGlassConfig.setDarkness(v / 100f) }
                            )
                            // 类型专用参数
                            when (bgGlassType) {
                                BackgroundGlassConfig.GlassType.SANDBLASTED -> {
                                    val noise by BackgroundGlassConfig.noiseFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.bg_glass_noise),
                                        value = (noise * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> BackgroundGlassConfig.setNoise(v / 100f) }
                                    )
                                }
                                BackgroundGlassConfig.GlassType.GRID -> {
                                    val gridSize by BackgroundGlassConfig.gridSizeFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.bg_glass_grid_size),
                                        value = (gridSize * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> BackgroundGlassConfig.setGridSize(v / 100f) }
                                    )
                                }
                                BackgroundGlassConfig.GlassType.MISTY -> {
                                    val gradient by BackgroundGlassConfig.gradientFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.bg_glass_gradient),
                                        value = (gradient * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> BackgroundGlassConfig.setGradient(v / 100f) }
                                    )
                                }
                                BackgroundGlassConfig.GlassType.SILK -> {
                                    val sheen by BackgroundGlassConfig.sheenFlow.collectAsState()
                                    SettingSliderItem(
                                        title = stringResource(R.string.bg_glass_sheen),
                                        value = (sheen * 100f).toInt(),
                                        valueRange = 0..100,
                                        onValueChange = { v -> BackgroundGlassConfig.setSheen(v / 100f) }
                                    )
                                }
                                else -> { /* FROSTED 无专用参数 */ }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                SettingCategorySection(
                    title = stringResource(R.string.general),
                    isExpanded = expandedSections["general"] == true,
                    onToggle = {
                        expandedSections["general"] = expandedSections["general"] != true
                    }
                ) {
                    SettingSwitchItem(
                        title = stringResource(R.string.auto_play_on_startup),
                        checked = autoPlayEnabled,
                        onCheckedChange = { viewModel.setAutoPlay(it) }
                    )

                    SettingSliderItem(
                        title = "通知持续时间",
                        value = globalState.toastDurationSeconds,
                        valueRange = 3..10,
                        onValueChange = { ConfigStorage.setToastDurationSeconds(it) }
                    )
                    
                    SettingClickItem(
                        title = stringResource(R.string.initialize_white_noise_list),
                        value = stringResource(R.string.initialize_white_noise_list_desc),
                        onClick = {
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    com.bicy.whitenoise.utils.SoundStorageManager.reinitializeFromRemoteManifest(context)
                                    com.bicy.whitenoise.storage.whitenoise.WhiteNoiseStorage.init()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.initialize_success),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("Settings", "初始化白噪音列表失败", e)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "初始化失败: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    )
                    
                    SettingClickItem(
                        title = stringResource(R.string.audio_effect_order),
                        value = stringResource(R.string.customize),
                        onClick = { showEffectOrderDialog = true }
                    )
                    
                    val mediaControlPriority = ConfigStorage.getMediaControlPriority()
                    val priorityLabel = when (mediaControlPriority) {
                        "white_noise" -> stringResource(R.string.media_control_white_noise)
                        "music" -> stringResource(R.string.media_control_music)
                        "all" -> stringResource(R.string.media_control_all)
                        else -> stringResource(R.string.media_control_smart)
                    }
                    SettingClickItem(
                        title = stringResource(R.string.media_control_priority),
                        value = priorityLabel,
                        onClick = { showMediaControlPriorityDialog = true }
                    )
                    
                    SettingSwitchItem(
                        title = stringResource(R.string.log_recording),
                        checked = globalState.logEnabled,
                        onCheckedChange = { enabled ->
                            ConfigStorage.setLogEnabled(enabled)
                            LogManager.setLogEnabled(enabled)
                            // :log 进程同步
                            val intent = Intent(context, LogCaptureService::class.java)
                            intent.action = if (enabled)
                                "com.bicy.whitenoise.ACTION_ENABLE_APP_LOG"
                            else
                                "com.bicy.whitenoise.ACTION_DISABLE_APP_LOG"
                            context.startService(intent)
                        }
                    )
                    
                    // 使用教程设置项
                    SettingClickItem(
                        title = stringResource(R.string.tutorial),
                        value = stringResource(R.string.start_tutorial),
                        onClick = {
                            com.bicy.whitenoise.ui.tutorial.TutorialManager.resetAllTutorials()
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    )

                    // 应用后台保活引导
                    SettingClickItemWithIcon(
                        icon = Icons.Filled.PowerSettingsNew,
                        title = stringResource(R.string.keep_alive_guide),
                        subtitle = stringResource(R.string.keep_alive_guide_desc),
                        onClick = {
                            val success = BatteryOptimizationHelper.openKeepAliveSettings(context)
                            if (!success) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.keep_alive_guide_open_failed),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )

                    SettingClickItem(
                        title = stringResource(R.string.about_app),
                        value = "BicyOne",
                        onClick = { showAboutDialog = true }
                    )
                    
                    // GitHub 开源仓库
                    GitHubRepoButton()
                }
            }
            
            // 应用统计分类条
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                val usageStats by UsageStatsManager.stats.collectAsState()
                
                SettingCategorySection(
                    title = stringResource(R.string.app_statistics),
                    isExpanded = expandedSections["app_statistics"] == true,
                    onToggle = {
                        expandedSections["app_statistics"] = expandedSections["app_statistics"] != true
                    }
                ) {
                    SettingTextItem(
                        title = stringResource(R.string.total_used_days),
                        value = "${usageStats.usedDates.size} ${stringResource(R.string.days_unit)}"
                    )
                    
                    SettingTextItem(
                        title = stringResource(R.string.total_run_duration),
                        value = UsageStatsManager.formatDurationFromStart(UsageStatsManager.getTotalRunDuration())
                    )
                    
                    SettingTextItem(
                        title = stringResource(R.string.total_white_noise_duration),
                        value = UsageStatsManager.formatDurationFromStart(usageStats.totalWhiteNoiseDuration)
                    )
                    
                    SettingTextItem(
                        title = stringResource(R.string.total_music_duration),
                        value = UsageStatsManager.formatDurationFromStart(usageStats.totalMusicDuration)
                    )
                    
                    SettingTextItem(
                        title = stringResource(R.string.total_timer_duration),
                        value = UsageStatsManager.formatDurationFromStart(usageStats.totalTimerDuration)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingTextItem(
                        title = stringResource(R.string.today_white_noise),
                        value = UsageStatsManager.formatDurationSimple(usageStats.todayWhiteNoiseDuration)
                    )
                    
                    SettingTextItem(
                        title = stringResource(R.string.today_music),
                        value = UsageStatsManager.formatDurationSimple(usageStats.todayMusicDuration)
                    )
                    
                    SettingTextItem(
                        title = stringResource(R.string.today_timer),
                        value = UsageStatsManager.formatDurationSimple(usageStats.todayTimerDuration)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 历史统计三项，各带查看按钮
                    SettingTextItemWithButton(
                        title = "${stringResource(R.string.history_stats)} - ${stringResource(R.string.white_noise)}",
                        value = "${usageStats.historyStats.size} ${stringResource(R.string.days_unit)}",
                        buttonText = stringResource(R.string.view),
                        onClick = { showWnHistoryDialog = true }
                    )
                    
                    SettingTextItemWithButton(
                        title = "${stringResource(R.string.history_stats)} - ${stringResource(R.string.music)}",
                        value = "${usageStats.historyStats.size} ${stringResource(R.string.days_unit)}",
                        buttonText = stringResource(R.string.view),
                        onClick = { showMusicHistoryDialog = true }
                    )
                    
                    SettingTextItemWithButton(
                        title = "${stringResource(R.string.history_stats)} - ${stringResource(R.string.timer)}",
                        value = "${usageStats.historyStats.size} ${stringResource(R.string.days_unit)}",
                        buttonText = stringResource(R.string.view),
                        onClick = { showTimerHistoryDialog = true }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingTextItem(
                        title = stringResource(R.string.total_timer_starts),
                        value = "${usageStats.timerStartCount} ${stringResource(R.string.times_unit)}"
                    )
                }
            }
        }
    }
    
    if (showThemeColorDialog) {
        ThemeColorDialog(
            currentColorId = ThemeColorManager.getCurrentColorId(),
            onDismiss = { showThemeColorDialog = false },
            onConfirm = { colorId ->
                when (currentThemeMode) {
                    ThemeMode.OFF -> {
                        ThemeColorManager.setThemeColor(colorId)
                    }
                    ThemeMode.FOLLOW_SYSTEM -> {
                        // 需要区分日间或夜间主题
                        // 这里简化处理，通过一个状态变量来区分
                        // 实际可以添加一个专门的日间/夜间主题选择对话框
                        if (ConfigStorage.getDayThemeId() != colorId && ConfigStorage.getNightThemeId() != colorId) {
                            // 如果两个都不是，则设置当前系统状态对应的主题
                            ThemeColorManager.setThemeColor(colorId)
                        }
                    }
                    ThemeMode.SCHEDULED -> {
                        ThemeColorManager.setScheduledDefaultThemeId(colorId)
                    }
                }
                showThemeColorDialog = false
            },
            onCustomColorSelected = { accent, primary, background, text ->
                ThemeColorManager.setCustomColors(accent, primary, background, text)
                showThemeColorDialog = false
            },
            onCreateCustomTheme = {
                showThemeColorDialog = false
                editingTheme = null
                showCreateCustomThemeDialog = true
            },
            onDeleteCustomTheme = { themeId ->
                CustomThemeLibrary.deleteTheme(themeId)
            },
            onEditCustomTheme = { scheme ->
                showThemeColorDialog = false
                editingTheme = com.bicy.whitenoise.ui.theme.CustomTheme(
                    id = scheme.id,
                    name = scheme.name,
                    accent = scheme.accent.toArgb(),
                    primary = scheme.primary.toArgb(),
                    background = scheme.background.toArgb(),
                    text = scheme.text.toArgb()
                )
                showCreateCustomThemeDialog = true
            }
        )
    }
    
    if (showThemeModeDialog) {
        ThemeModeSelectionDialog(
            currentMode = currentThemeMode,
            onDismiss = { showThemeModeDialog = false },
            onConfirm = { mode ->
                ThemeColorManager.setThemeMode(mode)
            }
        )
    }
    
    if (showScheduleConfigDialog) {
        ScheduleConfigDialog(
            defaultThemeId = ConfigStorage.getScheduledDefaultThemeId(),
            onDismiss = { showScheduleConfigDialog = false },
            onDefaultThemeChange = { themeId ->
                ThemeColorManager.setScheduledDefaultThemeId(themeId)
            },
            onAddTask = {
                showScheduleConfigDialog = false
                showAddScheduleTaskDialog = true
            },
            onDeleteTask = { taskId ->
                ThemeScheduleManager.deleteTask(taskId)
            }
        )
    }
    
    if (showAddScheduleTaskDialog) {
        AddScheduleTaskDialog(
            onDismiss = { showAddScheduleTaskDialog = false },
            onConfirm = { task ->
                val success = ThemeScheduleManager.addTask(task)
                if (success) {
                    showAddScheduleTaskDialog = false
                    showScheduleConfigDialog = true
                }
            }
        )
    }
    
    if (showCreateCustomThemeDialog) {
        CreateCustomThemeDialog(
            initialTheme = editingTheme,
            onDismiss = {
                showCreateCustomThemeDialog = false
                editingTheme = null
            },
            onConfirm = { customTheme ->
                if (editingTheme != null) {
                    CustomThemeLibrary.updateTheme(customTheme)
                } else {
                    CustomThemeLibrary.addTheme(customTheme)
                }
                showCreateCustomThemeDialog = false
                editingTheme = null
                showThemeColorDialog = true
            }
        )
    }
    
    editingColorType?.let { colorType ->
        val colors = customColors
        val accentColorStr = stringResource(R.string.accent_color)
        val primaryColorStr = stringResource(R.string.primary_color)
        val backgroundColorStr = stringResource(R.string.background_color)
        val textColorStr = stringResource(R.string.text_color)
        
        val initialColor = when (colorType) {
            accentColorStr -> Color(colors?.accent ?: 0xFFB8A07A.toInt())
            primaryColorStr -> Color(colors?.primary ?: 0xFFB8A07A.toInt())
            backgroundColorStr -> Color(colors?.background ?: 0xFFFAF6F0.toInt())
            textColorStr -> Color(colors?.text ?: 0xFF3D3A35.toInt())
            else -> Color(0xFFB8A07A)
        }
        
        SingleColorPickerDialog(
            title = colorType,
            initialColor = initialColor,
            onDismiss = { editingColorType = null },
            onConfirm = { newColor ->
                val currentAccent = colors?.accent ?: 0xFFB8A07A.toInt()
                val currentPrimary = colors?.primary ?: 0xFFB8A07A.toInt()
                val currentBackground = colors?.background ?: 0xFFFAF6F0.toInt()
                val currentText = colors?.text ?: 0xFF3D3A35.toInt()
                
                when (colorType) {
                    accentColorStr -> ThemeColorManager.setCustomColors(newColor, currentPrimary, currentBackground, currentText)
                    primaryColorStr -> ThemeColorManager.setCustomColors(currentAccent, newColor, currentBackground, currentText)
                    backgroundColorStr -> ThemeColorManager.setCustomColors(currentAccent, currentPrimary, newColor, currentText)
                    textColorStr -> ThemeColorManager.setCustomColors(currentAccent, currentPrimary, currentBackground, newColor)
                }
                editingColorType = null
            }
        )
    }
    
    if (showMusicDirDialog) {
        MusicDirectoryDialog(
            directories = musicDirectories,
            isScanning = scanProgress.isScanning,
            onAddDirectory = {
                checkAndRequestPermission()
            },
            onRemoveDirectory = { path ->
                MusicStorage.removeDirectory(path)
                scope.launch {
                    MusicLibrary.scanLibrary(context)
                }
            },
            onDismiss = { showMusicDirDialog = false }
        )
    }
    
    if (showDonationDialog) {
        DonationDialog(
            onDismiss = { showDonationDialog = false }
        )
    }
    
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
    
    if (showEffectOrderDialog) {
        EffectOrderDialog(
            currentOrder = globalState.audioEffectOrder,
            onDismiss = { showEffectOrderDialog = false },
            onConfirm = { newOrder ->
                ConfigStorage.setAudioEffectOrder(newOrder)
                MusicService.getInstance()?.reloadAllTracksWithNewEffectOrder()
                MusicPlayerController.reloadCurrentTrackWithNewEffectOrder()
            }
        )
    }
    
    if (showMediaControlPriorityDialog) {
        MediaControlPriorityDialog(
            currentPriority = ConfigStorage.getMediaControlPriority(),
            onDismiss = { showMediaControlPriorityDialog = false },
            onConfirm = { priority ->
                ConfigStorage.setMediaControlPriority(priority)
            }
        )
    }
    
    if (showWnHistoryDialog) {
        UsageHistoryDialog(
            type = HistoryType.WHITE_NOISE,
            onDismiss = { showWnHistoryDialog = false }
        )
    }
    
    if (showMusicHistoryDialog) {
        UsageHistoryDialog(
            type = HistoryType.MUSIC,
            onDismiss = { showMusicHistoryDialog = false }
        )
    }
    
    if (showTimerHistoryDialog) {
        UsageHistoryDialog(
            type = HistoryType.TIMER,
            onDismiss = { showTimerHistoryDialog = false }
        )
    }
    
    if (showFloatingPetSelectorDialog) {
        FloatingPetSelectorDialog(
            currentPetId = globalState.floatingPetId,
            onPetSelected = { petId ->
                ConfigStorage.setFloatingPetId(petId)
                com.bicy.whitenoise.floatingpet.FloatingPetService.getInstance(context).updateConfig()
            },
            onDismiss = { showFloatingPetSelectorDialog = false }
        )
    }

    if (showGlassScopeDialog) {
        GlassScopeDialog(
            onDismiss = { showGlassScopeDialog = false }
        )
    }

    if (showFrostedScopeDialog) {
        FrostedScopeDialog(
            onDismiss = { showFrostedScopeDialog = false }
        )
    }

    if (showBgGlassTypeDialog) {
        BgGlassTypeSelectionDialog(
            onDismiss = { showBgGlassTypeDialog = false }
        )
    }

    if (showScriptDialog) {
        ScriptManagementDialog(
            scriptManager = scriptManager,
            onDismiss = { showScriptDialog = false },
            onScriptCountChanged = { scriptCount = scriptManager.getScriptCount() },
            onImport = { scriptFilePicker.launch("*/*") }
        )
    }

    if (showEqUnlimitedWarning) {
        AlertDialog(
            onDismissRequest = { showEqUnlimitedWarning = false },
            title = { Text(stringResource(R.string.eq_unlimited_points_warning_title)) },
            text = { Text(stringResource(R.string.eq_unlimited_points_warning_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    ConfigStorage.setEqUnlimitedPoints(true)
                    MemoryLockService.reportAnomaly(
                        AnomalyType.MEMORY_WARNING,
                        "均衡器频段点限制已解除",
                        "用户开启了无限频段点模式，内存锁已开始监控"
                    )
                    showEqUnlimitedWarning = false
                }) {
                    Text(stringResource(R.string.eq_unlimited_points_warning_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEqUnlimitedWarning = false }) {
                    Text(stringResource(R.string.eq_unlimited_points_warning_cancel))
                }
            }
        )
    }

}

@Composable
private fun GlassScopeDialog(onDismiss: () -> Unit) {
    val currentScope by GlassScopeConfig.scopeFlow.collectAsState()
    val scopes = remember { GlassScopeConfig.ALL_SCOPES }

    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.glass_scope),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            scopes.forEach { scope ->
                val isSelected = currentScope == scope.key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { GlassScopeConfig.setScope(scope.key) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { GlassScopeConfig.setScope(scope.key) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(getGlassScopeLabel(scope.key)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Composable
private fun FrostedScopeDialog(onDismiss: () -> Unit) {
    val scopes = remember { FrostedGlassScopeConfig.ALL_SCOPES }

    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.glass_scope),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(scopes.size) { index ->
                    val scope = scopes[index]
                    val checked by FrostedGlassScopeConfig.flow(scope.key).collectAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { FrostedGlassScopeConfig.setEnabled(scope.key, !checked) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { FrostedGlassScopeConfig.setEnabled(scope.key, it) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(getFrostedScopeLabel(scope.key)),
                            style = MaterialTheme.typography.bodyLarge
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
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Composable
private fun getGlassScopeLabel(key: String): Int = when (key) {
    GlassScopeConfig.SCOPE_BOTTOM_NAV -> R.string.glass_scope_bottom_nav
    GlassScopeConfig.SCOPE_TOP_BAR -> R.string.glass_scope_top_bar
    GlassScopeConfig.SCOPE_ALL -> R.string.glass_scope_all
    else -> R.string.glass_scope_bottom_nav
}

@Composable
private fun getFrostedScopeLabel(key: String): Int = when (key) {
    FrostedGlassScopeConfig.SCOPE_SECTIONS -> R.string.frosted_scope_sections
    FrostedGlassScopeConfig.SCOPE_CARDS -> R.string.frosted_scope_cards
    FrostedGlassScopeConfig.SCOPE_DIALOGS -> R.string.frosted_scope_dialogs
    FrostedGlassScopeConfig.SCOPE_NOTIFICATIONS -> R.string.frosted_scope_notifications
    else -> R.string.frosted_scope_cards
}

/**
 * 背景玻璃类型选择对话框（单选）
 */
@Composable
private fun BgGlassTypeSelectionDialog(onDismiss: () -> Unit) {
    val currentType by BackgroundGlassConfig.typeFlow.collectAsState()
    var selectedType by remember { mutableStateOf(currentType) }

    val typeList = listOf(
        BackgroundGlassConfig.GlassType.SANDBLASTED to R.string.bg_glass_sandblasted,
        BackgroundGlassConfig.GlassType.FROSTED to R.string.bg_glass_frosted,
        BackgroundGlassConfig.GlassType.GRID to R.string.bg_glass_grid,
        BackgroundGlassConfig.GlassType.MISTY to R.string.bg_glass_misty,
        BackgroundGlassConfig.GlassType.SILK to R.string.bg_glass_silk
    )

    GlassAlertDialogSimple(
        onDismissRequest = onDismiss,
        scrollableContent = {
            Text(
                text = stringResource(R.string.bg_glass_select_type),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            typeList.forEach { (type, labelRes) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedType = type }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (selectedType == type)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedType == type) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selectedType == type)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                    )
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
                        BackgroundGlassConfig.setType(selectedType)
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

@Composable
private fun ScriptManagementDialog(
    scriptManager: SourceScriptManager,
    onDismiss: () -> Unit,
    onScriptCountChanged: () -> Unit,
    onImport: () -> Unit
) {
    val scripts = remember { mutableStateOf(scriptManager.getScriptList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.source_script_management)) },
        text = {
            Column {
                if (scripts.value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_scripts),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(scripts.value, key = { it.id }) { script ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = script.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (script.description.isNotBlank()) {
                                        Text(
                                            text = script.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                TextButton(onClick = {
                                    try {
                                        scriptManager.removeScript(script.id)
                                        scripts.value = scriptManager.getScriptList()
                                        onScriptCountChanged()
                                    } catch (_: Exception) {}
                                }) {
                                    Text(
                                        text = stringResource(R.string.delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onImport) {
                Text(stringResource(R.string.import_script))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
