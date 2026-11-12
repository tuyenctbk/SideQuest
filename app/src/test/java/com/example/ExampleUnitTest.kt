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
}
