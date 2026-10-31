package com.reelmeter.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.CountDownTimer
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams as LP
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.animation.doOnRepeat
import com.reelmeter.domain.model.Limits

data class OverlayConfig(
    val total: Int,
    val limit: Int,
    val strictMode: Boolean,
    val extensionsLeft: Int,
    val onCloseApp: () -> Unit,
    val onExtend: () -> Unit,
    val onDone: () -> Unit,
)

/**
 * Full-screen "take a break" overlay built with plain Views (no Compose lifecycle needed in a Service).
 * Uses TYPE_APPLICATION_OVERLAY when SYSTEM_ALERT_WINDOW is granted, otherwise falls back to the
 * accessibility overlay window type, so the intervention still works if the permission is missing.
 */
class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: View? = null
    private var timer: CountDownTimer? = null
    private var breathing: ValueAnimator? = null

    val isShowing: Boolean get() = root != null

    fun show(config: OverlayConfig) {
        if (root != null) return
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        var locked = config.strictMode

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xF2101018.toInt())
            setPadding(dp(28), dp(48), dp(28), dp(48))
            isFocusableInTouchMode = true
        }

        fun label(text: String, sp: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
            this.text = text
            textSize = sp
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val title = label("Daily limit reached!\nApp closed automatically.", 26f, Color.WHITE, bold = true)
        val subtitle = label("${config.total} / ${config.limit} reels today", 16f, 0xFFB8B8C8.toInt())
        val breatheText = label("Breathe in…", 18f, 0xFFB9A8FF.toInt())
        val circle = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x667C5CFF)
                setStroke(dp(3), 0xFF7C5CFF.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(150), dp(150)).apply {
                topMargin = dp(32); bottomMargin = dp(12)
            }
            scaleX = 0.7f; scaleY = 0.7f
        }
        val countdown = label(
            if (locked) "You can continue in ${Limits.STRICT_COUNTDOWN_SECONDS}s" else "",
            14f, 0xFFB8B8C8.toInt(),
        )

        fun pill(text: String, bg: Int, fg: Int) = Button(context).apply {
            this.text = text
            isAllCaps = false
            textSize = 16f
            setTextColor(fg)
            background = GradientDrawable().apply { cornerRadius = dp(28).toFloat(); setColor(bg) }
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(56)).apply { topMargin = dp(12) }
        }

        val closeButton = pill("Close app", 0xFF7C5CFF.toInt(), Color.WHITE)
        val extendText = if (config.extensionsLeft > 0) "+5 more minutes (${config.extensionsLeft} left today)" else "No extra time left today"
        val extendButton = pill(extendText, 0xFF2A2A3A.toInt(), Color.WHITE)
        val doneButton = pill("I'm done", 0x00000000, 0xFFB8B8C8.toInt())

        val buttons = listOf(closeButton, extendButton, doneButton)
        fun applyLock() {
            buttons.forEach { it.isEnabled = !locked; it.alpha = if (locked) 0.35f else 1f }
            if (!locked && config.extensionsLeft <= 0) { extendButton.isEnabled = false; extendButton.alpha = 0.35f }
        }
        applyLock()

        closeButton.setOnClickListener { config.onCloseApp() }
        extendButton.setOnClickListener { config.onExtend() }
        doneButton.setOnClickListener { config.onDone() }

        listOf(title, subtitle, circle, breatheText, countdown, closeButton, extendButton, doneButton)
            .forEach { container.addView(it) }

        // Breathing animation: 4s in, 4s out
        var inhale = true
        breathing = ValueAnimator.ofFloat(0.7f, 1f).apply {
            duration = 4_000
            interpolator = LinearInterpolator()
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { circle.scaleX = it.animatedValue as Float; circle.scaleY = it.animatedValue as Float }
            doOnRepeat { inhale = !inhale; breatheText.text = if (inhale) "Breathe in…" else "Breathe out…" }
            start()
        }

        if (locked) {
            timer = object : CountDownTimer(Limits.STRICT_COUNTDOWN_SECONDS * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    countdown.text = "You can continue in ${(millisUntilFinished + 999) / 1000}s"
                }
                override fun onFinish() {
                    locked = false
                    countdown.text = "You can continue now"
                    applyLock()
                }
            }.start()
        }

        container.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (!locked) config.onCloseApp()
                true
            } else false
        }

        val type = if (Settings.canDrawOverlays(context)) LP.TYPE_APPLICATION_OVERLAY else LP.TYPE_ACCESSIBILITY_OVERLAY
        val params = LP(MATCH_PARENT, MATCH_PARENT, type, LP.FLAG_LAYOUT_IN_SCREEN or LP.FLAG_KEEP_SCREEN_ON, PixelFormat.TRANSLUCENT)
        try {
            windowManager.addView(container, params)
            root = container
            container.requestFocus()
        } catch (e: Exception) {
            timer?.cancel(); breathing?.cancel()
        }
    }

    fun dismiss() {
        timer?.cancel(); timer = null
        breathing?.cancel(); breathing = null
        val viewToDismiss = root
        root = null
        if (viewToDismiss != null) {
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                runCatching { windowManager.removeView(viewToDismiss) }
            }
        }
    }
}
