package com.reelmeter.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelmeter.presentation.theme.PremiumColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProgressRing(total: Int, limit: Int, color: Color, modifier: Modifier = Modifier) {
    // Start at 0 on first composition so the ring fills and the number ticks up.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val target = if (started && limit > 0) (total.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(target, tween(1100, easing = FastOutSlowInEasing), label = "ring")
    val shownCount by animateIntAsState(if (started) total else 0, tween(900), label = "count")
    val ringColor by animateColorAsState(color, tween(500), label = "ringColor")
    val track = MaterialTheme.colorScheme.surfaceVariant

    // Pulsing glow when near/over limit
    val fraction = if (limit > 0) total.toFloat() / limit else 0f
    val shouldPulse = fraction >= 0.85f
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = if (shouldPulse) 0.25f else 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val outerStroke = 4.dp.toPx()
            val innerStroke = 20.dp.toPx()
            val padding = innerStroke / 2 + 8.dp.toPx()

            // --- Glow effect behind progress arc ---
            if (progress > 0f) {
                val glowStroke = innerStroke + 16.dp.toPx()
                val glowPadding = glowStroke / 2 + 2.dp.toPx()
                val glowTopLeft = Offset(glowPadding, glowPadding)
                val glowSize = Size(size.width - glowPadding * 2, size.height - glowPadding * 2)
                drawArc(
                    color = ringColor.copy(alpha = pulseAlpha),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = glowTopLeft,
                    size = glowSize,
                    style = Stroke(glowStroke, cap = StrokeCap.Round)
                )
            }

            // --- Outer thin guide ring ---
            val outerTopLeft = Offset(outerStroke / 2, outerStroke / 2)
            val outerSize = Size(size.width - outerStroke, size.height - outerStroke)
            drawArc(
                color = track.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = outerTopLeft,
                size = outerSize,
                style = Stroke(outerStroke)
            )

            // --- Inner track ring ---
            val innerTopLeft = Offset(padding, padding)
            val innerArcSize = Size(size.width - padding * 2, size.height - padding * 2)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerArcSize,
                style = Stroke(innerStroke)
            )

            // --- Inner progress arc with sweep gradient ---
            if (progress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(ringColor, PremiumColors.GlowPurple, ringColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerArcSize,
                    style = Stroke(innerStroke, cap = StrokeCap.Round)
                )

                // --- Dot indicator at the end of the progress arc ---
                val sweepRad = Math.toRadians((-90.0 + 360.0 * progress))
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = (size.width - padding * 2) / 2f
                val dotX = cx + radius * cos(sweepRad).toFloat()
                val dotY = cy + radius * sin(sweepRad).toFloat()
                // Outer glow dot
                drawCircle(
                    color = ringColor.copy(alpha = 0.4f),
                    radius = innerStroke * 0.6f,
                    center = Offset(dotX, dotY)
                )
                // Inner solid dot
                drawCircle(
                    color = Color.White,
                    radius = innerStroke * 0.25f,
                    center = Offset(dotX, dotY)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$shownCount",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-1).sp
            )
            Text(
                "of $limit reels",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
