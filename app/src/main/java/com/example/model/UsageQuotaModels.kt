package com.example.model

import com.example.util.AiModelConfig
import com.example.util.AiModelConfig.AiCapability
import com.example.util.AiModelConfig.AiExecutionTier
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore-backed Quota Ledger model representing the user's daily AI usage.
 * Stored at `/users/{userId}/usageQuota/daily` with automatic period rollover.
 */
@IgnoreExtraProperties
data class UsageQuotaLedger(
    @DocumentId val id: String = "daily",
    val userId: String = "",
    val dateIso: String = AiModelConfig.getTodayDateIso(),
    val visionUsed: Long = 0L,
    val chatUsed: Long = 0L,
    val studioUsed: Long = 0L,
    val audioUsed: Long = 0L,
    val bonusGraceCredits: Long = 0L,
    val graceClaimed: Boolean = false,
    val ecoLiteMode: Boolean = false,
    val fallbackCascadeCount: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalUsed: Long
        get() = visionUsed + chatUsed + studioUsed + audioUsed

    val effectiveLimit: Long
        get() = AiModelConfig.DAILY_FREE_TOTAL_STANDARD_LIMIT + bonusGraceCredits

    val remainingCredits: Long
        get() = maxOf(0L, effectiveLimit - totalUsed)

    val usageRatio: Float
        get() = if (effectiveLimit > 0) totalUsed.toFloat() / effectiveLimit else 1f

    val isNearLimit: Boolean
        get() = usageRatio >= AiModelConfig.NEAR_LIMIT_RATIO && !isExhausted

    val isExhausted: Boolean
        get() = totalUsed >= effectiveLimit

    val resetsInFormatted: String
        get() = AiModelConfig.formatTimeUntilMidnightReset()

    fun usageFor(capability: AiCapability): Long = when (capability) {
        AiCapability.VISION_PROOF -> visionUsed
        AiCapability.COMPANION_CHAT, AiCapability.FACT_DOSSIER, AiCapability.RIDDLE_CIPHER, AiCapability.QUEST_NARRATIVE -> chatUsed
        AiCapability.AUDIO_FIELD_NOTE -> audioUsed
        AiCapability.IMAGE_STUDIO -> studioUsed
    }

    fun getDailyChecksUsed(): Int = totalUsed.toInt()
    fun getDailyLimit(): Int = effectiveLimit.toInt()
    fun getResetCountdownFormatted(): String = resetsInFormatted
    fun canClaimGraceCredits(): Boolean = !graceClaimed
    fun isSoftNudgeThresholdReached(): Boolean = isNearLimit && !ecoLiteMode
}

/**
 * Personal "Value Receipt" displayed to the user summarizing the real utility
 * provided by the AI before offering conversion or quota options.
 */
data class AiValueReceipt(
    val dateIso: String = AiModelConfig.getTodayDateIso(),
    val landmarksVerified: Int = 0,
    val dmChatsExchanged: Int = 0,
    val relicsCrafted: Int = 0,
    val voiceNotesLogged: Int = 0,
    val questsCompleted: Int = 0,
    val distanceMetersWalked: Long = 0L,
    val totalXpEarned: Long = 0L
)

/**
 * State representing an active Smart Quota Recovery prompt.
 */
data class SmartQuotaRecoveryState(
    val isVisible: Boolean = false,
    val triggeringCapability: AiCapability = AiCapability.COMPANION_CHAT,
    val isRateLimited429: Boolean = false,
    val ledger: UsageQuotaLedger = UsageQuotaLedger(),
    val customContextMessage: String? = null
)

/**
 * Result of an atomic quota deduction request.
 */
sealed class QuotaAcquisitionResult {
    abstract val allowed: Boolean
    abstract val executionTier: AiExecutionTier

    data class Allowed(
        val tier: AiExecutionTier = AiExecutionTier.TIER_1_PRIMARY
    ) : QuotaAcquisitionResult() {
        override val allowed: Boolean = true
        override val executionTier: AiExecutionTier = tier
    }

    data class Exhausted(
        val reason: String = "Daily free-tier quota limit reached"
    ) : QuotaAcquisitionResult() {
        override val allowed: Boolean = false
        override val executionTier: AiExecutionTier = AiExecutionTier.TIER_3_LOCAL_FALLBACK
    }
}
