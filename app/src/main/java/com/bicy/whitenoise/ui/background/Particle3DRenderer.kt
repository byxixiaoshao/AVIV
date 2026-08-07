package com.bicy.whitenoise.ui.background

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.bicy.whitenoise.audio.aVzM
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.service.AnomalyType
import com.bicy.whitenoise.service.MemoryLockService
import com.bicy.whitenoise.storage.config.Filament3DConfig
import com.bicy.whitenoise.ui.theme.ThemeColorManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sin

private const val TAG = "Particle3D"

// ── 3D 渲染内存锁阈值: 用户调整粒子配置时的内存防护 ──
private const val GL_MEMORY_CHECK_DELAY_MS = 600L   // 重建后延迟校验, 等分配稳定
private const val GL_HEAP_CRITICAL_RATIO = 0.85f    // Dalvik 堆占比上限(对齐内存锁 MEMORY_HIGH_RATIO)
private const val GL_PSS_DELTA_KB = 220 * 1024      // 单次调整新增 PSS > 220MB 视为异常
private const val GL_PSS_ABSOLUTE_KB = 1024 * 1024  // 进程总 PSS > 1GB 视为异常

// 对齐 web index.html: 边长粒子数量(edge×edge 网格), 默认边长 55 → 55×55 = 3025 粒子
private const val DEFAULT_EDGE = 55
private const val MIN_EDGE = 10
private const val MAX_EDGE = 2000
// 对齐 web: camera z=12, FOV=60°, PLANE_SIZE=4.8 (正方形平面)
private const val FOV_Y_DEG = 60.0f
private const val CAM_DIST = 12.0f
private const val PLANE_SIZE = 4.8f
// 散开坐标基准半径(归一化≈1, 实际由 uScatterRadius 缩放), 云团半径 1 → 实际 3.5 占屏比例合适
private const val SCATTER_BASE = 1.0f
// 顶点: position(3) + rand(1) + uv(2) + coverColor(3) + scatterPos(3) = 12 float = 48 bytes
private const val STRIDE = 12 * 4
// 顶点内各 attribute 起始偏移(float 数)
private const val OFF_RAND = 3
private const val OFF_UV = 4
private const val OFF_COVER = 6
private const val OFF_SCATTER = 9

// ── 粒子顶点着色器（完全对齐 3d-particle-preview/index.html）──
private const val VS_SRC = """
uniform mat4 uMVP;
uniform mat4 uModel;
uniform vec3 uThemeColor;
uniform float uTime, uBass, uMid, uTreble, uPixelRatio;
uniform float uSpeed, uPulse, uGlowStrength, uGlowPulse, uGlowDark, uGlowBright, uPointScale, uHasCover;
uniform float uScatterRadius;
uniform float uGlowSize;
attribute vec3 aPosition;
attribute float aRand;
attribute vec2 aUv;
attribute vec3 aCoverColor;
attribute vec3 aScatterPos;
varying vec3 vColor;
varying float vGlow;
varying float vAlpha;
void main() {
    float t = uTime * uSpeed;
    // 无专辑时粒子从平面网格 3D 散开(围绕中心 bloom), 有封面时平滑收回网格拼封面
    vec3 pos = mix(aScatterPos * uScatterRadius, aPosition, uHasCover);
    float breath = uBass * 1.3;
    float wave = sin(pos.x * 3.0 + t * 2.2) * cos(pos.y * 3.0 - t * 1.6);
    float trebleJ = sin(t * 6.0 + aRand * 21.0) * uTreble * 0.30;
    float ripple = sin(pos.x * 1.3 + t * 1.2) * cos(pos.y * 1.3 - t * 0.8) * 0.18;
    float animK = 1.0 - uHasCover * 0.4;
    pos.xy += vec2(sin(pos.y * 4.0 + t * 2.6), cos(pos.x * 4.0 - t * 2.2)) * uMid * 0.14 * uPulse * (1.0 - uHasCover);
    pos.z += breath * 0.45 * animK;
    pos.z += wave * uMid * 0.55 * uPulse * animK;
    pos.z += trebleJ * animK;
    pos.z += ripple * animK;
    // 封面模式: 直接跟 bass 包络脉冲(鼓点一来封面整体前冲), 而非纯周期正弦呼吸
    pos.z += uHasCover * uBass * 0.4;
    vec3 color = mix(uThemeColor, aCoverColor, uHasCover);
    float drive = clamp(uBass * 1.5 + uMid * 0.8 + uTreble * 0.4 + 0.35 * sin(t * 3.0 + aRand * 9.0), 0.0, 1.0);
    float glow;
    if (uGlowPulse > 0.5) {
        glow = mix(uGlowDark, uGlowBright, drive);
    } else {
        glow = uGlowStrength * (0.7 + 0.3 * drive);
    }
    vGlow = mix(glow, 1.0, uHasCover * 0.5);
    vColor = color;
    vAlpha = 1.0;
    vec4 worldPos = uModel * vec4(pos, 1.0);
    gl_Position = uMVP * worldPos;
    float depthSize = 36.0 / max(0.5, gl_Position.w);
    float audioBoost = 1.0 + uBass * 0.6 + uMid * 0.35 + uTreble * 0.5;
    float sz = clamp(depthSize * audioBoost, 1.05, 5.5);
    gl_PointSize = sz * uPixelRatio * uPointScale * uGlowSize;
}
"""

private const val FS_SRC = """
precision mediump float;
varying vec3 vColor;
varying float vGlow;
varying float vAlpha;
uniform float uGlowPass;   // 1=核心层, 0=光晕层
uniform float uGlowAlpha;  // 光晕层透明度(档位控制)
void main() {
    vec2 uv = gl_PointCoord - 0.5;
    float dist = length(uv);
    if (uGlowPass > 0.5) {
        // 核心层: 硬边圆形 (NormalBlending, 保留封面原色)
        if (dist > 0.5) discard;
        float alpha = 1.0 - smoothstep(0.2, 0.5, dist);
        gl_FragColor = vec4(vColor * vGlow, alpha * vAlpha);
    } else {
        // 光晕层: 大尺寸径向柔和扩散 (AdditiveBlending, 叠加成发光星云)
        float g = pow(max(0.0, 1.0 - dist * 1.7), 2.6);
        if (g < 0.012) discard;
        gl_FragColor = vec4(vColor * vGlow * 1.2, g * uGlowAlpha);
    }
}
"""

// ── 中心光晕: 相机面向 XY 平面(纯平移 view), 中心放一个随低频呼吸缩放的 billboard 光幕 ──
// 样式: 0经典柔光 / 1十字光芒 / 2星芒 / 3环形光波。先于粒子绘制 → 光晕在粒子后方。
// 半圆问题根因(已修复): 旧实现是 3D 球体 + 法线朝向符号写反, 只有背面半球亮 → 视觉残缺半圆;
// 改为 billboard 后不再依赖法线朝向, 任何角度都是完整圆形光晕。
private const val HALO_VS = """
attribute vec2 aPosition;
uniform mat4 uMVP;
varying vec2 vUv;
void main() {
    vUv = aPosition;
    gl_Position = uMVP * vec4(aPosition, 0.0, 1.0);
}
"""

private const val HALO_FS = """
precision mediump float;
uniform vec3 uColor;
uniform float uBass, uHasCover, uTime, uStyle;
varying vec2 vUv;
void main() {
    vec2 uv = vUv;
    float d = length(uv);
    if (d > 1.0) discard;
    float amp = (0.5 + uBass * 1.3) * (1.0 - uHasCover);
    float alpha;
    if (uStyle < 0.5) {
        // 经典柔光: 柔和径向光球
        alpha = pow(max(0.0, 1.0 - d), 2.4);
    } else if (uStyle < 1.5) {
        // 十字光芒: 光球 + 水平/垂直光条 (anamorphic 镜头耀斑)
        float ball = pow(max(0.0, 1.0 - d), 2.6);
        float streakX = exp(-uv.y * uv.y * 16.0) * exp(-uv.x * uv.x * 2.4);
        float streakY = exp(-uv.x * uv.x * 16.0) * exp(-uv.y * uv.y * 2.4);
        alpha = ball * 0.75 + (streakX + streakY) * 0.45;
    } else if (uStyle < 2.5) {
        // 星芒: 光球 + 6 角星芒
        float ball = pow(max(0.0, 1.0 - d), 2.4);
        float ang = atan(uv.y, uv.x);
        float best = 3.14159265;
        for (int i = 0; i < 6; i++) {
            float a = mod(ang - float(i) * 1.04719755 + 3.14159265, 6.28318530) - 3.14159265;
            best = min(best, abs(a));
        }
        float streak = exp(-best * 10.0) * exp(-d * 1.8);
        alpha = ball * 0.7 + streak * 0.6;
    } else {
        // 环形光波: 随低频/时间向外扩散的同心圆环
        float ball = pow(max(0.0, 1.0 - d), 2.6) * 0.5;
        float ringR = fract(d * 5.0 - uTime * 0.9 - uBass * 2.0);
        float ring = exp(-abs(ringR - 0.5) * 6.0) * (1.0 - d) * 0.7;
        alpha = ball + ring;
    }
    alpha *= amp;
    gl_FragColor = vec4(uColor * (0.9 + uBass * 0.8), alpha);
}
"""

// ── 全屏 quad 通用顶点着色器 ──
private const val QUAD_VS = """
attribute vec2 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
"""

// ── 亮度提取 pass: 采样场景纹理，超过 threshold 的保留 ──
private const val BRIGHT_FS = """
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uScene;
uniform float uThreshold;
void main() {
    vec4 c = texture2D(uScene, vTexCoord);
    float l = dot(c.rgb, vec3(0.299, 0.587, 0.114));
    float k = smoothstep(uThreshold, uThreshold + 0.2, l);
    gl_FragColor = vec4(c.rgb * k, 1.0);
}
"""

// ── 高斯模糊 pass (单向, 水平/垂直用 uDirection 区分) ──
private const val BLUR_FS = """
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uScene;
uniform vec2 uDirection;
uniform float uRadius;
void main() {
    vec2 texel = uDirection * uRadius;
    vec4 sum = vec4(0.0);
    sum += texture2D(uScene, vTexCoord - 4.0 * texel) * 0.051;
    sum += texture2D(uScene, vTexCoord - 3.0 * texel) * 0.0918;
    sum += texture2D(uScene, vTexCoord - 2.0 * texel) * 0.12245;
    sum += texture2D(uScene, vTexCoord - 1.0 * texel) * 0.1531;
    sum += texture2D(uScene, vTexCoord) * 0.1633;
    sum += texture2D(uScene, vTexCoord + 1.0 * texel) * 0.1531;
    sum += texture2D(uScene, vTexCoord + 2.0 * texel) * 0.12245;
    sum += texture2D(uScene, vTexCoord + 3.0 * texel) * 0.0918;
    sum += texture2D(uScene, vTexCoord + 4.0 * texel) * 0.051;
    gl_FragColor = sum;
}
"""

// ── 合成 pass: 场景 + bloom * strength ──
private const val COMPOSITE_FS = """
precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uScene;
uniform sampler2D uBloom;
uniform float uBloomStrength;
void main() {
    vec4 scene = texture2D(uScene, vTexCoord);
    vec4 bloom = texture2D(uBloom, vTexCoord);
    gl_FragColor = vec4(scene.rgb + bloom.rgb * uBloomStrength, 1.0);
}
"""

/**
 * TextureView + 自建 EGL 的渲染宿主。
 * 相比 GLSurfaceView(独立窗口 Surface): 内容进入 View 绘制管线,
 * 可被 Compose 的 RenderEffect(Modifier.blur / GlassContainer 的 RuntimeShader) 采样,
 * 从而让液态玻璃/背景玻璃模糊真正对 3D 粒子背景生效。
 */
class ParticleTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    /** 渲染回调(全部在 GL 线程执行, 调用前已 eglMakeCurrent) */
    interface Renderer {
        fun onGlCreated()
        fun onGlChanged(w: Int, h: Int)
        fun onGlDraw()
        fun onGlDestroyed()
    }

    var renderer: Renderer? = null
    @Volatile var fpsLimit = 60
    @Volatile private var rendering = false
    @Volatile private var framePosted = false
    // 当前绑定的 SurfaceTexture: 拆除 EGL 后由本类负责 release
    @Volatile private var surfaceTexture: SurfaceTexture? = null

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private val glThread = HandlerThread("ParticleGL")
    private val glHandler: Handler

    init {
        surfaceTextureListener = this
        glThread.start()
        glHandler = Handler(glThread.looper)
    }

    fun startRendering() {
        rendering = true
        // 单一渲染循环: 重复 startRendering 不会叠加多个 frame 链
        if (!framePosted) {
            framePosted = true
            glHandler.post { frame() }
        }
    }

    fun stopRendering() { rendering = false }

    private fun frame() {
        if (!rendering) { framePosted = false; return }
        if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
            renderer?.onGlDraw()
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }
        val delay = if (fpsLimit <= 0) 5L else (1000L / fpsLimit).coerceAtLeast(5L)
        glHandler.postDelayed({ frame() }, delay)
    }

    private fun initEgl(surface: SurfaceTexture, w: Int, h: Int): Boolean {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val major = IntArray(1); val minor = IntArray(1)
        if (!EGL14.eglInitialize(display, major, 0, minor, 0)) return false
        val attribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, num, 0) || num[0] == 0) return false
        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(display, configs[0]!!, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        surface.setDefaultBufferSize(w, h)
        eglSurface = EGL14.eglCreateWindowSurface(display, configs[0]!!, surface, intArrayOf(EGL14.EGL_NONE), 0)
        eglDisplay = display
        if (eglContext == EGL14.EGL_NO_CONTEXT || eglSurface == EGL14.EGL_NO_SURFACE) return false
        EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
        surfaceTexture = surface
        glHandler.post {
            if (initEgl(surface, w, h)) {
                renderer?.onGlCreated()
                renderer?.onGlChanged(w, h)
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {
        glHandler.post { renderer?.onGlChanged(w, h) }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // 返回 false: 由本类在 GL 线程拆除 EGL 后自行 release SurfaceTexture,
        // 避免框架提前销毁导致 GL 线程访问失效 surface 而闪退
        glHandler.post { releaseSurfaceAndEgl() }
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    /** 拆除 EGL 并释放 SurfaceTexture (必须在 GL 线程执行) */
    private fun releaseSurfaceAndEgl() {
        renderer?.onGlDestroyed()
        releaseEgl()
        surfaceTexture?.let { s -> try { s.release() } catch (_: Exception) {} }
        surfaceTexture = null
    }

    private fun releaseEgl() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
        }
    }

    fun shutdown() {
        rendering = false
        framePosted = false
        glHandler.removeCallbacksAndMessages(null)
        glHandler.post { releaseSurfaceAndEgl() }
        glThread.quitSafely()
    }

    /** 在 GL 线程延迟执行任务 (走渲染线程 Handler, 不经过 UI 线程) */
    fun postOnGlThread(delayMs: Long, task: () -> Unit) {
        glHandler.postDelayed({ task() }, delayMs)
    }
}

class Particle3DRenderer(context: Context) : SensorEventListener, ParticleTextureView.Renderer {

    val textureView: ParticleTextureView = ParticleTextureView(context).apply {
        renderer = this@Particle3DRenderer
    }

    private val density = context.resources.displayMetrics.density.coerceAtMost(2f)
    private val appContext = context.applicationContext

    // 粒子 program
    private var program = 0
    private var aPos = 0; private var aRand = 0; private var aUv = 0; private var aCover = 0; private var aScatter = 0
    private var uMVP = 0; private var uModel = 0; private var uTheme = 0
    private var uTime = 0; private var uBass = 0; private var uMid = 0; private var uTreble = 0; private var uPixel = 0
    private var uSpeed = 0; private var uPulse = 0; private var uGlowStrength = 0
    private var uGlowPulse = 0; private var uGlowDark = 0; private var uGlowBright = 0; private var uPointScale = 0; private var uHasCover = 0
    private var uGlowPass = 0; private var uGlowAlpha = 0; private var uGlowSize = 0
    private var uScatterRadius = 0

    // 中心光晕 billboard program
    private var haloProgram = 0
    private var haloAPos = 0
    private var haloUMVP = 0; private var haloUColor = 0; private var haloUBass = 0; private var haloUHasCover = 0
    private var haloUTime = 0; private var haloUStyle = 0

    // bloom programs
    private var brightProgram = 0
    private var blurProgram = 0
    private var compositeProgram = 0
    private var quadPos = 0; private var quadUv = 0
    private var quadBuffer: FloatBuffer

    // FBO 句柄
    private var sceneFbo = 0; private var sceneTex = 0
    private var brightFbo = 0; private var brightTex = 0
    private var blurAFbo = 0; private var blurATex = 0
    private var blurBFbo = 0; private var blurBTex = 0
    private var fboW = 0; private var fboH = 0
    private var halfW = 0; private var halfH = 0

    // 动态粒子网格 (边长粒子数量, 数量可在设置中调整)
    private var gridX = DEFAULT_EDGE
    private var gridY = DEFAULT_EDGE
    private var particleCount = DEFAULT_EDGE * DEFAULT_EDGE
    @Volatile private var pendingParticleEdge = -1

    private var vertexBuffer: FloatBuffer
    private var randArray = FloatArray(DEFAULT_EDGE * DEFAULT_EDGE)

    // 无专辑散开坐标(归一化半径, 实际由 uScatterRadius 缩放): 当前值 + 形态切换过渡的目标值
    private var scatterPos = FloatArray(DEFAULT_EDGE * DEFAULT_EDGE * 3)
    private var targetScatter = FloatArray(DEFAULT_EDGE * DEFAULT_EDGE * 3)
    @Volatile private var pendingScatterMode = -1
    @Volatile private var scatterAnimating = false
    private var scatterMode = Filament3DConfig.DEFAULT_SCATTER_MODE

    @Volatile private var enabled = false
    @Volatile private var foreground = true
    // 帧率限制挡位: 30/60/90/120/144/165/无限制(0)
    @Volatile private var fpsLimit = 60

    // uHasCover 平滑过渡 (easeInOut, 对齐 web transDuration)
    private var hasCoverCur = 0f
    private var hasCoverTarget = 0f

    // 封面 bitmap (待 onSurfaceCreated 后采样)
    @Volatile private var pendingCover: Bitmap? = null
    private var coverBitmap: Bitmap? = null
    // 封面主色 (对齐 web extractColors K-means, 用于背景色跟随/主题色)
    @Volatile private var hasCoverColor = false
    @Volatile private var coverDominant = floatArrayOf(0.7f, 0.6f, 0.5f)
    // 平滑显示中的背景/主题色: 切换歌曲时封面主色突变 → 逐帧 lerp 过渡
    private val displayDominant = floatArrayOf(0.7f, 0.6f, 0.5f)

    private var smoothBass = 0f
    private var smoothMid = 0f
    private var smoothTreble = 0f

    // 时间基准: 相对渲染器创建时刻的秒数 (对齐 web performance.now()/1000 语义;
    // 若直接用开机纳秒转 Float, 数值巨大导致 sin(t) 时间律动精度劣化, 粒子"不活")
    private val startNano = System.nanoTime()

    // ── 陀螺仪驱动粒子排布微偏移旋转 ──
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    @Volatile private var gyroEnabled = true
    // 初始姿态校准 (以启动时刻为原点, 避免竖屏 pitch≈90° 恒歪)
    @Volatile private var initCalibrated = false
    private var initYaw = 0f
    private var initPitch = 0f
    // 传感器相对角度 (弧度, 已限幅)
    private var sensorYaw = 0f
    private var sensorPitch = 0f
    // 平滑跟随 + 自动回正后的角度
    private var smoothYaw = 0f
    private var smoothPitch = 0f
    // 陀螺仪增量累计 (delta 累计 + 持续回中 + 限幅防镜像)
    private var lastAppliedYaw = 0f
    private var lastAppliedPitch = 0f
    private var accYaw = 0f
    private var accPitch = 0f
    // 限幅: ±115° = 2.007 rad (低于180° 防止旋转到镜像)
    private val maxYaw = Math.toRadians(115.0).toFloat()
    private val maxPitch = Math.toRadians(30.0).toFloat()

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val tmpRot = FloatArray(16)
    private val mvp = FloatArray(16)

    init {
        val rnd = java.util.Random(20260805L)
        for (i in 0 until particleCount) randArray[i] = rnd.nextFloat()
        // 初始化散开坐标(当前形态)
        generateScatter(scatterMode, scatterPos)
        targetScatter = scatterPos.copyOf()
        vertexBuffer = ByteBuffer.allocateDirect(particleCount * STRIDE)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        buildStatic()
        // 全屏 quad: position(2) + uv(2) = 4 float
        // uv 必须覆盖整个纹理: 左下(0,0) 右下(1,0) 左上(0,1) 右上(1,1)
        val quad = floatArrayOf(
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        )
        quadBuffer = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quad)
        quadBuffer.position(0)
        // 初始 model 为单位矩阵
        Matrix.setIdentityM(model, 0)
    }

    // 对齐 web buildParticleGeometry: 位置 = (uv - 0.5) * PLANE_SIZE
    private fun buildStatic() {
        val texelStep = 1f / gridX
        vertexBuffer.clear()
        for (i in 0 until particleCount) {
            val gx = i % gridX; val gy = i / gridX
            val u = (gx + 0.5f) * texelStep
            val v = (gy + 0.5f) * texelStep
            vertexBuffer.put((u - 0.5f) * PLANE_SIZE)  // pos.x
            vertexBuffer.put((v - 0.5f) * PLANE_SIZE)  // pos.y
            vertexBuffer.put(0f)                        // pos.z
            vertexBuffer.put(randArray[i])              // aRand
            vertexBuffer.put(u); vertexBuffer.put(v)    // aUv
            vertexBuffer.put(0f); vertexBuffer.put(0f); vertexBuffer.put(0f)  // aCoverColor
            vertexBuffer.put(scatterPos[i * 3]); vertexBuffer.put(scatterPos[i * 3 + 1]); vertexBuffer.put(scatterPos[i * 3 + 2])  // aScatterPos
        }
        vertexBuffer.position(0)
    }

    /** 重建粒子网格: n 为边长粒子数量, 排布为 n×n (对齐 web grid=round(sqrt(count)) 语义, 直接按边长) */
    private fun rebuildGrid(n: Int) {
        val e = n.coerceIn(MIN_EDGE, MAX_EDGE)
        gridX = e; gridY = e
        particleCount = e * e
        randArray = FloatArray(particleCount)
        val rnd = java.util.Random(System.currentTimeMillis())
        for (i in 0 until particleCount) randArray[i] = rnd.nextFloat()
        // 重新生成散开坐标(当前形态, 固定种子保证稳定)
        scatterPos = FloatArray(particleCount * 3)
        targetScatter = FloatArray(particleCount * 3)
        generateScatter(scatterMode, scatterPos)
        scatterAnimating = false
        vertexBuffer = ByteBuffer.allocateDirect(particleCount * STRIDE)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        buildStatic()
        // 若有封面则对新网格重新采样封面色
        coverBitmap?.let { sampleCover(it) }
    }

    /**
     * 生成 4 种无专辑散开形态的坐标(归一化半径≈1, 实际由 uScatterRadius 缩放)。
     * 固定种子 + 顺序生成: 每次生成结果稳定, 形态切换时用逐帧 lerp 平滑过渡。
     */
    private fun generateScatter(mode: Int, out: FloatArray) {
        val rnd = java.util.Random(20260807L + mode * 31L)
        val pi2 = 2f * kotlin.math.PI.toFloat()
        for (i in 0 until particleCount) {
            val x: Float; val y: Float; val z: Float
            when (mode) {
                Filament3DConfig.SCATTER_MODE_SHELL -> {
                    // 球面薄壳: 均匀球面方向 + 少量径向抖动
                    val u = rnd.nextFloat() * 2f - 1f
                    val theta = rnd.nextFloat() * pi2
                    val sq = kotlin.math.sqrt(1f - u * u)
                    val r = 0.88f + rnd.nextFloat() * 0.16f
                    x = r * sq * kotlin.math.cos(theta); y = r * u; z = r * sq * kotlin.math.sin(theta)
                }
                Filament3DConfig.SCATTER_MODE_RING -> {
                    // 扁平星环: 圆环内面积均匀分布, 上下薄
                    val theta = rnd.nextFloat() * pi2
                    val rr = kotlin.math.sqrt(rnd.nextFloat()) * 0.75f + 0.25f
                    x = rr * kotlin.math.cos(theta); z = rr * kotlin.math.sin(theta)
                    y = (rnd.nextFloat() - 0.5f) * 0.22f
                }
                Filament3DConfig.SCATTER_MODE_WAVE -> {
                    // 上下声波罩: 上下两片抛物面罩住 bloom
                    val theta = rnd.nextFloat() * pi2
                    val rr = kotlin.math.sqrt(rnd.nextFloat())
                    val side = if (rnd.nextBoolean()) 1f else -1f
                    x = rr * kotlin.math.cos(theta); y = side * (0.5f + rr * rr * 0.5f); z = rr * kotlin.math.sin(theta)
                }
                else -> {
                    // 球形/椭圆云团: 球体体积均匀分布, 纵向略扁更自然
                    val u = rnd.nextFloat() * 2f - 1f
                    val theta = rnd.nextFloat() * pi2
                    val sq = kotlin.math.sqrt(1f - u * u)
                    val r = Math.cbrt(rnd.nextFloat().toDouble()).toFloat()
                    x = r * sq * kotlin.math.cos(theta); y = r * u * 0.82f; z = r * sq * kotlin.math.sin(theta)
                }
            }
            out[i * 3] = x; out[i * 3 + 1] = y; out[i * 3 + 2] = z
        }
    }

    fun setEnabled(v: Boolean) { enabled = v; updateLoop(); updateSensor() }
    fun setPlaying(p: Boolean) { updateLoop() }
    fun setForeground(f: Boolean) { foreground = f; updateLoop(); updateSensor() }
    fun setGyroEnabled(v: Boolean) { gyroEnabled = v; updateSensor(); if (!v) { sensorYaw = 0f; sensorPitch = 0f; smoothYaw = 0f; smoothPitch = 0f; accYaw = 0f; accPitch = 0f } }
    fun setParticleEdge(edge: Int) { if (edge > 0) pendingParticleEdge = edge }
    fun setFpsLimit(fps: Int) { fpsLimit = fps; textureView.fpsLimit = fps }

    // ── 3D 渲染内存锁: 进程 PSS 采样 (binder 调用, 仅在调整粒子配置时低频使用) ──
    private fun processPssKb(): Int = try {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val arr = am.getProcessMemoryInfo(intArrayOf(Process.myPid()))
        if (arr.isNotEmpty()) arr[0].totalPss else 0
    } catch (_: Exception) { 0 }

    private fun heapRatio(): Float {
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()).toFloat() / rt.maxMemory().toFloat()
    }

    /** 粒子重建后延迟校验内存: 超限即回滚配置并上报内存锁日志 */
    private fun scheduleGlMemoryCheck(oldEdge: Int, newEdge: Int, baselinePssKb: Int) {
        textureView.postOnGlThread(GL_MEMORY_CHECK_DELAY_MS) {
            // 期间已再次调整或网格已替换 → 跳过(新一次校验自行负责)
            if (pendingParticleEdge > 0 || gridX != newEdge) return@postOnGlThread
            val heap = heapRatio()
            val pssAfterKb = processPssKb()
            val deltaKb = if (baselinePssKb > 0 && pssAfterKb > 0) pssAfterKb - baselinePssKb else 0
            val anomalous = heap > GL_HEAP_CRITICAL_RATIO ||
                deltaKb > GL_PSS_DELTA_KB || pssAfterKb > GL_PSS_ABSOLUTE_KB
            if (anomalous) {
                // 回滚: flow+prefs 同步更新, UI 滑块与渲染器自动回到旧值
                Filament3DConfig.setParticleEdge(oldEdge)
                MemoryLockService.reportAnomaly(
                    AnomalyType.RENDER_GL_MEMORY_ANOMALY,
                    "3D粒子配置调整导致内存异常, 已自动回滚: ${newEdge}×${newEdge} → ${oldEdge}×${oldEdge}",
                    "堆占比=${String.format("%.1f", heap * 100)}%, " +
                        "PSS ${baselinePssKb / 1024}MB → ${pssAfterKb / 1024}MB (Δ${deltaKb / 1024}MB)"
                )
            }
        }
    }

    /** 散开形态切换: 标记目标形态, 由 GL 线程生成新坐标并逐帧 lerp 平滑过渡 */
    fun setScatterMode(mode: Int) { if (mode in 0..3) pendingScatterMode = mode }

    /** 专辑封面模式: bitmap 非空时粒子拼成封面, null 时恢复主题色点阵 */
    fun setCoverBitmap(bmp: Bitmap?) {
        pendingCover = bmp
        hasCoverTarget = if (bmp != null) 1f else 0f
        if (bmp == null) hasCoverColor = false
    }

    fun destroy() {
        updateSensor()
        textureView.shutdown()
    }

    // 3D 背景开启后无论是否播放音乐都渲染 (对齐 html: 无音频时粒子缓慢律动)
    private fun shouldRender() = enabled && foreground

    private fun updateLoop() {
        if (shouldRender()) textureView.startRendering() else textureView.stopRendering()
    }

    // ── 陀螺仪注册/注销 (仅在前台+3D启用+开关开启时注册, 省电) ──
    private fun updateSensor() {
        val need = enabled && foreground && gyroEnabled
        if (need && rotationSensor != null) {
            try {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
            } catch (_: Exception) {}
        } else {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR) return
        // 旋转向量 → 欧拉角
        val r = FloatArray(9); val o = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(r, e.values)
        SensorManager.getOrientation(r, o)
        // o[0]=azimuth(绕Z), o[1]=pitch(绕X), o[2]=roll(绕Y)
        // 首次数据校准为原点: 设备姿态变化相对启动时刻, 竖屏手持不会恒歪
        if (!initCalibrated) {
            initYaw = o[0]; initPitch = o[1]; initCalibrated = true
        }
        // 相对初始姿态 + 限幅 (o[0]→粒子绕Y纵轴, o[1]→粒子绕X), 幅度可调
        val gyroAmount = Filament3DConfig.getGyroAmount()
        sensorYaw = (o[0] - initYaw).coerceIn(-maxYaw * gyroAmount, maxYaw * gyroAmount)
        sensorPitch = (o[1] - initPitch).coerceIn(-maxPitch * gyroAmount, maxPitch * gyroAmount)
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    override fun onGlCreated() {
        program = makeProgram(VS_SRC, FS_SRC)
        if (program != 0) {
            aPos = GLES20.glGetAttribLocation(program, "aPosition")
            aRand = GLES20.glGetAttribLocation(program, "aRand")
            aUv = GLES20.glGetAttribLocation(program, "aUv")
            aCover = GLES20.glGetAttribLocation(program, "aCoverColor")
            aScatter = GLES20.glGetAttribLocation(program, "aScatterPos")
            uMVP = GLES20.glGetUniformLocation(program, "uMVP")
            uModel = GLES20.glGetUniformLocation(program, "uModel")
            uTheme = GLES20.glGetUniformLocation(program, "uThemeColor")
            uTime = GLES20.glGetUniformLocation(program, "uTime")
            uBass = GLES20.glGetUniformLocation(program, "uBass")
            uMid = GLES20.glGetUniformLocation(program, "uMid")
            uTreble = GLES20.glGetUniformLocation(program, "uTreble")
            uPixel = GLES20.glGetUniformLocation(program, "uPixelRatio")
            uSpeed = GLES20.glGetUniformLocation(program, "uSpeed")
            uPulse = GLES20.glGetUniformLocation(program, "uPulse")
            uGlowStrength = GLES20.glGetUniformLocation(program, "uGlowStrength")
            uGlowPulse = GLES20.glGetUniformLocation(program, "uGlowPulse")
            uGlowDark = GLES20.glGetUniformLocation(program, "uGlowDark")
            uGlowBright = GLES20.glGetUniformLocation(program, "uGlowBright")
            uPointScale = GLES20.glGetUniformLocation(program, "uPointScale")
            uHasCover = GLES20.glGetUniformLocation(program, "uHasCover")
            uGlowPass = GLES20.glGetUniformLocation(program, "uGlowPass")
            uGlowAlpha = GLES20.glGetUniformLocation(program, "uGlowAlpha")
            uGlowSize = GLES20.glGetUniformLocation(program, "uGlowSize")
            uScatterRadius = GLES20.glGetUniformLocation(program, "uScatterRadius")
        }
        haloProgram = makeProgram(HALO_VS, HALO_FS)
        if (haloProgram != 0) {
            haloAPos = GLES20.glGetAttribLocation(haloProgram, "aPosition")
            haloUMVP = GLES20.glGetUniformLocation(haloProgram, "uMVP")
            haloUColor = GLES20.glGetUniformLocation(haloProgram, "uColor")
            haloUBass = GLES20.glGetUniformLocation(haloProgram, "uBass")
            haloUHasCover = GLES20.glGetUniformLocation(haloProgram, "uHasCover")
            haloUTime = GLES20.glGetUniformLocation(haloProgram, "uTime")
            haloUStyle = GLES20.glGetUniformLocation(haloProgram, "uStyle")
        }
        brightProgram = makeProgram(QUAD_VS, BRIGHT_FS)
        blurProgram = makeProgram(QUAD_VS, BLUR_FS)
        compositeProgram = makeProgram(QUAD_VS, COMPOSITE_FS)
        quadPos = GLES20.glGetAttribLocation(compositeProgram, "aPosition")
        quadUv = GLES20.glGetAttribLocation(compositeProgram, "aTexCoord")
        // 背景色对齐 web: 0x0a0a12
        GLES20.glClearColor(10f / 255f, 10f / 255f, 18f / 255f, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        // 若有 pending cover, 立即采样
        pendingCover?.let { sampleCover(it) }
        pendingCover = null
    }

    override fun onGlChanged(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        GLES20.glViewport(0, 0, w, h)
        Matrix.perspectiveM(proj, 0, FOV_Y_DEG, w.toFloat() / h.toFloat(), 0.1f, 200f)
        Matrix.setLookAtM(view, 0, 0f, 0f, CAM_DIST, 0f, 0f, 0f, 0f, 1f, 0f)
        setupFbo(w, h)
    }

    override fun onGlDestroyed() {
        // 释放 GL 资源(SurfaceTexture 销毁时, 避免 context 重建后泄漏)
        try {
            deleteFbo()
            intArrayOf(program, haloProgram, brightProgram, blurProgram, compositeProgram)
                .filter { it != 0 }.forEach { GLES20.glDeleteProgram(it) }
        } catch (_: Exception) {}
        program = 0; haloProgram = 0; brightProgram = 0; blurProgram = 0; compositeProgram = 0
        fboW = 0; fboH = 0
    }

    /** 创建/重建 FBO (全分辨率场景 + 半分辨率亮部/模糊) */
    private fun setupFbo(w: Int, h: Int) {
        if (fboW == w && fboH == h && sceneFbo != 0) return
        deleteFbo()
        fboW = w; fboH = h
        halfW = (w / 2).coerceAtLeast(1); halfH = (h / 2).coerceAtLeast(1)
        val (sf, sb) = makeFbo(w, h); sceneTex = sf; sceneFbo = sb
        val (bf, bb) = makeFbo(halfW, halfH); brightTex = bf; brightFbo = bb
        val (af, ab) = makeFbo(halfW, halfH); blurATex = af; blurAFbo = ab
        val (xf, xb) = makeFbo(halfW, halfH); blurBTex = xf; blurBFbo = xb
    }

    private fun makeFbo(w: Int, h: Int): Pair<Int, Int> {
        val tex = IntArray(1); GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        val fbo = IntArray(1); GLES20.glGenFramebuffers(1, fbo, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, tex[0], 0)
        val st = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        if (st != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "FBO incomplete: $st ${w}x${h}, 回退直接渲染")
            GLES20.glDeleteTextures(1, tex, 0)
            GLES20.glDeleteFramebuffers(1, fbo, 0)
            return 0 to 0
        }
        return tex[0] to fbo[0]
    }

    private fun deleteFbo() {
        val t = intArrayOf(sceneTex, brightTex, blurATex, blurBTex).filter { it != 0 }
        val f = intArrayOf(sceneFbo, brightFbo, blurAFbo, blurBFbo).filter { it != 0 }
        if (t.isNotEmpty()) GLES20.glDeleteTextures(t.size, t.toIntArray(), 0)
        if (f.isNotEmpty()) GLES20.glDeleteFramebuffers(f.size, f.toIntArray(), 0)
        sceneTex = 0; sceneFbo = 0; brightTex = 0; brightFbo = 0
        blurATex = 0; blurAFbo = 0; blurBTex = 0; blurBFbo = 0
    }

    override fun onGlDraw() {
        if (!shouldRender() || program == 0) return

        // 粒子数量变更(边长) → 在 GL 线程重建网格 (内存锁保护: OOM/内存超限自动回滚)
        val pending = pendingParticleEdge
        if (pending > 0 && pending != gridX) {
            pendingParticleEdge = -1
            val oldEdge = gridX
            val baselinePssKb = processPssKb()
            val rebuilt = try {
                rebuildGrid(pending)
                true
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "粒子重建 OOM edge $oldEdge→$pending, 回滚", e)
                try { rebuildGrid(oldEdge) } catch (e2: OutOfMemoryError) {
                    // 极端内存枯竭: 停止渲染保活
                    enabled = false; updateLoop()
                }
                Filament3DConfig.setParticleEdge(oldEdge)
                MemoryLockService.reportAnomaly(
                    AnomalyType.RENDER_GL_MEMORY_ANOMALY,
                    "3D粒子重建内存不足(OOM), 已回滚: ${pending}×${pending} → ${oldEdge}×${oldEdge}",
                    "堆占比=${String.format("%.1f", heapRatio() * 100)}%, PSS 基线=${baselinePssKb / 1024}MB"
                )
                false
            }
            if (rebuilt) scheduleGlMemoryCheck(oldEdge, pending, baselinePssKb)
        }

        // 散开形态切换: 生成新目标坐标, 逐帧 lerp 平滑过渡(仅过渡期更新 CPU 顶点)
        val pm = pendingScatterMode
        if (pm >= 0 && pm != scatterMode) {
            pendingScatterMode = -1
            scatterMode = pm
            generateScatter(scatterMode, targetScatter)
            scatterAnimating = true
        }
        if (scatterAnimating) {
            var maxDiff = 0f
            val strideF = STRIDE / 4
            for (i in 0 until particleCount) {
                val b = i * 3
                var d = 0f
                for (k in 0 until 3) {
                    val cur = scatterPos[b + k]
                    val tgt = targetScatter[b + k]
                    val nv = cur + (tgt - cur) * 0.12f
                    scatterPos[b + k] = nv
                    val diff = if (tgt > nv) tgt - nv else nv - tgt
                    if (diff > d) d = diff
                }
                if (d > maxDiff) maxDiff = d
                val base = i * strideF + OFF_SCATTER
                vertexBuffer.put(base, scatterPos[b])
                vertexBuffer.put(base + 1, scatterPos[b + 1])
                vertexBuffer.put(base + 2, scatterPos[b + 2])
            }
            if (maxDiff < 0.01f) scatterAnimating = false
        }

        // 音频数据 (对齐 web updateFrequencyBands: 无音频时用基础律动)
        val data = aVzM.musicFftData.value
        val cfg = Filament3DConfig
        val t = (System.nanoTime() - startNano) / 1_000_000_000f
        val isAudio = com.bicy.whitenoise.music.MusicPlayerController.isPlaying
        // bass: toWebByte(-100..-30dB) 对 bass 过饱和(间隙 0.77 鼓点 1.0), 动态被压缩后
        // 平滑值近乎恒定 → 呼吸只剩周期正弦, 显得"太规律"。
        // 改用更窄 dB 窗(-60..-15dB → 0..1)保留鼓点间隙回落, 配合 attack/release 包络:
        // 上升快(k=0.7 跟鼓点瞬态)衰减慢(k=0.18 留余韵) → 节拍脉冲而非平滑正弦呼吸。
        val bass = if (isAudio) toBeatDb(band(data, 0, 2)) * cfg.getBassSens()
                   else 0.1f + sin(t) * 0.05f
        // 中/高频沿用 web 的 getByteFrequencyData dB 刻度(-100..-30dB → 0..1)
        val mid = if (isAudio) toWebByte(band(data, 2, 8)) * cfg.getMidSens() else 0.05f
        val treble = if (isAudio) toWebByte(band(data, 8, 16)) * cfg.getTrebleSens() else 0.03f
        // 非对称包络: 上升快(attack)下降慢(release), 让鼓点形成清晰脉冲
        smoothBass += (bass - smoothBass) * (if (bass > smoothBass) 0.7f else 0.18f)
        smoothMid += (mid - smoothMid) * 0.3f
        smoothTreble += (treble - smoothTreble) * 0.3f

        // uHasCover easeInOut 缓动过渡 (对齐 web transDuration, 从 config 读取)
        if (hasCoverCur != hasCoverTarget) {
            val dur = cfg.getTransDuration().coerceAtLeast(0.1f)
            val step = (1f / 30f) / dur  // 30fps 近似
            hasCoverCur = if (hasCoverCur < hasCoverTarget)
                (hasCoverCur + step).coerceAtMost(hasCoverTarget)
            else (hasCoverCur - step).coerceAtLeast(hasCoverTarget)
        }
        val eased = easeInOut(hasCoverCur)

        // 处理 pending cover (新封面到达时采样到 aCoverColor)
        pendingCover?.let { sampleCover(it); pendingCover = null }

        // 陀螺仪: 增量累计(跟随摆动) + 持续回中(自动回正) + 限幅(防镜像)
        // 灵敏度/回正速度/平滑度均可调 (对齐 config 新增的陀螺仪参数)
        val gSens = cfg.getGyroSensitivity()
        val gReturn = cfg.getGyroReturn()
        val gSmooth = cfg.getGyroSmoothing()
        val dYaw = sensorYaw - lastAppliedYaw
        lastAppliedYaw = sensorYaw
        if (kotlin.math.abs(dYaw) > 0.004f) accYaw += dYaw * gSens
        accYaw *= gReturn
        accYaw = accYaw.coerceIn(-maxYaw, maxYaw)
        val dPitch = sensorPitch - lastAppliedPitch
        lastAppliedPitch = sensorPitch
        if (kotlin.math.abs(dPitch) > 0.004f) accPitch += dPitch * gSens
        accPitch *= gReturn
        accPitch = accPitch.coerceIn(-maxPitch, maxPitch)
        // 平滑应用
        smoothYaw += (accYaw - smoothYaw) * gSmooth
        smoothPitch += (accPitch - smoothPitch) * gSmooth

        // model matrix: 绕 Y 轴(纵轴) yaw + 绕 X 轴 pitch (粒子排布微偏移旋转)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, Math.toDegrees(smoothYaw.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(model, 0, Math.toDegrees(smoothPitch.toDouble()).toFloat(), 1f, 0f, 0f)

        // 主题色: 有封面时用封面主色(对齐 web applyThemeColor 更新 themeColor), 否则用 App 主题色
        val tc = ThemeColorManager.getCurrentThemeColor().primary
        // 平滑过渡: 封面主色/主题色切换时逐帧 lerp(约 2s 完成), 避免切歌背景色突变
        val tgtC = if (hasCoverColor) coverDominant
                   else floatArrayOf(tc.red, tc.green, tc.blue)
        for (i in 0..2) displayDominant[i] += (tgtC[i] - displayDominant[i]) * 0.045f
        val effTc = androidx.compose.ui.graphics.Color(displayDominant[0], displayDominant[1], displayDominant[2])
        Matrix.multiplyMM(mvp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, mvp, 0)

        // 对齐 web L760: bloomPass.strength = params.bloomStrength * (1 - eased)
        // 但 web 在有封面时 strength 归零导致 Android 上"怎么调都没变化",
        // 故保留最低 30% 让参数始终可感知
        val bloomStrength = cfg.getBloomStrength() * (1f - eased * 0.7f)

        // 背景色: 跟随封面(用封面主色*亮度, 亮度可调, 对齐 web color*0.05/0.08) 或 自定义背景色
        val bgBright = cfg.getBgBrightness()
        val (bgR, bgG, bgB) = if (cfg.isBgFollowCover()) {
            // 用平滑中的 displayDominant, 切歌时背景色渐变而非突变
            Triple(displayDominant[0] * bgBright, displayDominant[1] * bgBright, displayDominant[2] * bgBright * 1.6f)
        } else {
            val c = cfg.getCustomBg()
            Triple(((c shr 16) and 0xFF) / 255f, ((c shr 8) and 0xFF) / 255f, (c and 0xFF) / 255f)
        }
        GLES20.glClearColor(bgR, bgG, bgB, 1f)

        // ===== FBO bloom 流程: 若 FBO 不可用则回退到直接渲染 =====
        val fboReady = sceneFbo != 0 && brightFbo != 0 && blurAFbo != 0 && blurBFbo != 0
        if (fboReady) {
            // 1. 渲染场景到 sceneFbo
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, sceneFbo)
            GLES20.glViewport(0, 0, fboW, fboH)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            drawScene(effTc, t, eased, cfg)
            // 2. 亮度提取 → brightFbo (半分辨率)
            drawQuadPass(brightProgram, brightFbo, halfW, halfH, sceneTex, listOf(
                "uScene" to 0, "uThreshold" to cfg.getBloomThreshold()
            ))
            // 3. 水平模糊 → blurAFbo (模糊半径: 放大系数 3→8, 半分辨率下 0.6→4.8texel 才可见)
            drawQuadPass(blurProgram, blurAFbo, halfW, halfH, brightTex, listOf(
                "uScene" to 0, "uDirection" to floatArrayOf(1f, 0f), "uRadius" to cfg.getBloomRadius() * 8f
            ))
            // 4. 垂直模糊 → blurBFbo
            drawQuadPass(blurProgram, blurBFbo, halfW, halfH, blurATex, listOf(
                "uScene" to 0, "uDirection" to floatArrayOf(0f, 1f), "uRadius" to cfg.getBloomRadius() * 8f
            ))
            // 5. 合成到默认 framebuffer
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glViewport(0, 0, fboW, fboH)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            drawComposite(sceneTex, blurBTex, bloomStrength)
        } else {
            // 回退: 直接渲染到默认 framebuffer (additive 混合近似 bloom)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glViewport(0, 0, fboW, fboH)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            drawScene(effTc, t, eased, cfg)
        }
    }

    /** 渲染粒子 + 中心光晕到当前绑定的 framebuffer */
    private fun drawScene(tc: androidx.compose.ui.graphics.Color, t: Float, eased: Float, cfg: Filament3DConfig) {
        // 中心光晕: billboard 光幕, 先于粒子绘制 → 光晕位于粒子后方(背景锚点)。
        // 缩放随低频呼吸(无封面时更明显), 样式由 cfg.getBloomStyle() 切换。
        if (haloProgram != 0) {
            val style = cfg.getBloomStyle()
            val styleScale = haloStyleScale(style)
            val haloScale = styleScale * (1.35f + smoothBass * 1.25f * (1f - eased))
            Matrix.setIdentityM(tmpRot, 0)
            Matrix.scaleM(tmpRot, 0, haloScale, haloScale, haloScale)
            val haloMvp = FloatArray(16)
            Matrix.multiplyMM(haloMvp, 0, view, 0, tmpRot, 0)
            Matrix.multiplyMM(haloMvp, 0, proj, 0, haloMvp, 0)
            GLES20.glUseProgram(haloProgram)
            GLES20.glUniformMatrix4fv(haloUMVP, 1, false, haloMvp, 0)
            GLES20.glUniform3f(haloUColor, tc.red, tc.green, tc.blue)
            GLES20.glUniform1f(haloUBass, smoothBass)
            GLES20.glUniform1f(haloUHasCover, eased)
            GLES20.glUniform1f(haloUTime, t)
            GLES20.glUniform1f(haloUStyle, style.toFloat())
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)  // additive
            quadBuffer.position(0)
            GLES20.glEnableVertexAttribArray(haloAPos)
            GLES20.glVertexAttribPointer(haloAPos, 2, GLES20.GL_FLOAT, false, 16, quadBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(haloAPos)
        }

        // 粒子点阵: 两趟渲染 (光晕层 additive 大尺寸 + 核心层 NormalBlending 保留封面原色)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniform3f(uTheme, tc.red, tc.green, tc.blue)
        GLES20.glUniform1f(uTime, t)
        GLES20.glUniform1f(uBass, smoothBass)
        GLES20.glUniform1f(uMid, smoothMid)
        GLES20.glUniform1f(uTreble, smoothTreble)
        GLES20.glUniform1f(uPixel, density)
        GLES20.glUniform1f(uSpeed, cfg.getMoveSpeed())
        GLES20.glUniform1f(uPulse, cfg.getPulseAmt())
        GLES20.glUniform1f(uGlowStrength, cfg.getGlowStrength())
        GLES20.glUniform1f(uGlowPulse, if (cfg.isGlowPulse()) 1f else 0f)
        GLES20.glUniform1f(uGlowDark, cfg.getGlowDark())
        GLES20.glUniform1f(uGlowBright, cfg.getGlowBright())
        GLES20.glUniform1f(uPointScale, cfg.getParticleSize())
        GLES20.glUniform1f(uHasCover, eased)
        GLES20.glUniform1f(uScatterRadius, cfg.getScatterRadius())

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, STRIDE, vertexBuffer)
        vertexBuffer.position(OFF_RAND)
        GLES20.glEnableVertexAttribArray(aRand)
        GLES20.glVertexAttribPointer(aRand, 1, GLES20.GL_FLOAT, false, STRIDE, vertexBuffer)
        vertexBuffer.position(OFF_UV)
        GLES20.glEnableVertexAttribArray(aUv)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, STRIDE, vertexBuffer)
        vertexBuffer.position(OFF_COVER)
        GLES20.glEnableVertexAttribArray(aCover)
        GLES20.glVertexAttribPointer(aCover, 3, GLES20.GL_FLOAT, false, STRIDE, vertexBuffer)
        vertexBuffer.position(OFF_SCATTER)
        GLES20.glEnableVertexAttribArray(aScatter)
        GLES20.glVertexAttribPointer(aScatter, 3, GLES20.GL_FLOAT, false, STRIDE, vertexBuffer)

        // 光晕层: 档位>0 时先用加法混合画大尺寸柔和光晕 (封面渐显时随 eased 淡出, 避免糊掉封面)
        val glowTier = cfg.getGlowTier()
        if (glowTier > Filament3DConfig.GLOW_TIER_OFF) {
            val (gSize, gAlpha) = glowTierParams(glowTier)
            GLES20.glUniform1f(uGlowPass, 0f)
            GLES20.glUniform1f(uGlowSize, gSize)
            GLES20.glUniform1f(uGlowAlpha, gAlpha * (1f - eased))
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particleCount)
        }
        // 核心层: 正常混合, 硬边圆保留粒子原色
        GLES20.glUniform1f(uGlowPass, 1f)
        GLES20.glUniform1f(uGlowSize, 1f)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particleCount)
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aRand)
        GLES20.glDisableVertexAttribArray(aUv)
        GLES20.glDisableVertexAttribArray(aCover)
        GLES20.glDisableVertexAttribArray(aScatter)
    }

    /** 光晕样式基础缩放(十字/星芒/环形需要更大光幕) */
    private fun haloStyleScale(style: Int): Float = when (style) {
        Filament3DConfig.BLOOM_STYLE_CROSS -> 1.35f
        Filament3DConfig.BLOOM_STYLE_STAR -> 1.25f
        Filament3DConfig.BLOOM_STYLE_RING -> 1.5f
        else -> 1.0f
    }

    /** 粒子光晕档位 → (扩散倍数, 光晕透明度) */
    private fun glowTierParams(tier: Int): Pair<Float, Float> = when (tier) {
        Filament3DConfig.GLOW_TIER_SOFT -> 2.2f to 0.32f
        Filament3DConfig.GLOW_TIER_NEON -> 4.6f to 0.68f
        else -> 3.2f to 0.5f
    }

    /** 全屏 quad pass (亮度提取 / 模糊) */
    private fun drawQuadPass(prog: Int, fbo: Int, w: Int, h: Int, srcTex: Int, uniforms: List<Pair<String, Any>>) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glViewport(0, 0, w, h)
        GLES20.glUseProgram(prog)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, srcTex)
        val uScene = GLES20.glGetUniformLocation(prog, "uScene")
        GLES20.glUniform1i(uScene, 0)
        uniforms.forEach { (name, v) ->
            val loc = GLES20.glGetUniformLocation(prog, name)
            when (v) {
                is Float -> GLES20.glUniform1f(loc, v)
                is Int -> GLES20.glUniform1i(loc, v)
                is FloatArray -> if (v.size == 2) GLES20.glUniform2fv(loc, 1, v, 0)
            }
        }
        drawQuad(prog)
    }

    /** 合成 pass: 场景 + bloom */
    private fun drawComposite(sceneT: Int, bloomT: Int, strength: Float) {
        GLES20.glUseProgram(compositeProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sceneT)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(compositeProgram, "uScene"), 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bloomT)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(compositeProgram, "uBloom"), 1)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(compositeProgram, "uBloomStrength"), strength)
        GLES20.glDisable(GLES20.GL_BLEND)
        drawQuad(compositeProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
    }

    private fun drawQuad(prog: Int) {
        val aPosLoc = GLES20.glGetAttribLocation(prog, "aPosition")
        val aUvLoc = GLES20.glGetAttribLocation(prog, "aTexCoord")
        quadBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosLoc)
        GLES20.glVertexAttribPointer(aPosLoc, 2, GLES20.GL_FLOAT, false, 16, quadBuffer)
        quadBuffer.position(2)
        GLES20.glEnableVertexAttribArray(aUvLoc)
        GLES20.glVertexAttribPointer(aUvLoc, 2, GLES20.GL_FLOAT, false, 16, quadBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPosLoc)
        GLES20.glDisableVertexAttribArray(aUvLoc)
    }

    /** JS 预采样封面像素色 → aCoverColor (对齐 web applyCoverColors) */
    private fun sampleCover(bmp: Bitmap) {
        coverBitmap = bmp
        // 居中裁剪为正方形 512x512
        val size = 512
        val side = minOf(bmp.width, bmp.height)
        val sx = (bmp.width - side) / 2
        val sy = (bmp.height - side) / 2
        val scaled = try {
            Bitmap.createScaledBitmap(
                if (side == bmp.width && side == bmp.height) bmp
                else Bitmap.createBitmap(bmp, sx, sy, side, side),
                size, size, true
            )
        } catch (e: Exception) { Log.e(TAG, "sampleCover scale failed", e); return }
        // 写入 aCoverColor: 按 uv 采样
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        // aCoverColor 在顶点缓冲区中的偏移: OFF_COVER(6), 每个 stride=12 float
        val strideF = STRIDE / 4
        // 重新构建顶点缓冲区 aCoverColor 段
        val texelStepX = 1f / gridX
        val texelStepY = 1f / gridY
        for (i in 0 until particleCount) {
            val gx = i % gridX; val gy = i / gridX
            val u = (gx + 0.5f) * texelStepX
            val v = (gy + 0.5f) * texelStepY
            // 对齐 web applyCoverColors: v=0→画布底部, 需翻转
            val px = (u * size).toInt().coerceIn(0, size - 1)
            val py = ((1f - v) * size).toInt().coerceIn(0, size - 1)
            val c = pixels[py * size + px]
            val r = ((c shr 16) and 0xFF) / 255f
            val g = ((c shr 8) and 0xFF) / 255f
            val b = (c and 0xFF) / 255f
            // 跳到顶点 i 的 aCoverColor 偏移
            val base = i * strideF + OFF_COVER
            vertexBuffer.put(base, r)
            vertexBuffer.put(base + 1, g)
            vertexBuffer.put(base + 2, b)
        }
        vertexBuffer.position(0)
        // 提取封面主色 (背景色跟随/无封面主题色用)
        try {
            val dom = extractDominantColor(scaled)
            coverDominant = floatArrayOf(dom[0] / 255f, dom[1] / 255f, dom[2] / 255f)
            hasCoverColor = true
        } catch (e: Exception) { Log.e(TAG, "extractDominantColor failed", e) }
        // GLES 顶点缓冲区更新 (无需重新绑定, 直接用同一 buffer)
    }

    private fun easeInOut(t: Float): Float = if (t < 0.5f) 2f * t * t else 1f - Math.pow((-2f * t + 2f).toDouble(), 2.0).toFloat() / 2f

    private fun band(d: FloatArray, from: Int, until: Int): Float {
        if (d.size < until) return 0f
        var s = 0f
        for (i in from until until) s += d[i]
        return s / (until - from)
    }

    /**
     * 对齐 web analyser.getByteFrequencyData 的 dB 字节刻度:
     * WebAudio 默认 minDecibels=-100 / maxDecibels=-30 → byte 0..255 即 dB∈[-100,-30] → 0..1。
     * C++ FFT 输出线性幅度(满刻度正弦=1.0), 直接使用比 web 的 dB 字节小一个量级,
     * 导致 uBass/uMid/uTreble 过小 → breath/wave/trebleJ 律动不可见。
     * 统一先转 dB 再映射, 三频段与 web 感知强度一致。
     */
    private fun toWebByte(raw: Float): Float {
        if (raw <= 1e-5f) return 0f
        val db = 20f * kotlin.math.log10(raw)
        return ((db + 100f) / 70f).coerceIn(0f, 1f)
    }

    /**
     * bass 专用 dB 窗: -60dB..-15dB → 0..1。
     * toWebByte(-100..-30dB) 在 bass 上过饱和(鼓点间隙 ~0.77、鼓点 ~1.0, 动态范围被压缩),
     * 平滑后近乎恒定 → 呼吸只剩周期正弦, 显得"太规律"。
     * 更窄的窗让鼓点间隙明显回落(0.003 → 0.22), 配合 attack/release 包络形成节拍脉冲。
     */
    private fun toBeatDb(raw: Float): Float {
        if (raw <= 1e-5f) return 0f
        val db = 20f * kotlin.math.log10(raw)
        return ((db + 60f) / 45f).coerceIn(0f, 1f)
    }

    /**
     * 提取封面主色: Median Cut 量化(确定性强、感知自然、O(N log k) 实时)
     * + 过滤近黑/近白/透明像素(避免取到黑色背景/白色文字)
     * + 按「占比 × 饱和度 × 亮度居中」评分选主色(避开暗背景与白字, 取主题色)。
     * 参考: 相比 K-means(随机初始化、结果不稳定), Median Cut 每次切割保证两半非空,
     * 相同输入永远相同输出, 更适合封面切换时的稳定取色。
     */
    private fun extractDominantColor(bmp: Bitmap): IntArray {
        val size = 60
        val scaled = Bitmap.createScaledBitmap(bmp, size, size, true)
        val px = IntArray(size * size)
        scaled.getPixels(px, 0, size, 0, 0, size, size)

        val raw = ArrayList<FloatArray>(size * size)
        for (c in px) {
            if (((c shr 24) and 0xFF) < 200) continue
            val r = ((c shr 16) and 0xFF) / 255f
            val g = ((c shr 8) and 0xFF) / 255f
            val b = (c and 0xFF) / 255f
            val v = 0.299f * r + 0.587f * g + 0.114f * b
            if (v < 0.12f || v > 0.93f) continue
            raw.add(floatArrayOf(r, g, b))
        }
        // 过滤后为空(黑白封面) → 退回全部不透明像素
        val samples = if (raw.isEmpty()) {
            val all = ArrayList<FloatArray>(size * size)
            for (c in px) {
                if (((c shr 24) and 0xFF) < 200) continue
                all.add(floatArrayOf(
                    ((c shr 16) and 0xFF) / 255f,
                    ((c shr 8) and 0xFF) / 255f,
                    (c and 0xFF) / 255f
                ))
            }
            all
        } else raw

        // Median Cut: 切到 6 个色盒, 每次取像素最多的盒沿最长轴中位数切割
        data class Box(val list: ArrayList<FloatArray>)
        val boxes = ArrayList<Box>()
        boxes.add(Box(samples))
        while (boxes.size < 6) {
            var bi = -1; var bmax = -1
            for (i in boxes.indices) if (boxes[i].list.size > bmax) { bmax = boxes[i].list.size; bi = i }
            val box = boxes[bi]
            if (box.list.size < 4) break
            var minR = 1f; var maxR = 0f; var minG = 1f; var maxG = 0f; var minB = 1f; var maxB = 0f
            for (p in box.list) {
                if (p[0] < minR) minR = p[0]; if (p[0] > maxR) maxR = p[0]
                if (p[1] < minG) minG = p[1]; if (p[1] > maxG) maxG = p[1]
                if (p[2] < minB) minB = p[2]; if (p[2] > maxB) maxB = p[2]
            }
            val rR = maxR - minR; val gR = maxG - minG; val bR = maxB - minB
            val axis = if (rR >= gR && rR >= bR) 0 else if (gR >= bR) 1 else 2
            box.list.sortBy { it[axis] }
            val mid = box.list.size / 2
            boxes[bi] = Box(ArrayList(box.list.subList(0, mid)))
            boxes.add(Box(ArrayList(box.list.subList(mid, box.list.size))))
        }

        // 每盒代表色 = 均值; 评分 = 占比 × 饱和度 × 亮度居中
        var best = intArrayOf(128, 128, 128)
        var bestScore = -1f
        val total = samples.size.coerceAtLeast(1)
        for (box in boxes) {
            if (box.list.isEmpty()) continue
            var sr = 0f; var sg = 0f; var sb = 0f
            for (p in box.list) { sr += p[0]; sg += p[1]; sb += p[2] }
            val n = box.list.size
            val r = sr / n; val g = sg / n; val b = sb / n
            val v = 0.299f * r + 0.587f * g + 0.114f * b
            val sat = (maxOf(r, g, b) - minOf(r, g, b)) / (maxOf(r, g, b) + 1e-5f)
            val vScore = (1f - 2.2f * kotlin.math.abs(v - 0.55f)).coerceIn(0.05f, 1f)
            val score = (n.toFloat() / total) * (0.5f + sat) * vScore
            if (score > bestScore) {
                bestScore = score
                best = intArrayOf((r * 255f).toInt(), (g * 255f).toInt(), (b * 255f).toInt())
            }
        }
        return best
    }

    private fun makeProgram(vs: String, fs: String): Int {
        val v = compile(GLES20.GL_VERTEX_SHADER, vs)
        val f = compile(GLES20.GL_FRAGMENT_SHADER, fs)
        if (v == 0 || f == 0) return 0
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v); GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] != GLES20.GL_TRUE) { Log.e(TAG, GLES20.glGetProgramInfoLog(p)); GLES20.glDeleteProgram(p); return 0 }
        return p
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src); GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] != GLES20.GL_TRUE) { Log.e(TAG, GLES20.glGetShaderInfoLog(s)); GLES20.glDeleteShader(s); return 0 }
        return s
    }
}

@Composable
fun Particle3DBackground(enabled: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val renderer = remember { Particle3DRenderer(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val playerState by MusicPlayerController.state.collectAsState()
    val gyroEnabled by Filament3DConfig.gyroEnabledFlow.collectAsState()
    val particleEdge by Filament3DConfig.particleEdgeFlow.collectAsState()
    val fpsLimit by Filament3DConfig.fpsLimitFlow.collectAsState()
    val scatterMode by Filament3DConfig.scatterModeFlow.collectAsState()

    LaunchedEffect(enabled) { renderer.setEnabled(enabled) }
    LaunchedEffect(playerState.isPlaying) { renderer.setPlaying(playerState.isPlaying) }
    LaunchedEffect(gyroEnabled) { renderer.setGyroEnabled(gyroEnabled) }
    LaunchedEffect(particleEdge) { renderer.setParticleEdge(particleEdge) }
    LaunchedEffect(fpsLimit) { renderer.setFpsLimit(fpsLimit) }
    LaunchedEffect(scatterMode) { renderer.setScatterMode(scatterMode) }

    // 监听专辑封面变化: 注入封面 Bitmap (对齐 web loadCoverImage)
    val artMap by com.bicy.whitenoise.music.AlbumArtCache.artFlow.collectAsState()
    LaunchedEffect(playerState.currentTrack?.id) {
        val track = playerState.currentTrack
        if (track != null) {
            com.bicy.whitenoise.music.AlbumArtCache.requestAlbumArt(track)
        } else {
            renderer.setCoverBitmap(null)
        }
    }
    LaunchedEffect(artMap, playerState.currentTrack?.id) {
        val track = playerState.currentTrack
        if (track != null) {
            val bytes = artMap[track.id]
            if (bytes != null) {
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                renderer.setCoverBitmap(bmp)
            } else if (artMap.containsKey(track.id)) {
                // 确认无封面
                renderer.setCoverBitmap(null)
            }
        } else {
            renderer.setCoverBitmap(null)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_PAUSE -> renderer.setForeground(false)
                Lifecycle.Event.ON_RESUME -> renderer.setForeground(true)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs); renderer.destroy() }
    }
    // TextureView 生命周期由 SurfaceTextureListener 自管理, 无需手动 onResume/onPause
    AndroidView(factory = { renderer.textureView }, modifier = modifier.fillMaxSize())
}
