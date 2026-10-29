package com.reelmeter.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.reelmeter.R
import com.reelmeter.core.TimeProvider
import com.reelmeter.data.local.AppDatabase
import com.reelmeter.domain.model.Limits
import com.reelmeter.presentation.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReelMeterWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun db(): AppDatabase
        fun timeProvider(): TimeProvider
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(ComponentName(context, ReelMeterWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids)
            }
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = runCatching { goAsync() }.getOrNull()
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                )
                val timeProvider = entryPoint.timeProvider()
                val db = entryPoint.db()
                val todayKey = timeProvider.today().toString()

                val dailyStat = db.dailyStatDao().get(todayKey)
                val settings = db.settingsDao().get()

                val dailyLimit = settings?.dailyLimit ?: Limits.DEFAULT_DAILY_LIMIT
                val igCount = dailyStat?.instagramCount ?: 0
                val ytCount = dailyStat?.youtubeCount ?: 0
                val fbCount = dailyStat?.facebookCount ?: 0
                val scCount = dailyStat?.snapchatCount ?: 0
                val totalCount = igCount + ytCount + fbCount + scCount

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val robotEmoji = com.reelmeter.presentation.components.getRobotStageEmoji(totalCount, dailyLimit)
                val fraction = if (dailyLimit > 0) totalCount.toFloat() / dailyLimit else 0f
                val stageNum = when {
                    fraction < 0.25f -> 1
                    fraction < 0.50f -> 2
                    fraction < 0.75f -> 3
                    fraction < 1.00f -> 4
                    else -> 5
                }
                val resName = "robot$stageNum"
                val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_reel_meter).apply {
                        setTextViewText(R.id.widget_total_reels, "$totalCount Reels")
                        setTextViewText(R.id.widget_limit_subtitle, "Daily limit: $dailyLimit")

                        if (resId != 0) {
                            setViewVisibility(R.id.widget_robot_image, android.view.View.VISIBLE)
                            setViewVisibility(R.id.widget_robot_mascot, android.view.View.GONE)
                            setImageViewResource(R.id.widget_robot_image, resId)
                        } else {
                            setViewVisibility(R.id.widget_robot_image, android.view.View.GONE)
                            setViewVisibility(R.id.widget_robot_mascot, android.view.View.VISIBLE)
                            setTextViewText(R.id.widget_robot_mascot, robotEmoji)
                        }

                        setProgressBar(R.id.widget_progress_bar, dailyLimit, totalCount, false)
                        setTextViewText(
                            R.id.widget_breakdown,
                            "IG: $igCount  •  YT: $ytCount  •  FB: $fbCount  •  SC: $scCount"
                        )
                        setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                        setOnClickPendingIntent(R.id.widget_total_reels, pendingIntent)
                        setOnClickPendingIntent(R.id.widget_robot_mascot, pendingIntent)
                        setOnClickPendingIntent(R.id.widget_robot_image, pendingIntent)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                runCatching { pendingResult?.finish() }
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, ReelMeterWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, ReelMeterWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
