package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AdventurerProfile
import com.example.model.ArtifactCategory
import com.example.model.CharacterStyle
import com.example.model.CompletedQuest
import com.example.model.EarnedBadge
import com.example.model.FoundArtifact
import com.example.model.InventoryItem
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class SideQuestRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(userId: String): Flow<AdventurerProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        AdventurerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeQuests(userId: String): Flow<List<CompletedQuest>> {
        val path = "users/$userId/quests"
        return db.collection("users").document(userId).collection("quests")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    CompletedQuest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeBadges(userId: String): Flow<List<EarnedBadge>> {
        val path = "users/$userId/badges"
        return db.collection("users").document(userId).collection("badges")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    EarnedBadge::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeInventory(userId: String): Flow<List<InventoryItem>> {
        val path = "users/$userId/inventory"
        return db.collection("users").document(userId).collection("inventory")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    InventoryItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<InventoryItem> { it.equipped }
                        .thenByDescending { it.powerBonus }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeArtifacts(userId: String): Flow<List<FoundArtifact>> {
        val path = "users/$userId/artifacts"
        return db.collection("users").document(userId).collection("artifacts")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    FoundArtifact::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<FoundArtifact> { it.isFavorite }
                        .thenByDescending { it.resonanceValue }
                        .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun ensureAdventurerProfile(
        defaultName: String,
        style: CharacterStyle
    ): AdventurerProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(
                    AdventurerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: AdventurerProfile(id = uid, userId = uid, displayName = defaultName)
            } else {
                val safeName = defaultName.trim().ifBlank { "Wanderer" }.take(80)
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "characterStyle" to style.id,
                    "xp" to 0L,
                    "level" to 1L,
                    "questsCompleted" to 0L,
                    "landmarksVisited" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()

                val invSnap = docRef.collection("inventory").limit(1).get().await()
                if (invSnap.isEmpty) {
                    addInventoryItem(
                        name = style.defaultRelicName,
                        loreDescription = style.defaultRelicLore,
                        itemType = style.defaultRelicType,
                        rarity = "RARE",
                        originLandmark = "Guild Initiation Sanctum",
                        characterStyle = style,
                        powerBonus = 15,
                        equipped = true
                    )
                }

                val artSnap = docRef.collection("artifacts").limit(1).get().await()
                if (artSnap.isEmpty) {
                    recordFoundArtifact(
                        artifactCode = "FOUNDER_COMPASS_SHARD",
                        name = "Astral Wayfarer's Compass Shard",
                        loreInscription = "Awarded upon initiation into the SideQuest Guild. Resonates with nearby parks, historical plaques, and coffee houses.",
                        category = ArtifactCategory.RELIC_SHARD,
                        rarity = "RARE",
                        landmarkName = "Ferry Building Marketplace Clocktower",
                        regionName = "San Francisco — Embarcadero",
                        characterStyle = style,
                        latitude = 37.7955,
                        longitude = -122.3937,
                        resonanceValue = 85,
                        isFavorite = true,
                        inspectionNotes = "Inaugural Guild curio calibrated to real-world ley-lines."
                    )
                }

                unlockBadgeIfAbsent(
                    badgeId = "badge_guild_initiate",
                    badgeCode = "GUILD_INITIATE_CREST",
                    title = "Guild Initiate Crest",
                    description = "Swore the SideQuest Explorer's Oath and calibrated the tactical radar.",
                    category = "EXPLORATION",
                    rarity = "RARE",
                    landmarkName = "Guild Initiation Sanctum"
                )

                AdventurerProfile(
                    id = uid,
                    userId = uid,
                    displayName = safeName,
                    characterStyle = style.id,
                    xp = 0L,
                    level = 1L,
                    questsCompleted = 0L,
                    landmarksVisited = 0L
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateCharacterStyle(style: CharacterStyle) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                ensureAdventurerProfile(
                    defaultName = auth.currentUser?.displayName ?: "Wanderer",
                    style = style
                )
            } else {
                docRef.update(
                    mapOf(
                        "characterStyle" to style.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun awardProgress(
        xpDelta: Long,
        landmarksDelta: Long,
        questsCompletedDelta: Long
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = (auth.currentUser?.displayName ?: "Wanderer").trim().ifBlank { "Wanderer" }.take(80)
                val initialXp = xpDelta.coerceIn(0L, 10_000_000L)
                val initialLevel = ((initialXp / 250L) + 1L).coerceIn(1L, 1000L)
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "characterStyle" to CharacterStyle.CYBERPUNK_SCOUT.id,
                        "xp" to initialXp,
                        "level" to initialLevel,
                        "questsCompleted" to questsCompletedDelta.coerceIn(0L, 100_000L),
                        "landmarksVisited" to landmarksDelta.coerceIn(0L, 500_000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val currentXp = (snap.getLong("xp") ?: 0L) + xpDelta
                val newLevel = (currentXp / 250L) + 1L
                val newLandmarks = (snap.getLong("landmarksVisited") ?: 0L) + landmarksDelta
                val newQuests = (snap.getLong("questsCompleted") ?: 0L) + questsCompletedDelta
                docRef.update(
                    mapOf(
                        "xp" to currentXp.coerceIn(0L, 10_000_000L),
                        "level" to newLevel.coerceIn(1L, 1000L),
                        "landmarksVisited" to newLandmarks.coerceIn(0L, 500_000L),
                        "questsCompleted" to newQuests.coerceIn(0L, 100_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun createQuest(
        title: String,
        characterStyle: CharacterStyle,
        regionName: String,
        summary: String,
        landmarksCount: Int,
        distanceMeters: Int,
        stopsSummary: List<String>
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("quests").document()
        val safeStops = stopsSummary.take(15).map { it.trim().ifBlank { "Waypoint" }.take(140) }
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().ifBlank { "Untold Expedition" }.take(140),
            "characterStyle" to characterStyle.id,
            "regionName" to regionName.trim().ifBlank { "Local Sector" }.take(120),
            "summary" to summary.trim().ifBlank { "Walking quest initialized." }.take(1000),
            "landmarksCount" to landmarksCount.coerceIn(1, 20),
            "completedStops" to 0,
            "xpEarned" to 0,
            "distanceMeters" to distanceMeters.coerceIn(0, 500_000),
            "status" to "ACTIVE",
            "stopsSummary" to safeStops,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateQuestProgress(
        questId: String,
        completedStops: Int,
        xpEarned: Int,
        status: String,
        summary: String
    ) {
        val uid = requireUserId()
        require(questId.isNotBlank()) { "questId must not be blank" }
        val docRef = db.collection("users").document(uid).collection("quests").document(questId)
        val payload = mapOf(
            "completedStops" to completedStops.coerceIn(0, 20),
            "xpEarned" to xpEarned.coerceIn(0, 100_000),
            "status" to if (status == "COMPLETED") "COMPLETED" else "ACTIVE",
            "summary" to summary.trim().ifBlank { "Quest updated." }.take(1000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun unlockBadgeIfAbsent(
        badgeId: String,
        badgeCode: String,
        title: String,
        description: String,
        category: String,
        rarity: String,
        landmarkName: String
    ): Boolean {
        val uid = requireUserId()
        val safeDocId = badgeId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "badge_1" }
        val docRef = db.collection("users").document(uid).collection("badges").document(safeDocId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                false
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "badgeCode" to badgeCode.trim().ifBlank { "BADGE" }.take(60),
                    "title" to title.trim().ifBlank { "Explorer Badge" }.take(100),
                    "description" to description.trim().ifBlank { "Earned on a SideQuest walk." }.take(400),
                    "category" to if (category in listOf("EXPLORATION", "LORE", "HISTORIAN", "CYBERPUNK", "MASTERY")) category else "EXPLORATION",
                    "rarity" to if (rarity in listOf("COMMON", "RARE", "EPIC", "LEGENDARY")) rarity else "COMMON",
                    "landmarkName" to landmarkName.trim().ifBlank { "Unknown Landmark" }.take(140),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun addInventoryItem(
        name: String,
        loreDescription: String,
        itemType: String,
        rarity: String,
        originLandmark: String,
        characterStyle: CharacterStyle,
        powerBonus: Int,
        equipped: Boolean = false
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inventory").document()
        val validType = if (itemType in listOf("RELIC", "CYBER_DECK", "CHRONICLE", "ELIXIR", "KEYSTONE")) itemType else "RELIC"
        val validRarity = if (rarity in listOf("COMMON", "RARE", "EPIC", "LEGENDARY")) rarity else "COMMON"
        val payload = mapOf(
            "userId" to uid,
            "name" to name.trim().ifBlank { "Mysterious Relic" }.take(100),
            "loreDescription" to loreDescription.trim().ifBlank { "Discovered during a walking quest." }.take(600),
            "itemType" to validType,
            "rarity" to validRarity,
            "originLandmark" to originLandmark.trim().ifBlank { "Local Waypoint" }.take(140),
            "characterStyle" to characterStyle.id,
            "powerBonus" to powerBonus.coerceIn(1, 500),
            "equipped" to equipped,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleEquipItem(itemId: String, equipped: Boolean) {
        val uid = requireUserId()
        require(itemId.isNotBlank()) { "itemId must not be blank" }
        val docRef = db.collection("users").document(uid).collection("inventory").document(itemId)
        try {
            docRef.update(
                mapOf(
                    "equipped" to equipped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordFoundArtifact(
        artifactCode: String,
        name: String,
        loreInscription: String,
        category: ArtifactCategory,
        rarity: String,
        landmarkName: String,
        regionName: String,
        characterStyle: CharacterStyle,
        latitude: Double,
        longitude: Double,
        resonanceValue: Int,
        isFavorite: Boolean = false,
        inspectionNotes: String = ""
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("artifacts").document()
        val validRarity = if (rarity in listOf("COMMON", "RARE", "EPIC", "LEGENDARY")) rarity else "RARE"
        val cleanCode = artifactCode.trim().uppercase(Locale.US)
            .replace(Regex("[^A-Z0-9_\\-]"), "_")
            .ifBlank { "ARTIFACT_CURIO" }
            .take(80)
        val payload = mapOf(
            "userId" to uid,
            "artifactCode" to cleanCode,
            "name" to name.trim().ifBlank { "Unearthed Landmark Curio" }.take(100),
            "loreInscription" to loreInscription.trim().ifBlank { "Discovered during a SideQuest expedition." }.take(600),
            "category" to category.id,
            "rarity" to validRarity,
            "landmarkName" to landmarkName.trim().ifBlank { "Local Waypoint" }.take(140),
            "regionName" to regionName.trim().ifBlank { "Local Sector" }.take(120),
            "characterStyle" to characterStyle.id,
            "latitude" to latitude.coerceIn(-90.0, 90.0),
            "longitude" to longitude.coerceIn(-180.0, 180.0),
            "resonanceValue" to resonanceValue.coerceIn(1, 1000),
            "isFavorite" to isFavorite,
            "inspectionNotes" to inspectionNotes.trim().take(400),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleFavoriteArtifact(artifactId: String, isFavorite: Boolean) {
        val uid = requireUserId()
        require(artifactId.isNotBlank()) { "artifactId must not be blank" }
        val docRef = db.collection("users").document(uid).collection("artifacts").document(artifactId)
        try {
            docRef.update(
                mapOf(
                    "isFavorite" to isFavorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateArtifactInspectionNotes(artifactId: String, notes: String) {
        val uid = requireUserId()
        require(artifactId.isNotBlank()) { "artifactId must not be blank" }
        val docRef = db.collection("users").document(uid).collection("artifacts").document(artifactId)
        try {
            docRef.update(
                mapOf(
                    "inspectionNotes" to notes.trim().take(400),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteFoundArtifact(artifactId: String) {
        val uid = requireUserId()
        require(artifactId.isNotBlank()) { "artifactId must not be blank" }
        val docRef = db.collection("users").document(uid).collection("artifacts").document(artifactId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun getQuestById(userId: String, questId: String): CompletedQuest? {
        val docRef = db.collection("users").document(userId).collection("quests").document(questId)
        return try {
            val snap = docRef.get().await()
            snap.toObject(
                CompletedQuest::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }
}
