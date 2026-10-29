package com.reelmeter.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reelmeter.domain.model.AdvancedStatsData
import com.reelmeter.domain.model.Limits
import com.reelmeter.presentation.theme.PremiumColors

@Composable
fun WeeklyCertificateCard(
    data: AdvancedStatsData,
    modifier: Modifier = Modifier
) {
    val userName = data.days.firstOrNull()?.let { "" } ?: "Goal Crusher"
    val stageEmoji = getRobotStageEmoji(data.totalReelsPeriod, data.dailyLimit * 7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF020617)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        PremiumColors.GlowCyan,
                        PremiumColors.GlowPurple,
                        PremiumColors.GlowMint
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Branding
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ReelMeter Certificate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PremiumColors.GlowCyan
                )
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(PremiumColors.GlowMint.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "WEEKLY WINNER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PremiumColors.GlowMint
                    )
                }
            }

            // Robot Stage Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(PremiumColors.GlowCyan, PremiumColors.GlowPurple, PremiumColors.GlowMint)
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Text(stageEmoji, style = MaterialTheme.typography.headlineLarge)
            }

            Text(
                "Digital Wellness Achievement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Stat Badges Row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streak Card
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.LocalFireDepartment, null, tint = PremiumColors.GlowAmber, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("${data.streak} Days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Clean Streak", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                // Time Saved Card
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Timer, null, tint = PremiumColors.GlowCyan, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("${data.estimatedTimeSavedMinutes}m", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Time Saved", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                // Reels Avoided Card
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.EmojiEvents, null, tint = PremiumColors.GlowMint, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("${data.reelsAvoided}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Reels Avoided", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            // Certificate Footer
            Text(
                "Verified by ReelMeter Privacy Guard · Non-intrusive Swipe Counter",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}
