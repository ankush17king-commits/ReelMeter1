package com.reelmeter.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reelmeter.presentation.dashboard.DashboardScreen
import com.reelmeter.presentation.onboarding.OnboardingScreen
import com.reelmeter.presentation.privacy.PrivacyPolicyScreen
import com.reelmeter.presentation.settings.SettingsScreen
import com.reelmeter.presentation.stats.StatsScreen
import com.reelmeter.presentation.theme.PremiumColors
import kotlinx.coroutines.launch

private object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN_TABS = "main_tabs"
    const val PRIVACY = "privacy"
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Rounded.Home),
    BATTLE("battle", "Battle", Icons.Rounded.Groups),
    STATS("stats", "Stats", Icons.Rounded.BarChart),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings),
}

@Composable
fun AppNav(startOnboarding: Boolean) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.MAIN_TABS,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    nav.navigate(Routes.MAIN_TABS) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                },
            )
        }
        composable(Routes.MAIN_TABS) {
            MainTabsPager(onOpenPrivacy = { nav.navigate(Routes.PRIVACY) })
        }
        composable(Routes.PRIVACY) {
            PrivacyPolicyScreen(onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun MainTabsPager(onOpenPrivacy: () -> Unit) {
    val pagerState = rememberPagerState { Tab.entries.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    PremiumColors.GlowCyan.copy(alpha = 0.2f),
                                    PremiumColors.GlowPurple.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Tab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PremiumColors.GlowCyan,
                                selectedTextColor = PremiumColors.GlowCyan,
                                indicatorColor = PremiumColors.GlowCyan.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) { page ->
            when (page) {
                0 -> DashboardScreen()
                1 -> com.reelmeter.presentation.battle.BattleScreen()
                2 -> StatsScreen()
                3 -> SettingsScreen(onOpenPrivacy = onOpenPrivacy)
            }
        }
    }
}
