package com.example.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Model-Agnostic Capability Tiers & Free-Tier Guardrails.
 * Centralizes all model identifiers, capability roles, and quota thresholds
 * so models can be changed without editing UI screens or ViewModels.
 */
object AiModelConfig {

    // Tier 1 — Primary Standard Models (used when user has healthy quota and Eco Mode is off)
    const val PRIMARY_REASONING_MODEL = "gemini-3.8-flash"
    const val PRIMARY_IMAGE_MODEL = "gemini-nano-banana-2.1"

    // Tier 2 — High-Efficiency Lite Models (auto-activated on 429, >=80% quota, or Eco Mode toggle)
    const val ECONOMY_LITE_MODEL = "gemini-3.5-flash-lite"
    const val ECONOMY_IMAGE_MODEL = "gemini-3.1-flash-lite-image"

    // Tier 3 — Local Deterministic Identifier
    const val LOCAL_REFLEX_MODEL = "local-reflex-v1"

    // Free Tier Standard Quota Limits (Per Day)
    const val DAILY_FREE_TOTAL_STANDARD_LIMIT = 25
    const val DAILY_FREE_VISION_LIMIT = 8
    const val DAILY_FREE_CHAT_LIMIT = 20
    const val DAILY_FREE_STUDIO_LIMIT = 4
    const val DAILY_FREE_AUDIO_LIMIT = 10

    const val FREE_DAILY_VISION_LIMIT = DAILY_FREE_VISION_LIMIT
    const val FREE_DAILY_CHAT_LIMIT = DAILY_FREE_CHAT_LIMIT
    const val FREE_DAILY_STUDIO_LIMIT = DAILY_FREE_STUDIO_LIMIT
    const val FREE_DAILY_AUDIO_LIMIT = DAILY_FREE_AUDIO_LIMIT

    // Grace Credit Allowance (Awarded once per day mid-task)
    const val GRACE_CREDITS_AMOUNT = 3

    // Near-Limit Soft Nudge Trigger (80% usage threshold)
    const val NEAR_LIMIT_RATIO = 0.80f

    // Fingerprint Deduplication Cache Window (30-45s TTL)
    const val DEDUPLICATION_CACHE_TTL_MS = 45_000L

    // Media Downscaling Constraints
    const val MAX_IMAGE_DIMENSION_PX = 1024
    const val LITE_IMAGE_DIMENSION_PX = 768
    const val MAX_VOICE_NOTE_DURATION_SEC = 300 // 5 minutes cap

    // Sliding Context Window Turn Limit
    const val MAX_CHAT_HISTORY_TURNS = 6

    enum class AiCapability(val label: String, val dailyFeatureLimit: Int) {
        VISION_PROOF("Vision Verification", DAILY_FREE_VISION_LIMIT),
        COMPANION_CHAT("Dungeon Master Audio Comms", DAILY_FREE_CHAT_LIMIT),
        AUDIO_FIELD_NOTE("Voice Field Note", DAILY_FREE_AUDIO_LIMIT),
        IMAGE_STUDIO("Relic Art Studio", DAILY_FREE_STUDIO_LIMIT),
        FACT_DOSSIER("Fact-Check Dossier", DAILY_FREE_CHAT_LIMIT),
        RIDDLE_CIPHER("Riddle Duel", DAILY_FREE_CHAT_LIMIT),
        QUEST_NARRATIVE("AI Quest Synthesis", DAILY_FREE_CHAT_LIMIT)
    }

    enum class AiExecutionTier(val label: String, val badge: String) {
        TIER_1_PRIMARY("Primary Standard", "STANDARD"),
        TIER_2_ECONOMY_LITE("Eco Lite Mode", "ECO LITE"),
        TIER_3_LOCAL_FALLBACK("Zero-Cost Local", "LOCAL REFLEX");

        companion object {
            val TIER_1_STANDARD = TIER_1_PRIMARY
            val TIER_2_LITE = TIER_2_ECONOMY_LITE
        }
    }

    /**
     * Resolves the target model string based on capability, user settings, and quota state.
     */
    fun resolveModel(
        capability: AiCapability,
        isEcoLiteMode: Boolean,
        isNearLimit: Boolean,
        isQuotaExhausted: Boolean
    ): Pair<String, AiExecutionTier> {
        if (isQuotaExhausted) {
            return Pair(LOCAL_REFLEX_MODEL, AiExecutionTier.TIER_3_LOCAL_FALLBACK)
        }
        if (isEcoLiteMode || isNearLimit) {
            val model = when (capability) {
                AiCapability.IMAGE_STUDIO -> ECONOMY_IMAGE_MODEL
                else -> ECONOMY_LITE_MODEL
            }
            return Pair(model, AiExecutionTier.TIER_2_ECONOMY_LITE)
        }
        val model = when (capability) {
            AiCapability.IMAGE_STUDIO -> PRIMARY_IMAGE_MODEL
            else -> PRIMARY_REASONING_MODEL
        }
        return Pair(model, AiExecutionTier.TIER_1_PRIMARY)
    }

    /**
     * Calculates the hours and minutes remaining until the next midnight rollover.
     */
    fun calculateTimeUntilMidnightReset(): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = midnight.timeInMillis - now.timeInMillis
        val totalMinutes = (diffMillis / (1000 * 60)).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return Pair(hours, minutes)
    }

    /**
     * Formats the time until midnight as a user-friendly string (e.g., "5h 20m").
     */
    fun formatTimeUntilMidnightReset(): String {
        val (hours, minutes) = calculateTimeUntilMidnightReset()
        return "${hours}h ${minutes}m"
    }

    /**
     * Formats today's date ISO (YYYY-MM-DD).
     */
    fun getTodayDateIso(): String {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }
}
