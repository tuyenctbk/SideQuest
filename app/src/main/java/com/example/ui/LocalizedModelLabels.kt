package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.ArtifactCategory
import com.example.model.CharacterStyle
import com.example.model.ExplorerRank
import com.example.model.LandmarkCategory
import com.example.model.PartyRole
import com.example.model.WorldAtmosphereEvent
import com.example.util.AppEventType

@Composable
fun CharacterStyle.localizedTitle(): String = when (this) {
    CharacterStyle.CYBERPUNK_SCOUT -> stringResource(R.string.style_cyberpunk_title)
    CharacterStyle.VICTORIAN_HISTORIAN -> stringResource(R.string.style_victorian_title)
    CharacterStyle.ARCANE_CARTOGRAPHER -> stringResource(R.string.style_arcane_title)
    CharacterStyle.NOIR_DETECTIVE -> stringResource(R.string.style_noir_title)
}

fun CharacterStyle.taglineRes(): Int = when (this) {
    CharacterStyle.CYBERPUNK_SCOUT -> R.string.style_cyberpunk_tagline
    CharacterStyle.VICTORIAN_HISTORIAN -> R.string.style_victorian_tagline
    CharacterStyle.ARCANE_CARTOGRAPHER -> R.string.style_arcane_tagline
    CharacterStyle.NOIR_DETECTIVE -> R.string.style_noir_tagline
}

@Composable
fun CharacterStyle.localizedTagline(): String = stringResource(taglineRes())

@Composable
fun ExplorerRank.localizedTitle(): String = when (this) {
    ExplorerRank.INITIATE -> stringResource(R.string.rank_initiate_title)
    ExplorerRank.PATHFINDER -> stringResource(R.string.rank_pathfinder_title)
    ExplorerRank.LOREKEEPER -> stringResource(R.string.rank_lorekeeper_title)
    ExplorerRank.GRAND_CARTOGRAPHER -> stringResource(R.string.rank_cartographer_title)
    ExplorerRank.MYTHIC_VANGUARD -> stringResource(R.string.rank_vanguard_title)
}

@Composable
fun ExplorerRank.localizedBadgeLabel(): String = when (this) {
    ExplorerRank.INITIATE -> stringResource(R.string.rank_initiate_badge)
    ExplorerRank.PATHFINDER -> stringResource(R.string.rank_pathfinder_badge)
    ExplorerRank.LOREKEEPER -> stringResource(R.string.rank_lorekeeper_badge)
    ExplorerRank.GRAND_CARTOGRAPHER -> stringResource(R.string.rank_cartographer_badge)
    ExplorerRank.MYTHIC_VANGUARD -> stringResource(R.string.rank_vanguard_badge)
}

@Composable
fun ExplorerRank.localizedPerkSummary(): String = when (this) {
    ExplorerRank.INITIATE -> stringResource(R.string.rank_initiate_perk)
    ExplorerRank.PATHFINDER -> stringResource(R.string.rank_pathfinder_perk)
    ExplorerRank.LOREKEEPER -> stringResource(R.string.rank_lorekeeper_perk)
    ExplorerRank.GRAND_CARTOGRAPHER -> stringResource(R.string.rank_cartographer_perk)
    ExplorerRank.MYTHIC_VANGUARD -> stringResource(R.string.rank_vanguard_perk)
}

@Composable
fun ArtifactCategory.localizedTitle(): String = when (this) {
    ArtifactCategory.PLAQUE_RUBBING -> stringResource(R.string.cat_plaque_rubbing)
    ArtifactCategory.RELIC_SHARD -> stringResource(R.string.cat_relic_shard)
    ArtifactCategory.BOTANICAL_SPECIMEN -> stringResource(R.string.cat_botanical_specimen)
    ArtifactCategory.BREW_SEAL -> stringResource(R.string.cat_brew_seal)
    ArtifactCategory.VISION_SCAN -> stringResource(R.string.cat_vision_scan)
    ArtifactCategory.CREATOR_TOKEN -> stringResource(R.string.cat_creator_token)
}

@Composable
fun WorldAtmosphereEvent.localizedTitle(): String = when (this) {
    WorldAtmosphereEvent.GOLDEN_HOUR_SURGE -> stringResource(R.string.atmos_golden_hour_title)
    WorldAtmosphereEvent.NOIR_RAINSTORM -> stringResource(R.string.atmos_noir_rain_title)
    WorldAtmosphereEvent.SOLAR_CANOPY_BLOOM -> stringResource(R.string.atmos_solar_canopy_title)
    WorldAtmosphereEvent.ROASTER_ALCHEMY_HOUR -> stringResource(R.string.atmos_roaster_hour_title)
}

@Composable
fun WorldAtmosphereEvent.localizedBonusLabel(): String = when (this) {
    WorldAtmosphereEvent.GOLDEN_HOUR_SURGE -> stringResource(R.string.atmos_golden_hour_bonus)
    WorldAtmosphereEvent.NOIR_RAINSTORM -> stringResource(R.string.atmos_noir_rain_bonus)
    WorldAtmosphereEvent.SOLAR_CANOPY_BLOOM -> stringResource(R.string.atmos_solar_canopy_bonus)
    WorldAtmosphereEvent.ROASTER_ALCHEMY_HOUR -> stringResource(R.string.atmos_roaster_hour_bonus)
}

@Composable
fun PartyRole.localizedTitle(): String = when (this) {
    PartyRole.SCOUT -> stringResource(R.string.role_scout_title)
    PartyRole.LOREKEEPER -> stringResource(R.string.role_lorekeeper_title)
    PartyRole.QUARTERMASTER -> stringResource(R.string.role_quartermaster_title)
}

@Composable
fun PartyRole.localizedPerk(): String = when (this) {
    PartyRole.SCOUT -> stringResource(R.string.role_scout_perk)
    PartyRole.LOREKEEPER -> stringResource(R.string.role_lorekeeper_perk)
    PartyRole.QUARTERMASTER -> stringResource(R.string.role_quartermaster_perk)
}

@Composable
fun PartyRole.localizedClueSpecialty(): String = when (this) {
    PartyRole.SCOUT -> stringResource(R.string.role_scout_clue)
    PartyRole.LOREKEEPER -> stringResource(R.string.role_lorekeeper_clue)
    PartyRole.QUARTERMASTER -> stringResource(R.string.role_quartermaster_clue)
}

@Composable
fun LandmarkCategory.localizedLabel(): String = when (this) {
    LandmarkCategory.PARK -> stringResource(R.string.landmark_cat_park)
    LandmarkCategory.PLAQUE -> stringResource(R.string.landmark_cat_plaque)
    LandmarkCategory.COFFEE_SHOP -> stringResource(R.string.landmark_cat_coffee)
    LandmarkCategory.MONUMENT -> stringResource(R.string.landmark_cat_monument)
    LandmarkCategory.MUSEUM -> stringResource(R.string.landmark_cat_museum)
}

@Composable
fun AppEventType.localizedLabel(): String = when (this) {
    AppEventType.APP_LAUNCH -> stringResource(R.string.event_app_launch)
    AppEventType.ONBOARDING_COMPLETED -> stringResource(R.string.event_onboarding_completed)
    AppEventType.QUEST_GENERATED -> stringResource(R.string.event_quest_generated)
    AppEventType.WAYPOINT_CLEARED -> stringResource(R.string.event_waypoint_cleared)
    AppEventType.QUEST_COMPLETED -> stringResource(R.string.event_quest_completed)
    AppEventType.VISION_SCAN_VERIFIED -> stringResource(R.string.event_vision_scan)
    AppEventType.RIDDLE_SOLVED -> stringResource(R.string.event_riddle_solved)
    AppEventType.ARTIFACT_CATALOGED -> stringResource(R.string.event_artifact_cataloged)
    AppEventType.DONATION_CLICKED -> stringResource(R.string.event_donation_clicked)
    AppEventType.PROMPT_SHOWN -> stringResource(R.string.event_prompt_shown)
    AppEventType.PROMPT_ACCEPTED -> stringResource(R.string.event_prompt_accepted)
    AppEventType.PROMPT_DISMISSED -> stringResource(R.string.event_prompt_dismissed)
}

@Composable
fun localizedRarityLabel(rarity: String): String = when (rarity.uppercase(java.util.Locale.US)) {
    "LEGENDARY" -> stringResource(R.string.vault_rarity_legendary)
    "EPIC" -> stringResource(R.string.vault_rarity_epic)
    "RARE" -> stringResource(R.string.vault_rarity_rare)
    "COMMON" -> stringResource(R.string.vault_rarity_common)
    else -> rarity
}
