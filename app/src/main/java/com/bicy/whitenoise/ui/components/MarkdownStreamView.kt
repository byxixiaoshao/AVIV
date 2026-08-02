package com.bicy.whitenoise.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 基于 WebView 的 Markdown 流式渲染组件。
 *
 * 页面加载为异步操作，首帧文本注入需等待 onPageFinished。流式更新直接注入。
 * 渲染完全在 WebView 自有线程，不阻塞 Compose 主线程 → 不会 ANR。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownStreamView(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    val colorHex = remember { String.format("#%06X", 0xFFFFFF and textColor.toArgb()) }
    val context = LocalContext.current

    // 页面是否已加载完成（异步 HTML 模板加载）
    var isPageLoaded by remember { mutableStateOf(false) }
    // 页面加载完成前的待渲染文本
    val pendingText = remember { mutableStateOf(text) }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(0x00000000)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isFocusable = false
            isClickable = false
            isLongClickable = false
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.loadWithOverviewMode = true
            addJavascriptInterface(AndroidBridge(), "Android")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    isPageLoaded = true
                    // 注入未完成前保留的文本
                    injectJs(view, pendingText.value)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        webView.loadDataWithBaseURL(null, buildHtmlTemplate(colorHex), "text/html", "UTF-8", null)
        onDispose {
            isPageLoaded = false
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { wv ->
            pendingText.value = text
            if (isPageLoaded) {
                injectJs(wv, text)
            }
        }
    )
}

/** 转义并注入 JS 渲染 */
private fun injectJs(webView: WebView?, raw: String) {
    val escaped = raw
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("$", "\\$")
    webView?.evaluateJavascript(
        "renderMarkdown(`$escaped`)", null
    )
}

/**
 * JS→Kotlin 桥接。
 */
private class AndroidBridge {
    @JavascriptInterface
    fun onContentHeight(height: Int) { /* 占位 */ }
}

private fun buildHtmlTemplate(textColorHex: String): String = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body {
    font-family: -apple-system, 'Segoe UI', Roboto, sans-serif;
    font-size: 15px;
    line-height: 1.6;
    color: $textColorHex;
    background: transparent;
    padding: 0;
    word-wrap: break-word;
    overflow-wrap: break-word;
}
b, strong { font-weight: 700; }
i, em { font-style: italic; }
code {
    font-family: 'Courier New', monospace;
    font-size: 13px;
    background: rgba(255,255,255,0.12);
    padding: 1px 5px;
    border-radius: 3px;
}
pre {
    background: rgba(255,255,255,0.08);
    border-radius: 8px;
    padding: 12px;
    margin: 8px 0;
    overflow-x: auto;
}
pre code {
    background: transparent;
    padding: 0;
    font-size: 13px;
    line-height: 1.5;
    white-space: pre-wrap;
}
h1 { font-size: 1.4em; font-weight: 700; margin: 10px 0 6px; }
h2 { font-size: 1.25em; font-weight: 700; margin: 8px 0 5px; }
h3 { font-size: 1.1em; font-weight: 700; margin: 6px 0 4px; }
ul, ol { padding-left: 20px; margin: 4px 0; }
li { margin: 2px 0; }
blockquote {
    border-left: 3px solid rgba(255,255,255,0.3);
    padding-left: 12px;
    margin: 8px 0;
    opacity: 0.85;
}
a { color: #6ab7ff; text-decoration: none; }
hr { border: none; border-top: 1px solid rgba(255,255,255,0.15); margin: 12px 0; }
p { margin: 4px 0; }
</style>
</head>
<body><div id="content"></div>
<script>
function renderMarkdown(text) {
    var html = text;

    // 代码块 ```...```
    html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, function(m, lang, code) {
        return '<pre><code>' + escapeHtml(code.trimEnd()) + '</code></pre>';
    });

    // 行内代码 `...`
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

    // 粗体 **...**
    html = html.replace(/\*\*(.+?)\*\*/g, '<b>$1</b>');

    // 斜体 *...*
    html = html.replace(/(?<!\*)\*([^*\n]+?)\*(?!\*)/g, '<i>$1</i>');

    // ### 标题
    html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
    html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
    html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');

    // 无序列表 - item
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>');
    html = html.replace(/<\/ul>\n<ul>/g, '\n');

    // 有序列表 1. item
    html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ol>$1</ol>');
    html = html.replace(/<\/ol>\n<ol>/g, '\n');

    // > 引用
    html = html.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>');

    // --- 分隔线
    html = html.replace(/^---$/gm, '<hr>');

    // 链接 [text](url)
    html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>');

    // 换行
    html = html.replace(/\n\n/g, '</p><p>');
    html = html.replace(/\n/g, '<br>');
    html = '<p>' + html + '</p>';

    // 清理空段落
    html = html.replace(/<p><\/p>/g, '');
    html = html.replace(/<p><br><\/p>/g, '');

    document.getElementById('content').innerHTML = html;
}

function escapeHtml(text) {
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
</script>
</body>
</html>
""".trimIndent()
