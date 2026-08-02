@file:Suppress("DEPRECATION")

package com.bicy.whitenoise.ui.screens

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import com.bicy.whitenoise.R
import com.bicy.whitenoise.audio.PlaybackStateManager
import com.bicy.whitenoise.music.MusicPlayerController
import com.bicy.whitenoise.servies.MusicService
import com.bicy.whitenoise.storage.config.ConfigStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import com.bicy.whitenoise.ui.screens.ChatStatePart.*

/**
 * 创建悬浮窗口视图并返回 (rootView, cleanup)。
 * cleanup 在窗口关闭时调用以释放监听器/协程。
 */
fun createFloatingWindowView(
    context: Context,
    onCloseRequest: () -> Unit
): Pair<View, () -> Unit> {
    val dp12 = dpPx(12, context)
    val dp8 = dpPx(8, context)
    val dp6 = dpPx(6, context)
    val dp4 = dpPx(4, context)

    // 局部引用 — 无全局状态
    var wnIconView: ImageView? = null
    var musicIconView: ImageView? = null
    val iconScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    var wnListener: (() -> Unit)? = null

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp12, dp12, dp12, dp12)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#FF1C1B1F"))
            cornerRadius = dpPx(16, context).toFloat()
        }
    }

    val buttonRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val aiMode = ConfigStorage.config.value.floatingPetWindowMode

    // === 白噪音按钮 ===
    val wnBtn = makeIconView(context, getWnIconRes(), !aiMode)
    wnIconView = wnBtn
    wnBtn.setOnClickListener {
        val service = MusicService.getInstance()
        val playing = PlaybackStateManager.getPlayingSounds()
        if (playing.isNotEmpty()) {
            PlaybackStateManager.pauseAll()
            service?.pauseAllSounds()
        } else {
            PlaybackStateManager.resumeAll()
            service?.resumeAllSounds()
        }
    }
    buttonRow.addView(wrapWithLabel(context, wnBtn, context.getString(R.string.floating_window_wn_play)))

    // 注册 PlaybackStateManager 监听器
    wnListener = { wnIconView.post { wnIconView.setImageResource(getWnIconRes()) } }
    PlaybackStateManager.addListener(wnListener)

    // === 音乐按钮 ===
    buttonRow.addView(wrapWithLabel(context,
        makeIconView(context, R.drawable.ic_floating_skip_prev, !aiMode),
        context.getString(R.string.floating_window_music_prev)
    ) { MusicPlayerController.previous() })

    val musicBtn = makeIconView(context, getMusicIconRes(), !aiMode)
    musicIconView = musicBtn
    musicBtn.setOnClickListener { MusicPlayerController.playPause() }
    buttonRow.addView(wrapWithLabel(context, musicBtn, context.getString(R.string.floating_window_music_play)))

    // 监听音乐播放状态实时更新图标
    iconScope.launch {
        MusicPlayerController.state.collectLatest {
            musicIconView.post {
                musicIconView.setImageResource(getMusicIconRes())
            }
        }
    }

    buttonRow.addView(wrapWithLabel(context,
        makeIconView(context, R.drawable.ic_floating_skip_next, !aiMode),
        context.getString(R.string.floating_window_music_next)
    ) { MusicPlayerController.next() })

    root.addView(buttonRow)

    // === AI 对话区 ===
    if (aiMode) {
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpPx(1, context)
            ).apply { topMargin = dp8; bottomMargin = dp8 }
            setBackgroundColor(Color.parseColor("#40FFFFFF"))
        }
        root.addView(divider)

        val chatScroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpPx(200, context)
            )
        }
        val chatContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp4)
        }
        chatScroll.addView(chatContainer)
        root.addView(chatScroll)

        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp8 }
        }

        val input = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            hint = context.getString(R.string.floating_window_chat_hint)
            maxLines = 2
            setTextSize(12f)
            setPadding(dp8, dp6, dp8, dp6)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1AFFFFFF"))
                cornerRadius = dpPx(12, context).toFloat()
            }
            setHintTextColor(Color.parseColor("#80FFFFFF"))
            setTextColor(Color.WHITE)
        }
        inputRow.addView(input)

        val sendBtn = TextView(context).apply {
            text = context.getString(R.string.ai_send)
            setTextColor(Color.parseColor("#4FC3F7"))
            setTextSize(13f)
            gravity = Gravity.CENTER
            setPadding(dp8, dp6, dp8, dp6)
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    ChatState.messages.add(ChatMessage(text, isUser = true))
                    input.text.clear()
                    refreshChatMessages(chatContainer)
                }
            }
        }
        inputRow.addView(sendBtn)
        root.addView(inputRow)

        root.tag = chatContainer
    }

    // 返回 View 和清理函数
    val cleanup: () -> Unit = {
        wnListener.let { PlaybackStateManager.removeListener(it) }
        iconScope.cancel()
    }

    return Pair(root, cleanup)
}

fun refreshChatMessages(chatContainer: LinearLayout) {
    chatContainer.removeAllViews()
    val ctx = chatContainer.context
    val messages = ChatState.messages.takeLast(20)
    for (msg in messages) {
        chatContainer.addView(createChatBubbleView(ctx, msg))
    }
}

private fun createChatBubbleView(context: Context, message: ChatMessage): View {
    val dp8 = dpPx(8, context)
    val dp12 = dpPx(12, context)
    val dp4 = dpPx(4, context)

    val container = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp4 }
        gravity = if (message.isUser) Gravity.END else Gravity.START
    }

    val bubble = TextView(context).apply {
        text = message.content
        setTextSize(12f)
        setPadding(dp12, dp8, dp12, dp8)
        maxWidth = dpPx(200, context)
        background = GradientDrawable().apply {
            setColor(Color.parseColor(if (message.isUser) "#4FC3F7" else "#2A2A2E"))
            cornerRadius = dpPx(12, context).toFloat()
        }
        setTextColor(if (message.isUser) Color.BLACK else Color.WHITE)
    }

    container.addView(bubble)
    return container
}

private fun makeIconView(context: Context, iconRes: Int, big: Boolean): ImageView {
    val size = dpPx(if (big) 44 else 36, context)
    val dp6 = dpPx(6, context)
    return ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(size, size).apply { bottomMargin = dp6 }
        setImageResource(iconRes)
        setColorFilter(Color.parseColor("#E0E0E0"))
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#1A4FC3F7"))
            shape = GradientDrawable.OVAL
        }
        setPadding(dp6 * 2, dp6 * 2, dp6 * 2, dp6 * 2)
        scaleType = ImageView.ScaleType.FIT_CENTER
        tag = iconRes
    }
}

private fun wrapWithLabel(
    context: Context,
    icon: View,
    label: String,
    onClick: (() -> Unit)? = null
): View {
    if (onClick != null) {
        icon.setOnClickListener { onClick() }
    }
    val dp4 = dpPx(4, context)
    return LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        )
        addView(icon)
        addView(TextView(context).apply {
            text = label
            setTextSize(10f)
            setTextColor(Color.parseColor("#B0B0B0"))
            gravity = Gravity.CENTER
        })
    }
}

private fun getWnIconRes(): Int =
    if (PlaybackStateManager.getPlayingSounds().isNotEmpty())
        R.drawable.ic_floating_wn_playing
    else
        R.drawable.ic_floating_wn_paused

private fun getMusicIconRes(): Int =
    if (MusicPlayerController.state.value.isPlaying)
        R.drawable.ic_floating_pause
    else
        R.drawable.ic_floating_play

private fun dpPx(dp: Int, context: Context): Int =
    (dp * context.resources.displayMetrics.density).toInt()
