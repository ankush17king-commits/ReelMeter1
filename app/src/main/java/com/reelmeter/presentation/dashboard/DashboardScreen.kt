package com.reelmeter.presentation.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelmeter.domain.logic.Formatters
import com.reelmeter.domain.model.DashboardData
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.presentation.components.AppCard
import com.reelmeter.presentation.components.EmptyState
import com.reelmeter.presentation.components.ErrorState
import com.reelmeter.presentation.components.LoadingState
import com.reelmeter.presentation.components.PermissionFixCard
import com.reelmeter.presentation.components.ProgressRing
import com.reelmeter.presentation.components.SectionTitle
import com.reelmeter.presentation.components.WeeklyBarChart
import com.reelmeter.presentation.theme.PremiumColors
import com.reelmeter.presentation.util.progressMessage
import com.reelmeter.presentation.util.statusColor
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusState by viewModel.focusTimerManager.timerState.collectAsStateWithLifecycle()

    // Re-check permissions every time the user comes back from system settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

    when (val s = state) {
        DashboardUiState.Loading -> LoadingState()
        is DashboardUiState.Error -> ErrorState(s.message, viewModel::retry)
        is DashboardUiState.Success -> DashboardContent(s, focusState, viewModel)
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    focusState: com.reelmeter.domain.logic.FocusTimerState,
    viewModel: DashboardViewModel
) {
    val data = state.data
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(Modifier.fillMaxSize()) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme() || (MaterialTheme.colorScheme.background.red * 0.299 + MaterialTheme.colorScheme.background.green * 0.587 + MaterialTheme.colorScheme.background.blue * 0.114) < 0.5f
        val bgBase = if (isDark) "bg_dashboard" else "bg_dashboard1"
        var bgResId = context.resources.getIdentifier(bgBase, "drawable", context.packageName)
        if (bgResId == 0) {
            bgResId = context.resources.getIdentifier("bg_dashboard", "drawable", context.packageName)
        }

        if (bgResId != 0) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = bgResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
        // Premium Header with personalized greeting & top-right streak badge
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Hey ${data.userName.ifBlank { "there" }} 👋",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("ReelMeter", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    data.today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Top-right compact streak badge
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PremiumColors.GlowAmber.copy(alpha = 0.2f), Color(0xFFFF8C00).copy(alpha = 0.2f))
                        )
                    )
                    .border(1.dp, PremiumColors.GlowAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${data.streak}d",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PremiumColors.GlowAmber
                    )
                }
            }
        }

        if (!state.permissions.allGranted) PermissionFixCard(state.permissions)

        // Main Robot-Centered Counter Card (Applied universally across Light & Dark modes)
        ExperimentalCounterCard(data, state.settings)

        // By app section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .width(32.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(PremiumColors.GradientStart, PremiumColors.GradientEnd)
                        )
                    )
            )
            SectionTitle("By app")
        }
        data.trackedApps.forEach { app ->
            AppUsageCard(app, data.perApp[app] ?: 0, data.total, true, data.secondsPerReel)
        }

        // Focus Timer moved to the VERY BOTTOM of the home screen
        FocusTimerCard(
            focusState = focusState,
            onStart = { mins -> viewModel.focusTimerManager.startTimer(context, mins) },
            onCancel = { viewModel.focusTimerManager.cancelTimer() },
            onStartUnlock = { viewModel.focusTimerManager.startUnlocking() }
        )

        Spacer(Modifier.height(8.dp))
    }
}
}

private fun motivationalMicroCopy(total: Int, limit: Int): String {
    val f = if (limit > 0) total.toFloat() / limit else 0f
    return when {
        total == 0 -> "✨ A fresh start awaits"
        f < 0.3f -> "🌟 You're doing amazing"
        f < 0.6f -> "💪 Stay strong, you got this"
        f < 0.8f -> "🧘 Time for a mindful pause"
        f < 1f -> "⚡ Almost there, stay focused"
        else -> "🛑 Take a deep breath"
    }
}

@Composable
private fun StreakCard(streak: Int) {
    val hasGoldenGlow = streak > 3
    val cardModifier = if (hasGoldenGlow) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    PremiumColors.GlowAmber.copy(alpha = 0.5f),
                    Color(0xFFFF8C00).copy(alpha = 0.3f),
                    PremiumColors.GlowAmber.copy(alpha = 0.5f)
                )
            ),
            shape = RoundedCornerShape(28.dp)
        )
    } else Modifier

    AppCard(modifier = cardModifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Gradient background icon with fire emoji
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PremiumColors.GlowAmber, Color(0xFFFF8C00))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$streak", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (streak > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("🔥", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text(
                    when (streak) {
                        0 -> "Stay under your limit today to start a streak"
                        1 -> "1 day under your limit"
                        else -> "$streak days under your limit"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AppUsageCard(app: TrackedApp, count: Int, total: Int, tracked: Boolean, secondsPerReel: Int) {
    // App-specific gradient colors
    val appGradient = when (app.label) {
        "Instagram" -> listOf(Color(0xFFE1306C), Color(0xFF833AB4))
        "YouTube" -> listOf(Color(0xFFFF0000), Color(0xFFCC0000))
        "Facebook" -> listOf(Color(0xFF1877F2), Color(0xFF0D5AB9))
        "Snapchat" -> listOf(Color(0xFFFFFC00), Color(0xFFFFD000))
        else -> listOf(MaterialTheme.colorScheme.primary, PremiumColors.GlowPurple)
    }
    val avatarTextColor = if (app.label == "Snapchat") Color.Black else Color.White

    val context = androidx.compose.ui.platform.LocalContext.current
    val appLogoName = app.label.lowercase()
    val logoResId = context.resources.getIdentifier(appLogoName, "drawable", context.packageName)

    AppCard(Modifier.alpha(if (tracked) 1f else 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (logoResId != 0) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = logoResId),
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                // Gradient letter avatar fallback
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(appGradient)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        app.label.first().toString(),
                        fontWeight = FontWeight.Bold,
                        color = avatarTextColor
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold)
                Text(
                    if (tracked) "${Formatters.duration(count.toLong() * secondsPerReel)} today" else "Not tracked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Larger bolder reel count
            Text(
                "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (tracked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (tracked) {
            Spacer(Modifier.height(12.dp))
            // Progress bar with shimmer glow
            Box(contentAlignment = Alignment.CenterStart) {
                LinearProgressIndicator(
                    progress = { if (total > 0) count.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = appGradient.first(),
                )
                // Glow overlay on progress
                val progressFraction = if (total > 0) count.toFloat() / total else 0f
                Box(
                    Modifier
                        .fillMaxWidth(progressFraction)
                        .height(8.dp)
                        .clip(CircleShape)
                        .graphicsLayer { alpha = 0.3f }
                        .blur(4.dp)
                        .background(appGradient.first())
                )
            }
        }
    }
}

@Composable
private fun FocusTimerCard(
    focusState: com.reelmeter.domain.logic.FocusTimerState,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onStartUnlock: () -> Unit,
) {
    var selectedMins by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(30) }
    val presets = listOf(15, 30, 45, 60)

    // Focus timer card with gradient overlay
    AppCard {
        Box(
            Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = PremiumColors.GlowCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Focus Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))

                when (focusState) {
                    is com.reelmeter.domain.logic.FocusTimerState.Idle -> {
                        Text("Block reels & stay focused during study or work.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            presets.forEach { mins ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedMins == mins,
                                    onClick = { selectedMins = mins },
                                    label = { Text("${mins}m") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        com.reelmeter.presentation.components.PremiumButton(
                            text = "Start $selectedMins min Focus",
                            onClick = { onStart(selectedMins) }
                        )
                    }
                    is com.reelmeter.domain.logic.FocusTimerState.Running -> {
                        var showFullScreenTimer by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        val remainingSecs = (focusState.remainingMs(System.currentTimeMillis()) / 1000).toInt()
                        val mins = remainingSecs / 60
                        val secs = remainingSecs % 60
                        val formattedTime = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

                        Column(
                            Modifier.fillMaxWidth().clickable { showFullScreenTimer = true },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Focus Session Active (Tap for Full Screen)", style = MaterialTheme.typography.titleSmall, color = PremiumColors.GlowCyan)
                            Spacer(Modifier.height(8.dp))

                            // Glowing timer digits
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    formattedTime,
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumColors.GlowCyan,
                                    modifier = Modifier
                                        .graphicsLayer { alpha = 0.3f }
                                        .blur(12.dp)
                                )
                                Text(
                                    formattedTime,
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("End Focus Early")
                            }
                        }

                        if (showFullScreenTimer) {
                            androidx.compose.ui.window.Dialog(
                                onDismissRequest = { showFullScreenTimer = false },
                                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0A0A14))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "🧘 Focus Mode Active",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PremiumColors.GlowCyan
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Video apps are currently blocked. Stay focused on your goal!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(Modifier.height(48.dp))

                                        // Giant glowing digits
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                formattedTime,
                                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 90.sp),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PremiumColors.GlowCyan,
                                                modifier = Modifier
                                                    .graphicsLayer { alpha = 0.35f }
                                                    .blur(24.dp)
                                            )
                                            Text(
                                                formattedTime,
                                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 90.sp),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }

                                        Spacer(Modifier.height(48.dp))

                                        com.reelmeter.presentation.components.PremiumButton(
                                            text = "Minimize",
                                            onClick = { showFullScreenTimer = false }
                                        )

                                        Spacer(Modifier.height(12.dp))

                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { showFullScreenTimer = false; onCancel() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("End Focus Early", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is com.reelmeter.domain.logic.FocusTimerState.Locked -> {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔒 Apps Locked", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Focus timer ended. Video apps are blocked.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            com.reelmeter.presentation.components.PremiumButton(
                                text = "Unlock (60s Breathing)",
                                onClick = onStartUnlock
                            )
                        }
                    }
                    is com.reelmeter.domain.logic.FocusTimerState.Unlocking -> {
                        val remainingSecs = focusState.remainingSeconds(System.currentTimeMillis())
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Breathe in… Breathe out…", style = MaterialTheme.typography.titleMedium, color = PremiumColors.GlowCyan)
                            Spacer(Modifier.height(8.dp))
                            Text("${remainingSecs}s", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Unlocking in progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperimentalCounterCard(data: DashboardData, settings: com.reelmeter.domain.model.UserSettings) {
    val color = statusColor(data.total, data.limit)
    val context = androidx.compose.ui.platform.LocalContext.current
    val stageEmoji = com.reelmeter.presentation.components.getRobotStageEmoji(data.total, data.limit)

    val fraction = if (data.limit > 0) data.total.toFloat() / data.limit else 0f
    val stageNum = when {
        fraction < 0.25f -> 1
        fraction < 0.50f -> 2
        fraction < 0.75f -> 3
        fraction < 1.00f -> 4
        else -> 5
    }
    val resName = "robot$stageNum"
    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)



    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Reel count at top (e.g. "60 Reels")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${data.total} Reels",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Daily limit: ${data.limit} reels",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            // Subtle linear progress bar
            LinearProgressIndicator(
                progress = { (data.total.toFloat() / data.limit.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // 2. Larger Centered Robot Mascot (Directly on wallpaper, no card or box background)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(170.dp)
        ) {
            if (resId != 0) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = resId),
                    contentDescription = "Robot Mascot",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else {
                Text(stageEmoji, fontSize = 85.sp)
            }
        }

        // 3. Dynamic Status Message in Thought/Speech Bubble
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomEnd = 18.dp,
                        bottomStart = 2.dp // Thought bubble tail
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                com.reelmeter.domain.logic.RobotPersonalizationEngine.generateMascotMessage(settings, data.total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (data.total >= data.limit) PremiumColors.GlowRed else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Time Spent Summary
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                "About ${Formatters.duration(data.secondsLost)} spent on reels today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
