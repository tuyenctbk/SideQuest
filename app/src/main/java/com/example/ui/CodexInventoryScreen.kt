package com.example.ui

import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import com.example.model.CompletedQuest
import com.example.model.EarnedBadge
import com.example.model.ExplorerRank
import com.example.model.InventoryItem
import com.example.model.NeighborhoodStanding
import com.example.model.PartyMemberStatus
import com.example.model.PartyRole
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.NoirCrimson
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateCardSurface

@Composable
fun CodexInventoryScreen(
    viewModel: SideQuestViewModel,
    onOpenDonationDialog: () -> Unit = {},
    onOpenQuestJournal: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by viewModel.profileState.collectAsState()
    val inventory by viewModel.inventoryState.collectAsState()
    val artifacts by viewModel.artifactsState.collectAsState()
    val badges by viewModel.badgesState.collectAsState()
    val quests by viewModel.questsState.collectAsState()
    val isGuildPassActive by viewModel.isGuildPassActive.collectAsState()
    val partyCode by viewModel.partyCode.collectAsState()
    val selectedPartyRole by viewModel.selectedPartyRole.collectAsState()
    val partyMembers by viewModel.partyMembers.collectAsState()
    val isCoOpRiddleSolved by viewModel.isCoOpRiddleSolved.collectAsState()
    val regionName by viewModel.regionName.collectAsState()
    val donationEngagementState by viewModel.donationEngagementState.collectAsState()

    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    val xp = profile?.xp ?: 0L
    val level = profile?.level ?: 1L
    val rank = ExplorerRank.fromLevel(level)
    val xpWithinLevel = (xp % 250L).toFloat() / 250f
    val equippedPower = inventory.filter { it.equipped }.sumOf { it.powerBonus } +
        artifacts.sumOf { it.resonanceValue.toInt() }
    val leaderboard = remember(profile, isGuildPassActive) {
        viewModel.buildNeighborhoodLeaderboard(profile)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("codex_inventory_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Adventurer Stats, Guild Rank & Adventurer's Guild Pass Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
                border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${rank.badgeLabel.uppercase()} • ${rank.title.uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GuildGold
                            )
                            Text(
                                text = profile?.displayName ?: "Adventurer",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = rank.perkSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberCyan
                            )
                        }
                        Surface(
                            color = GuildGold,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "LVL $level",
                                style = MaterialTheme.typography.labelLarge,
                                color = MidnightAbyss,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total XP: $xp (${250L - (xp % 250L)} XP to next Level)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Relic Power: +$equippedPower",
                                style = MaterialTheme.typography.labelMedium,
                                color = GuildGold,
                                maxLines = 1
                            )
                        }
                        LinearProgressIndicator(
                            progress = { xpWithinLevel },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = GuildGold,
                            trackColor = MidnightAbyss
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatPill(
                            modifier = Modifier.weight(1f),
                            value = "${artifacts.size}",
                            label = "Found Artifacts"
                        )
                        StatPill(
                            modifier = Modifier.weight(1f),
                            value = "${badges.size}",
                            label = "Badges Earned"
                        )
                        StatPill(
                            modifier = Modifier.weight(1f),
                            value = "${inventory.size}",
                            label = "Satchel Relics"
                        )
                    }

                    // Suggestion #3: Adventurer's Guild Pass Membership Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isGuildPassActive) ArcaneEmerald.copy(alpha = 0.16f) else MidnightAbyss,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isGuildPassActive) ArcaneEmerald else GuildGold.copy(alpha = 0.55f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = if (isGuildPassActive) ArcaneEmerald else GuildGold,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isGuildPassActive) {
                                            "ADVENTURER'S GUILD PASS • ACTIVE"
                                        } else {
                                            "ADVENTURER'S GUILD PASS"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isGuildPassActive) ArcaneEmerald else GuildGold
                                    )
                                    Text(
                                        text = "1.5x XP Boost • Offline Lore Packs • Streak Shield • Relic Forge",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.activateGuildPass() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isGuildPassActive) ArcaneEmerald else GuildGold,
                                    contentColor = MidnightAbyss
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("toggle_guild_pass_button")
                            ) {
                                Text(
                                    text = if (isGuildPassActive) "Active" else "Activate",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Section Selector Chips (Artifacts & Badges Vault / Satchel & Forge / Quests & Creator Studio / Party & Leaderboard)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    label = { Text(stringResource(id = R.string.codex_tab_artifacts_badges_format, artifacts.size + badges.size)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.24f),
                        selectedLabelColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("subtab_badges")
                )
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    label = { Text(stringResource(id = R.string.codex_tab_satchel_forge_format, inventory.size)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GuildGold.copy(alpha = 0.22f),
                        selectedLabelColor = GuildGold
                    ),
                    modifier = Modifier.testTag("subtab_inventory")
                )
                FilterChip(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    label = { Text(stringResource(id = R.string.codex_tab_quests_studio_format, quests.size)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GuildGold.copy(alpha = 0.22f),
                        selectedLabelColor = GuildGold
                    ),
                    modifier = Modifier.testTag("subtab_quests")
                )
                FilterChip(
                    selected = selectedSection == 3,
                    onClick = { selectedSection = 3 },
                    label = { Text(stringResource(id = R.string.codex_tab_party_rank)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                        selectedLabelColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("subtab_party_leaderboard")
                )
            }
        }

        // 3. Content List based on selected section
        when (selectedSection) {
            0 -> {
                item {
                    val relicArtState by viewModel.relicArtUiState.collectAsState()
                    val isPaidImageApiApproved by viewModel.isPaidImageApiApproved.collectAsState()
                    val cacheHitsSaved by viewModel.cacheHitsSaved.collectAsState()
                    ArtifactBadgeVaultComponent(
                        artifacts = artifacts,
                        badges = badges,
                        currentRegionName = regionName,
                        onToggleFavoriteArtifact = { viewModel.toggleFavoriteArtifact(it) },
                        onUpdateArtifactNotes = { id, notes -> viewModel.updateArtifactNotes(id, notes) },
                        onDeleteArtifact = { id -> viewModel.deleteFoundArtifact(id) },
                        onCatalogCustomArtifact = { name, category, landmarkName, inscription, notes ->
                            viewModel.catalogCustomFieldArtifact(
                                name = name,
                                category = category,
                                landmarkName = landmarkName,
                                inscription = inscription,
                                notes = notes
                            )
                        },
                        onNarrateLore = { title, lore ->
                            viewModel.narrateArtifactOrBadgeLore(title, lore)
                        },
                        relicArtUiState = relicArtState,
                        isPaidImageApiApproved = isPaidImageApiApproved,
                        cacheHitsSaved = cacheHitsSaved,
                        onTogglePaidImageApiApproval = { approved ->
                            viewModel.togglePaidImageApiApproval(approved)
                        },
                        onGenerateRelicArtwork = { prompt -> viewModel.generateAiRelicArtwork(prompt) },
                        onEditRelicArtwork = { instruction -> viewModel.editAiRelicArtwork(instruction) },
                        onClearRelicArt = { viewModel.clearRelicArtState() }
                    )
                }
            }
            1 -> {
                // Suggestion #3: Relic Alchemy Forge Card
                item {
                    RelicAlchemyForgeCard(
                        relicCount = inventory.size,
                        onForgeRelic = { viewModel.forgeLegendaryRelic() }
                    )
                }

                if (inventory.isEmpty()) {
                    item {
                        EmptyCodexStateCard(
                            title = "Your Satchel is Empty",
                            subtitle = "Visit a real-world park, historical plaque, or coffee shop on the World Map and perform an action to claim RPG relics!"
                        )
                    }
                } else {
                    items(inventory, key = { it.id.ifBlank { it.name } }) { item ->
                        InventoryItemCard(
                            item = item,
                            onToggleEquip = { viewModel.toggleEquipInventoryItem(item) }
                        )
                    }
                }
            }
            2 -> {
                // Approved Suggestion: Community Creator Quest Builder Studio
                item {
                    CommunityCreatorQuestStudioCard(
                        defaultNeighborhood = regionName,
                        onPublishQuest = { title, neighborhood, landmark, riddle ->
                            viewModel.publishCreatorQuest(title, neighborhood, landmark, riddle)
                        }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onOpenQuestJournal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_full_quest_journal_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GuildGold)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = null,
                            tint = GuildGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.codex_open_full_journal_btn),
                            color = GuildGold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (quests.isEmpty()) {
                    item {
                        EmptyCodexStateCard(
                            title = "No Quests Recorded Yet",
                            subtitle = "Start a walking quest on the World Map or publish a trail above to chronicle your real-world adventures in Cloud Firestore."
                        )
                    }
                } else {
                    items(quests, key = { it.id.ifBlank { it.title } }) { quest ->
                        QuestJournalEntryCard(
                            quest = quest,
                            onNarrateQuest = {
                                viewModel.narrateArtifactOrBadgeLore(
                                    title = quest.questName,
                                    loreText = "Completed in ${quest.location}. ${quest.summary}"
                                )
                            },
                            onShareQuest = {
                                val formattedDate = formatQuestCompletionDate(quest.completedAt)
                                val text = "📜 SideQuest Journal: ${quest.questName}\n📍 ${quest.location}\n🗓️ Completed: $formattedDate\n✨ ${quest.completedStops}/${quest.landmarksCount} stops (${quest.distanceMeters}m) • +${quest.xpEarned} XP"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Quest Chronicle"))
                                } catch (_: Exception) {
                                }
                            },
                            onCompleteActiveQuest = { viewModel.completeQuestById(quest) },
                            onDeleteQuest = { viewModel.deleteQuestFromJournal(quest.id) }
                        )
                    }
                }
            }
            else -> {
                // Suggestion #6: Co-Op Party Expeditions & Neighborhood Sovereignty Leaderboard
                item {
                    CoOpPartyExpeditionCard(
                        partyCode = partyCode,
                        selectedRole = selectedPartyRole,
                        partyMembers = partyMembers,
                        isRiddleSolved = isCoOpRiddleSolved,
                        onSelectRole = { viewModel.selectPartyRole(it) },
                        onCyclePartyCode = { viewModel.cyclePartyRoomCode() },
                        onSolveCoOpRiddle = { viewModel.solveCoOpPartyRiddle() }
                    )
                }

                item {
                    NeighborhoodLeaderboardCard(
                        regionName = regionName,
                        standings = leaderboard
                    )
                }
            }
        }

        // 4. Static & Non-Intrusive Support Independent Development Section
        item {
            StaticSupportDevelopmentSectionCard(
                engagementState = donationEngagementState,
                onOpenSupportScreen = onOpenDonationDialog
            )
        }
    }
}

@Composable
private fun CommunityCreatorQuestStudioCard(
    defaultNeighborhood: String,
    onPublishQuest: (title: String, neighborhood: String, landmark: String, riddle: String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var questTitle by rememberSaveable { mutableStateOf("Lanterns of the Old Quarter") }
    var neighborhood by rememberSaveable { mutableStateOf(defaultNeighborhood) }
    var landmarkStop by rememberSaveable { mutableStateOf("1898 Foundry Archway Plaque") }
    var riddleText by rememberSaveable {
        mutableStateOf("Locate the iron compass rose embedded in the walkway and read the motto aloud.")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocationAlt,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.codex_creator_studio_eyebrow),
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan
                        )
                        Text(
                            text = stringResource(id = R.string.codex_creator_studio_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("toggle_creator_studio_button")
                ) {
                    Text(
                        text = if (expanded) "Hide" else "Create Trail",
                        color = CyberCyan,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.codex_creator_studio_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                OutlinedTextField(
                    value = questTitle,
                    onValueChange = { questTitle = it },
                    label = { Text(stringResource(id = R.string.codex_trail_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = landmarkStop,
                    onValueChange = { landmarkStop = it },
                    label = { Text(stringResource(id = R.string.codex_featured_landmark_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = riddleText,
                    onValueChange = { riddleText = it },
                    label = { Text(stringResource(id = R.string.codex_onsite_riddle_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onPublishQuest(questTitle, neighborhood, landmarkStop, riddleText)
                        expanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_creator_quest_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = MidnightAbyss
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.codex_publish_trail_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RelicAlchemyForgeCard(
    relicCount: Int,
    onForgeRelic: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GuildGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.codex_alchemy_eyebrow),
                        style = MaterialTheme.typography.labelLarge,
                        color = GuildGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.codex_alchemy_reward_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan,
                    maxLines = 1
                )
            }
            Text(
                text = stringResource(id = R.string.codex_alchemy_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onForgeRelic,
                enabled = relicCount >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forge_legendary_relic_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuildGold,
                    contentColor = MidnightAbyss
                )
            ) {
                Text(
                    text = if (relicCount >= 2) {
                        "Synthesize Legendary Relic (2+ Relics Ready)"
                    } else {
                        "Collect ${2 - relicCount} More Relic(s) to Unlock Forge"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun CoOpPartyExpeditionCard(
    partyCode: String,
    selectedRole: PartyRole,
    partyMembers: List<PartyMemberStatus>,
    isRiddleSolved: Boolean,
    onSelectRole: (PartyRole) -> Unit,
    onCyclePartyCode: () -> Unit,
    onSolveCoOpRiddle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.codex_coop_eyebrow),
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan
                    )
                    Text(
                        text = stringResource(id = R.string.codex_party_room_format, partyCode),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onCyclePartyCode,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("switch_party_room_button")
                ) {
                    Text(stringResource(id = R.string.codex_switch_room_btn), color = CyberCyan, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }

            Text(
                text = stringResource(id = R.string.codex_select_party_role),
                style = MaterialTheme.typography.labelMedium,
                color = GuildGold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PartyRole.entries.forEach { role ->
                    FilterChip(
                        selected = role == selectedRole,
                        onClick = { onSelectRole(role) },
                        label = { Text(role.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                            selectedLabelColor = CyberCyan
                        ),
                        modifier = Modifier.testTag("party_role_${role.id}")
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MidnightAbyss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.codex_role_perk_format, selectedRole.perk),
                        style = MaterialTheme.typography.labelLarge,
                        color = GuildGold
                    )
                    Text(
                        text = stringResource(id = R.string.codex_clue_specialty_format, selectedRole.clueSpecialty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            partyMembers.forEach { member ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ElevatedSanctum,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${member.name} • ${member.role.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.codex_member_distance_ready_format, member.distanceMetersAway),
                                style = MaterialTheme.typography.labelMedium,
                                color = ArcaneEmerald,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = member.discoveredClue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GuildGold
                        )
                    }
                }
            }

            Button(
                onClick = onSolveCoOpRiddle,
                enabled = !isRiddleSolved,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("solve_coop_riddle_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRiddleSolved) ArcaneEmerald else CyberCyan,
                    contentColor = MidnightAbyss
                )
            ) {
                Text(
                    text = if (isRiddleSolved) {
                        "Co-Op Riddle Solved! (+250 Party XP & Vanguard Badge)"
                    } else {
                        "Combine Squad Clues & Solve Co-Op Riddle (+250 XP)"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun NeighborhoodLeaderboardCard(
    regionName: String,
    standings: List<NeighborhoodStanding>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.codex_leaderboard_eyebrow),
                style = MaterialTheme.typography.labelLarge,
                color = GuildGold
            )
            Text(
                text = stringResource(id = R.string.codex_leaderboard_title_format, regionName),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            standings.forEach { entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (entry.isCurrentUser) GuildGold.copy(alpha = 0.18f) else MidnightAbyss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (entry.isCurrentUser) GuildGold else SlateCardSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = if (entry.rankPosition == 1) GuildGold else SlateCardSurface,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "#${entry.rankPosition}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (entry.rankPosition == 1) MidnightAbyss else Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = entry.explorerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (entry.isCurrentUser) GuildGold else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(id = R.string.codex_leaderboard_stats_format, entry.titleBadge, entry.landmarksCleared),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.codex_xp_score_format, entry.xpScore),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (entry.isCurrentUser) GuildGold else CyberCyan,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        color = MidnightAbyss.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SlateCardSurface)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = GuildGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    onToggleEquip: () -> Unit
) {
    val rarityColor = rarityColor(item.rarity)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(
            width = if (item.equipped) 2.dp else 1.dp,
            color = if (item.equipped) GuildGold else rarityColor.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = rarityColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, rarityColor)
                ) {
                    Text(
                        text = "${item.rarity} • ${item.itemType}",
                        style = MaterialTheme.typography.labelMedium,
                        color = rarityColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.codex_resonance_bonus_format, item.powerBonus),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold
                )
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Text(
                text = item.loreDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = GuildGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.originLandmark,
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold
                    )
                }

                Button(
                    onClick = onToggleEquip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.equipped) ArcaneEmerald else SlateCardSurface,
                        contentColor = if (item.equipped) MidnightAbyss else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("equip_item_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.equipped) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.equipped) "Equipped" else "Equip Relic",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun EarnedBadgeCard(badge: EarnedBadge) {
    val color = rarityColor(badge.rarity)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${badge.rarity} • ${badge.category}",
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.codex_landmark_prefix_format, badge.landmarkName),
                    style = MaterialTheme.typography.labelMedium,
                    color = GuildGold
                )
            }
        }
    }
}

@Composable
private fun QuestChronicleCard(
    quest: CompletedQuest,
    onShareQuest: () -> Unit
) {
    val isCompleted = quest.status == "COMPLETED"
    val accent = if (isCompleted) ArcaneEmerald else GuildGold
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, accent)
                ) {
                    Text(
                        text = stringResource(id = R.string.codex_quest_status_stops_format, quest.status, quest.completedStops, quest.landmarksCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.codex_quest_xp_dist_format, quest.xpEarned, quest.distanceMeters),
                        style = MaterialTheme.typography.labelLarge,
                        color = GuildGold
                    )
                    IconButton(
                        onClick = onShareQuest,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Quest Chronicle",
                            tint = GuildGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = quest.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Text(
                text = quest.regionName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = quest.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (quest.stopsSummary.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.journal_waypoints_format, quest.stopsSummary.joinToString(" → ")),
                    style = MaterialTheme.typography.labelMedium,
                    color = GuildGold
                )
            }
        }
    }
}

@Composable
private fun EmptyCodexStateCard(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, SlateCardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = GuildGold,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun rarityColor(rarity: String): Color = when (rarity) {
    "LEGENDARY" -> GuildGold
    "EPIC" -> NoirCrimson
    "RARE" -> CyberCyan
    else -> ArcaneEmerald
}
