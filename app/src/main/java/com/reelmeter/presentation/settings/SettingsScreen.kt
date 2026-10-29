package com.reelmeter.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelmeter.domain.logic.Formatters
import com.reelmeter.domain.model.ThemeMode
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.domain.model.UserSettings
import com.reelmeter.presentation.components.AppCard
import com.reelmeter.presentation.components.ErrorState
import com.reelmeter.presentation.components.LoadingState
import com.reelmeter.presentation.components.PermissionRow
import com.reelmeter.presentation.components.SectionTitle
import com.reelmeter.presentation.components.rememberPermissionActions
import com.reelmeter.presentation.theme.PremiumColors
import com.reelmeter.core.PermissionStatus

@Composable
fun SettingsScreen(onOpenPrivacy: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

    when (val s = state) {
        SettingsUiState.Loading -> LoadingState()
        is SettingsUiState.Error -> ErrorState(s.message, viewModel::retry)
        is SettingsUiState.Success -> SettingsContent(s.settings, s.permissions, viewModel, onOpenPrivacy)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    settings: UserSettings,
    permissions: PermissionStatus,
    vm: SettingsViewModel,
    onOpenPrivacy: () -> Unit,
) {
    val context = LocalContext.current
    val actions = rememberPermissionActions()
    var showReset by remember { mutableStateOf(false) }
    var limitDraft by remember(settings.dailyLimit) { mutableFloatStateOf(settings.dailyLimit.toFloat()) }

    Box(Modifier.fillMaxSize()) {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme() || (MaterialTheme.colorScheme.background.red * 0.299 + MaterialTheme.colorScheme.background.green * 0.587 + MaterialTheme.colorScheme.background.blue * 0.114) < 0.5f
        val bgBase = if (isDark) "bg_settings" else "bg_settings1"
        var bgResId = context.resources.getIdentifier(bgBase, "drawable", context.packageName)
        if (bgResId == 0) {
            bgResId = context.resources.getIdentifier("bg_settings", "drawable", context.packageName)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Section title with accent bar
        PremiumSectionTitle("Daily limit")
        AppCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${limitDraft.toInt()} reels per day", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                // Elegant value label
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PremiumColors.GlowCyan.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${limitDraft.toInt()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PremiumColors.GlowCyan
                    )
                }
            }
            Slider(
                value = limitDraft,
                onValueChange = { limitDraft = it },
                onValueChangeFinished = { vm.setLimit(limitDraft.toInt()) },
                valueRange = 10f..500f,
                steps = 48, // steps of 10
                colors = SliderDefaults.colors(
                    thumbColor = PremiumColors.GlowCyan,
                    activeTrackColor = PremiumColors.GlowCyan,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
            Text("Warning at 80%: ${kotlin.math.ceil(limitDraft * 0.8).toInt()} reels", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        PremiumSectionTitle("Tracked apps")
        AppCard {
            TrackedApp.entries.forEachIndexed { index, app ->
                SwitchRow(app.label, null, app in settings.trackedApps) { vm.setTracked(app, it) }
                if (index < TrackedApp.entries.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )
                }
            }
        }

        PremiumSectionTitle("Intervention")
        AppCard {
            SwitchRow("Reminders", "Gentle notification at 80% of your limit", settings.remindersEnabled, vm::setReminders)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            SwitchRow(
                "Floating counter",
                "Show live reel count on top of active apps while watching",
                settings.showFloatingCounter,
                vm::setFloatingCounter,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            SwitchRow(
                "Strict mode",
                "The break screen can't be skipped until a 60-second breathing countdown ends",
                settings.strictMode,
                vm::setStrictMode,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            SwitchRow(
                "No-Scroll Blackout Hours",
                "Block video apps during 11:00 PM – 7:00 AM (protect sleep)",
                settings.blackoutEnabled,
                vm::setBlackoutEnabled,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            SwitchRow(
                "Doomscroll Session Alert",
                "Eye-rest warning after ${settings.doomscrollThreshold} continuous reels",
                settings.doomscrollAlertEnabled,
                vm::setDoomscrollAlert,
            )
        }

        PremiumSectionTitle("Appearance")
        AppCard {
            val modes = ThemeMode.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { vm.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = PremiumColors.GlowCyan.copy(alpha = 0.15f),
                            activeBorderColor = PremiumColors.GlowCyan.copy(alpha = 0.5f),
                        ),
                    ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                }
            }
        }

        PremiumSectionTitle("Permissions")
        AppCard {
            PermissionRow("Accessibility Service", "Needed to count reels", permissions.accessibility, actions.fixAccessibility)
            PermissionRow("Display over other apps", "Needed for the break screen", permissions.overlay, actions.fixOverlay)
            PermissionRow("Notifications", "Needed for the 80% warning", permissions.notifications, actions.fixNotifications)
        }

        PremiumSectionTitle("Privacy & data")
        AppCard {
            OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) { Text("Privacy policy") }
            Spacer(Modifier.height(8.dp))
            // Danger zone with subtle error tint background
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showReset = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Reset all data", color = MaterialTheme.colorScheme.error) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset all data?") },
            text = { Text("This deletes your counted reels, history and streak. Your settings are kept. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    vm.reset { Toast.makeText(context, "Data reset", Toast.LENGTH_SHORT).show() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } },
        )
    }
}

/** Section title with premium accent bar */
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

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = PremiumColors.GlowCyan,
                checkedBorderColor = PremiumColors.GlowCyan,
            )
        )
    }
}
