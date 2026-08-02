package com.bicy.whitenoise.data.agent.effect.EffectToolsPart

import com.bicy.whitenoise.data.agent.AgentToolPart.*
import org.json.JSONObject

/**
 * 算法信息查询工具
 * 供 AI 主动查询各类音频效果的算法说明、参数范围、调整建议、注意事项。
 * 支持查询：EQ 算法、速率音调算法、音质效果算法、混响声向算法、EQ in/out 插值
 */
class GetAlgorithmInfoTool : AgentTool {
    override val name = "get_algorithm_info"
    override val description = "查询音频效果算法的详细说明（算法原理、参数范围、调整建议、注意事项）。" +
        "在调整 EQ、速率音调、音质效果、混响声向等参数前，若不确定算法行为可先调用此工具查询。" +
        "支持查询：equalizer(均衡器)、pitch_speed(速率音调)、creative_effect(音质效果)、" +
        "reverb_spatial(混响声向)、eq_interpolation(EQ插值类型)、limiter(限幅器)。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "effect_name" to ToolProperty("string", "效果名称",
                enum = listOf("equalizer", "pitch_speed", "creative_effect", "reverb_spatial", "eq_interpolation", "limiter")),
            "sub_effect" to ToolProperty("string", "子效果名称（可选）。如 creative_effect 可指定 lofi/eight_bit/virtual_bass/multiband_compressor 等具体效果")
        ), required = listOf("effect_name")
    )

    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val effectName = params.getString("effect_name")
        val subEffect = params.optString("sub_effect", "")
        val info = AlgorithmDatabase.query(effectName, subEffect)
        if (info == null) {
            val msg = if (subEffect.isNotEmpty()) "未知效果：$effectName/$subEffect" else "未知效果：$effectName"
            return ToolResult.Error(msg)
        }
        return ToolResult.Success(info)
    }
}

/**
 * 算法数据库：集中维护各效果的算法说明
 */
object AlgorithmDatabase {

    fun query(effectName: String, subEffect: String = ""): String? = when (effectName) {
        "equalizer" -> equalizerInfo()
        "pitch_speed" -> pitchSpeedInfo()
        "creative_effect" -> creativeEffectInfo(subEffect)
        "reverb_spatial" -> reverbSpatialInfo()
        "eq_interpolation" -> eqInterpolationInfo()
        "limiter" -> limiterInfo()
        else -> null
    }

    private fun equalizerInfo(): String = """
【均衡器（EQ）算法说明】
原理：C++ DSP 层采用 biquad（双二阶）级联滤波器实现无极均衡器。每个控制点对应一个独立 biquad 滤波器，按频率升序串联，输出累加。
滤波类型：
  - peaking（峰值）：在指定频率处进行峰值提升/衰减，Q 值控制带宽（Q 越大带宽越窄）
  - low_shelf（低频架）：对低于指定频率的频段整体提升/衰减
  - high_shelf（高频架）：对高于指定频率的频段整体提升/衰减
参数范围：
  - 频率 frequency：10Hz - 24000Hz
  - 增益 gain：-24dB 到 +24dB
  - Q 值（带宽因子）：0.1 - 10.0，默认 1.0（Q 越大曲线越尖锐）
  - 频段数：默认上限 16 个（设置中可解除限制）
调整建议：
  - 人声增强：1kHz-3kHz 区域 +2~4dB
  - 低频浑浊：200Hz 附近 -2~3dB
  - 高频刺耳：5kHz-8kHz 区域 -1~2dB
  - 整体明亮：10kHz high_shelf +2dB
注意事项：
  - 多个频段叠加可能导致总增益超限，限幅器会自动介入
  - Q 值过低（<0.3）会显著影响相邻频段
  - 极端增益（±24dB）可能引起相位失真
""".trim()

    private fun pitchSpeedInfo(): String = """
【速率/音调算法说明】
原理：基于 SoundTouch WSOLA（Waveform Similarity Overlap-Add）时域拉伸算法。
  - setTempo(speed)：控制播放速率，变速不变调（1.0=原速，2.0=加速一倍，0.5=减速一半）
  - setPitchSemiTones(semitones)：控制音调偏移，变调不变速（+12=升一个八度，-12=降一个八度）
  - 速度与音调解耦：可独立调整，互不影响
参数范围：
  - 速度 speed：0.1 - 5.0（1.0=原速）
  - 音调 pitch：-12 到 +12 半音（0=原调）
固定步进（AI 未指定具体数值时）：
  - 速度步进 ±0.25x
  - 音调步进 ±1 半音
音乐与白噪音：完全独立，互不影响；白噪音按轨道持久化。
调整建议：
  - 学习/有声书：1.25x - 1.5x
  - 音乐鉴赏：0.9x - 1.1x（保持原调）
  - 变调练习：±2~5 半音
注意事项：
  - 极端速度（<0.3 或 >3.0）可能引入 WSOLA 伪影
  - 大幅参数变化（速度>0.3 或 音调>0.8 半音）会重置 WSOLA 缓冲，有短暂延迟
  - 变速时进度计算基于输出帧数 × 速度比，避免漂移
""".trim()

    private fun creativeEffectInfo(subEffect: String): String = when (subEffect) {
        "lofi" -> """
【LoFi 效果】
原理：位深降低 + 采样率抽取，模拟老式磁带/唱片质感。
参数：强度 0.0-1.0
注意：强度过高（>0.7）会产生明显量化噪声，已在本次更新中弱化噪声
""".trim()
        "eight_bit" -> """
【8-bit 效果】
原理：位深降至 3-5 bit + 2-5x 抽取 + 5kHz 低通，模拟 8-bit 游戏机音色。
参数：强度 0.0-1.0
注意：本次更新已调软参数（最小位深 3-5bit，抽取 2-5x，低通 5kHz），减少刺耳感
""".trim()
        "underwater" -> """
【水下效果】
原理：低通滤波 + 调制，模拟水下闷响。
参数：强度 0.0-1.0
""".trim()
        "alien_signal" -> """
【外星信号效果】
原理：环形调制 + 频率偏移，产生金属感异响。
参数：强度 0.0-1.0
""".trim()
        "megaphone" -> """
【扩音器效果】
原理：带通滤波 + 软削波失真，模拟喇叭扩音。
参数：强度 0.0-1.0
注意：本次更新已提升最大强度，保持线性参数映射
""".trim()
        "hifi" -> """
【伪还原（HiFi）效果】
原理：软 NLD（非线性器件）+ 带通滤波 + 软膝限制 + DC 阻断，伪还原高频细节。
参数：强度 0.0-1.0
注意：处理前对样本值 clamp 到 [-1,1] 再应用平方项，防止爆音
""".trim()
        "stereo_widener" -> """
【立体声加宽】
原理：Mid/Side 处理，增强 Side 通道扩展声场。
参数：强度 0.0-1.0
""".trim()
        "virtual_bass" -> """
【虚拟低音增强】
原理：时域 NLD（非线性器件），低频前 80-100Hz 紧密低通，NLD 后 80-300Hz 带通，减少互调失真。
参数：强度 0.0-1.0
注意：基于 LGPL 2.1 组件实现（参考 FFmpeg af_virtualbass）
""".trim()
        "multiband_compressor" -> """
【多段压缩】
原理：Linkwitz-Riley 交叉滤波器分 2-4 段，每段独立压缩，求和后主限制。
参数：强度 0.0-1.0
注意：强度 >1 时使用 sqrt 压缩 + tanh 软限制防爆音；基于 LGPL 2.1 组件（参考 mcompand）
""".trim()
        else -> """
【音质效果总览】
支持效果：lofi / eight_bit / underwater / alien_signal / megaphone / hifi / stereo_widener / virtual_bass / multiband_compressor
统一参数：强度 0.0-1.0
查询具体效果请传入 sub_effect 参数（如 creative_effect + sub_effect=virtual_bass）
所有效果均通过 set_creative_effect 工具调整
""".trim()
    }

    private fun reverbSpatialInfo(): String = """
【混响/声向算法说明】
混响原理：Freeverb 衍生算法，基于梳状滤波器 + 全通滤波器模拟房间反射。
混响参数：
  - room_size 房间大小：0.0-1.0
  - decay_time 衰减时间：0.1-10.0 秒
  - damping 高频衰减：0.0-1.0
  - wet_level 湿润电平：0.0-1.0
  - dry_level 干声电平：0.0-1.0
  - pre_delay 预延迟：0-0.2 秒
  - insulation 隔声：0.0-1.0
声向原理：通过 L/R 通道增益差 + 延迟模拟空间定位。
  - fixed 模式：固定 L/R/Front/Back 偏移
  - surround 模式：环绕轨迹（半径 + 速度）
  - random 模式：随机游走（最大距离 + 速度）
调整建议：
  - 小房间人声：room_size=0.3, decay=0.8s, wet=0.3
  - 大厅音乐：room_size=0.8, decay=3.0s, wet=0.4
注意事项：
  - wet_level 过高（>0.6）会导致声音模糊
  - decay_time 过长（>5s）可能掩盖细节
""".trim()

    private fun eqInterpolationInfo(): String = """
【EQ 插值类型（in/out）说明】
作用：决定两个频段控制点之间的频响曲线变化规律。
每个控制点有两个独立插值参数：
  - curve_in：该点向低频方向的插值类型
  - curve_out：该点向高频方向的插值类型
四种类型：
  - linear（折线）：两点间线性插值，过渡直接，无平滑
  - catmull（Catmull-Rom 样条）：自然样条曲线，过渡平滑，可能轻微过冲
  - cubic（三次 Hermite）：单调三次插值，Fritsch-Carlson 约束防过冲/振铃
  - step（阶梯保持）：保持当前点增益直到下一点，突变式
合并规则：相邻两点取较"硬"的算法（step > linear > cubic > catmull）
调整建议：
  - 精确控制：linear 或 step
  - 自然过渡：catmull
  - 防过冲：cubic
持久化：插值类型随 EQ 预设保存，切换预设时恢复
""".trim()

    private fun limiterInfo(): String = """
【限幅器算法说明】
原理：动态范围压缩，超过阈值的信号按比例衰减，防止削波。
参数：
  - threshold 阈值：0.0-1.0（默认 0.9，即 -1.94dB）
  - attack 启动时间：毫秒（默认 5.0）
  - release 释放时间：毫秒（默认 50.0）
  - limit_equalizer/effects/reverb/spatial：是否分别限制各通路输出
调整建议：
  - 防爆音：threshold=0.9, attack=5ms, release=50ms
  - 更激进：threshold=0.8（音量略降但更安全）
注意事项：
  - threshold 过低（<0.5）会显著降低音量
  - attack 过短（<1ms）可能引入失真
  - 建议保持 limit_* 全开，保护后续通路
""".trim()
}
