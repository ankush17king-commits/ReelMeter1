package com.reelmeter.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import com.reelmeter.presentation.theme.PremiumColors

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(24.dp)
    val cardModifier = if (isDark) {
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = cardShape,
                spotColor = PremiumColors.CardGlow,
                ambientColor = PremiumColors.CardGlow
            )
            .border(1.dp, PremiumColors.CardBorder, cardShape)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 0.5.dp),
    ) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

/**
 * Premium glass card with a colored glow border.
 * Use for status-dependent cards where the glow changes color.
 */
@Composable
fun GlowingCard(
    glowColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(24.dp)
    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.4f),
                glowColor.copy(alpha = 0.1f),
                PremiumColors.GlowPurple.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.2f),
                glowColor.copy(alpha = 0.05f)
            )
        )
    }

    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = if (isDark) 16.dp else 4.dp,
            shape = cardShape,
            spotColor = glowColor.copy(alpha = if (isDark) 0.15f else 0.05f),
            ambientColor = glowColor.copy(alpha = if (isDark) 0.08f else 0.02f)
        )
        .border(1.dp, borderBrush, cardShape)

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(24.dp), content = content)
    }
}

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "buttonScale")

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = PremiumColors.GlowCyan.copy(alpha = 0.3f),
                ambientColor = PremiumColors.GlowCyan.copy(alpha = 0.1f)
            )
            .scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            PremiumColors.GradientStart,
                            PremiumColors.GradientEnd
                        )
                    ),
                    alpha = if (enabled) 1f else 0.5f
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical accent bar indicator
        Box(
            Modifier
                .width(3.dp)
                .height(20.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PremiumColors.GradientStart,
                            PremiumColors.GradientEnd
                        )
                    ),
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LabeledRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { content() }
}
