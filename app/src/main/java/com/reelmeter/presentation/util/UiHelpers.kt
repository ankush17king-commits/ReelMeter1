package com.reelmeter.presentation.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import com.reelmeter.presentation.theme.StatusColors

/** green < 60%, orange < 100%, red >= 100% */
fun statusColor(total: Int, limit: Int): Color {
    val f = if (limit > 0) total.toFloat() / limit else 0f
    return when {
        f < 0.6f -> StatusColors.Green
        f < 1f -> StatusColors.Orange
        else -> StatusColors.Red
    }
}

fun progressMessage(total: Int, limit: Int): String {
    val f = if (limit > 0) total.toFloat() / limit else 0f
    return when {
        total == 0 -> "🌱 Clean slate today! Keep your focus high."
        f < 0.4f -> "⚡ Great momentum! You're well under your limit."
        f < 0.8f -> "🎯 Balanced usage. Stay mindful of your time."
        f < 1f -> "⚠️ Nearing your limit! Consider wrapping up soon."
        else -> "🛑 Daily limit reached! Time to recharge off-screen."
    }
}

object SystemIntents {
    private fun start(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun openAccessibilitySettings(context: Context) =
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openOverlaySettings(context: Context) =
        start(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))

    fun openNotificationSettings(context: Context) =
        start(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))

    fun openAppInfo(context: Context) =
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
}

object PrivacyText {
    const val DISCLOSURE =
        "ReelMeter uses Android's Accessibility Service only to detect when you swipe to a new short video " +
            "(Reel / Short) in Instagram, YouTube and Facebook, so it can count them.\n\n" +
            "• It does NOT read, record, store or share any text, messages, passwords, photos or other content on your screen.\n" +
            "• It only saves: the app name, a timestamp and a count.\n" +
            "• Everything stays on your phone. ReelMeter has no internet access."
}
