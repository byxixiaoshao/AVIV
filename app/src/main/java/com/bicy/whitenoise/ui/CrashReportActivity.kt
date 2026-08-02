package com.bicy.whitenoise.ui

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.bicy.whitenoise.service.AnomalyLevel
import com.bicy.whitenoise.utils.LogManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CrashReportActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_INFO = "crash_info"
        const val EXTRA_CRASH_FILE = "crash_file"
        const val EXTRA_ANOMALY_LEVEL = "anomaly_level"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashInfo = intent.getStringExtra(EXTRA_CRASH_INFO) ?: "未知崩溃"
        val crashFilePath = intent.getStringExtra(EXTRA_CRASH_FILE) ?: ""
        val anomalyLevel = try {
            AnomalyLevel.valueOf(intent.getStringExtra(EXTRA_ANOMALY_LEVEL) ?: "CRITICAL")
        } catch (_: Exception) { AnomalyLevel.CRITICAL }

        setContent {
            MaterialTheme {
                CrashReportScreen(
                    crashInfo = crashInfo,
                    crashFilePath = crashFilePath,
                    anomalyLevel = anomalyLevel,
                    onExport = { exportCrashLog(crashFilePath) },
                    onShare = { shareCrashLog(crashFilePath) },
                    onContinue = { finish() },
                    onRestart = { restartApp() },
                    onClose = {
                        finish()
                        Process.killProcess(Process.myPid())
                    }
                )
            }
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
        Process.killProcess(Process.myPid())
    }

    private fun exportCrashLog(filePath: String) {
        try {
            var sourceFile = File(filePath)
            if (!sourceFile.exists() || filePath.isBlank()) {
                val crashFiles = LogManager.getCrashLogFiles()
                if (crashFiles.isEmpty()) {
                    Toast.makeText(this, "没有找到崩溃日志", Toast.LENGTH_SHORT).show()
                    return
                }
                sourceFile = crashFiles.first()
            }
            val exportDir = File(filesDir, "logs/ExportedLogs")
            exportDir.mkdirs()
            val exportName = "crash_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.log"
            val destFile = File(exportDir, exportName)
            sourceFile.copyTo(destFile, overwrite = true)
            Toast.makeText(this, "已导出到: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCrashLog(filePath: String) {
        try {
            var sourceFile = File(filePath)
            if (!sourceFile.exists() || filePath.isBlank()) {
                val crashFiles = LogManager.getCrashLogFiles()
                if (crashFiles.isEmpty()) {
                    Toast.makeText(this, "没有找到崩溃日志", Toast.LENGTH_SHORT).show()
                    return
                }
                sourceFile = crashFiles.first()
            }
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", sourceFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "崩溃日志 - ${sourceFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "分享崩溃日志"))
        } catch (e: Exception) {
            Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun CrashReportScreen(
    crashInfo: String,
    crashFilePath: String,
    anomalyLevel: AnomalyLevel,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isCritical = anomalyLevel == AnomalyLevel.CRITICAL
    var fullContent by remember {
        mutableStateOf(
            try {
                if (crashFilePath.isNotBlank()) File(crashFilePath).readText()
                else crashInfo
            } catch (e: Exception) { crashInfo }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = if (isCritical) "应用崩溃详情" else "应用异常",
            fontSize = 22.sp,
            color = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 提示信息
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isCritical)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isCritical)
                        "出现崩溃：$crashInfo\n请将日志交给开发者。"
                    else
                        "继续运行可能会不稳定，但不代表目前不可用。建议重新启动应用，并将日志交给开发者。",
                    fontSize = 13.sp,
                    color = if (isCritical)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 按钮栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onExport, modifier = Modifier.weight(1f)) { Text("导出日志") }
            Button(onClick = onShare, modifier = Modifier.weight(1f)) { Text("分享日志") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 完整日志
        Text(
            text = "完整日志",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Text(
                text = fullContent,
                modifier = Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 底部按钮
        if (isCritical) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("关闭应用") }
                OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("重新启动") }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onContinue, modifier = Modifier.weight(1f)) { Text("继续运行") }
                Button(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("重新启动") }
            }
        }
    }
}
