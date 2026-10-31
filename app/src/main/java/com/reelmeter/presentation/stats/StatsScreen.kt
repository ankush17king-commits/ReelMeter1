package com.reelmeter.presentation.stats

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelmeter.domain.logic.Formatters
import com.reelmeter.domain.model.AdvancedStatsData
import com.reelmeter.domain.model.AppAnalyticsSummary
import com.reelmeter.domain.model.DayBar
import com.reelmeter.domain.model.StatsPeriod
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.presentation.components.AppCard
import com.reelmeter.presentation.components.EmptyState
import com.reelmeter.presentation.components.ErrorState
import com.reelmeter.presentation.components.LoadingState
import com.reelmeter.presentation.components.SectionTitle
import com.reelmeter.presentation.theme.PremiumColors
import com.reelmeter.presentation.util.statusColor
import java.io.File
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val s = state) {
        StatsUiState.Loading -> LoadingState()
        is StatsUiState.Error -> ErrorState(s.message, viewModel::retry)
        is StatsUiState.Success -> StatsContent(s.data, viewModel::selectPeriod)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsContent(data: AdvancedStatsData, onSelectPeriod: (StatsPeriod) -> Unit) {
    val context = LocalContext.current
    var selectedBar by remember { mutableStateOf<DayBar?>(null) }

    Box(Modifier.fillMaxSize()) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme() || (MaterialTheme.colorScheme.background.red * 0.299 + MaterialTheme.colorScheme.background.green * 0.587 + MaterialTheme.colorScheme.background.blue * 0.114) < 0.5f
        val bgBase = if (isDark) "bg_stats" else "bg_stats1"
        var bgResId = context.resources.getIdentifier(bgBase, "drawable", context.packageName)
        if (bgResId == 0) {
            bgResId = context.resources.getIdentifier("bg_stats", "drawable", context.packageName)
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
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row with Title & Export CSV Button
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Stats & Analytics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Track your reduction progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { exportCsv(context, data) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Export CSV", tint = PremiumColors.GlowCyan)
                }
            }

            // Period Selector (Today / 7 Days / 30 Days)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                StatsPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = data.selectedPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(index, StatsPeriod.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = PremiumColors.GlowCyan.copy(alpha = 0.15f),
                            activeBorderColor = PremiumColors.GlowCyan.copy(alpha = 0.5f),
                        ),
                    ) { Text(period.label) }
                }
            }

            // Overview Summary Cards
            OverviewSection(data)

            // Interactive Usage Chart with Tap Inspection / Hour-wise / Line Graph
            PremiumSectionTitle("${data.selectedPeriod.label} Usage Chart")
            AppCard {
                when (data.selectedPeriod) {
                    StatsPeriod.TODAY -> {
                        TodayHourlyChart(data.todayHourlyBars)
                    }
                    StatsPeriod.THIRTY_DAYS -> {
                        ThirtyDayLineChart(data.chartBars, data.dailyLimit)
                    }
                    StatsPeriod.SEVEN_DAYS -> {
                        if (data.chartBars.any { it.total > 0 }) {
                            InteractiveChart(data.chartBars, data.dailyLimit, selectedBar) { selectedBar = it }
                            
                            AnimatedVisibility(selectedBar != null) {
                                selectedBar?.let { bar ->
                                    Spacer(Modifier.height(12.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PremiumColors.GlowCyan.copy(alpha = 0.08f))
                                            .border(1.dp, PremiumColors.GlowCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(
                                                    bar.day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    if (bar.total >= bar.limit) "Over limit by ${bar.total - bar.limit}" else "Under limit of ${bar.limit}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (bar.total >= bar.limit) PremiumColors.GlowRed else PremiumColors.GlowMint
                                                )
                                            }
                                            Text(
                                                "${bar.total} reels",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PremiumColors.GlowCyan
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            EmptyState(Icons.Rounded.BarChart, "No usage recorded", "Your reel counts for ${data.selectedPeriod.label.lowercase()} will appear here.")
                        }
                    }
                }
            }

            // Reduction & Time Saved Section
            PremiumSectionTitle("Time Saved & Reduction")
            ReductionProgressCard(data)

            // App-wise Analytics Breakdown
            PremiumSectionTitle("App-wise Analytics")
            AppAnalyticsCard(data.appAnalytics, data.secondsPerReel)

            // Shareable Weekly Progress Certificate
            PremiumSectionTitle("Weekly Progress Certificate")
            com.reelmeter.presentation.components.WeeklyCertificateCard(data)

            // Advanced Stats Option at Bottom
            PremiumSectionTitle("Advanced Stats")
            AdvancedStatsExpandableCard(data)

            // Insights & Insights Cards
            if (data.insights.isNotEmpty()) {
                PremiumSectionTitle("Smart Insights")
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.insights.forEach { message ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, null, tint = PremiumColors.GlowCyan, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(message, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Privacy Guarantee Notice
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = PremiumColors.GlowMint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("100% Privacy Preserved", fontWeight = FontWeight.Bold)
                        Text(
                            "ReelMeter only counts scroll deltas. No screen content, videos, or messages are ever read or recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OverviewSection(data: AdvancedStatsData) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Total Reels", "${data.totalReelsPeriod}", PremiumColors.GlowCyan, Modifier.weight(1f))
                StatTile("Daily Average", String.format("%.1f", data.dailyAverageReels), PremiumColors.GlowPurple, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val bestText = data.bestDay?.let { "${it.total} (${it.day.format(DateTimeFormatter.ofPattern("EEE"))})" } ?: "N/A"
                val peakText = data.peakDay?.let { "${it.total} (${it.day.format(DateTimeFormatter.ofPattern("EEE"))})" } ?: "N/A"
                StatTile("Best Day", bestText, PremiumColors.GlowMint, Modifier.weight(1f))
                StatTile("Peak Day", peakText, PremiumColors.GlowRed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        Column {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accentColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InteractiveChart(
    bars: List<DayBar>,
    limit: Int,
    selectedBar: DayBar?,
    onSelectBar: (DayBar) -> Unit
) {
    val maxBar = (bars.maxOfOrNull { it.total } ?: 1).coerceAtLeast(limit).coerceAtLeast(1)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(160.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bar ->
                val heightFraction = bar.total.toFloat() / maxBar
                val isSelected = selectedBar?.day == bar.day
                val barColor = if (bar.total >= limit) PremiumColors.GlowRed else PremiumColors.GlowCyan

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectBar(bar) }
                ) {
                    Text(
                        "${bar.total}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) PremiumColors.GlowCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(if (isSelected) 18.dp else 12.dp)
                            .fillMaxWidth(0.6f)
                            .height((120 * heightFraction).dp.coerceAtLeast(6.dp))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(listOf(barColor, PremiumColors.GlowPurple))
                                } else {
                                    Brush.verticalGradient(listOf(barColor.copy(alpha = 0.7f), barColor.copy(alpha = 0.3f)))
                                }
                            )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        bar.day.format(DateTimeFormatter.ofPattern("EEE")),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PremiumColors.GlowCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Dashed line = daily limit ($limit reels)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ReductionProgressCard(data: AdvancedStatsData) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val change = data.percentageChange
            if (change != null) {
                val isImproved = change > 0
                val icon = if (isImproved) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp
                val color = if (isImproved) PremiumColors.GlowMint else PremiumColors.GlowRed
                val textLabel = if (isImproved) "${change.toInt()}% reduction in reels" else "${kotlin.math.abs(change.toInt())}% increase in reels"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(textLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                        Text("Compared to preceding ${data.selectedPeriod.daysCount} days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text("Start tracking daily to see reduction trends!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("${data.reelsAvoided}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PremiumColors.GlowMint)
                        Text("Reels Avoided", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(Formatters.duration(data.estimatedTimeSavedMinutes * 60L), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PremiumColors.GlowCyan)
                        Text("Estimated Time Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppAnalyticsCard(analytics: List<AppAnalyticsSummary>, secondsPerReel: Int) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            analytics.forEach { item ->
                val appGradient = when (item.app.label) {
                    "Instagram" -> listOf(Color(0xFFE1306C), Color(0xFF833AB4))
                    "YouTube" -> listOf(Color(0xFFFF0000), Color(0xFFCC0000))
                    "Facebook" -> listOf(Color(0xFF1877F2), Color(0xFF0D5AB9))
                    "Snapchat" -> listOf(Color(0xFFFFFC00), Color(0xFFFFD000))
                    else -> listOf(MaterialTheme.colorScheme.primary, PremiumColors.GlowPurple)
                }

                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.app.label, fontWeight = FontWeight.SemiBold)
                        Text("${item.count} reels (${item.percentageShare.toInt()}%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (item.percentageShare / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = appGradient.first(),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumSectionTitle(text: String) {
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
        SectionTitle(text)
    }
}

private fun exportCsv(context: android.content.Context, data: AdvancedStatsData) {
    val csvHeader = "Date,Total Reels,Limit\n"
    val csvRows = data.chartBars.joinToString("\n") { "${it.day},${it.total},${it.limit}" }
    val csvContent = csvHeader + csvRows

    val file = File(context.cacheDir, "ReelMeter_Stats.csv")
    file.writeText(csvContent)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Stats CSV"))
}

@Composable
private fun AdvancedStatsExpandableCard(data: AdvancedStatsData) {
    var expanded by remember { mutableStateOf(false) }

    AppCard {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BarChart, null, tint = PremiumColors.GlowCyan)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Advanced Stats & Historical Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Complete daily history & monthly records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(if (expanded) "▲" else "▼", fontWeight = FontWeight.Bold, color = PremiumColors.GlowCyan)
            }

            AnimatedVisibility(expanded) {
                Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Canvas Pie Chart
                    AppUsagePieChart(data.appAnalytics)

                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )

                    Text("Daily History (${data.days.size} days recorded)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    data.days.forEach { dayStat ->
                        DayHistoryRow(dayStat, data.secondsPerReel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsagePieChart(appAnalytics: List<com.reelmeter.domain.model.AppAnalyticsSummary>) {
    val totalCount = appAnalytics.sumOf { it.count }
    val slices = appAnalytics.filter { it.count > 0 }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("App Usage Share", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        if (totalCount > 0 && slices.isNotEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
                    var startAngle = -90f
                    slices.forEach { item ->
                        val sweepAngle = (item.count.toFloat() / totalCount) * 360f
                        val sliceColor = when (item.app.label) {
                            "Instagram" -> Color(0xFFE1306C)
                            "YouTube" -> Color(0xFFFF0000)
                            "Facebook" -> Color(0xFF1877F2)
                            "Snapchat" -> Color(0xFFFFFC00)
                            else -> PremiumColors.GlowCyan
                        }
                        drawArc(
                            color = sliceColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = size
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Legend
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.forEach { item ->
                    val dotColor = when (item.app.label) {
                        "Instagram" -> Color(0xFFE1306C)
                        "YouTube" -> Color(0xFFFF0000)
                        "Facebook" -> Color(0xFF1877F2)
                        "Snapchat" -> Color(0xFFFFFC00)
                        else -> PremiumColors.GlowCyan
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(item.app.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        Text("${item.count} reels (${item.percentageShare.toInt()}%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Text("No app usage recorded for pie chart display.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayHourlyChart(bars: List<DayBar>) {
    val maxBar = (bars.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { hourIndex, bar ->
                val heightFraction = bar.total.toFloat() / maxBar
                val barColor = PremiumColors.GlowCyan

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (bar.total > 0) {
                        Text(
                            "${bar.total}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = PremiumColors.GlowCyan
                        )
                    }
                    Box(
                        Modifier
                            .width(6.dp)
                            .height((100 * heightFraction).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (bar.total > 0) Brush.verticalGradient(listOf(barColor, PremiumColors.GlowPurple))
                                else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("00:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("06:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("12:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("18:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("23:00", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThirtyDayLineChart(
    bars: List<DayBar>,
    limit: Int
) {
    val maxBar = (bars.maxOfOrNull { it.total } ?: 1).coerceAtLeast(limit).coerceAtLeast(1)

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 8.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val stepX = width / (bars.size - 1).coerceAtLeast(1)

                val points = bars.mapIndexed { index, bar ->
                    val x = index * stepX
                    val y = height - (bar.total.toFloat() / maxBar) * height
                    androidx.compose.ui.geometry.Offset(x, y)
                }

                // Draw limit line
                val limitY = height - (limit.toFloat() / maxBar) * height
                drawLine(
                    color = PremiumColors.GlowRed.copy(alpha = 0.5f),
                    start = androidx.compose.ui.geometry.Offset(0f, limitY),
                    end = androidx.compose.ui.geometry.Offset(width, limitY),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                // Build Line Path & Area Gradient
                if (points.isNotEmpty()) {
                    val linePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(points.first().x, height)
                        for (p in points) {
                            lineTo(p.x, p.y)
                        }
                        lineTo(points.last().x, height)
                        close()
                    }

                    // Fill gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PremiumColors.GlowCyan.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw Stroke Line
                    drawPath(
                        path = linePath,
                        color = PremiumColors.GlowCyan,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )

                    // Draw data point dots
                    points.forEach { point ->
                        drawCircle(
                            color = PremiumColors.GlowCyan,
                            radius = 6f,
                            center = point
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(bars.firstOrNull()?.day?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("30-Day Trend Line", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PremiumColors.GlowCyan)
            Text(bars.lastOrNull()?.day?.format(DateTimeFormatter.ofPattern("d MMM")) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayHistoryRow(stat: com.reelmeter.domain.model.DayStat, secondsPerReel: Int) {
    val color = statusColor(stat.total, stat.limitSnapshot)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stat.day.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    stat.perApp.filterValues { it > 0 }.entries.joinToString(" · ") { "${it.key.label} ${it.value}" }.ifEmpty { "0 reels" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${stat.total} reels", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    Formatters.duration(stat.total.toLong() * secondsPerReel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
