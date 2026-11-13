package com.example.ui

import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.model.ArtifactCategory
import com.example.model.EarnedBadge
import com.example.model.FoundArtifact
import com.example.model.RelicArtUiState
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.NoirCrimson
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateCardSurface
import java.util.Locale

/**
 * Dedicated Firestore-backed UI Component for managing, filtering, inspecting, and cataloging
 * a user's collection of Found Artifacts (`/users/{userId}/artifacts/{artifactId}`) and
 * Earned Badges (`/users/{userId}/badges/{badgeId}`) collected during walking quests.
 */
@Composable
fun ArtifactBadgeVaultComponent(
    artifacts: List<FoundArtifact>,
    badges: List<EarnedBadge>,
    currentRegionName: String,
    onToggleFavoriteArtifact: (FoundArtifact) -> Unit,
    onUpdateArtifactNotes: (artifactId: String, notes: String) -> Unit,
    onDeleteArtifact: (artifactId: String) -> Unit,
    onCatalogCustomArtifact: (
        name: String,
        category: ArtifactCategory,
        landmarkName: String,
        inscription: String,
        notes: String
    ) -> Unit,
    onNarrateLore: (title: String, loreText: String) -> Unit,
    relicArtUiState: RelicArtUiState = RelicArtUiState.Idle,
    isPaidImageApiApproved: Boolean = false,
    cacheHitsSaved: Int = 0,
    onTogglePaidImageApiApproval: (Boolean) -> Unit = {},
    onGenerateRelicArtwork: (prompt: String) -> Unit = {},
    onEditRelicArtwork: (editInstruction: String) -> Unit = {},
    onClearRelicArt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 0 = Unified Vault (Artifacts + Badges), 1 = Found Artifacts Only, 2 = Earned Badges Only
    var collectionTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedRarity by rememberSaveable { mutableStateOf("ALL") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isGridLayout by rememberSaveable { mutableStateOf(true) }

    var inspectedArtifact by remember { mutableStateOf<FoundArtifact?>(null) }
    var inspectedBadge by remember { mutableStateOf<EarnedBadge?>(null) }
    var showCatalogDialog by rememberSaveable { mutableStateOf(false) }

    val totalResonance = remember(artifacts) { artifacts.sumOf { it.resonanceValue } }
    val favoriteCount = remember(artifacts) { artifacts.count { it.isFavorite } }

    val filteredArtifacts = remember(
        artifacts,
        selectedRarity,
        selectedCategory,
        favoritesOnly,
        searchQuery
    ) {
        artifacts.filter { artifact ->
            val matchesRarity = selectedRarity == "ALL" ||
                artifact.rarity.equals(selectedRarity, ignoreCase = true)
            val matchesCat = selectedCategory == "ALL" ||
                artifact.category.equals(selectedCategory, ignoreCase = true)
            val matchesFav = !favoritesOnly || artifact.isFavorite
            val matchesSearch = searchQuery.isBlank() ||
                artifact.name.contains(searchQuery, ignoreCase = true) ||
                artifact.originLandmark.contains(searchQuery, ignoreCase = true) ||
                artifact.loreInscription.contains(searchQuery, ignoreCase = true)
            matchesRarity && matchesCat && matchesFav && matchesSearch
        }
    }

    val filteredBadges = remember(badges, selectedRarity, searchQuery) {
        badges.filter { badge ->
            val matchesRarity = selectedRarity == "ALL" ||
                badge.rarity.equals(selectedRarity, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                badge.title.contains(searchQuery, ignoreCase = true) ||
                badge.landmarkName.contains(searchQuery, ignoreCase = true) ||
                badge.description.contains(searchQuery, ignoreCase = true)
            matchesRarity && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("artifact_badge_vault_component"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Vault Mastery & Resonance Telemetry Card
        VaultMasterySummaryCard(
            artifacts = artifacts,
            badges = badges,
            totalResonance = totalResonance,
            favoriteCount = favoriteCount,
            onOpenCatalogDialog = { showCatalogDialog = true }
        )

        // 1b. Runtime AI Relic & Travelogue Art Studio (gemini-nano-banana-2.1) with Zero-Bill Eco Mode Guard
        AiRelicArtStudioCard(
            relicArtUiState = relicArtUiState,
            isPaidImageApiApproved = isPaidImageApiApproved,
            cacheHitsSaved = cacheHitsSaved,
            onTogglePaidImageApiApproval = onTogglePaidImageApiApproval,
            featuredArtifactName = artifacts.firstOrNull()?.name ?: "Astral Wayfarer's Compass Shard",
            currentRegionName = currentRegionName,
            onGenerateArt = onGenerateRelicArtwork,
            onEditArt = onEditRelicArtwork,
            onClearArt = onClearRelicArt
        )

        // 2. Collection Mode + Search + Rarity & Category Filters
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, SlateCardSurface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mode Toggle Row + Grid/List Layout Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = collectionTab == 0,
                            onClick = { collectionTab = 0 },
                            label = { Text(stringResource(id = R.string.vault_filter_all_format, artifacts.size + badges.size)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GuildGold.copy(alpha = 0.24f),
                                selectedLabelColor = GuildGold
                            ),
                            modifier = Modifier.testTag("vault_tab_all")
                        )
                        FilterChip(
                            selected = collectionTab == 1,
                            onClick = { collectionTab = 1 },
                            label = { Text(stringResource(id = R.string.vault_filter_artifacts_format, artifacts.size)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.24f),
                                selectedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("vault_tab_artifacts")
                        )
                        FilterChip(
                            selected = collectionTab == 2,
                            onClick = { collectionTab = 2 },
                            label = { Text(stringResource(id = R.string.vault_filter_badges_format, badges.size)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ArcaneEmerald.copy(alpha = 0.24f),
                                selectedLabelColor = ArcaneEmerald
                            ),
                            modifier = Modifier.testTag("vault_tab_badges")
                        )
                    }

                    IconButton(
                        onClick = { isGridLayout = !isGridLayout },
                        modifier = Modifier.testTag("vault_toggle_layout_button")
                    ) {
                        Icon(
                            imageVector = if (isGridLayout) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = if (isGridLayout) "Switch to detailed list view" else "Switch to curio grid view",
                            tint = GuildGold
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(id = R.string.vault_search_placeholder)) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = GuildGold
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search_input")
                )

                // Rarity & Favorites Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = !favoritesOnly },
                        label = { Text(stringResource(id = R.string.vault_favorites_format, favoriteCount)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GuildGold.copy(alpha = 0.25f),
                            selectedLabelColor = GuildGold
                        ),
                        modifier = Modifier.testTag("vault_filter_favorites")
                    )

                    listOf("ALL", "LEGENDARY", "EPIC", "RARE", "COMMON").forEach { rarity ->
                        val chipColor = vaultRarityColor(rarity)
                        FilterChip(
                            selected = selectedRarity == rarity,
                            onClick = { selectedRarity = rarity },
                            label = {
                                Text(
                                    if (rarity == "ALL") "All Rarities" else rarity.lowercase(Locale.US)
                                        .replaceFirstChar { it.titlecase(Locale.US) }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.22f),
                                selectedLabelColor = chipColor
                            ),
                            modifier = Modifier.testTag("vault_filter_rarity_${rarity.lowercase(Locale.US)}")
                        )
                    }
                }

                // Artifact Category Filter Row (shown when viewing Artifacts or Unified)
                AnimatedVisibility(visible = collectionTab != 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == "ALL",
                            onClick = { selectedCategory = "ALL" },
                            label = { Text(stringResource(id = R.string.vault_all_curio_types)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan
                            )
                        )
                        ArtifactCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.id,
                                onClick = { selectedCategory = cat.id },
                                label = { Text(cat.label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = CyberCyan
                                ),
                                modifier = Modifier.testTag("vault_filter_cat_${cat.id.lowercase(Locale.US)}")
                            )
                        }
                    }
                }
            }
        }

        // 3. Found Artifacts Section
        if (collectionTab == 0 || collectionTab == 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.vault_artifacts_header_format, filteredArtifacts.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
                Text(
                    text = stringResource(id = R.string.vault_artifacts_subhint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (filteredArtifacts.isEmpty()) {
                EmptyVaultSectionCard(
                    title = "No Matching Found Artifacts",
                    subtitle = "Clear real-world waypoints on the World Map, scan plaques with Gemini Vision, or tap 'Catalog Field Discovery' above to record curios in Firestore."
                )
            } else if (isGridLayout) {
                val rows = filteredArtifacts.chunked(2)
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { artifact ->
                            FoundArtifactGridTile(
                                artifact = artifact,
                                modifier = Modifier.weight(1f),
                                onInspect = { inspectedArtifact = artifact },
                                onToggleFavorite = { onToggleFavoriteArtifact(artifact) }
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                filteredArtifacts.forEach { artifact ->
                    FoundArtifactDetailRowCard(
                        artifact = artifact,
                        onInspect = { inspectedArtifact = artifact },
                        onToggleFavorite = { onToggleFavoriteArtifact(artifact) },
                        onNarrate = {
                            onNarrateLore(artifact.name, artifact.loreInscription)
                        }
                    )
                }
            }
        }

        // 4. Earned Badges Section
        if (collectionTab == 0 || collectionTab == 2) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.vault_badges_header_format, filteredBadges.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold
                )
                Text(
                    text = stringResource(id = R.string.vault_badges_subhint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (filteredBadges.isEmpty()) {
                EmptyVaultSectionCard(
                    title = "No Matching Guild Badges",
                    subtitle = "Complete walking quests, scan landmarks with Gemini Vision, or solve Co-Op Party Riddles to earn Guild Badges."
                )
            } else if (isGridLayout) {
                val badgeRows = filteredBadges.chunked(2)
                badgeRows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { badge ->
                            EarnedBadgeGridTile(
                                badge = badge,
                                modifier = Modifier.weight(1f),
                                onInspect = { inspectedBadge = badge }
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                filteredBadges.forEach { badge ->
                    EarnedBadgeDetailRowCard(
                        badge = badge,
                        onInspect = { inspectedBadge = badge },
                        onNarrate = {
                            onNarrateLore(badge.title, badge.description)
                        }
                    )
                }
            }
        }
    }

    // 5. Interactive 3D-Style Found Artifact Inspector Dialog
    inspectedArtifact?.let { currentArtifact ->
        // Keep synced with latest Firestore snapshot if favorite/notes changed
        val liveArtifact = artifacts.firstOrNull { it.id == currentArtifact.id } ?: currentArtifact
        ArtifactInspectorDialog(
            artifact = liveArtifact,
            onDismiss = { inspectedArtifact = null },
            onToggleFavorite = { onToggleFavoriteArtifact(liveArtifact) },
            onSaveNotes = { updatedNotes ->
                onUpdateArtifactNotes(liveArtifact.id, updatedNotes)
            },
            onNarrate = {
                onNarrateLore(liveArtifact.name, liveArtifact.loreInscription)
            },
            onShareTravelogue = {
                val shareText = buildString {
                    appendLine("🧭 SIDEQUEST ARTIFACT CHRONICLE")
                    appendLine("✨ ${liveArtifact.name} [${liveArtifact.rarity}]")
                    appendLine("📍 Origin: ${liveArtifact.originLandmark} (${liveArtifact.regionName})")
                    appendLine("🌐 Coordinates: ${String.format(Locale.US, "%.4f, %.4f", liveArtifact.latitude, liveArtifact.longitude)}")
                    appendLine("⚡ Resonance: +${liveArtifact.resonanceValue} • Method: ${liveArtifact.discoveryMethod}")
                    appendLine()
                    appendLine("📜 Inscription: \"${liveArtifact.loreInscription}\"")
                    if (liveArtifact.inspectionNotes.isNotBlank()) {
                        appendLine("🖋️ Explorer Notes: ${liveArtifact.inspectionNotes}")
                    }
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                try {
                    context.startActivity(Intent.createChooser(sendIntent, "Share Illustrated Travelogue Card"))
                } catch (_: Exception) {
                }
            },
            onDelete = {
                onDeleteArtifact(liveArtifact.id)
                inspectedArtifact = null
            }
        )
    }

    // 6. Earned Badge Inspector Dialog
    inspectedBadge?.let { badge ->
        BadgeInspectorDialog(
            badge = badge,
            onDismiss = { inspectedBadge = null },
            onNarrate = { onNarrateLore(badge.title, badge.description) },
            onShareBadge = {
                val shareText = "🏅 SideQuest Guild Crest Unlocked: ${badge.title} [${badge.rarity}]\n" +
                    "📍 Earned at: ${badge.landmarkName}\n" +
                    "📜 Citation: ${badge.description}"
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                try {
                    context.startActivity(Intent.createChooser(sendIntent, "Share Guild Badge"))
                } catch (_: Exception) {
                }
            }
        )
    }

    // 7. Catalog Field Discovery Dialog (Creates FoundArtifact in Firestore)
    if (showCatalogDialog) {
        CatalogFieldArtifactDialog(
            defaultRegionName = currentRegionName,
            onDismiss = { showCatalogDialog = false },
            onConfirmCatalog = { name, category, landmarkName, inscription, notes ->
                onCatalogCustomArtifact(name, category, landmarkName, inscription, notes)
                showCatalogDialog = false
            }
        )
    }
}

@Composable
private fun VaultMasterySummaryCard(
    artifacts: List<FoundArtifact>,
    badges: List<EarnedBadge>,
    totalResonance: Long,
    favoriteCount: Int,
    onOpenCatalogDialog: () -> Unit
) {
    val legendaryCount = artifacts.count { it.rarity == "LEGENDARY" } + badges.count { it.rarity == "LEGENDARY" }
    val epicCount = artifacts.count { it.rarity == "EPIC" } + badges.count { it.rarity == "EPIC" }
    val rareCount = artifacts.count { it.rarity == "RARE" } + badges.count { it.rarity == "RARE" }
    val commonCount = artifacts.count { it.rarity == "COMMON" } + badges.count { it.rarity == "COMMON" }
    val totalItems = (artifacts.size + badges.size).coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            CyberCyan.copy(alpha = 0.12f),
                            ElevatedSanctum
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.vault_eyebrow),
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan
                    )
                    Text(
                        text = stringResource(id = R.string.vault_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = GuildGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, GuildGold)
                ) {
                    Text(
                        text = stringResource(id = R.string.vault_total_resonance_format, totalResonance),
                        style = MaterialTheme.typography.labelLarge,
                        color = GuildGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        maxLines = 1
                    )
                }
            }

            // 4-Column Telemetry Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VaultMetricBox(
                    modifier = Modifier.weight(1f),
                    value = "${artifacts.size}",
                    label = "Artifacts",
                    accent = CyberCyan
                )
                VaultMetricBox(
                    modifier = Modifier.weight(1f),
                    value = "${badges.size}",
                    label = "Badges",
                    accent = GuildGold
                )
                VaultMetricBox(
                    modifier = Modifier.weight(1f),
                    value = "$favoriteCount",
                    label = "Pinned ★",
                    accent = ArcaneEmerald
                )
                VaultMetricBox(
                    modifier = Modifier.weight(1f),
                    value = "$legendaryCount",
                    label = "Legendary",
                    accent = NoirCrimson
                )
            }

            // Visual Rarity Breakdown Spectrum Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.vault_rarity_spectrum),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${legendaryCount}L • ${epicCount}E • ${rareCount}R • ${commonCount}C",
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MidnightAbyss)
                ) {
                    if (legendaryCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(legendaryCount.toFloat() / totalItems)
                                .height(8.dp)
                                .background(GuildGold)
                        )
                    }
                    if (epicCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(epicCount.toFloat() / totalItems)
                                .height(8.dp)
                                .background(NoirCrimson)
                        )
                    }
                    if (rareCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(rareCount.toFloat() / totalItems)
                                .height(8.dp)
                                .background(CyberCyan)
                        )
                    }
                    if (commonCount > 0 || (legendaryCount + epicCount + rareCount == 0)) {
                        Box(
                            modifier = Modifier
                                .weight((commonCount.coerceAtLeast(1)).toFloat() / totalItems)
                                .height(8.dp)
                                .background(ArcaneEmerald)
                        )
                    }
                }
            }

            Button(
                onClick = onOpenCatalogDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_catalog_artifact_dialog_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = MidnightAbyss
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.vault_catalog_discovery_btn),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VaultMetricBox(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accent: Color
) {
    Surface(
        modifier = modifier,
        color = MidnightAbyss.copy(alpha = 0.75f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FoundArtifactGridTile(
    artifact: FoundArtifact,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val rarityColor = vaultRarityColor(artifact.rarity)
    val category = ArtifactCategory.fromId(artifact.category)

    Card(
        modifier = modifier
            .clickable { onInspect() }
            .testTag("artifact_tile_${artifact.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(
            width = if (artifact.isFavorite) 1.5.dp else 1.dp,
            color = if (artifact.isFavorite) GuildGold else rarityColor.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(rarityColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = category.label,
                        tint = rarityColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("favorite_artifact_${artifact.id}")
                ) {
                    Icon(
                        imageVector = if (artifact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (artifact.isFavorite) "Unfavorite artifact" else "Favorite artifact",
                        tint = if (artifact.isFavorite) GuildGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                color = rarityColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, rarityColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "${artifact.rarity} • +${artifact.resonanceValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = rarityColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Text(
                text = artifact.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = artifact.originLandmark,
                style = MaterialTheme.typography.labelSmall,
                color = GuildGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = category.label,
                style = MaterialTheme.typography.labelSmall,
                color = CyberCyan
            )
        }
    }
}

@Composable
private fun FoundArtifactDetailRowCard(
    artifact: FoundArtifact,
    onInspect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNarrate: () -> Unit
) {
    val rarityColor = vaultRarityColor(artifact.rarity)
    val category = ArtifactCategory.fromId(artifact.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
            .testTag("artifact_row_${artifact.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(
            width = if (artifact.isFavorite) 1.5.dp else 1.dp,
            color = if (artifact.isFavorite) GuildGold else rarityColor.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(rarityColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = category.label,
                            tint = rarityColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "${artifact.rarity} • ${category.label.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = rarityColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = artifact.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNarrate) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Narrate artifact lore",
                            tint = CyberCyan
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (artifact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (artifact.isFavorite) GuildGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = artifact.loreInscription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (artifact.inspectionNotes.isNotBlank()) {
                Surface(
                    color = MidnightAbyss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.vault_field_note_format, artifact.inspectionNotes),
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = GuildGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = artifact.originLandmark,
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.vault_resonance_method_format, artifact.resonanceValue, artifact.discoveryMethod),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EarnedBadgeGridTile(
    badge: EarnedBadge,
    modifier: Modifier = Modifier,
    onInspect: () -> Unit
) {
    val color = vaultRarityColor(badge.rarity)
    Card(
        modifier = modifier
            .clickable { onInspect() }
            .testTag("badge_tile_${badge.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Surface(
                color = color.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "${badge.rarity} • ${badge.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = badge.landmarkName,
                style = MaterialTheme.typography.labelSmall,
                color = GuildGold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EarnedBadgeDetailRowCard(
    badge: EarnedBadge,
    onInspect: () -> Unit,
    onNarrate: () -> Unit
) {
    val color = vaultRarityColor(badge.rarity)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
            .testTag("badge_row_${badge.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
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
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.vault_earned_at_format, badge.landmarkName),
                    style = MaterialTheme.typography.labelSmall,
                    color = GuildGold
                )
            }
            IconButton(onClick = onNarrate) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Narrate badge citation",
                    tint = CyberCyan
                )
            }
        }
    }
}

@Composable
private fun ArtifactInspectorDialog(
    artifact: FoundArtifact,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onNarrate: () -> Unit,
    onShareTravelogue: () -> Unit,
    onDelete: () -> Unit
) {
    val rarityColor = vaultRarityColor(artifact.rarity)
    val category = ArtifactCategory.fromId(artifact.category)
    var notesDraft by remember(artifact.id, artifact.inspectionNotes) {
        mutableStateOf(artifact.inspectionNotes)
    }

    val pulseTransition = rememberInfiniteTransition(label = "curio_glow")
    val emblemScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 48.dp)
                .testTag("artifact_inspector_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
            border = BorderStroke(1.5.dp, rarityColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            text = "${artifact.rarity} • ${category.label.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = rarityColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (artifact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (artifact.isFavorite) GuildGold else Color.White
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove artifact from vault",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Animated 3D-style Curio Emblem Showcase
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .scale(emblemScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        rarityColor.copy(alpha = 0.38f),
                                        MidnightAbyss
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = null,
                            tint = rarityColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = artifact.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(id = R.string.vault_ley_resonance_method_format, artifact.resonanceValue, artifact.discoveryMethod),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MidnightAbyss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.vault_lore_inscription),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan
                        )
                        Text(
                            text = artifact.loreInscription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        HorizontalDivider(color = SlateCardSurface, modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = stringResource(id = R.string.vault_landmark_location_format, artifact.originLandmark, artifact.regionName),
                            style = MaterialTheme.typography.labelMedium,
                            color = GuildGold
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "🌐 GPS Telemetry: %.4f, %.4f",
                                artifact.latitude,
                                artifact.longitude
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Editable Explorer Field Notes persisted to Firestore
                OutlinedTextField(
                    value = notesDraft,
                    onValueChange = { notesDraft = it },
                    label = { Text(stringResource(id = R.string.vault_explorer_notes_firestore_label)) },
                    placeholder = { Text(stringResource(id = R.string.vault_explorer_notes_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = GuildGold
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inspector_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNarrate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyberCyan)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.vault_listen_btn), color = CyberCyan)
                    }

                    Button(
                        onClick = { onSaveNotes(notesDraft) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_artifact_notes_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GuildGold,
                            contentColor = MidnightAbyss
                        )
                    ) {
                        Text(stringResource(id = R.string.vault_save_notes_btn), fontWeight = FontWeight.Bold)
                    }
                }

                // Share Illustrated Travelogue Postcard
                Button(
                    onClick = onShareTravelogue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_travelogue_postcard_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcaneEmerald,
                        contentColor = MidnightAbyss
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.vault_export_postcard_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(id = R.string.vault_close_inspector_btn), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BadgeInspectorDialog(
    badge: EarnedBadge,
    onDismiss: () -> Unit,
    onNarrate: () -> Unit,
    onShareBadge: () -> Unit
) {
    val color = vaultRarityColor(badge.rarity)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 48.dp)
                .testTag("badge_inspector_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
            border = BorderStroke(1.5.dp, color)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Surface(
                    color = color.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, color)
                ) {
                    Text(
                        text = stringResource(id = R.string.vault_crest_format, badge.rarity, badge.category),
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.vault_landmark_citation_format, badge.landmarkName),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNarrate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyberCyan)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.vault_listen_btn), color = CyberCyan)
                    }

                    Button(
                        onClick = onShareBadge,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GuildGold,
                            contentColor = MidnightAbyss
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.vault_share_crest_btn), fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.vault_close_btn), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CatalogFieldArtifactDialog(
    defaultRegionName: String,
    onDismiss: () -> Unit,
    onConfirmCatalog: (
        name: String,
        category: ArtifactCategory,
        landmarkName: String,
        inscription: String,
        notes: String
    ) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf(defaultRegionName) }
    var inscription by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedCat by rememberSaveable { mutableStateOf(ArtifactCategory.PLAQUE_RUBBING) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 48.dp)
                .testTag("catalog_field_artifact_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
            border = BorderStroke(1.5.dp, CyberCyan)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.vault_catalog_eyebrow),
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
                Text(
                    text = stringResource(id = R.string.vault_catalog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArtifactCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.vault_artifact_title_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_artifact_name_input")
                )

                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text(stringResource(id = R.string.vault_origin_landmark_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inscription,
                    onValueChange = { inscription = it },
                    label = { Text(stringResource(id = R.string.vault_lore_desc_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(id = R.string.vault_explorer_notes_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.75f)
                    ) {
                        Text(stringResource(id = R.string.vault_cancel_btn))
                    }
                    Button(
                        onClick = {
                            onConfirmCatalog(name, selectedCat, landmark, inscription, notes)
                        },
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("confirm_catalog_artifact_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = MidnightAbyss
                        )
                    ) {
                        Text(stringResource(id = R.string.vault_save_xp_btn), fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultSectionCard(
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = GuildGold,
                fontWeight = FontWeight.Bold
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

@Composable
private fun AiRelicArtStudioCard(
    relicArtUiState: RelicArtUiState,
    isPaidImageApiApproved: Boolean,
    cacheHitsSaved: Int,
    onTogglePaidImageApiApproval: (Boolean) -> Unit,
    featuredArtifactName: String,
    currentRegionName: String,
    onGenerateArt: (String) -> Unit,
    onEditArt: (String) -> Unit,
    onClearArt: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var promptDraft by rememberSaveable(featuredArtifactName, currentRegionName) {
        mutableStateOf(
            "Ornate RPG relic '$featuredArtifactName' discovered in $currentRegionName, glowing astral runes, studio lighting, concept art"
        )
    }
    var editInstructionDraft by rememberSaveable {
        mutableStateOf("Transform into a neon cyberpunk hologram with golden ley-line sparks")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_relic_art_studio_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = GuildGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = stringResource(id = R.string.studio_eyebrow),
                            style = MaterialTheme.typography.labelLarge,
                            color = GuildGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(id = R.string.studio_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("toggle_ai_art_studio_button")
                ) {
                    Text(
                        text = if (expanded) "Hide Studio" else "Open Studio",
                        color = GuildGold,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            // Smart Zero-Bill Eco Mode vs. User Opt-In Paid API Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isPaidImageApiApproved) GuildGold.copy(alpha = 0.14f) else ArcaneEmerald.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isPaidImageApiApproved) GuildGold.copy(alpha = 0.6f) else ArcaneEmerald.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPaidImageApiApproved) {
                                "PAID IMAGE API ENABLED (USER OPT-IN)"
                            } else {
                                "ZERO-BILL ECO MODE ACTIVE ($0 COST GUARD)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isPaidImageApiApproved) GuildGold else ArcaneEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPaidImageApiApproved) {
                                "Live gemini-nano-banana-2.1 image calls unlocked • $cacheHitsSaved API calls saved by cache"
                            } else {
                                "Paid image APIs locked by default. Toggle on only if you want to use Blaze image quota ($cacheHitsSaved cached calls saved)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isPaidImageApiApproved,
                        onCheckedChange = onTogglePaidImageApiApproval,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MidnightAbyss,
                            checkedTrackColor = GuildGold,
                            uncheckedThumbColor = ArcaneEmerald,
                            uncheckedTrackColor = MidnightAbyss
                        ),
                        modifier = Modifier.testTag("toggle_paid_image_api_switch")
                    )
                }
            }

            if (expanded || relicArtUiState !is RelicArtUiState.Idle) {
                OutlinedTextField(
                    value = promptDraft,
                    onValueChange = { promptDraft = it },
                    label = { Text(stringResource(id = R.string.studio_prompt_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_relic_art_prompt_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onGenerateArt(promptDraft) },
                        enabled = relicArtUiState !is RelicArtUiState.Loading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_ai_relic_art_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaidImageApiApproved) GuildGold else SlateCardSurface,
                            contentColor = if (isPaidImageApiApproved) MidnightAbyss else Color.White
                        )
                    ) {
                        Text(
                            text = if (isPaidImageApiApproved) "Generate Art (+150 XP)" else "Opt-In Above to Generate",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onEditArt(editInstructionDraft) },
                        enabled = relicArtUiState !is RelicArtUiState.Loading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_ai_relic_art_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isPaidImageApiApproved) CyberCyan else SlateCardSurface)
                    ) {
                        Text(
                            text = if (isPaidImageApiApproved) "Remix Image (+125 XP)" else "Locked ($0 Eco Mode)",
                            color = if (isPaidImageApiApproved) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = editInstructionDraft,
                    onValueChange = { editInstructionDraft = it },
                    label = { Text(stringResource(id = R.string.studio_remix_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                when (val state = relicArtUiState) {
                    is RelicArtUiState.Idle -> {
                        Text(
                            text = stringResource(id = R.string.studio_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is RelicArtUiState.Loading -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MidnightAbyss,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = GuildGold)
                                Text(
                                    text = stringResource(id = R.string.studio_synthesizing),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GuildGold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = state.prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    is RelicArtUiState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = state.bitmap.asImageBitmap(),
                                contentDescription = "Generated AI Relic Artwork",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✨ ${state.prompt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GuildGold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(onClick = onClearArt) {
                                    Text(stringResource(id = R.string.studio_clear_btn), color = CyberCyan)
                                }
                            }
                        }
                    }
                    is RelicArtUiState.Error -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = stringResource(id = R.string.studio_error_format, state.message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun categoryIcon(category: ArtifactCategory): ImageVector = when (category) {
    ArtifactCategory.PLAQUE_RUBBING -> Icons.Default.HistoryEdu
    ArtifactCategory.RELIC_SHARD -> Icons.Default.AutoAwesome
    ArtifactCategory.BOTANICAL_SPECIMEN -> Icons.Default.Park
    ArtifactCategory.BREW_SEAL -> Icons.Default.LocalCafe
    ArtifactCategory.VISION_SCAN -> Icons.Default.CameraAlt
    ArtifactCategory.CREATOR_TOKEN -> Icons.Default.Explore
}

private fun vaultRarityColor(rarity: String): Color = when (rarity.uppercase(Locale.US)) {
    "LEGENDARY" -> GuildGold
    "EPIC" -> NoirCrimson
    "RARE" -> CyberCyan
    else -> ArcaneEmerald
}
