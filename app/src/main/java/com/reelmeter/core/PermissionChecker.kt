package com.reelmeter.core

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.reelmeter.service.ReelAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionStatus(
    val accessibility: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
) {
    val allGranted: Boolean get() = accessibility && overlay && notifications
    /** Counting only works with the accessibility service. */
    val countingActive: Boolean get() = accessibility
}

@Singleton
class PermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun current() = PermissionStatus(
        accessibility = isAccessibilityEnabled(),
        overlay = Settings.canDrawOverlays(context),
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
    )

    private fun isAccessibilityEnabled(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val s = it.resolveInfo.serviceInfo
                s.packageName == context.packageName && s.name == ReelAccessibilityService::class.java.name
            }
    }
}
