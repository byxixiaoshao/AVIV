package com.bicy.whitenoise.wRT1

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.bicy.whitenoise.BuildConfig
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object SecurityManager {

    private const val TAG = "SecurityManager"

    private val referenceHashes = setOf(
        "430924A84A7426955374733B1860D041BCAB3524CBA1110D3FF1B7A5D1706682"
    )

    private val trustedInstallers = setOf(
        "com.android.vending",
        "com.amazon.venezia",
        "com.huawei.appmarket",
        "com.oppo.market",
        "com.xiaomi.market",
        "com.sec.android.app.samsungapps"
    )

    private var cacheResult: Boolean? = null

    fun validate(context: Context): Boolean {
        if (cacheResult != null) {
            return cacheResult!!
        }

        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                cacheResult = false
                return false
            }

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signature.toByteArray())
                val hash = digest.joinToString("") { "%02X".format(it) }

                if (hash in referenceHashes) {
                    cacheResult = true
                    return true
                }
            }

            cacheResult = false
            return false
        } catch (e: Exception) {
            cacheResult = false
            return false
        }
    }

    fun computeHash(context: Context): String? {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (!signatures.isNullOrEmpty()) {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signatures[0].toByteArray())
                return digest.joinToString("") { "%02X".format(it) }
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    fun checkInstaller(context: Context): Boolean {
        return try {
            val installer = context.packageManager.getInstallerPackageName(context.packageName)
            installer == null || installer in trustedInstallers
        } catch (e: Exception) {
            false
        }
    }

    fun checkDebuggable(context: Context): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                0
            )
            (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0
        } catch (e: Exception) {
            false
        }
    }

    fun checkApkIntegrity(context: Context): Boolean {
        return try {
            val apkPath = context.applicationInfo.sourceDir
            val zipFile = ZipFile(apkPath)
            var entryCount = 0
            var totalCrc = 0L

            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory) {
                    entryCount++
                    totalCrc += entry.crc.toLong() and 0xFFFFFFFFL
                }
            }
            zipFile.close()

            entryCount > 0
        } catch (e: Exception) {
            false
        }
    }

    fun inspect(context: Context): Boolean {
        if (!validate(context)) return false
        if (!BuildConfig.DEBUG && !checkDebuggable(context)) return false
        if (!checkApkIntegrity(context)) return false
        return true
    }

    fun flush() {
        cacheResult = null
    }
}
