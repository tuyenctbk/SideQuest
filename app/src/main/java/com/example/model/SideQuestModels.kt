package com.example.model

import android.graphics.Bitmap
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class CharacterStyle(
    val id: String,
    val title: String,
    val narratorTitle: String,
    val tagline: String,
    val voicePitch: Float,
    val voiceRate: Float,
    val systemPersonaPrompt: String,
    val defaultRelicName: String,
    val defaultRelicLore: String,
    val defaultRelicType: String
) {
    CYBERPUNK_SCOUT(
        id = "CYBERPUNK_SCOUT",
        title = "Cyberpunk Scout",
        narratorTitle = "AI Handler NEXUS-9",
        tagline = "Neon-drenched recon through encrypted monuments & synth-cafes",
        voicePitch = 0.92f,
        voiceRate = 1.06f,
        systemPersonaPrompt = "You are NEXUS-9, a tactical cyberpunk AI handler guiding a street-level Netrunner Scout in the year 2099. Frame real parks as biodome sanctuaries, historical plaques as ancient pre-collapse data monoliths, and coffee shops as underground stim-breweries and safehouses. Always weave real historical/geographical facts about the landmark into high-tech cyberpunk lore.",
        defaultRelicName = "Optic Neural Visor Mk.IV",
        defaultRelicLore = "Calibrated to detect hidden electromagnetic ley-lines around historic city markers.",
        defaultRelicType = "CYBER_DECK"
    ),
    VICTORIAN_HISTORIAN(
        id = "VICTORIAN_HISTORIAN",
        title = "Victorian Historian",
        narratorTitle = "Archivist Alistair Finch",
        tagline = "Gaslight antiquarian uncovering forgotten plaques & botanical marvels",
        voicePitch = 0.86f,
        voiceRate = 0.95f,
        systemPersonaPrompt = "You are Professor Alistair Finch, Chief Archivist of the Royal Antiquarian Society (1894). Speak with warm, evocative Victorian eloquence. Frame real parks as grand botanical estates, plaques as sacred historical chronicles, and coffee shops as lively penny-university coffeehouses where scholars debate. Always ground your narration in authentic real-world history and architecture.",
        defaultRelicName = "Brass Chronometer Compass",
        defaultRelicLore = "Engraved with the coordinates of the Royal Geographical Society; hums near heritage plaques.",
        defaultRelicType = "CHRONICLE"
    ),
    ARCANE_CARTOGRAPHER(
        id = "ARCANE_CARTOGRAPHER",
        title = "Arcane Cartographer",
        narratorTitle = "Oracle Lyra of the Star-Chart",
        tagline = "Mythic ley-line seeker mapping sacred groves, runes & alchemy taverns",
        voicePitch = 1.05f,
        voiceRate = 0.98f,
        systemPersonaPrompt = "You are Oracle Lyra, Grand Cartographer of the Astral Guild. Frame real-world parks as ancient sylvan sanctuaries, historical plaques as runestones of mortal memory, and coffee shops as alchemical apothecaries brewing restorative elixirs. Blend real facts about each landmark with mythic high-fantasy wonder.",
        defaultRelicName = "Luminous Astrolabe of Aethel",
        defaultRelicLore = "Projects shimmering constellations above real-world parks and monuments.",
        defaultRelicType = "RELIC"
    ),
    NOIR_DETECTIVE(
        id = "NOIR_DETECTIVE",
        title = "Noir Detective",
        narratorTitle = "Inspector Jack Vance",
        tagline = "Rain-slicked gumshoe piecing together city secrets over black coffee",
        voicePitch = 0.80f,
        voiceRate = 0.93f,
        systemPersonaPrompt = "You are Inspector Jack Vance, a hardboiled 1940s noir detective narrating a gripping urban mystery. Frame real parks as foggy rendezvous points, historical plaques as cold-case clues hidden in plain sight, and coffee shops as late-night diners where informants talk over black espresso. Keep every clue grounded in the real history and character of the place.",
        defaultRelicName = "Weathered Casebook & Magnifier",
        defaultRelicLore = "Margin notes connect every monument and street corner in town.",
        defaultRelicType = "KEYSTONE"
    );

    companion object {
        fun fromId(id: String): CharacterStyle {
            return entries.firstOrNull { it.id == id } ?: CYBERPUNK_SCOUT
        }
    }
}

enum class ExplorerRank(
    val title: String,
    val badgeLabel: String,
    val perkSummary: String
) {
    INITIATE("Guild Initiate", "Tier I", "Base 1.0x XP • Standard Audio Lore"),
    PATHFINDER("Urban Pathfinder", "Tier II", "+10% Bonus XP • Deep Historical Footnotes"),
    LOREKEEPER("Master Lorekeeper", "Tier III", "+20% Bonus XP • Rare Relic Drop Boost"),
    GRAND_CARTOGRAPHER("Grand Cartographer", "Tier IV", "+35% Bonus XP • Priority Local Cafe Perks"),
    MYTHIC_VANGUARD("Mythic Vanguard", "Tier V", "+50% Bonus XP • Legendary Guild Aura");

    companion object {
        fun fromLevel(level: Long): ExplorerRank = when {
            level >= 10L -> MYTHIC_VANGUARD
            level >= 6L -> GRAND_CARTOGRAPHER
            level >= 4L -> LOREKEEPER
            level >= 2L -> PATHFINDER
            else -> INITIATE
        }
    }
}

enum class ArtifactCategory(
    val id: String,
    val displayTitle: String,
    val badgeTag: String
) {
    PLAQUE_RUBBING("PLAQUE_RUBBING", "Plaque Rubbing", "INSCRIPTION"),
    RELIC_SHARD("RELIC_SHARD", "Relic Shard", "MONUMENT"),
    BOTANICAL_SPECIMEN("BOTANICAL_SPECIMEN", "Botanical Specimen", "SANCTUARY"),
    BREW_SEAL("BREW_SEAL", "Roaster Brew Seal", "TAVERN"),
    VISION_SCAN("VISION_SCAN", "Optical Vision Curio", "GEMINI LENS"),
    CREATOR_TOKEN("CREATOR_TOKEN", "Trailblazer Crest", "AUTHORED");

    val label: String get() = displayTitle

    companion object {
        fun fromId(id: String): ArtifactCategory {
            return entries.firstOrNull { it.id == id } ?: RELIC_SHARD
        }

        fun forLandmarkCategory(category: LandmarkCategory): ArtifactCategory = when (category) {
            LandmarkCategory.PARK -> BOTANICAL_SPECIMEN
            LandmarkCategory.PLAQUE -> PLAQUE_RUBBING
            LandmarkCategory.COFFEE_SHOP -> BREW_SEAL
            LandmarkCategory.MONUMENT, LandmarkCategory.MUSEUM -> RELIC_SHARD
        }

        fun fromLandmarkCategory(category: LandmarkCategory): ArtifactCategory =
            forLandmarkCategory(category)
    }
}

enum class WorldAtmosphereEvent(
    val id: String,
    val title: String,
    val buffSummary: String,
    val favouredCategory: LandmarkCategory,
    val promptAtmosphereNote: String
) {
    GOLDEN_HOUR_SURGE(
        id = "GOLDEN_HOUR_SURGE",
        title = "Golden Hour Ley-Surge",
        buffSummary = "+25% Bonus XP & Resonance on Monuments",
        favouredCategory = LandmarkCategory.MONUMENT,
        promptAtmosphereNote = "The city is bathed in dramatic amber twilight; long shadows stretch across monuments and plazas."
    ),
    NOIR_RAINSTORM(
        id = "NOIR_RAINSTORM",
        title = "Nocturne Fog & Rain",
        buffSummary = "+25% Bonus XP & Resonance on Plaques",
        favouredCategory = LandmarkCategory.PLAQUE,
        promptAtmosphereNote = "Neon and gaslight reflect off rain-slicked cobblestones; secret plaque inscriptions glow in the mist."
    ),
    SOLAR_CANOPY_BLOOM(
        id = "SOLAR_CANOPY_BLOOM",
        title = "Sunlit Canopy Bloom",
        buffSummary = "+25% Bonus XP & Resonance in Parks",
        favouredCategory = LandmarkCategory.PARK,
        promptAtmosphereNote = "Warm sunlight filters through lush park foliage; birdsong and gentle breezes carry ancient echoes."
    ),
    ROASTER_ALCHEMY_HOUR(
        id = "ROASTER_ALCHEMY_HOUR",
        title = "Morning Roaster Alchemy",
        buffSummary = "+25% Bonus XP & Resonance at Coffee Houses",
        favouredCategory = LandmarkCategory.COFFEE_SHOP,
        promptAtmosphereNote = "The aroma of freshly roasted espresso and warm pastries fills the street corners."
    );

    val bonusLabel: String get() = buffSummary
    val favoredCategory: LandmarkCategory get() = favouredCategory
    val xpMultiplier: Double get() = 1.25
}

enum class PartyRole(
    val id: String,
    val title: String,
    val perk: String,
    val clueSpecialty: String
) {
    SCOUT(
        id = "SCOUT",
        title = "Recon Scout",
        perk = "+20% Radar Range & Proximity Alerts",
        clueSpecialty = "Detects architectural symbols & compass bearings"
    ),
    LOREKEEPER(
        id = "LOREKEEPER",
        title = "Master Lorekeeper",
        perk = "+25% Bonus XP on Plaques & Monuments",
        clueSpecialty = "Deciphers historical dates & brass inscriptions"
    ),
    QUARTERMASTER(
        id = "QUARTERMASTER",
        title = "Alchemical Quartermaster",
        perk = "2x Tavern Voucher Perks & Relic Resonance",
        clueSpecialty = "Identifies local roasters, elixirs & hidden supply caches"
    )
}

data class PartyMemberStatus(
    val name: String,
    val role: PartyRole,
    val style: CharacterStyle,
    val distanceMetersAway: Int,
    val isReady: Boolean,
    val discoveredClue: String
)

data class OfficialHeritageTrail(
    val id: String,
    val title: String,
    val sponsorAuthority: String,
    val cityRegionName: String,
    val durationMinutes: Int,
    val xpMultiplierLabel: String,
    val badgeRewardTitle: String,
    val description: String,
    val recommendedStyle: CharacterStyle
)

data class TavernVoucher(
    val id: String,
    val partnerName: String,
    val discountHeadline: String,
    val termsDescription: String,
    val redemptionCode: String,
    val landmarkCategory: LandmarkCategory,
    val isUnlocked: Boolean = false,
    val isRedeemed: Boolean = false
)

data class NeighborhoodStanding(
    val rankPosition: Int,
    val explorerName: String,
    val titleBadge: String,
    val xpScore: Long,
    val landmarksCleared: Long,
    val isCurrentUser: Boolean = false
)

data class VisionScanResult(
    val landmarkTitle: String,
    val inCharacterAnalysis: String,
    val historicalVerification: String,
    val awardedRelicName: String,
    val bonusXp: Int
)

enum class LandmarkCategory(val id: String, val label: String) {
    PARK("PARK", "Sanctuary Park"),
    PLAQUE("PLAQUE", "Historical Plaque"),
    COFFEE_SHOP("COFFEE_SHOP", "Elixir / Coffee House"),
    MONUMENT("MONUMENT", "Landmark Monument"),
    MUSEUM("MUSEUM", "Lore Archive");

    fun rpgLabelFor(style: CharacterStyle): String = when (style) {
        CharacterStyle.CYBERPUNK_SCOUT -> when (this) {
            PARK -> "Bio-Dome Sector"
            PLAQUE -> "Encrypted Data Monolith"
            COFFEE_SHOP -> "Synth-Caffeine Safehouse"
            MONUMENT -> "Signal Relay Spire"
            MUSEUM -> "Memory Vault"
        }
        CharacterStyle.VICTORIAN_HISTORIAN -> when (this) {
            PARK -> "Botanical Promenade"
            PLAQUE -> "Heritage Brass Plaque"
            COFFEE_SHOP -> "Scholar's Coffeehouse"
            MONUMENT -> "Imperial Memorial"
            MUSEUM -> "Grand Athenaeum"
        }
        CharacterStyle.ARCANE_CARTOGRAPHER -> when (this) {
            PARK -> "Sylvan Ley-Grove"
            PLAQUE -> "Runestone of Memory"
            COFFEE_SHOP -> "Alchemist's Tavern"
            MONUMENT -> "Obelisk of Convergence"
            MUSEUM -> "Sanctum of Scrolls"
        }
        CharacterStyle.NOIR_DETECTIVE -> when (this) {
            PARK -> "Foggy Rendezvous Square"
            PLAQUE -> "Cold-Case Marker"
            COFFEE_SHOP -> "Midnight Espresso Diner"
            MONUMENT -> "Old Precinct Monument"
            MUSEUM -> "City Hall Archives"
        }
    }
}

data class GuildBounty(
    val id: String,
    val title: String,
    val sponsorTag: String,
    val description: String,
    val rewardPerk: String,
    val targetCategory: LandmarkCategory,
    val bonusXp: Int,
    val isCompleted: Boolean = false
)

data class RealLandmark(
    val id: String,
    val name: String,
    val category: LandmarkCategory,
    val latitude: Double,
    val longitude: Double,
    val realFactOrAddress: String,
    val distanceMeters: Int,
    val rpgNodeTitle: String,
    val loreSnippet: String,
    val questChallenge: String,
    val actionChoices: List<String>,
    val rewardItemName: String,
    val rewardItemType: String,
    val rewardRarity: String,
    val xpReward: Int,
    val isCleared: Boolean = false
)

data class PresetCityRegion(
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val fallbackLandmarks: List<SeedLandmark>
)

data class SeedLandmark(
    val name: String,
    val category: LandmarkCategory,
    val latitude: Double,
    val longitude: Double,
    val realFact: String
)

@IgnoreExtraProperties
data class AdventurerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Wanderer",
    val characterStyle: String = CharacterStyle.CYBERPUNK_SCOUT.id,
    val xp: Long = 0L,
    val level: Long = 1L,
    val questsCompleted: Long = 0L,
    val landmarksVisited: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CompletedQuest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val characterStyle: String = CharacterStyle.CYBERPUNK_SCOUT.id,
    val regionName: String = "",
    val summary: String = "",
    val landmarksCount: Long = 1L,
    val completedStops: Long = 0L,
    val xpEarned: Long = 0L,
    val distanceMeters: Long = 0L,
    val status: String = "ACTIVE",
    val stopsSummary: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EarnedBadge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val badgeCode: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "EXPLORATION",
    val rarity: String = "COMMON",
    val landmarkName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InventoryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val loreDescription: String = "",
    val itemType: String = "RELIC",
    val rarity: String = "COMMON",
    val originLandmark: String = "",
    val characterStyle: String = CharacterStyle.CYBERPUNK_SCOUT.id,
    val powerBonus: Long = 10L,
    val equipped: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FoundArtifact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val artifactCode: String = "",
    val name: String = "",
    val loreInscription: String = "",
    val category: String = ArtifactCategory.RELIC_SHARD.id,
    val rarity: String = "RARE",
    val landmarkName: String = "",
    val regionName: String = "",
    val characterStyle: String = CharacterStyle.CYBERPUNK_SCOUT.id,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val resonanceValue: Long = 50L,
    @get:PropertyName("isFavorite")
    @field:PropertyName("isFavorite")
    val isFavorite: Boolean = false,
    val inspectionNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val originLandmark: String get() = landmarkName
    val discoveryMethod: String get() = artifactCode
}

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val senderName: String,
    val text: String,
    val landmarkName: String? = null,
    val isStreaming: Boolean = false,
    val groundingSources: List<String> = emptyList(),
    val isVisionScan: Boolean = false
)

sealed interface RelicArtUiState {
    data object Idle : RelicArtUiState
    data class Loading(val prompt: String) : RelicArtUiState
    data class Success(val bitmap: Bitmap, val prompt: String) : RelicArtUiState
    data class Error(val message: String) : RelicArtUiState
}

data class LandmarkFactDossier(
    val landmarkName: String,
    val verifiedEra: String,
    val architecturalStyle: String,
    val verifiedHistoricalFacts: List<String>,
    val localInsiderTip: String,
    val rpgEncounterHook: String
)

data class AiLandmarkRiddle(
    val landmarkName: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val hint: String,
    val explanationLore: String,
    val xpReward: Int = 175
)

data class PolishedFieldLogResult(
    val curioTitle: String,
    val polishedChronicle: String,
    val historicalConnection: String
)

