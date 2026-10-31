package com.reelmeter.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reelmeter.domain.model.TrackedApp
import com.reelmeter.presentation.components.AppCard
import com.reelmeter.presentation.components.MascotComponent
import com.reelmeter.presentation.components.PermissionRow
import com.reelmeter.presentation.components.PremiumButton
import com.reelmeter.presentation.components.rememberPermissionActions
import com.reelmeter.presentation.theme.PremiumColors
import com.reelmeter.presentation.util.PrivacyText
import com.reelmeter.presentation.util.SystemIntents
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val totalPages = 17 // 10 survey/permission pages + 7 personalized reveal screens
    val pagerState = rememberPagerState { totalPages }
    val scope = rememberCoroutineScope()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    var understood by rememberSaveable { mutableStateOf(false) }

    // Questionnaire Answers State
    var userName by rememberSaveable { mutableStateOf("") }
    var selectedGoals by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedApps by rememberSaveable { mutableStateOf(TrackedApp.entries.toSet()) }
    var estimatedUsage by rememberSaveable { mutableStateOf("1–2 hours") }
    var unplannedOpens by rememberSaveable { mutableStateOf("Whenever I'm bored") }
    var scrollTimes by rememberSaveable { mutableStateOf(setOf<String>()) }
    var affectedAreas by rememberSaveable { mutableStateOf(setOf<String>()) }
    var savedTimeActivities by rememberSaveable { mutableStateOf(setOf<String>()) }
    var targetTimeSaved by rememberSaveable { mutableStateOf("1 hour") }
    var limitBehaviors by rememberSaveable { mutableStateOf(setOf<String>("Lock supported apps temporarily")) }
    var dailyLimitReels by rememberSaveable { mutableFloatStateOf(100f) }
    var resetTime by rememberSaveable { mutableStateOf("Midnight") }
    var strictnessMode by rememberSaveable { mutableStateOf("Balanced") }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

    val page = pagerState.currentPage
    val canContinue = page != 9 || understood

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        // Top Header Progress Bar
        if (page in 1..9) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Question $page of 9",
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumColors.GlowCyan,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } }
                ) {
                    Text("Skip", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (page in 10..16) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Personalized Reveal ${page - 9} of 7",
                    style = MaterialTheme.typography.labelMedium,
                    color = PremiumColors.GlowCyan,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(16) } }
                ) {
                    Text("Skip to Finish", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f),
        ) { index ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (index) {
                    0 -> Q1WelcomeScreen(onNext = { scope.launch { pagerState.animateScrollToPage(1) } })
                    1 -> Q2GoalsScreen(selectedGoals) { selectedGoals = it }
                    2 -> Q3AppsScreen(selectedApps) { selectedApps = it }
                    3 -> Q4DailyUsageScreen(estimatedUsage) { estimatedUsage = it }
                    4 -> Q5UnplannedOpensScreen(unplannedOpens) { unplannedOpens = it }
                    5 -> Q6ScrollTimesScreen(scrollTimes) { scrollTimes = it }
                    6 -> Q7AffectedAreasScreen(affectedAreas) { affectedAreas = it }
                    7 -> Q8SavedTimeActivitiesScreen(savedTimeActivities) { savedTimeActivities = it }
                    8 -> Q9DailyLimitAndStrictnessScreen(dailyLimitReels, { dailyLimitReels = it }, strictnessMode, { strictnessMode = it })
                    9 -> Q10CommitmentAndPermissionsPage(
                        userName = userName,
                        onNameChange = { userName = it },
                        goals = selectedGoals,
                        limit = dailyLimitReels.toInt(),
                        mode = strictnessMode,
                        understood = understood,
                        onUnderstoodChange = { understood = it },
                        permissions = permissions
                    )
                    10 -> RevealScreen1Daily(estimatedUsage)
                    11 -> RevealScreen2Weekly(estimatedUsage)
                    12 -> RevealScreen3Monthly(estimatedUsage)
                    13 -> RevealScreen4Yearly(estimatedUsage)
                    14 -> RevealScreen5PeakHours(scrollTimes)
                    15 -> RevealScreen6Alternatives(estimatedUsage, savedTimeActivities)
                    16 -> RevealScreen7Motivational {
                        viewModel.finishOnboarding(
                            userName = userName,
                            occupation = "User",
                            selectedGoals = selectedGoals,
                            selectedApps = selectedApps,
                            estimatedDailyUsage = estimatedUsage,
                            unplannedOpenFrequency = unplannedOpens,
                            scrollTimesOfDay = scrollTimes,
                            scrollAffectsAreas = affectedAreas,
                            preferredSavedTimeActivities = savedTimeActivities,
                            targetTimeSaved = targetTimeSaved,
                            limitReachedBehavior = limitBehaviors,
                            dailyLimit = dailyLimitReels.toInt(),
                            resetTime = resetTime,
                            strictnessMode = strictnessMode,
                            onDone = onFinished
                        )
                    }
                }
            }
        }

        // Navigation Footer Controls
        if (page in 1..8 || page in 9..15) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } }
                ) {
                    Text("Back", fontWeight = FontWeight.Medium)
                }

                if (page < 9) {
                    Button(
                        enabled = canContinue,
                        onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = ButtonDefaults.ContentPadding,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(PremiumColors.GlowCyan, PremiumColors.GlowPurple)))
                    ) {
                        Text("Continue", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (page == 9) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(10) } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = ButtonDefaults.ContentPadding,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(PremiumColors.GlowCyan, PremiumColors.GlowPurple)))
                    ) {
                        Text("See My Report 🚀", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = ButtonDefaults.ContentPadding,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(PremiumColors.GlowCyan, PremiumColors.GlowPurple)))
                    ) {
                        Text("Next ➔", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (selected) {
                    Modifier.border(
                        2.dp,
                        Brush.linearGradient(listOf(PremiumColors.GlowCyan, PremiumColors.GlowPurple)),
                        RoundedCornerShape(20.dp)
                    )
                } else Modifier
            )
            .clickable { onClick() }
    ) {
        AppCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) PremiumColors.GlowCyan else MaterialTheme.colorScheme.onSurface
                )
                if (selected) {
                    Text("✓", fontWeight = FontWeight.Bold, color = PremiumColors.GlowCyan)
                }
            }
        }
    }
}

// Q1 Welcome Screen
@Composable
private fun Q1WelcomeScreen(onNext: () -> Unit) {
    Spacer(Modifier.height(16.dp))
    MascotComponent(
        message = "Hey! 👋 I'm your Reel Meter buddy. I'm here to help you take control of your scrolling, save time, and focus on what matters to you. Let's build a plan that actually works for you!"
    )
    Spacer(Modifier.height(32.dp))
    PremiumButton(text = "Let's Start", onClick = onNext)
}

// Q2 What do you want to achieve?
@Composable
private fun Q2GoalsScreen(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    MascotComponent("Selecting goals helps me tailor recommendations for you!")
    Spacer(Modifier.height(12.dp))
    Text("What do you want to achieve?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Text("Select all that apply", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))

    val options = listOf(
        "Reduce my Instagram scrolling",
        "Stop mindless scrolling and watching Reels",
        "Spend more time studying",
        "Improve my productivity",
        "Focus on fitness and personal growth",
        "Sleep better and use my phone less",
        "Build better digital habits"
    )

    options.forEach { item ->
        val isSelected = item in selected
        OptionCard(text = item, selected = isSelected) {
            onChange(if (isSelected) selected - item else selected + item)
        }
    }
}

// Q3 Apps consumed
@Composable
private fun Q3AppsScreen(selected: Set<TrackedApp>, onChange: (Set<TrackedApp>) -> Unit) {
    MascotComponent("Which apps consume most of your time?")
    Spacer(Modifier.height(12.dp))
    Text("Select your primary apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(16.dp))

    TrackedApp.entries.forEach { app ->
        val isSelected = app in selected
        OptionCard(text = app.label, selected = isSelected) {
            onChange(if (isSelected) selected - app else selected + app)
        }
    }
}

// Q4 Daily time
@Composable
private fun Q4DailyUsageScreen(selected: String, onChange: (String) -> Unit) {
    MascotComponent("Be honest! No judgment here.")
    Spacer(Modifier.height(12.dp))
    Text("How much time do you spend on short-form content daily?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(16.dp))

    val options = listOf(
        "Less than 30 minutes",
        "30 minutes–1 hour",
        "1–2 hours",
        "2–3 hours",
        "3–5 hours",
        "More than 5 hours",
        "I'm not sure"
    )

    options.forEach { item ->
        OptionCard(text = item, selected = selected == item) { onChange(item) }
    }
}

// Q5 Unplanned opens
@Composable
private fun Q5UnplannedOpensScreen(selected: String, onChange: (String) -> Unit) {
    MascotComponent("Many of us open social media automatically!")
    Spacer(Modifier.height(12.dp))
    Text("How often do you open these apps without planning to?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(16.dp))

    val options = listOf(
        "Rarely",
        "A few times a day",
        "Every hour or so",
        "Whenever I'm bored",
        "Almost automatically, without thinking"
    )

    options.forEach { item ->
        OptionCard(text = item, selected = selected == item) { onChange(item) }
    }
}

// Q6 When do you scroll
@Composable
private fun Q6ScrollTimesScreen(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    MascotComponent("Identifying triggers is step #1.")
    Spacer(Modifier.height(12.dp))
    Text("When do you scroll the most?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(16.dp))

    val options = listOf(
        "Immediately after waking up",
        "During classes or work breaks",
        "When I'm bored",
        "When I feel stressed or low",
        "While eating",
        "Before sleeping",
        "Whenever I receive a notification"
    )

    options.forEach { item ->
        val isSelected = item in selected
        OptionCard(text = item, selected = isSelected) {
            onChange(if (isSelected) selected - item else selected + item)
        }
    }
}

// Q7 Affected areas
@Composable
private fun Q7AffectedAreasScreen(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    MascotComponent("We'll protect what matters to you.")
    Spacer(Modifier.height(12.dp))
    Text("What does scrolling usually affect?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(16.dp))

    val options = listOf(
        "My studies or work",
        "My sleep schedule",
        "My exercise or fitness",
        "My personal projects",
        "Time with friends or family",
        "My mood or concentration",
        "Nothing significant at the moment"
    )

    options.forEach { item ->
        val isSelected = item in selected
        OptionCard(text = item, selected = isSelected) {
            onChange(if (isSelected) selected - item else selected + item)
        }
    }
}

// Q8 Rather do
@Composable
private fun Q8SavedTimeActivitiesScreen(selected: Set<String>, onChange: (Set<String>) -> Unit) {
    MascotComponent("Imagine what you can accomplish!")
    Spacer(Modifier.height(12.dp))
    Text("What would you rather do with the time you save?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(16.dp))

    val options = listOf(
        "Study or prepare for exams",
        "Learn programming or another skill",
        "Exercise or play sports",
        "Work on personal projects",
        "Read books",
        "Spend time with friends and family",
        "Relax without endless scrolling",
        "Sleep earlier"
    )

    options.forEach { item ->
        val isSelected = item in selected
        OptionCard(text = item, selected = isSelected) {
            onChange(if (isSelected) selected - item else selected + item)
        }
    }
}

// Q9 Daily Limit & Strictness Combined
@Composable
private fun Q9DailyLimitAndStrictnessScreen(
    selectedLimit: Float,
    onLimitChange: (Float) -> Unit,
    selectedMode: String,
    onModeChange: (String) -> Unit
) {
    MascotComponent("Set your daily target & strictness guidance.")
    Spacer(Modifier.height(12.dp))
    Text("Set your daily limit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))

    Text("${selectedLimit.toInt()} reels daily", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = PremiumColors.GlowCyan)
    Spacer(Modifier.height(8.dp))

    Slider(
        value = selectedLimit,
        onValueChange = onLimitChange,
        valueRange = 10f..300f,
        steps = 28,
        colors = SliderDefaults.colors(
            thumbColor = PremiumColors.GlowCyan,
            activeTrackColor = PremiumColors.GlowCyan
        )
    )

    Spacer(Modifier.height(16.dp))
    Text("Select guidance mode:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))

    val modes = listOf(
        "Balanced Mode" to "Reminders at 75% and temporary intervention at 100%.",
        "Focus Mode" to "Stronger app blocking and strict breathing countdowns."
    )

    modes.forEach { (title, desc) ->
        val isSelected = selectedMode == title.split(" ").first() || selectedMode == title
        OptionCard(text = "$title - $desc", selected = isSelected) {
            onModeChange(title.split(" ").first())
        }
    }
}

// Q10 Personal Commitment & Permissions Combined
@Composable
private fun Q10CommitmentAndPermissionsPage(
    userName: String,
    onNameChange: (String) -> Unit,
    goals: Set<String>,
    limit: Int,
    mode: String,
    understood: Boolean,
    onUnderstoodChange: (Boolean) -> Unit,
    permissions: com.reelmeter.core.PermissionStatus
) {
    val actions = rememberPermissionActions()

    MascotComponent("Confirm your setup & enable required permissions.")
    Spacer(Modifier.height(12.dp))
    Text("Setup & Permissions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(12.dp))

    AppCard {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = userName,
                onValueChange = onNameChange,
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Text("Daily Limit: $limit reels", fontWeight = FontWeight.Bold, color = PremiumColors.GlowCyan)
            Text("Mode: $mode", fontWeight = FontWeight.Medium)
        }
    }

    Spacer(Modifier.height(12.dp))
    PermissionRow("1. Accessibility Service", "Counts swipes in Instagram, Shorts, FB & Snapchat.", permissions.accessibility, actions.fixAccessibility)
    PermissionRow("2. Display over other apps", "Shows break overlay when limit is reached.", permissions.overlay, actions.fixOverlay)

    Spacer(Modifier.height(12.dp))
    // Google Play Protect Sideload Explanation Card
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Shield, null, tint = PremiumColors.GlowAmber, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Google Play Protect Notice", fontWeight = FontWeight.Bold, color = PremiumColors.GlowAmber)
                Text(
                    "ReelMeter is sideloaded directly via APK and is 100% safe. If Play Protect shows an unverified developer prompt, simply tap 'More details → Install anyway'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = understood,
            onCheckedChange = onUnderstoodChange,
            colors = CheckboxDefaults.colors(checkedColor = PremiumColors.GlowCyan)
        )
        Text("I agree to the privacy policy", style = MaterialTheme.typography.bodySmall)
    }
}
