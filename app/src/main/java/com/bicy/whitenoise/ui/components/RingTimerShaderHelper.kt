package com.bicy.whitenoise.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect

private const val TIMER_RING_SHADER = """
    uniform float2 uSize;
    uniform float uProgress;
    uniform float uStrokeWidth;
    uniform float uCornerRadius;
    uniform half4 uColor;
    uniform shader contents;

    half4 main(float2 fragCoord) {
        half4 bg = contents.eval(fragCoord);
        float2 center = uSize * 0.5;
        float2 halfSize = uSize * 0.5;

        float2 d = abs(fragCoord - center) - (halfSize - uCornerRadius);
        float dist = length(max(d, float2(0.0))) + min(max(d.x, d.y), 0.0) - uCornerRadius;

        float halfStroke = uStrokeWidth * 0.5;
        float absDist = abs(dist);

        if (absDist > halfStroke) {
            return bg;
        }

        if (uProgress <= 0.0) return bg;

        float2 dir = fragCoord - center;
        float angle = (atan(dir.y, dir.x) + 1.5707963) / 6.2831853;

        if (angle > uProgress && uProgress < 1.0) {
            return bg;
        }

        float edgeFade = 1.5;
        float innerEdge = smoothstep(halfStroke - edgeFade, halfStroke, absDist);
        float outerEdge = 1.0 - smoothstep(halfStroke, halfStroke + edgeFade, absDist);
        float edgeAlpha = innerEdge * outerEdge;

        float angleAlpha = 1.0;
        if (uProgress < 1.0) {
            float angleFade = 0.02;
            angleAlpha = smoothstep(uProgress - angleFade, uProgress + angleFade, angle);
        }

        float alpha = uColor.a * edgeAlpha * angleAlpha;
        return half4(mix(bg.rgb, uColor.rgb, alpha), max(bg.a, alpha));
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class RingTimerShaderHelper {
    val shader = RuntimeShader(TIMER_RING_SHADER)

    fun updateUniforms(
        width: Float,
        height: Float,
        progress: Float,
        strokeWidth: Float,
        cornerRadius: Float,
        colorRed: Float,
        colorGreen: Float,
        colorBlue: Float,
        colorAlpha: Float
    ) {
        shader.setFloatUniform("uSize", width, height)
        shader.setFloatUniform("uProgress", progress)
        shader.setFloatUniform("uStrokeWidth", strokeWidth)
        shader.setFloatUniform("uCornerRadius", cornerRadius)
        shader.setFloatUniform("uColor", colorRed, colorGreen, colorBlue, colorAlpha)
    }

    fun getRenderEffect(): RenderEffect =
        RenderEffect.createRuntimeShaderEffect(shader, "contents")

    fun asComposeRenderEffect() = getRenderEffect().asComposeRenderEffect()
}
