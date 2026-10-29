package com.reelmeter.presentation.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reelmeter.core.PermissionStatus
import com.reelmeter.presentation.theme.PremiumColors
import com.reelmeter.presentation.theme.StatusColors
import com.reelmeter.presentation.util.PrivacyText
import com.reelmeter.presentation.util.SystemIntents

/** Prominent disclosure required by Google Play before sending the user to Accessibility settings. */
@Composable
fun AccessibilityDisclosureDialog(onAgree: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Accessibility permission", fontWeight = FontWeight.SemiBold) },
        text = { Text(PrivacyText.DISCLOSURE) },
        confirmButton = { TextButton(onClick = onAgree) { Text("Agree and continue") } },
        dismissButton = { TextButton(onClick = onDecline) { Text("No thanks") } },
    )
}

class PermissionActions(
    val fixAccessibility: () -> Unit,
    val fixOverlay: () -> Unit,
    val fixNotifications: () -> Unit,
)

/** Emits the disclosure dialog when needed and returns click handlers for each permission. */
@Composable
fun rememberPermissionActions(): PermissionActions {
    val context = LocalContext.current
    var showDisclosure by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) SystemIntents.openNotificationSettings(context)
    }
    if (showDisclosure) {
        AccessibilityDisclosureDialog(
            onAgree = { showDisclosure = false; SystemIntents.openAccessibilitySettings(context) },
            onDecline = { showDisclosure = false },
        )
    }
    return PermissionActions(
        fixAccessibility = { showDisclosure = true },
        fixOverlay = { SystemIntents.openOverlaySettings(context) },
        fixNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                SystemIntents.openNotificationSettings(context)
            }
        },
    )
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (granted) {
            // Green glow checkmark for granted state
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Enabled",
                tint = PremiumColors.GlowMint,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = "Not enabled",
                tint = StatusColors.Orange,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!granted) {
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onFix) { Text("Enable") }
        }
    }
}

/** Shown on the dashboard when something is missing; the app keeps working with what it has. */
@Composable
fun PermissionFixCard(status: PermissionStatus, modifier: Modifier = Modifier) {
    val actions = rememberPermissionActions()

    // Subtle pulsing for warning icon
    val infiniteTransition = rememberInfiniteTransition(label = "warningPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Card(
        modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp).scale(pulseScale)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (status.accessibility) "Almost there" else "Counting is paused",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text(
                if (status.accessibility) "Some features need a permission:" else "Turn on the Accessibility Service so ReelMeter can count your reels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            if (!status.accessibility) PermissionRow("Accessibility Service", "Counts swipes. Reads no content.", false, actions.fixAccessibility)
            if (!status.overlay) PermissionRow("Display over other apps", "Shows the break screen at your limit.", false, actions.fixOverlay)
            if (!status.notifications) PermissionRow("Notifications", "Warns you at 80% of your limit.", false, actions.fixNotifications)
        }
    }
}
