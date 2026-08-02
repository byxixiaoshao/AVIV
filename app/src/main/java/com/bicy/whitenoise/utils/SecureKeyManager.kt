package com.bicy.whitenoise.utils

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureKeyManager {

    private const val TAG = "SecureKeyManager"
    private const val FILE_NAME = "ai_secure_prefs"
    private const val KEY_API_KEY = "ai_api_key"

    enum class AuthError { USER_CANCELED, NO_BIOMETRIC, OTHER }

    fun canAuthenticate(context: Context): Boolean {
        val bm = AndroidBiometricManager.from(context)
        return bm.canAuthenticate(
            AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK or
            AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == AndroidBiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (AuthError, String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("取消")
            .setAllowedAuthenticators(AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val err = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> AuthError.USER_CANCELED
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> AuthError.NO_BIOMETRIC
                    else -> AuthError.OTHER
                }
                onError(err, errString.toString())
            }
        }

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    private fun getEncryptedPrefs(context: Context) = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    fun saveApiKey(context: Context, apiKey: String): Boolean {
        val prefs = getEncryptedPrefs(context) ?: run {
            Log.e(TAG, "无法创建加密存储")
            return false
        }
        return runCatching {
            prefs.edit().putString(KEY_API_KEY, apiKey).apply()
            true
        }.getOrElse {
            Log.e(TAG, "保存 API Key 失败", it)
            false
        }
    }

    fun loadApiKey(context: Context): String? = getEncryptedPrefs(context)?.getString(KEY_API_KEY, null)

    fun clearApiKey(context: Context) {
        getEncryptedPrefs(context)?.edit()?.remove(KEY_API_KEY)?.apply()
    }
}
