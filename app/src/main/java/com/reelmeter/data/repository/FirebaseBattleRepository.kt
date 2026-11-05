package com.reelmeter.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.reelmeter.domain.repository.BattleFriend
import com.reelmeter.domain.repository.BattleRepository
import com.reelmeter.domain.repository.BattleSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

@Singleton
class FirebaseBattleRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : BattleRepository {

    private val auth by lazy { try { FirebaseAuth.getInstance() } catch (e: Exception) { null } }
    private val firestore by lazy { try { FirebaseFirestore.getInstance() } catch (e: Exception) { null } }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _currentBattle = MutableStateFlow<BattleSession?>(null)
    private val _savedFriends = MutableStateFlow<List<BattleFriend>>(emptyList())
    private var snapshotListener: ListenerRegistration? = null
    private var activeCode: String? = null
    private var amICreator: Boolean = false

    private val prefs by lazy {
        context.getSharedPreferences("reelmeter_battle_prefs", Context.MODE_PRIVATE)
    }

    init {
        loadFriendsFromPrefs()
    }

    override fun observeCurrentBattle(): Flow<BattleSession?> = _currentBattle.asStateFlow()
    override fun observeSavedFriends(): Flow<List<BattleFriend>> = _savedFriends.asStateFlow()

    private fun loadFriendsFromPrefs() {
        val jsonStr = prefs.getString("saved_friends", null)
        val list = mutableListOf<BattleFriend>()
        if (!jsonStr.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        BattleFriend(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            code = obj.getString("code"),
                            wins = obj.optInt("wins", 0),
                            losses = obj.optInt("losses", 0),
                            lastActive = obj.optString("lastActive", "Today")
                        )
                    )
                }
            }
        }
        if (list.isEmpty()) {
            list.add(
                BattleFriend(
                    id = "friend_alex",
                    name = "Alex (Practice Rival)",
                    code = "RM-2048",
                    wins = 3,
                    losses = 2,
                    lastActive = "Today"
                )
            )
            saveFriendsToPrefs(list)
        } else {
            _savedFriends.value = list
        }
    }

    private fun saveFriendsToPrefs(list: List<BattleFriend>) {
        _savedFriends.value = list
        scope.launch {
            runCatching {
                val array = JSONArray()
                for (f in list) {
                    val obj = JSONObject().apply {
                        put("id", f.id)
                        put("name", f.name)
                        put("code", f.code)
                        put("wins", f.wins)
                        put("losses", f.losses)
                        put("lastActive", f.lastActive)
                    }
                    array.put(obj)
                }
                prefs.edit().putString("saved_friends", array.toString()).apply()
            }
        }
    }

    override suspend fun addFriend(name: String, code: String) {
        val current = _savedFriends.value.toMutableList()
        val cleanCode = code.trim().uppercase()
        val cleanName = name.trim().ifBlank { "Friend (${cleanCode.takeLast(4)})" }
        if (current.none { it.code == cleanCode }) {
            current.add(
                0,
                BattleFriend(
                    id = "friend_${System.currentTimeMillis()}",
                    name = cleanName,
                    code = cleanCode,
                    wins = 0,
                    losses = 0,
                    lastActive = "Just now"
                )
            )
            saveFriendsToPrefs(current)
        }
    }

    override suspend fun removeFriend(id: String) {
        val updated = _savedFriends.value.filter { it.id != id }
        saveFriendsToPrefs(updated)
    }

    private suspend fun ensureAuth(): String? = suspendCancellableCoroutine { continuation ->
        val fbAuth = auth
        if (fbAuth == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val existing = fbAuth.currentUser
        if (existing != null) {
            continuation.resume(existing.uid)
            return@suspendCancellableCoroutine
        }
        fbAuth.signInAnonymously()
            .addOnSuccessListener { result ->
                continuation.resume(result.user?.uid ?: "user_${System.currentTimeMillis()}")
            }
            .addOnFailureListener { error ->
                Log.w("BattleRepo", "Anonymous auth not enabled or failed", error)
                continuation.resume(null)
            }
    }

    override suspend fun createBattle(creatorName: String, myInitialCount: Int): Result<String> {
        val code = "RM-" + Random.nextInt(1000, 9999)
        activeCode = code
        amICreator = true
        val validCreatorName = creatorName.trim().ifBlank { "You" }

        // 1. Instantly transition UI with the user's real configured name
        _currentBattle.value = BattleSession(
            code = code,
            isCreator = true,
            myName = validCreatorName,
            opponentName = "Waiting for friend...",
            opponentCount = 0,
            isOpponentJoined = false,
            opponentDailyCounts = emptyMap()
        )

        // 2. Sync to Firebase in the background
        scope.launch {
            try {
                val db = firestore
                val uid = ensureAuth() ?: "guest_${Random.nextInt(1000, 9999)}"
                if (db != null) {
                    val data = hashMapOf(
                        "code" to code,
                        "creatorId" to uid,
                        "creatorName" to validCreatorName,
                        "creatorCount" to myInitialCount,
                        "opponentId" to null,
                        "opponentName" to "Waiting...",
                        "opponentCount" to 0,
                        "status" to "WAITING",
                        "createdAt" to System.currentTimeMillis()
                    )
                    awaitTask(db.collection("battles").document(code).set(data))
                    listenToBattle(code, uid, isCreator = true, myName = validCreatorName)
                } else {
                    simulateBotOpponent(code, validCreatorName)
                }
            } catch (e: Exception) {
                Log.w("BattleRepo", "Firebase sync failed, using fallback bot", e)
                simulateBotOpponent(code, validCreatorName)
            }
        }

        return Result.success(code)
    }

    override suspend fun joinBattle(code: String, myName: String, myInitialCount: Int): Result<Unit> {
        val cleanCode = code.trim().uppercase()
        activeCode = cleanCode
        amICreator = false
        val validMyName = myName.trim().ifBlank { "You" }

        // Instantly transition UI
        _currentBattle.value = BattleSession(
            code = cleanCode,
            isCreator = false,
            myName = validMyName,
            opponentName = "Friend ($cleanCode)",
            opponentCount = 0,
            isOpponentJoined = true,
            opponentDailyCounts = emptyMap()
        )

        addFriend(name = "Friend ($cleanCode)", code = cleanCode)

        scope.launch {
            try {
                val db = firestore
                val uid = ensureAuth() ?: "guest_${Random.nextInt(1000, 9999)}"
                if (db != null) {
                    val docRef = db.collection("battles").document(cleanCode)
                    val snapshot = awaitTask(docRef.get())
                    if (snapshot.exists()) {
                        val updates = mapOf(
                            "opponentId" to uid,
                            "opponentName" to validMyName,
                            "opponentCount" to myInitialCount,
                            "status" to "ACTIVE"
                        )
                        awaitTask(docRef.set(updates, SetOptions.merge()))
                        listenToBattle(cleanCode, uid, isCreator = false, myName = validMyName)
                    }
                }
            } catch (e: Exception) {
                Log.w("BattleRepo", "Could not join remote battle, using local session", e)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun updateMyCount(count: Int) {
        val code = activeCode ?: return
        val current = _currentBattle.value ?: return
        _currentBattle.value = current.copy(
            opponentCount = current.opponentCount
        )

        val field = if (amICreator) "creatorCount" else "opponentCount"
        try {
            firestore?.collection("battles")?.document(code)?.update(field, count)
        } catch (_: Exception) {
            // Non-critical background sync
        }
    }

    override suspend fun updateMyDailyStats(statsMap: Map<String, Int>) {
        val code = activeCode ?: return
        val field = if (amICreator) "creatorDailyCounts" else "opponentDailyCounts"
        try {
            firestore?.collection("battles")?.document(code)?.update(field, statsMap)
        } catch (_: Exception) {
            // Non-critical background sync
        }
    }

    override suspend fun leaveBattle() {
        snapshotListener?.remove()
        snapshotListener = null
        activeCode = null
        _currentBattle.value = null
    }

    private fun listenToBattle(code: String, myUid: String, isCreator: Boolean, myName: String) {
        val db = firestore ?: return
        snapshotListener?.remove()
        snapshotListener = db.collection("battles").document(code)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }

                val creatorName = snapshot.getString("creatorName") ?: "Host"
                val creatorCount = snapshot.getLong("creatorCount")?.toInt() ?: 0
                val opponentName = snapshot.getString("opponentName") ?: "Waiting..."
                val opponentCount = snapshot.getLong("opponentCount")?.toInt() ?: 0
                val opponentId = snapshot.getString("opponentId")

                val finalOpponentName = if (opponentId != null) opponentName else "Waiting for friend..."

                @Suppress("UNCHECKED_CAST")
                val creatorDaily = (snapshot.get("creatorDailyCounts") as? Map<String, Long>)
                    ?.mapValues { it.value.toInt() } ?: emptyMap()

                @Suppress("UNCHECKED_CAST")
                val opponentDaily = (snapshot.get("opponentDailyCounts") as? Map<String, Long>)
                    ?.mapValues { it.value.toInt() } ?: emptyMap()

                val oppDailyCounts = if (isCreator) opponentDaily else creatorDaily

                if (opponentId != null && opponentName != "Waiting...") {
                    scope.launch { addFriend(name = opponentName, code = code) }
                }

                val session = if (isCreator) {
                    BattleSession(
                        code = code,
                        isCreator = true,
                        myName = myName,
                        opponentName = finalOpponentName,
                        opponentCount = opponentCount,
                        isOpponentJoined = opponentId != null,
                        opponentDailyCounts = oppDailyCounts
                    )
                } else {
                    BattleSession(
                        code = code,
                        isCreator = false,
                        myName = myName,
                        opponentName = creatorName,
                        opponentCount = creatorCount,
                        isOpponentJoined = true,
                        opponentDailyCounts = oppDailyCounts
                    )
                }

                _currentBattle.value = session
            }
    }

    private fun simulateBotOpponent(code: String, myName: String) {
        scope.launch {
            delay(3000)
            if (activeCode == code) {
                val botOpponentCount = Random.nextInt(2, 6)
                _currentBattle.value = BattleSession(
                    code = code,
                    isCreator = true,
                    myName = myName,
                    opponentName = "Alex (Rival)",
                    opponentCount = botOpponentCount,
                    isOpponentJoined = true,
                    opponentDailyCounts = emptyMap()
                )
                addFriend(name = "Alex (Rival)", code = code)
            }
        }
    }

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { result ->
            cont.resume(result)
        }.addOnFailureListener { error ->
            cont.resumeWithException(error)
        }
    }
}
