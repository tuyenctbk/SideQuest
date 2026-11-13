package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.CharacterStyle
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateCardSurface
import com.example.util.EngagementTelemetryState
import com.example.util.SmartPromptType
import kotlinx.coroutines.launch

/**
 * Interactive 4-Step Onboarding Screen Flow for SideQuest.
 * Lets new and returning explorers preview the real-world radar, pick their AI Narrator persona,
 * learn about Gemini Vision & Zero-Bill Eco Mode, and understand the Cloud Firestore Quest Journal.
 */
@Composable
fun OnboardingFlowScreen(
    selectedStyle: CharacterStyle,
    onSelectStyle: (CharacterStyle) -> Unit,
    onPreviewVoice: (CharacterStyle) -> Unit = {},
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    BackHandler {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        } else {
            onCompleteOnboarding()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_flow_screen"),
        color = MidnightAbyss
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Step Indicator & Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("onboarding_step_indicator")
                ) {
                    repeat(4) { index ->
                        val isCurrent = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isCurrent) 28.dp else 10.dp,
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                            label = "onboarding_dot_width_$index"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isCurrent) GuildGold else SlateCardSurface,
                            animationSpec = tween(durationMillis = 260),
                            label = "onboarding_dot_color_$index"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(RoundedCornerShape(50))
                                .background(dotColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.onboarding_step_of_4, pagerState.currentPage + 1),
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold
                    )
                }

                TextButton(
                    onClick = onCompleteOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_skip_tour),
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (page) {
                        0 -> OnboardingStepOneRadar()
                        1 -> OnboardingStepTwoPersonas(
                            selectedStyle = selectedStyle,
                            onSelectStyle = onSelectStyle,
                            onPreviewVoice = onPreviewVoice
                        )
                        2 -> OnboardingStepThreeAiAndEco()
                        else -> OnboardingStepFourFirestoreJournal()
                    }
                }
            }

            // Bottom Navigation Buttons (Back / Next / Start Questing)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SlateCardSurface),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("onboarding_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous step",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.onboarding_back_btn), color = Color.White)
                    }
                }

                val isLastPage = pagerState.currentPage == 3
                Button(
                    onClick = {
                        if (isLastPage) {
                            onCompleteOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastPage) ArcaneEmerald else GuildGold,
                        contentColor = MidnightAbyss
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag(if (isLastPage) "onboarding_finish_button" else "onboarding_next_button")
                ) {
                    Text(
                        text = if (isLastPage) "Finish & Start Exploring" else "Next Step",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isLastPage) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepOneRadar() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_sidequest_1794444307373),
                contentDescription = "SideQuest Real-World Radar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MidnightAbyss.copy(alpha = 0.9f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                Surface(
                    color = GuildGold.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, GuildGold)
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_step1_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = GuildGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(id = R.string.onboarding_step1_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, SlateCardSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingBulletRow(
                icon = Icons.Default.Explore,
                title = "Live OpenStreetMap & GPS Discovery",
                description = "Scan your real device GPS or search any city worldwide to lock onto real parks, historical plaques, monuments, and coffee shops."
            )
            OnboardingBulletRow(
                icon = Icons.Default.RecordVoiceOver,
                title = "Hands-Free Pocket Dungeon Master",
                description = "Keep your phone in your pocket while walking—spatial proximity cues automatically narrate lore as you approach each landmark."
            )
        }
    }
}

@Composable
private fun OnboardingStepTwoPersonas(
    selectedStyle: CharacterStyle,
    onSelectStyle: (CharacterStyle) -> Unit,
    onPreviewVoice: (CharacterStyle) -> Unit
) {
    Text(
        text = stringResource(id = R.string.onboarding_step2_eyebrow),
        style = MaterialTheme.typography.labelLarge,
        color = CyberCyan
    )
    Text(
        text = stringResource(id = R.string.onboarding_step2_title),
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(id = R.string.onboarding_step2_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    CharacterStyle.entries.forEach { style ->
        val isSelected = style == selectedStyle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectStyle(style) }
                .testTag("onboarding_persona_${style.id}"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) ElevatedSanctum else ObsidianSurface
            ),
            border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) GuildGold else SlateCardSurface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = style.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) GuildGold else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            Surface(
                                color = GuildGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.onboarding_selected_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GuildGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(id = R.string.onboarding_narrator_format, style.narratorTitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                    Text(
                        text = style.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        onSelectStyle(style)
                        onPreviewVoice(style)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Preview ${style.title} Voice",
                        tint = GuildGold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepThreeAiAndEco() {
    Text(
        text = stringResource(id = R.string.onboarding_step3_eyebrow),
        style = MaterialTheme.typography.labelLarge,
        color = ArcaneEmerald
    )
    Text(
        text = stringResource(id = R.string.onboarding_step3_title),
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, ArcaneEmerald.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OnboardingBulletRow(
                icon = Icons.Default.CameraAlt,
                title = "Gemini Multimodal Plaque & Landmark Scanner",
                description = "Snap or pick a photo of any real plaque or monument to verify its history and mint an Epic Chronicle Relic."
            )
            OnboardingBulletRow(
                icon = Icons.Default.AutoAwesome,
                title = "AI Cipher Duels & Grounded Fact Dossiers",
                description = "Challenge the Dungeon Master to 3-option historical riddles or generate architectural fact-check dossiers."
            )
            OnboardingBulletRow(
                icon = Icons.Default.CheckCircle,
                title = "Zero-Bill Eco Mode ($0 Default Cost Guard)",
                description = "In-memory response caching, OpenStreetMap discovery, and on-device TTS keep everyday exploration at $0 API cost."
            )
        }
    }
}

@Composable
private fun OnboardingStepFourFirestoreJournal() {
    Text(
        text = stringResource(id = R.string.onboarding_step4_eyebrow),
        style = MaterialTheme.typography.labelLarge,
        color = GuildGold
    )
    Text(
        text = stringResource(id = R.string.onboarding_step4_title),
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OnboardingBulletRow(
                icon = Icons.Default.HistoryEdu,
                title = "Cloud Firestore Quest Journal",
                description = "Every completed walking quest is permanently recorded with its quest name, neighborhood location, and completion timestamp."
            )
            OnboardingBulletRow(
                icon = Icons.Default.Star,
                title = "Respectful, Non-Intrusive Experience",
                description = "Zero startup pop-ups or ad clutter. Optional support and app feedback are tucked cleanly in Settings or shown only after major milestones."
            )
        }
    }
}

@Composable
private fun OnboardingBulletRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GuildGold.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GuildGold,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact banner shown on the Map screen when the user hasn't taken the interactive 4-step Onboarding Tour yet.
 */
@Composable
fun OnboardingQuickStartBanner(
    onLaunchOnboarding: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("onboarding_quick_start_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(id = R.string.onboarding_banner_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.onboarding_banner_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onLaunchOnboarding,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = MidnightAbyss
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("launch_onboarding_screen_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_start_tour_btn),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss onboarding banner",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Host composable that renders Rate App, Share App, or Update App suggestion dialogs
 * ONLY when triggered by the smart engagement algorithm after a positive value moment
 * (or when manually opened from Settings). Never triggers at app startup.
 */
@Composable
fun SmartLifecyclePromptHost(
    telemetryState: EngagementTelemetryState,
    adventurerName: String,
    completedQuestsCount: Int,
    onAcceptPrompt: (SmartPromptType, String) -> Unit,
    onDismissPrompt: (SmartPromptType) -> Unit
) {
    val context = LocalContext.current
    when (val activePrompt = telemetryState.activeAutoPrompt) {
        SmartPromptType.NONE -> Unit
        SmartPromptType.RATE_APP -> {
            RateAppPromptDialog(
                onSubmitRating = { stars, feedback ->
                    openPlayStoreForApp(context)
                    onAcceptPrompt(
                        SmartPromptType.RATE_APP,
                        "Rated $stars/5 stars (${feedback.ifBlank { "No comment" }})"
                    )
                },
                onDismiss = { onDismissPrompt(SmartPromptType.RATE_APP) }
            )
        }
        SmartPromptType.SHARE_APP -> {
            ShareAppPromptDialog(
                adventurerName = adventurerName,
                completedQuestsCount = completedQuestsCount,
                onShareNow = {
                    launchShareAppIntent(context, adventurerName, completedQuestsCount)
                    onAcceptPrompt(SmartPromptType.SHARE_APP, "Shared app invite sheet")
                },
                onDismiss = { onDismissPrompt(SmartPromptType.SHARE_APP) }
            )
        }
        SmartPromptType.UPDATE_APP -> {
            UpdateAppPromptDialog(
                installedVersionName = telemetryState.installedVersionName,
                availableVersionName = telemetryState.availableUpdateVersionName,
                onUpdateNow = {
                    openPlayStoreForApp(context)
                    onAcceptPrompt(
                        SmartPromptType.UPDATE_APP,
                        "Opened Google Play update for v${telemetryState.availableUpdateVersionName}"
                    )
                },
                onDismiss = { onDismissPrompt(SmartPromptType.UPDATE_APP) }
            )
        }
    }
}

@Composable
private fun RateAppPromptDialog(
    onSubmitRating: (stars: Int, feedback: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStars by rememberSaveable { mutableIntStateOf(5) }
    var feedbackNote by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 48.dp)
                .testTag("rate_app_prompt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
            border = BorderStroke(1.5.dp, GuildGold)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = GuildGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, GuildGold)
                ) {
                    Text(
                        text = stringResource(id = R.string.prompt_milestone_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = GuildGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.prompt_rate_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.prompt_rate_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        IconButton(
                            onClick = { selectedStars = star },
                            modifier = Modifier.testTag("rate_star_$star")
                        ) {
                            Icon(
                                imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star Stars",
                                tint = GuildGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = feedbackNote,
                    onValueChange = { feedbackNote = it },
                    label = { Text(stringResource(id = R.string.prompt_optional_note_label)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GuildGold,
                        unfocusedBorderColor = SlateCardSurface,
                        focusedContainerColor = MidnightAbyss,
                        unfocusedContainerColor = MidnightAbyss
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateCardSurface),
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("dismiss_rate_app_button")
                    ) {
                        Text(stringResource(id = R.string.prompt_later_btn), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }

                    Button(
                        onClick = { onSubmitRating(selectedStars, feedbackNote) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GuildGold,
                            contentColor = MidnightAbyss
                        ),
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("confirm_rate_app_button")
                    ) {
                        Text(
                            text = stringResource(id = R.string.prompt_rate_submit_format, selectedStars),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareAppPromptDialog(
    adventurerName: String,
    completedQuestsCount: Int,
    onShareNow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 48.dp)
                .testTag("share_app_prompt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
            border = BorderStroke(1.5.dp, CyberCyan)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.prompt_share_heading),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.prompt_share_body_format, adventurerName, completedQuestsCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateCardSurface),
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("dismiss_share_app_button")
                    ) {
                        Text(stringResource(id = R.string.prompt_later_btn), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }

                    Button(
                        onClick = onShareNow,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = MidnightAbyss
                        ),
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("confirm_share_app_button")
                    ) {
                        Text(stringResource(id = R.string.prompt_share_xp_btn), fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateAppPromptDialog(
    installedVersionName: String,
    availableVersionName: String,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 48.dp)
                .testTag("update_app_prompt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
            border = BorderStroke(1.5.dp, ArcaneEmerald)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ArcaneEmerald.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = ArcaneEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = stringResource(id = R.string.prompt_update_title_format, availableVersionName),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.prompt_update_body_format, installedVersionName, availableVersionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateCardSurface),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dismiss_update_app_button")
                    ) {
                        Text(stringResource(id = R.string.prompt_later_7d_btn), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = onUpdateNow,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArcaneEmerald,
                            contentColor = MidnightAbyss
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_update_app_button")
                    ) {
                        Text(stringResource(id = R.string.prompt_update_now_btn), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Settings Card showing the Smart Auto-Pop-Up Algorithm Status, Onboarding Replay button,
 * Manual Rate/Share/Update triggers, and Live App Event Tracking Log.
 */
@Composable
fun EngagementTelemetryAndPromptsCard(
    telemetryState: EngagementTelemetryState,
    onReplayOnboarding: () -> Unit,
    onOpenRatePrompt: () -> Unit,
    onOpenSharePrompt: () -> Unit,
    onOpenUpdatePrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEventLog by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_engagement_telemetry_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.lifecycle_section_eyebrow),
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan
                        )
                        Text(
                            text = stringResource(id = R.string.lifecycle_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onReplayOnboarding,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GuildGold),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("settings_open_onboarding_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.lifecycle_tour_btn),
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold
                    )
                }
            }

            // Algorithm Status Summary
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
                        text = stringResource(id = R.string.lifecycle_decision_format, telemetryState.lastDecisionReason),
                        style = MaterialTheme.typography.labelMedium,
                        color = ArcaneEmerald
                    )
                    Text(
                        text = "• Startup Guard: Active (0 pop-ups at launch)\n" +
                            "• Session Cap: ${telemetryState.promptsShownThisSession}/1 shown this session\n" +
                            "• Value Milestones: ${telemetryState.positiveValueMoments} (Satisfaction Score: ${telemetryState.satisfactionScore})\n" +
                            "• Cooldowns: Rate=21d (Rated=${telemetryState.hasRatedApp}) • Share=14d • Update=7d",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Manual Actions for Rate App, Share App, Update App
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenRatePrompt,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_open_rate_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GuildGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.lifecycle_rate_btn), color = GuildGold, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onOpenSharePrompt,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_open_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.lifecycle_share_btn), color = CyberCyan, style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onOpenUpdatePrompt,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ArcaneEmerald.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_open_update_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = ArcaneEmerald,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.lifecycle_update_btn), color = ArcaneEmerald, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Expandable Tracked App Event Log
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.lifecycle_tracked_events_format, telemetryState.eventHistory.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { showEventLog = !showEventLog },
                    modifier = Modifier.testTag("toggle_app_event_log_button")
                ) {
                    Text(
                        text = if (showEventLog) "Hide Event Log" else "View Event Log",
                        color = CyberCyan,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            AnimatedVisibility(visible = showEventLog) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    telemetryState.eventHistory.take(8).forEach { event ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ElevatedSanctum,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SlateCardSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = event.type.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GuildGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = event.formattedTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = event.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openPlayStoreForApp(context: Context) {
    val pkg = context.packageName
    val hostActivity = context.findActivity()
    try {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
        if (hostActivity != null) {
            hostActivity.startActivity(marketIntent)
        } else {
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(marketIntent)
        }
    } catch (_: Exception) {
        try {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
            )
            if (hostActivity != null) {
                hostActivity.startActivity(webIntent)
            } else {
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            }
        } catch (_: Exception) {
        }
    }
}

private fun launchShareAppIntent(
    context: Context,
    adventurerName: String,
    completedQuestsCount: Int
) {
    val shareText = "🧭 Join me ($adventurerName) on SideQuest — the real-world Audio RPG! " +
        "I've completed $completedQuestsCount walking quests across local parks, plaques, and coffee shops. " +
        "https://play.google.com/store/apps/details?id=${context.packageName}"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    try {
        val chooser = Intent.createChooser(sendIntent, "Share SideQuest")
        val hostActivity = context.findActivity()
        if (hostActivity != null) {
            hostActivity.startActivity(chooser)
        } else {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    } catch (_: Exception) {
    }
}
