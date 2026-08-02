package com.bicy.whitenoise.ui.screens.SettingScreenPart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import com.bicy.whitenoise.ui.components.glass.GlassAlertDialogSimple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.bicy.whitenoise.ui.components.InteractiveSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bicy.whitenoise.R
import com.bicy.whitenoise.storage.music.MusicDirectory
import com.bicy.whitenoise.storage.theme.CustomThemeLibrary
import com.bicy.whitenoise.ui.theme.CustomTheme
import com.bicy.whitenoise.ui.theme.ThemeColorManager
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorPresets
import com.bicy.whitenoise.ui.theme.ThemeColorsPart.ThemeColorScheme
import com.bicy.whitenoise.ui.utils.ResponsiveDimensions
import com.bicy.whitenoise.utils.UsageStatsManager

@Composable
fun ThemeColorDialog(
    currentColorId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onCustomColorSelected: (accent: Int, primary: Int, background: Int, text: Int) -> Unit,
    onCreateCustomTheme: () -> Unit = {},
    onDeleteCustomTheme: (String) -> Unit = {},
    onEditCustomTheme: (ThemeColorScheme) -> Unit = {}
) {
    var selectedColorId by remember { mutableStateOf(currentColorId) }
    // 长按弹出的主题（非空时显示菜单）
    var longPressedTheme by remember { mutableStateOf<ThemeColorScheme?>(null) }
    // 等待二次确认删除的主题 id（非空时该卡片变红，点击确认删除）
    var pendingDeleteThemeId by remember { mutableStateOf<String?>(null) }

    val allThemes = com.bicy.whitenoise.storage.theme.CustomThemeLibrary.getAllThemesIncludingPresets()

    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.theme_color_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height((((allThemes.size + 1) / 4 + 1) * 70).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allThemes, key = { it.id }) { theme ->
                    val isPendingDelete = pendingDeleteThemeId == theme.id
                    ThemeColorGridItem(
                        themeColor = theme,
                        isSelected = selectedColorId == theme.id,
                        isPendingDelete = isPendingDelete,
                        onClick = {
                            if (isPendingDelete) {
                                // 二次确认删除
                                pendingDeleteThemeId = null
                                onDeleteCustomTheme(theme.id)
                            } else {
                                selectedColorId = theme.id
                            }
                        },
                        onLongClick = {
                            pendingDeleteThemeId = null
                            longPressedTheme = theme
                        }
                    )
                }

                // 添加按钮放在最后
                item {
                    ThemeColorCustomGridItem(
                        onClick = { onCreateCustomTheme() }
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
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                TextButton(
                    onClick = { onConfirm(selectedColorId) },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // 长按菜单（编辑/删除/复制）
    longPressedTheme?.let { theme ->
        val isCustom = com.bicy.whitenoise.storage.theme.CustomThemeLibrary.isCustomTheme(theme.id)
        ThemeLongPressMenuDialog(
            themeName = theme.name,
            isCustom = isCustom,
            onDismiss = { longPressedTheme = null },
            onEdit = {
                longPressedTheme = null
                onEditCustomTheme(theme)
            },
            onDelete = {
                longPressedTheme = null
                pendingDeleteThemeId = theme.id
            },
            onCopy = {
                longPressedTheme = null
                copyThemeAsNew(theme)
            }
        )
    }
}

/**
 * 复制主题色为新自定义主题，命名为"原色名_副本"
 * 若名称重复则自动追加数字后缀
 */
private fun copyThemeAsNew(theme: ThemeColorScheme) {
    var newName = "${theme.name}_副本"
    var suffix = 1
    while (com.bicy.whitenoise.storage.theme.CustomThemeLibrary.isThemeNameExists(newName)) {
        newName = "${theme.name}_副本${suffix}"
        suffix++
    }
    val newTheme = CustomTheme(
        id = java.util.UUID.randomUUID().toString(),
        name = newName,
        accent = theme.accent.toArgb(),
        primary = theme.primary.toArgb(),
        background = theme.background.toArgb(),
        text = theme.text.toArgb(),
        createdAt = System.currentTimeMillis()
    )
    com.bicy.whitenoise.storage.theme.CustomThemeLibrary.addTheme(newTheme)
}

/**
 * 主题色长按菜单：编辑/删除/复制
 * 仅自定义主题可编辑/删除；预设主题仅可复制
 */
@Composable
private fun ThemeLongPressMenuDialog(
    themeName: String,
    isCustom: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = themeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                if (isCustom) {
                    MenuRow(icon = Icons.Default.Edit, label = "编辑", onClick = onEdit)
                    MenuRow(
                        icon = Icons.Default.Delete,
                        label = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onDelete
                    )
                }
                MenuRow(icon = Icons.Default.ContentCopy, label = "复制", onClick = onCopy)
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemeColorGridItem(
    themeColor: ThemeColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    isPendingDelete: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(themeColor.primary)
                .then(
                    if (isPendingDelete) {
                        Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else if (isSelected) {
                        Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPendingDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = themeColor.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isPendingDelete) "再次点击删除" else themeColor.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isPendingDelete) MaterialTheme.colorScheme.error
                    else if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ThemeColorCustomGridItem(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 显示添加图标
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = stringResource(R.string.custom_color),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
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
    
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "自定义颜色",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                
                TextButton(
                    onClick = {
                        onConfirm(
                            accentColor.toArgb(),
                            primaryColor.toArgb(),
                            backgroundColor.toArgb(),
                            textColor.toArgb()
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SingleColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var currentColor by remember { mutableStateOf(initialColor) }
    
    // Tab: 0 = RGB, 1 = HSL
    var selectedTab by remember { mutableStateOf(0) }
    
    // RGB values
    var red by remember { mutableIntStateOf((initialColor.red * 255).toInt().coerceIn(0, 255)) }
    var green by remember { mutableIntStateOf((initialColor.green * 255).toInt().coerceIn(0, 255)) }
    var blue by remember { mutableIntStateOf((initialColor.blue * 255).toInt().coerceIn(0, 255)) }
    
    // HSL values
    var hue by remember { mutableStateOf(getHue(currentColor)) }
    var saturation by remember { mutableStateOf(getSaturation(currentColor)) }
    var value by remember { mutableStateOf(getValue(currentColor)) }
    
    // Hex input
    var hexInput by remember { mutableStateOf(String.format("#%02X%02X%02X", red, green, blue)) }
    
    fun updateColorFromRGB() {
        currentColor = Color(red, green, blue)
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(red, green, blue, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexInput = String.format("#%02X%02X%02X", red, green, blue)
    }
    
    fun updateColorFromHSL() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        currentColor = Color(argb)
        red = (currentColor.red * 255).toInt()
        green = (currentColor.green * 255).toInt()
        blue = (currentColor.blue * 255).toInt()
        hexInput = String.format("#%02X%02X%02X", red, green, blue)
    }
    
    fun parseHexInput(input: String) {
        val cleaned = input.trimStart('#').trim()
        if (cleaned.length == 6) {
            try {
                val parsed = cleaned.toLong(16).toInt()
                red = (parsed shr 16) and 0xFF
                green = (parsed shr 8) and 0xFF
                blue = parsed and 0xFF
                updateColorFromRGB()
            } catch (_: Exception) { }
        }
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Hex input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HEX",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    var hexField by remember { mutableStateOf(hexInput) }
                    androidx.compose.foundation.text.BasicTextField(
                        value = hexField,
                        onValueChange = { 
                            hexField = it
                            parseHexInput(it)
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tab bar
                val tabItems = listOf(
                    "RGB" to 0,
                    "HSL" to 1
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabItems.forEach { (label, index) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (selectedTab == index) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { 
                                    selectedTab = index
                                    // Sync values when switching tabs
                                    if (index == 0) {
                                        updateColorFromHSL()
                                    } else {
                                        updateColorFromRGB()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // RGB mode
                if (selectedTab == 0) {
                    Text(
                        text = "R (${red})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InteractiveSlider(
                        value = red.toFloat(),
                        onValueChange = { 
                            red = it.toInt()
                            updateColorFromRGB()
                        },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "G (${green})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InteractiveSlider(
                        value = green.toFloat(),
                        onValueChange = { 
                            green = it.toInt()
                            updateColorFromRGB()
                        },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "B (${blue})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InteractiveSlider(
                        value = blue.toFloat(),
                        onValueChange = { 
                            blue = it.toInt()
                            updateColorFromRGB()
                        },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // HSL mode
                if (selectedTab == 1) {
                    Text(
                        text = "${stringResource(R.string.hue)} (${hue.toInt()})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InteractiveSlider(
                        value = hue,
                        onValueChange = { 
                            hue = it
                            updateColorFromHSL()
                        },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${stringResource(R.string.saturation)} (${(saturation * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InteractiveSlider(
                        value = saturation,
                        onValueChange = { 
                            saturation = it
                            updateColorFromHSL()
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${stringResource(R.string.brightness)} (${(value * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InteractiveSlider(
                        value = value,
                        onValueChange = { 
                            value = it
                            updateColorFromHSL()
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // RGB/HSL hex values display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "#${String.format("%02X%02X%02X", red, green, blue)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "RGB($red, $green, $blue)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                    Text(stringResource(R.string.cancel))
                }
                
                TextButton(
                    onClick = { onConfirm(currentColor.toArgb()) },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
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
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "音乐目录",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                            
                            // 默认目录不可删除
                            if (!dir.isDefault) {
                                IconButton(onClick = { onRemoveDirectory(dir.path) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                Text(
                                    text = "默认",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.padding(horizontal = 8.dp)
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
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(stringResource(R.string.close))
                }
                
                TextButton(
                    onClick = onAddDirectory,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add_directory))
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun DonationDialog(
    onDismiss: () -> Unit
) {
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.donate_support),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.donate_encourage),
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
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
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
    
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ===== 顶部固定区域：关闭按钮 =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // ===== 顶部固定区域：应用图标、名称、版本 =====
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
            
            // ===== 下半部分：纵向滚动内容 =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 开源许可
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
                    "SoundTouch" to "LGPL v2.1",
                    "FFmpeg" to "LGPL v2.1+",
                    "Liquid Glass Android" to "Apache 2.0"
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
                
                // 音源致谢
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
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.sound_source_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 特别鸣谢
                Text(
                    text = stringResource(R.string.special_thanks),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.software_testing),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 硬编码用户名
                val testers = listOf(
                    "条纹哦里GHT",
                    "土豆仙人",
                    "AAA哈密瓜批发星见雅"
                )
                testers.forEach { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 12.dp).padding(vertical = 1.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.art_support),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val artists = listOf(
                    "AAA哈密瓜批发星见雅",
                    "☆雨の日が好き☔"
                )
                artists.forEach { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 12.dp).padding(vertical = 1.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
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
    
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss,
        scrollableContent = {
            Text(
                text = stringResource(R.string.audio_effect_order_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
        },
        bottomContent = {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "取消",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                TextButton(
                    onClick = {
                        onConfirm(order)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "确定",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}


@Composable
fun MediaControlPriorityDialog(
    currentPriority: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val options = listOf(
        "white_noise" to stringResource(R.string.media_control_white_noise),
        "music" to stringResource(R.string.media_control_music),
        "all" to stringResource(R.string.media_control_all),
        "smart" to stringResource(R.string.media_control_smart)
    )
    
    var selectedPriority by remember { mutableStateOf(currentPriority) }
    
    GlassAlertDialogSimple(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.media_control_priority_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            options.forEach { (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPriority = value }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (selectedPriority == value) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPriority == value) {
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
                            color = if (selectedPriority == value)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (value == "smart") {
                            Text(
                                text = stringResource(R.string.media_control_smart_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
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
                        onConfirm(selectedPriority)
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
    }
}

enum class HistoryType {
    WHITE_NOISE, MUSIC, TIMER, ALL
}

@Composable
fun UsageHistoryDialog(
    type: HistoryType = HistoryType.ALL,
    onDismiss: () -> Unit
) {
    val historyStats = UsageStatsManager.getHistoryStats()
    
    val titleText = when (type) {
        HistoryType.WHITE_NOISE -> "${stringResource(R.string.history_stats)} - ${stringResource(R.string.white_noise)}"
        HistoryType.MUSIC -> "${stringResource(R.string.history_stats)} - ${stringResource(R.string.music)}"
        HistoryType.TIMER -> "${stringResource(R.string.history_stats)} - ${stringResource(R.string.timer)}"
        HistoryType.ALL -> stringResource(R.string.history_dialog_title)
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
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (historyStats.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_history_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(historyStats) { stat ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = stat.date,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            when (type) {
                                HistoryType.WHITE_NOISE -> {
                                    Text(
                                        text = UsageStatsManager.formatDurationSimple(stat.whiteNoiseDuration),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                HistoryType.MUSIC -> {
                                    Text(
                                        text = UsageStatsManager.formatDurationSimple(stat.musicDuration),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                HistoryType.TIMER -> {
                                    Text(
                                        text = UsageStatsManager.formatDurationSimple(stat.timerDuration),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                HistoryType.ALL -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "WN: ${UsageStatsManager.formatDurationSimple(stat.whiteNoiseDuration)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Music: ${UsageStatsManager.formatDurationSimple(stat.musicDuration)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Timer: ${UsageStatsManager.formatDurationSimple(stat.timerDuration)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
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
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
