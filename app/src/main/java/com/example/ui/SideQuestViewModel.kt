package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.DungeonMasterAiRepository
import com.example.data.LandmarkDiscoveryRepository
import com.example.data.SideQuestRepository
import com.example.data.UsageQuotaManager
import com.example.model.AdventurerProfile
import com.example.model.AiLandmarkRiddle
import com.example.model.AiValueReceipt
import com.example.model.ArtifactCategory
import com.example.model.CharacterStyle
import com.example.model.ChatMessage
import com.example.model.CompletedQuest
import com.example.model.EarnedBadge
import com.example.model.FoundArtifact
import com.example.model.GuildBounty
import com.example.model.InventoryItem
import com.example.model.LandmarkCategory
import com.example.model.LandmarkFactDossier
import com.example.model.NeighborhoodStanding
import com.example.model.OfficialHeritageTrail
import com.example.model.PartyMemberStatus
import com.example.model.PartyRole
import com.example.model.PresetCityRegion
import com.example.model.RealLandmark
import com.example.model.RelicArtUiState
import com.example.model.SeedLandmark
import com.example.model.SmartQuotaRecoveryState
import com.example.model.TavernVoucher
import com.example.model.UsageQuotaLedger
import com.example.model.VisionScanResult
import com.example.model.WorldAtmosphereEvent
import com.example.util.AiModelConfig
import com.example.util.AiModelConfig.AiCapability
import com.example.util.AiModelConfig.AiExecutionTier
import com.example.util.AppEngagementTracker
import com.example.util.AppEventType
import com.example.util.EngagementTelemetryState
import com.example.util.SmartPromptType
import com.example.util.VoiceNarrator
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Locale
import java.util.UUID

private const val TAG = "SideQuestVM"

class SideQuestViewModel(
    private val firestoreRepo: SideQuestRepository,
    private val landmarkRepo: LandmarkDiscoveryRepository,
    private val aiRepo: DungeonMasterAiRepository,
    val voiceNarrator: VoiceNarrator,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val engagementTracker: AppEngagementTracker = AppEngagementTracker(),
    private val usageQuotaManager: UsageQuotaManager? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val isAuthenticatedSession: Boolean = try {
        Firebase.auth.currentUser?.uid == currentUserId
    } catch (_: Exception) {
        false
    }

    private fun getStr(@StringRes resId: Int, fallback: String, vararg args: Any): String {
        val ctx = appContext ?: return fallback
        return try {
            if (args.isEmpty()) ctx.getString(resId) else ctx.getString(resId, *args)
        } catch (_: Exception) {
            fallback
        }
    }

    val engagementTelemetryState: StateFlow<EngagementTelemetryState> = engagementTracker.state

    val presetRegions: List<PresetCityRegion> = landmarkRepo.presetRegions

    private val _firestoreErrorMessage = MutableStateFlow<String?>(null)
    val firestoreErrorMessage: StateFlow<String?> = _firestoreErrorMessage.asStateFlow()

    // In-memory backing stores for Guest Explorer sessions (zero Firestore writes/reads when unauthenticated)
    private val _guestProfileState = MutableStateFlow<AdventurerProfile?>(
        if (isAuthenticatedSession) null else AdventurerProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = initialDisplayName.ifBlank { getStr(R.string.guest_explorer_name, "Guest Explorer") },
            characterStyle = CharacterStyle.CYBERPUNK_SCOUT.id,
            xp = 0L,
            level = 1L,
            questsCompleted = 0L,
            landmarksVisited = 0L
        )
    )
    private val _guestQuestsState = MutableStateFlow<List<CompletedQuest>>(emptyList())
    private val _guestBadgesState = MutableStateFlow<List<EarnedBadge>>(emptyList())
    private val _guestInventoryState = MutableStateFlow<List<InventoryItem>>(emptyList())
    private val _guestArtifactsState = MutableStateFlow<List<FoundArtifact>>(emptyList())

    val profileState: StateFlow<AdventurerProfile?> = if (isAuthenticatedSession) {
        firestoreRepo.observeProfile(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing profile", root)
                _firestoreErrorMessage.value = root.localizedMessage ?: getStr(R.string.vm_err_load_profile, "Failed to load adventurer profile")
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = null
            )
    } else {
        _guestProfileState.asStateFlow()
    }

    val questsState: StateFlow<List<CompletedQuest>> = if (isAuthenticatedSession) {
        firestoreRepo.observeQuests(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing quests", root)
                _firestoreErrorMessage.value = root.localizedMessage ?: getStr(R.string.vm_err_load_quest_log, "Failed to load quest log")
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    } else {
        _guestQuestsState.asStateFlow()
    }

    val completedQuestsState: StateFlow<List<CompletedQuest>> = if (isAuthenticatedSession) {
        firestoreRepo.observeCompletedQuests(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing completed quests", root)
                _firestoreErrorMessage.value = root.localizedMessage ?: getStr(R.string.vm_err_load_journal, "Failed to load quest journal")
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    } else {
        _guestQuestsState
            .map { list -> list.filter { it.status == "COMPLETED" } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    }

    val badgesState: StateFlow<List<EarnedBadge>> = if (isAuthenticatedSession) {
        firestoreRepo.observeBadges(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing badges", root)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    } else {
        _guestBadgesState.asStateFlow()
    }

    val inventoryState: StateFlow<List<InventoryItem>> = if (isAuthenticatedSession) {
        firestoreRepo.observeInventory(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing inventory", root)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    } else {
        _guestInventoryState.asStateFlow()
    }

    val artifactsState: StateFlow<List<FoundArtifact>> = if (isAuthenticatedSession) {
        firestoreRepo.observeArtifacts(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing found artifacts", root)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    } else {
        _guestArtifactsState.asStateFlow()
    }

    // Approved Suggestion: Dynamic Weather & Time-of-Day World Atmosphere Engine
    private val _activeAtmosphereEvent = MutableStateFlow(WorldAtmosphereEvent.GOLDEN_HOUR_SURGE)
    val activeAtmosphereEvent: StateFlow<WorldAtmosphereEvent> = _activeAtmosphereEvent.asStateFlow()

    private val _selectedStyle = MutableStateFlow(CharacterStyle.CYBERPUNK_SCOUT)
    val selectedStyle: StateFlow<CharacterStyle> = _selectedStyle.asStateFlow()

    private val _regionName = MutableStateFlow(presetRegions.first().name)
    val regionName: StateFlow<String> = _regionName.asStateFlow()

    private val _userCoordinates = MutableStateFlow(
        Pair(presetRegions.first().latitude, presetRegions.first().longitude)
    )
    val userCoordinates: StateFlow<Pair<Double, Double>> = _userCoordinates.asStateFlow()

    private val _landmarks = MutableStateFlow<List<RealLandmark>>(emptyList())
    val landmarks: StateFlow<List<RealLandmark>> = _landmarks.asStateFlow()

    private val _selectedLandmarkIndex = MutableStateFlow(0)
    val selectedLandmarkIndex: StateFlow<Int> = _selectedLandmarkIndex.asStateFlow()

    private val _activeQuestTitle = MutableStateFlow("Protocol: Embarcadero Signal Recon")
    val activeQuestTitle: StateFlow<String> = _activeQuestTitle.asStateFlow()

    private val _activeQuestIntro = MutableStateFlow(
        "Sensors calibrated. Approach nearby real-world parks, historical plaques, and coffee houses to unlock lore and relics."
    )
    val activeQuestIntro: StateFlow<String> = _activeQuestIntro.asStateFlow()

    private val _activeFirestoreQuestId = MutableStateFlow<String?>(null)
    val activeFirestoreQuestId: StateFlow<String?> = _activeFirestoreQuestId.asStateFlow()

    private val _isGeneratingQuest = MutableStateFlow(false)
    val isGeneratingQuest: StateFlow<Boolean> = _isGeneratingQuest.asStateFlow()

    private val _isScanningVision = MutableStateFlow(false)
    val isScanningVision: StateFlow<Boolean> = _isScanningVision.asStateFlow()

    private val _lastVisionScanResult = MutableStateFlow<VisionScanResult?>(null)
    val lastVisionScanResult: StateFlow<VisionScanResult?> = _lastVisionScanResult.asStateFlow()

    private val _aiErrorBanner = MutableStateFlow<String?>(null)
    val aiErrorBanner: StateFlow<String?> = _aiErrorBanner.asStateFlow()

    private val _rewardBanner = MutableStateFlow<String?>(null)
    val rewardBanner: StateFlow<String?> = _rewardBanner.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isDungeonMasterStreaming = MutableStateFlow(false)
    val isDungeonMasterStreaming: StateFlow<Boolean> = _isDungeonMasterStreaming.asStateFlow()

    // New AI Feature 1: Runtime AI Relic & Travelogue Image Generator / Editor (gemini-nano-banana-2.1)
    private val _relicArtUiState = MutableStateFlow<RelicArtUiState>(RelicArtUiState.Idle)
    val relicArtUiState: StateFlow<RelicArtUiState> = _relicArtUiState.asStateFlow()

    // New AI Feature 2: AI Grounded Landmark Fact-Check & Deep Lore Dossier (gemini-3.8-flash)
    private val _isGeneratingFactDossier = MutableStateFlow(false)
    val isGeneratingFactDossier: StateFlow<Boolean> = _isGeneratingFactDossier.asStateFlow()

    private val _activeFactDossier = MutableStateFlow<LandmarkFactDossier?>(null)
    val activeFactDossier: StateFlow<LandmarkFactDossier?> = _activeFactDossier.asStateFlow()

    // New AI Feature 3: AI Dungeon Master Riddle & Cipher Duel Mini-Game (gemini-3.8-flash)
    private val _isGeneratingRiddle = MutableStateFlow(false)
    val isGeneratingRiddle: StateFlow<Boolean> = _isGeneratingRiddle.asStateFlow()

    private val _activeAiRiddle = MutableStateFlow<AiLandmarkRiddle?>(null)
    val activeAiRiddle: StateFlow<AiLandmarkRiddle?> = _activeAiRiddle.asStateFlow()

    private val _riddleAnswerStatus = MutableStateFlow<String?>(null)
    val riddleAnswerStatus: StateFlow<String?> = _riddleAnswerStatus.asStateFlow()

    private val _isActiveRiddleSolved = MutableStateFlow(false)
    val isActiveRiddleSolved: StateFlow<Boolean> = _isActiveRiddleSolved.asStateFlow()

    // New AI Feature 4: Live Voice Field-Log Polisher & Transcriber (gemini-3.8-flash)
    private val _isPolishingVoiceNote = MutableStateFlow(false)
    val isPolishingVoiceNote: StateFlow<Boolean> = _isPolishingVoiceNote.asStateFlow()

    // Smart Zero-Cost / Free-Tier Eco Mode & User-Controlled Paid API Opt-In
    private val _isPaidImageApiApproved = MutableStateFlow(false)
    val isPaidImageApiApproved: StateFlow<Boolean> = _isPaidImageApiApproved.asStateFlow()

    val freeTierCallsUsed: StateFlow<Int> = aiRepo.freeTierCallsUsed
    val cacheHitsSaved: StateFlow<Int> = aiRepo.cacheHitsSaved
    val paidImageCallsUsed: StateFlow<Int> = aiRepo.paidImageCallsUsed
    val autoCascadeCount: StateFlow<Int> = aiRepo.autoCascadeCount

    // Hybrid Quota Ledger & Smart Quota Recovery State (Playbook Sections 1 & 3)
    val usageQuotaLedger: StateFlow<UsageQuotaLedger> = usageQuotaManager?.quotaState
        ?: MutableStateFlow(UsageQuotaLedger()).asStateFlow()

    private val _smartQuotaRecoveryState = MutableStateFlow(SmartQuotaRecoveryState())
    val smartQuotaRecoveryState: StateFlow<SmartQuotaRecoveryState> = _smartQuotaRecoveryState.asStateFlow()

    private val _isAiValueReceiptVisible = MutableStateFlow(false)
    val isAiValueReceiptVisible: StateFlow<Boolean> = _isAiValueReceiptVisible.asStateFlow()

    private val _aiValueReceipt = MutableStateFlow(AiValueReceipt())
    val aiValueReceipt: StateFlow<AiValueReceipt> = _aiValueReceipt.asStateFlow()

    // Smart Non-Intrusive Donation Engagement Engine State (Never triggers at app startup)
    private val _donationEngagementState = MutableStateFlow(
        DonationEngagementState(
            isPermanentSupporter = false,
            successfulOperationsCount = 0,
            appLaunchCount = 3,
            daysSinceInstall = 3
        )
    )
    val donationEngagementState: StateFlow<DonationEngagementState> = _donationEngagementState.asStateFlow()

    // Daily Walking Distance & Streak Tracker
    val dailyWalkGoalMeters: Int = 1000
    private val _dailyWalkMetersLogged = MutableStateFlow(320)
    val dailyWalkMetersLogged: StateFlow<Int> = _dailyWalkMetersLogged.asStateFlow()

    // Suggestion 3: Freemium Adventurer's Guild Pass & Offline Pack Caching
    private val _isGuildPassActive = MutableStateFlow(false)
    val isGuildPassActive: StateFlow<Boolean> = _isGuildPassActive.asStateFlow()

    private val _isOfflinePackCached = MutableStateFlow(false)
    val isOfflinePackCached: StateFlow<Boolean> = _isOfflinePackCached.asStateFlow()

    // Suggestion 5: Hands-Free "Pocket Dungeon Master" Auto-Proximity Walk Mode
    private val _isPocketWalkModeActive = MutableStateFlow(false)
    val isPocketWalkModeActive: StateFlow<Boolean> = _isPocketWalkModeActive.asStateFlow()

    private val _pocketWalkProximityMeters = MutableStateFlow(110)
    val pocketWalkProximityMeters: StateFlow<Int> = _pocketWalkProximityMeters.asStateFlow()

    private var pocketWalkJob: Job? = null

    // Suggestion 2: Curated Official Tourism & Heritage Trails
    val officialHeritageTrails: List<OfficialHeritageTrail> = listOf(
        OfficialHeritageTrail(
            id = "trail_sf_maritime",
            title = "Gold Rush & Waterfront Cipher",
            sponsorAuthority = "VISIT BAY AREA HERITAGE COMMISSION",
            cityRegionName = "San Francisco — Embarcadero",
            durationMinutes = 35,
            xpMultiplierLabel = "1.5x Official Trail XP",
            badgeRewardTitle = "Bay Area Heritage Laureate",
            description = "Trace 1898 ferry terminals, literary bohemian cafes, and hidden telegraph hill gardens.",
            recommendedStyle = CharacterStyle.CYBERPUNK_SCOUT
        ),
        OfficialHistoricTrailBritish(),
        OfficialHeritageTrail(
            id = "trail_kyoto_shinto",
            title = "Torii Gates & Matcha Alchemy Path",
            sponsorAuthority = "KYOTO CULTURAL TOURISM BUREAU",
            cityRegionName = "Kyoto — Higashiyama",
            durationMinutes = 45,
            xpMultiplierLabel = "1.5x Official Trail XP",
            badgeRewardTitle = "Higashiyama Star-Cartographer",
            description = "Walk lantern-lit stone lanes from Yasaka Pagoda to centuries-old tea houses.",
            recommendedStyle = CharacterStyle.ARCANE_CARTOGRAPHER
        ),
        OfficialHeritageTrail(
            id = "trail_nyc_bohemian",
            title = "Midnight Beatnik & Archway Casefile",
            sponsorAuthority = "NYC LANDMARKS PRESERVATION FUND",
            cityRegionName = "New York — Central Park",
            durationMinutes = 40,
            xpMultiplierLabel = "1.5x Official Trail XP",
            badgeRewardTitle = "Manhattan Nocturne Detective",
            description = "Investigate 1873 Bethesda arches, literary promenades, and espresso haunts.",
            recommendedStyle = CharacterStyle.NOIR_DETECTIVE
        )
    )

    // Suggestion 1: Location-Based B2B2C "Tavern Rest Stops" & Redeemable Vouchers
    private val _tavernVouchers = MutableStateFlow(
        listOf(
            TavernVoucher(
                id = "voucher_artisan_espresso",
                partnerName = "Guild Roasters & Co. (Partner Cafe)",
                discountHeadline = "20% OFF Any Artisanal Espresso or Matcha Elixir",
                termsDescription = "Unlocked by visiting a Coffee House waypoint on your walking route.",
                redemptionCode = "SQ-BREW-2099",
                landmarkCategory = LandmarkCategory.COFFEE_SHOP,
                isUnlocked = false
            ),
            TavernVoucher(
                id = "voucher_museum_pass",
                partnerName = "Civic Heritage Society & Athenaeum",
                discountHeadline = "Complimentary Audio Guide & Commemorative Pin",
                termsDescription = "Unlocked by deciphering any Historical Plaque or Monument.",
                redemptionCode = "SQ-LORE-1894",
                landmarkCategory = LandmarkCategory.PLAQUE,
                isUnlocked = false
            ),
            TavernVoucher(
                id = "voucher_botanical_tea",
                partnerName = "Conservatory Apothecary & Tea Pavilion",
                discountHeadline = "Free Herbal Infusion Boost After Park Walk",
                termsDescription = "Unlocked by completing a Sanctuary Park waypoint.",
                redemptionCode = "SQ-GROVE-77",
                landmarkCategory = LandmarkCategory.PARK,
                isUnlocked = false
            )
        )
    )
    val tavernVouchers: StateFlow<List<TavernVoucher>> = _tavernVouchers.asStateFlow()

    // Suggestion 6: Co-Op Party Expeditions & Role Clues
    private val _partyCode = MutableStateFlow("NEXUS-7")
    val partyCode: StateFlow<String> = _partyCode.asStateFlow()

    private val _selectedPartyRole = MutableStateFlow(PartyRole.SCOUT)
    val selectedPartyRole: StateFlow<PartyRole> = _selectedPartyRole.asStateFlow()

    private val _isCoOpRiddleSolved = MutableStateFlow(false)
    val isCoOpRiddleSolved: StateFlow<Boolean> = _isCoOpRiddleSolved.asStateFlow()

    private val _partyMembers = MutableStateFlow(
        listOf(
            PartyMemberStatus(
                name = "Aria_Vanguard",
                role = PartyRole.LOREKEEPER,
                style = CharacterStyle.VICTORIAN_HISTORIAN,
                distanceMetersAway = 18,
                isReady = true,
                discoveredClue = "Inscription Fragment: 'Founded when iron rails met the tidal bay (1898)'"
            ),
            PartyMemberStatus(
                name = "Kaelen_Hex",
                role = PartyRole.QUARTERMASTER,
                style = CharacterStyle.ARCANE_CARTOGRAPHER,
                distanceMetersAway = 32,
                isReady = true,
                discoveredClue = "Ley-Compass Bearing: 'Follow the roasted bean aroma 40 paces north of the arch'"
            )
        )
    )
    val partyMembers: StateFlow<List<PartyMemberStatus>> = _partyMembers.asStateFlow()

    private val _dailyBounties = MutableStateFlow(
        listOf(
            GuildBounty(
                id = "bounty_cafe_elixir",
                title = "Local Roaster Alchemy",
                sponsorTag = "FEATURED CAFE PARTNER",
                description = "Visit any real-world Coffee Shop waypoint to unlock the Artisanal Elixir Relic & support local roasters.",
                rewardPerk = "Unlocks 'Cafe Alchemist' Crest + 20% Cafe Voucher + 150 XP",
                targetCategory = LandmarkCategory.COFFEE_SHOP,
                bonusXp = 150
            ),
            GuildBounty(
                id = "bounty_heritage_plaque",
                title = "Echoes in Brass & Stone",
                sponsorTag = "HERITAGE SOCIETY BOUNTY",
                description = "Decipher any Historical Plaque or Monument inscription along your walking tour.",
                rewardPerk = "Unlocks 'Keeper of Plaques' Crest + Museum Pass + 175 XP",
                targetCategory = LandmarkCategory.PLAQUE,
                bonusXp = 175
            ),
            GuildBounty(
                id = "bounty_green_sanctuary",
                title = "Canopy Ley-Line Survey",
                sponsorTag = "DAILY WELLNESS WALK",
                description = "Explore a Sanctuary Park waypoint to attune your compass and log outdoor steps.",
                rewardPerk = "Unlocks 'Sanctuary Pathfinder' + Botanical Perk + 125 XP",
                targetCategory = LandmarkCategory.PARK,
                bonusXp = 125
            )
        )
    )
    val dailyBounties: StateFlow<List<GuildBounty>> = _dailyBounties.asStateFlow()

    init {
        val initialPreset = presetRegions.first()
        val seeded = initialPreset.fallbackLandmarks.mapIndexed { idx, seed ->
            landmarkRepo.buildStyledLandmark(
                index = idx,
                seed = seed,
                distanceMeters = (idx + 1) * 180,
                style = _selectedStyle.value
            )
        }
        _landmarks.value = seeded
        _chatMessages.value = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                senderName = _selectedStyle.value.narratorTitle,
                text = getStr(
                    R.string.vm_welcome_dm_format,
                    "Welcome to SideQuest, explorer. I am ${_selectedStyle.value.narratorTitle}. Your tactical map has locked onto ${seeded.size} real-world landmarks in ${initialPreset.name}. Select any waypoint, scan a real plaque with Gemini Vision, or tap 'Quest' to weave a fresh adventure!",
                    _selectedStyle.value.narratorTitle,
                    seeded.size,
                    initialPreset.name
                ),
                landmarkName = seeded.firstOrNull()?.name,
                groundingSources = listOf("OpenStreetMap & Google Maps Grounding", initialPreset.name)
            )
        )

        if (isAuthenticatedSession) {
            viewModelScope.launch {
                try {
                    val profile = firestoreRepo.ensureAdventurerProfile(
                        defaultName = initialDisplayName,
                        style = _selectedStyle.value
                    )
                    val restoredStyle = CharacterStyle.fromId(profile.characterStyle)
                    if (restoredStyle != _selectedStyle.value) {
                        _selectedStyle.value = restoredStyle
                        restyleCurrentLandmarks(restoredStyle)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Initial profile check failed: ${e.message}")
                }
            }
        }

        // Sync earned badges & inventory from Firestore so bounties/vouchers stay completed across sessions
        viewModelScope.launch {
            badgesState.collect { badges ->
                val codes = badges.map { it.badgeCode }.toSet()
                if (codes.isNotEmpty()) {
                    _dailyBounties.value = _dailyBounties.value.map { b ->
                        val earned = when (b.targetCategory) {
                            LandmarkCategory.COFFEE_SHOP -> "BADGE_CAFE_ALCHEMIST" in codes
                            LandmarkCategory.PLAQUE, LandmarkCategory.MONUMENT ->
                                "BADGE_PLAQUE_SCHOLAR" in codes || "BADGE_MONUMENT_SENTINEL" in codes
                            LandmarkCategory.PARK -> "BADGE_PARK_EXPLORER" in codes
                            else -> false
                        }
                        if (earned) b.copy(isCompleted = true) else b
                    }
                    if ("COOP_PARTY_VANGUARD" in codes) {
                        _isCoOpRiddleSolved.value = true
                    }
                    if ("ACTIVE_GUILD_SUPPORTER" in codes) {
                        val supporterBadge = badges.firstOrNull { it.badgeCode == "ACTIVE_GUILD_SUPPORTER" }
                        _donationEngagementState.value = _donationEngagementState.value.copy(
                            isPermanentSupporter = true,
                            supporterTierTitle = supporterBadge?.title ?: "Guild Supporter",
                            isHomeBannerDismissedCooldownActive = true,
                            showPostOperationCard = false,
                            showMilestoneCelebrationBanner = false,
                            shouldTriggerAutoPromptDialog = false
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            inventoryState.collect { items ->
                val descriptions = items.joinToString(" ") { it.loreDescription }
                _tavernVouchers.value = _tavernVouchers.value.map { v ->
                    if (descriptions.contains(v.redemptionCode)) {
                        v.copy(isUnlocked = true, isRedeemed = true)
                    } else {
                        v
                    }
                }
            }
        }

        if (usageQuotaManager != null) {
            viewModelScope.launch {
                usageQuotaManager.observeUserQuota(currentUserId).collect {
                    updateAiValueReceipt()
                }
            }
        }
    }

    fun dismissBanners() {
        _aiErrorBanner.value = null
        _rewardBanner.value = null
        _firestoreErrorMessage.value = null
    }

    // =========================================================================
    // FREE-TIER HANDLING, FALLBACK ARCHITECTURE & USER RECOVERY PLAYBOOK
    // =========================================================================

    fun updateAiValueReceipt() {
        val prof = profileState.value
        val quests = questsState.value
        val artifacts = artifactsState.value
        val verifiedLandmarks = artifacts.count { it.category == ArtifactCategory.VISION_SCAN.id }
        val craftedRelics = artifacts.count { it.category == ArtifactCategory.RELIC_SHARD.id }
        val voiceNotes = artifacts.count { it.category == ArtifactCategory.PLAQUE_RUBBING.id }
        val completedQuestsCount = quests.count { it.status == "COMPLETED" }
        val totalDistance = quests.sumOf { it.distanceMeters }
        val totalXp = prof?.xp ?: (completedQuestsCount * 175L)

        _aiValueReceipt.value = AiValueReceipt(
            dateIso = AiModelConfig.getTodayDateIso(),
            landmarksVerified = maxOf(verifiedLandmarks, (prof?.landmarksVisited ?: 0L).toInt()),
            dmChatsExchanged = _chatMessages.value.count { it.isUser },
            relicsCrafted = craftedRelics,
            voiceNotesLogged = voiceNotes,
            questsCompleted = completedQuestsCount,
            distanceMetersWalked = maxOf(totalDistance, _dailyWalkMetersLogged.value.toLong()),
            totalXpEarned = totalXp
        )
    }

    fun toggleEcoLiteMode(enabled: Boolean) {
        viewModelScope.launch {
            usageQuotaManager?.toggleEcoLiteMode(enabled, currentUserId)
            if (enabled) {
                _rewardBanner.value = getStr(R.string.vm_eco_lite_enabled, "⚡ Eco Lite Mode Enabled: Running high-speed lightweight AI (3x-5x quota stretch)!")
            } else {
                _rewardBanner.value = getStr(R.string.vm_primary_ai_active, "Primary Standard AI Mode Active.")
            }
        }
    }

    fun claimDailyGraceCredits() {
        viewModelScope.launch {
            val success = usageQuotaManager?.claimDailyGraceCredits(currentUserId) ?: false
            if (success) {
                _smartQuotaRecoveryState.value = _smartQuotaRecoveryState.value.copy(isVisible = false)
                _rewardBanner.value = getStr(R.string.vm_grace_claimed_banner, "🎁 +3 Free Grace Credits Claimed! Continue your exploration without interruption.")
            } else {
                _aiErrorBanner.value = getStr(R.string.vm_grace_already_claimed, "Daily Grace Credits already claimed for today. Quota resets at midnight.")
            }
        }
    }

    fun dismissSmartQuotaRecovery() {
        _smartQuotaRecoveryState.value = _smartQuotaRecoveryState.value.copy(isVisible = false)
    }

    fun showAiValueReceipt(show: Boolean) {
        updateAiValueReceipt()
        _isAiValueReceiptVisible.value = show
    }

    fun showSmartQuotaRecovery(capability: AiCapability, isRateLimited429: Boolean = false, customMessage: String? = null) {
        _smartQuotaRecoveryState.value = SmartQuotaRecoveryState(
            isVisible = true,
            triggeringCapability = capability,
            isRateLimited429 = isRateLimited429,
            ledger = usageQuotaLedger.value,
            customContextMessage = customMessage
        )
    }

    /**
     * Executes the zero-quota deterministic local alternative based on capability (Playbook Section 2).
     */
    fun executeZeroQuotaAlternative(capability: AiCapability) {
        dismissSmartQuotaRecovery()
        viewModelScope.launch {
            usageQuotaManager?.recordFallbackCascade(currentUserId)
        }
        when (capability) {
            AiCapability.VISION_PROOF -> {
                scanLandmarkPhotoWithHonorPledge()
            }
            AiCapability.COMPANION_CHAT -> {
                triggerLocalReflexDmReply()
            }
            AiCapability.IMAGE_STUDIO -> {
                craftLocalCanvasEmblem()
            }
            AiCapability.AUDIO_FIELD_NOTE -> {
                _rewardBanner.value = getStr(R.string.vm_voice_preserved_banner, "Spoken note preserved in raw format — auto-cleanup will process when quota resets.")
            }
            AiCapability.FACT_DOSSIER -> {
                val lm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
                if (lm != null) {
                    _activeFactDossier.value = aiRepo.generateLocalFactDossierFallback(_selectedStyle.value, _regionName.value, lm)
                    _rewardBanner.value = getStr(R.string.vm_local_dossier_loaded, "Local Historical Fact Dossier Loaded (0 cloud tokens).")
                }
            }
            AiCapability.RIDDLE_CIPHER -> {
                val lm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
                if (lm != null) {
                    _activeAiRiddle.value = aiRepo.generateLocalRiddleFallback(_selectedStyle.value, _regionName.value, lm)
                    _rewardBanner.value = getStr(R.string.vm_local_riddle_loaded, "Local Architectural Cipher Challenge Loaded (0 cloud tokens).")
                }
            }
            AiCapability.QUEST_NARRATIVE -> {
                regenerateAiQuestForCurrentArea()
            }
        }
    }

    /**
     * Tier 3 Zero-Quota Honor Pledge Check-In:
     * Protects walking streak and awards standard explorer XP (+100 XP) and an on-device Honor Crest without cloud tokens.
     */
    fun scanLandmarkPhotoWithHonorPledge() {
        val activeLandmark = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val style = _selectedStyle.value
        val (lat, lon) = _userCoordinates.value
        val fallbackResult = aiRepo.generateHonorPledgeScanResult(
            style = style,
            landmark = activeLandmark,
            regionName = _regionName.value
        )
        _lastVisionScanResult.value = fallbackResult

        viewModelScope.launch {
            try {
                persistAddInventoryItem(
                    name = fallbackResult.awardedRelicName,
                    loreDescription = "Honor-Verified at ${fallbackResult.landmarkTitle}: ${fallbackResult.historicalVerification}",
                    itemType = "CHRONICLE",
                    rarity = "RARE",
                    originLandmark = activeLandmark?.name ?: fallbackResult.landmarkTitle,
                    characterStyle = style,
                    powerBonus = 35,
                    equipped = true
                )
                persistRecordFoundArtifact(
                    artifactCode = "HONOR_PLEDGE_SCAN",
                    name = fallbackResult.awardedRelicName,
                    loreInscription = fallbackResult.inCharacterAnalysis,
                    category = ArtifactCategory.VISION_SCAN,
                    rarity = "RARE",
                    landmarkName = activeLandmark?.name ?: fallbackResult.landmarkTitle,
                    regionName = _regionName.value,
                    characterStyle = style,
                    latitude = activeLandmark?.latitude ?: lat,
                    longitude = activeLandmark?.longitude ?: lon,
                    resonanceValue = 45,
                    isFavorite = true,
                    inspectionNotes = fallbackResult.historicalVerification
                )
                persistAwardProgress(
                    xpDelta = fallbackResult.bonusXp.toLong(),
                    landmarksDelta = 1L,
                    questsCompletedDelta = 0L
                )
                persistUnlockBadgeIfAbsent(
                    badgeId = "badge_honor_pledge_sentinel",
                    badgeCode = "HONOR_PLEDGE_SENTINEL",
                    title = "Honor-Pledged Sentinel",
                    description = "Checked in via Zero-Quota Honor Pledge at ${fallbackResult.landmarkTitle} to maintain walking streak.",
                    category = "EXPLORATION",
                    rarity = "RARE",
                    landmarkName = fallbackResult.landmarkTitle
                )

                val msg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    senderName = "${style.narratorTitle} • Honor Pledge",
                    text = "🛡️ **[ZERO-QUOTA HONOR PLEDGE CHECK-IN: ${fallbackResult.landmarkTitle}]**\n\n" +
                        "${fallbackResult.inCharacterAnalysis}\n\n" +
                        "🏛️ *Verification:* ${fallbackResult.historicalVerification}\n" +
                        "🎁 *Awarded:* **${fallbackResult.awardedRelicName}** (+${fallbackResult.bonusXp} XP)",
                    landmarkName = activeLandmark?.name ?: fallbackResult.landmarkTitle,
                    groundingSources = listOf("On-Device Local Honor Engine", fallbackResult.landmarkTitle),
                    isVisionScan = true
                )
                _chatMessages.value = _chatMessages.value + msg
                _rewardBanner.value = getStr(
                    R.string.vm_honor_pledge_verified_format,
                    "Honor Pledge Verified! +${fallbackResult.bonusXp} XP & Walking Streak Preserved (0 cloud tokens)!",
                    fallbackResult.bonusXp
                )
                recordCoreOperationSuccess("Honor Pledge (${fallbackResult.landmarkTitle})")
                engagementTracker.logEvent(
                    type = AppEventType.VISION_SCAN_VERIFIED,
                    detail = "Honor Pledge '${fallbackResult.landmarkTitle}' (+${fallbackResult.bonusXp} XP)",
                    allowAutoPromptEvaluation = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Honor pledge error", e)
            }
        }
    }

    /**
     * Tier 3 Instant Local Reflex Engine:
     * Deterministic context-aware lore from rank, style, and landmark facts without cloud tokens.
     */
    fun triggerLocalReflexDmReply() {
        val activeLandmark = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val style = _selectedStyle.value
        val reflexText = aiRepo.generateLocalReflexReply(
            style = style,
            regionName = _regionName.value,
            landmark = activeLandmark,
            userMessage = "Report area status and tactical briefing"
        )
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            senderName = "${style.narratorTitle} • Reflex",
            text = reflexText,
            landmarkName = activeLandmark?.name,
            groundingSources = listOf("On-Device Local Reflex Engine", _regionName.value)
        )
        _chatMessages.value = _chatMessages.value + msg
        _rewardBanner.value = getStr(R.string.vm_reflex_dm_rendered, "Local Reflex DM briefing rendered (0 cloud tokens).")
        if (voiceNarrator.autoNarrate.value) {
            voiceNarrator.speak(reflexText, style, title = getStr(R.string.vm_reflex_dm_title, "Local Reflex DM"))
        }
    }

    /**
     * Tier 3 On-Device Canvas Emblem Generator:
     * Composes a personalized high-contrast 512×512 emblem using Android Canvas saved with 0 cloud credits.
     */
    fun craftLocalCanvasEmblem() {
        val activeLandmark = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val landmarkName = activeLandmark?.name ?: _regionName.value
        val style = _selectedStyle.value
        val bitmap = aiRepo.generateLocalCanvasEmblem(
            style = style,
            landmarkName = landmarkName,
            regionName = _regionName.value
        )
        val emblemTitle = "On-Device ${style.title} Canvas Emblem"
        _relicArtUiState.value = RelicArtUiState.Success(bitmap, emblemTitle)

        viewModelScope.launch {
            try {
                val (lat, lon) = _userCoordinates.value
                persistAddInventoryItem(
                    name = emblemTitle,
                    loreDescription = "Geometric insignia synthesized directly on-device using Android Canvas for $landmarkName. Zero cloud credits consumed.",
                    itemType = "INSIGNIA",
                    rarity = "RARE",
                    originLandmark = landmarkName,
                    characterStyle = style,
                    powerBonus = 40,
                    equipped = true
                )
                persistRecordFoundArtifact(
                    artifactCode = "CANVAS_EMBLEM",
                    name = emblemTitle,
                    loreInscription = "On-device Canvas emblem forged under $landmarkName. 100% zero-cost local art synthesis.",
                    category = ArtifactCategory.RELIC_SHARD,
                    rarity = "RARE",
                    landmarkName = landmarkName,
                    regionName = _regionName.value,
                    characterStyle = style,
                    latitude = activeLandmark?.latitude ?: lat,
                    longitude = activeLandmark?.longitude ?: lon,
                    resonanceValue = 50,
                    isFavorite = true,
                    inspectionNotes = "Synthesized on Android Canvas."
                )
                persistAwardProgress(xpDelta = 75L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                persistUnlockBadgeIfAbsent(
                    badgeId = "badge_canvas_illuminator",
                    badgeCode = "CANVAS_ARTISAN",
                    title = "Local Canvas Artisan",
                    description = "Synthesized an on-device geometric insignia using Android Canvas without cloud credits.",
                    category = "MASTERY",
                    rarity = "RARE",
                    landmarkName = landmarkName
                )
                _rewardBanner.value = getStr(R.string.vm_canvas_emblem_crafted_75, "On-Device Canvas Emblem crafted! +75 XP (0 cloud credits used)!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Canvas emblem error", e)
            }
        }
    }

    // =========================================================================
    // SMART NON-INTRUSIVE DONATION ENGAGEMENT ENGINE
    // =========================================================================

    fun recordCoreOperationSuccess(taskTitle: String) {
        val current = _donationEngagementState.value
        val nextCount = current.successfulOperationsCount + 1
        if (current.isPermanentSupporter) {
            _donationEngagementState.value = current.copy(successfulOperationsCount = nextCount)
            return
        }

        val isMilestone = nextCount in setOf(7, 10, 25, 50)
        val shouldAutoPrompt = nextCount == 7 && !current.isHomeBannerDismissedCooldownActive

        _donationEngagementState.value = current.copy(
            successfulOperationsCount = nextCount,
            showPostOperationCard = true,
            lastCompletedTaskTitle = taskTitle,
            showMilestoneCelebrationBanner = isMilestone || current.showMilestoneCelebrationBanner,
            milestoneReachedCount = if (isMilestone) nextCount else current.milestoneReachedCount,
            shouldTriggerAutoPromptDialog = shouldAutoPrompt
        )
    }

    fun dismissHomeCompactDonationBannerFor14Days() {
        _donationEngagementState.value = _donationEngagementState.value.copy(
            isHomeBannerDismissedCooldownActive = true
        )
        _rewardBanner.value = getStr(R.string.vm_support_silenced_14d, "Support banner silenced for 14 days. You can always find it in the Guild Hub or Codex.")
    }

    fun dismissPostOperationSupportCard() {
        _donationEngagementState.value = _donationEngagementState.value.copy(
            showPostOperationCard = false
        )
    }

    fun dismissMilestoneSupportBanner() {
        _donationEngagementState.value = _donationEngagementState.value.copy(
            showMilestoneCelebrationBanner = false
        )
    }

    fun consumeAutoPromptDonationTrigger() {
        _donationEngagementState.value = _donationEngagementState.value.copy(
            shouldTriggerAutoPromptDialog = false
        )
    }

    fun showDonationStatusBanner(message: String) {
        _rewardBanner.value = message
    }

    // =========================================================================
    // ONBOARDING & SMART LIFECYCLE AUTO POP-UP ENGINE (RATE / SHARE / UPDATE)
    // =========================================================================

    fun completeOnboardingTour() {
        engagementTracker.markOnboardingCompleted()
        _rewardBanner.value = getStr(R.string.vm_onboarding_completed, "Onboarding Tour Completed! Choose any real-world waypoint or scan a plaque to begin.")
    }

    fun openSmartPromptManually(promptType: SmartPromptType) {
        engagementTracker.openPromptManuallyFromSettings(promptType)
    }

    fun acceptSmartPrompt(promptType: SmartPromptType, detail: String) {
        engagementTracker.onPromptAccepted(promptType, detail)
        when (promptType) {
            SmartPromptType.RATE_APP -> {
                _rewardBanner.value = getStr(R.string.vm_rated_thanks_xp, "Thank you for rating SideQuest! +100 Explorer XP awarded.")
                viewModelScope.launch {
                    try {
                        persistAwardProgress(xpDelta = 100L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {}
                }
            }
            SmartPromptType.SHARE_APP -> {
                _rewardBanner.value = getStr(R.string.vm_shared_thanks_xp, "SideQuest shared with fellow explorers! +75 Explorer XP awarded.")
                viewModelScope.launch {
                    try {
                        persistAwardProgress(xpDelta = 75L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {}
                }
            }
            SmartPromptType.UPDATE_APP -> {
                val ver = engagementTracker.state.value.availableUpdateVersionName
                _rewardBanner.value = getStr(R.string.vm_update_check_format, "Update check completed: v$ver", ver)
            }
            SmartPromptType.NONE -> Unit
        }
    }

    fun dismissSmartPrompt(promptType: SmartPromptType) {
        engagementTracker.onPromptDismissed(promptType)
    }

    fun logDonationInteraction(methodLabel: String) {
        engagementTracker.logEvent(
            type = AppEventType.DONATION_CLICKED,
            detail = methodLabel,
            allowAutoPromptEvaluation = false
        )
    }

    fun completeGooglePlaySupporterPurchase(tier: GooglePlayTipTier) {
        engagementTracker.logEvent(
            type = AppEventType.DONATION_CLICKED,
            detail = "Google Play Supporter Tier: ${tier.tierLabel} (${tier.priceDisplay})",
            allowAutoPromptEvaluation = false
        )
        val current = _donationEngagementState.value
        _donationEngagementState.value = current.copy(
            isPermanentSupporter = true,
            supporterTierTitle = "${tier.tierLabel} (${tier.priceDisplay}) • ${tier.title}",
            isHomeBannerDismissedCooldownActive = true,
            showPostOperationCard = false,
            showMilestoneCelebrationBanner = false,
            shouldTriggerAutoPromptDialog = false
        )
        _isGuildPassActive.value = true
        _isOfflinePackCached.value = true

        val style = _selectedStyle.value
        val (lat, lon) = _userCoordinates.value

        viewModelScope.launch {
            try {
                persistAwardProgress(
                    xpDelta = tier.bonusXp.toLong(),
                    landmarksDelta = 0L,
                    questsCompletedDelta = 0L
                )
                persistUnlockBadgeIfAbsent(
                    badgeId = "badge_guild_supporter",
                    badgeCode = "ACTIVE_GUILD_SUPPORTER",
                    title = "Active Supporter: ${tier.title}",
                    description = "Supported independent SideQuest development via Google Play (${tier.priceDisplay}). All prompts permanently disabled.",
                    category = "MASTERY",
                    rarity = "LEGENDARY",
                    landmarkName = _regionName.value
                )
                persistRecordFoundArtifact(
                    artifactCode = "PATRON_SUPPORTER_CREST",
                    name = "Patron Crest: ${tier.title}",
                    loreInscription = "Forged in gratitude for supporting independent, ad-free SideQuest development (${tier.tierLabel} • ${tier.priceDisplay}).",
                    category = ArtifactCategory.CREATOR_TOKEN,
                    rarity = "LEGENDARY",
                    landmarkName = "SideQuest Indie Guild • ${_regionName.value}",
                    regionName = _regionName.value,
                    characterStyle = style,
                    latitude = lat,
                    longitude = lon,
                    resonanceValue = 120,
                    isFavorite = true,
                    inspectionNotes = "Verified Google Play 1-Tap Supporter Receipt (${tier.skuId})."
                )
                _rewardBanner.value = getStr(
                    R.string.vm_supporter_thanks_full_format,
                    "Thank you, Active Supporter! +${tier.bonusXp} XP, Permanent Supporter Badge unlocked & all donation prompts disabled!",
                    tier.bonusXp
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed saving supporter badge to Firestore", e)
                _rewardBanner.value = getStr(R.string.vm_supporter_thanks_short, "Thank you, Active Supporter! Permanent Supporter status unlocked.")
            }
        }
    }

    fun clearVisionScanModal() {
        _lastVisionScanResult.value = null
    }

    // Daily Step & Distance Goal Boost
    fun logWalkStepBoost() {
        val previous = _dailyWalkMetersLogged.value
        val updated = (previous + 250).coerceAtMost(5000)
        _dailyWalkMetersLogged.value = updated
        val justCrossedGoal = previous < dailyWalkGoalMeters && updated >= dailyWalkGoalMeters

        viewModelScope.launch {
            try {
                val bonusXp = if (justCrossedGoal) 150L else 50L
                persistAwardProgress(xpDelta = bonusXp, landmarksDelta = 0L, questsCompletedDelta = 0L)
                if (justCrossedGoal) {
                    persistUnlockBadgeIfAbsent(
                        badgeId = "badge_daily_strider",
                        badgeCode = "DAILY_MARATHON_STRIDER",
                        title = "Daily 1km Vanguard Strider",
                        description = "Completed the 1,000m daily SideQuest walking goal!",
                        category = "EXPLORATION",
                        rarity = "EPIC",
                        landmarkName = _regionName.value
                    )
                    _rewardBanner.value = getStr(R.string.vm_walk_goal_reached, "1,000m Daily Walk Goal Reached! +150 Bonus XP & 'Daily 1km Vanguard Strider' Badge!")
                } else {
                    _rewardBanner.value = getStr(
                        R.string.vm_walk_cadence_logged_format,
                        "Logged +250m Walking Cadence ($updated / ${dailyWalkGoalMeters}m) • +50 XP!",
                        updated,
                        dailyWalkGoalMeters
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed logging step boost", e)
            }
        }
    }

    // B2B Sponsored Partner Stop Generator (Business Optimization)
    fun spawnSponsoredPartnerStop(partnerBusinessName: String, offerHeadline: String) {
        val cleanName = partnerBusinessName.trim().ifBlank { "Artisan Guild Roasters" }.take(60)
        val cleanOffer = offerHeadline.trim().ifBlank { "25% OFF Specialty Brew & Pastry" }.take(80)
        val (baseLat, baseLon) = _userCoordinates.value
        val style = _selectedStyle.value

        val newSeed = SeedLandmark(
            name = "$cleanName (Sponsored Tavern)",
            category = LandmarkCategory.COFFEE_SHOP,
            latitude = baseLat + 0.0022,
            longitude = baseLon + 0.0018,
            realFact = "Verified SideQuest Local Partner Venue. Exclusive Explorer Perk: $cleanOffer."
        )
        val currentList = _landmarks.value
        val styledStop = landmarkRepo.buildStyledLandmark(
            index = currentList.size,
            seed = newSeed,
            distanceMeters = 210,
            style = style
        ).copy(
            rewardItemName = "Partner Seal of $cleanName",
            rewardRarity = "EPIC",
            xpReward = 175
        )

        val updatedLandmarks = currentList + styledStop
        _landmarks.value = updatedLandmarks
        _selectedLandmarkIndex.value = updatedLandmarks.lastIndex

        val code = "SQ-${cleanName.filter { it.isLetterOrDigit() }.take(4).uppercase(Locale.US)}-VIP"
        val newVoucher = TavernVoucher(
            id = "voucher_${UUID.randomUUID().toString().take(8)}",
            partnerName = cleanName,
            discountHeadline = cleanOffer,
            termsDescription = "Sponsored Waypoint added to your live tactical radar.",
            redemptionCode = code,
            landmarkCategory = LandmarkCategory.COFFEE_SHOP,
            isUnlocked = true,
            isRedeemed = false
        )
        _tavernVouchers.value = listOf(newVoucher) + _tavernVouchers.value
        _rewardBanner.value = getStr(
            R.string.vm_sponsored_stop_pinned_format,
            "Sponsored Waypoint '$cleanName' pinned to map + Voucher $code unlocked!",
            cleanName,
            code
        )
    }

    // Suggestion 3: Toggle Adventurer's Guild Pass & Offline Pack Cache
    fun activateGuildPass() {
        val next = !_isGuildPassActive.value
        _isGuildPassActive.value = next
        if (next) {
            _isOfflinePackCached.value = true
            _rewardBanner.value = getStr(R.string.vm_guild_pass_activated, "Adventurer's Guild Pass Active! 1.5x XP Multiplier, Streak Shield & Offline Lore Pack unlocked.")
        } else {
            _rewardBanner.value = getStr(R.string.vm_guild_pass_deactivated, "Switched to Standard Explorer Tier.")
        }
    }

    fun toggleOfflineExpeditionCache() {
        val next = !_isOfflinePackCached.value
        _isOfflinePackCached.value = next
        _rewardBanner.value = if (next) {
            getStr(
                R.string.vm_offline_pack_cached_format,
                "Offline Expedition Pack cached for ${_regionName.value} (${_landmarks.value.size} landmarks & voice scripts).",
                _regionName.value,
                _landmarks.value.size
            )
        } else {
            getStr(
                R.string.vm_offline_pack_cleared_format,
                "Offline cache cleared for ${_regionName.value}.",
                _regionName.value
            )
        }
    }

    // Suggestion 5: Hands-Free Pocket Dungeon Master Walk Mode
    fun togglePocketWalkMode() {
        val next = !_isPocketWalkModeActive.value
        _isPocketWalkModeActive.value = next
        pocketWalkJob?.cancel()

        if (next) {
            val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
            _pocketWalkProximityMeters.value = 95
            val startMsg = "Pocket Dungeon Master active. Keep your phone in your pocket—I will narrate spatial proximity cues as you approach ${activeLm?.name ?: "your next waypoint"}."
            val waypointFallback = activeLm?.name ?: "waypoint"
            _rewardBanner.value = getStr(
                R.string.vm_pocket_dm_on_format,
                "Hands-Free Pocket DM Mode ON • Tracking proximity to $waypointFallback",
                waypointFallback
            )
            voiceNarrator.speak(
                startMsg,
                _selectedStyle.value,
                title = getStr(R.string.vm_pocket_dm_title, "Pocket DM • Hands-Free Mode")
            )

            pocketWalkJob = viewModelScope.launch {
                val steps = listOf(75, 45, 15, 0)
                for (dist in steps) {
                    delay(4500L)
                    if (!_isPocketWalkModeActive.value) break
                    _pocketWalkProximityMeters.value = dist
                    val currentLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value) ?: break
                    when (dist) {
                        45 -> {
                            val cue = "Proximity alert: 45 meters to ${currentLm.name}. ${currentLm.rpgNodeTitle} energy signature rising."
                            voiceNarrator.speak(
                                cue,
                                _selectedStyle.value,
                                title = getStr(R.string.vm_pocket_proximity_title_format, "45m to ${currentLm.name}", currentLm.name)
                            )
                        }
                        0 -> {
                            val arrival = "Waypoint reached: ${currentLm.name}. ${currentLm.loreSnippet} Challenge: ${currentLm.questChallenge}"
                            _rewardBanner.value = getStr(
                                R.string.vm_pocket_arrived_format,
                                "Arrived at ${currentLm.name}! Auto-narrating waypoint briefing.",
                                currentLm.name
                            )
                            voiceNarrator.speak(
                                arrival,
                                _selectedStyle.value,
                                title = getStr(R.string.vm_pocket_arrived_title_format, "Arrived • ${currentLm.name}", currentLm.name)
                            )
                        }
                    }
                }
            }
        } else {
            _rewardBanner.value = getStr(R.string.vm_pocket_dm_paused, "Hands-Free Pocket DM Mode paused.")
        }
    }

    // Suggestion 2: Activate Curated Official Heritage Trail
    fun activateOfficialHeritageTrail(trail: OfficialHeritageTrail) {
        _selectedStyle.value = trail.recommendedStyle
        val matchingPreset = presetRegions.firstOrNull {
            it.name.contains(trail.cityRegionName.substringBefore(" —"), ignoreCase = true)
        } ?: presetRegions.first()

        _regionName.value = "${trail.title} (${trail.sponsorAuthority})"
        _userCoordinates.value = Pair(matchingPreset.latitude, matchingPreset.longitude)
        _selectedLandmarkIndex.value = 0
        _activeFirestoreQuestId.value = null
        _lastVisionScanResult.value = null

        _rewardBanner.value = getStr(
            R.string.vm_trail_activated_format,
            "Official Trail Activated: ${trail.title} • ${trail.xpMultiplierLabel}",
            trail.title,
            trail.xpMultiplierLabel
        )
        generateQuestForCoordinates(
            lat = matchingPreset.latitude,
            lon = matchingPreset.longitude,
            regionLabel = "${trail.cityRegionName} • ${trail.title}",
            fallbackPreset = matchingPreset
        )
    }

    // Suggestion 4: Multimodal Camera / Photo Relic Scan with Gemini Vision
    fun scanLandmarkPhotoWithGemini(bitmap: Bitmap) {
        if (_isScanningVision.value) return
        val activeLandmark = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val style = _selectedStyle.value

        val quotaResult = usageQuotaManager?.evaluateTierForCapability(AiCapability.VISION_PROOF)
            ?: com.example.model.QuotaAcquisitionResult.Allowed(AiExecutionTier.TIER_1_STANDARD)

        if (quotaResult is com.example.model.QuotaAcquisitionResult.Exhausted) {
            showSmartQuotaRecovery(AiCapability.VISION_PROOF, isRateLimited429 = false)
            return
        }

        val preferredTier = (quotaResult as com.example.model.QuotaAcquisitionResult.Allowed).tier

        viewModelScope.launch {
            _isScanningVision.value = true
            _aiErrorBanner.value = null
            try {
                val scanResult = aiRepo.analyzeLandmarkPhotoWithGemini(
                    bitmap = bitmap,
                    style = style,
                    landmark = activeLandmark,
                    regionName = _regionName.value,
                    preferredTier = preferredTier
                )
                scanResult.onSuccess { result ->
                    if (result.usedCloudQuota) {
                        usageQuotaManager?.recordUsageSuccess(AiCapability.VISION_PROOF, currentUserId)
                    }
                    val multiplier = if (_isGuildPassActive.value) 1.5 else 1.0
                    val finalXp = (result.bonusXp * multiplier).toInt()
                    val finalResult = result.copy(bonusXp = finalXp)
                    _lastVisionScanResult.value = finalResult

                    persistAddInventoryItem(
                        name = finalResult.awardedRelicName,
                        loreDescription = "Vision-Verified at ${finalResult.landmarkTitle}: ${finalResult.historicalVerification}",
                        itemType = "CHRONICLE",
                        rarity = "EPIC",
                        originLandmark = activeLandmark?.name ?: finalResult.landmarkTitle,
                        characterStyle = style,
                        powerBonus = 48,
                        equipped = true
                    )

                    val (scanLat, scanLon) = _userCoordinates.value
                    persistRecordFoundArtifact(
                        artifactCode = "VISION_SCAN",
                        name = finalResult.awardedRelicName,
                        loreInscription = finalResult.inCharacterAnalysis,
                        category = ArtifactCategory.VISION_SCAN,
                        rarity = "EPIC",
                        landmarkName = activeLandmark?.name ?: finalResult.landmarkTitle,
                        regionName = _regionName.value,
                        characterStyle = style,
                        latitude = activeLandmark?.latitude ?: scanLat,
                        longitude = activeLandmark?.longitude ?: scanLon,
                        resonanceValue = 65,
                        isFavorite = true,
                        inspectionNotes = "Optical verification: ${finalResult.historicalVerification}"
                    )

                    persistAwardProgress(
                        xpDelta = finalXp.toLong(),
                        landmarksDelta = 1L,
                        questsCompletedDelta = 0L
                    )

                    persistUnlockBadgeIfAbsent(
                        badgeId = "badge_vision_scanner",
                        badgeCode = "GEMINI_VISION_SCANNER",
                        title = "Optical Lore Seeker",
                        description = "Verified ${finalResult.landmarkTitle} using Gemini Multimodal Vision.",
                        category = "HISTORIAN",
                        rarity = "EPIC",
                        landmarkName = activeLandmark?.name ?: finalResult.landmarkTitle
                    )

                    val chatSummary = "📸 **[GEMINI VISION SCAN: ${finalResult.landmarkTitle}]**\n\n" +
                        "${finalResult.inCharacterAnalysis}\n\n" +
                        "🏛️ *Verified Intel:* ${finalResult.historicalVerification}\n" +
                        "🎁 *Minted Epic Chronicle:* **${finalResult.awardedRelicName}** (+$finalXp XP)"

                    val msg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        senderName = "${style.narratorTitle} • Vision Lens",
                        text = chatSummary,
                        landmarkName = activeLandmark?.name ?: finalResult.landmarkTitle,
                        groundingSources = listOf("Gemini Multimodal Vision", finalResult.landmarkTitle),
                        isVisionScan = true
                    )
                    _chatMessages.value = _chatMessages.value + msg
                    _rewardBanner.value = getStr(
                        R.string.vm_vision_verified_format,
                        "Vision Scan Verified! +$finalXp XP & Minted '${finalResult.awardedRelicName}'",
                        finalXp,
                        finalResult.awardedRelicName
                    )
                    recordCoreOperationSuccess("Gemini Vision Scan (${finalResult.landmarkTitle})")
                    engagementTracker.logEvent(
                        type = AppEventType.VISION_SCAN_VERIFIED,
                        detail = "Verified '${finalResult.landmarkTitle}' (+$finalXp XP)",
                        allowAutoPromptEvaluation = true
                    )

                    voiceNarrator.speak(
                        text = "${finalResult.landmarkTitle} verified. ${finalResult.inCharacterAnalysis}",
                        style = style,
                        title = "Vision Scan • ${finalResult.landmarkTitle}"
                    )
                }.onFailure { err ->
                    val is429 = err is QuotaExceededException ||
                        err.message?.contains("429") == true ||
                        err.message?.contains("quota", ignoreCase = true) == true
                    if (is429) {
                        usageQuotaManager?.recordFallbackCascade(currentUserId)
                        showSmartQuotaRecovery(AiCapability.VISION_PROOF, isRateLimited429 = true)
                    } else {
                        _aiErrorBanner.value = err.localizedMessage ?: getStr(R.string.vm_vision_err_analyze, "Failed to analyze landmark image.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multimodal vision scan error", e)
                val is429 = e is QuotaExceededException ||
                    e.message?.contains("429") == true ||
                    e.message?.contains("quota", ignoreCase = true) == true
                if (is429) {
                    usageQuotaManager?.recordFallbackCascade(currentUserId)
                    showSmartQuotaRecovery(AiCapability.VISION_PROOF, isRateLimited429 = true)
                } else {
                    _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_vision_err_generic, "Vision scanner encountered an error.")
                }
            } finally {
                _isScanningVision.value = false
            }
        }
    }

    // Suggestion 1: Redeem B2B2C Local Partner Tavern Voucher
    fun redeemTavernVoucher(voucherId: String) {
        val target = _tavernVouchers.value.firstOrNull { it.id == voucherId } ?: return
        if (target.isRedeemed) return

        _tavernVouchers.value = _tavernVouchers.value.map { v ->
            if (v.id == voucherId) v.copy(isUnlocked = true, isRedeemed = true) else v
        }

        viewModelScope.launch {
            try {
                persistAddInventoryItem(
                    name = "Tavern Seal: ${target.partnerName.take(28)}",
                    loreDescription = "Redeemed Perk (${target.redemptionCode}): ${target.discountHeadline}",
                    itemType = "ELIXIR",
                    rarity = "RARE",
                    originLandmark = target.partnerName,
                    characterStyle = _selectedStyle.value,
                    powerBonus = 30,
                    equipped = false
                )
                persistAwardProgress(xpDelta = 100L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                _rewardBanner.value = getStr(
                    R.string.vm_voucher_activated_format,
                    "Voucher Activated! Show code ${target.redemptionCode} at ${target.partnerName} (+100 XP)",
                    target.redemptionCode,
                    target.partnerName
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed saving voucher relic", e)
            }
        }
    }

    // Suggestion 3: Relic Alchemy Forge (Combine 2 Relics into a Legendary Artifact)
    fun forgeLegendaryRelic() {
        val currentItems = inventoryState.value
        if (currentItems.size < 2) {
            _aiErrorBanner.value = getStr(R.string.vm_forge_need_2, "Collect at least 2 Relics from waypoints or scans to use the Relic Alchemy Forge!")
            return
        }
        val first = currentItems[0]
        val second = currentItems[1]
        val style = _selectedStyle.value
        val forgedName = "${style.title}'s Astral Singularity Core"
        val combinedPower = (first.powerBonus + second.powerBonus + 45L).coerceAtMost(500L).toInt()

        viewModelScope.launch {
            try {
                persistAddInventoryItem(
                    name = forgedName,
                    loreDescription = "Synthesized in the Guild Alchemy Forge by fusing '${first.name}' and '${second.name}'. Radiates mythic neighborhood resonance.",
                    itemType = "KEYSTONE",
                    rarity = "LEGENDARY",
                    originLandmark = "Guild Alchemy Forge • ${_regionName.value}",
                    characterStyle = style,
                    powerBonus = combinedPower,
                    equipped = true
                )
                val (forgeLat, forgeLon) = _userCoordinates.value
                persistRecordFoundArtifact(
                    artifactCode = "ALCHEMY_FORGE",
                    name = forgedName,
                    loreInscription = "Synthesized in the Guild Alchemy Forge by fusing '${first.name}' and '${second.name}'. Radiates mythic neighborhood resonance.",
                    category = ArtifactCategory.RELIC_SHARD,
                    rarity = "LEGENDARY",
                    landmarkName = "Guild Alchemy Forge • ${_regionName.value}",
                    regionName = _regionName.value,
                    characterStyle = style,
                    latitude = forgeLat,
                    longitude = forgeLon,
                    resonanceValue = combinedPower,
                    isFavorite = true,
                    inspectionNotes = "Alchemical fusion of ${first.name} + ${second.name}."
                )
                persistAwardProgress(xpDelta = 225L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                persistUnlockBadgeIfAbsent(
                    badgeId = "badge_relic_alchemist",
                    badgeCode = "RELIC_ALCHEMIST_MASTER",
                    title = "Grand Relic Alchemist",
                    description = "Forged $forgedName by combining relics in the Codex.",
                    category = "MASTERY",
                    rarity = "LEGENDARY",
                    landmarkName = _regionName.value
                )
                _rewardBanner.value = getStr(
                    R.string.vm_forge_success_format,
                    "Forged Legendary '$forgedName' (+$combinedPower Resonance & +225 XP)!",
                    forgedName,
                    combinedPower
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed forging legendary relic", e)
                _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_forge_failed, "Failed to forge relic.")
            }
        }
    }

    // Found Artifacts & Curio Vault Management (Firestore CRUD / Guest In-Memory)
    fun toggleFavoriteArtifact(artifact: FoundArtifact) {
        if (artifact.id.isBlank()) return
        val nextFav = !artifact.isFavorite
        viewModelScope.launch {
            try {
                persistToggleFavoriteArtifact(artifact.id, nextFav)
                _rewardBanner.value = if (nextFav) {
                    getStr(R.string.vm_fav_pinned_format, "Pinned '${artifact.name}' to Showcase Favorites ★", artifact.name)
                } else {
                    getStr(R.string.vm_fav_removed_format, "Removed '${artifact.name}' from Showcase Favorites", artifact.name)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed toggling favorite artifact", e)
                _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_fav_update_failed, "Could not update artifact favorite status.")
            }
        }
    }

    fun updateArtifactNotes(artifactId: String, notes: String) {
        if (artifactId.isBlank()) return
        viewModelScope.launch {
            try {
                persistUpdateArtifactNotes(artifactId, notes)
                _rewardBanner.value = if (isAuthenticatedSession) {
                    getStr(R.string.vm_notes_saved_cloud, "Explorer field notes saved to Cloud Firestore!")
                } else {
                    getStr(R.string.vm_notes_saved_local, "Explorer field notes saved locally!")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating artifact notes", e)
                _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_notes_save_failed, "Could not save field notes.")
            }
        }
    }

    fun deleteFoundArtifact(artifactId: String) {
        if (artifactId.isBlank()) return
        viewModelScope.launch {
            try {
                persistDeleteFoundArtifact(artifactId)
                _rewardBanner.value = getStr(R.string.vm_artifact_removed, "Artifact removed from Vault.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting artifact", e)
            }
        }
    }

    fun catalogCustomFieldArtifact(
        name: String,
        category: ArtifactCategory,
        landmarkName: String,
        inscription: String,
        notes: String,
        overrideSuccessBanner: String? = null
    ) {
        val cleanName = name.trim().ifBlank { "Inscribed Street Curio" }.take(110)
        val cleanLandmark = landmarkName.trim().ifBlank {
            _landmarks.value.getOrNull(_selectedLandmarkIndex.value)?.name ?: _regionName.value
        }.take(110)
        val cleanInscription = inscription.trim().ifBlank {
            "Field-cataloged curio recovered along ${_regionName.value} during a ${_selectedStyle.value.title} patrol."
        }.take(550)
        val cleanNotes = notes.trim().ifBlank {
            "Cataloged by explorer on foot."
        }.take(380)
        val (lat, lon) = _userCoordinates.value
        val style = _selectedStyle.value

        viewModelScope.launch {
            try {
                persistRecordFoundArtifact(
                    artifactCode = "FIELD_CATALOG",
                    name = cleanName,
                    loreInscription = cleanInscription,
                    category = category,
                    rarity = "RARE",
                    landmarkName = cleanLandmark,
                    regionName = _regionName.value,
                    characterStyle = style,
                    latitude = lat,
                    longitude = lon,
                    resonanceValue = 50,
                    isFavorite = true,
                    inspectionNotes = cleanNotes
                )
                persistAwardProgress(xpDelta = 120L, landmarksDelta = 1L, questsCompletedDelta = 0L)
                persistUnlockBadgeIfAbsent(
                    badgeId = "badge_curio_archivist",
                    badgeCode = "CURIO_FIELD_ARCHIVIST",
                    title = "Guild Curio Archivist",
                    description = "Cataloged '$cleanName' into the Artifact Vault.",
                    category = "HISTORIAN",
                    rarity = "RARE",
                    landmarkName = cleanLandmark
                )
                _rewardBanner.value = overrideSuccessBanner
                    ?: getStr(
                        R.string.vm_cataloged_artifact_format,
                        "Cataloged '$cleanName' into Vault (+120 XP & 'Guild Curio Archivist' Badge)!",
                        cleanName
                    )
                engagementTracker.logEvent(
                    type = AppEventType.ARTIFACT_CATALOGED,
                    detail = "Cataloged '$cleanName' at $cleanLandmark",
                    allowAutoPromptEvaluation = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed cataloging custom artifact", e)
                _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_artifact_catalog_failed, "Failed to catalog artifact.")
            }
        }
    }

    fun narrateArtifactOrBadgeLore(title: String, loreText: String) {
        voiceNarrator.speak(
            text = "$title. $loreText",
            style = _selectedStyle.value,
            title = title
        )
    }

    // Approved Suggestion: Dynamic Weather & Time-of-Day World Event Engine
    fun cycleWorldAtmosphereEvent() {
        val entries = WorldAtmosphereEvent.entries
        val nextIdx = (entries.indexOf(_activeAtmosphereEvent.value) + 1) % entries.size
        val next = entries[nextIdx]
        _activeAtmosphereEvent.value = next
        _rewardBanner.value = getStr(
            R.string.vm_atmosphere_shifted_format,
            "World Atmosphere Shifted: ${next.title} • ${next.bonusLabel}",
            next.title,
            next.bonusLabel
        )
    }

    // Approved Suggestion: Community Creator Quest Builder Studio
    fun publishCreatorQuest(
        questTitle: String,
        neighborhood: String,
        featuredLandmark: String,
        riddleChallenge: String
    ) {
        val cleanTitle = questTitle.trim().ifBlank { "The Hidden Lantern Trail" }.take(90)
        val cleanRegion = neighborhood.trim().ifBlank { _regionName.value }.take(80)
        val cleanStop = featuredLandmark.trim().ifBlank { "Historic Cornerstone Plaque" }.take(80)
        val cleanRiddle = riddleChallenge.trim().ifBlank {
            "Count the bronze stars on the archway and whisper the founding year to the Dungeon Master."
        }.take(240)
        val style = _selectedStyle.value
        val (baseLat, baseLon) = _userCoordinates.value

        val creatorSeed = SeedLandmark(
            name = "$cleanStop (Creator Trail)",
            category = LandmarkCategory.PLAQUE,
            latitude = baseLat + 0.0016,
            longitude = baseLon - 0.0014,
            realFact = "Community Creator Trail in $cleanRegion. Explorer Riddle: $cleanRiddle"
        )
        val currentList = _landmarks.value
        val newLandmark = landmarkRepo.buildStyledLandmark(
            index = currentList.size,
            seed = creatorSeed,
            distanceMeters = 165,
            style = style
        ).copy(
            questChallenge = cleanRiddle,
            rewardItemName = "Architect Seal: $cleanTitle",
            rewardRarity = "EPIC",
            xpReward = 190
        )

        val updated = currentList + newLandmark
        _landmarks.value = updated
        _selectedLandmarkIndex.value = updated.lastIndex
        _activeQuestTitle.value = cleanTitle

        viewModelScope.launch {
            try {
                persistRecordCompletedQuest(
                    title = "$cleanTitle (Creator Trail)",
                    characterStyle = style,
                    regionName = cleanRegion,
                    summary = "Community-Authored Quest featuring $cleanStop. Riddle: $cleanRiddle",
                    landmarksCount = updated.size,
                    xpEarned = 200,
                    distanceMeters = updated.sumOf { it.distanceMeters },
                    stopsSummary = updated.map { it.name }
                )
                persistRecordFoundArtifact(
                    artifactCode = "CREATOR_STUDIO",
                    name = "Trailblazer Seal: $cleanTitle",
                    loreInscription = "Minted by authoring the Community Walking Trail '$cleanTitle' in $cleanRegion. Featured riddle at $cleanStop: $cleanRiddle",
                    category = ArtifactCategory.CREATOR_TOKEN,
                    rarity = "EPIC",
                    landmarkName = cleanStop,
                    regionName = cleanRegion,
                    characterStyle = style,
                    latitude = newLandmark.latitude,
                    longitude = newLandmark.longitude,
                    resonanceValue = 70,
                    isFavorite = true,
                    inspectionNotes = "Published to Community Trail Exchange."
                )
                persistAwardProgress(xpDelta = 200L, landmarksDelta = 1L, questsCompletedDelta = 1L)
                persistUnlockBadgeIfAbsent(
                    badgeId = "badge_community_trailblazer",
                    badgeCode = "COMMUNITY_QUEST_ARCHITECT",
                    title = "Community Quest Architect",
                    description = "Designed and published '$cleanTitle' in the Creator Quest Studio.",
                    category = "MASTERY",
                    rarity = "EPIC",
                    landmarkName = cleanStop
                )
                _rewardBanner.value = getStr(
                    R.string.vm_creator_published_format,
                    "Published Creator Trail '$cleanTitle'! +200 XP, Trailblazer Artifact & Architect Badge unlocked!",
                    cleanTitle
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed publishing creator quest", e)
            }
        }
    }

    fun logCompletedQuestToJournal(
        questName: String,
        location: String,
        summary: String,
        landmarksCount: Int = 3,
        distanceMeters: Int = 750,
        xpEarned: Int = 250
    ) {
        val cleanName = questName.trim().ifBlank { "${_selectedStyle.value.title} Expedition" }.take(130)
        val cleanLocation = location.trim().ifBlank { _regionName.value }.take(110)
        val cleanSummary = summary.trim().ifBlank {
            "Completed all waypoints along the walking route in $cleanLocation."
        }.take(900)
        val style = _selectedStyle.value
        val stops = _landmarks.value.take(landmarksCount.coerceIn(1, 10)).map { it.name }
            .ifEmpty { listOf(cleanLocation) }

        viewModelScope.launch {
            try {
                persistRecordCompletedQuest(
                    title = cleanName,
                    characterStyle = style,
                    regionName = cleanLocation,
                    summary = cleanSummary,
                    landmarksCount = landmarksCount.coerceIn(1, 20),
                    xpEarned = xpEarned.coerceIn(50, 5000),
                    distanceMeters = distanceMeters.coerceIn(100, 100_000),
                    stopsSummary = stops
                )
                persistAwardProgress(
                    xpDelta = xpEarned.toLong(),
                    landmarksDelta = landmarksCount.toLong(),
                    questsCompletedDelta = 1L
                )
                _rewardBanner.value = getStr(
                    R.string.vm_quest_recorded_format,
                    "Quest '$cleanName' recorded in Quest Journal (+$xpEarned XP)!",
                    cleanName,
                    xpEarned
                )
                engagementTracker.logEvent(
                    type = AppEventType.QUEST_COMPLETED,
                    detail = "Logged '$cleanName' in $cleanLocation (+$xpEarned XP)",
                    allowAutoPromptEvaluation = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed logging completed quest", e)
                _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_quest_record_failed, "Failed to record completed quest.")
            }
        }
    }

    fun completeQuestById(quest: CompletedQuest) {
        if (quest.id.isBlank() || quest.status == "COMPLETED") return
        val totalStops = quest.landmarksCount.toInt().coerceIn(1, 20)
        val earnedXp = (quest.xpEarned.toInt()).coerceAtLeast(totalStops * 110)
        viewModelScope.launch {
            try {
                persistUpdateQuestProgress(
                    questId = quest.id,
                    completedStops = totalStops,
                    xpEarned = earnedXp,
                    status = "COMPLETED",
                    summary = "Completed all $totalStops waypoints in ${quest.regionName}. ${quest.summary}".take(950)
                )
                persistAwardProgress(
                    xpDelta = 150L,
                    landmarksDelta = 1L,
                    questsCompletedDelta = 1L
                )
                _rewardBanner.value = getStr(
                    R.string.vm_quest_marked_completed_format,
                    "Quest '${quest.title}' marked COMPLETED in Quest Journal (+150 Bonus XP)!",
                    quest.title
                )
                engagementTracker.logEvent(
                    type = AppEventType.QUEST_COMPLETED,
                    detail = "Completed '${quest.title}' in ${quest.regionName}",
                    allowAutoPromptEvaluation = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed completing quest", e)
                _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_quest_mark_complete_failed, "Could not mark quest completed.")
            }
        }
    }

    fun deleteQuestFromJournal(questId: String) {
        if (questId.isBlank()) return
        viewModelScope.launch {
            try {
                persistDeleteQuest(questId)
                _rewardBanner.value = getStr(R.string.vm_quest_removed, "Quest chronicle removed from Quest Journal.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed deleting quest", e)
                _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_quest_remove_failed, "Could not remove quest entry.")
            }
        }
    }

    // =========================================================================
    // NEW AI FEATURES (Image Gen & Edit, Fact-Check Dossier, AI Riddle, Voice Log)
    // =========================================================================

    fun clearRelicArtState() {
        _relicArtUiState.value = RelicArtUiState.Idle
    }

    fun togglePaidImageApiApproval(approved: Boolean) {
        _isPaidImageApiApproved.value = approved
        if (approved) {
            _relicArtUiState.value = RelicArtUiState.Idle
            _rewardBanner.value = getStr(R.string.vm_paid_studio_unlocked, "Paid Image API Unlocked by User Opt-In. Live Gemini Relic Art Studio enabled.")
        } else {
            _rewardBanner.value = getStr(R.string.vm_paid_studio_locked, "Zero-Bill Free-Tier Eco Mode Active ($0 Cost Guard). Paid Image API calls locked.")
        }
    }

    /**
     * Generates custom RPG Relic / Travelogue artwork at runtime using the studio image model tier.
     * Strictly follows Firebase AI Logic guidelines: 60s timeout, real loading state, no mock fallback,
     * and respects the user's Free-Tier Eco Mode opt-in preference.
     */
    fun generateAiRelicArtwork(customPrompt: String) {
        if (!_isPaidImageApiApproved.value) {
            val msg = getStr(R.string.vm_studio_eco_blocked, "Zero-Bill Eco Mode is active ($0 cost guard). Toggle 'Allow Paid Image API' in the studio card if you decide to run live Gemini Image Generation.")
            _relicArtUiState.value = RelicArtUiState.Error(msg)
            _aiErrorBanner.value = msg
            return
        }

        val quotaResult = usageQuotaManager?.evaluateTierForCapability(AiCapability.IMAGE_STUDIO)
            ?: com.example.model.QuotaAcquisitionResult.Allowed(AiExecutionTier.TIER_1_STANDARD)

        if (quotaResult is com.example.model.QuotaAcquisitionResult.Exhausted) {
            showSmartQuotaRecovery(AiCapability.IMAGE_STUDIO, isRateLimited429 = false)
            return
        }

        val preferredTier = (quotaResult as com.example.model.QuotaAcquisitionResult.Allowed).tier
        val style = _selectedStyle.value
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val cleanPrompt = customPrompt.trim().ifBlank {
            "Ornate ${style.title} RPG artifact discovered at ${activeLm?.name ?: _regionName.value}, dramatic studio lighting, intricate engravings, high fantasy concept art"
        }

        viewModelScope.launch {
            _relicArtUiState.value = RelicArtUiState.Loading(cleanPrompt)
            try {
                val artResult = withTimeout(60_000L) {
                    aiRepo.generateRelicArtworkWithGemini(
                        promptText = cleanPrompt,
                        style = style,
                        landmarkName = activeLm?.name ?: _regionName.value,
                        preferredTier = preferredTier
                    )
                }
                val bitmap = artResult.bitmap
                if (bitmap != null) {
                    if (artResult.usedCloudQuota) {
                        usageQuotaManager?.recordUsageSuccess(AiCapability.IMAGE_STUDIO, currentUserId)
                        _relicArtUiState.value = RelicArtUiState.Success(bitmap, cleanPrompt)
                        persistAwardProgress(xpDelta = 150L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                        persistUnlockBadgeIfAbsent(
                            badgeId = "badge_arcane_illuminator",
                            badgeCode = "AI_RELIC_ILLUMINATOR",
                            title = "Arcane Relic Illuminator",
                            description = "Synthesized custom RPG relic artwork using Gemini Image Generation.",
                            category = "MASTERY",
                            rarity = "EPIC",
                            landmarkName = activeLm?.name ?: _regionName.value
                        )
                        _rewardBanner.value = getStr(R.string.vm_studio_synthesized, "AI Relic Artwork Synthesized! +150 XP & 'Arcane Relic Illuminator' Badge!")
                    } else {
                        if (artResult.hitRateLimit429) {
                            usageQuotaManager?.recordFallbackCascade(currentUserId)
                            showSmartQuotaRecovery(AiCapability.IMAGE_STUDIO, isRateLimited429 = true)
                        }
                        _relicArtUiState.value = RelicArtUiState.Success(bitmap, cleanPrompt)
                        _rewardBanner.value = getStr(R.string.vm_studio_canvas_0, "On-Device Canvas Emblem crafted (0 cloud credits used)!")
                    }
                } else {
                    _relicArtUiState.value = RelicArtUiState.Error(artResult.errorMessage ?: getStr(R.string.vm_studio_no_image, "No image was returned by the model."))
                }
            } catch (e: TimeoutCancellationException) {
                _relicArtUiState.value = RelicArtUiState.Error(getStr(R.string.vm_studio_timeout, "Image generation timed out after 60 seconds."))
            } catch (e: QuotaExceededException) {
                usageQuotaManager?.recordFallbackCascade(currentUserId)
                showSmartQuotaRecovery(AiCapability.IMAGE_STUDIO, isRateLimited429 = true)
                _relicArtUiState.value = RelicArtUiState.Error(getStr(R.string.vm_studio_rate_limit, "Rate limit exceeded. Try On-Device Canvas Emblem."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Relic image generation failed", e)
                val is429 = e is QuotaExceededException ||
                    e.message?.contains("429") == true ||
                    e.message?.contains("quota", ignoreCase = true) == true
                if (is429) {
                    usageQuotaManager?.recordFallbackCascade(currentUserId)
                    showSmartQuotaRecovery(AiCapability.IMAGE_STUDIO, isRateLimited429 = true)
                }
                _relicArtUiState.value = RelicArtUiState.Error(e.localizedMessage ?: getStr(R.string.vm_studio_failed, "Image generation failed."))
            }
        }
    }

    /**
     * Edits/remixes the currently generated AI Relic artwork or a landmark plaque snapshot.
     */
    fun editAiRelicArtwork(editInstruction: String) {
        if (!_isPaidImageApiApproved.value) {
            val msg = getStr(R.string.vm_studio_remix_eco_blocked, "Zero-Bill Eco Mode is active ($0 cost guard). Toggle 'Allow Paid Image API' in the studio card if you decide to run live Gemini Image Remixing.")
            _relicArtUiState.value = RelicArtUiState.Error(msg)
            _aiErrorBanner.value = msg
            return
        }

        val quotaResult = usageQuotaManager?.evaluateTierForCapability(AiCapability.IMAGE_STUDIO)
            ?: com.example.model.QuotaAcquisitionResult.Allowed(AiExecutionTier.TIER_1_STANDARD)

        if (quotaResult is com.example.model.QuotaAcquisitionResult.Exhausted) {
            showSmartQuotaRecovery(AiCapability.IMAGE_STUDIO, isRateLimited429 = false)
            return
        }

        val preferredTier = (quotaResult as com.example.model.QuotaAcquisitionResult.Allowed).tier
        val instruction = editInstruction.trim().ifBlank {
            "Add glowing astral runes and golden twilight mist around the artifact"
        }
        val currentState = _relicArtUiState.value
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val style = _selectedStyle.value
        val sourceBitmap = when (currentState) {
            is RelicArtUiState.Success -> currentState.bitmap
            else -> DungeonMasterAiRepository.createLandmarkPlaqueBitmap(
                landmarkName = activeLm?.name ?: _regionName.value,
                categoryLabel = activeLm?.category?.label ?: "Relic",
                realFact = activeLm?.realFactOrAddress ?: "SideQuest Explorer Curio",
                coordinatesText = "SideQuest Guild Vault"
            )
        }

        viewModelScope.launch {
            _relicArtUiState.value = RelicArtUiState.Loading(instruction)
            try {
                val editResult = withTimeout(60_000L) {
                    aiRepo.editRelicArtworkWithGemini(
                        inputBitmap = sourceBitmap,
                        editInstruction = instruction,
                        style = style,
                        landmarkName = activeLm?.name ?: _regionName.value,
                        preferredTier = preferredTier
                    )
                }
                val editedBitmap = editResult.bitmap
                if (editedBitmap != null && editResult.usedCloudQuota) {
                    usageQuotaManager?.recordUsageSuccess(AiCapability.IMAGE_STUDIO, currentUserId)
                    _relicArtUiState.value = RelicArtUiState.Success(editedBitmap, instruction)
                    persistAwardProgress(xpDelta = 125L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                    _rewardBanner.value = getStr(R.string.vm_studio_remixed, "AI Relic Image Remixed! (+125 XP)")
                } else {
                    if (editResult.hitRateLimit429) {
                        usageQuotaManager?.recordFallbackCascade(currentUserId)
                        showSmartQuotaRecovery(AiCapability.IMAGE_STUDIO, isRateLimited429 = true)
                    }
                    _relicArtUiState.value = RelicArtUiState.Error(
                        editResult.errorMessage ?: getStr(R.string.vm_studio_no_edit_image, "No edited image was returned by the model.")
                    )
                }
            } catch (e: TimeoutCancellationException) {
                _relicArtUiState.value = RelicArtUiState.Error(getStr(R.string.vm_studio_edit_timeout, "Image editing timed out after 60 seconds."))
            } catch (e: QuotaExceededException) {
                usageQuotaManager?.recordFallbackCascade(currentUserId)
                showSmartQuotaRecovery(AiCapability.IMAGE_STUDIO, isRateLimited429 = true)
                _relicArtUiState.value = RelicArtUiState.Error(getStr(R.string.vm_studio_edit_rate_limit, "Rate limit exceeded. Please try again later."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Relic image editing failed", e)
                val is429 = e is QuotaExceededException ||
                    e.message?.contains("429") == true ||
                    e.message?.contains("quota", ignoreCase = true) == true
                if (is429) {
                    usageQuotaManager?.recordFallbackCascade(currentUserId)
                    showSmartQuotaRecovery(AiCapability.IMAGE_STUDIO, isRateLimited429 = true)
                }
                _relicArtUiState.value = RelicArtUiState.Error(e.localizedMessage ?: getStr(R.string.vm_studio_edit_failed, "Image editing failed."))
            }
        }
    }

    fun dismissFactDossier() {
        _activeFactDossier.value = null
    }

    /**
     * Synthesizes a Grounded Real-World Historical & Architectural Fact-Check Dossier (`gemini-3.8-flash`).
     */
    fun requestLandmarkFactCheckDossier() {
        if (_isGeneratingFactDossier.value) return
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
            ?: _landmarks.value.firstOrNull() ?: return
        val style = _selectedStyle.value

        val quotaResult = usageQuotaManager?.evaluateTierForCapability(AiCapability.FACT_DOSSIER)
            ?: com.example.model.QuotaAcquisitionResult.Allowed(AiExecutionTier.TIER_1_STANDARD)

        if (quotaResult is com.example.model.QuotaAcquisitionResult.Exhausted) {
            showSmartQuotaRecovery(AiCapability.FACT_DOSSIER, isRateLimited429 = false)
            return
        }

        val preferredTier = (quotaResult as com.example.model.QuotaAcquisitionResult.Allowed).tier

        viewModelScope.launch {
            _isGeneratingFactDossier.value = true
            _aiErrorBanner.value = null
            try {
                val result = aiRepo.generateLandmarkFactDossier(
                    style = style,
                    regionName = _regionName.value,
                    landmark = activeLm,
                    preferredTier = preferredTier
                )
                result.onSuccess { dossier ->
                    if (dossier.usedCloudQuota) {
                        usageQuotaManager?.recordUsageSuccess(AiCapability.FACT_DOSSIER, currentUserId)
                    }
                    _activeFactDossier.value = dossier
                    persistAwardProgress(xpDelta = 110L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                    val factsBullet = dossier.verifiedHistoricalFacts.joinToString("\n• ", prefix = "• ")
                    val chatReport = "🏛️ **[AI FACT-CHECK & DEEP LORE DOSSIER: ${dossier.landmarkName}]**\n" +
                        "**Era:** ${dossier.verifiedEra} • **Style:** ${dossier.architecturalStyle}\n\n" +
                        "$factsBullet\n\n" +
                        "🔍 *Walker Observation Tip:* ${dossier.localInsiderTip}\n" +
                        "⚔️ *RPG Hook:* ${dossier.rpgEncounterHook}"
                    appendDungeonMasterMessage(chatReport, landmarkName = dossier.landmarkName)
                    _rewardBanner.value = getStr(
                        R.string.vm_dossier_verified_format,
                        "Verified Fact-Check Dossier for ${dossier.landmarkName} (+110 XP)!",
                        dossier.landmarkName
                    )
                    if (voiceNarrator.autoNarrate.value) {
                        voiceNarrator.speak(
                            text = "${dossier.landmarkName}, ${dossier.verifiedEra}. ${dossier.verifiedHistoricalFacts.firstOrNull().orEmpty()} ${dossier.rpgEncounterHook}",
                            style = style,
                            title = getStr(R.string.vm_dossier_tts_title_format, "Fact Dossier • ${dossier.landmarkName}", dossier.landmarkName)
                        )
                    }
                }.onFailure { err ->
                    val is429 = err is QuotaExceededException ||
                        err.message?.contains("429") == true ||
                        err.message?.contains("quota", ignoreCase = true) == true
                    if (is429) {
                        usageQuotaManager?.recordFallbackCascade(currentUserId)
                        showSmartQuotaRecovery(AiCapability.FACT_DOSSIER, isRateLimited429 = true)
                    } else {
                        _aiErrorBanner.value = err.localizedMessage ?: getStr(R.string.vm_dossier_failed, "Could not synthesize Fact-Check Dossier.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fact dossier error", e)
                val is429 = e is QuotaExceededException ||
                    e.message?.contains("429") == true ||
                    e.message?.contains("quota", ignoreCase = true) == true
                if (is429) {
                    usageQuotaManager?.recordFallbackCascade(currentUserId)
                    showSmartQuotaRecovery(AiCapability.FACT_DOSSIER, isRateLimited429 = true)
                } else {
                    _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_dossier_error, "Fact-Check Dossier error.")
                }
            } finally {
                _isGeneratingFactDossier.value = false
            }
        }
    }

    fun dismissAiRiddle() {
        _activeAiRiddle.value = null
        _riddleAnswerStatus.value = null
        _isActiveRiddleSolved.value = false
    }

    /**
     * Generates an interactive 3-option AI Landmark Riddle & Cipher Duel.
     */
    fun requestAiLandmarkRiddle() {
        if (_isGeneratingRiddle.value) return
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
            ?: _landmarks.value.firstOrNull() ?: return
        val style = _selectedStyle.value

        val quotaResult = usageQuotaManager?.evaluateTierForCapability(AiCapability.RIDDLE_CIPHER)
            ?: com.example.model.QuotaAcquisitionResult.Allowed(AiExecutionTier.TIER_1_STANDARD)

        if (quotaResult is com.example.model.QuotaAcquisitionResult.Exhausted) {
            showSmartQuotaRecovery(AiCapability.RIDDLE_CIPHER, isRateLimited429 = false)
            return
        }

        val preferredTier = (quotaResult as com.example.model.QuotaAcquisitionResult.Allowed).tier

        viewModelScope.launch {
            _isGeneratingRiddle.value = true
            _riddleAnswerStatus.value = null
            _isActiveRiddleSolved.value = false
            _aiErrorBanner.value = null
            try {
                val result = aiRepo.generateLandmarkRiddleWithGemini(
                    style = style,
                    regionName = _regionName.value,
                    landmark = activeLm,
                    preferredTier = preferredTier
                )
                result.onSuccess { riddle ->
                    if (riddle.usedCloudQuota) {
                        usageQuotaManager?.recordUsageSuccess(AiCapability.RIDDLE_CIPHER, currentUserId)
                    }
                    _activeAiRiddle.value = riddle
                    if (voiceNarrator.autoNarrate.value) {
                        voiceNarrator.speak(
                            text = "Riddle Challenge at ${riddle.landmarkName}: ${riddle.question}",
                            style = style,
                            title = getStr(R.string.vm_riddle_tts_title_format, "Riddle Duel • ${riddle.landmarkName}", riddle.landmarkName)
                        )
                    }
                }.onFailure { err ->
                    val is429 = err is QuotaExceededException ||
                        err.message?.contains("429") == true ||
                        err.message?.contains("quota", ignoreCase = true) == true
                    if (is429) {
                        usageQuotaManager?.recordFallbackCascade(currentUserId)
                        showSmartQuotaRecovery(AiCapability.RIDDLE_CIPHER, isRateLimited429 = true)
                    } else {
                        _aiErrorBanner.value = err.localizedMessage ?: getStr(R.string.vm_riddle_failed, "Could not generate AI Riddle.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Riddle error", e)
                val is429 = e is QuotaExceededException ||
                    e.message?.contains("429") == true ||
                    e.message?.contains("quota", ignoreCase = true) == true
                if (is429) {
                    usageQuotaManager?.recordFallbackCascade(currentUserId)
                    showSmartQuotaRecovery(AiCapability.RIDDLE_CIPHER, isRateLimited429 = true)
                } else {
                    _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_riddle_gen_failed, "AI Riddle generation failed.")
                }
            } finally {
                _isGeneratingRiddle.value = false
            }
        }
    }

    fun submitAiRiddleAnswer(selectedIndex: Int) {
        val riddle = _activeAiRiddle.value ?: return
        val isCorrect = selectedIndex == riddle.correctIndex
        val style = _selectedStyle.value
        if (isCorrect) {
            _isActiveRiddleSolved.value = true
            _riddleAnswerStatus.value = getStr(
                R.string.vm_riddle_correct_status_format,
                "CORRECT! +${riddle.xpReward} XP — ${riddle.explanationLore}",
                riddle.xpReward,
                riddle.explanationLore
            )
            viewModelScope.launch {
                try {
                    persistAwardProgress(
                        xpDelta = riddle.xpReward.toLong(),
                        landmarksDelta = 1L,
                        questsCompletedDelta = 0L
                    )
                    persistUnlockBadgeIfAbsent(
                        badgeId = "badge_ai_riddle_duelist",
                        badgeCode = "AI_CIPHER_GRANDMASTER",
                        title = "Cipher Duel Grandmaster",
                        description = "Solved the Dungeon Master's AI Landmark Riddle at ${riddle.landmarkName}.",
                        category = "LORE",
                        rarity = "EPIC",
                        landmarkName = riddle.landmarkName
                    )
                    _rewardBanner.value = getStr(
                        R.string.vm_riddle_solved_banner_format,
                        "Riddle Solved! +${riddle.xpReward} XP & 'Cipher Duel Grandmaster' Badge!",
                        riddle.xpReward
                    )
                    recordCoreOperationSuccess("Riddle Duel (${riddle.landmarkName})")
                    engagementTracker.logEvent(
                        type = AppEventType.RIDDLE_SOLVED,
                        detail = "Solved AI Riddle at ${riddle.landmarkName} (+${riddle.xpReward} XP)",
                        allowAutoPromptEvaluation = true
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed awarding riddle reward", e)
                }
            }
            if (voiceNarrator.autoNarrate.value) {
                voiceNarrator.speak(
                    text = "Correct! ${riddle.explanationLore}",
                    style = style,
                    title = getStr(R.string.vm_riddle_solved_tts_title, "Riddle Solved!")
                )
            }
        } else {
            _isActiveRiddleSolved.value = false
            _riddleAnswerStatus.value = getStr(
                R.string.vm_riddle_hint_status_format,
                "Not quite! Hint: ${riddle.hint}",
                riddle.hint
            )
        }
    }

    /**
     * Polishes a live voice-transcribed field note with Gemini (`gemini-3.8-flash`) and saves it as a FoundArtifact in Firestore.
     */
    fun transcribeAndMintVoiceFieldLog(rawSpokenText: String) {
        val cleanText = rawSpokenText.trim()
        if (cleanText.isBlank() || _isPolishingVoiceNote.value) return
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val landmarkName = activeLm?.name ?: _regionName.value
        val style = _selectedStyle.value
        val (lat, lon) = _userCoordinates.value

        val quotaResult = usageQuotaManager?.evaluateTierForCapability(AiCapability.AUDIO_FIELD_NOTE)
            ?: com.example.model.QuotaAcquisitionResult.Allowed(AiExecutionTier.TIER_1_STANDARD)

        if (quotaResult is com.example.model.QuotaAcquisitionResult.Exhausted) {
            // Tier 3 Zero-Cost Local fallback: Save raw spoken note directly into vault so thought is never lost
            catalogCustomFieldArtifact(
                name = "Raw Voice Note • $landmarkName",
                category = ArtifactCategory.PLAQUE_RUBBING,
                landmarkName = landmarkName,
                inscription = cleanText,
                notes = "Saved locally in raw audio transcript form (0 Cloud Quota Used).",
                overrideSuccessBanner = getStr(
                    R.string.vm_voice_log_raw_banner,
                    "Raw Voice Note preserved in Vault! Auto-cleanup available when quota resets."
                )
            )
            return
        }

        val preferredTier = (quotaResult as com.example.model.QuotaAcquisitionResult.Allowed).tier

        viewModelScope.launch {
            _isPolishingVoiceNote.value = true
            _aiErrorBanner.value = null
            try {
                val result = aiRepo.polishSpokenFieldNoteWithGemini(
                    rawTranscript = cleanText,
                    style = style,
                    regionName = _regionName.value,
                    landmarkName = landmarkName,
                    preferredTier = preferredTier
                )
                result.onSuccess { polished ->
                    if (polished.usedCloudQuota) {
                        usageQuotaManager?.recordUsageSuccess(AiCapability.AUDIO_FIELD_NOTE, currentUserId)
                    }
                    persistRecordFoundArtifact(
                        artifactCode = "VOICE_FIELD_LOG",
                        name = polished.curioTitle,
                        loreInscription = "${polished.polishedChronicle} (${polished.historicalConnection})",
                        category = ArtifactCategory.PLAQUE_RUBBING,
                        rarity = "EPIC",
                        landmarkName = landmarkName,
                        regionName = _regionName.value,
                        characterStyle = style,
                        latitude = activeLm?.latitude ?: lat,
                        longitude = activeLm?.longitude ?: lon,
                        resonanceValue = 60,
                        isFavorite = true,
                        inspectionNotes = "Voice Transcript: \"$cleanText\""
                    )
                    persistAwardProgress(xpDelta = 140L, landmarksDelta = 1L, questsCompletedDelta = 0L)
                    persistUnlockBadgeIfAbsent(
                        badgeId = "badge_voice_chronicler",
                        badgeCode = "VOICE_LORE_CHRONICLER",
                        title = "Spoken Lore Chronicler",
                        description = "Transcribed and immortalized a spoken field note at $landmarkName with Gemini.",
                        category = "LORE",
                        rarity = "EPIC",
                        landmarkName = landmarkName
                    )
                    val msg = "🎙️ **[VOICE FIELD LOG IMMORTALIZED: ${polished.curioTitle}]**\n\n" +
                        "${polished.polishedChronicle}\n\n" +
                        "📍 *${polished.historicalConnection}* (+140 XP • Saved to Firestore Artifact Vault)"
                    appendDungeonMasterMessage(msg, landmarkName = landmarkName)
                    _rewardBanner.value = getStr(
                        R.string.vm_voice_log_minted_format,
                        "Voice Field Log Minted as '${polished.curioTitle}' (+140 XP)!",
                        polished.curioTitle
                    )
                    if (voiceNarrator.autoNarrate.value) {
                        voiceNarrator.speak(
                            text = "${polished.curioTitle}. ${polished.polishedChronicle}",
                            style = style,
                            title = polished.curioTitle
                        )
                    }
                }.onFailure { err ->
                    val is429 = err is QuotaExceededException ||
                        err.message?.contains("429") == true ||
                        err.message?.contains("quota", ignoreCase = true) == true
                    if (is429) {
                        usageQuotaManager?.recordFallbackCascade(currentUserId)
                        showSmartQuotaRecovery(AiCapability.AUDIO_FIELD_NOTE, isRateLimited429 = true)
                    } else {
                        _aiErrorBanner.value = err.localizedMessage ?: getStr(R.string.vm_voice_log_failed, "Failed to polish voice field note.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice field note error", e)
                val is429 = e is QuotaExceededException ||
                    e.message?.contains("429") == true ||
                    e.message?.contains("quota", ignoreCase = true) == true
                if (is429) {
                    usageQuotaManager?.recordFallbackCascade(currentUserId)
                    showSmartQuotaRecovery(AiCapability.AUDIO_FIELD_NOTE, isRateLimited429 = true)
                } else {
                    _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_voice_note_failed, "Voice field note failed.")
                }
            } finally {
                _isPolishingVoiceNote.value = false
            }
        }
    }

    // Suggestion 6: Co-Op Party Role & Riddle Resolution
    fun selectPartyRole(role: PartyRole) {
        _selectedPartyRole.value = role
        _rewardBanner.value = getStr(
            R.string.vm_coop_role_set_format,
            "Co-Op Party Role set to ${role.title} (${role.perk})",
            role.title,
            role.perk
        )
    }

    fun cyclePartyRoomCode() {
        val codes = listOf("NEXUS-7", "AETHEL-4", "FINCH-9", "VANCE-2")
        val nextIdx = (codes.indexOf(_partyCode.value) + 1) % codes.size
        _partyCode.value = codes[nextIdx]
        _isCoOpRiddleSolved.value = false
        _rewardBanner.value = getStr(
            R.string.vm_coop_room_synced_format,
            "Synced with Co-Op Party Room ${_partyCode.value} (3/3 Explorers Ready)",
            _partyCode.value
        )
    }

    fun solveCoOpPartyRiddle() {
        if (_isCoOpRiddleSolved.value) return
        _isCoOpRiddleSolved.value = true
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)

        viewModelScope.launch {
            try {
                persistAwardProgress(xpDelta = 250L, landmarksDelta = 1L, questsCompletedDelta = 0L)
                persistUnlockBadgeIfAbsent(
                    badgeId = "badge_coop_vanguard",
                    badgeCode = "COOP_PARTY_VANGUARD",
                    title = "Guild Co-Op Vanguard",
                    description = "Combined Scout, Lorekeeper, and Quartermaster clues in Party ${_partyCode.value}.",
                    category = "MASTERY",
                    rarity = "LEGENDARY",
                    landmarkName = activeLm?.name ?: _regionName.value
                )
                _rewardBanner.value = getStr(R.string.vm_coop_riddle_solved, "Co-Op Party Riddle Solved! +250 Shared Party XP & 'Guild Co-Op Vanguard' Badge Unlocked!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed awarding co-op riddle progress", e)
            }
        }
    }

    fun buildNeighborhoodLeaderboard(profile: AdventurerProfile?): List<NeighborhoodStanding> {
        val userXp = profile?.xp ?: 0L
        val userLandmarks = profile?.landmarksVisited ?: 0L
        val youFallback = getStr(R.string.vm_you_explorer, "You (Explorer)")
        val userName = profile?.displayName?.ifBlank { youFallback } ?: youFallback

        val rivals = listOf(
            NeighborhoodStanding(
                rankPosition = 0,
                explorerName = "Lyra_StarChart",
                titleBadge = "Grand Cartographer",
                xpScore = 1420L,
                landmarksCleared = 14L,
                isCurrentUser = false
            ),
            NeighborhoodStanding(
                rankPosition = 0,
                explorerName = "ZeroCool_2099",
                titleBadge = "Master Lorekeeper",
                xpScore = 980L,
                landmarksCleared = 9L,
                isCurrentUser = false
            ),
            NeighborhoodStanding(
                rankPosition = 0,
                explorerName = "Prof_Cromwell",
                titleBadge = "Urban Pathfinder",
                xpScore = 540L,
                landmarksCleared = 6L,
                isCurrentUser = false
            ),
            NeighborhoodStanding(
                rankPosition = 0,
                explorerName = "Detective_Marlowe",
                titleBadge = "Guild Initiate",
                xpScore = 210L,
                landmarksCleared = 3L,
                isCurrentUser = false
            ),
            NeighborhoodStanding(
                rankPosition = 0,
                explorerName = getStr(R.string.vm_you_suffix_format, "$userName (You)", userName),
                titleBadge = if (_isGuildPassActive.value) {
                    getStr(R.string.vm_guild_pass_sovereign, "Guild Pass Sovereign")
                } else {
                    getStr(R.string.vm_active_explorer, "Active Explorer")
                },
                xpScore = userXp,
                landmarksCleared = userLandmarks,
                isCurrentUser = true
            )
        )

        return rivals
            .sortedByDescending { it.xpScore }
            .mapIndexed { idx, entry -> entry.copy(rankPosition = idx + 1) }
    }

    fun selectCharacterStyle(style: CharacterStyle) {
        _selectedStyle.value = style
        restyleCurrentLandmarks(style)
        viewModelScope.launch {
            try {
                persistUpdateCharacterStyle(style)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update character style", e)
            }
        }
        val firstLandmark = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val greeting = "${style.narratorTitle} online. Persona shifted to ${style.title}. ${firstLandmark?.loreSnippet ?: ""}"
        appendDungeonMasterMessage(
            text = greeting,
            landmarkName = firstLandmark?.name
        )
        if (voiceNarrator.autoNarrate.value) {
            voiceNarrator.speak(greeting, style, title = "${style.narratorTitle} • ${style.title}")
        }
    }

    private fun restyleCurrentLandmarks(style: CharacterStyle) {
        val current = _landmarks.value
        val updated = current.mapIndexed { index, lm ->
            val rebuilt = landmarkRepo.buildStyledLandmark(
                index = index,
                seed = SeedLandmark(
                    name = lm.name,
                    category = lm.category,
                    latitude = lm.latitude,
                    longitude = lm.longitude,
                    realFact = lm.realFactOrAddress
                ),
                distanceMeters = lm.distanceMeters,
                style = style
            )
            rebuilt.copy(isCleared = lm.isCleared)
        }
        _landmarks.value = updated
    }

    fun selectLandmark(index: Int, narrateAloud: Boolean = false) {
        val list = _landmarks.value
        if (index !in list.indices) return
        _selectedLandmarkIndex.value = index
        _lastVisionScanResult.value = null
        val lm = list[index]
        if (narrateAloud || voiceNarrator.autoNarrate.value) {
            voiceNarrator.speak(
                text = "${lm.rpgNodeTitle}. ${lm.loreSnippet}",
                style = _selectedStyle.value,
                title = lm.name
            )
        }
    }

    fun narrateCurrentWaypoint() {
        val lm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value) ?: return
        if (voiceNarrator.isSpeaking.value) {
            voiceNarrator.stop()
        } else {
            voiceNarrator.speak(
                text = "${lm.rpgNodeTitle}. ${lm.loreSnippet} Your challenge: ${lm.questChallenge}",
                style = _selectedStyle.value,
                title = lm.name
            )
        }
    }

    fun selectPresetRegion(preset: PresetCityRegion) {
        _regionName.value = preset.name
        _userCoordinates.value = Pair(preset.latitude, preset.longitude)
        _selectedLandmarkIndex.value = 0
        _activeFirestoreQuestId.value = null
        _lastVisionScanResult.value = null
        generateQuestForCoordinates(
            lat = preset.latitude,
            lon = preset.longitude,
            regionLabel = preset.name,
            fallbackPreset = preset
        )
    }

    fun scanRealDeviceGps() {
        viewModelScope.launch {
            _isGeneratingQuest.value = true
            _aiErrorBanner.value = null
            _activeFirestoreQuestId.value = null
            _lastVisionScanResult.value = null
            val location = landmarkRepo.fetchCurrentDeviceLocation()
            if (location != null) {
                val lat = location.latitude
                val lon = location.longitude
                _userCoordinates.value = Pair(lat, lon)
                val label = getStr(
                    R.string.vm_live_gps_sector_format,
                    String.format(Locale.US, "Live GPS Sector (%.4f, %.4f)", lat, lon),
                    lat,
                    lon
                )
                _regionName.value = label
                generateQuestForCoordinates(
                    lat = lat,
                    lon = lon,
                    regionLabel = label,
                    fallbackPreset = presetRegions.first()
                )
            } else {
                val fallback = presetRegions.first()
                _rewardBanner.value = getStr(
                    R.string.vm_gps_calibrated_format,
                    "GPS signal indoor/unavailable — calibrated to ${fallback.name}.",
                    fallback.name
                )
                selectPresetRegion(fallback)
            }
        }
    }

    fun searchRealCityOrPlace(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isGeneratingQuest.value = true
            _aiErrorBanner.value = null
            _activeFirestoreQuestId.value = null
            _lastVisionScanResult.value = null
            val coords = landmarkRepo.geocodePlaceQuery(query)
            if (coords != null) {
                val (lat, lon) = coords
                _userCoordinates.value = Pair(lat, lon)
                val cleanTitle = query.trim().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }
                _regionName.value = cleanTitle
                generateQuestForCoordinates(
                    lat = lat,
                    lon = lon,
                    regionLabel = cleanTitle,
                    fallbackPreset = presetRegions.first()
                )
            } else {
                _isGeneratingQuest.value = false
                _aiErrorBanner.value = getStr(
                    R.string.vm_geocode_failed_format,
                    "Could not geocode '$query'. Try a city or landmark like 'Kyoto', 'Boston Common', or 'SoHo NYC'.",
                    query
                )
            }
        }
    }

    fun regenerateAiQuestForCurrentArea() {
        _activeFirestoreQuestId.value = null
        _lastVisionScanResult.value = null
        val (lat, lon) = _userCoordinates.value
        generateQuestForCoordinates(
            lat = lat,
            lon = lon,
            regionLabel = _regionName.value,
            fallbackPreset = presetRegions.first()
        )
    }

    private fun generateQuestForCoordinates(
        lat: Double,
        lon: Double,
        regionLabel: String,
        fallbackPreset: PresetCityRegion
    ) {
        viewModelScope.launch {
            _isGeneratingQuest.value = true
            _aiErrorBanner.value = null
            val style = _selectedStyle.value

            try {
                val discovered = landmarkRepo.discoverRealLandmarksNearby(
                    latitude = lat,
                    longitude = lon,
                    style = style,
                    fallbackRegion = fallbackPreset
                )
                _landmarks.value = discovered
                _selectedLandmarkIndex.value = 0

                val quotaResult = usageQuotaManager?.evaluateTierForCapability(AiCapability.QUEST_NARRATIVE)
                val preferredTier = when (quotaResult) {
                    is com.example.model.QuotaAcquisitionResult.Allowed -> quotaResult.tier
                    is com.example.model.QuotaAcquisitionResult.Exhausted -> {
                        usageQuotaManager?.recordFallbackCascade(currentUserId)
                        AiExecutionTier.TIER_3_LOCAL_FALLBACK
                    }
                    null -> AiExecutionTier.TIER_1_STANDARD
                }

                val aiResult = aiRepo.generateWalkingQuestWithGemini(
                    style = style,
                    regionName = regionLabel,
                    landmarks = discovered,
                    preferredTier = preferredTier
                )

                val narrative = aiResult.getOrNull()
                if (narrative?.usedCloudQuota == true) {
                    usageQuotaManager?.recordUsageSuccess(AiCapability.QUEST_NARRATIVE, currentUserId)
                }

                val finalLandmarks = narrative?.enrichedLandmarks ?: discovered
                val questTitle = narrative?.questTitle
                    ?: "${style.title}: $regionLabel Expedition"
                val questIntro = narrative?.questIntro
                    ?: "Your ${style.title} sensors have pinpointed ${finalLandmarks.size} real-world landmarks across $regionLabel."

                if (aiResult.isFailure) {
                    val err = aiResult.exceptionOrNull()
                    _aiErrorBanner.value = err?.localizedMessage
                        ?: getStr(R.string.vm_quest_enrichment_err, "Gemini quest enrichment encountered an error.")
                }

                _landmarks.value = finalLandmarks
                _activeQuestTitle.value = questTitle
                _activeQuestIntro.value = questIntro

                val totalDistance = finalLandmarks.sumOf { it.distanceMeters }
                val questId = persistCreateQuest(
                    title = questTitle,
                    characterStyle = style,
                    regionName = regionLabel,
                    summary = questIntro,
                    landmarksCount = finalLandmarks.size,
                    distanceMeters = totalDistance,
                    stopsSummary = finalLandmarks.map { it.name }
                )
                _activeFirestoreQuestId.value = questId
                engagementTracker.logEvent(
                    type = AppEventType.QUEST_GENERATED,
                    detail = "Generated '$questTitle' ($regionLabel • ${finalLandmarks.size} stops)",
                    allowAutoPromptEvaluation = false
                )

                appendDungeonMasterMessage(
                    text = "**$questTitle**\n\n$questIntro\n\nFirst Stop: **${finalLandmarks.firstOrNull()?.name.orEmpty()}** — ${finalLandmarks.firstOrNull()?.loreSnippet.orEmpty()}",
                    landmarkName = finalLandmarks.firstOrNull()?.name
                )

                if (voiceNarrator.autoNarrate.value) {
                    voiceNarrator.speak(
                        text = "$questTitle. $questIntro",
                        style = style,
                        title = questTitle
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error generating walking quest", e)
                _aiErrorBanner.value = e.localizedMessage ?: getStr(R.string.vm_quest_generate_failed, "Failed to generate walking quest.")
            } finally {
                _isGeneratingQuest.value = false
            }
        }
    }

    fun performLandmarkEncounter(landmarkIndex: Int, chosenAction: String) {
        val currentList = _landmarks.value
        if (landmarkIndex !in currentList.indices) return
        val landmark = currentList[landmarkIndex]
        val style = _selectedStyle.value
        val wasAlreadyCleared = landmark.isCleared

        val updatedList = currentList.mapIndexed { idx, item ->
            if (idx == landmarkIndex) item.copy(isCleared = true) else item
        }
        _landmarks.value = updatedList

        if (!wasAlreadyCleared) {
            _dailyWalkMetersLogged.value = (_dailyWalkMetersLogged.value + landmark.distanceMeters)
                .coerceAtMost(5000)
        }

        val clearedCount = updatedList.count { it.isCleared }
        val allCleared = clearedCount == updatedList.size

        // Check if this encounter completes any Daily Guild Bounty
        var bountyBonusXp = 0
        var completedBountyTitle: String? = null
        _dailyBounties.value = _dailyBounties.value.map { bounty ->
            val categoryMatch = bounty.targetCategory == landmark.category ||
                (bounty.targetCategory == LandmarkCategory.PLAQUE && landmark.category == LandmarkCategory.MONUMENT)
            if (!bounty.isCompleted && categoryMatch) {
                bountyBonusXp += bounty.bonusXp
                completedBountyTitle = bounty.title
                bounty.copy(isCompleted = true)
            } else {
                bounty
            }
        }

        // Unlock matching B2B2C Local Partner Tavern Voucher
        _tavernVouchers.value = _tavernVouchers.value.map { voucher ->
            val catMatch = voucher.landmarkCategory == landmark.category ||
                (voucher.landmarkCategory == LandmarkCategory.PLAQUE && landmark.category == LandmarkCategory.MONUMENT)
            if (catMatch) voucher.copy(isUnlocked = true) else voucher
        }

        viewModelScope.launch {
            try {
                if (!wasAlreadyCleared) {
                    val atmosphere = _activeAtmosphereEvent.value
                    val isWeatherFavored = atmosphere.favoredCategory == landmark.category
                    val weatherResonanceMultiplier = if (isWeatherFavored) atmosphere.xpMultiplier else 1.0

                    persistAddInventoryItem(
                        name = landmark.rewardItemName,
                        loreDescription = "Recovered at ${landmark.name} (${landmark.category.label}). ${landmark.realFactOrAddress}",
                        itemType = landmark.rewardItemType,
                        rarity = landmark.rewardRarity,
                        originLandmark = landmark.name,
                        characterStyle = style,
                        powerBonus = landmark.xpReward / 4,
                        equipped = false
                    )

                    val artifactCat = ArtifactCategory.fromLandmarkCategory(landmark.category)
                    val baseResonance = (landmark.xpReward / 3).coerceAtLeast(25)
                    val finalResonance = (baseResonance * weatherResonanceMultiplier).toInt()

                    persistRecordFoundArtifact(
                        artifactCode = "WAYPOINT_CLEAR",
                        name = landmark.rewardItemName,
                        loreInscription = "${landmark.loreSnippet} Historical grounding: ${landmark.realFactOrAddress}",
                        category = artifactCat,
                        rarity = if (allCleared) "LEGENDARY" else landmark.rewardRarity,
                        landmarkName = landmark.name,
                        regionName = _regionName.value,
                        characterStyle = style,
                        latitude = landmark.latitude,
                        longitude = landmark.longitude,
                        resonanceValue = finalResonance,
                        isFavorite = allCleared || landmark.rewardRarity == "LEGENDARY",
                        inspectionNotes = "Recovered via action '$chosenAction' under ${atmosphere.title}."
                    )

                    val questBonusXp = if (allCleared) 200L else 0L
                    val rawXp = landmark.xpReward.toLong() + questBonusXp + bountyBonusXp.toLong()
                    val guildMultiplier = if (_isGuildPassActive.value) 1.5 else 1.0
                    val totalDeltaXp = (rawXp * guildMultiplier * weatherResonanceMultiplier).toLong()

                    persistAwardProgress(
                        xpDelta = totalDeltaXp,
                        landmarksDelta = 1L,
                        questsCompletedDelta = if (allCleared) 1L else 0L
                    )

                    val (badgeId, badgeTitle, badgeCat) = when (landmark.category) {
                        LandmarkCategory.PARK -> Triple(
                            "badge_park_explorer",
                            "Sanctuary Pathfinder",
                            "EXPLORATION"
                        )
                        LandmarkCategory.PLAQUE -> Triple(
                            "badge_plaque_scholar",
                            "Keeper of Brass Plaques",
                            "HISTORIAN"
                        )
                        LandmarkCategory.COFFEE_SHOP -> Triple(
                            "badge_cafe_alchemist",
                            "Caffeine & Rumors Initiate",
                            "LORE"
                        )
                        LandmarkCategory.MONUMENT, LandmarkCategory.MUSEUM -> Triple(
                            "badge_monument_sentinel",
                            "Monument Vanguard",
                            "MASTERY"
                        )
                    }
                    persistUnlockBadgeIfAbsent(
                        badgeId = badgeId,
                        badgeCode = badgeId.uppercase(Locale.US),
                        title = badgeTitle,
                        description = "Investigated ${landmark.name} as a ${style.title}.",
                        category = badgeCat,
                        rarity = landmark.rewardRarity,
                        landmarkName = landmark.name
                    )

                    if (allCleared) {
                        persistUnlockBadgeIfAbsent(
                            badgeId = "badge_quest_master_${style.id.lowercase(Locale.US)}",
                            badgeCode = "QUEST_MASTER_${style.id}",
                            title = "${style.title} Vanguard",
                            description = "Completed every real-world waypoint in ${_activeQuestTitle.value}.",
                            category = if (style == CharacterStyle.CYBERPUNK_SCOUT) "CYBERPUNK" else "MASTERY",
                            rarity = "LEGENDARY",
                            landmarkName = landmark.name
                        )
                    }

                    val existingQuestId = _activeFirestoreQuestId.value
                    val totalXpEarned = updatedList.filter { it.isCleared }.sumOf { it.xpReward } + bountyBonusXp
                    val newStatus = if (allCleared) "COMPLETED" else "ACTIVE"
                    val summaryText = if (allCleared) {
                        "Completed all ${updatedList.size} real-world waypoints in ${_regionName.value}, culminating at ${landmark.name}."
                    } else {
                        "Cleared $clearedCount of ${updatedList.size} waypoints. Latest discovery: ${landmark.name}."
                    }

                    if (existingQuestId != null) {
                        persistUpdateQuestProgress(
                            questId = existingQuestId,
                            completedStops = clearedCount,
                            xpEarned = totalXpEarned,
                            status = newStatus,
                            summary = summaryText
                        )
                        if (allCleared) {
                            _activeFirestoreQuestId.value = null
                        }
                    } else {
                        val createdId = persistCreateQuest(
                            title = _activeQuestTitle.value,
                            characterStyle = style,
                            regionName = _regionName.value,
                            summary = summaryText,
                            landmarksCount = updatedList.size,
                            distanceMeters = updatedList.sumOf { it.distanceMeters },
                            stopsSummary = updatedList.map { it.name }
                        )
                        if (allCleared) {
                            persistUpdateQuestProgress(
                                questId = createdId,
                                completedStops = clearedCount,
                                xpEarned = totalXpEarned,
                                status = "COMPLETED",
                                summary = summaryText
                            )
                        } else {
                            _activeFirestoreQuestId.value = createdId
                        }
                    }

                    val bountySuffix = if (completedBountyTitle != null) {
                        getStr(
                            R.string.vm_bounty_unlocked_suffix_format,
                            " • Bounty '$completedBountyTitle' & Partner Voucher Unlocked!",
                            completedBountyTitle!!
                        )
                    } else {
                        ""
                    }
                    _rewardBanner.value = if (allCleared) {
                        getStr(
                            R.string.vm_quest_completed_banner_format,
                            "Quest Completed! +$totalDeltaXp XP & Looted '${landmark.rewardItemName}'$bountySuffix",
                            totalDeltaXp,
                            landmark.rewardItemName,
                            bountySuffix
                        )
                    } else {
                        getStr(
                            R.string.vm_waypoint_cleared_banner_format,
                            "Waypoint Cleared! +$totalDeltaXp XP & Looted '${landmark.rewardItemName}'$bountySuffix",
                            totalDeltaXp,
                            landmark.rewardItemName,
                            bountySuffix
                        )
                    }
                    recordCoreOperationSuccess(
                        if (allCleared) "Walking Quest '${_activeQuestTitle.value}'" else "Waypoint '${landmark.name}'"
                    )
                    engagementTracker.logEvent(
                        type = if (allCleared) AppEventType.QUEST_COMPLETED else AppEventType.WAYPOINT_CLEARED,
                        detail = if (allCleared) {
                            "Completed Quest '${_activeQuestTitle.value}' at ${landmark.name}"
                        } else {
                            "Cleared Waypoint '${landmark.name}' ($clearedCount/${updatedList.size})"
                        },
                        allowAutoPromptEvaluation = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving waypoint progress to Firestore", e)
            }
        }

        sendChatMessageToDungeonMaster(
            userText = "At ${landmark.name}: $chosenAction",
            overrideLandmark = landmark
        )
    }

    fun sendChatMessageToDungeonMaster(
        userText: String,
        overrideLandmark: RealLandmark? = null
    ) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isDungeonMasterStreaming.value) return

        val activeLandmark = overrideLandmark ?: _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val style = _selectedStyle.value

        val quotaResult = usageQuotaManager?.evaluateTierForCapability(AiCapability.COMPANION_CHAT)
            ?: com.example.model.QuotaAcquisitionResult.Allowed(AiExecutionTier.TIER_1_STANDARD)

        if (quotaResult is com.example.model.QuotaAcquisitionResult.Exhausted) {
            showSmartQuotaRecovery(AiCapability.COMPANION_CHAT, isRateLimited429 = false)
            return
        }

        val preferredTier = (quotaResult as com.example.model.QuotaAcquisitionResult.Allowed).tier

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            senderName = getStr(R.string.vm_sender_you_format, "You (${style.title})", style.title),
            text = trimmed,
            landmarkName = activeLandmark?.name
        )

        val dmMessageId = UUID.randomUUID().toString()
        val placeholderDm = ChatMessage(
            id = dmMessageId,
            isUser = false,
            senderName = style.narratorTitle,
            text = getStr(R.string.vm_dm_consulting, "Consulting the real-world archives..."),
            landmarkName = activeLandmark?.name,
            isStreaming = true,
            groundingSources = listOfNotNull(
                activeLandmark?.name,
                _regionName.value
            )
        )

        val historySnapshot = _chatMessages.value
        _chatMessages.value = historySnapshot + userMessage + placeholderDm
        _isDungeonMasterStreaming.value = true
        _aiErrorBanner.value = null

        viewModelScope.launch {
            var finalReplyText = ""
            var usedCloudQuota = false
            try {
                aiRepo.streamDungeonMasterReplyEvents(
                    style = style,
                    regionName = _regionName.value,
                    activeLandmark = activeLandmark,
                    recentMessages = historySnapshot,
                    userMessage = trimmed,
                    preferredTier = preferredTier
                ).collect { chunk ->
                    finalReplyText = chunk.text
                    usedCloudQuota = chunk.usedCloudQuota
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == dmMessageId) {
                            msg.copy(text = chunk.text, isStreaming = true)
                        } else {
                            msg
                        }
                    }
                }

                if (usedCloudQuota) {
                    usageQuotaManager?.recordUsageSuccess(AiCapability.COMPANION_CHAT, currentUserId)
                }

                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == dmMessageId) {
                        msg.copy(text = finalReplyText, isStreaming = false)
                    } else {
                        msg
                    }
                }

                if (voiceNarrator.autoNarrate.value && finalReplyText.isNotBlank()) {
                    voiceNarrator.speak(
                        text = finalReplyText,
                        style = style,
                        title = activeLandmark?.name ?: style.narratorTitle
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Dungeon Master streaming timed out", e)
                val errText = getStr(R.string.vm_dm_timeout, "Dungeon Master transmission timed out after 60 seconds. Please try again.")
                _aiErrorBanner.value = errText
                updateFailedDmMessage(dmMessageId, errText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Dungeon Master streaming failed", e)
                val is429 = e is QuotaExceededException ||
                    e.message?.contains("429") == true ||
                    e.message?.contains("quota", ignoreCase = true) == true
                if (is429) {
                    usageQuotaManager?.recordFallbackCascade(currentUserId)
                    showSmartQuotaRecovery(AiCapability.COMPANION_CHAT, isRateLimited429 = true)
                }
                val errText = e.localizedMessage ?: getStr(R.string.vm_dm_interrupted, "Dungeon Master connection interrupted.")
                _aiErrorBanner.value = errText
                updateFailedDmMessage(
                    dmMessageId,
                    getStr(R.string.vm_dm_tx_error_format, "Transmission error: $errText", errText)
                )
            } finally {
                _isDungeonMasterStreaming.value = false
            }
        }
    }

    private fun updateFailedDmMessage(dmMessageId: String, errorText: String) {
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.id == dmMessageId) {
                msg.copy(text = errorText, isStreaming = false)
            } else {
                msg
            }
        }
    }

    private fun appendDungeonMasterMessage(text: String, landmarkName: String?) {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            senderName = _selectedStyle.value.narratorTitle,
            text = text,
            landmarkName = landmarkName,
            groundingSources = listOfNotNull(landmarkName, _regionName.value)
        )
        _chatMessages.value = _chatMessages.value + msg
    }

    fun toggleEquipInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                persistToggleEquipItem(item.id, !item.equipped)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed toggling equip state", e)
            }
        }
    }

    // =========================================================================
    // AUTHENTICATED FIRESTORE / GUEST IN-MEMORY PERSISTENCE ADAPTERS
    // =========================================================================

    private suspend fun persistAwardProgress(
        xpDelta: Long,
        landmarksDelta: Long,
        questsCompletedDelta: Long
    ) {
        if (isAuthenticatedSession) {
            firestoreRepo.awardProgress(xpDelta, landmarksDelta, questsCompletedDelta)
            return
        }
        val current = _guestProfileState.value ?: AdventurerProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = initialDisplayName.ifBlank { getStr(R.string.guest_explorer_name, "Guest Explorer") },
            characterStyle = _selectedStyle.value.id
        )
        val newXp = (current.xp + xpDelta).coerceAtLeast(0L)
        val newLevel = (newXp / 250L) + 1L
        _guestProfileState.value = current.copy(
            xp = newXp,
            level = newLevel,
            landmarksVisited = current.landmarksVisited + landmarksDelta,
            questsCompleted = current.questsCompleted + questsCompletedDelta,
            updatedAt = Timestamp.now()
        )
    }

    private suspend fun persistUpdateCharacterStyle(style: CharacterStyle) {
        if (isAuthenticatedSession) {
            firestoreRepo.updateCharacterStyle(style)
            return
        }
        val current = _guestProfileState.value ?: return
        _guestProfileState.value = current.copy(characterStyle = style.id, updatedAt = Timestamp.now())
    }

    private suspend fun persistCreateQuest(
        title: String,
        characterStyle: CharacterStyle,
        regionName: String,
        summary: String,
        landmarksCount: Int,
        distanceMeters: Int,
        stopsSummary: List<String>
    ): String {
        if (isAuthenticatedSession) {
            return firestoreRepo.createQuest(title, characterStyle, regionName, summary, landmarksCount, distanceMeters, stopsSummary)
        }
        val id = "guest_quest_${UUID.randomUUID().toString().take(8)}"
        val quest = CompletedQuest(
            id = id,
            userId = currentUserId,
            title = title,
            characterStyle = characterStyle.id,
            regionName = regionName,
            summary = summary,
            landmarksCount = landmarksCount.toLong(),
            completedStops = 0L,
            xpEarned = 0L,
            distanceMeters = distanceMeters.toLong(),
            status = "ACTIVE",
            stopsSummary = stopsSummary,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _guestQuestsState.value = listOf(quest) + _guestQuestsState.value
        return id
    }

    private suspend fun persistUpdateQuestProgress(
        questId: String,
        completedStops: Int,
        xpEarned: Int,
        status: String,
        summary: String
    ) {
        if (isAuthenticatedSession) {
            firestoreRepo.updateQuestProgress(questId, completedStops, xpEarned, status, summary)
            return
        }
        _guestQuestsState.value = _guestQuestsState.value.map { q ->
            if (q.id == questId) {
                q.copy(
                    completedStops = completedStops.toLong(),
                    xpEarned = xpEarned.toLong(),
                    status = status,
                    summary = summary,
                    updatedAt = Timestamp.now()
                )
            } else q
        }
    }

    private suspend fun persistRecordCompletedQuest(
        title: String,
        characterStyle: CharacterStyle,
        regionName: String,
        summary: String,
        landmarksCount: Int,
        xpEarned: Int,
        distanceMeters: Int,
        stopsSummary: List<String>
    ) {
        if (isAuthenticatedSession) {
            firestoreRepo.recordCompletedQuest(title, characterStyle, regionName, summary, landmarksCount, xpEarned, distanceMeters, stopsSummary)
            return
        }
        val quest = CompletedQuest(
            id = "guest_quest_${UUID.randomUUID().toString().take(8)}",
            userId = currentUserId,
            title = title,
            characterStyle = characterStyle.id,
            regionName = regionName,
            summary = summary,
            landmarksCount = landmarksCount.toLong(),
            completedStops = landmarksCount.toLong(),
            xpEarned = xpEarned.toLong(),
            distanceMeters = distanceMeters.toLong(),
            status = "COMPLETED",
            stopsSummary = stopsSummary,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _guestQuestsState.value = listOf(quest) + _guestQuestsState.value
    }

    private suspend fun persistDeleteQuest(questId: String) {
        if (isAuthenticatedSession) {
            firestoreRepo.deleteQuest(questId)
            return
        }
        _guestQuestsState.value = _guestQuestsState.value.filterNot { it.id == questId }
    }

    private suspend fun persistUnlockBadgeIfAbsent(
        badgeId: String,
        badgeCode: String,
        title: String,
        description: String,
        category: String,
        rarity: String,
        landmarkName: String
    ) {
        if (isAuthenticatedSession) {
            firestoreRepo.unlockBadgeIfAbsent(badgeId, badgeCode, title, description, category, rarity, landmarkName)
            return
        }
        if (_guestBadgesState.value.any { it.id == badgeId || it.badgeCode == badgeCode }) return
        val badge = EarnedBadge(
            id = badgeId,
            userId = currentUserId,
            badgeCode = badgeCode,
            title = title,
            description = description,
            category = category,
            rarity = rarity,
            landmarkName = landmarkName,
            createdAt = Timestamp.now()
        )
        _guestBadgesState.value = listOf(badge) + _guestBadgesState.value
    }

    private suspend fun persistAddInventoryItem(
        name: String,
        loreDescription: String,
        itemType: String,
        rarity: String,
        originLandmark: String,
        characterStyle: CharacterStyle,
        powerBonus: Int,
        equipped: Boolean
    ) {
        if (isAuthenticatedSession) {
            firestoreRepo.addInventoryItem(name, loreDescription, itemType, rarity, originLandmark, characterStyle, powerBonus, equipped)
            return
        }
        val item = InventoryItem(
            id = "guest_inv_${UUID.randomUUID().toString().take(8)}",
            userId = currentUserId,
            name = name,
            loreDescription = loreDescription,
            itemType = itemType,
            rarity = rarity,
            originLandmark = originLandmark,
            characterStyle = characterStyle.id,
            powerBonus = powerBonus.toLong(),
            equipped = equipped,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _guestInventoryState.value = (listOf(item) + _guestInventoryState.value)
            .sortedWith(compareByDescending<InventoryItem> { it.equipped }.thenByDescending { it.powerBonus })
    }

    private suspend fun persistToggleEquipItem(itemId: String, equipped: Boolean) {
        if (isAuthenticatedSession) {
            firestoreRepo.toggleEquipItem(itemId, equipped)
            return
        }
        _guestInventoryState.value = _guestInventoryState.value
            .map { if (it.id == itemId) it.copy(equipped = equipped, updatedAt = Timestamp.now()) else it }
            .sortedWith(compareByDescending<InventoryItem> { it.equipped }.thenByDescending { it.powerBonus })
    }

    private suspend fun persistRecordFoundArtifact(
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
        isFavorite: Boolean,
        inspectionNotes: String
    ) {
        if (isAuthenticatedSession) {
            firestoreRepo.recordFoundArtifact(
                artifactCode, name, loreInscription, category, rarity, landmarkName,
                regionName, characterStyle, latitude, longitude, resonanceValue, isFavorite, inspectionNotes
            )
            return
        }
        val artifact = FoundArtifact(
            id = "guest_art_${UUID.randomUUID().toString().take(8)}",
            userId = currentUserId,
            artifactCode = artifactCode,
            name = name,
            loreInscription = loreInscription,
            category = category.id,
            rarity = rarity,
            landmarkName = landmarkName,
            regionName = regionName,
            characterStyle = characterStyle.id,
            latitude = latitude,
            longitude = longitude,
            resonanceValue = resonanceValue.toLong(),
            isFavorite = isFavorite,
            inspectionNotes = inspectionNotes,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        _guestArtifactsState.value = (listOf(artifact) + _guestArtifactsState.value)
            .sortedWith(compareByDescending<FoundArtifact> { it.isFavorite }.thenByDescending { it.resonanceValue })
    }

    private suspend fun persistToggleFavoriteArtifact(artifactId: String, nextFav: Boolean) {
        if (isAuthenticatedSession) {
            firestoreRepo.toggleFavoriteArtifact(artifactId, nextFav)
            return
        }
        _guestArtifactsState.value = _guestArtifactsState.value
            .map { if (it.id == artifactId) it.copy(isFavorite = nextFav, updatedAt = Timestamp.now()) else it }
            .sortedWith(compareByDescending<FoundArtifact> { it.isFavorite }.thenByDescending { it.resonanceValue })
    }

    private suspend fun persistUpdateArtifactNotes(artifactId: String, notes: String) {
        if (isAuthenticatedSession) {
            firestoreRepo.updateArtifactInspectionNotes(artifactId, notes)
            return
        }
        _guestArtifactsState.value = _guestArtifactsState.value.map {
            if (it.id == artifactId) it.copy(inspectionNotes = notes, updatedAt = Timestamp.now()) else it
        }
    }

    private suspend fun persistDeleteFoundArtifact(artifactId: String) {
        if (isAuthenticatedSession) {
            firestoreRepo.deleteFoundArtifact(artifactId)
            return
        }
        _guestArtifactsState.value = _guestArtifactsState.value.filterNot { it.id == artifactId }
    }

    override fun onCleared() {
        super.onCleared()
        pocketWalkJob?.cancel()
        voiceNarrator.shutdown()
    }
}

private fun OfficialHistoricTrailBritish() = OfficialHeritageTrail(
    id = "trail_london_royal",
    title = "Gaslight Antiquarian & Bloomsbury Chronicle",
    sponsorAuthority = "ROYAL HERITAGE & LITERARY TRUST",
    cityRegionName = "London — Westminster & Soho",
    durationMinutes = 50,
    xpMultiplierLabel = "1.5x Official Trail XP",
    badgeRewardTitle = "Fellow of the Royal Archive",
    description = "Uncover Rosetta Stone lore, Trafalgar admiralty monuments, and 18th-century scholar coffeehouses.",
    recommendedStyle = CharacterStyle.VICTORIAN_HISTORIAN
)
