package com.bicy.whitenoise

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.bicy.whitenoise.H3HO.ReverbManager
import com.bicy.whitenoise.xnef.MusicLibrary
import com.bicy.whitenoise.wRT1.SecurityManager
import com.bicy.whitenoise.JwJY.AppStorage
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.JwJY.sBYh.WhiteNoiseStorage
import com.bicy.whitenoise.DzBD.PeeU.ItemList
import com.bicy.whitenoise.oJft.TimerManager
import com.bicy.whitenoise.yODW.ZFNn.ThemeColorManager
import com.bicy.whitenoise.y10p.LanguageManager
import com.bicy.whitenoise.y10p.LogManager
import com.bicy.whitenoise.y10p.ScatteredStorageManager
import com.bicy.whitenoise.y10p.SoundStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var splashContainer: FrameLayout
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var progressText: TextView
    private lateinit var securityWarningView: LinearLayout

    private var isAllLoaded = false
    private var isAnimationReady = false
    private var isPermissionGranted = false
    private var isInitializing = false
    private var isAnimationFinished = false
    private var isSignatureInvalid = false
    private var isSecurityWarningShown = false

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private val permissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        splashContainer = FrameLayout(this)
        splashContainer.setBackgroundColor(0xFFF7F5F0.toInt())
        setContentView(splashContainer)

        setupSecurityWarningView()
        setupLottieAnimation()

        android.util.Log.d("SplashActivity", "当前签名哈希: ${SecurityManager.computeHash(this)}")
        android.util.Log.d("SplashActivity", "签名检查开关: ${BuildConfig.ENABLE_INTEGRITY}")

        if (BuildConfig.ENABLE_INTEGRITY && !SecurityManager.inspect(this)) {
            isSignatureInvalid = true
            hideProgressTextForSecurityWarning()
            return
        }

        if (checkStoragePermission()) {
            isPermissionGranted = true
            tryStartInitialization()
        } else {
            requestPermissions(requiredPermissions, permissionRequestCode)
        }
    }

    private fun hideProgressTextForSecurityWarning() {
        progressText.visibility = View.INVISIBLE
    }

    private fun setupSecurityWarningView() {
        securityWarningView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            alpha = 0f
        }

        val titleText = TextView(this).apply {
            text = getString(R.string.security_warning_title)
            textSize = 22f
            setTextColor(0xFF7A7A7A.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(24))
        }
        securityWarningView.addView(
            titleText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val descText = TextView(this).apply {
            text = getString(R.string.security_warning_message)
            textSize = 15f
            setTextColor(0xFF3D3A35.toInt())
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.5f)
            setPadding(dpToPx(48), 0, dpToPx(48), dpToPx(40))
        }
        securityWarningView.addView(
            descText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val confirmButton = Button(this).apply {
            text = getString(R.string.security_warning_uninstall)
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF7A7A7A.toInt())
            setPadding(dpToPx(48), dpToPx(14), dpToPx(48), dpToPx(14))
            isAllCaps = false
            setOnClickListener {
                val intent = Intent(Intent.ACTION_DELETE)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                finishAffinity()
            }
        }
        securityWarningView.addView(
            confirmButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        splashContainer.addView(securityWarningView, params)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun checkStoragePermission(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            isPermissionGranted = true
            tryStartInitialization()
        }
    }

    private fun setupLottieAnimation() {
        lottieAnimationView = LottieAnimationView(this).apply {
            setAnimation(R.raw.splash_animation)
            repeatCount = 0
            speed = 1f
            enableMergePathsForKitKatAndAbove(true)
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        splashContainer.addView(lottieAnimationView, params)

        progressText = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFF3D3A35.toInt())
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 150
        }
        splashContainer.addView(progressText, textParams)

        lottieAnimationView.addLottieOnCompositionLoadedListener {
            isAnimationReady = true
            lottieAnimationView.playAnimation()
            tryStartInitialization()
        }

        lottieAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isAnimationFinished = true
                tryFadeOut()
            }
        })
    }

    private fun tryStartInitialization() {
        if (isAnimationReady && isPermissionGranted && !isInitializing && !isSignatureInvalid) {
            isInitializing = true
            initializeComponents()
        }
    }

    private fun initializeComponents() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SoundStorageManager.init(applicationContext)
                ScatteredStorageManager.init(applicationContext)

                AppStorage.init(applicationContext)
                ConfigStorage.init()

                LogManager.init(applicationContext)
                if (ConfigStorage.isLogEnabled()) {
                    LogManager.setLogEnabled(true)
                }

                ThemeColorManager.initAsync(applicationContext)
                TimerManager.init(applicationContext)
                LanguageManager.init(applicationContext)

                ItemList.loadManifest(applicationContext) { _, _ -> }

                WhiteNoiseStorage.init()

                ReverbManager.initAsync(applicationContext)

                MusicLibrary.init(applicationContext)
                MusicLibrary.loadFromCacheOnly()

                isAllLoaded = true

                withContext(Dispatchers.Main) {
                    tryFadeOut()
                }
            } catch (e: Exception) {
                android.util.Log.e("SplashActivity", "初始化失败", e)
                isAllLoaded = true
                tryFadeOut()
            }
        }
    }

    private fun tryFadeOut() {
        if (isSignatureInvalid && isAnimationFinished && !isSecurityWarningShown) {
            showSecurityWarning()
            return
        }

        if (isAllLoaded && isAnimationFinished) {
            startFadeOut()
        }
    }

    private fun showSecurityWarning() {
        isSecurityWarningShown = true

        val fadeOutLottie = ObjectAnimator.ofFloat(lottieAnimationView, "alpha", 1f, 0f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    lottieAnimationView.visibility = View.GONE
                }
            })
        }

        val fadeInWarning = ObjectAnimator.ofFloat(securityWarningView, "alpha", 0f, 1f).apply {
            startDelay = 300
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    securityWarningView.visibility = View.VISIBLE
                }
            })
        }

        fadeOutLottie.start()
        fadeInWarning.start()
    }

    private fun startFadeOut() {
        onReadyCallback?.invoke()

        splashContainer.postDelayed({
            val alpha = ObjectAnimator.ofFloat(splashContainer, "alpha", 1f, 0f)

            alpha.duration = 500
            alpha.interpolator = AccelerateDecelerateInterpolator()

            alpha.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finish()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0)
                    }
                }
            })

            alpha.start()
        }, 150)
    }

    override fun onDestroy() {
        super.onDestroy()
        lottieAnimationView.cancelAnimation()
    }

    companion object {
        var onReadyCallback: (() -> Unit)? = null
    }
}
