package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.QuotaAcquisitionResult
import com.example.model.UsageQuotaLedger
import com.example.util.AiModelConfig
import com.example.util.AiModelConfig.AiCapability
import com.example.util.AiModelConfig.AiExecutionTier
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "UsageQuotaManager"
private const val PREFS_NAME = "sidequest_ai_quota_mirror"

class UsageQuotaManager(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // In-memory state backed immediately by the local SharedPreferences mirror
    private val _quotaState = MutableStateFlow(loadFromLocalMirror())
    val quotaState: StateFlow<UsageQuotaLedger> = _quotaState.asStateFlow()

    private val auth get() = Firebase.auth

    private fun loadFromLocalMirror(): UsageQuotaLedger {
        val today = AiModelConfig.getTodayDateIso()
        val cachedDate = prefs.getString("dateIso", "") ?: ""
        val ecoLiteMode = prefs.getBoolean("ecoLiteMode", false)

        if (cachedDate != today) {
            // Rollover detected locally
            return UsageQuotaLedger(
                dateIso = today,
                ecoLiteMode = ecoLiteMode
            )
        }

        return UsageQuotaLedger(
            dateIso = today,
            visionUsed = prefs.getLong("visionUsed", 0L),
            chatUsed = prefs.getLong("chatUsed", 0L),
            studioUsed = prefs.getLong("studioUsed", 0L),
            audioUsed = prefs.getLong("audioUsed", 0L),
            bonusGraceCredits = prefs.getLong("bonusGraceCredits", 0L),
            graceClaimed = prefs.getBoolean("graceClaimed", false),
            ecoLiteMode = ecoLiteMode,
            fallbackCascadeCount = prefs.getLong("fallbackCascadeCount", 0L)
        )
    }

    private fun saveToLocalMirror(ledger: UsageQuotaLedger) {
        prefs.edit()
            .putString("dateIso", ledger.dateIso)
            .putLong("visionUsed", ledger.visionUsed)
            .putLong("chatUsed", ledger.chatUsed)
            .putLong("studioUsed", ledger.studioUsed)
            .putLong("audioUsed", ledger.audioUsed)
            .putLong("bonusGraceCredits", ledger.bonusGraceCredits)
            .putBoolean("graceClaimed", ledger.graceClaimed)
            .putBoolean("ecoLiteMode", ledger.ecoLiteMode)
            .putLong("fallbackCascadeCount", ledger.fallbackCascadeCount)
            .apply()
    }

    private fun isAuthenticatedFor(userId: String?): Boolean {
        return !userId.isNullOrBlank() && auth.currentUser?.uid == userId
    }

    /**
     * Observes the Firestore source of truth at `/users/{userId}/usageQuota/daily`.
     * Automatically syncs changes to the local mirror and triggers period rollovers when dateIso changes.
     * In Guest Explorer mode (unauthenticated), returns the local SharedPreferences mirror flow without hitting Firestore.
     */
    fun observeUserQuota(userId: String): Flow<UsageQuotaLedger> {
        if (!isAuthenticatedFor(userId)) {
            return quotaState
        }

        val path = "users/$userId/usageQuota/daily"
        val docRef = db.collection("users").document(userId).collection("usageQuota").document("daily")

        return docRef.snapshots().map { snapshot ->
            val today = AiModelConfig.getTodayDateIso()
            if (!snapshot.exists()) {
                val initial = UsageQuotaLedger(userId = userId, dateIso = today, ecoLiteMode = _quotaState.value.ecoLiteMode)
                if (isAuthenticatedFor(userId)) {
                    scope.launch {
                        try {
                            docRef.set(initial).await()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to initialize quota doc in Firestore", e)
                        }
                    }
                }
                saveToLocalMirror(initial)
                _quotaState.value = initial
                initial
            } else {
                val stored = snapshot.toObject(UsageQuotaLedger::class.java) ?: UsageQuotaLedger()
                if (stored.dateIso != today) {
                    // Automatic rollover reset on date change
                    val rolledOver = stored.copy(
                        dateIso = today,
                        visionUsed = 0L,
                        chatUsed = 0L,
                        studioUsed = 0L,
                        audioUsed = 0L,
                        bonusGraceCredits = 0L,
                        graceClaimed = false,
                        // Preserves user preference
                        ecoLiteMode = stored.ecoLiteMode,
                        fallbackCascadeCount = 0L
                    )
                    if (isAuthenticatedFor(userId)) {
                        scope.launch {
                            try {
                                docRef.set(rolledOver).await()
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to rollover quota in Firestore", e)
                            }
                        }
                    }
                    saveToLocalMirror(rolledOver)
                    _quotaState.value = rolledOver
                    rolledOver
                } else {
                    saveToLocalMirror(stored)
                    _quotaState.value = stored
                    stored
                }
            }
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && isAuthenticatedFor(userId)) {
                handleFirestoreError(exceptionToLog, OperationType.GET, path)
            }
            // Fallback to local mirror on network disconnection
            emit(_quotaState.value)
        }
    }

    /**
     * Checks quota availability and determines the capability tier to use.
     */
    fun checkQuotaTier(capability: AiCapability): Pair<AiExecutionTier, Boolean> {
        val current = _quotaState.value
        val isExhausted = current.isExhausted
        val isNearLimit = current.isNearLimit
        val isEco = current.ecoLiteMode

        val (_, tier) = AiModelConfig.resolveModel(
            capability = capability,
            isEcoLiteMode = isEco,
            isNearLimit = isNearLimit,
            isQuotaExhausted = isExhausted
        )
        return Pair(tier, !isExhausted)
    }

    /**
     * Evaluates quota tier and wraps into QuotaAcquisitionResult for typed recovery handling.
     */
    fun evaluateTierForCapability(capability: AiCapability): QuotaAcquisitionResult {
        val (tier, allowed) = checkQuotaTier(capability)
        return if (allowed) {
            QuotaAcquisitionResult.Allowed(tier)
        } else {
            QuotaAcquisitionResult.Exhausted("Daily free-tier quota limit reached")
        }
    }

    /**
     * Atomic Deduct: Records a successful AI generation and charges the corresponding quota counter.
     */
    suspend fun recordUsageSuccess(capability: AiCapability, userId: String? = auth.currentUser?.uid) {
        val current = _quotaState.value
        val today = AiModelConfig.getTodayDateIso()
        val rolled = if (current.dateIso != today) {
            current.copy(
                dateIso = today,
                visionUsed = 0L,
                chatUsed = 0L,
                studioUsed = 0L,
                audioUsed = 0L,
                bonusGraceCredits = 0L,
                graceClaimed = false,
                fallbackCascadeCount = 0L
            )
        } else current

        val updated = when (capability) {
            AiCapability.VISION_PROOF -> rolled.copy(visionUsed = rolled.visionUsed + 1)
            AiCapability.COMPANION_CHAT, AiCapability.FACT_DOSSIER, AiCapability.RIDDLE_CIPHER, AiCapability.QUEST_NARRATIVE ->
                rolled.copy(chatUsed = rolled.chatUsed + 1)
            AiCapability.AUDIO_FIELD_NOTE -> rolled.copy(audioUsed = rolled.audioUsed + 1)
            AiCapability.IMAGE_STUDIO -> rolled.copy(studioUsed = rolled.studioUsed + 1)
        }

        _quotaState.value = updated
        saveToLocalMirror(updated)

        if (isAuthenticatedFor(userId) && userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    val field = when (capability) {
                        AiCapability.VISION_PROOF -> "visionUsed"
                        AiCapability.COMPANION_CHAT, AiCapability.FACT_DOSSIER, AiCapability.RIDDLE_CIPHER, AiCapability.QUEST_NARRATIVE -> "chatUsed"
                        AiCapability.AUDIO_FIELD_NOTE -> "audioUsed"
                        AiCapability.IMAGE_STUDIO -> "studioUsed"
                    }
                    val docRef = db.collection("users").document(userId).collection("usageQuota").document("daily")
                    docRef.set(
                        mapOf(
                            "userId" to userId,
                            "dateIso" to today,
                            field to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to increment usage in Firestore", e)
                }
            }
        }
    }

    /**
     * Atomic Refund: Restores a charged credit if an AI generation call fails, cancels, or times out.
     */
    suspend fun refundUsage(capability: AiCapability, userId: String? = auth.currentUser?.uid) {
        val current = _quotaState.value
        val updated = when (capability) {
            AiCapability.VISION_PROOF -> current.copy(visionUsed = maxOf(0L, current.visionUsed - 1))
            AiCapability.COMPANION_CHAT, AiCapability.FACT_DOSSIER, AiCapability.RIDDLE_CIPHER, AiCapability.QUEST_NARRATIVE ->
                current.copy(chatUsed = maxOf(0L, current.chatUsed - 1))
            AiCapability.AUDIO_FIELD_NOTE -> current.copy(audioUsed = maxOf(0L, current.audioUsed - 1))
            AiCapability.IMAGE_STUDIO -> current.copy(studioUsed = maxOf(0L, current.studioUsed - 1))
        }

        _quotaState.value = updated
        saveToLocalMirror(updated)

        if (isAuthenticatedFor(userId) && userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    val field = when (capability) {
                        AiCapability.VISION_PROOF -> "visionUsed"
                        AiCapability.COMPANION_CHAT, AiCapability.FACT_DOSSIER, AiCapability.RIDDLE_CIPHER, AiCapability.QUEST_NARRATIVE -> "chatUsed"
                        AiCapability.AUDIO_FIELD_NOTE -> "audioUsed"
                        AiCapability.IMAGE_STUDIO -> "studioUsed"
                    }
                    val docRef = db.collection("users").document(userId).collection("usageQuota").document("daily")
                    db.runTransaction { tx ->
                        val snap = tx.get(docRef)
                        val remoteCount = snap.getLong(field) ?: 0L
                        val clamped = maxOf(0L, remoteCount - 1L)
                        tx.set(
                            docRef,
                            mapOf(
                                "userId" to userId,
                                "dateIso" to updated.dateIso,
                                field to clamped,
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        )
                    }.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to refund usage in Firestore", e)
                }
            }
        }
    }

    /**
     * Claims +3 One-Time Daily Grace Credits for mid-task continuity.
     */
    suspend fun claimDailyGraceCredits(userId: String? = auth.currentUser?.uid): Boolean {
        val current = _quotaState.value
        if (current.graceClaimed) return false

        val updated = current.copy(
            bonusGraceCredits = current.bonusGraceCredits + AiModelConfig.GRACE_CREDITS_AMOUNT,
            graceClaimed = true
        )
        _quotaState.value = updated
        saveToLocalMirror(updated)

        if (isAuthenticatedFor(userId) && userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    val docRef = db.collection("users").document(userId).collection("usageQuota").document("daily")
                    docRef.set(
                        mapOf(
                            "userId" to userId,
                            "dateIso" to updated.dateIso,
                            "bonusGraceCredits" to FieldValue.increment(AiModelConfig.GRACE_CREDITS_AMOUNT.toLong()),
                            "graceClaimed" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to save grace credits to Firestore", e)
                }
            }
        }
        return true
    }

    /**
     * Toggles High-Efficiency Eco Lite Mode (stretching remaining quota 3x-5x further).
     */
    suspend fun toggleEcoLiteMode(enabled: Boolean, userId: String? = auth.currentUser?.uid) {
        val current = _quotaState.value.copy(ecoLiteMode = enabled)
        _quotaState.value = current
        saveToLocalMirror(current)

        if (isAuthenticatedFor(userId) && userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    val docRef = db.collection("users").document(userId).collection("usageQuota").document("daily")
                    docRef.set(
                        mapOf(
                            "userId" to userId,
                            "dateIso" to current.dateIso,
                            "ecoLiteMode" to enabled,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update ecoLiteMode in Firestore", e)
                }
            }
        }
    }

    /**
     * Increments the fallback cascade counter whenever an auto-cascade or fallback is triggered.
     */
    suspend fun recordFallbackCascade(userId: String? = auth.currentUser?.uid) {
        val current = _quotaState.value.copy(fallbackCascadeCount = _quotaState.value.fallbackCascadeCount + 1)
        _quotaState.value = current
        saveToLocalMirror(current)

        if (isAuthenticatedFor(userId) && userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    val docRef = db.collection("users").document(userId).collection("usageQuota").document("daily")
                    docRef.set(
                        mapOf(
                            "userId" to userId,
                            "dateIso" to current.dateIso,
                            "fallbackCascadeCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to record fallback cascade in Firestore", e)
                }
            }
        }
    }
}
