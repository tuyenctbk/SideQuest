package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DungeonMasterAiRepository
import com.example.model.CharacterStyle
import com.example.model.GuildBounty
import com.example.model.LandmarkCategory
import com.example.model.OfficialHeritageTrail
import com.example.model.RealLandmark
import com.example.model.TavernVoucher
import com.example.model.VisionScanResult
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.NoirCrimson
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateCardSurface
import com.example.ui.theme.VictorianAmber
import java.util.Locale
import kotlin.math.hypot

@Composable
fun QuestMapScreen(
    viewModel: SideQuestViewModel,
    onOpenDungeonMasterChat: () -> Unit,
    onOpenDonationDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val regionName by viewModel.regionName.collectAsState()
    val landmarks by viewModel.landmarks.collectAsState()
    val selectedIndex by viewModel.selectedLandmarkIndex.collectAsState()
    val questTitle by viewModel.activeQuestTitle.collectAsState()
    val questIntro by viewModel.activeQuestIntro.collectAsState()
    val isGenerating by viewModel.isGeneratingQuest.collectAsState()
    val isScanningVision by viewModel.isScanningVision.collectAsState()
    val lastVisionScan by viewModel.lastVisionScanResult.collectAsState()
    val dailyBounties by viewModel.dailyBounties.collectAsState()
    val tavernVouchers by viewModel.tavernVouchers.collectAsState()
    val dailyWalkMeters by viewModel.dailyWalkMetersLogged.collectAsState()
    val isPocketWalkActive by viewModel.isPocketWalkModeActive.collectAsState()
    val pocketProximityMeters by viewModel.pocketWalkProximityMeters.collectAsState()
    val isGuildPassActive by viewModel.isGuildPassActive.collectAsState()
    val isOfflineCached by viewModel.isOfflinePackCached.collectAsState()
    val activeAtmosphere by viewModel.activeAtmosphereEvent.collectAsState()
    val donationEngagementState by viewModel.donationEngagementState.collectAsState()
    val isSpeaking by viewModel.voiceNarrator.isSpeaking.collectAsState()
    val autoNarrate by viewModel.voiceNarrator.autoNarrate.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.scanRealDeviceGps()
    }

    // Suggestion 4: Live Camera Preview Snapshot Launcher (Zero Storage Permission)
    val cameraSnapshotLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.scanLandmarkPhotoWithGemini(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraSnapshotLauncher.launch(null)
        } else {
            val activeLm = landmarks.getOrNull(selectedIndex) ?: landmarks.firstOrNull()
            if (activeLm != null) {
                val bmp = DungeonMasterAiRepository.createLandmarkPlaqueBitmap(
                    landmarkName = activeLm.name,
                    categoryLabel = activeLm.category.label,
                    realFact = activeLm.realFactOrAddress,
                    coordinatesText = String.format(Locale.US, "GPS %.4f, %.4f", activeLm.latitude, activeLm.longitude)
                )
                viewModel.scanLandmarkPhotoWithGemini(bmp)
            }
        }
    }

    // Suggestion 4: Android Photo Picker (Play-Policy Compliant, Zero Permissions)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    if (decoded != null) {
                        viewModel.scanLandmarkPhotoWithGemini(decoded)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    val activeLandmark = landmarks.getOrNull(selectedIndex) ?: landmarks.firstOrNull()
    val clearedCount = landmarks.count { it.isCleared }
    val totalDistanceMeters = landmarks.sumOf { it.distanceMeters }
    val estimatedMinutes = (totalDistanceMeters / 75).coerceAtLeast(8)
    val estimatedKcal = (totalDistanceMeters * 0.055).toInt().coerceAtLeast(35)

    val walkGoalProgress by animateFloatAsState(
        targetValue = (dailyWalkMeters.toFloat() / viewModel.dailyWalkGoalMeters.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "walk_goal_progress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quest_map_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Character Style Picker & Hands-Free Pocket DM / Voice Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(id = R.string.choose_character_archetype),
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pocketDmBase = stringResource(id = R.string.pocket_dm_label)
                    AssistChip(
                        onClick = { viewModel.togglePocketWalkMode() },
                        label = {
                            Text(
                                text = if (isPocketWalkActive) "$pocketDmBase: ${pocketProximityMeters}m" else pocketDmBase,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Toggle Hands-Free Pocket Dungeon Master",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isPocketWalkActive) ArcaneEmerald.copy(alpha = 0.22f) else ObsidianSurface,
                            labelColor = if (isPocketWalkActive) ArcaneEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIconContentColor = if (isPocketWalkActive) ArcaneEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("toggle_pocket_dm_chip")
                    )

                    AssistChip(
                        onClick = { viewModel.voiceNarrator.toggleAutoNarrate() },
                        label = {
                            Text(
                                text = stringResource(
                                    id = if (autoNarrate) R.string.auto_voice_label else R.string.muted_label
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (autoNarrate) {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                },
                                contentDescription = "Toggle Auto Narration",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ObsidianSurface,
                            labelColor = if (autoNarrate) GuildGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            leadingIconContentColor = if (autoNarrate) GuildGold else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("toggle_auto_narrate_chip")
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(CharacterStyle.entries) { style ->
                        CharacterStyleCard(
                            style = style,
                            isSelected = style == selectedStyle,
                            onClick = { viewModel.selectCharacterStyle(style) }
                        )
                    }
                }
            }
        }

        // 2. Daily Walking Streak & 1km Cadence Goal Tracker
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(
                    1.dp,
                    if (dailyWalkMeters >= viewModel.dailyWalkGoalMeters) ArcaneEmerald else GuildGold.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = GuildGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DAILY WALKING CADENCE",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "${dailyWalkMeters}m / ${viewModel.dailyWalkGoalMeters}m",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dailyWalkMeters >= viewModel.dailyWalkGoalMeters) ArcaneEmerald else GuildGold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { walkGoalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (dailyWalkMeters >= viewModel.dailyWalkGoalMeters) ArcaneEmerald else GuildGold,
                        trackColor = MidnightAbyss
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (dailyWalkMeters >= viewModel.dailyWalkGoalMeters) {
                                "Cadence Goal Reached! (+100 XP)"
                            } else {
                                "${viewModel.dailyWalkGoalMeters - dailyWalkMeters}m remaining today"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dailyWalkMeters >= viewModel.dailyWalkGoalMeters) ArcaneEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        AssistChip(
                            onClick = { viewModel.logWalkStepBoost() },
                            label = {
                                Text(
                                    text = "+250m Step Boost (+50 XP)",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = GuildGold.copy(alpha = 0.18f),
                                labelColor = GuildGold
                            ),
                            modifier = Modifier.testTag("log_step_boost_chip")
                        )
                    }

                    // Approved Suggestion: Dynamic Weather & Time-of-Day World Event Engine
                    Surface(
                        onClick = { viewModel.cycleWorldAtmosphereEvent() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cycle_world_atmosphere_banner"),
                        color = MidnightAbyss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
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
                                    text = "WORLD ATMOSPHERE: ${activeAtmosphere.title.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "${activeAtmosphere.bonusLabel} (${activeAtmosphere.favoredCategory.label})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GuildGold
                                )
                            }
                            Text(
                                text = "Shift Weather ↻",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }
        }

        // 3. Real-World Location Scanner & City Search
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, SlateCardSurface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    stringResource(id = R.string.search_city_placeholder),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Location"
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.searchRealCityOrPlace(searchQuery)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = SlateCardSurface,
                                focusedContainerColor = MidnightAbyss,
                                unfocusedContainerColor = MidnightAbyss
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_city_input")
                        )

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    viewModel.searchRealCityOrPlace(searchQuery)
                                } else {
                                    viewModel.regenerateAiQuestForCurrentArea()
                                }
                            },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                            modifier = Modifier.testTag("go_search_quest_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Generate Quest",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(id = R.string.quest_action_btn), style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            label = { Text(stringResource(id = R.string.use_live_gps), maxLines = 1) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Scan Live GPS",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                labelColor = MaterialTheme.colorScheme.primary,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("scan_gps_chip")
                        )

                        AssistChip(
                            onClick = { viewModel.toggleOfflineExpeditionCache() },
                            label = {
                                Text(
                                    text = stringResource(
                                        id = if (isOfflineCached) R.string.offline_pack_ready else R.string.cache_offline_pack
                                    ),
                                    maxLines = 1
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Offline Expedition Pack",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isOfflineCached) ArcaneEmerald.copy(alpha = 0.18f) else MidnightAbyss,
                                labelColor = if (isOfflineCached) ArcaneEmerald else GuildGold,
                                leadingIconContentColor = if (isOfflineCached) ArcaneEmerald else GuildGold
                            ),
                            modifier = Modifier.testTag("offline_pack_chip")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        viewModel.presetRegions.forEach { preset ->
                            val selected = regionName == preset.name
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectPresetRegion(preset) },
                                label = {
                                    Text(
                                        text = preset.name.substringBefore(" —"),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GuildGold.copy(alpha = 0.22f),
                                    selectedLabelColor = GuildGold
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Interactive Tactical Cartography & Real-World Landmark Map
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = regionName.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = questTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = SlateCardSurface,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "$clearedCount / ${landmarks.size} Stops",
                                style = MaterialTheme.typography.labelLarge,
                                color = GuildGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Route Telemetry Bar (Distance, Est. Walk Time, Calories, Guild Multiplier)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MidnightAbyss,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🚶 ${totalDistanceMeters}m • ~$estimatedMinutes min walk • 🔥 $estimatedKcal kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (isGuildPassActive) {
                            Surface(
                                color = GuildGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GuildGold)
                            ) {
                                Text(
                                    text = "1.5x GUILD PASS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GuildGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = questIntro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedVisibility(visible = isGenerating) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = GuildGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pinpointing real parks, historical plaques & cafes + consulting Gemini Dungeon Master...",
                                style = MaterialTheme.typography.labelMedium,
                                color = GuildGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TacticalRealWorldMapCanvas(
                        landmarks = landmarks,
                        selectedIndex = selectedIndex,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onSelectLandmark = { idx ->
                            viewModel.selectLandmark(idx, narrateAloud = true)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(landmarks) { idx, lm ->
                            val isSelected = idx == selectedIndex
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectLandmark(idx, narrateAloud = false) },
                                label = {
                                    Text(
                                        text = "${idx + 1}. ${lm.name.take(22)}",
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (lm.isCleared) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            iconForCategory(lm.category)
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (lm.isCleared) ArcaneEmerald else GuildGold
                                    )
                                },
                                modifier = Modifier.testTag("waypoint_chip_$idx")
                            )
                        }
                    }
                }
            }
        }

        // 5. Selected Real-World Waypoint Lore, Multimodal Vision Scanner & Audio RPG Encounter Card
        if (activeLandmark != null) {
            item {
                ActiveWaypointEncounterCard(
                    landmark = activeLandmark,
                    waypointNumber = selectedIndex + 1,
                    totalWaypoints = landmarks.size,
                    style = selectedStyle,
                    isSpeaking = isSpeaking,
                    isScanningVision = isScanningVision,
                    lastVisionScan = lastVisionScan,
                    onDismissVisionResult = { viewModel.clearVisionScanModal() },
                    onToggleNarrate = { viewModel.narrateCurrentWaypoint() },
                    onPerformEncounterChoice = { choice ->
                        viewModel.performLandmarkEncounter(selectedIndex, choice)
                    },
                    onLaunchCameraVisionScan = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onLaunchPhotoPickerVisionScan = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onInstantPlaqueVisionVerify = {
                        val bmp = DungeonMasterAiRepository.createLandmarkPlaqueBitmap(
                            landmarkName = activeLandmark.name,
                            categoryLabel = activeLandmark.category.label,
                            realFact = activeLandmark.realFactOrAddress,
                            coordinatesText = String.format(
                                Locale.US,
                                "GPS %.4f, %.4f",
                                activeLandmark.latitude,
                                activeLandmark.longitude
                            )
                        )
                        viewModel.scanLandmarkPhotoWithGemini(bmp)
                    },
                    onAskDungeonMaster = onOpenDungeonMasterChat,
                    onOpenInGoogleMaps = {
                        val uri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1&destination=${activeLandmark.latitude},${activeLandmark.longitude}&travelmode=walking"
                        )
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                        }
                    },
                    onShareQuestPassport = {
                        val shareText = buildString {
                            append("🗺️ SideQuest Walking RPG: $questTitle\n")
                            append("🎭 Archetype: ${selectedStyle.title} (${selectedStyle.narratorTitle})\n")
                            append("📍 Current Landmark: ${activeLandmark.name} (${activeLandmark.category.label})\n")
                            append("🏆 Progress: $clearedCount/${landmarks.size} real-world stops cleared in $regionName!")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        try {
                            context.startActivity(Intent.createChooser(sendIntent, "Share SideQuest Walk"))
                        } catch (_: Exception) {
                        }
                    }
                )
            }
        }

        // 6. Suggestion #2: Official Tourism Board & Cultural Heritage Trails
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "OFFICIAL TOURISM & CULTURAL HERITAGE TRAILS (1.5X XP)",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.officialHeritageTrails, key = { it.id }) { trail ->
                        OfficialHeritageTrailCard(
                            trail = trail,
                            onActivateTrail = {
                                viewModel.activateOfficialHeritageTrail(trail)
                            }
                        )
                    }
                }
            }
        }

        // 7. Suggestion #1: Daily Guild Bounties & Local Partner Tavern Vouchers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DAILY GUILD BOUNTIES & LOCAL PARTNER PERKS",
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dailyBounties, key = { it.id }) { bounty ->
                        GuildBountyCard(
                            bounty = bounty,
                            onSelectMatchingStop = {
                                val matchIdx = landmarks.indexOfFirst {
                                    it.category == bounty.targetCategory ||
                                        (bounty.targetCategory == LandmarkCategory.PLAQUE && it.category == LandmarkCategory.MONUMENT)
                                }
                                if (matchIdx >= 0) {
                                    viewModel.selectLandmark(matchIdx, narrateAloud = true)
                                }
                            }
                        )
                    }
                }
            }
        }

        // 8. Redeemable B2B2C "Tavern Rest Stop" Vouchers
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TAVERN REST STOP VOUCHERS (LOCAL CAFE & MUSEUM REWARDS)",
                    style = MaterialTheme.typography.labelLarge,
                    color = ArcaneEmerald
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tavernVouchers, key = { it.id }) { voucher ->
                        TavernVoucherCard(
                            voucher = voucher,
                            onRedeem = { viewModel.redeemTavernVoucher(voucher.id) },
                            onCopyCode = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("SideQuest Voucher Code", voucher.redemptionCode)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficialHeritageTrailCard(
    trail: OfficialHeritageTrail,
    onActivateTrail: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(284.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onActivateTrail)
            .testTag("official_trail_${trail.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CyberCyan.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = trail.sponsorAuthority.take(26),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "${trail.durationMinutes} min",
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold
                )
            }
            Text(
                text = trail.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = trail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏅 ${trail.badgeRewardTitle}",
                    style = MaterialTheme.typography.labelMedium,
                    color = GuildGold
                )
                Text(
                    text = "Launch →",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan
                )
            }
        }
    }
}

@Composable
private fun TavernVoucherCard(
    voucher: TavernVoucher,
    onRedeem: () -> Unit,
    onCopyCode: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val borderColor = when {
        voucher.isRedeemed -> ArcaneEmerald
        voucher.isUnlocked -> GuildGold
        else -> SlateCardSurface
    }
    Card(
        modifier = Modifier
            .width(284.dp)
            .clip(RoundedCornerShape(18.dp))
            .testTag("voucher_card_${voucher.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = if (voucher.isRedeemed) ArcaneEmerald else GuildGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = voucher.partnerName.take(22),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (voucher.isRedeemed) ArcaneEmerald else GuildGold
                    )
                }
                Surface(
                    modifier = Modifier.clickable {
                        onCopyCode()
                        copied = true
                    },
                    color = MidnightAbyss,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = CyberCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (copied) "COPIED" else voucher.redemptionCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberCyan
                        )
                    }
                }
            }

            Text(
                text = voucher.discountHeadline,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = voucher.termsDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Button(
                onClick = onRedeem,
                enabled = !voucher.isRedeemed,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("redeem_voucher_${voucher.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (voucher.isRedeemed) ArcaneEmerald else GuildGold,
                    contentColor = MidnightAbyss
                ),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text(
                    text = if (voucher.isRedeemed) {
                        "Perk Claimed (${voucher.redemptionCode})"
                    } else {
                        "Claim Partner Perk (+100 XP)"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun GuildBountyCard(
    bounty: GuildBounty,
    onSelectMatchingStop: () -> Unit
) {
    val borderColor = if (bounty.isCompleted) ArcaneEmerald else GuildGold.copy(alpha = 0.45f)
    Card(
        modifier = Modifier
            .width(268.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelectMatchingStop)
            .testTag("bounty_card_${bounty.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (bounty.isCompleted) {
                        ArcaneEmerald.copy(alpha = 0.2f)
                    } else {
                        GuildGold.copy(alpha = 0.16f)
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (bounty.isCompleted) "BOUNTY CLAIMED" else bounty.sponsorTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (bounty.isCompleted) ArcaneEmerald else GuildGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "+${bounty.bonusXp} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (bounty.isCompleted) ArcaneEmerald else CyberCyan
                )
            }
            Text(
                text = bounty.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = bounty.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = bounty.rewardPerk,
                style = MaterialTheme.typography.labelMedium,
                color = GuildGold
            )
        }
    }
}

@Composable
private fun CharacterStyleCard(
    style: CharacterStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = when (style) {
        CharacterStyle.CYBERPUNK_SCOUT -> CyberCyan
        CharacterStyle.VICTORIAN_HISTORIAN -> VictorianAmber
        CharacterStyle.ARCANE_CARTOGRAPHER -> ArcaneEmerald
        CharacterStyle.NOIR_DETECTIVE -> NoirCrimson
    }
    val imageRes = when (style) {
        CharacterStyle.CYBERPUNK_SCOUT -> R.drawable.img_persona_cyberpunk_1794444308262
        CharacterStyle.VICTORIAN_HISTORIAN -> R.drawable.img_persona_victorian_1794444307617
        CharacterStyle.ARCANE_CARTOGRAPHER -> R.drawable.img_persona_arcane_1794444308955
        CharacterStyle.NOIR_DETECTIVE -> R.drawable.img_hero_sidequest_1794444307373
    }

    Card(
        modifier = Modifier
            .width(192.dp)
            .height(118.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("style_card_${style.id}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) accent else SlateCardSurface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = style.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MidnightAbyss.copy(alpha = 0.25f),
                                MidnightAbyss.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = style.narratorTitle.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = style.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TacticalRealWorldMapCanvas(
    landmarks: List<RealLandmark>,
    selectedIndex: Int,
    accentColor: Color,
    onSelectLandmark: (Int) -> Unit
) {
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }
    var zoomLevel by rememberSaveable { mutableFloatStateOf(1f) }

    val radarTransition = rememberInfiniteTransition(label = "radar_sweep")
    val radarAngle by radarTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )
    val beaconPulse by radarTransition.animateFloat(
        initialValue = 22f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_pulse"
    )

    val normalizedPoints = remember(landmarks, canvasWidth, canvasHeight, zoomLevel) {
        if (landmarks.isEmpty()) {
            emptyList()
        } else {
            val minLat = landmarks.minOf { it.latitude }
            val maxLat = landmarks.maxOf { it.latitude }
            val minLon = landmarks.minOf { it.longitude }
            val maxLon = landmarks.maxOf { it.longitude }
            val latSpan = (maxLat - minLat).coerceAtLeast(0.0015)
            val lonSpan = (maxLon - minLon).coerceAtLeast(0.0015)
            val padX = canvasWidth * 0.15f
            val padY = canvasHeight * 0.20f
            val usableW = (canvasWidth - padX * 2).coerceAtLeast(10f)
            val usableH = (canvasHeight - padY * 2).coerceAtLeast(10f)
            val cx = canvasWidth / 2f
            val cy = canvasHeight / 2f

            val maxX = (canvasWidth - 28f).coerceAtLeast(28f)
            val maxY = (canvasHeight - 28f).coerceAtLeast(28f)

            landmarks.map { lm ->
                val nx = ((lm.longitude - minLon) / lonSpan).toFloat()
                val ny = (1.0 - ((lm.latitude - minLat) / latSpan)).toFloat()
                val baseX = padX + nx * usableW
                val baseY = padY + ny * usableH
                Offset(
                    x = if (maxX > 28f) (cx + (baseX - cx) * zoomLevel).coerceIn(28f, maxX) else cx,
                    y = if (maxY > 28f) (cy + (baseY - cy) * zoomLevel).coerceIn(28f, maxY) else cy
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MidnightAbyss)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(landmarks, normalizedPoints) {
                    detectTapGestures { tapOffset ->
                        val tappedIdx = normalizedPoints.indexOfFirst { pt ->
                            hypot(tapOffset.x - pt.x, tapOffset.y - pt.y) <= 68f
                        }
                        if (tappedIdx >= 0) {
                            onSelectLandmark(tappedIdx)
                        }
                    }
                }
        ) {
            canvasWidth = size.width
            canvasHeight = size.height

            val gridStep = size.width / 8f
            for (i in 1..7) {
                val x = i * gridStep
                drawLine(
                    color = accentColor.copy(alpha = 0.08f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
            val rowStep = size.height / 5f
            for (j in 1..4) {
                val y = j * rowStep
                drawLine(
                    color = accentColor.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            val center = Offset(size.width / 2f, size.height / 2f)
            listOf(0.20f, 0.35f, 0.48f).forEach { factor ->
                drawCircle(
                    color = accentColor.copy(alpha = 0.11f),
                    radius = size.minDimension * factor,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // Rotating Tactical Radar Sweep
            rotate(degrees = radarAngle, pivot = center) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                        start = center,
                        end = Offset(center.x + size.minDimension * 0.48f, center.y)
                    ),
                    start = center,
                    end = Offset(center.x + size.minDimension * 0.48f, center.y),
                    strokeWidth = 3f
                )
            }

            if (normalizedPoints.size >= 2) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                for (i in 0 until normalizedPoints.lastIndex) {
                    val start = normalizedPoints[i]
                    val end = normalizedPoints[i + 1]
                    val segmentCleared = landmarks.getOrNull(i)?.isCleared == true
                    drawLine(
                        color = if (segmentCleared) ArcaneEmerald else GuildGold.copy(alpha = 0.78f),
                        start = start,
                        end = end,
                        strokeWidth = 5f,
                        pathEffect = if (segmentCleared) null else dashEffect
                    )
                }
            }

            normalizedPoints.forEachIndexed { idx, pt ->
                val lm = landmarks.getOrNull(idx) ?: return@forEachIndexed
                val isSelected = idx == selectedIndex
                val nodeColor = when {
                    lm.isCleared -> ArcaneEmerald
                    isSelected -> accentColor
                    else -> GuildGold
                }

                if (isSelected) {
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.22f),
                        radius = beaconPulse,
                        center = pt
                    )
                    drawCircle(
                        color = nodeColor,
                        radius = 24f,
                        center = pt,
                        style = Stroke(width = 3f)
                    )
                }

                drawCircle(
                    color = MidnightAbyss,
                    radius = 16f,
                    center = pt
                )
                drawCircle(
                    color = nodeColor,
                    radius = 12f,
                    center = pt
                )
            }
        }

        // Top-left HUD Coordinate Overlay
        val activeLm = landmarks.getOrNull(selectedIndex)
        if (activeLm != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                color = ObsidianSurface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationSearching,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "GPS %.4f, %.4f • %dm walk",
                            activeLm.latitude,
                            activeLm.longitude,
                            activeLm.distanceMeters
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Top-right Zoom Toggle Button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    zoomLevel = when {
                        zoomLevel < 1.2f -> 1.35f
                        zoomLevel < 1.6f -> 1.7f
                        else -> 1.0f
                    }
                }
                .testTag("map_zoom_button"),
            color = ObsidianSurface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Cycle Map Zoom",
                    tint = GuildGold,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = String.format(Locale.US, "%.1fx", zoomLevel),
                    style = MaterialTheme.typography.labelMedium,
                    color = GuildGold
                )
            }
        }

        // Bottom-right Legend
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            color = ObsidianSurface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Tap any waypoint pin on the map",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ActiveWaypointEncounterCard(
    landmark: RealLandmark,
    waypointNumber: Int,
    totalWaypoints: Int,
    style: CharacterStyle,
    isSpeaking: Boolean,
    isScanningVision: Boolean,
    lastVisionScan: VisionScanResult?,
    onDismissVisionResult: () -> Unit,
    onToggleNarrate: () -> Unit,
    onPerformEncounterChoice: (String) -> Unit,
    onLaunchCameraVisionScan: () -> Unit,
    onLaunchPhotoPickerVisionScan: () -> Unit,
    onInstantPlaqueVisionVerify: () -> Unit,
    onAskDungeonMaster: () -> Unit,
    onOpenInGoogleMaps: () -> Unit,
    onShareQuestPassport: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = ElevatedSanctum)
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
                Surface(
                    modifier = Modifier.weight(1f, fill = false),
                    color = if (landmark.isCleared) {
                        ArcaneEmerald.copy(alpha = 0.2f)
                    } else {
                        GuildGold.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.dp,
                        if (landmark.isCleared) ArcaneEmerald else GuildGold
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = iconForCategory(landmark.category),
                            contentDescription = null,
                            tint = if (landmark.isCleared) ArcaneEmerald else GuildGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (landmark.isCleared) {
                                "WAYPOINT $waypointNumber/$totalWaypoints CLEARED"
                            } else {
                                "STOP $waypointNumber/$totalWaypoints • ${landmark.category.rpgLabelFor(style).uppercase(Locale.US)}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (landmark.isCleared) ArcaneEmerald else GuildGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onShareQuestPassport,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ObsidianSurface)
                            .testTag("share_quest_passport_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Quest Passport",
                            tint = GuildGold
                        )
                    }

                    IconButton(
                        onClick = onToggleNarrate,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .testTag("narrate_waypoint_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isSpeaking) "Stop Voice Narration" else "Play Voice Narration",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = landmark.name,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Text(
                text = landmark.rpgNodeTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MidnightAbyss.copy(alpha = 0.72f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SlateCardSurface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = GuildGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${style.narratorTitle} Audio Lore",
                            style = MaterialTheme.typography.labelLarge,
                            color = GuildGold
                        )
                    }
                    Text(
                        text = landmark.loreSnippet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ObsidianSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "REAL LANDMARK INTEL",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = landmark.realFactOrAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Suggestion #4: Multimodal Gemini Vision Relic & Plaque Scanner Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MidnightAbyss.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "GEMINI VISION RELIC SCANNER",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "+175 XP & Epic Relic",
                            style = MaterialTheme.typography.labelMedium,
                            color = GuildGold
                        )
                    }

                    Text(
                        text = "Point your lens at ${landmark.name}'s plaque, facade, or surroundings so ${style.narratorTitle} can inspect it with Gemini Vision.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isScanningVision) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CyberCyan,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Gemini Multimodal Vision is analyzing landmark inscription & minting relic...",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onLaunchCameraVisionScan,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("camera_vision_scan_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = MidnightAbyss
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = onLaunchPhotoPickerVisionScan,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("picker_vision_scan_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Photo", color = CyberCyan, style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = onInstantPlaqueVisionVerify,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("instant_plaque_vision_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = GuildGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan Plaque", color = GuildGold, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }

                    AnimatedVisibility(visible = lastVisionScan != null) {
                        if (lastVisionScan != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                                border = BorderStroke(1.dp, ArcaneEmerald)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "VISION VERIFIED • +${lastVisionScan.bonusXp} XP",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ArcaneEmerald
                                        )
                                        Text(
                                            text = "Dismiss",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GuildGold,
                                            modifier = Modifier.clickable { onDismissVisionResult() }
                                        )
                                    }
                                    Text(
                                        text = lastVisionScan.landmarkTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = lastVisionScan.inCharacterAnalysis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Minted Epic Relic: ${lastVisionScan.awardedRelicName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = GuildGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "CHOOSE YOUR ACTION AT THIS LANDMARK (+${landmark.xpReward} XP & '${landmark.rewardItemName}')",
                style = MaterialTheme.typography.labelLarge,
                color = GuildGold
            )

            landmark.actionChoices.forEachIndexed { idx, choice ->
                Button(
                    onClick = { onPerformEncounterChoice(choice) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("encounter_choice_$idx"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (idx == 0) MaterialTheme.colorScheme.primary else ObsidianSurface,
                        contentColor = if (idx == 0) MidnightAbyss else Color.White
                    ),
                    border = if (idx == 0) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = choice,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenInGoogleMaps,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_google_maps_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = GuildGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google Maps",
                        color = GuildGold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onAskDungeonMaster,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ask_dm_about_landmark_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ask DM Lore",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private fun iconForCategory(category: LandmarkCategory) = when (category) {
    LandmarkCategory.PARK -> Icons.Default.Park
    LandmarkCategory.PLAQUE -> Icons.Default.HistoryEdu
    LandmarkCategory.COFFEE_SHOP -> Icons.Default.Coffee
    LandmarkCategory.MONUMENT -> Icons.Default.Map
    LandmarkCategory.MUSEUM -> Icons.Default.HistoryEdu
}
