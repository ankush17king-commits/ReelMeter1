package com.reelmeter.presentation.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reelmeter.presentation.components.AppCard
import com.reelmeter.presentation.components.MascotComponent
import com.reelmeter.presentation.components.PremiumButton
import com.reelmeter.presentation.theme.PremiumColors

// Helper to parse daily hours from estimated daily usage string
fun parseDailyHours(estimatedUsageStr: String): Double {
    val lower = estimatedUsageStr.lowercase()
    return when {
        lower.contains("< 30") || lower.contains("less than 30") -> 0.5
        lower.contains("30") && lower.contains("1") -> 0.75
        lower.contains("1–2") || lower.contains("1-2") -> 1.5
        lower.contains("2–3") || lower.contains("2-3") -> 2.5
        lower.contains("3–5") || lower.contains("3-5") -> 4.0
        lower.contains("> 5") || lower.contains("more than 5") -> 6.0
        else -> 2.0
    }
}

@Composable
fun RevealScreen1Daily(estimatedUsageStr: String) {
    val dailyHours = parseDailyHours(estimatedUsageStr)
    val dailyReels = (dailyHours * 60).toInt()
    val animatedHours = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedHours.animateTo(dailyHours.toFloat(), tween(1200))
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MascotComponent("Let's see where your time goes.", total = dailyReels, limit = 100)

        AppCard {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Daily Reel Consumption",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    String.format("%.1f Hours", animatedHours.value),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = PremiumColors.GlowCyan
                )
                Text(
                    "Approx. $dailyReels reels every single day",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RevealScreen2Weekly(estimatedUsageStr: String) {
    val dailyHours = parseDailyHours(estimatedUsageStr)
    val weeklyHours = dailyHours * 7.0
    val animatedWeekly = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedWeekly.animateTo(weeklyHours.toFloat(), tween(1200))
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MascotComponent("What about an entire week?", total = (dailyHours * 60 * 2).toInt(), limit = 100)

        AppCard {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Weekly Scrolling Impact",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    String.format("%.0f Hours", animatedWeekly.value),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = PremiumColors.GlowPurple
                )
                Text(
                    "You could spend ${String.format("%.0f", weeklyHours)} hours every week scrolling reels.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RevealScreen3Monthly(estimatedUsageStr: String) {
    val dailyHours = parseDailyHours(estimatedUsageStr)
    val monthlyHours = dailyHours * 30.0
    val animatedMonthly = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedMonthly.animateTo(monthlyHours.toFloat(), tween(1200))
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MascotComponent("Imagine what you could do with this time.", total = (dailyHours * 60 * 3).toInt(), limit = 100)

        AppCard {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Monthly Total",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    String.format("%.0f Hours", animatedMonthly.value),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = PremiumColors.GlowAmber
                )
                Text(
                    "That's nearly ${String.format("%.1f", monthlyHours / 24.0)} full 24-hour days spent on social media every month.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RevealScreen4Yearly(estimatedUsageStr: String) {
    val dailyHours = parseDailyHours(estimatedUsageStr)
    val yearlyHours = dailyHours * 365.0
    val yearlyDays = yearlyHours / 24.0
    val wakingPercent = (yearlyHours / (365.0 * 16.0)) * 100.0
    val animatedDays = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedDays.animateTo(yearlyDays.toFloat(), tween(1400))
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MascotComponent("One year of scrolling. Think about it.", total = (dailyHours * 60 * 4).toInt(), limit = 100)

        AppCard {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Yearly Scrolling Footprint",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    String.format("%.0f Full Days", animatedDays.value),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = PremiumColors.GlowRed
                )
                Text(
                    "That's ${String.format("%.0f", yearlyHours)} hours/year—or ${String.format("%.1f", wakingPercent)}% of your total waking life!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RevealScreen5PeakHours(scrollTimes: Set<String>) {
    val peakPeriods = scrollTimes.ifEmpty { setOf("Late Night", "Before Sleeping") }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MascotComponent("When do you usually get trapped scrolling?", total = 50, limit = 100)

        AppCard {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Likely Peak Scroll Triggers",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                peakPeriods.forEach { trigger ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PremiumColors.GlowCyan.copy(alpha = 0.12f))
                            .border(1.dp, PremiumColors.GlowCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(trigger, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PremiumColors.GlowCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun RevealScreen6Alternatives(estimatedUsageStr: String, savedActivities: Set<String>) {
    val dailyHours = parseDailyHours(estimatedUsageStr)
    val yearlyHours = dailyHours * 365.0
    val booksRead = (yearlyHours / 10.0).toInt().coerceAtLeast(5)
    val skillsMastered = (yearlyHours / 50.0).toInt().coerceAtLeast(2)

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MascotComponent("Your time is valuable. Imagine what you could achieve!", total = 10, limit = 100)

        AppCard {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "In ${yearlyHours.toInt()} hours, you could:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Book, null, tint = PremiumColors.GlowMint, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Read $booksRead books cover to cover 📚", fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Code, null, tint = PremiumColors.GlowCyan, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Master $skillsMastered new programming or career skills 💻", fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FitnessCenter, null, tint = PremiumColors.GlowPurple, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Build peak physical fitness and health 💪", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun RevealScreen7Motivational(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        MascotComponent(
            "You cannot get your time back, but you can decide how to spend your next minute. Let's make your scrolling intentional!",
            total = 0,
            limit = 100
        )

        AppCard {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📲 Home Screen Widget Preview", fontWeight = FontWeight.Bold, color = PremiumColors.GlowCyan)
                Text(
                    "Track live reel counts right from your home screen widget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        PremiumButton(text = "Let's Get Started", onClick = onStart)
    }
}
