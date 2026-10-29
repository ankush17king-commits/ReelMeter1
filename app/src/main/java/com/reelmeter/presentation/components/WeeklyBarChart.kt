package com.reelmeter.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reelmeter.domain.model.DayBar
import com.reelmeter.presentation.theme.PremiumColors
import com.reelmeter.presentation.util.statusColor
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyBarChart(bars: List<DayBar>, limit: Int, modifier: Modifier = Modifier) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val maxValue = maxOf(limit, bars.maxOfOrNull { it.total } ?: 0, 1)
    val limitLineColor = PremiumColors.GlowCyan.copy(alpha = 0.5f)
    val emptyBar = MaterialTheme.colorScheme.surfaceVariant
    val today = LocalDate.now()

    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            // Limit guide line with glow
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - limit.toFloat() / maxValue)
                // Glow layer
                drawLine(
                    color = limitLineColor.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 6.dp.toPx(),
                )
                // Crisp dashed line
                drawLine(
                    color = limitLineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
                )
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                bars.forEach { bar ->
                    val target = if (started) (bar.total.toFloat() / maxValue).coerceIn(0.03f, 1f) else 0.03f
                    val fraction by animateFloatAsState(target, tween(900), label = "bar")
                    val barColor = if (bar.total == 0) emptyBar else statusColor(bar.total, bar.limit)

                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Count label on top of bar when > 0
                            if (bar.total > 0) {
                                Text(
                                    "${bar.total}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Box(
                                Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            if (bar.total == 0)
                                                listOf(emptyBar, emptyBar)
                                            else
                                                listOf(barColor, barColor.copy(alpha = 0.3f))
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            bars.forEach { bar ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        bar.day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (bar.day == today) FontWeight.Bold else FontWeight.Normal,
                        color = if (bar.day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
