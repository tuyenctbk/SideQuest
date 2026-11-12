package com.example.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CharacterStyle
import com.example.model.CompletedQuest
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateCardSurface
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dedicated 'Quest Journal' screen using a LazyColumn list layout to display the history
 * of completed quests fetched in real time from Cloud Firestore (`/users/{userId}/quests`),
 * including the quest name, location, and date completed.
 */
@Composable
fun QuestJournalScreen(
    viewModel: SideQuestViewModel,
    modifier: Modifier = Modifier,
    onNavigateToMap: () -> Unit = {}
) {
    val completedQuests by viewModel.completedQuestsState.collectAsState()
    val allQuests by viewModel.questsState.collectAsState()
    val currentRegion by viewModel.regionName.collectAsState()

    QuestJournalContent(
        completedQuests = completedQuests,
        allQuests = allQuests,
        defaultLocation = currentRegion,
        onLogCompletedQuest = { name, location, summary ->
            viewModel.logCompletedQuestToJournal(
                questName = name,
                location = location,
                summary = summary
            )
        },
        onCompleteActiveQuest = { quest ->
            viewModel.completeQuestById(quest)
        },
        onDeleteQuest = { questId ->
            viewModel.deleteQuestFromJournal(questId)
        },
        onNarrateQuest = { quest ->
            viewModel.narrateArtifactOrBadgeLore(
                title = quest.questName,
                loreText = "Completed in ${quest.location}. ${quest.summary}"
            )
        },
        onNavigateToMap = onNavigateToMap,
        modifier = modifier
    )
}

@Composable
fun QuestJournalContent(
    completedQuests: List<CompletedQuest>,
    allQuests: List<CompletedQuest> = completedQuests,
    defaultLocation: String = "San Francisco — Embarcadero",
    onLogCompletedQuest: (questName: String, location: String, summary: String) -> Unit = { _, _, _ -> },
    onCompleteActiveQuest: (CompletedQuest) -> Unit = {},
    onDeleteQuest: (String) -> Unit = {},
    onNarrateQuest: (CompletedQuest) -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOnlyCompleted by rememberSaveable { mutableStateOf(true) }
    var selectedStyleFilter by rememberSaveable { mutableStateOf("ALL") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showLogQuestForm by rememberSaveable { mutableStateOf(false) }

    var newQuestName by rememberSaveable { mutableStateOf("Shadows of the Ferry Archway") }
    var newQuestLocation by rememberSaveable(defaultLocation) { mutableStateOf(defaultLocation) }
    var newQuestSummary by rememberSaveable {
        mutableStateOf("Investigated the historic waterfront plaques and decoded the brass compass inscription.")
    }

    val baseQuests = if (showOnlyCompleted) {
        if (completedQuests.isNotEmpty()) completedQuests else allQuests.filter { it.status == "COMPLETED" }
    } else {
        allQuests
    }

    val filteredQuests = remember(baseQuests, selectedStyleFilter, searchQuery) {
        baseQuests.filter { quest ->
            val matchesStyle = selectedStyleFilter == "ALL" || quest.characterStyle == selectedStyleFilter
            val query = searchQuery.trim()
            val matchesSearch = query.isBlank() ||
                quest.questName.contains(query, ignoreCase = true) ||
                quest.location.contains(query, ignoreCase = true) ||
                quest.summary.contains(query, ignoreCase = true) ||
                quest.stopsSummary.any { it.contains(query, ignoreCase = true) }
            matchesStyle && matchesSearch
        }
    }

    val activeQuestsCount = remember(allQuests) {
        allQuests.count { it.status != "COMPLETED" }
    }
    val totalCompletedCount = remember(completedQuests, allQuests) {
        maxOf(completedQuests.size, allQuests.count { it.status == "COMPLETED" })
    }
    val totalXpEarned = remember(completedQuests) {
        completedQuests.sumOf { it.xpEarned }
    }
    val totalDistanceMeters = remember(completedQuests) {
        completedQuests.sumOf { it.distanceMeters }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("quest_journal_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Quest Journal Header & Firestore Summary Stats
        item(key = "quest_journal_header") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quest_journal_header_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
                border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.55f))
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(GuildGold.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryEdu,
                                    contentDescription = "Quest Journal",
                                    tint = GuildGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "CLOUD FIRESTORE CHRONICLE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "Quest Journal",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { showLogQuestForm = !showLogQuestForm },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GuildGold,
                                contentColor = MidnightAbyss
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("toggle_log_completed_quest_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddTask,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showLogQuestForm) "Close" else "Log Quest",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Your permanent ledger of completed real-world walking quests, locations explored, and completion dates synced with Cloud Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        JournalMetricPill(
                            modifier = Modifier.weight(1f),
                            value = "$totalCompletedCount",
                            label = "Quests Completed",
                            accentColor = ArcaneEmerald
                        )
                        JournalMetricPill(
                            modifier = Modifier.weight(1f),
                            value = "+$totalXpEarned",
                            label = "Journal XP",
                            accentColor = GuildGold
                        )
                        JournalMetricPill(
                            modifier = Modifier.weight(1f),
                            value = "${totalDistanceMeters}m",
                            label = "Distance Walked",
                            accentColor = CyberCyan
                        )
                    }

                    AnimatedVisibility(visible = showLogQuestForm) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            color = ObsidianSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "RECORD COMPLETED QUEST IN FIRESTORE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = CyberCyan
                                )
                                OutlinedTextField(
                                    value = newQuestName,
                                    onValueChange = { newQuestName = it },
                                    label = { Text("Quest Name") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = SlateCardSurface,
                                        focusedContainerColor = MidnightAbyss,
                                        unfocusedContainerColor = MidnightAbyss
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quest_journal_name_input")
                                )
                                OutlinedTextField(
                                    value = newQuestLocation,
                                    onValueChange = { newQuestLocation = it },
                                    label = { Text("Location / Neighborhood") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = SlateCardSurface,
                                        focusedContainerColor = MidnightAbyss,
                                        unfocusedContainerColor = MidnightAbyss
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quest_journal_location_input")
                                )
                                OutlinedTextField(
                                    value = newQuestSummary,
                                    onValueChange = { newQuestSummary = it },
                                    label = { Text("Field Summary & Landmarks Cleared") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = SlateCardSurface,
                                        focusedContainerColor = MidnightAbyss,
                                        unfocusedContainerColor = MidnightAbyss
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quest_journal_summary_input")
                                )
                                Button(
                                    onClick = {
                                        onLogCompletedQuest(
                                            newQuestName,
                                            newQuestLocation,
                                            newQuestSummary
                                        )
                                        showLogQuestForm = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_completed_quest_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ArcaneEmerald,
                                        contentColor = MidnightAbyss
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Save Completed Quest to Firestore (+250 XP)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Search & Status/Persona Filter Bar
        item(key = "quest_journal_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by quest name, location, or landmark...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Quest Journal",
                            tint = GuildGold
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GuildGold,
                        unfocusedBorderColor = SlateCardSurface,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quest_journal_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showOnlyCompleted,
                        onClick = { showOnlyCompleted = true },
                        label = { Text("Completed History ($totalCompletedCount)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArcaneEmerald.copy(alpha = 0.22f),
                            selectedLabelColor = ArcaneEmerald
                        ),
                        modifier = Modifier.testTag("filter_completed_quests_chip")
                    )

                    FilterChip(
                        selected = !showOnlyCompleted,
                        onClick = { showOnlyCompleted = false },
                        label = {
                            Text(
                                if (activeQuestsCount > 0) {
                                    "All Quests (${allQuests.size} • $activeQuestsCount Active)"
                                } else {
                                    "All Quests (${allQuests.size})"
                                }
                            )
                        },
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
                        modifier = Modifier.testTag("filter_all_quests_chip")
                    )

                    FilterChip(
                        selected = selectedStyleFilter == "ALL",
                        onClick = { selectedStyleFilter = "ALL" },
                        label = { Text("All Personas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                            selectedLabelColor = CyberCyan
                        )
                    )

                    CharacterStyle.entries.forEach { style ->
                        FilterChip(
                            selected = selectedStyleFilter == style.id,
                            onClick = {
                                selectedStyleFilter = if (selectedStyleFilter == style.id) "ALL" else style.id
                            },
                            label = { Text(style.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }
                }
            }
        }

        // 3. Quest Journal List Items (Quest Name, Location, Date Completed)
        if (filteredQuests.isEmpty()) {
            item(key = "quest_journal_empty_state") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quest_journal_empty_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = BorderStroke(1.dp, SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = null,
                            tint = GuildGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "No Matching Quests in Journal"
                            } else {
                                "No Completed Quests Recorded Yet"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                "Try clearing your search filter or switching persona filters above."
                            } else {
                                "Complete all waypoints on a walking quest or tap 'Log Quest' above to record your expedition in Cloud Firestore."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showLogQuestForm = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GuildGold)
                            ) {
                                Text("Log Completed Quest", color = GuildGold)
                            }
                            Button(
                                onClick = onNavigateToMap,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = MidnightAbyss
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Explore Map")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = filteredQuests,
                key = { quest -> quest.id.ifBlank { "${quest.title}_${quest.regionName}" } }
            ) { quest ->
                QuestJournalEntryCard(
                    quest = quest,
                    onNarrateQuest = { onNarrateQuest(quest) },
                    onShareQuest = {
                        val formattedDate = formatQuestCompletionDate(quest.completedAt)
                        val shareBody = buildString {
                            appendLine("📜 SideQuest Journal — Completed Quest")
                            appendLine("⚔️ Quest: ${quest.questName}")
                            appendLine("📍 Location: ${quest.location}")
                            appendLine("🗓️ Completed: $formattedDate")
                            appendLine("✨ Rewards: +${quest.xpEarned} XP • ${quest.distanceMeters}m walked (${quest.completedStops}/${quest.landmarksCount} stops)")
                            if (quest.stopsSummary.isNotEmpty()) {
                                appendLine("🧭 Waypoints: ${quest.stopsSummary.joinToString(" → ")}")
                            }
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareBody)
                        }
                        try {
                            context.startActivity(Intent.createChooser(sendIntent, "Share Quest Journal Entry"))
                        } catch (_: Exception) {
                        }
                    },
                    onCompleteActiveQuest = { onCompleteActiveQuest(quest) },
                    onDeleteQuest = {
                        if (quest.id.isNotBlank()) {
                            onDeleteQuest(quest.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun QuestJournalEntryCard(
    quest: CompletedQuest,
    onNarrateQuest: () -> Unit = {},
    onShareQuest: () -> Unit = {},
    onCompleteActiveQuest: () -> Unit = {},
    onDeleteQuest: () -> Unit = {}
) {
    val isCompleted = quest.status == "COMPLETED"
    val statusAccent = if (isCompleted) ArcaneEmerald else GuildGold
    val persona = remember(quest.characterStyle) { CharacterStyle.fromId(quest.characterStyle) }
    val formattedDate = remember(quest.updatedAt, quest.createdAt) {
        formatQuestCompletionDate(quest.completedAt)
    }
    val safeTagSuffix = quest.id.ifBlank { quest.questName.hashCode().toString() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quest_journal_item_$safeTagSuffix"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Status Badge + Persona Archetype + XP & Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = statusAccent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, statusAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = statusAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isCompleted) "COMPLETED" else "IN PROGRESS (${quest.completedStops}/${quest.landmarksCount})",
                                style = MaterialTheme.typography.labelMedium,
                                color = statusAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = CyberCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = persona.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "+${quest.xpEarned} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold,
                    fontWeight = FontWeight.Bold
                )
            }

            // 1. QUEST NAME (Title)
            Text(
                text = quest.questName.ifBlank { "Untitled Walking Quest" },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("quest_journal_item_name_$safeTagSuffix")
            )

            // 2. LOCATION & 3. DATE COMPLETED Metadata Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ElevatedSanctum,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SlateCardSurface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Location Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Quest Location",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Location",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = quest.location.ifBlank { "Local Exploration Sector" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("quest_journal_item_location_$safeTagSuffix")
                            )
                        }
                    }

                    HorizontalDivider(color = SlateCardSurface.copy(alpha = 0.6f))

                    // Date Completed & Walking Stats Row
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
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Date Completed",
                                tint = GuildGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = if (isCompleted) "Date Completed" else "Started On",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GuildGold,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.testTag("quest_journal_item_date_$safeTagSuffix")
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = ArcaneEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${quest.distanceMeters}m • ${quest.completedStops}/${quest.landmarksCount} stops",
                                style = MaterialTheme.typography.labelMedium,
                                color = ArcaneEmerald
                            )
                        }
                    }
                }
            }

            // Quest Narrative Summary
            if (quest.summary.isNotBlank()) {
                Text(
                    text = quest.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Landmarks / Waypoints Visited
            if (quest.stopsSummary.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = GuildGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Waypoints: ${quest.stopsSummary.joinToString(" → ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Bar: Narrate, Share, Complete (if active), Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onNarrateQuest,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("quest_journal_narrate_$safeTagSuffix")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Narrate Quest Chronicle",
                            tint = GuildGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Narrate",
                            style = MaterialTheme.typography.labelMedium,
                            color = GuildGold
                        )
                    }

                    if (!isCompleted) {
                        Button(
                            onClick = onCompleteActiveQuest,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArcaneEmerald,
                                contentColor = MidnightAbyss
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("quest_journal_complete_$safeTagSuffix")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark Complete",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShareQuest,
                        modifier = Modifier.testTag("quest_journal_share_$safeTagSuffix")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Quest Journal Entry",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (quest.id.isNotBlank()) {
                        IconButton(
                            onClick = onDeleteQuest,
                            modifier = Modifier.testTag("quest_journal_delete_$safeTagSuffix")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Quest Journal Entry",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalMetricPill(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        color = MidnightAbyss.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun formatQuestCompletionDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "Synced • Just now"
    return try {
        val formatter = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US)
        formatter.format(timestamp.toDate())
    } catch (_: Exception) {
        "Completed recently"
    }
}
