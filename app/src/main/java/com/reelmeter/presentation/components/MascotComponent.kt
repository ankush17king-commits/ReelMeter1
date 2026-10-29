package com.reelmeter.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reelmeter.presentation.theme.PremiumColors

fun getRobotStageEmoji(total: Int, limit: Int): String {
    val f = if (limit > 0) total.toFloat() / limit else 0f
    return when {
        f < 0.25f -> "🤖"       // Stage 1: Healthy & bright
        f < 0.50f -> "🤖⚡"     // Stage 2: Warming up
        f < 0.75f -> "🩹🤖"     // Stage 3: Damaged
        f < 1.00f -> "💥🤖"     // Stage 4: Heavily glitched
        else -> "💀🤖"         // Stage 5: Total Rot
    }
}

@Composable
fun MascotComponent(
    message: String,
    total: Int = 0,
    limit: Int = 100,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot")

    // Smoother bounce animation
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotOffset"
    )

    // Subtle scale pulse
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotScale"
    )

    val robotEmoji = getRobotStageEmoji(total, limit)
    val fraction = if (limit > 0) total.toFloat() / limit else 0f
    val glowColors = when {
        fraction < 0.25f -> listOf(PremiumColors.GlowCyan, PremiumColors.GlowMint, PremiumColors.GlowCyan)
        fraction < 0.50f -> listOf(PremiumColors.GlowPurple, PremiumColors.GlowCyan, PremiumColors.GlowPurple)
        fraction < 0.75f -> listOf(PremiumColors.GlowAmber, Color(0xFFFF8C00), PremiumColors.GlowAmber)
        fraction < 1.00f -> listOf(PremiumColors.GlowRed, Color(0xFFFF2A55), PremiumColors.GlowRed)
        else -> listOf(Color(0xFFFF0033), Color(0xFF990000), Color(0xFFFF0033))
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(16.dp)) {
        // Mascot avatar with glowing gradient border ring matching rot stage
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = offsetY.dp)
                .scale(scalePulse)
                .size(64.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(glowColors),
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val stageNum = when {
                fraction < 0.25f -> 1
                fraction < 0.50f -> 2
                fraction < 0.75f -> 3
                fraction < 1.00f -> 4
                else -> 5
            }
            val resName = "robot$stageNum"
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)

            if (resId != 0) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = resId),
                    contentDescription = "Robot Stage $stageNum",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(
                    text = robotEmoji,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Speech bubble with subtle gradient background
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                        bottomStart = 0.dp // Speech bubble tail effect
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
