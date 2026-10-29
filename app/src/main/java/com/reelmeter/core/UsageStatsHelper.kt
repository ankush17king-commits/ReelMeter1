package com.reelmeter.core

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.reelmeter.domain.model.TrackedApp
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsHelper @Inject constructor() {

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Returns total foreground usage time in minutes for each tracked app for today. */
    fun getTodayAppUsageMinutes(context: Context): Map<TrackedApp, Long> {
        if (!hasUsagePermission(context)) return emptyMap()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyMap()

        val map = mutableMapOf<TrackedApp, Long>()
        for (stat in stats) {
            val app = TrackedApp.fromPackage(stat.packageName)
            if (app != null) {
                val existing = map[app] ?: 0L
                map[app] = existing + (stat.totalTimeInForeground / 60_000L)
            }
        }
        return map
    }
}
