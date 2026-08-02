package com.bicy.whitenoise.ui.components.glass

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import org.intellij.lang.annotations.Language
import kotlin.random.Random

internal data class GlassElement(
    val id: String,
    val position: Offset,
    val size: Size,
    val scale: Float,
    val blur: Float,
    val centerDistortion: Float,
    val cornerRadius: Float,
    val elevation: Float,
    val tint: Color,
    val darkness: Float,
    val warpEdges: Float,
) {
    fun equalsWithTolerance(other: GlassElement): Boolean {
        if (id != other.id) return false
        val tolerance = 0.01f
        val positionDiff = (position - other.position)
        val positionDistance = kotlin.math.sqrt(positionDiff.x * positionDiff.x + positionDiff.y * positionDiff.y)
        return positionDistance < tolerance &&
                kotlin.math.abs(size.width - other.size.width) < tolerance &&
                kotlin.math.abs(size.height - other.size.height) < tolerance &&
                kotlin.math.abs(scale - other.scale) < tolerance &&
                kotlin.math.abs(blur - other.blur) < tolerance &&
                kotlin.math.abs(centerDistortion - other.centerDistortion) < tolerance &&
                kotlin.math.abs(cornerRadius - other.cornerRadius) < tolerance &&
                kotlin.math.abs(elevation - other.elevation) < tolerance &&
                kotlin.math.abs(darkness - other.darkness) < tolerance &&
                kotlin.math.abs(warpEdges - other.warpEdges) < tolerance &&
                tint == other.tint
    }
}

interface GlassScope {
    fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp = 0.dp,
        tint: Color = Color.Transparent,
        darkness: Float = 0f,
        warpEdges: Float = 0f,
    ): Modifier
}

interface GlassBoxScope : BoxScope, GlassScope

/** 全局GlassScope，由GlassContainer提供，子组件可直接读取并调用glassBackground */
val LocalGlassScope = staticCompositionLocalOf<GlassScope?> { null }

@Composable
fun GlassBoxScope.GlassBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    @FloatRange(from = 0.0, to = 1.0)
    scale: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    blur: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    centerDistortion: Float = 0f,
    shape: CornerBasedShape = RoundedCornerShape(0.dp),
    elevation: Dp = 0.dp,
    tint: Color = Color.Transparent,
    @FloatRange(from = 0.0, to = 1.0)
    darkness: Float = 0f,
    @FloatRange(from = 0.0, to = 1.0)
    warpEdges: Float = 0f,
    content: @Composable BoxScope.() -> Unit = { },
) {
    val id = remember { Random.nextLong() }
    Box(
        modifier = modifier.glassBackground(
            id,
            scale.coerceIn(0f, 1f),
            blur.coerceIn(0f, 1f),
            centerDistortion.coerceIn(0f, 1f),
            shape,
            elevation,
            tint,
            darkness.coerceIn(0f, 1f),
            warpEdges.coerceIn(0f, 1f)
        ),
        contentAlignment, propagateMinConstraints, content
    )
}

private class GlassBoxScopeImpl(
    boxScope: BoxScope,
    glassScope: GlassScope
) : GlassBoxScope, BoxScope by boxScope, GlassScope by glassScope

private class GlassScopeImpl(private val density: Density) : GlassScope {
    val elements: MutableList<GlassElement> = mutableStateListOf()
    private val activeElements = mutableSetOf<String>()

    fun markElementAsActive(elementId: String) {
        activeElements.add(elementId)
    }

    fun cleanupInactiveElements() {
        val elementsToRemove = elements.filter { it.id !in activeElements }
        if (elementsToRemove.isNotEmpty()) {
            elements.removeAll { it.id !in activeElements }
        }
        activeElements.clear()
    }

    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
    ): Modifier = this
        .background(color = Color.Transparent, shape = shape)
        .onGloballyPositioned { coordinates ->
            val elementId = "glass_$id"
            markElementAsActive(elementId)
            val position = coordinates.positionInRoot()
            val size = coordinates.size.toSize()
            val element = GlassElement(
                id = elementId,
                position = position,
                size = size,
                cornerRadius = shape.topStart.toPx(size, density),
                scale = scale,
                blur = blur,
                centerDistortion = centerDistortion,
                elevation = with(density) { elevation.toPx() },
                tint = tint,
                darkness = darkness,
                warpEdges = warpEdges,
            )
            val existingIndex = elements.indexOfFirst { it.id == element.id }
            if (existingIndex == -1) {
                elements.add(element)
            } else {
                val existing = elements[existingIndex]
                if (!existing.equalsWithTolerance(element)) {
                    elements[existingIndex] = element
                }
            }
        }
}

private class GlassScopeFallbackImpl(private val density: Density) : GlassScope {
    override fun Modifier.glassBackground(
        id: Long,
        scale: Float,
        blur: Float,
        centerDistortion: Float,
        shape: CornerBasedShape,
        elevation: Dp,
        tint: Color,
        darkness: Float,
        warpEdges: Float,
    ): Modifier {
        val glassTint = if (tint == Color.Transparent) {
            Color.White.copy(alpha = 0.1f)
        } else {
            tint.copy(alpha = (tint.alpha * 0.9f).coerceIn(0f, 1f))
        }
        val darknessOverlay = if (darkness > 0f) {
            Color.Black.copy(alpha = darkness * 0.3f)
        } else {
            Color.Transparent
        }
        val glassGradient = Brush.verticalGradient(
            colors = listOf(
                glassTint,
                glassTint.copy(alpha = glassTint.alpha * 0.7f),
                glassTint.copy(alpha = glassTint.alpha * 0.5f),
                glassTint
            )
        )
        return this
            .background(brush = glassGradient, shape = shape)
            .let { modifier ->
                if (darknessOverlay != Color.Transparent) {
                    modifier.background(color = darknessOverlay, shape = shape)
                } else {
                    modifier
                }
            }
            .let { modifier ->
                if (scale > 0f) {
                    modifier.graphicsLayer {
                        scaleX = 1f + (scale * 0.1f)
                        scaleY = 1f + (scale * 0.1f)
                    }
                } else {
                    modifier
                }
            }
            .let { modifier ->
                if (warpEdges > 0f) {
                    modifier.alpha(1f - (warpEdges * 0.2f).coerceIn(0f, 0.8f))
                } else {
                    modifier
                }
            }
    }
}

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassContainerWithShader(modifier, content, glassContent)
    } else {
        GlassContainerFallback(modifier, content, glassContent)
    }
}

@SuppressLint("NewApi")
@Composable
private fun GlassContainerWithShader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeImpl(density) }
    // Shader只创建一次，避免频繁重建导致的重载感
    val shader = remember { RuntimeShader(GLASS_DISPLACEMENT_SHADER) }

    SideEffect {
        glassScope.cleanupInactiveElements()
    }

    DisposableEffect(Unit) {
        onDispose {
            glassScope.elements.clear()
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                shader.setFloatUniform("resolution", size.width, size.height)
                val elements = glassScope.elements
                val maxElements = 10
                val positions = FloatArray(maxElements * 2)
                val sizes = FloatArray(maxElements * 2)
                val scales = FloatArray(maxElements)
                val radii = FloatArray(maxElements)
                val elevations = FloatArray(maxElements)
                val centerDistortions = FloatArray(maxElements)
                val tints = FloatArray(maxElements * 4)
                val darkness = FloatArray(maxElements)
                val warpEdges = FloatArray(maxElements)
                val blurs = FloatArray(maxElements)

                val elementsCount = minOf(elements.size, maxElements)
                shader.setIntUniform("elementsCount", elementsCount)

                for (i in 0 until elementsCount) {
                    val element = elements[i]
                    positions[i * 2] = element.position.x
                    positions[i * 2 + 1] = element.position.y
                    sizes[i * 2] = element.size.width
                    sizes[i * 2 + 1] = element.size.height
                    scales[i] = element.scale
                    radii[i] = element.cornerRadius
                    elevations[i] = element.elevation
                    centerDistortions[i] = element.centerDistortion
                    tints[i * 4] = element.tint.red
                    tints[i * 4 + 1] = element.tint.green
                    tints[i * 4 + 2] = element.tint.blue
                    tints[i * 4 + 3] = element.tint.alpha
                    darkness[i] = element.darkness
                    warpEdges[i] = element.warpEdges
                    blurs[i] = element.blur
                }

                shader.setFloatUniform("glassPositions", positions)
                shader.setFloatUniform("glassSizes", sizes)
                shader.setFloatUniform("glassScales", scales)
                shader.setFloatUniform("cornerRadii", radii)
                shader.setFloatUniform("elevations", elevations)
                shader.setFloatUniform("centerDistortions", centerDistortions)
                shader.setFloatUniform("glassTints", tints)
                shader.setFloatUniform("glassDarkness", darkness)
                shader.setFloatUniform("glassWarpEdges", warpEdges)
                shader.setFloatUniform("glassBlurs", blurs)

                renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "contents")
                    .asComposeRenderEffect()
            }
    ) {
        CompositionLocalProvider(LocalGlassScope provides glassScope) {
            content()
        }
    }
    Box(modifier = modifier) {
        CompositionLocalProvider(LocalGlassScope provides glassScope) {
            GlassBoxScopeImpl(this, glassScope).glassContent()
        }
    }
}

@Composable
private fun GlassContainerFallback(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    glassContent: @Composable GlassBoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val glassScope = remember { GlassScopeFallbackImpl(density) }
    Box(modifier = modifier) {
        CompositionLocalProvider(LocalGlassScope provides glassScope) {
            content()
        }
    }
    Box(modifier = modifier) {
        CompositionLocalProvider(LocalGlassScope provides glassScope) {
            GlassBoxScopeImpl(this, glassScope).glassContent()
        }
    }
}

@Language("AGSL")
private val GLASS_DISPLACEMENT_SHADER = """
    uniform float2 resolution;
    uniform shader contents;
    uniform int elementsCount;
    uniform float2 glassPositions[10];
    uniform float2 glassSizes[10];
    uniform float glassScales[10];
    uniform float cornerRadii[10];
    uniform float elevations[10];
    uniform float centerDistortions[10];
    uniform float glassTints[40];
    uniform float glassDarkness[10];
    uniform float glassWarpEdges[10];
    uniform float glassBlurs[10];

    // 预计算的 7×7 高斯模糊 1D 权重，消除每像素 49 次 exp() 调用
    // G(x) = exp(-x² * 0.32)，核大小 7，sigma ≈ 1.25
    // AGSL 不支持 const 数组初始化器，使用独立常量 + 辅助函数
    const float W0 = 0.0561; const float W1 = 0.2780; const float W2 = 0.7261;
    const float W3 = 1.0;    const float W4 = 0.7261; const float W5 = 0.2780;
    const float W6 = 0.0561;
    
    float blurWeight(int idx) {
        if (idx == 0) return W0; if (idx == 1) return W1;
        if (idx == 2) return W2; if (idx == 3) return W3;
        if (idx == 4) return W4; if (idx == 5) return W5;
        return W6;
    }

    float sdfRoundedRect(float2 p, float2 halfSize, float radius) {
        float2 d = abs(p) - halfSize + radius;
        return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - radius;
    }

    // 合并版：一次性计算 warp 判区域定 + 扭曲坐标，消除重复的 inset/innerSize/innerRadius 计算
    float2 applyWarp(float2 localCoord, float2 halfSize, float cornerRadius, float warpEdges) {
        if (warpEdges <= 0.0) return localCoord;
        float outerSdf = sdfRoundedRect(localCoord, halfSize, cornerRadius);
        if (outerSdf >= 0.0) return localCoord;
        float inset = warpEdges * min(halfSize.x, halfSize.y) * 0.5;
        float2 innerSize = max(halfSize - inset, 0.1);
        float innerRadius = max(cornerRadius * min(innerSize.x / halfSize.x, innerSize.y / halfSize.y), 0.0);
        float innerSdf = sdfRoundedRect(localCoord, innerSize, innerRadius);
        if (innerSdf <= 0.0) return localCoord;
        float normalizedDist = clamp(innerSdf / inset, 0.0, 1.0);
        float warpIntensity = normalizedDist * normalizedDist * warpEdges;
        float pullStrength = warpIntensity * 0.8;
        float targetScale = max(0.1, 1.0 - pullStrength);
        float2 pulledCoord = localCoord * targetScale;
        float2 centerDir = normalize(localCoord);
        float2 radialOffset = centerDir * (warpIntensity * 0.03 * length(localCoord));
        if (warpEdges > 0.7 && normalizedDist > 0.8) {
            float angle = atan(localCoord.y, localCoord.x) + normalizedDist * warpEdges * 0.5;
            float r = length(pulledCoord);
            pulledCoord = float2(cos(angle), sin(angle)) * r;
        }
        return pulledCoord + radialOffset;
    }

    float2 applyLensEffect(float2 fragCoord, float2 center, float2 halfSize, float cornerRadius, 
                           float scale, float centerDistortion, float sdf) {
        if (scale <= 0.0) return fragCoord;
        if (sdf >= 0.0) return fragCoord;
        float2 rel = (fragCoord - center) / halfSize;
        float normalizedDist = length(rel) / 1.414;
        float baseScale = 1.0 + scale;
        float distortionFactor = 1.0;
        if (centerDistortion > 0.0) {
            float profile = 1.0 - smoothstep(0.0, 1.0, normalizedDist);
            distortionFactor = 1.0 + centerDistortion * profile;
        }
        float finalScale = baseScale * distortionFactor;
        return center + (fragCoord - center) / finalScale;
    }

    // 直接接收预计算的 sdf，避免重复计算
    float getShadowIntensity(float2 localCoord, float2 halfSize, float cornerRadius, float elevation, float sdf) {
        if (elevation <= 0.0 || sdf <= 0.0) return 0.0;
        float shadowOffset = elevation * 0.5;
        float shadowBlur = elevation * 2.0;
        float shadowSdf = sdfRoundedRect(localCoord - float2(0.0, shadowOffset), halfSize, cornerRadius);
        if (shadowSdf > shadowBlur) return 0.0;
        return (1.0 - shadowSdf / shadowBlur) * 0.15;
    }

    // 直接接收预计算的 sdf，避免重复计算
    float getRimHighlight(float sdf, float verticalRatio) {
        float rimWidth = 5.0;
        if (sdf <= 0.0 || sdf >= rimWidth) return 0.0;
        float intensity = (rimWidth - sdf) / rimWidth;
        float lightingFactor = mix(1.2, 0.7, (verticalRatio + 1.0) * 0.5);
        return intensity * 0.8 * lightingFactor;
    }

    float4 main(float2 fragCoord) {
        // 像素剔除：AABB 快速路径，不在任何玻璃元素影响范围内的像素直接跳过
        bool nearAnyElement = false;
        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            float pad = glassBlurs[i] * 25.0 + 15.0;
            float2 center = glassPositions[i] + glassSizes[i] * 0.5;
            float2 halfSize = glassSizes[i] * 0.5 + pad;
            float2 diff = abs(fragCoord - center) - halfSize;
            if (max(diff.x, diff.y) < 0.0) { nearAnyElement = true; break; }
        }
        if (!nearAnyElement) return contents.eval(fragCoord);

        float2 finalCoord = fragCoord;
        float shadowAlpha = 0.0;
        float rimHighlight = 0.0;
        float4 tintColor = float4(0.0);
        float darknessEffect = 0.0;
        float blurRadius = 0.0;
        float2 surfaceNormal = float2(0.0);

        for (int i = 0; i < 10; i++) {
            if (i >= elementsCount) break;
            // 每个元素的常量：仅计算一次
            float2 center = glassPositions[i] + glassSizes[i] * 0.5;
            float2 localCoord = fragCoord - center;
            float2 halfSize = glassSizes[i] * 0.5;
            float cr = cornerRadii[i];
            float sdf = sdfRoundedRect(localCoord, halfSize, cr);

            // 累积模糊半径
            if (sdf < 0.0 && glassBlurs[i] > 0.0) {
                blurRadius = max(blurRadius, glassBlurs[i] * 20.0);
            }

            // 合并 warp 判定 + 扭曲：消除原 getWarpRegion + applyWarpDistortion 的重复计算
            if (glassWarpEdges[i] > 0.0) {
                float2 warpedCoord = applyWarp(localCoord, halfSize, cr, glassWarpEdges[i]);
                if (warpedCoord.x != localCoord.x || warpedCoord.y != localCoord.y) {
                    float2 warpedFragCoord = center + warpedCoord;
                    // 对 warp 后的坐标透镜前先判断 SDF
                    float warpSdf = sdfRoundedRect(warpedFragCoord - center, halfSize, cr);
                    finalCoord = applyLensEffect(warpedFragCoord, center, halfSize, cr,
                                               glassScales[i], centerDistortions[i], warpSdf);
                } else {
                    finalCoord = applyLensEffect(finalCoord, center, halfSize, cr,
                                               glassScales[i], centerDistortions[i], sdf);
                }
            } else {
                finalCoord = applyLensEffect(finalCoord, center, halfSize, cr,
                                           glassScales[i], centerDistortions[i], sdf);
            }

            // 阴影 + 高光：传入预计算的 sdf，消除函数内重复 SDF 计算
            shadowAlpha = max(shadowAlpha, getShadowIntensity(localCoord, halfSize, cr, elevations[i], sdf));
            rimHighlight = max(rimHighlight, getRimHighlight(sdf, localCoord.y / halfSize.y));

            // 表面法线（仅首个有效边缘采样）
            if (sdf > 0.0 && sdf < 4.0 && surfaceNormal.x == 0.0 && surfaceNormal.y == 0.0) {
                float epsilon = 1.0;
                float sdfX = sdfRoundedRect(localCoord + float2(epsilon, 0.0), halfSize, cr);
                float sdfY = sdfRoundedRect(localCoord + float2(0.0, epsilon), halfSize, cr);
                surfaceNormal = normalize(float2(sdfX - sdf, sdfY - sdf));
            }

            // 元素内部：着色 + 暗度
            if (sdf < 0.0) {
                float4 elementTint = float4(glassTints[i * 4], glassTints[i * 4 + 1],
                                          glassTints[i * 4 + 2], glassTints[i * 4 + 3]);
                if (elementTint.a > 0.0) {
                    tintColor = mix(tintColor, elementTint, elementTint.a);
                }
                float currentDarkness = glassDarkness[i];
                if (currentDarkness > 0.0) {
                    float maxRadius = min(halfSize.x, halfSize.y) * 0.8;
                    float distanceFromEdge = abs(sdf);
                    if (distanceFromEdge < maxRadius) {
                        float intensity = smoothstep(0.0, 1.0, (maxRadius - distanceFromEdge) / maxRadius);
                        darknessEffect = max(darknessEffect, currentDarkness * intensity);
                    }
                }
            }
        }

        float4 color = contents.eval(finalCoord);

        // 高斯模糊：使用预计算权重表替代 49 次 exp() 调用
        if (blurRadius > 0.0) {
            float4 blurredColor = float4(0.0);
            float totalWeight = 0.0;
            float stepSize = blurRadius * 0.4;
            for (int dx = -3; dx <= 3; dx++) {
                float wx = blurWeight(dx + 3);
                for (int dy = -3; dy <= 3; dy++) {
                    float weight = wx * blurWeight(dy + 3);
                    float2 offset = float2(float(dx), float(dy)) * stepSize;
                    blurredColor += contents.eval(finalCoord + offset) * weight;
                    totalWeight += weight;
                }
            }
            color = blurredColor / totalWeight;
        }

        if (tintColor.a > 0.0) {
            color.rgb = mix(color.rgb, tintColor.rgb, tintColor.a * 0.9);
        }

        if (darknessEffect > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), darknessEffect * 0.5);
        }

        if (rimHighlight > 0.0) {
            float2 reflectionOffset = surfaceNormal * 24.0;
            float4 reflectedColor = contents.eval(fragCoord + reflectionOffset);
            reflectedColor.rgb = max(reflectedColor.rgb * 1.8 + 0.35, 0.15);
            color = mix(color, reflectedColor, rimHighlight);
        }

        if (shadowAlpha > 0.0) {
            color.rgb = mix(color.rgb, float3(0.0), shadowAlpha);
        }

        return color;
    }
""".trimIndent()