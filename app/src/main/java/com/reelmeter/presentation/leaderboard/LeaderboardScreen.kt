package com.reelmeter.presentation.leaderboard

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelmeter.domain.repository.LeaderboardUser
import com.reelmeter.presentation.components.AppCard
import com.reelmeter.presentation.components.PremiumButton
import com.reelmeter.presentation.components.SectionTitle
import com.reelmeter.presentation.theme.PremiumColors

// Rank-specific colors
private val GoldColor = Color(0xFFFFD700)
private val SilverColor = Color(0xFFC0C0C0)
private val BronzeColor = Color(0xFFCD7F32)

@Composable
fun LeaderboardScreen(viewModel: LeaderboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Leaderboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        when (val s = state) {
            is LeaderboardUiState.OptInRequired -> OptInConsentContent(onJoin = viewModel::join)
            is LeaderboardUiState.Active -> ActiveLeaderboardContent(s, viewModel)
        }
    }
}

@Composable
private fun OptInConsentContent(onJoin: (String) -> Unit) {
    var nick by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        // Decorative sparkle dots in background
        Canvas(Modifier.fillMaxSize()) {
            val sparklePositions = listOf(
                Offset(size.width * 0.1f, size.height * 0.15f),
                Offset(size.width * 0.85f, size.height * 0.1f),
                Offset(size.width * 0.7f, size.height * 0.25f),
                Offset(size.width * 0.15f, size.height * 0.35f),
                Offset(size.width * 0.9f, size.height * 0.4f),
                Offset(size.width * 0.3f, size.height * 0.5f),
                Offset(size.width * 0.8f, size.height * 0.55f),
                Offset(size.width * 0.05f, size.height * 0.6f),
                Offset(size.width * 0.6f, size.height * 0.7f),
                Offset(size.width * 0.2f, size.height * 0.8f),
            )
            sparklePositions.forEachIndexed { index, pos ->
                val alpha = if (index % 2 == 0) 0.15f else 0.08f
                val radius = if (index % 3 == 0) 3f else 2f
                drawCircle(
                    color = GoldColor.copy(alpha = alpha),
                    radius = radius.dp.toPx(),
                    center = pos
                )
            }
        }

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Trophy with golden glow
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer { alpha = 0.3f }
                        .blur(16.dp),
                    tint = GoldColor
                )
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = GoldColor
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Join Friends Leaderboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Compete with friends to watch the FEWEST reels today! Privacy guaranteed: ONLY your nickname, daily reel count, and streak are shared. NO screen content is ever read or transmitted.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = nick,
                onValueChange = { nick = it },
                label = { Text("Your Nickname") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            PremiumButton(
                text = "Join Leaderboard",
                onClick = { if (nick.isNotBlank()) onJoin(nick) },
                enabled = nick.isNotBlank()
            )
        }
    }
}

@Composable
private fun ActiveLeaderboardContent(
    state: LeaderboardUiState.Active,
    viewModel: LeaderboardViewModel
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Rankings (Lowest = Best)")
        Row {
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Friend")
            }
            IconButton(onClick = {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Join my ReelMeter leaderboard! My code: ${state.inviteCode}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Invite Friend"))
            }) {
                Icon(Icons.Rounded.Share, contentDescription = "Share Code")
            }
        }
    }

    if (state.users.isNotEmpty()) {
        PodiumView(state.users.take(3))
        Spacer(Modifier.height(12.dp))
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(state.users) { index, user ->
            UserRankRow(rank = index + 1, user = user)
        }
        item {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { viewModel.leave() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leave Leaderboard & Clear Data", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showAddDialog) {
        var codeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Friend by Code") },
            text = {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Friend Code (e.g. RM-1234)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (codeInput.isNotBlank()) {
                        viewModel.addFriend(codeInput)
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PodiumView(topThree: List<LeaderboardUser>) {
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            if (topThree.size >= 2) PodiumItem(user = topThree[1], rank = 2, heightDp = 80)
            if (topThree.isNotEmpty()) PodiumItem(user = topThree[0], rank = 1, heightDp = 110)
            if (topThree.size >= 3) PodiumItem(user = topThree[2], rank = 3, heightDp = 65)
        }
    }
}

@Composable
private fun PodiumItem(user: LeaderboardUser, rank: Int, heightDp: Int) {
    val rankColor = when (rank) {
        1 -> GoldColor
        2 -> SilverColor
        3 -> BronzeColor
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Crown glow for first place
        if (rank == 1) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "👑",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.graphicsLayer { alpha = 0.4f }.blur(8.dp)
                )
                Text("👑", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            Text("#$rank", style = MaterialTheme.typography.titleMedium, color = rankColor, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        // Avatar with colored ring
        Box(
            Modifier
                .size(if (rank == 1) 52.dp else 44.dp)
                .clip(CircleShape)
                .border(
                    width = if (rank == 1) 3.dp else 2.dp,
                    color = rankColor,
                    shape = CircleShape
                )
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(user.nickname.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(4.dp))
        // Gradient height pillar
        Box(
            Modifier
                .width(40.dp)
                .height(heightDp.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(rankColor.copy(alpha = 0.6f), rankColor.copy(alpha = 0.15f))
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier.padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(user.nickname, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("${user.reelsToday} reels", style = MaterialTheme.typography.labelSmall, color = rankColor)
            }
        }
    }
}

@Composable
private fun UserRankRow(rank: Int, user: LeaderboardUser) {
    val rankColor = when (rank) {
        1 -> GoldColor
        2 -> SilverColor
        3 -> BronzeColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cardModifier = if (user.isYou) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(PremiumColors.GlowCyan.copy(alpha = 0.4f), PremiumColors.GlowPurple.copy(alpha = 0.4f))
            ),
            shape = RoundedCornerShape(28.dp)
        )
    } else {
        Modifier
    }

    AppCard(modifier = cardModifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = rankColor,
                modifier = Modifier.width(32.dp)
            )
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (rank <= 3) Modifier.border(2.dp, rankColor, CircleShape) else Modifier
                    )
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(user.nickname.firstOrNull()?.toString() ?: "?", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.nickname, fontWeight = FontWeight.SemiBold)
                    if (user.isYou) {
                        Spacer(Modifier.width(6.dp))
                        Text("(You)", style = MaterialTheme.typography.labelSmall, color = PremiumColors.GlowCyan)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${user.streakDays} day streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (user.streakDays > 3) {
                        Spacer(Modifier.width(4.dp))
                        Text("🔥", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("${user.reelsToday} reels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
