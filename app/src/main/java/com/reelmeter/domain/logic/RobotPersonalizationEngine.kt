package com.reelmeter.domain.logic

import com.reelmeter.domain.model.UserSettings

object RobotPersonalizationEngine {

    fun generateMascotMessage(settings: UserSettings, totalReels: Int): String {
        val limit = settings.dailyLimit
        val percentage = if (limit > 0) (totalReels.toFloat() / limit) else 0f

        val goalFocus = when {
            settings.selectedGoals.any { it.contains("Study", ignoreCase = true) } -> "study session"
            settings.selectedGoals.any { it.contains("Sleep", ignoreCase = true) } -> "good night's rest"
            settings.selectedGoals.any { it.contains("Fitness", ignoreCase = true) } -> "workout goal"
            else -> "productivity time"
        }

        return when {
            totalReels == 0 -> "Hey ${settings.userName.ifBlank { "friend" }}! 👋 0 reels so far. Ready for a great $goalFocus?"
            percentage < 0.4f -> "Great job staying in control! $totalReels reels watched. Protect your $goalFocus!"
            percentage < 0.8f -> "You're at $totalReels reels ($totalReels / $limit). Consider taking a short pause for your $goalFocus."
            percentage < 1.0f -> "Nearing your daily limit! Almost time to put the phone down."
            else -> "Limit reached! $totalReels reels today. Time to recharge off-screen!"
        }
    }
}
