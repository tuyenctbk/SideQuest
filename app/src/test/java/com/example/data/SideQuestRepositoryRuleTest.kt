package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.CharacterStyle
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SideQuestRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileAndCreateQuest_authenticatedOwner_succeedsAndEmitsUpdates() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SideQuestRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureAdventurerProfile("Vesper", CharacterStyle.CYBERPUNK_SCOUT)
        }
        assertEquals(uid, profile.userId)

        val questId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createQuest(
                title = "Protocol of the Bronze Plaque",
                characterStyle = CharacterStyle.CYBERPUNK_SCOUT,
                regionName = "Downtown Sector",
                summary = "Trace the historic grid.",
                landmarksCount = 3,
                distanceMeters = 900,
                stopsSummary = listOf("Heritage Park", "Founder Plaque", "Neon Espresso")
            )
        }
        assertTrue(questId.isNotBlank())

        val emittedQuests = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeQuests(uid).first { list -> list.any { it.id == questId } }
        }
        assertTrue(emittedQuests.any { it.id == questId && it.status == "ACTIVE" })

        // Complete quest
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateQuestProgress(
                questId = questId,
                completedStops = 3,
                xpEarned = 300,
                status = "COMPLETED",
                summary = "All stops cleared."
            )
        }

        val completedJournalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordCompletedQuest(
                title = "Shadows of the Ferry Archway",
                characterStyle = CharacterStyle.NOIR_DETECTIVE,
                regionName = "San Francisco — Embarcadero",
                summary = "Decoded the brass compass inscription.",
                landmarksCount = 3,
                xpEarned = 275,
                distanceMeters = 780,
                stopsSummary = listOf("Ferry Clocktower", "Rincon Park", "Red Bay Roasters")
            )
        }
        assertTrue(completedJournalId.isNotBlank())

        val completedQuests = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCompletedQuests(uid).first { list ->
                list.any { it.id == questId } && list.any { it.id == completedJournalId }
            }
        }
        val recordedEntry = completedQuests.first { it.id == completedJournalId }
        assertEquals("Shadows of the Ferry Archway", recordedEntry.questName)
        assertEquals("San Francisco — Embarcadero", recordedEntry.location)
        assertNotNull(recordedEntry.completedAt)

        // Unlock a badge with dynamic ID
        val uniqueBadgeId = "badge_${UUID.randomUUID()}"
        val unlocked = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.unlockBadgeIfAbsent(
                badgeId = uniqueBadgeId,
                badgeCode = "FIRST_QUEST",
                title = "Grid Pathfinder",
                description = "Cleared your first walking quest.",
                category = "EXPLORATION",
                rarity = "RARE",
                landmarkName = "Heritage Park"
            )
        }
        assertTrue(unlocked)
    }

    @Test
    fun getQuestById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = SideQuestRepository(firestore)
        val questId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createQuest(
                title = "Alice's Secret Walk",
                characterStyle = CharacterStyle.VICTORIAN_HISTORIAN,
                regionName = "Old Quarter",
                summary = "Private expedition log.",
                landmarksCount = 2,
                distanceMeters = 500,
                stopsSummary = listOf("Royal Garden", "Old Clocktower")
            )
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = SideQuestRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getQuestById(aliceUid, questId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeQuests_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = SideQuestRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeQuests(aliceUid).first()
            }
            fail("Expected exception when observing quests unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun recordAndManageFoundArtifacts_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = SideQuestRepository(firestore)

        val artifactId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordFoundArtifact(
                artifactCode = "WAYPOINT_CLEAR",
                name = "1898 Brass Ferry Token",
                loreInscription = "Recovered beside the waterfront clocktower.",
                category = com.example.model.ArtifactCategory.PLAQUE_RUBBING,
                rarity = "EPIC",
                landmarkName = "Ferry Building Clocktower",
                regionName = "San Francisco — Embarcadero",
                characterStyle = CharacterStyle.CYBERPUNK_SCOUT,
                latitude = 37.7955,
                longitude = -122.3937,
                resonanceValue = 55,
                isFavorite = false,
                inspectionNotes = "Initial field rubbing."
            )
        }
        assertTrue(artifactId.isNotBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleFavoriteArtifact(artifactId, true)
            repository.updateArtifactInspectionNotes(artifactId, "Verified 1898 inscription in sunlight.")
        }

        val emittedArtifacts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeArtifacts(uid).first { list ->
                list.any { it.id == artifactId && it.isFavorite && it.inspectionNotes.contains("1898") }
            }
        }
        assertTrue(emittedArtifacts.any { it.id == artifactId && it.name == "1898 Brass Ferry Token" })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_sidequest@test.com"
        const val BOB_EMAIL = "bob_sidequest@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
