package com.reelmeter.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowManager.LayoutParams as LP
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Floating on-screen counter badge showing the number of reels/shorts watched today
 * on top of running target apps (Instagram, YouTube, Facebook).
 */
class FloatingCounterController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: View? = null
    private var robotImageView: android.widget.ImageView? = null
    private var robotTextView: TextView? = null
    private var countTextView: TextView? = null

    val isShowing: Boolean get() = root != null

    private var lastTotal: Int = -1

    fun showOrUpdate(total: Int, limit: Int) {
        // Concurrency Guard: Never allow a stale lower count to overwrite a higher current count
        if (root != null && total < lastTotal && lastTotal > 0) {
            return
        }
        lastTotal = total

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            if (root != null) {
                updateText(total, limit)
                return@post
            }

            val density = context.resources.displayMetrics.density
            fun dp(v: Int) = (v * density).toInt()

            val isOverLimit = total >= limit

            val pillBg = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(0xF0141420.toInt()) // Deep translucent background
                setStroke(
                    dp(1),
                    if (isOverLimit) 0xFFFF5252.toInt() else 0xFF7C5CFF.toInt()
                )
            }

            // Vertical layout: Robot image on top, Reel count number below
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = pillBg
                setPadding(dp(14), dp(8), dp(14), dp(8))
            }

            val imgView = android.widget.ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = dp(2)
                }
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
            robotImageView = imgView

            val txtRobot = TextView(context).apply {
                textSize = 20f
                gravity = Gravity.CENTER
            }
            robotTextView = txtRobot

            val txtCount = TextView(context).apply {
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            countTextView = txtCount

            container.addView(imgView)
            container.addView(txtRobot)
            container.addView(txtCount)

            updateText(total, limit)

            val type = if (Settings.canDrawOverlays(context)) {
                LP.TYPE_APPLICATION_OVERLAY
            } else {
                LP.TYPE_ACCESSIBILITY_OVERLAY
            }

            val params = LP(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                type,
                LP.FLAG_NOT_FOCUSABLE or LP.FLAG_LAYOUT_IN_SCREEN or LP.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp(20)
            }

            try {
                windowManager.addView(container, params)
                root = container
            } catch (e: Exception) {
                root = null
                robotImageView = null
                robotTextView = null
                countTextView = null
            }
        }
    }

    private fun updateText(total: Int, limit: Int) {
        val isOverLimit = total >= limit
        val fraction = if (limit > 0) total.toFloat() / limit else 0f
        val stageNum = when {
            fraction < 0.25f -> 1
            fraction < 0.50f -> 2
            fraction < 0.75f -> 3
            fraction < 1.00f -> 4
            else -> 5
        }
        val resName = "robot$stageNum"
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        val robotEmoji = com.reelmeter.presentation.components.getRobotStageEmoji(total, limit)

        if (resId != 0) {
            robotImageView?.visibility = View.VISIBLE
            robotImageView?.setImageResource(resId)
            robotTextView?.visibility = View.GONE
        } else {
            robotImageView?.visibility = View.GONE
            robotTextView?.visibility = View.VISIBLE
            robotTextView?.text = robotEmoji
        }

        countTextView?.text = "$total"
        countTextView?.setTextColor(if (isOverLimit) 0xFFFF6B6B.toInt() else Color.WHITE)
    }

    fun dismiss() {
        val viewToDismiss = root
        root = null
        robotImageView = null
        robotTextView = null
        countTextView = null

        if (viewToDismiss != null) {
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                runCatching { windowManager.removeView(viewToDismiss) }
            }
        }
    }
}
