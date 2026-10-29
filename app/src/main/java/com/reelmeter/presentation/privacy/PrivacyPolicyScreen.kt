package com.reelmeter.presentation.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val sections = listOf(
    "Summary" to "ReelMeter helps you notice and limit how many short videos you watch. All data stays on your device. We do not collect, transmit or sell any data, and the app has no internet permission.",
    "What the Accessibility Service does" to "The service listens only for scroll events in Instagram, YouTube and Facebook in order to count swipes. It is configured so it cannot retrieve window content: it never reads, records, stores or shares text, messages, passwords, images or any other on-screen content.",
    "What is stored on your device" to "• App name (Instagram, YouTube or Facebook)\n• Timestamp of each counted swipe (kept for 30 days)\n• Daily totals per app (kept until you reset your data)\n• Your settings (limit, theme, tracked apps)",
    "What is NOT collected" to "Personal information, account details, contacts, location, advertising IDs, screen content, analytics or crash reports. Nothing is sent to us or to any third party.",
    "Permissions" to "• Accessibility Service: counts swipes.\n• Display over other apps: shows the break screen at your limit.\n• Notifications: sends the warning at 80% of your limit.\nEach can be turned off at any time in Android settings; the app keeps working with the remaining features.",
    "Your control" to "Use Settings → Reset all data to delete everything stored by the app. Uninstalling the app removes all data. You can disable the Accessibility Service at any time in Android Settings.",
    "Children and teens" to "ReelMeter is intended to help young people. It does not knowingly collect any personal data from anyone.",
    "Contact" to "Replace this section with your developer contact email before publishing on Google Play.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            sections.forEach { (title, body) ->
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
