package com.reelmeter.data.repository

import com.reelmeter.domain.repository.LeaderboardRepository
import com.reelmeter.domain.repository.LeaderboardUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Firebase Production Implementation Guide for Play Store Deployment:
 *
 * To connect to real Firebase backend:
 * 1. Add Firebase SDK dependencies to build.gradle.kts:
 *    - implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
 *    - implementation("com.google.firebase:firebase-auth-ktx")
 *    - implementation("com.google.firebase:firebase-firestore-ktx")
 * 2. Initialize Firebase Anonymous Auth:
 *    - FirebaseAuth.getInstance().signInAnonymously()
 * 3. Sync user daily reel count & streak to Firestore collection "leaderboards":
 *    - FirebaseFirestore.getInstance().collection("users").document(uid).set(...)
 * 4. Replace FakeLeaderboardRepository binding with FirebaseLeaderboardRepository in AppModule.kt
 */
class FirebaseLeaderboardRepository : LeaderboardRepository {

    override fun observeParticipation(): Flow<Boolean> = flowOf(false)

    override fun observeLeaderboard(): Flow<List<LeaderboardUser>> = flowOf(emptyList())

    override suspend fun joinLeaderboard(nickname: String) {
        // Firebase Auth anonymous sign-in & Firestore document creation
    }

    override suspend fun leaveLeaderboard() {
        // Firestore document deletion & sign-out
    }

    override suspend fun addFriendByCode(code: String): Boolean {
        // Query Firestore collection by invite code
        return true
    }

    override fun getMyInviteCode(): String = "RM-FIREBASE"
}
