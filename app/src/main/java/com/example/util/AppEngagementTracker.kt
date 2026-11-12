package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppEventType(val label: String, val satisfactionWeight: Int) {
    APP_LAUNCH("App Session Started", 0),
    ONBOARDING_COMPLETED("Onboarding Tour Completed", 1),
    QUEST_GENERATED("AI Walking Quest Generated", 1),
    WAYPOINT_CLEARED("Landmark Waypoint Cleared", 2),
    QUEST_COMPLETED("Full Walking Quest Completed", 4),
    VISION_SCAN_VERIFIED("Gemini Vision Plaque Verified", 3),
    RIDDLE_SOLVED("AI Landmark Riddle Solved", 3),
    ARTIFACT_CATALOGED("Artifact Cataloged to Firestore", 2),
    DONATION_CLICKED("Support Channel Opened", 2),
    PROMPT_SHOWN("Smart Suggestion Shown", 0),
    PROMPT_ACCEPTED("Smart Suggestion Accepted", 1),
    PROMPT_DISMISSED("Smart Suggestion Dismissed (Cooldown Applied)", 0)
}

data class LoggedAppEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: AppEventType,
    val detail: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() = try {
            SimpleDateFormat("MMM d, h:mm:ss a", Locale.US).format(Date(timestampEpochMs))
        } catch (_: Exception) {
            "Just now"
        }
}

enum class SmartPromptType(val title: String) {
    NONE("None"),
    RATE_APP("Enjoying SideQuest? Rate the App"),
    SHARE_APP("Share Your Explorer Chronicle"),
    UPDATE_APP("New SideQuest Update Available")
}

data class EngagementTelemetryState(
    val installedVersionCode: Int = BuildConfig.VERSION_CODE,
    val installedVersionName: String = BuildConfig.VERSION_NAME,
    val availableUpdateVersionCode: Int = BuildConfig.VERSION_CODE + 1,
    val availableUpdateVersionName: String = "1.1",
    val isOnboardingCompleted: Boolean = false,
    val appLaunchCount: Int = 2,
    val daysSinceInstall: Int = 3,
    val positiveValueMoments: Int = 0,
    val satisfactionScore: Int = 0,
    val promptsShownThisSession: Int = 0,
    val hasRatedApp: Boolean = false,
    val ratePromptShownCount: Int = 0,
    val lastRatePromptDismissedEpochMs: Long = 0L,
    val sharePromptShownCount: Int = 0,
    val lastSharePromptDismissedEpochMs: Long = 0L,
    val updatePromptShownCount: Int = 0,
    val lastUpdatePromptDismissedEpochMs: Long = 0L,
    val activeAutoPrompt: SmartPromptType = SmartPromptType.NONE,
    val lastDecisionReason: String = "Startup Guard Active: Never show pop-ups at app start.",
    val eventHistory: List<LoggedAppEvent> = emptyList()
)

/**
 * Tracks application events and implements a strict, non-intrusive algorithm for choosing
 * when to show Rate App, Share App, or Update App suggestions.
 *
 * Key Anti-Spam Safeguards:
 * 1. NEVER triggers at app startup (`APP_LAUNCH` never opens a pop-up).
 * 2. Hard cap of 1 automatic pop-up per app session (`MAX_PROMPTS_PER_SESSION = 1`).
 * 3. Only evaluates after verified high-satisfaction value realization moments
 *    (completing a quest, clearing waypoints, solving a riddle, or verifying a vision scan).
 * 4. Enforces multi-day cooldowns (21 days for Rate, 14 days for Share, 7 days for Update)
 *    and lifetime impression caps so prompts never repeat annoyingly.
 */
class AppEngagementTracker(context: Context? = null) {

    private val prefs: SharedPreferences? = try {
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<EngagementTelemetryState> = _state.asStateFlow()

    init {
        logEvent(
            type = AppEventType.APP_LAUNCH,
            detail = "Session initialized on v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}). Startup pop-up block enforced.",
            allowAutoPromptEvaluation = false
        )
    }

    private fun loadInitialState(): EngagementTelemetryState {
        val p = prefs ?: return EngagementTelemetryState()
        val now = System.currentTimeMillis()
        val firstInstall = p.getLong(KEY_FIRST_INSTALL_MS, now - (3L * DAY_MS))
        if (!p.contains(KEY_FIRST_INSTALL_MS)) {
            p.edit().putLong(KEY_FIRST_INSTALL_MS, firstInstall).apply()
        }
        val launches = (p.getInt(KEY_LAUNCH_COUNT, 2) + 1).coerceAtLeast(1)
        p.edit().putInt(KEY_LAUNCH_COUNT, launches).apply()
        val days = ((now - firstInstall) / DAY_MS).toInt().coerceAtLeast(2)

        return EngagementTelemetryState(
            isOnboardingCompleted = p.getBoolean(KEY_ONBOARDING_COMPLETED, false),
            appLaunchCount = launches,
            daysSinceInstall = days,
            positiveValueMoments = p.getInt(KEY_POSITIVE_MOMENTS, 0),
            satisfactionScore = p.getInt(KEY_SATISFACTION_SCORE, 0),
            promptsShownThisSession = 0,
            hasRatedApp = p.getBoolean(KEY_HAS_RATED, false),
            ratePromptShownCount = p.getInt(KEY_RATE_SHOWN_COUNT, 0),
            lastRatePromptDismissedEpochMs = p.getLong(KEY_LAST_RATE_DISMISSED_MS, 0L),
            sharePromptShownCount = p.getInt(KEY_SHARE_SHOWN_COUNT, 0),
            lastSharePromptDismissedEpochMs = p.getLong(KEY_LAST_SHARE_DISMISSED_MS, 0L),
            updatePromptShownCount = p.getInt(KEY_UPDATE_SHOWN_COUNT, 0),
            lastUpdatePromptDismissedEpochMs = p.getLong(KEY_LAST_UPDATE_DISMISSED_MS, 0L)
        )
    }

    fun markOnboardingCompleted() {
        prefs?.edit()?.putBoolean(KEY_ONBOARDING_COMPLETED, true)?.apply()
        _state.value = _state.value.copy(isOnboardingCompleted = true)
        logEvent(
            type = AppEventType.ONBOARDING_COMPLETED,
            detail = "Explorer completed the interactive SideQuest onboarding guide.",
            allowAutoPromptEvaluation = false
        )
    }

    /**
     * Logs a structured app event and optionally evaluates whether this value realization moment
     * qualifies for a single, non-intrusive suggestion dialog.
     */
    fun logEvent(
        type: AppEventType,
        detail: String,
        allowAutoPromptEvaluation: Boolean = true,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        val current = _state.value
        val newEntry = LoggedAppEvent(
            type = type,
            detail = detail,
            timestampEpochMs = nowEpochMs
        )
        val isPositiveMoment = type.satisfactionWeight >= 2
        val nextMoments = if (isPositiveMoment) {
            current.positiveValueMoments + 1
        } else {
            current.positiveValueMoments
        }
        val nextScore = (current.satisfactionScore + type.satisfactionWeight).coerceAtLeast(0)

        prefs?.edit()
            ?.putInt(KEY_POSITIVE_MOMENTS, nextMoments)
            ?.putInt(KEY_SATISFACTION_SCORE, nextScore)
            ?.apply()

        val updatedHistory = (listOf(newEntry) + current.eventHistory).take(MAX_EVENT_HISTORY)
        var nextState = current.copy(
            positiveValueMoments = nextMoments,
            satisfactionScore = nextScore,
            eventHistory = updatedHistory
        )

        if (allowAutoPromptEvaluation && isPositiveMoment) {
            val (chosenPrompt, reason) = evaluateBestPrompt(nextState, type, nowEpochMs)
            nextState = if (chosenPrompt != SmartPromptType.NONE) {
                recordPromptShownInternal(nextState, chosenPrompt, reason, nowEpochMs)
            } else {
                nextState.copy(lastDecisionReason = reason)
            }
        } else if (type == AppEventType.APP_LAUNCH) {
            nextState = nextState.copy(
                activeAutoPrompt = SmartPromptType.NONE,
                lastDecisionReason = "App Launch Guard: Never show Rate/Share/Update pop-ups at startup."
            )
        }

        _state.value = nextState
    }

    /**
     * Pure decision algorithm that selects at most ONE optimal prompt after a positive user moment,
     * while enforcing session caps, minimum engagement thresholds, lifetime limits, and multi-day cooldowns.
     */
    fun evaluateBestPrompt(
        snapshot: EngagementTelemetryState,
        triggerEvent: AppEventType,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Pair<SmartPromptType, String> {
        // 1. Never show at app startup
        if (triggerEvent == AppEventType.APP_LAUNCH) {
            return SmartPromptType.NONE to "Suppressed: Never show pop-ups at app startup."
        }

        // 2. Never stack or show more than 1 prompt per session
        if (snapshot.activeAutoPrompt != SmartPromptType.NONE) {
            return SmartPromptType.NONE to "Suppressed: A prompt is already active."
        }
        if (snapshot.promptsShownThisSession >= MAX_PROMPTS_PER_SESSION) {
            return SmartPromptType.NONE to "Suppressed: Session limit reached (${snapshot.promptsShownThisSession}/$MAX_PROMPTS_PER_SESSION shown this session)."
        }

        // 3. Require verified user maturity & satisfaction before any prompt
        if (snapshot.appLaunchCount < MIN_LAUNCHES_BEFORE_PROMPT || snapshot.daysSinceInstall < MIN_DAYS_BEFORE_PROMPT) {
            return SmartPromptType.NONE to "Suppressed: User maturity threshold not yet met (${snapshot.appLaunchCount} launches, ${snapshot.daysSinceInstall}d)."
        }
        if (snapshot.positiveValueMoments < MIN_POSITIVE_MOMENTS_FOR_RATE) {
            return SmartPromptType.NONE to "Building value first: ${snapshot.positiveValueMoments}/$MIN_POSITIVE_MOMENTS_FOR_RATE positive milestones completed."
        }

        // 4. Candidate 1: Rate App (Highest priority at 3+ positive moments & satisfactionScore >= 6)
        val rateCooldownElapsed = (nowEpochMs - snapshot.lastRatePromptDismissedEpochMs) >= RATE_COOLDOWN_MS
        if (!snapshot.hasRatedApp &&
            snapshot.ratePromptShownCount < MAX_LIFETIME_RATE_PROMPTS &&
            rateCooldownElapsed &&
            snapshot.satisfactionScore >= 6
        ) {
            return SmartPromptType.RATE_APP to "Triggered RATE_APP after high-satisfaction moment (${triggerEvent.label}, score=${snapshot.satisfactionScore})."
        }

        // 5. Candidate 2: Share App (When user hits 5+ positive moments or completes a full quest/riddle)
        val shareCooldownElapsed = (nowEpochMs - snapshot.lastSharePromptDismissedEpochMs) >= SHARE_COOLDOWN_MS
        val isShareWorthyMilestone = snapshot.positiveValueMoments >= MIN_POSITIVE_MOMENTS_FOR_SHARE ||
            triggerEvent == AppEventType.QUEST_COMPLETED
        if (isShareWorthyMilestone &&
            snapshot.sharePromptShownCount < MAX_LIFETIME_SHARE_PROMPTS &&
            shareCooldownElapsed
        ) {
            return SmartPromptType.SHARE_APP to "Triggered SHARE_APP at explorer milestone (${snapshot.positiveValueMoments} milestones)."
        }

        // 6. Candidate 3: Update App (When remote version > installed version and 6+ positive moments)
        val hasNewerVersion = snapshot.availableUpdateVersionCode > snapshot.installedVersionCode
        val updateCooldownElapsed = (nowEpochMs - snapshot.lastUpdatePromptDismissedEpochMs) >= UPDATE_COOLDOWN_MS
        if (hasNewerVersion &&
            snapshot.positiveValueMoments >= MIN_POSITIVE_MOMENTS_FOR_UPDATE &&
            snapshot.updatePromptShownCount < MAX_LIFETIME_UPDATE_PROMPTS &&
            updateCooldownElapsed
        ) {
            return SmartPromptType.UPDATE_APP to "Triggered UPDATE_APP (v${snapshot.availableUpdateVersionName} available after ${snapshot.positiveValueMoments} tasks)."
        }

        return SmartPromptType.NONE to "No prompt triggered (cooldowns or lifetime caps active)."
    }

    private fun recordPromptShownInternal(
        current: EngagementTelemetryState,
        promptType: SmartPromptType,
        reason: String,
        nowEpochMs: Long
    ): EngagementTelemetryState {
        val shownEvent = LoggedAppEvent(
            type = AppEventType.PROMPT_SHOWN,
            detail = "${promptType.name}: $reason",
            timestampEpochMs = nowEpochMs
        )
        val nextRateCount = if (promptType == SmartPromptType.RATE_APP) current.ratePromptShownCount + 1 else current.ratePromptShownCount
        val nextShareCount = if (promptType == SmartPromptType.SHARE_APP) current.sharePromptShownCount + 1 else current.sharePromptShownCount
        val nextUpdateCount = if (promptType == SmartPromptType.UPDATE_APP) current.updatePromptShownCount + 1 else current.updatePromptShownCount

        prefs?.edit()
            ?.putInt(KEY_RATE_SHOWN_COUNT, nextRateCount)
            ?.putInt(KEY_SHARE_SHOWN_COUNT, nextShareCount)
            ?.putInt(KEY_UPDATE_SHOWN_COUNT, nextUpdateCount)
            ?.apply()

        return current.copy(
            promptsShownThisSession = current.promptsShownThisSession + 1,
            ratePromptShownCount = nextRateCount,
            sharePromptShownCount = nextShareCount,
            updatePromptShownCount = nextUpdateCount,
            activeAutoPrompt = promptType,
            lastDecisionReason = reason,
            eventHistory = (listOf(shownEvent) + current.eventHistory).take(MAX_EVENT_HISTORY)
        )
    }

    /**
     * Allows user-initiated opening of Rate, Share, or Update dialogs from the Settings screen.
     */
    fun openPromptManuallyFromSettings(promptType: SmartPromptType) {
        _state.value = _state.value.copy(
            activeAutoPrompt = promptType,
            lastDecisionReason = "User opened ${promptType.title} manually from Settings."
        )
    }

    fun onPromptAccepted(promptType: SmartPromptType, detail: String = "") {
        val now = System.currentTimeMillis()
        val current = _state.value
        val markRated = promptType == SmartPromptType.RATE_APP || current.hasRatedApp
        if (markRated) {
            prefs?.edit()?.putBoolean(KEY_HAS_RATED, true)?.apply()
        }
        val acceptedEvent = LoggedAppEvent(
            type = AppEventType.PROMPT_ACCEPTED,
            detail = "${promptType.name} accepted. $detail".trim(),
            timestampEpochMs = now
        )
        _state.value = current.copy(
            hasRatedApp = markRated,
            activeAutoPrompt = SmartPromptType.NONE,
            lastDecisionReason = "User completed ${promptType.title}.",
            eventHistory = (listOf(acceptedEvent) + current.eventHistory).take(MAX_EVENT_HISTORY)
        )
    }

    fun onPromptDismissed(promptType: SmartPromptType) {
        val now = System.currentTimeMillis()
        val current = _state.value
        val nextRateDismiss = if (promptType == SmartPromptType.RATE_APP) now else current.lastRatePromptDismissedEpochMs
        val nextShareDismiss = if (promptType == SmartPromptType.SHARE_APP) now else current.lastSharePromptDismissedEpochMs
        val nextUpdateDismiss = if (promptType == SmartPromptType.UPDATE_APP) now else current.lastUpdatePromptDismissedEpochMs

        prefs?.edit()
            ?.putLong(KEY_LAST_RATE_DISMISSED_MS, nextRateDismiss)
            ?.putLong(KEY_LAST_SHARE_DISMISSED_MS, nextShareDismiss)
            ?.putLong(KEY_LAST_UPDATE_DISMISSED_MS, nextUpdateDismiss)
            ?.apply()

        val cooldownDays = when (promptType) {
            SmartPromptType.RATE_APP -> 21
            SmartPromptType.SHARE_APP -> 14
            SmartPromptType.UPDATE_APP -> 7
            SmartPromptType.NONE -> 0
        }
        val dismissedEvent = LoggedAppEvent(
            type = AppEventType.PROMPT_DISMISSED,
            detail = "${promptType.name} dismissed — silenced for $cooldownDays days.",
            timestampEpochMs = now
        )
        _state.value = current.copy(
            lastRatePromptDismissedEpochMs = nextRateDismiss,
            lastSharePromptDismissedEpochMs = nextShareDismiss,
            lastUpdatePromptDismissedEpochMs = nextUpdateDismiss,
            activeAutoPrompt = SmartPromptType.NONE,
            lastDecisionReason = "${promptType.title} dismissed ($cooldownDays-day cooldown active).",
            eventHistory = (listOf(dismissedEvent) + current.eventHistory).take(MAX_EVENT_HISTORY)
        )
    }

    companion object {
        private const val PREFS_NAME = "sidequest_engagement_telemetry"
        private const val KEY_FIRST_INSTALL_MS = "first_install_ms"
        private const val KEY_LAUNCH_COUNT = "launch_count"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_POSITIVE_MOMENTS = "positive_moments"
        private const val KEY_SATISFACTION_SCORE = "satisfaction_score"
        private const val KEY_HAS_RATED = "has_rated_app"
        private const val KEY_RATE_SHOWN_COUNT = "rate_shown_count"
        private const val KEY_LAST_RATE_DISMISSED_MS = "last_rate_dismissed_ms"
        private const val KEY_SHARE_SHOWN_COUNT = "share_shown_count"
        private const val KEY_LAST_SHARE_DISMISSED_MS = "last_share_dismissed_ms"
        private const val KEY_UPDATE_SHOWN_COUNT = "update_shown_count"
        private const val KEY_LAST_UPDATE_DISMISSED_MS = "last_update_dismissed_ms"

        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val RATE_COOLDOWN_MS = 21L * DAY_MS
        const val SHARE_COOLDOWN_MS = 14L * DAY_MS
        const val UPDATE_COOLDOWN_MS = 7L * DAY_MS

        const val MAX_PROMPTS_PER_SESSION = 1
        const val MIN_LAUNCHES_BEFORE_PROMPT = 2
        const val MIN_DAYS_BEFORE_PROMPT = 2
        const val MIN_POSITIVE_MOMENTS_FOR_RATE = 3
        const val MIN_POSITIVE_MOMENTS_FOR_SHARE = 5
        const val MIN_POSITIVE_MOMENTS_FOR_UPDATE = 6

        const val MAX_LIFETIME_RATE_PROMPTS = 3
        const val MAX_LIFETIME_SHARE_PROMPTS = 4
        const val MAX_LIFETIME_UPDATE_PROMPTS = 3
        private const val MAX_EVENT_HISTORY = 25
    }
}
