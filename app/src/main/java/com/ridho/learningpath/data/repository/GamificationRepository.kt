package com.ridho.learningpath.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ridho.learningpath.data.model.Achievement
import com.ridho.learningpath.data.model.Progress
import com.ridho.learningpath.data.model.User
import kotlinx.coroutines.tasks.await

class GamificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getUser(userId: String): User? {
        return try {
            val doc = db.collection(AuthRepository.COLLECTION_USERS).document(userId).get().await()
            if (doc.exists()) {
                val u = doc.toObject(User::class.java)
                if (u != null) u.copy(id = doc.id) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Menandai modul selesai dan menambahkan XP.
     *
     * Seluruh pengecekan dan penulisan dijalankan di dalam satu Firestore Transaction
     * agar idempoten secara atomik: tap dua kali secara bersamaan tetap hanya
     * menambah XP satu kali. Karena transaction hanya bisa membaca `DocumentReference`
     * (bukan query), dokumen progress memakai ID deterministik `{userId}_{moduleId}`.
     *
     * Mengembalikan `false` jika modul sudah pernah diselesaikan.
     */
    suspend fun completeModule(userId: String, moduleId: String, xp: Int): Boolean {
        val progressRef = db.collection(COLLECTION_PROGRESS)
            .document(progressDocId(userId, moduleId))
        val userRef = db.collection(AuthRepository.COLLECTION_USERS).document(userId)

        val newXp = db.runTransaction { transaction ->
            val progressSnap = transaction.get(progressRef)
            if (progressSnap.exists()) return@runTransaction null

            val userSnap = transaction.get(userRef)
            val currentXp = (userSnap.getLong("totalPoin") ?: 0L).toInt()
            val updatedXp = currentXp + xp

            transaction.set(
                progressRef,
                hashMapOf(
                    "userId" to userId,
                    "moduleId" to moduleId,
                    "status" to "selesai",
                    "tanggalSelesai" to System.currentTimeMillis()
                )
            )
            transaction.update(
                userRef,
                mapOf(
                    "totalPoin" to updatedXp,
                    "level" to computeLevel(updatedXp)
                )
            )
            updatedXp
        }.await()

        if (newXp == null) return false
        checkAchievements(userId, newXp)
        return true
    }

    private fun progressDocId(userId: String, moduleId: String): String =
        "${userId}_${moduleId}"

    suspend fun getCompletedModuleIds(userId: String): Set<String> {
        return try {
            db.collection(COLLECTION_PROGRESS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("moduleId") }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun getProgress(userId: String): List<Progress> {
        return try {
            db.collection(COLLECTION_PROGRESS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val p = doc.toObject(Progress::class.java)
                    if (p != null) p.copy(id = doc.id) else null
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAchievements(): List<Achievement> {
        return try {
            db.collection(COLLECTION_ACHIEVEMENTS)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val a = doc.toObject(Achievement::class.java)
                    if (a != null) a.copy(id = doc.id) else null
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserAchievementIds(userId: String): Set<String> {
        return try {
            db.collection(COLLECTION_USER_ACHIEVEMENTS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.getString("achievementId") }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun listenLeaderboard(onChange: (List<User>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return db.collection(AuthRepository.COLLECTION_USERS)
            .whereEqualTo("role", "siswa")
            .orderBy("totalPoin", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val u = doc.toObject(User::class.java)
                    if (u != null) u.copy(id = doc.id) else null
                } ?: emptyList()
                onChange(list)
            }
    }

    fun getStudentListener(onChange: (List<User>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return db.collection(AuthRepository.COLLECTION_USERS)
            .whereEqualTo("role", "siswa")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val u = doc.toObject(User::class.java)
                    if (u != null) u.copy(id = doc.id) else null
                } ?: emptyList()
                onChange(list)
            }
    }

    private suspend fun checkAchievements(userId: String, totalPoin: Int) {
        val achievements = getAchievements()
        val ownedIds = getUserAchievementIds(userId)
        for (a in achievements) {
            if (a.id !in ownedIds && totalPoin >= a.syaratPoin) {
                val ua = hashMapOf(
                    "userId" to userId,
                    "achievementId" to a.id,
                    "tanggalDidapat" to System.currentTimeMillis()
                )
                db.collection(COLLECTION_USER_ACHIEVEMENTS).add(ua).await()
            }
        }
    }

    private fun computeLevel(xp: Int): Int {
        return xp / 500 + 1
    }

    companion object {
        const val COLLECTION_PROGRESS = "progress"
        const val COLLECTION_ACHIEVEMENTS = "achievements"
        const val COLLECTION_USER_ACHIEVEMENTS = "userAchievements"
    }
}
