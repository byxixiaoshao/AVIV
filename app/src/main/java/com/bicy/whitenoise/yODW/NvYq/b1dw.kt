package com.bicy.whitenoise.yODW.NvYq

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bicy.whitenoise.R
import com.bicy.whitenoise.JwJY.EY9i.MusicStorage
import com.bicy.whitenoise.xnef.MusicLibrary
import com.bicy.whitenoise.xnef.MusicPlayerController
import com.bicy.whitenoise.rgRE.MusicService
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.yODW.NvYq.BxAd.AboutDialog
import com.bicy.whitenoise.yODW.NvYq.BxAd.ContentPaddingTop
import com.bicy.whitenoise.yODW.NvYq.BxAd.EffectOrderDialog
import com.bicy.whitenoise.yODW.NvYq.BxAd.MusicDirectoryDialog
import com.bicy.whitenoise.yODW.NvYq.BxAd.SettingClickItem
import com.bicy.whitenoise.yODW.NvYq.BxAd.SettingSliderItem
import com.bicy.whitenoise.yODW.NvYq.BxAd.SettingSliderWithCheckboxItem
import com.bicy.whitenoise.yODW.NvYq.BxAd.SettingSwitchItem
import com.bicy.whitenoise.yODW.NvYq.BxAd.SingleColorPickerDialog
import com.bicy.whitenoise.yODW.NvYq.BxAd.ThankYouDialog
import com.bicy.whitenoise.yODW.NvYq.BxAd.ThemeColorDialog
import com.bicy.whitenoise.yODW.NvYq.BxAd.UnlockPremiumDialog
import com.bicy.whitenoise.yODW.NvYq.BxAd.UserLevelCard
import com.bicy.whitenoise.yODW.NvYq.BxAd.getFullPathFromUri
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorManager
import com.bicy.whitenoise.yODW.nU5N.SettingsViewModel
import com.bicy.whitenoise.y10p.LogManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val autoPlayEnabled by viewModel.autoPlayEnabled.collectAsState()
    val musicDirectories by MusicStorage.directories.collectAsState()
    val scanProgress by MusicLibrary.scanProgress.collectAsState()
    val globalState by ConfigStorage.config.collectAsState()
    val currentThemeColor by ThemeColorManager.currentThemeColor.collectAsState()
    val customColors by ThemeColorManager.customColors.collectAsState()
    
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var showMusicDirDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var showThankDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showEffectOrderDialog by remember { mutableStateOf(false) }
    var editingColorType by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
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
                .fillMaxSize()
                .padding(top = ContentPaddingTop)
        ) {
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
                    isPremium = globalState.isPremium,
                    onUnlockClick = { showUnlockDialog = true }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.music),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            if (globalState.isPremium) {
                item {
                    SettingClickItem(
                        title = stringResource(R.string.music_directory),
                        value = "${musicDirectories.size} ${stringResource(R.string.directories_count)}",
                        onClick = { showMusicDirDialog = true }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.personalization),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            item {
                SettingClickItem(
                    title = stringResource(R.string.theme_color),
                    value = currentThemeColor.name,
                    onClick = { showThemeColorDialog = true }
                )
            }
            
            if (currentThemeColor.id == "custom" && globalState.isPremium) {
                item {
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
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                SettingSliderWithCheckboxItem(
                    title = stringResource(R.string.viz_wn_sensitivity),
                    checked = globalState.vizWnEnabled,
                    onCheckedChange = { enabled -> ConfigStorage.setVizWnEnabled(enabled) },
                    value = globalState.vizWnSensitivity,
                    onValueChange = { value -> ConfigStorage.setVizWnSensitivity(value) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                SettingSliderWithCheckboxItem(
                    title = stringResource(R.string.viz_music_sensitivity),
                    checked = globalState.vizMusicEnabled,
                    onCheckedChange = { enabled -> ConfigStorage.setVizMusicEnabled(enabled) },
                    value = globalState.vizMusicSensitivity,
                    onValueChange = { value -> ConfigStorage.setVizMusicSensitivity(value) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                SettingSliderWithCheckboxItem(
                    title = stringResource(R.string.viz_flash_sensitivity),
                    checked = globalState.vizFlashEnabled,
                    onCheckedChange = { enabled -> ConfigStorage.setVizFlashEnabled(enabled) },
                    value = globalState.vizFlashSensitivity,
                    onValueChange = { value -> ConfigStorage.setVizFlashSensitivity(value) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                SettingSliderItem(
                    title = stringResource(R.string.viz_refresh_rate),
                    value = globalState.vizRefreshRate,
                    onValueChange = { ConfigStorage.setVizRefreshRate(it) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.general),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.auto_play_on_startup),
                    checked = autoPlayEnabled,
                    onCheckedChange = { viewModel.setAutoPlay(it) }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                SettingClickItem(
                    title = stringResource(R.string.audio_effect_order),
                    value = stringResource(R.string.customize),
                    onClick = { showEffectOrderDialog = true }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                SettingSwitchItem(
                    title = stringResource(R.string.log_recording),
                    checked = globalState.logEnabled,
                    onCheckedChange = { enabled ->
                        ConfigStorage.setLogEnabled(enabled)
                        LogManager.setLogEnabled(enabled)
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                SettingClickItem(
                    title = stringResource(R.string.about_app),
                    value = "BicyOne",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }
    
    if (showThemeColorDialog) {
        ThemeColorDialog(
            currentColorId = ThemeColorManager.getCurrentColorId(),
            isPremiumUser = globalState.isPremium,
            onDismiss = { showThemeColorDialog = false },
            onConfirm = { colorId ->
                ThemeColorManager.setThemeColor(colorId)
                showThemeColorDialog = false
            },
            onCustomColorSelected = { accent, primary, background, text ->
                ThemeColorManager.setCustomColors(accent, primary, background, text)
                showThemeColorDialog = false
            },
            onUnlockPremium = {
                showThemeColorDialog = false
                showUnlockDialog = true
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
    
    if (showUnlockDialog) {
        UnlockPremiumDialog(
            isPremium = globalState.isPremium,
            onDismiss = { showUnlockDialog = false },
            onPayClick = {
                showUnlockDialog = false
                showThankDialog = true
            }
        )
    }
    
    if (showThankDialog) {
        ThankYouDialog(
            onConfirm = {
                showThankDialog = false
                ConfigStorage.setPremiumUser(true)
            },
            onDismiss = { showThankDialog = false }
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
}
