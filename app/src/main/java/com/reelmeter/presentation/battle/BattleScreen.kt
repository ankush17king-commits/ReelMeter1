package com.reelmeter.presentation.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelmeter.domain.repository.BattleDailyScore
import com.reelmeter.domain.repository.BattleFriend
import com.reelmeter.presentation.components.AppCard
import com.reelmeter.presentation.components.MascotComponent
import com.reelmeter.presentation.components.PremiumButton
import com.reelmeter.presentation.components.SectionTitle
import com.reelmeter.presentation.theme.PremiumColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(viewModel: BattleViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = 1v1 Battle, 1 = Global Leaderboard
    val tabLabels = listOf("1v1 Challenge", "Global Leaderboard")
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        val isDark = isSystemInDarkTheme() || (MaterialTheme.colorScheme.background.red * 0.299 + MaterialTheme.colorScheme.background.green * 0.587 + MaterialTheme.colorScheme.background.blue * 0.114) < 0.5f
        val bgBase = if (isDark) "bg_battle" else "bg_battle1"
        var bgResId = context.resources.getIdentifier(bgBase, "drawable", context.packageName)
        if (bgResId == 0) {
            bgResId = context.resources.getIdentifier("bg_battle", "drawable", context.packageName)
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
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Battle & Leaderboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            // Top Segmented control switching between 1v1 Battle and Global Leaderboard
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                tabLabels.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index, tabLabels.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = PremiumColors.GlowCyan.copy(alpha = 0.15f),
                            activeBorderColor = PremiumColors.GlowCyan.copy(alpha = 0.5f),
                        ),
                    ) { Text(label) }
                }
            }

            if (selectedTab == 0) {
                when (val s = state) {
                    is BattleUiState.NoBattle -> NoBattleContent(s, viewModel)
                    is BattleUiState.ActiveBattle -> ActiveBattleContent(s, viewModel)
                }
            } else {
                com.reelmeter.presentation.leaderboard.LeaderboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoBattleContent(state: BattleUiState.NoBattle, viewModel: BattleViewModel) {
    var joinCode by remember { mutableStateOf("") }

    val greeting = if (state.userName.isNotBlank() && state.userName != "You") {
        "Hey ${state.userName}! Challenge a friend to watch fewer reels today!"
    } else {
        "Challenge a friend to watch fewer reels today!"
    }
    MascotComponent(greeting)

    // 1. Permanent Saved Friends Section
    if (state.friends.isNotEmpty()) {
        SectionTitle("Your Rivals & Friends")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.friends.forEach { friend ->
                FriendRivalCard(
                    friend = friend,
                    onChallenge = { viewModel.joinBattle(friend.code) },
                    onRemove = { viewModel.removeFriend(friend.id) }
                )
            }
        }
    }

    SectionTitle("Create a Battle")
    AppCard {
        Text("Generate a room code to challenge a friend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        PremiumButton(
            text = "Create New Battle",
            onClick = { viewModel.createBattle() }
        )
    }

    // VS divider between create and join
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PremiumColors.GlowCyan.copy(alpha = 0.2f), PremiumColors.GlowPurple.copy(alpha = 0.2f))
                        )
                    )
                    .border(1.dp, PremiumColors.GlowCyan.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("VS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = PremiumColors.GlowCyan)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }

    SectionTitle("Join Friend by Code")
    AppCard {
        OutlinedTextField(
            value = joinCode,
            onValueChange = { joinCode = it },
            label = { Text("Friend's Code (e.g. RM-1234)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { viewModel.joinBattle(joinCode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = joinCode.length > 3
        ) {
            Text("Join Battle")
        }
    }
}

@Composable
private fun FriendRivalCard(
    friend: BattleFriend,
    onChallenge: () -> Unit,
    onRemove: () -> Unit
) {
    AppCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PremiumColors.GlowCyan, PremiumColors.GlowPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.name.firstOrNull()?.toString() ?: "F",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(friend.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Code: ${friend.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "•  ${friend.wins}W - ${friend.losses}L",
                        style = MaterialTheme.typography.bodySmall,
                        color = PremiumColors.GlowMint,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            IconButton(onClick = onChallenge) {
                Icon(
                    Icons.Rounded.FlashOn,
                    contentDescription = "Challenge",
                    tint = PremiumColors.GlowCyan
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ActiveBattleContent(state: BattleUiState.ActiveBattle, viewModel: BattleViewModel) {
    val youWinning = state.myCount <= state.opponentCount
    val msg = if (youWinning) "You're winning! Keep off those reels!" else "Uh oh, you're watching more reels!"

    MascotComponent(msg)

    SectionTitle("Invite Code")
    val context = LocalContext.current
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.inviteCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("ReelMeter Battle Code", state.inviteCode)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(context, "Code copied: ${state.inviteCode}", android.widget.Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Rounded.ContentCopy, "Copy")
            }
            IconButton(onClick = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Join my 1v1 screen time battle on ReelMeter! My invite code is: ${state.inviteCode}")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Battle Code"))
            }) {
                Icon(Icons.Rounded.Groups, "Share")
            }
        }
        Text("Share this code with a friend so they can join.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SectionTitle("Today's Live Score (Lower is better)")
    AppCard {
        // You - with cyan accent
        val youCardModifier = if (youWinning) {
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PremiumColors.GlowMint.copy(alpha = 0.05f))
                .border(1.dp, PremiumColors.GlowMint.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
        }

        Box(youCardModifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PremiumColors.GlowCyan)
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(PremiumColors.GlowCyan, PremiumColors.GlowPurple))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.myName.firstOrNull()?.toString() ?: "U",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.myName, fontWeight = FontWeight.SemiBold)
                        if (youWinning) {
                            Spacer(Modifier.width(6.dp))
                            Text("🏆", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
                        LinearProgressIndicator(
                            progress = { (state.myCount.toFloat() / state.limit.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                            color = PremiumColors.GlowCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("${state.myCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        // VS badge between players
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PremiumColors.GlowCyan, PremiumColors.GlowPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "VS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // Opponent
        val opponentWinning = !youWinning
        val opponentCardModifier = if (opponentWinning) {
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PremiumColors.GlowMint.copy(alpha = 0.05f))
                .border(1.dp, PremiumColors.GlowMint.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
        }

        Box(opponentCardModifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PremiumColors.GlowRed)
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(PremiumColors.GlowRed, Color(0xFFFF6B81)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.opponentName.firstOrNull()?.toString() ?: "F",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.opponentName, fontWeight = FontWeight.SemiBold)
                        if (opponentWinning) {
                            Spacer(Modifier.width(6.dp))
                            Text("🏆", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (state.opponentCount.toFloat() / state.limit.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = PremiumColors.GlowRed
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("${state.opponentCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 2. Weekly Battle Report (Monday - Sunday breakdown)
    SectionTitle("Weekly Head-to-Head Report")
    AppCard {
        // Summary row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    state.youWonDays > state.opponentWonDays -> "🔥 You lead this week (${state.youWonDays} - ${state.opponentWonDays})"
                    state.youWonDays < state.opponentWonDays -> "👀 Rival leads (${state.opponentWonDays} - ${state.youWonDays})"
                    else -> "🤝 Tied this week (${state.youWonDays} - ${state.opponentWonDays})"
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = if (state.youWonDays >= state.opponentWonDays) PremiumColors.GlowMint else PremiumColors.GlowRed
            )
            Text(
                "Fewer reels = Win",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        // Daily breakdown rows (Monday through Sunday)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.weeklyHistory.forEach { dayScore ->
                WeeklyDayScoreRow(
                    day = dayScore,
                    myName = state.myName,
                    opponentName = state.opponentName
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = { viewModel.leaveBattle() },
        modifier = Modifier.fillMaxWidth(),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = Brush.horizontalGradient(
                listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error)
            )
        ),
    ) {
        Text("Leave Battle", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun WeeklyDayScoreRow(
    day: BattleDailyScore,
    myName: String,
    opponentName: String
) {
    val bgModifier = when {
        day.isToday -> Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PremiumColors.GlowCyan.copy(alpha = 0.1f))
            .border(1.dp, PremiumColors.GlowCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        else -> Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    }

    Box(bgModifier) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Day name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day.fullDayName,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(90.dp)
                )
                if (day.isToday) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PremiumColors.GlowCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "TODAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = PremiumColors.GlowCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (day.isFuture) {
                Text(
                    "Upcoming",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                // Score breakdown: Real counts with user names
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${day.myReels}",
                        fontWeight = FontWeight.Bold,
                        color = if (day.youWon) PremiumColors.GlowMint else MaterialTheme.colorScheme.onSurface
                    )
                    Text(" vs ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${day.opponentReels}",
                        fontWeight = FontWeight.Bold,
                        color = if (!day.youWon) PremiumColors.GlowRed else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(10.dp))

                    // Outcome badge with winner's actual name
                    if (day.isToday) {
                        val winnerName = if (day.youWon) myName else opponentName
                        Text(
                            "🔥 $winnerName leads",
                            color = if (day.youWon) PremiumColors.GlowMint else PremiumColors.GlowRed,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else if (day.youWon) {
                        Text(
                            "🏆 $myName won",
                            color = PremiumColors.GlowMint,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    } else {
                        Text(
                            "🏆 $opponentName won",
                            color = PremiumColors.GlowRed,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
