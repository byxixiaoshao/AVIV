package com.bicy.whitenoise.yODW.NvYq.BxAd

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.bicy.whitenoise.yODW.SrEO.InteractiveSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bicy.whitenoise.R
import com.bicy.whitenoise.JwJY.EY9i.MusicDirectory
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorManager
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorPresets
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorScheme

@Composable
fun ThemeColorDialog(
    currentColorId: String,
    isPremiumUser: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onCustomColorSelected: (accent: Int, primary: Int, background: Int, text: Int) -> Unit,
    onUnlockPremium: () -> Unit
) {
    var selectedColorId by remember { mutableStateOf(currentColorId) }
    
    val presets = ThemeColorPresets.allPresets
    val customColors by ThemeColorManager.customColors.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_color_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(presets, key = { it.id }) { preset ->
                        ThemeColorItem(
                            themeColor = preset,
                            isSelected = selectedColorId == preset.id,
                            onClick = { selectedColorId = preset.id }
                        )
                    }
                    
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedColorId = "custom" }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (customColors != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(customColors!!.accent))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(customColors!!.primary))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(customColors!!.background))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(customColors!!.text))
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Text(
                                text = stringResource(R.string.custom_color),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedColorId == "custom") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedColorId) }) {
                Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ThemeColorItem(
    themeColor: ThemeColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(themeColor.primary),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = themeColor.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = themeColor.name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColors: ThemeColorManager.CustomColors?,
    onDismiss: () -> Unit,
    onConfirm: (accent: Int, primary: Int, background: Int, text: Int) -> Unit
) {
    val defaultAccent = initialColors?.accent?.let { Color(it) } ?: Color(0xFFB8A07A)
    val defaultPrimary = initialColors?.primary?.let { Color(it) } ?: Color(0xFFB8A07A)
    val defaultBackground = initialColors?.background?.let { Color(it) } ?: Color(0xFFFAF6F0)
    val defaultText = initialColors?.text?.let { Color(it) } ?: Color(0xFF3D3A35)
    
    var accentColor by remember { mutableStateOf(defaultAccent) }
    var primaryColor by remember { mutableStateOf(defaultPrimary) }
    var backgroundColor by remember { mutableStateOf(defaultBackground) }
    var textColor by remember { mutableStateOf(defaultText) }
    
    var selectedColorType by remember { mutableStateOf("accent") }
    
    val colorTypeLabels = listOf(
        "accent" to stringResource(R.string.accent_color),
        "primary" to stringResource(R.string.primary_color),
        "background" to stringResource(R.string.background_color),
        "text" to stringResource(R.string.text_color)
    )
    
    val currentColor = when (selectedColorType) {
        "accent" -> accentColor
        "primary" -> primaryColor
        "background" -> backgroundColor
        "text" -> textColor
        else -> accentColor
    }
    
    var hue by remember(selectedColorType) { 
        mutableStateOf(getHue(currentColor)) 
    }
    var saturation by remember(selectedColorType) { 
        mutableStateOf(getSaturation(currentColor)) 
    }
    var value by remember(selectedColorType) { 
        mutableStateOf(getValue(currentColor)) 
    }
    
    fun updateCurrentColor() {
        val newColor = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(hue, saturation, value)
            )
        )
        when (selectedColorType) {
            "accent" -> accentColor = newColor
            "primary" -> primaryColor = newColor
            "background" -> backgroundColor = newColor
            "text" -> textColor = newColor
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义颜色") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colorTypeLabels.forEach { (type, label) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedColorType = type }
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
                                    .then(
                                        if (selectedColorType == type) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Color.Gray, CircleShape)
                                        }
                                    )
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedColorType == type) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                
                Text(
                    text = stringResource(R.string.hue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                InteractiveSlider(
                    value = hue,
                    onValueChange = { 
                        hue = it
                        updateCurrentColor()
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.saturation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                InteractiveSlider(
                    value = saturation,
                    onValueChange = { 
                        saturation = it
                        updateCurrentColor()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.brightness),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                InteractiveSlider(
                    value = value,
                    onValueChange = { 
                        value = it
                        updateCurrentColor()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        accentColor.toArgb(),
                        primaryColor.toArgb(),
                        backgroundColor.toArgb(),
                        textColor.toArgb()
                    )
                }
            ) {
                Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SingleColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var currentColor by remember { mutableStateOf(initialColor) }
    
    var hue by remember { mutableStateOf(getHue(currentColor)) }
    var saturation by remember { mutableStateOf(getSaturation(currentColor)) }
    var value by remember { mutableStateOf(getValue(currentColor)) }
    
    fun updateColor() {
        currentColor = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(hue, saturation, value)
            )
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.hue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                InteractiveSlider(
                    value = hue,
                    onValueChange = { 
                        hue = it
                        updateColor()
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.saturation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                InteractiveSlider(
                    value = saturation,
                    onValueChange = { 
                        saturation = it
                        updateColor()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.brightness),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                InteractiveSlider(
                    value = value,
                    onValueChange = { 
                        value = it
                        updateColor()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentColor.toArgb()) }
            ) {
                Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

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

@Composable
fun MusicDirectoryDialog(
    directories: List<MusicDirectory>,
    isScanning: Boolean,
    onAddDirectory: () -> Unit,
    onRemoveDirectory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("音乐目录") },
        text = {
            Column {
                if (directories.isEmpty()) {
                    Text(
                        text = "尚未添加任何音乐目录\n点击下方按钮添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(directories, key = { it.path }) { dir ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dir.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = dir.path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1
                                    )
                                }
                                
                                IconButton(onClick = { onRemoveDirectory(dir.path) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (isScanning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.scanning_music),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAddDirectory) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.add_directory))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Suppress("DEPRECATION")
@Composable
fun UnlockPremiumDialog(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onPayClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (isPremium) stringResource(R.string.donate_support) else stringResource(R.string.unlock_premium),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isPremium) {
                    Text(
                        text = stringResource(R.string.thank_you_premium),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.qrcode_wechat),
                                contentDescription = stringResource(R.string.wechat_pay),
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.wechat_pay),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.qrcode_alipay),
                                contentDescription = stringResource(R.string.alipay),
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.alipay),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.unlock_premium_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val features = listOf(
                        Triple(stringResource(R.string.music_player), stringResource(R.string.basic_playback), stringResource(R.string.full_feature)),
                        Triple(stringResource(R.string.sound_quality_adjust), stringResource(R.string.basic_adjust), stringResource(R.string.full_feature)),
                        Triple(stringResource(R.string.sound_classification), stringResource(R.string.preset_classification), stringResource(R.string.full_feature)),
                        Triple(stringResource(R.string.theme_color), stringResource(R.string.preset_classification), stringResource(R.string.full_feature)),
                        Triple(stringResource(R.string.spatial_audio), stringResource(R.string.basic_function), stringResource(R.string.full_feature)),
                        Triple(stringResource(R.string.import_sounds), stringResource(R.string.not_supported), stringResource(R.string.full_feature)),
                        Triple(stringResource(R.string.custom_classification), stringResource(R.string.not_supported), stringResource(R.string.full_feature)),
                        Triple(stringResource(R.string.weekly_usage_limit), "7d", "168h")
                    )
                    
                    val headerColor = MaterialTheme.colorScheme.primary
                    val rowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.feature),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.normal_version),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.premium_version),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    
                    features.forEach { (feature, normal, premium) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = rowColor,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = normal,
                                style = MaterialTheme.typography.bodySmall,
                                color = rowColor,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = premium,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isPremium) {
                TextButton(onClick = onPayClick) {
                    Text(
                        text = stringResource(R.string.go_to_payment),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPremium) stringResource(R.string.close) else stringResource(R.string.cancel))
            }
        }
    )
}

@Suppress("DEPRECATION")
@Composable
fun ThankYouDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(R.string.thank_you_support),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.thank_you_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.thank_you_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.continue_),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Suppress("DEPRECATION")
@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_icon),
                    modifier = Modifier.size(80.dp),
                    tint = Color.Unspecified
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = versionName ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.open_source_license),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val openSourceLicenses = listOf(
                    "AndroidX" to "Apache 2.0",
                    "Material Components" to "Apache 2.0",
                    "Gson" to "Apache 2.0",
                    "Lottie" to "Apache 2.0",
                    "OkHttp" to "Apache 2.0",
                    "Jetpack Compose" to "Apache 2.0",
                    "Oboe" to "Apache 2.0",
                    "FFmpeg" to "LGPL v2.1+"
                )
                
                openSourceLicenses.forEach { (name, license) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = license,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.sound_source_credit),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.sound_source_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val soundAuthors = listOf(
                    "FeedTheStrayCats" to "雨",
                    "SoundReality" to "风",
                    "DRAGON-STUDIO" to "篝火、河流、键盘",
                    "Soul_Serenity_Sounds" to "咖啡馆",
                    "Colto" to "风扇",
                    "SSPsurvival" to "雪中漫步",
                    "freesound_community" to "其他音源"
                )
                
                soundAuthors.forEach { (author, sounds) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = sounds,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
fun EffectOrderDialog(
    currentOrder: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val effectNames = mapOf(
        "spatial" to stringResource(R.string.spatial_offset),
        "reverb" to stringResource(R.string.spatial_reverb),
        "equalizer" to stringResource(R.string.equalizer),
        "quality" to stringResource(R.string.quality_effect)
    )
    
    var order by remember { mutableStateOf(currentOrder) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.audio_effect_order_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.audio_effect_order_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column {
                    order.forEachIndexed { index, effectId ->
                        val effectName = effectNames[effectId] ?: effectId
                        
                        key(effectId) {
                            AnimatedContent(
                                targetState = index,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                                    } else {
                                        slideInVertically { height -> -height } + fadeIn() togetherWith
                                        slideOutVertically { height -> height } + fadeOut()
                                    }
                                },
                                label = "item_animation_$effectId"
                            ) { targetIndex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${targetIndex + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    
                                    Text(
                                        text = effectName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    if (targetIndex > 0) {
                                        IconButton(
                                            onClick = {
                                                val newOrder = order.toMutableList()
                                                val item = newOrder.removeAt(targetIndex)
                                                newOrder.add(targetIndex - 1, item)
                                                order = newOrder
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_arrow_up),
                                                contentDescription = "上移",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    if (targetIndex < order.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val newOrder = order.toMutableList()
                                                val item = newOrder.removeAt(targetIndex)
                                                newOrder.add(targetIndex + 1, item)
                                                order = newOrder
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_arrow_down),
                                                contentDescription = "下移",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (index < order.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(order)
                    onDismiss()
                }
            ) {
                Text(
                    text = "确定",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
