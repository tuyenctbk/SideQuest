package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DungeonMasterAiRepository
import com.example.data.LandmarkDiscoveryRepository
import com.example.data.SideQuestRepository
import com.example.model.AdventurerProfile
import com.example.model.AiLandmarkRiddle
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
import com.example.model.TavernVoucher
import com.example.model.VisionScanResult
import com.example.model.WorldAtmosphereEvent
import com.example.util.VoiceNarrator
import com.google.firebase.ai.type.QuotaExceededException
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
    private val initialDisplayName: String
) : ViewModel() {

    val presetRegions: List<PresetCityRegion> = landmarkRepo.presetRegions

    private val _firestoreErrorMessage = MutableStateFlow<String?>(null)
    val firestoreErrorMessage: StateFlow<String?> = _firestoreErrorMessage.asStateFlow()

    val profileState: StateFlow<AdventurerProfile?> = firestoreRepo.observeProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing profile", root)
            _firestoreErrorMessage.value = root.localizedMessage ?: "Failed to load adventurer profile"
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val questsState: StateFlow<List<CompletedQuest>> = firestoreRepo.observeQuests(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing quests", root)
            _firestoreErrorMessage.value = root.localizedMessage ?: "Failed to load quest log"
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val badgesState: StateFlow<List<EarnedBadge>> = firestoreRepo.observeBadges(currentUserId)
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

    val inventoryState: StateFlow<List<InventoryItem>> = firestoreRepo.observeInventory(currentUserId)
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

    val artifactsState: StateFlow<List<FoundArtifact>> = firestoreRepo.observeArtifacts(currentUserId)
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

    // New AI Feature 4: Live Voice Field-Log Polisher & Transcriber (gemini-3.8-flash)
    private val _isPolishingVoiceNote = MutableStateFlow(false)
    val isPolishingVoiceNote: StateFlow<Boolean> = _isPolishingVoiceNote.asStateFlow()

    // Smart Zero-Cost / Free-Tier Eco Mode & User-Controlled Paid API Opt-In
    private val _isPaidImageApiApproved = MutableStateFlow(false)
    val isPaidImageApiApproved: StateFlow<Boolean> = _isPaidImageApiApproved.asStateFlow()

    val freeTierCallsUsed: StateFlow<Int> = aiRepo.freeTierCallsUsed
    val cacheHitsSaved: StateFlow<Int> = aiRepo.cacheHitsSaved
    val paidImageCallsUsed: StateFlow<Int> = aiRepo.paidImageCallsUsed

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
                text = "Welcome to SideQuest, explorer. I am ${_selectedStyle.value.narratorTitle}. Your tactical map has locked onto ${seeded.size} real-world landmarks in ${initialPreset.name}. Select any waypoint, scan a real plaque with Gemini Vision, or tap 'Quest' to weave a fresh adventure!",
                landmarkName = seeded.firstOrNull()?.name,
                groundingSources = listOf("OpenStreetMap & Google Maps Grounding", initialPreset.name)
            )
        )

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
    }

    fun dismissBanners() {
        _aiErrorBanner.value = null
        _rewardBanner.value = null
        _firestoreErrorMessage.value = null
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
        _rewardBanner.value = "Support banner silenced for 14 days. You can always find it in the Guild Hub or Codex."
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

    fun completeGooglePlaySupporterPurchase(tier: GooglePlayTipTier) {
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
                firestoreRepo.awardProgress(
                    xpDelta = tier.bonusXp.toLong(),
                    landmarksDelta = 0L,
                    questsCompletedDelta = 0L
                )
                firestoreRepo.unlockBadgeIfAbsent(
                    badgeId = "badge_guild_supporter",
                    badgeCode = "ACTIVE_GUILD_SUPPORTER",
                    title = "Active Supporter: ${tier.title}",
                    description = "Supported independent SideQuest development via Google Play (${tier.priceDisplay}). All prompts permanently disabled.",
                    category = "MASTERY",
                    rarity = "LEGENDARY",
                    landmarkName = _regionName.value
                )
                firestoreRepo.recordFoundArtifact(
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
                _rewardBanner.value = "Thank you, Active Supporter! +${tier.bonusXp} XP, Permanent Supporter Badge unlocked & all donation prompts disabled!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed saving supporter badge to Firestore", e)
                _rewardBanner.value = "Thank you, Active Supporter! Permanent Supporter status unlocked."
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
                firestoreRepo.awardProgress(xpDelta = bonusXp, landmarksDelta = 0L, questsCompletedDelta = 0L)
                if (justCrossedGoal) {
                    firestoreRepo.unlockBadgeIfAbsent(
                        badgeId = "badge_daily_strider",
                        badgeCode = "DAILY_MARATHON_STRIDER",
                        title = "Daily 1km Vanguard Strider",
                        description = "Completed the 1,000m daily SideQuest walking goal!",
                        category = "EXPLORATION",
                        rarity = "EPIC",
                        landmarkName = _regionName.value
                    )
                    _rewardBanner.value = "1,000m Daily Walk Goal Reached! +150 Bonus XP & 'Daily 1km Vanguard Strider' Badge!"
                } else {
                    _rewardBanner.value = "Logged +250m Walking Cadence ($updated / ${dailyWalkGoalMeters}m) • +50 XP!"
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
        _rewardBanner.value = "Sponsored Waypoint '$cleanName' pinned to map + Voucher $code unlocked!"
    }

    // Suggestion 3: Toggle Adventurer's Guild Pass & Offline Pack Cache
    fun activateGuildPass() {
        val next = !_isGuildPassActive.value
        _isGuildPassActive.value = next
        if (next) {
            _isOfflinePackCached.value = true
            _rewardBanner.value = "Adventurer's Guild Pass Active! 1.5x XP Multiplier, Streak Shield & Offline Lore Pack unlocked."
        } else {
            _rewardBanner.value = "Switched to Standard Explorer Tier."
        }
    }

    fun toggleOfflineExpeditionCache() {
        val next = !_isOfflinePackCached.value
        _isOfflinePackCached.value = next
        _rewardBanner.value = if (next) {
            "Offline Expedition Pack cached for ${_regionName.value} (${_landmarks.value.size} landmarks & voice scripts)."
        } else {
            "Offline cache cleared for ${_regionName.value}."
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
            _rewardBanner.value = "Hands-Free Pocket DM Mode ON • Tracking proximity to ${activeLm?.name ?: "waypoint"}"
            voiceNarrator.speak(startMsg, _selectedStyle.value, title = "Pocket DM • Hands-Free Mode")

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
                            voiceNarrator.speak(cue, _selectedStyle.value, title = "45m to ${currentLm.name}")
                        }
                        0 -> {
                            val arrival = "Waypoint reached: ${currentLm.name}. ${currentLm.loreSnippet} Challenge: ${currentLm.questChallenge}"
                            _rewardBanner.value = "Arrived at ${currentLm.name}! Auto-narrating waypoint briefing."
                            voiceNarrator.speak(arrival, _selectedStyle.value, title = "Arrived • ${currentLm.name}")
                        }
                    }
                }
            }
        } else {
            _rewardBanner.value = "Hands-Free Pocket DM Mode paused."
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

        _rewardBanner.value = "Official Trail Activated: ${trail.title} • ${trail.xpMultiplierLabel}"
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

        viewModelScope.launch {
            _isScanningVision.value = true
            _aiErrorBanner.value = null
            try {
                val scanResult = aiRepo.analyzeLandmarkPhotoWithGemini(
                    bitmap = bitmap,
                    style = style,
                    landmark = activeLandmark,
                    regionName = _regionName.value
                )
                scanResult.onSuccess { result ->
                    val multiplier = if (_isGuildPassActive.value) 1.5 else 1.0
                    val finalXp = (result.bonusXp * multiplier).toInt()
                    val finalResult = result.copy(bonusXp = finalXp)
                    _lastVisionScanResult.value = finalResult

                    firestoreRepo.addInventoryItem(
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
                    firestoreRepo.recordFoundArtifact(
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

                    firestoreRepo.awardProgress(
                        xpDelta = finalXp.toLong(),
                        landmarksDelta = 1L,
                        questsCompletedDelta = 0L
                    )

                    firestoreRepo.unlockBadgeIfAbsent(
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
                    _rewardBanner.value = "Vision Scan Verified! +$finalXp XP & Minted '${finalResult.awardedRelicName}'"
                    recordCoreOperationSuccess("Gemini Vision Scan (${finalResult.landmarkTitle})")

                    voiceNarrator.speak(
                        text = "${finalResult.landmarkTitle} verified. ${finalResult.inCharacterAnalysis}",
                        style = style,
                        title = "Vision Scan • ${finalResult.landmarkTitle}"
                    )
                }.onFailure { err ->
                    _aiErrorBanner.value = err.localizedMessage ?: "Failed to analyze landmark image."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multimodal vision scan error", e)
                _aiErrorBanner.value = e.localizedMessage ?: "Vision scanner encountered an error."
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
                firestoreRepo.addInventoryItem(
                    name = "Tavern Seal: ${target.partnerName.take(28)}",
                    loreDescription = "Redeemed Perk (${target.redemptionCode}): ${target.discountHeadline}",
                    itemType = "ELIXIR",
                    rarity = "RARE",
                    originLandmark = target.partnerName,
                    characterStyle = _selectedStyle.value,
                    powerBonus = 30,
                    equipped = false
                )
                firestoreRepo.awardProgress(xpDelta = 100L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                _rewardBanner.value = "Voucher Activated! Show code ${target.redemptionCode} at ${target.partnerName} (+100 XP)"
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
            _aiErrorBanner.value = "Collect at least 2 Relics from waypoints or scans to use the Relic Alchemy Forge!"
            return
        }
        val first = currentItems[0]
        val second = currentItems[1]
        val style = _selectedStyle.value
        val forgedName = "${style.title}'s Astral Singularity Core"
        val combinedPower = (first.powerBonus + second.powerBonus + 45L).coerceAtMost(500L).toInt()

        viewModelScope.launch {
            try {
                firestoreRepo.addInventoryItem(
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
                firestoreRepo.recordFoundArtifact(
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
                firestoreRepo.awardProgress(xpDelta = 225L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                firestoreRepo.unlockBadgeIfAbsent(
                    badgeId = "badge_relic_alchemist",
                    badgeCode = "RELIC_ALCHEMIST_MASTER",
                    title = "Grand Relic Alchemist",
                    description = "Forged $forgedName by combining relics in the Codex.",
                    category = "MASTERY",
                    rarity = "LEGENDARY",
                    landmarkName = _regionName.value
                )
                _rewardBanner.value = "Forged Legendary '$forgedName' (+$combinedPower Resonance & +225 XP)!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed forging legendary relic", e)
                _aiErrorBanner.value = e.localizedMessage ?: "Failed to forge relic."
            }
        }
    }

    // Found Artifacts & Curio Vault Management (Firestore CRUD)
    fun toggleFavoriteArtifact(artifact: FoundArtifact) {
        if (artifact.id.isBlank()) return
        val nextFav = !artifact.isFavorite
        viewModelScope.launch {
            try {
                firestoreRepo.toggleFavoriteArtifact(artifact.id, nextFav)
                _rewardBanner.value = if (nextFav) {
                    "Pinned '${artifact.name}' to Showcase Favorites ★"
                } else {
                    "Removed '${artifact.name}' from Showcase Favorites"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed toggling favorite artifact", e)
                _aiErrorBanner.value = e.localizedMessage ?: "Could not update artifact favorite status."
            }
        }
    }

    fun updateArtifactNotes(artifactId: String, notes: String) {
        if (artifactId.isBlank()) return
        viewModelScope.launch {
            try {
                firestoreRepo.updateArtifactInspectionNotes(artifactId, notes)
                _rewardBanner.value = "Explorer field notes saved to Cloud Firestore!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating artifact notes", e)
                _aiErrorBanner.value = e.localizedMessage ?: "Could not save field notes."
            }
        }
    }

    fun deleteFoundArtifact(artifactId: String) {
        if (artifactId.isBlank()) return
        viewModelScope.launch {
            try {
                firestoreRepo.deleteFoundArtifact(artifactId)
                _rewardBanner.value = "Artifact removed from Vault."
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
        notes: String
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
                firestoreRepo.recordFoundArtifact(
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
                firestoreRepo.awardProgress(xpDelta = 120L, landmarksDelta = 1L, questsCompletedDelta = 0L)
                firestoreRepo.unlockBadgeIfAbsent(
                    badgeId = "badge_curio_archivist",
                    badgeCode = "CURIO_FIELD_ARCHIVIST",
                    title = "Guild Curio Archivist",
                    description = "Cataloged '$cleanName' into the Firestore Artifact Vault.",
                    category = "HISTORIAN",
                    rarity = "RARE",
                    landmarkName = cleanLandmark
                )
                _rewardBanner.value = "Cataloged '$cleanName' into Firestore Vault (+120 XP & 'Guild Curio Archivist' Badge)!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed cataloging custom artifact", e)
                _aiErrorBanner.value = e.localizedMessage ?: "Failed to catalog artifact."
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
        _rewardBanner.value = "World Atmosphere Shifted: ${next.title} • ${next.bonusLabel}"
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
                firestoreRepo.createQuest(
                    title = "$cleanTitle (Creator Trail)",
                    characterStyle = style,
                    regionName = cleanRegion,
                    summary = "Community-Authored Quest featuring $cleanStop. Riddle: $cleanRiddle",
                    landmarksCount = updated.size,
                    distanceMeters = updated.sumOf { it.distanceMeters },
                    stopsSummary = updated.map { it.name }
                )
                firestoreRepo.recordFoundArtifact(
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
                firestoreRepo.awardProgress(xpDelta = 200L, landmarksDelta = 1L, questsCompletedDelta = 0L)
                firestoreRepo.unlockBadgeIfAbsent(
                    badgeId = "badge_community_trailblazer",
                    badgeCode = "COMMUNITY_QUEST_ARCHITECT",
                    title = "Community Quest Architect",
                    description = "Designed and published '$cleanTitle' in the Creator Quest Studio.",
                    category = "MASTERY",
                    rarity = "EPIC",
                    landmarkName = cleanStop
                )
                _rewardBanner.value = "Published Creator Trail '$cleanTitle'! +200 XP, Trailblazer Artifact & Architect Badge unlocked!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed publishing creator quest", e)
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
            _rewardBanner.value = "Paid Image API Unlocked by User Opt-In. Live Gemini Relic Art Studio enabled."
        } else {
            _rewardBanner.value = "Zero-Bill Free-Tier Eco Mode Active ($0 Cost Guard). Paid Image API calls locked."
        }
    }

    /**
     * Generates custom RPG Relic / Travelogue artwork at runtime using `gemini-nano-banana-2.1`.
     * Strictly follows Firebase AI Logic guidelines: 60s timeout, real loading state, no mock fallback,
     * and respects the user's Free-Tier Eco Mode opt-in preference.
     */
    fun generateAiRelicArtwork(customPrompt: String) {
        if (!_isPaidImageApiApproved.value) {
            val msg = "Zero-Bill Eco Mode is active ($0 cost guard). Toggle 'Allow Paid Image API' in the studio card if you decide to run live Gemini Image Generation."
            _relicArtUiState.value = RelicArtUiState.Error(msg)
            _aiErrorBanner.value = msg
            return
        }

        val style = _selectedStyle.value
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
        val cleanPrompt = customPrompt.trim().ifBlank {
            "Ornate ${style.title} RPG artifact discovered at ${activeLm?.name ?: _regionName.value}, dramatic studio lighting, intricate engravings, high fantasy concept art"
        }

        viewModelScope.launch {
            _relicArtUiState.value = RelicArtUiState.Loading(cleanPrompt)
            try {
                val bitmap = withTimeout(60_000L) {
                    aiRepo.generateRelicArtworkWithGemini(cleanPrompt)
                }
                if (bitmap != null) {
                    _relicArtUiState.value = RelicArtUiState.Success(bitmap, cleanPrompt)
                    firestoreRepo.awardProgress(xpDelta = 150L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                    firestoreRepo.unlockBadgeIfAbsent(
                        badgeId = "badge_arcane_illuminator",
                        badgeCode = "AI_RELIC_ILLUMINATOR",
                        title = "Arcane Relic Illuminator",
                        description = "Synthesized custom RPG relic artwork using Gemini Image Generation.",
                        category = "MASTERY",
                        rarity = "EPIC",
                        landmarkName = activeLm?.name ?: _regionName.value
                    )
                    _rewardBanner.value = "AI Relic Artwork Synthesized! +150 XP & 'Arcane Relic Illuminator' Badge!"
                } else {
                    _relicArtUiState.value = RelicArtUiState.Error("No image was returned by the model.")
                }
            } catch (e: TimeoutCancellationException) {
                _relicArtUiState.value = RelicArtUiState.Error("Image generation timed out after 60 seconds.")
            } catch (e: QuotaExceededException) {
                _relicArtUiState.value = RelicArtUiState.Error("Rate limit exceeded. Please try again later.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Relic image generation failed", e)
                _relicArtUiState.value = RelicArtUiState.Error(e.localizedMessage ?: "Image generation failed.")
            }
        }
    }

    /**
     * Edits/remixes the currently generated AI Relic artwork or a landmark plaque snapshot using `gemini-nano-banana-2.1`.
     */
    fun editAiRelicArtwork(editInstruction: String) {
        if (!_isPaidImageApiApproved.value) {
            val msg = "Zero-Bill Eco Mode is active ($0 cost guard). Toggle 'Allow Paid Image API' in the studio card if you decide to run live Gemini Image Remixing."
            _relicArtUiState.value = RelicArtUiState.Error(msg)
            _aiErrorBanner.value = msg
            return
        }

        val instruction = editInstruction.trim().ifBlank {
            "Add glowing astral runes and golden twilight mist around the artifact"
        }
        val currentState = _relicArtUiState.value
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
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
                val editedBitmap = withTimeout(60_000L) {
                    aiRepo.editRelicArtworkWithGemini(sourceBitmap, instruction)
                }
                if (editedBitmap != null) {
                    _relicArtUiState.value = RelicArtUiState.Success(editedBitmap, instruction)
                    firestoreRepo.awardProgress(xpDelta = 125L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                    _rewardBanner.value = "AI Relic Image Remixed! (+125 XP)"
                } else {
                    _relicArtUiState.value = RelicArtUiState.Error("No edited image was returned by the model.")
                }
            } catch (e: TimeoutCancellationException) {
                _relicArtUiState.value = RelicArtUiState.Error("Image editing timed out after 60 seconds.")
            } catch (e: QuotaExceededException) {
                _relicArtUiState.value = RelicArtUiState.Error("Rate limit exceeded. Please try again later.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Relic image editing failed", e)
                _relicArtUiState.value = RelicArtUiState.Error(e.localizedMessage ?: "Image editing failed.")
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

        viewModelScope.launch {
            _isGeneratingFactDossier.value = true
            _aiErrorBanner.value = null
            try {
                val result = aiRepo.generateLandmarkFactDossier(
                    style = style,
                    regionName = _regionName.value,
                    landmark = activeLm
                )
                result.onSuccess { dossier ->
                    _activeFactDossier.value = dossier
                    firestoreRepo.awardProgress(xpDelta = 110L, landmarksDelta = 0L, questsCompletedDelta = 0L)
                    val factsBullet = dossier.verifiedHistoricalFacts.joinToString("\n• ", prefix = "• ")
                    val chatReport = "🏛️ **[AI FACT-CHECK & DEEP LORE DOSSIER: ${dossier.landmarkName}]**\n" +
                        "**Era:** ${dossier.verifiedEra} • **Style:** ${dossier.architecturalStyle}\n\n" +
                        "$factsBullet\n\n" +
                        "🔍 *Walker Observation Tip:* ${dossier.localInsiderTip}\n" +
                        "⚔️ *RPG Hook:* ${dossier.rpgEncounterHook}"
                    appendDungeonMasterMessage(chatReport, landmarkName = dossier.landmarkName)
                    _rewardBanner.value = "Verified Fact-Check Dossier for ${dossier.landmarkName} (+110 XP)!"
                    if (voiceNarrator.autoNarrate.value) {
                        voiceNarrator.speak(
                            text = "${dossier.landmarkName}, ${dossier.verifiedEra}. ${dossier.verifiedHistoricalFacts.firstOrNull().orEmpty()} ${dossier.rpgEncounterHook}",
                            style = style,
                            title = "Fact Dossier • ${dossier.landmarkName}"
                        )
                    }
                }.onFailure { err ->
                    _aiErrorBanner.value = err.localizedMessage ?: "Could not synthesize Fact-Check Dossier."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fact dossier error", e)
                _aiErrorBanner.value = e.localizedMessage ?: "Fact-Check Dossier error."
            } finally {
                _isGeneratingFactDossier.value = false
            }
        }
    }

    fun dismissAiRiddle() {
        _activeAiRiddle.value = null
        _riddleAnswerStatus.value = null
    }

    /**
     * Generates an interactive 3-option AI Landmark Riddle & Cipher Duel (`gemini-3.8-flash`).
     */
    fun requestAiLandmarkRiddle() {
        if (_isGeneratingRiddle.value) return
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)
            ?: _landmarks.value.firstOrNull() ?: return
        val style = _selectedStyle.value

        viewModelScope.launch {
            _isGeneratingRiddle.value = true
            _riddleAnswerStatus.value = null
            _aiErrorBanner.value = null
            try {
                val result = aiRepo.generateLandmarkRiddleWithGemini(
                    style = style,
                    regionName = _regionName.value,
                    landmark = activeLm
                )
                result.onSuccess { riddle ->
                    _activeAiRiddle.value = riddle
                    if (voiceNarrator.autoNarrate.value) {
                        voiceNarrator.speak(
                            text = "Riddle Challenge at ${riddle.landmarkName}: ${riddle.question}",
                            style = style,
                            title = "Riddle Duel • ${riddle.landmarkName}"
                        )
                    }
                }.onFailure { err ->
                    _aiErrorBanner.value = err.localizedMessage ?: "Could not generate AI Riddle."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Riddle error", e)
                _aiErrorBanner.value = e.localizedMessage ?: "AI Riddle generation failed."
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
            _riddleAnswerStatus.value = "CORRECT! +${riddle.xpReward} XP — ${riddle.explanationLore}"
            viewModelScope.launch {
                try {
                    firestoreRepo.awardProgress(
                        xpDelta = riddle.xpReward.toLong(),
                        landmarksDelta = 1L,
                        questsCompletedDelta = 0L
                    )
                    firestoreRepo.unlockBadgeIfAbsent(
                        badgeId = "badge_ai_riddle_duelist",
                        badgeCode = "AI_CIPHER_GRANDMASTER",
                        title = "Cipher Duel Grandmaster",
                        description = "Solved the Dungeon Master's AI Landmark Riddle at ${riddle.landmarkName}.",
                        category = "LORE",
                        rarity = "EPIC",
                        landmarkName = riddle.landmarkName
                    )
                    _rewardBanner.value = "Riddle Solved! +${riddle.xpReward} XP & 'Cipher Duel Grandmaster' Badge!"
                    recordCoreOperationSuccess("Riddle Duel (${riddle.landmarkName})")
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
                    title = "Riddle Solved!"
                )
            }
        } else {
            _riddleAnswerStatus.value = "Not quite! Hint: ${riddle.hint}"
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

        viewModelScope.launch {
            _isPolishingVoiceNote.value = true
            _aiErrorBanner.value = null
            try {
                val result = aiRepo.polishSpokenFieldNoteWithGemini(
                    rawTranscript = cleanText,
                    style = style,
                    regionName = _regionName.value,
                    landmarkName = landmarkName
                )
                result.onSuccess { polished ->
                    firestoreRepo.recordFoundArtifact(
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
                    firestoreRepo.awardProgress(xpDelta = 140L, landmarksDelta = 1L, questsCompletedDelta = 0L)
                    firestoreRepo.unlockBadgeIfAbsent(
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
                    _rewardBanner.value = "Voice Field Log Minted as '${polished.curioTitle}' (+140 XP)!"
                    if (voiceNarrator.autoNarrate.value) {
                        voiceNarrator.speak(
                            text = "${polished.curioTitle}. ${polished.polishedChronicle}",
                            style = style,
                            title = polished.curioTitle
                        )
                    }
                }.onFailure { err ->
                    _aiErrorBanner.value = err.localizedMessage ?: "Failed to polish voice field note."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice field note error", e)
                _aiErrorBanner.value = e.localizedMessage ?: "Voice field note failed."
            } finally {
                _isPolishingVoiceNote.value = false
            }
        }
    }

    // Suggestion 6: Co-Op Party Role & Riddle Resolution
    fun selectPartyRole(role: PartyRole) {
        _selectedPartyRole.value = role
        _rewardBanner.value = "Co-Op Party Role set to ${role.title} (${role.perk})"
    }

    fun cyclePartyRoomCode() {
        val codes = listOf("NEXUS-7", "AETHEL-4", "FINCH-9", "VANCE-2")
        val nextIdx = (codes.indexOf(_partyCode.value) + 1) % codes.size
        _partyCode.value = codes[nextIdx]
        _isCoOpRiddleSolved.value = false
        _rewardBanner.value = "Synced with Co-Op Party Room ${_partyCode.value} (3/3 Explorers Ready)"
    }

    fun solveCoOpPartyRiddle() {
        if (_isCoOpRiddleSolved.value) return
        _isCoOpRiddleSolved.value = true
        val activeLm = _landmarks.value.getOrNull(_selectedLandmarkIndex.value)

        viewModelScope.launch {
            try {
                firestoreRepo.awardProgress(xpDelta = 250L, landmarksDelta = 1L, questsCompletedDelta = 0L)
                firestoreRepo.unlockBadgeIfAbsent(
                    badgeId = "badge_coop_vanguard",
                    badgeCode = "COOP_PARTY_VANGUARD",
                    title = "Guild Co-Op Vanguard",
                    description = "Combined Scout, Lorekeeper, and Quartermaster clues in Party ${_partyCode.value}.",
                    category = "MASTERY",
                    rarity = "LEGENDARY",
                    landmarkName = activeLm?.name ?: _regionName.value
                )
                _rewardBanner.value = "Co-Op Party Riddle Solved! +250 Shared Party XP & 'Guild Co-Op Vanguard' Badge Unlocked!"
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
        val userName = profile?.displayName?.ifBlank { "You (Explorer)" } ?: "You (Explorer)"

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
                explorerName = "$userName (You)",
                titleBadge = if (_isGuildPassActive.value) "Guild Pass Sovereign" else "Active Explorer",
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
                firestoreRepo.updateCharacterStyle(style)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update character style in Firestore", e)
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
                val label = String.format(Locale.US, "Live GPS Sector (%.4f, %.4f)", lat, lon)
                _regionName.value = label
                generateQuestForCoordinates(
                    lat = lat,
                    lon = lon,
                    regionLabel = label,
                    fallbackPreset = presetRegions.first()
                )
            } else {
                val fallback = presetRegions.first()
                _rewardBanner.value = "GPS signal indoor/unavailable — calibrated to ${fallback.name}."
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
                _aiErrorBanner.value = "Could not geocode '$query'. Try a city or landmark like 'Kyoto', 'Boston Common', or 'SoHo NYC'."
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

                val aiResult = aiRepo.generateWalkingQuestWithGemini(
                    style = style,
                    regionName = regionLabel,
                    landmarks = discovered
                )

                val finalLandmarks = aiResult.getOrNull()?.enrichedLandmarks ?: discovered
                val questTitle = aiResult.getOrNull()?.questTitle
                    ?: "${style.title}: $regionLabel Expedition"
                val questIntro = aiResult.getOrNull()?.questIntro
                    ?: "Your ${style.title} sensors have pinpointed ${finalLandmarks.size} real-world landmarks across $regionLabel."

                if (aiResult.isFailure) {
                    val err = aiResult.exceptionOrNull()
                    _aiErrorBanner.value = err?.localizedMessage
                        ?: "Gemini quest enrichment encountered an error."
                }

                _landmarks.value = finalLandmarks
                _activeQuestTitle.value = questTitle
                _activeQuestIntro.value = questIntro

                val totalDistance = finalLandmarks.sumOf { it.distanceMeters }
                val questId = firestoreRepo.createQuest(
                    title = questTitle,
                    characterStyle = style,
                    regionName = regionLabel,
                    summary = questIntro,
                    landmarksCount = finalLandmarks.size,
                    distanceMeters = totalDistance,
                    stopsSummary = finalLandmarks.map { it.name }
                )
                _activeFirestoreQuestId.value = questId

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
                _aiErrorBanner.value = e.localizedMessage ?: "Failed to generate walking quest."
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

                    firestoreRepo.addInventoryItem(
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

                    firestoreRepo.recordFoundArtifact(
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

                    firestoreRepo.awardProgress(
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
                    firestoreRepo.unlockBadgeIfAbsent(
                        badgeId = badgeId,
                        badgeCode = badgeId.uppercase(Locale.US),
                        title = badgeTitle,
                        description = "Investigated ${landmark.name} as a ${style.title}.",
                        category = badgeCat,
                        rarity = landmark.rewardRarity,
                        landmarkName = landmark.name
                    )

                    if (allCleared) {
                        firestoreRepo.unlockBadgeIfAbsent(
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
                        firestoreRepo.updateQuestProgress(
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
                        val createdId = firestoreRepo.createQuest(
                            title = _activeQuestTitle.value,
                            characterStyle = style,
                            regionName = _regionName.value,
                            summary = summaryText,
                            landmarksCount = updatedList.size,
                            distanceMeters = updatedList.sumOf { it.distanceMeters },
                            stopsSummary = updatedList.map { it.name }
                        )
                        if (allCleared) {
                            firestoreRepo.updateQuestProgress(
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
                        " • Bounty '$completedBountyTitle' & Partner Voucher Unlocked!"
                    } else {
                        ""
                    }
                    _rewardBanner.value = if (allCleared) {
                        "Quest Completed! +$totalDeltaXp XP & Looted '${landmark.rewardItemName}'$bountySuffix"
                    } else {
                        "Waypoint Cleared! +$totalDeltaXp XP & Looted '${landmark.rewardItemName}'$bountySuffix"
                    }
                    recordCoreOperationSuccess(
                        if (allCleared) "Walking Quest '${_activeQuestTitle.value}'" else "Waypoint '${landmark.name}'"
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

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            senderName = "You (${style.title})",
            text = trimmed,
            landmarkName = activeLandmark?.name
        )

        val dmMessageId = UUID.randomUUID().toString()
        val placeholderDm = ChatMessage(
            id = dmMessageId,
            isUser = false,
            senderName = style.narratorTitle,
            text = "Consulting the real-world archives...",
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
            try {
                aiRepo.streamDungeonMasterReply(
                    style = style,
                    regionName = _regionName.value,
                    activeLandmark = activeLandmark,
                    recentMessages = historySnapshot,
                    userMessage = trimmed
                ).collect { partialText ->
                    finalReplyText = partialText
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == dmMessageId) {
                            msg.copy(text = partialText, isStreaming = true)
                        } else {
                            msg
                        }
                    }
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
                val errText = "Dungeon Master transmission timed out after 60 seconds. Please try again."
                _aiErrorBanner.value = errText
                updateFailedDmMessage(dmMessageId, errText)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Dungeon Master streaming failed", e)
                val errText = e.localizedMessage ?: "Dungeon Master connection interrupted."
                _aiErrorBanner.value = errText
                updateFailedDmMessage(dmMessageId, "Transmission error: $errText")
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
                firestoreRepo.toggleEquipItem(item.id, !item.equipped)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed toggling equip state", e)
            }
        }
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
