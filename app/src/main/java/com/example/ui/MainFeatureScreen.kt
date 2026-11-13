package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.DungeonMasterAiRepository
import com.example.data.LandmarkDiscoveryRepository
import com.example.data.SideQuestRepository
import com.example.model.ExplorerRank
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SideQuestTheme
import com.example.ui.theme.SlateCardSurface
import com.example.util.AppEngagementTracker
import com.example.util.VoiceNarrator
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    currentUserId: String,
    displayName: String,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: SideQuestViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SideQuestViewModel(
                    firestoreRepo = SideQuestRepository(db),
                    landmarkRepo = LandmarkDiscoveryRepository(app),
                    aiRepo = DungeonMasterAiRepository(),
                    voiceNarrator = VoiceNarrator(app),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    engagementTracker = AppEngagementTracker(app)
                )
            }
        }
    )

    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val profile by viewModel.profileState.collectAsState()
    val aiErrorBanner by viewModel.aiErrorBanner.collectAsState()
    val rewardBanner by viewModel.rewardBanner.collectAsState()
    val firestoreError by viewModel.firestoreErrorMessage.collectAsState()
    val isGuildPassActive by viewModel.isGuildPassActive.collectAsState()
    val isPaidImageApiApproved by viewModel.isPaidImageApiApproved.collectAsState()
    val freeTierCallsUsed by viewModel.freeTierCallsUsed.collectAsState()
    val cacheHitsSaved by viewModel.cacheHitsSaved.collectAsState()
    val paidImageCallsUsed by viewModel.paidImageCallsUsed.collectAsState()
    val donationEngagementState by viewModel.donationEngagementState.collectAsState()
    val telemetryState by viewModel.engagementTelemetryState.collectAsState()
    val isSpeaking by viewModel.voiceNarrator.isSpeaking.collectAsState()
    val speakingTitle by viewModel.voiceNarrator.currentTitle.collectAsState()

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var showBusinessHubDialog by rememberSaveable { mutableStateOf(false) }
    var showOnboardingFlow by rememberSaveable { mutableStateOf(false) }
    var dismissedQuickStartBannerThisSession by rememberSaveable { mutableStateOf(false) }

    if (showOnboardingFlow) {
        BackHandler {
            showOnboardingFlow = false
        }
    } else if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    val level = profile?.level ?: 1L
    val rank = ExplorerRank.fromLevel(level)

    SideQuestTheme(characterStyle = selectedStyle) {
        if (showOnboardingFlow) {
            OnboardingFlowScreen(
                selectedStyle = selectedStyle,
                onSelectStyle = { viewModel.selectCharacterStyle(it) },
                onPreviewVoice = { style ->
                    viewModel.voiceNarrator.speak(
                        text = "${style.narratorTitle}: ${style.tagline}",
                        style = style,
                        title = "${style.narratorTitle} • Preview"
                    )
                },
                onCompleteOnboarding = {
                    viewModel.completeOnboardingTour()
                    showOnboardingFlow = false
                }
            )
            return@SideQuestTheme
        }

        // Smart Auto Pop-Up Suggestions (Update App, Share App, Rate App)
        // Strictly governed by AppEngagementTracker: NEVER shown at app start, max 1 per session, multi-day cooldowns
        SmartLifecyclePromptHost(
            telemetryState = telemetryState,
            adventurerName = profile?.displayName ?: displayName,
            completedQuestsCount = (profile?.questsCompleted ?: 1L).toInt().coerceAtLeast(1),
            onAcceptPrompt = { promptType, detail ->
                viewModel.acceptSmartPrompt(promptType, detail)
            },
            onDismissPrompt = { promptType ->
                viewModel.dismissSmartPrompt(promptType)
            }
        )

        if (showBusinessHubDialog) {
            GuildBusinessPartnerModal(
                isGuildPassActive = isGuildPassActive,
                isPaidImageApiApproved = isPaidImageApiApproved,
                freeTierCallsUsed = freeTierCallsUsed,
                cacheHitsSaved = cacheHitsSaved,
                paidImageCallsUsed = paidImageCallsUsed,
                donationEngagementState = donationEngagementState,
                onToggleGuildPass = { viewModel.activateGuildPass() },
                onTogglePaidImageApiApproval = { viewModel.togglePaidImageApiApproval(it) },
                onOpenDonationDialog = {
                    showBusinessHubDialog = false
                    currentTab = 4
                },
                onSpawnSponsoredPartner = { name, offer ->
                    viewModel.spawnSponsoredPartnerStop(name, offer)
                    showBusinessHubDialog = false
                    currentTab = 0
                },
                onDismiss = { showBusinessHubDialog = false }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 700.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MidnightAbyss,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = if (isExpandedScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                val animatedXp by animateIntAsState(
                                    targetValue = (profile?.xp ?: 0L).toInt(),
                                    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
                                    label = "xp_counter"
                                )
                                val badgeAccent by animateColorAsState(
                                    targetValue = if (isGuildPassActive) ArcaneEmerald else GuildGold,
                                    animationSpec = tween(durationMillis = 350),
                                    label = "guild_badge_accent"
                                )
                                Surface(
                                    onClick = { showBusinessHubDialog = true },
                                    color = badgeAccent.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, badgeAccent)
                                ) {
                                    Text(
                                        text = if (isExpandedScreen) {
                                            "LVL $level ${rank.title} • $animatedXp XP"
                                        } else {
                                            "LVL $level • $animatedXp XP"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = badgeAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                if (donationEngagementState.isPermanentSupporter && isExpandedScreen) {
                                    Surface(
                                        onClick = { currentTab = 4 },
                                        modifier = Modifier.testTag("active_supporter_header_badge"),
                                        color = ArcaneEmerald.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, ArcaneEmerald)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = ArcaneEmerald,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = stringResource(id = R.string.supporter_active_badge),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ArcaneEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            if (isExpandedScreen) {
                                IconButton(
                                    onClick = { showOnboardingFlow = true },
                                    modifier = Modifier.testTag("header_open_onboarding_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Interactive Onboarding Tour",
                                        tint = CyberCyan
                                    )
                                }
                                IconButton(
                                    onClick = { currentTab = 2 },
                                    modifier = Modifier.testTag("header_open_quest_journal_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HistoryEdu,
                                        contentDescription = stringResource(id = R.string.tab_quest_journal),
                                        tint = if (currentTab == 2) GuildGold else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { currentTab = 4 },
                                    modifier = Modifier.testTag("header_open_settings_support_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings & Support Development",
                                        tint = if (currentTab == 4) GuildGold else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { showBusinessHubDialog = true },
                                    modifier = Modifier.testTag("open_business_hub_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Guild Pass & Partner Hub",
                                        tint = if (isGuildPassActive) ArcaneEmerald else GuildGold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.voiceNarrator.stop()
                                        signOut(credentialManager, scope) {
                                            onSignOut()
                                        }
                                    },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign Out",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { showOnboardingFlow = true },
                                    modifier = Modifier.testTag("header_open_onboarding_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Interactive Onboarding Tour",
                                        tint = CyberCyan
                                    )
                                }
                                IconButton(
                                    onClick = { showBusinessHubDialog = true },
                                    modifier = Modifier.testTag("open_business_hub_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Guild Pass & Partner Hub",
                                        tint = if (isGuildPassActive) ArcaneEmerald else GuildGold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.voiceNarrator.stop()
                                        signOut(credentialManager, scope) {
                                            onSignOut()
                                        }
                                    },
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign Out",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ObsidianSurface
                        )
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        Column {
                            AnimatedVisibility(visible = isSpeaking) {
                                AudioNarratorMiniBar(
                                    title = speakingTitle.ifBlank { selectedStyle.narratorTitle },
                                    narrator = selectedStyle.narratorTitle,
                                    onStop = { viewModel.voiceNarrator.stop() }
                                )
                            }
                            NavigationBar(
                                containerColor = ObsidianSurface
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Explore,
                                            contentDescription = stringResource(id = R.string.tab_map_quest)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Map",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MidnightAbyss,
                                        selectedTextColor = GuildGold,
                                        indicatorColor = GuildGold
                                    ),
                                    modifier = Modifier.testTag("nav_tab_map")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.RecordVoiceOver,
                                            contentDescription = stringResource(id = R.string.tab_dungeon_master)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Dungeon",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MidnightAbyss,
                                        selectedTextColor = GuildGold,
                                        indicatorColor = GuildGold
                                    ),
                                    modifier = Modifier.testTag("nav_tab_dm")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.HistoryEdu,
                                            contentDescription = stringResource(id = R.string.tab_quest_journal)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Journal",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MidnightAbyss,
                                        selectedTextColor = GuildGold,
                                        indicatorColor = GuildGold
                                    ),
                                    modifier = Modifier.testTag("nav_tab_quest_journal")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = stringResource(id = R.string.tab_codex_loot)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "Codex",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MidnightAbyss,
                                        selectedTextColor = GuildGold,
                                        indicatorColor = GuildGold
                                    ),
                                    modifier = Modifier.testTag("nav_tab_codex")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 4,
                                    onClick = { currentTab = 4 },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings & Support"
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(id = R.string.tab_settings),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MidnightAbyss,
                                        selectedTextColor = GuildGold,
                                        indicatorColor = GuildGold
                                    ),
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .testTag("tv_navigation_rail")
                        ) {
                            val railItemColors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MidnightAbyss,
                                selectedTextColor = GuildGold,
                                indicatorColor = GuildGold,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            NavigationRailItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.Explore, contentDescription = "Map") },
                                label = { Text("Map") },
                                colors = railItemColors,
                                modifier = Modifier.testTag("rail_tab_map")
                            )
                            NavigationRailItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "DM") },
                                label = { Text("DM") },
                                colors = railItemColors,
                                modifier = Modifier.testTag("rail_tab_dm")
                            )
                            NavigationRailItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.HistoryEdu, contentDescription = "Quest Journal") },
                                label = { Text("Journal") },
                                colors = railItemColors,
                                modifier = Modifier.testTag("rail_tab_quest_journal")
                            )
                            NavigationRailItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = "Codex") },
                                label = { Text("Codex") },
                                colors = railItemColors,
                                modifier = Modifier.testTag("rail_tab_codex")
                            )
                            NavigationRailItem(
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                colors = railItemColors,
                                modifier = Modifier.testTag("rail_tab_settings")
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isExpandedScreen && isSpeaking) {
                            AudioNarratorMiniBar(
                                title = speakingTitle.ifBlank { selectedStyle.narratorTitle },
                                narrator = selectedStyle.narratorTitle,
                                onStop = { viewModel.voiceNarrator.stop() }
                            )
                        }

                        val activeBanner = rewardBanner ?: aiErrorBanner ?: firestoreError
                        val isError = (aiErrorBanner != null || firestoreError != null) && rewardBanner == null
                        AnimatedVisibility(visible = activeBanner != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isError) {
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                    } else {
                                        ArcaneEmerald.copy(alpha = 0.2f)
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isError) MaterialTheme.colorScheme.error else ArcaneEmerald
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (isError) MaterialTheme.colorScheme.error else ArcaneEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = activeBanner.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissBanners() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss notification",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = !telemetryState.isOnboardingCompleted && !dismissedQuickStartBannerThisSession && currentTab == 0
                        ) {
                            OnboardingQuickStartBanner(
                                onLaunchOnboarding = { showOnboardingFlow = true },
                                onDismiss = {
                                    dismissedQuickStartBannerThisSession = true
                                    viewModel.completeOnboardingTour()
                                }
                            )
                        }

                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith
                                    fadeOut(animationSpec = tween(180))
                            },
                            modifier = Modifier.fillMaxSize(),
                            label = "tab_content_transition"
                        ) { tab ->
                            when (tab) {
                                0 -> QuestMapScreen(
                                    viewModel = viewModel,
                                    onOpenDungeonMasterChat = { currentTab = 1 }
                                )
                                1 -> DungeonMasterChatScreen(viewModel = viewModel)
                                2 -> QuestJournalScreen(
                                    viewModel = viewModel,
                                    onNavigateToMap = { currentTab = 0 }
                                )
                                3 -> CodexInventoryScreen(
                                    viewModel = viewModel,
                                    onOpenDonationDialog = { currentTab = 4 },
                                    onOpenQuestJournal = { currentTab = 2 }
                                )
                                else -> SettingsSupportScreen(
                                    viewModel = viewModel,
                                    onSignOut = {
                                        viewModel.voiceNarrator.stop()
                                        signOut(credentialManager, scope) {
                                            onSignOut()
                                        }
                                    },
                                    onNavigateToMap = { currentTab = 0 },
                                    onReplayOnboarding = { showOnboardingFlow = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuildBusinessPartnerModal(
    isGuildPassActive: Boolean,
    isPaidImageApiApproved: Boolean,
    freeTierCallsUsed: Int,
    cacheHitsSaved: Int,
    paidImageCallsUsed: Int,
    donationEngagementState: DonationEngagementState,
    onToggleGuildPass: () -> Unit,
    onTogglePaidImageApiApproval: (Boolean) -> Unit,
    onOpenDonationDialog: () -> Unit,
    onSpawnSponsoredPartner: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var partnerName by rememberSaveable { mutableStateOf("Starlight Espresso & Books") }
    var partnerOffer by rememberSaveable { mutableStateOf("Free Cold Brew Upgrade for SideQuest Walkers") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 40.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
            border = BorderStroke(1.5.dp, GuildGold)
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = GuildGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "GUILD PASS & AI QUOTA HUB",
                            style = MaterialTheme.typography.titleMedium,
                            color = GuildGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // 0. Smart AI Quota & Zero-Bill Cost Control
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isPaidImageApiApproved) GuildGold.copy(alpha = 0.65f) else ArcaneEmerald
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPaidImageApiApproved) {
                                        "PAID AI IMAGE STUDIO: OPTED IN"
                                    } else {
                                        "ZERO-BILL ECO MODE ($0 API COST GUARD)"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPaidImageApiApproved) GuildGold else ArcaneEmerald
                                )
                                Text(
                                    text = "Smart Cache + OpenStreetMap + On-Device Voice/TTS keep daily exploration at $0 cost.",
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
                                modifier = Modifier.testTag("modal_paid_image_api_switch")
                            )
                        }
                        Text(
                            text = "📊 Session Quota: $cacheHitsSaved Calls Saved by Cache • $freeTierCallsUsed Free-Tier Flash Calls • $paidImageCallsUsed Paid Image Calls",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberCyan
                        )
                    }
                }

                // 1. Consumer Membership (Adventurer's Guild Pass)
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isGuildPassActive) ArcaneEmerald else GuildGold.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isGuildPassActive) "ADVENTURER'S GUILD PASS (ACTIVE)" else "ADVENTURER'S GUILD PASS",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isGuildPassActive) ArcaneEmerald else GuildGold
                        )
                        Text(
                            text = "• 1.5x XP on all Landmarks, Bounties & Vision Scans\n• Offline City Lore & Audio Pack Pre-Caching\n• Daily Walking Streak Shield & Relic Forge Boost",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Button(
                            onClick = onToggleGuildPass,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("modal_toggle_guild_pass_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGuildPassActive) ArcaneEmerald else GuildGold,
                                contentColor = MidnightAbyss
                            )
                        ) {
                            Text(
                                text = if (isGuildPassActive) "Guild Pass Active (1.5x XP)" else "Activate Guild Pass Perks",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // 2. B2B Local Merchant Sponsored Stop Creator
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "LOCAL MERCHANT 'TAVERN STOP' CREATOR",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan
                            )
                        }
                        Text(
                            text = "Simulate how local cafes, bookstores & museums sponsor a quest stop to drive real-world foot traffic:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it },
                            label = { Text("Venue / Cafe Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = SlateCardSurface,
                                focusedContainerColor = MidnightAbyss,
                                unfocusedContainerColor = MidnightAbyss
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = partnerOffer,
                            onValueChange = { partnerOffer = it },
                            label = { Text("Explorer Foot-Traffic Perk") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = SlateCardSurface,
                                focusedContainerColor = MidnightAbyss,
                                unfocusedContainerColor = MidnightAbyss
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { onSpawnSponsoredPartner(partnerName, partnerOffer) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spawn_sponsored_stop_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = MidnightAbyss
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Pin Sponsored Stop to Radar (+Voucher)",
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 3. Viral Explorer Referral Loop
                val shareContext = LocalContext.current
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "INVITE A FELLOW EXPLORER (+250 XP)",
                                style = MaterialTheme.typography.labelLarge,
                                color = GuildGold
                            )
                            Text(
                                text = "Share your Guild Invite so friends start with a 7-Day Guild Pass trial.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join me on SideQuest — turn your daily walk into a live AI Audio RPG! Use Guild Code: SIDEQUEST-VIP"
                                    )
                                }
                                shareContext.startActivity(Intent.createChooser(sendIntent, "Invite Explorer"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GuildGold,
                                contentColor = MidnightAbyss
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Invite",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invite", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // 4. Static Support Independent Development Section (Navigates to Settings -> Support Development)
                StaticSupportDevelopmentSectionCard(
                    engagementState = donationEngagementState,
                    onOpenSupportScreen = onOpenDonationDialog
                )
            }
        }
    }
}

@Composable
private fun AudioNarratorMiniBar(
    title: String,
    narrator: String,
    onStop: () -> Unit
) {
    val eqTransition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by eqTransition.animateFloat(
        initialValue = 6f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by eqTransition.animateFloat(
        initialValue = 18f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by eqTransition.animateFloat(
        initialValue = 8f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = ElevatedSanctum,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    listOf(bar1, bar2, bar3).forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(h.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GuildGold)
                        )
                    }
                }
                Column {
                    Text(
                        text = "LIVE AUDIO NARRATION • $narrator",
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MidnightAbyss)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Voice Narration",
                    tint = GuildGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
