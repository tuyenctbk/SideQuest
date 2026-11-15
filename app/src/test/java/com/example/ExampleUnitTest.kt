package com.example

import com.example.util.AppEngagementTracker
import com.example.util.AppEventType
import com.example.util.SmartPromptType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun appStartupNeverTriggersAutoPrompt() {
        val tracker = AppEngagementTracker(context = null)
        assertEquals(SmartPromptType.NONE, tracker.state.value.activeAutoPrompt)
        assertEquals(0, tracker.state.value.promptsShownThisSession)
        assertTrue(tracker.state.value.eventHistory.any { it.type == AppEventType.APP_LAUNCH })
    }

    @Test
    fun rateAppTriggersOnlyAfterValueMomentsAndRespectsSessionCap() {
        val tracker = AppEngagementTracker(context = null)
        val baseTime = 1_700_000_000_000L

        // Moment 1: Not enough positive milestones yet (1/3)
        tracker.logEvent(
            type = AppEventType.WAYPOINT_CLEARED,
            detail = "Cleared Ferry Building",
            allowAutoPromptEvaluation = true,
            nowEpochMs = baseTime + 1_000L
        )
        assertEquals(SmartPromptType.NONE, tracker.state.value.activeAutoPrompt)

        // Moment 2: Still building value (2/3)
        tracker.logEvent(
            type = AppEventType.VISION_SCAN_VERIFIED,
            detail = "Scanned plaque",
            allowAutoPromptEvaluation = true,
            nowEpochMs = baseTime + 2_000L
        )
        assertEquals(SmartPromptType.NONE, tracker.state.value.activeAutoPrompt)

        // Moment 3: Reaches 3+ positive moments and satisfactionScore >= 6 -> Triggers RATE_APP
        tracker.logEvent(
            type = AppEventType.QUEST_COMPLETED,
            detail = "Completed Embarcadero Quest",
            allowAutoPromptEvaluation = true,
            nowEpochMs = baseTime + 3_000L
        )
        assertEquals(SmartPromptType.RATE_APP, tracker.state.value.activeAutoPrompt)
        assertEquals(1, tracker.state.value.promptsShownThisSession)

        // Dismissing RATE_APP applies 21-day cooldown and clears active prompt
        tracker.onPromptDismissed(SmartPromptType.RATE_APP)
        assertEquals(SmartPromptType.NONE, tracker.state.value.activeAutoPrompt)

        // Subsequent positive events in the SAME session must NEVER trigger another pop-up (session cap = 1)
        tracker.logEvent(
            type = AppEventType.QUEST_COMPLETED,
            detail = "Completed second quest in same session",
            allowAutoPromptEvaluation = true,
            nowEpochMs = baseTime + 4_000L
        )
        assertEquals(SmartPromptType.NONE, tracker.state.value.activeAutoPrompt)
        assertEquals(1, tracker.state.value.promptsShownThisSession)
    }

    @Test
    fun onboardingCompletionIsLoggedWithoutTriggeringPopups() {
        val tracker = AppEngagementTracker(context = null)
        assertFalse(tracker.state.value.isOnboardingCompleted)

        tracker.markOnboardingCompleted()
        assertTrue(tracker.state.value.isOnboardingCompleted)
        assertEquals(SmartPromptType.NONE, tracker.state.value.activeAutoPrompt)
        assertTrue(tracker.state.value.eventHistory.any { it.type == AppEventType.ONBOARDING_COMPLETED })
    }

    @Test
    fun aiModelConfigResolvesTier1Tier2AndTier3ByQuotaAndEcoMode() {
        val primaryChat = com.example.util.AiModelConfig.resolveModel(
            capability = com.example.util.AiModelConfig.AiCapability.COMPANION_CHAT,
            isEcoLiteMode = false,
            isNearLimit = false,
            isQuotaExhausted = false
        )
        assertEquals(com.example.util.AiModelConfig.PRIMARY_REASONING_MODEL, primaryChat.first)
        assertEquals(com.example.util.AiModelConfig.AiExecutionTier.TIER_1_PRIMARY, primaryChat.second)

        val primaryImage = com.example.util.AiModelConfig.resolveModel(
            capability = com.example.util.AiModelConfig.AiCapability.IMAGE_STUDIO,
            isEcoLiteMode = false,
            isNearLimit = false,
            isQuotaExhausted = false
        )
        assertEquals(com.example.util.AiModelConfig.PRIMARY_IMAGE_MODEL, primaryImage.first)
        assertEquals(com.example.util.AiModelConfig.AiExecutionTier.TIER_1_PRIMARY, primaryImage.second)

        val ecoByToggle = com.example.util.AiModelConfig.resolveModel(
            capability = com.example.util.AiModelConfig.AiCapability.VISION_PROOF,
            isEcoLiteMode = true,
            isNearLimit = false,
            isQuotaExhausted = false
        )
        assertEquals(com.example.util.AiModelConfig.ECONOMY_LITE_MODEL, ecoByToggle.first)
        assertEquals(com.example.util.AiModelConfig.AiExecutionTier.TIER_2_ECONOMY_LITE, ecoByToggle.second)

        val ecoImageByNearLimit = com.example.util.AiModelConfig.resolveModel(
            capability = com.example.util.AiModelConfig.AiCapability.IMAGE_STUDIO,
            isEcoLiteMode = false,
            isNearLimit = true,
            isQuotaExhausted = false
        )
        assertEquals(com.example.util.AiModelConfig.ECONOMY_IMAGE_MODEL, ecoImageByNearLimit.first)
        assertEquals(com.example.util.AiModelConfig.AiExecutionTier.TIER_2_ECONOMY_LITE, ecoImageByNearLimit.second)

        val exhausted = com.example.util.AiModelConfig.resolveModel(
            capability = com.example.util.AiModelConfig.AiCapability.RIDDLE_CIPHER,
            isEcoLiteMode = false,
            isNearLimit = true,
            isQuotaExhausted = true
        )
        assertEquals(com.example.util.AiModelConfig.LOCAL_REFLEX_MODEL, exhausted.first)
        assertEquals(com.example.util.AiModelConfig.AiExecutionTier.TIER_3_LOCAL_FALLBACK, exhausted.second)
    }

    @Test
    fun usageQuotaLedgerThresholdsAndGraceCreditsWorkAccurately() {
        val healthy = com.example.model.UsageQuotaLedger(
            visionUsed = 2L,
            chatUsed = 5L,
            studioUsed = 0L,
            audioUsed = 1L
        )
        assertEquals(8L, healthy.totalUsed)
        assertEquals(25L, healthy.effectiveLimit)
        assertEquals(17L, healthy.remainingCredits)
        assertFalse(healthy.isNearLimit)
        assertFalse(healthy.isExhausted)
        assertTrue(healthy.canClaimGraceCredits())
        assertEquals(2L, healthy.usageFor(com.example.util.AiModelConfig.AiCapability.VISION_PROOF))
        assertEquals(5L, healthy.usageFor(com.example.util.AiModelConfig.AiCapability.FACT_DOSSIER))

        // 20 / 25 = 0.80 -> triggers near limit and soft nudge when ecoLiteMode is off
        val nearLimit = healthy.copy(chatUsed = 17L)
        assertEquals(20L, nearLimit.totalUsed)
        assertTrue(nearLimit.isNearLimit)
        assertTrue(nearLimit.isSoftNudgeThresholdReached())
        assertFalse(nearLimit.copy(ecoLiteMode = true).isSoftNudgeThresholdReached())

        // 25 / 25 -> exhausted until grace credits claimed
        val exhausted = nearLimit.copy(visionUsed = 7L)
        assertEquals(25L, exhausted.totalUsed)
        assertTrue(exhausted.isExhausted)
        assertFalse(exhausted.isNearLimit)

        val withGrace = exhausted.copy(
            bonusGraceCredits = com.example.util.AiModelConfig.GRACE_CREDITS_AMOUNT.toLong(),
            graceClaimed = true
        )
        assertEquals(28L, withGrace.effectiveLimit)
        assertEquals(3L, withGrace.remainingCredits)
        assertFalse(withGrace.isExhausted)
        assertFalse(withGrace.canClaimGraceCredits())
    }
}
