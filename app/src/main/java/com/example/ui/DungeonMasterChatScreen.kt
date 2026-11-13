package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DungeonMasterAiRepository
import com.example.model.AiLandmarkRiddle
import com.example.model.ChatMessage
import com.example.model.LandmarkFactDossier
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateCardSurface
import java.util.Locale

@Composable
fun DungeonMasterChatScreen(
    viewModel: SideQuestViewModel
) {
    val style by viewModel.selectedStyle.collectAsState()
    val landmarks by viewModel.landmarks.collectAsState()
    val selectedIndex by viewModel.selectedLandmarkIndex.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val isStreaming by viewModel.isDungeonMasterStreaming.collectAsState()
    val isScanningVision by viewModel.isScanningVision.collectAsState()
    val isSpeaking by viewModel.voiceNarrator.isSpeaking.collectAsState()

    val isGeneratingFactDossier by viewModel.isGeneratingFactDossier.collectAsState()
    val activeFactDossier by viewModel.activeFactDossier.collectAsState()
    val isGeneratingRiddle by viewModel.isGeneratingRiddle.collectAsState()
    val activeAiRiddle by viewModel.activeAiRiddle.collectAsState()
    val riddleAnswerStatus by viewModel.riddleAnswerStatus.collectAsState()
    val isPolishingVoiceNote by viewModel.isPolishingVoiceNote.collectAsState()

    val activeLandmark = landmarks.getOrNull(selectedIndex) ?: landmarks.firstOrNull()
    var inputText by rememberSaveable { mutableStateOf("") }
    var showVoiceFieldLogPanel by rememberSaveable { mutableStateOf(false) }
    var voiceFieldLogDraft by rememberSaveable {
        mutableStateOf("Spotted ornate 19th-century ironwork and weathered brass dates beside the main arch.")
    }
    var speechTargetMode by rememberSaveable { mutableStateOf("CHAT") }
    val listState = rememberLazyListState()

    // Live Speech-to-Text Recognizer Launcher for Voice Conversation & Voice Field Log Transcription
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val transcript = spokenList?.firstOrNull()?.trim().orEmpty()
            if (transcript.isNotBlank()) {
                if (speechTargetMode == "FIELD_LOG") {
                    voiceFieldLogDraft = transcript
                    viewModel.transcribeAndMintVoiceFieldLog(transcript)
                } else {
                    inputText = transcript
                    viewModel.sendChatMessageToDungeonMaster(transcript)
                    inputText = ""
                }
            }
        }
    }

    fun launchVoiceRecognition(mode: String) {
        speechTargetMode = mode
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (mode == "FIELD_LOG") {
                    "Speak your explorer field observation for Gemini transcription..."
                } else {
                    "Speak to ${style.narratorTitle}..."
                }
            )
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (_: Exception) {
            // Fallback in cloud streaming emulator when system speech activity is absent
            if (mode == "FIELD_LOG") {
                showVoiceFieldLogPanel = true
            } else {
                val sampleVoice = "Tell me the hidden historical backstory of ${activeLandmark?.name ?: "this landmark"} and what my ${style.title} senses."
                viewModel.sendChatMessageToDungeonMaster(sampleVoice)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val quickPrompts = listOf(
        "Tell me the real history of ${activeLandmark?.name ?: "this area"}",
        "What hidden secret does my ${style.title} sense here?",
        "Give me a field riddle about ${activeLandmark?.name ?: "this landmark"}",
        "What should I look for on the plaque or facade?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dungeon_master_chat_screen")
    ) {
        // 1. Active Landmark & Narrator Context Header + AI Power Tools Strip
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = style.narratorTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Anchored at: ${activeLandmark?.name ?: "Local Sector"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = GuildGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (activeLandmark != null) {
                            IconButton(
                                onClick = {
                                    if (!isScanningVision) {
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
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.16f))
                                    .testTag("chat_vision_scan_button")
                            ) {
                                if (isScanningVision) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = CyberCyan,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Gemini Vision Plaque Scan",
                                        tint = CyberCyan
                                    )
                                }
                            }
                        }

                        if (isSpeaking) {
                            IconButton(
                                onClick = { viewModel.voiceNarrator.stop() },
                                modifier = Modifier.testTag("stop_tts_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Voice",
                                    tint = GuildGold
                                )
                            }
                        }
                    }
                }

                // AI Power Action Chips: Fact-Check Dossier, AI Riddle Duel, & Voice Field Log
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { viewModel.requestLandmarkFactCheckDossier() },
                        enabled = !isGeneratingFactDossier,
                        label = {
                            Text(
                                text = if (isGeneratingFactDossier) "Verifying..." else "Fact-Check (+110 XP)",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = CyberCyan.copy(alpha = 0.15f),
                            labelColor = CyberCyan,
                            leadingIconContentColor = CyberCyan
                        ),
                        modifier = Modifier.testTag("ai_fact_check_dossier_chip")
                    )

                    AssistChip(
                        onClick = { viewModel.requestAiLandmarkRiddle() },
                        enabled = !isGeneratingRiddle,
                        label = {
                            Text(
                                text = if (isGeneratingRiddle) "Crafting..." else "Riddle Duel (+175 XP)",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = GuildGold.copy(alpha = 0.16f),
                            labelColor = GuildGold,
                            leadingIconContentColor = GuildGold
                        ),
                        modifier = Modifier.testTag("ai_riddle_duel_chip")
                    )

                    AssistChip(
                        onClick = { showVoiceFieldLogPanel = !showVoiceFieldLogPanel },
                        label = {
                            Text(
                                text = "Voice Log (+140 XP)",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ArcaneEmerald.copy(alpha = 0.16f),
                            labelColor = ArcaneEmerald,
                            leadingIconContentColor = ArcaneEmerald
                        ),
                        modifier = Modifier.testTag("toggle_voice_field_log_chip")
                    )
                }
            }
        }

        // 2. Collapsible Voice Field-Log Transcriber & Gemini Polisher Panel
        AnimatedVisibility(visible = showVoiceFieldLogPanel) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
                border = BorderStroke(1.dp, ArcaneEmerald.copy(alpha = 0.6f))
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
                        Text(
                            text = "LIVE VOICE FIELD-LOG TRANSCRIBER (GEMINI AI)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ArcaneEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showVoiceFieldLogPanel = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Voice Log",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = voiceFieldLogDraft,
                        onValueChange = { voiceFieldLogDraft = it },
                        label = { Text("Spoken / Dictated Field Observation") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_field_log_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { launchVoiceRecognition("FIELD_LOG") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ArcaneEmerald)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = ArcaneEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dictate Voice", color = ArcaneEmerald)
                        }

                        Button(
                            onClick = {
                                viewModel.transcribeAndMintVoiceFieldLog(voiceFieldLogDraft)
                                showVoiceFieldLogPanel = false
                            },
                            enabled = !isPolishingVoiceNote && voiceFieldLogDraft.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mint_voice_field_log_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArcaneEmerald,
                                contentColor = MidnightAbyss
                            )
                        ) {
                            Text(
                                text = if (isPolishingVoiceNote) "Polishing..." else "Mint Chronicle (+140 XP)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Interactive AI Landmark Riddle & Cipher Duel Card (when active)
        activeAiRiddle?.let { riddle ->
            AiLandmarkRiddleDuelCard(
                riddle = riddle,
                answerStatus = riddleAnswerStatus,
                onSelectOption = { idx -> viewModel.submitAiRiddleAnswer(idx) },
                onDismiss = { viewModel.dismissAiRiddle() }
            )
        }

        // 4. Interactive Grounded Fact-Check Dossier Card (when active)
        activeFactDossier?.let { dossier ->
            LandmarkFactDossierCard(
                dossier = dossier,
                onDismiss = { viewModel.dismissFactDossier() }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Message History List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    onSpeakMessage = {
                        viewModel.voiceNarrator.speak(message.text, style)
                    }
                )
            }
        }

        // 6. Quick Grounded Prompt Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEachIndexed { idx, prompt ->
                AssistChip(
                    onClick = {
                        if (!isStreaming) {
                            viewModel.sendChatMessageToDungeonMaster(prompt)
                        }
                    },
                    label = {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = ObsidianSurface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("quick_dm_prompt_$idx")
                )
            }
        }

        // 7. Chat Input Bar + Hands-Free Voice Microphone Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { launchVoiceRecognition("CHAT") },
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElevatedSanctum)
                    .testTag("dm_voice_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak to Dungeon Master",
                    tint = CyberCyan
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Ask ${style.narratorTitle} or tap mic to speak...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dm_chat_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank() && !isStreaming) {
                            val toSend = inputText
                            inputText = ""
                            viewModel.sendChatMessageToDungeonMaster(toSend)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = SlateCardSurface,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface
                )
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isStreaming) {
                        val toSend = inputText
                        inputText = ""
                        viewModel.sendChatMessageToDungeonMaster(toSend)
                    }
                },
                enabled = inputText.isNotBlank() && !isStreaming,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (inputText.isNotBlank() && !isStreaming) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            SlateCardSurface
                        }
                    )
                    .testTag("dm_send_button")
            ) {
                if (isStreaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = GuildGold,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send to Dungeon Master",
                        tint = if (inputText.isNotBlank()) MidnightAbyss else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AiLandmarkRiddleDuelCard(
    riddle: AiLandmarkRiddle,
    answerStatus: String?,
    onSelectOption: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("ai_riddle_duel_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
        border = BorderStroke(1.5.dp, GuildGold)
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
                Text(
                    text = "🧩 AI LANDMARK RIDDLE DUEL • ${riddle.landmarkName.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = GuildGold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Riddle",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = riddle.question,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            riddle.options.forEachIndexed { idx, option ->
                OutlinedButton(
                    onClick = { onSelectOption(idx) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("riddle_option_$idx"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "${'A' + idx}. $option",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!answerStatus.isNullOrBlank()) {
                val solved = answerStatus.startsWith("CORRECT")
                Surface(
                    color = if (solved) ArcaneEmerald.copy(alpha = 0.18f) else CyberCyan.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (solved) ArcaneEmerald else CyberCyan)
                ) {
                    Text(
                        text = answerStatus,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (solved) ArcaneEmerald else CyberCyan,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LandmarkFactDossierCard(
    dossier: LandmarkFactDossier,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("landmark_fact_dossier_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
        border = BorderStroke(1.5.dp, CyberCyan)
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
                Text(
                    text = "🏛️ VERIFIED FACT-CHECK DOSSIER • ${dossier.verifiedEra.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Dossier",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "${dossier.landmarkName} (${dossier.architecturalStyle})",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            dossier.verifiedHistoricalFacts.forEach { fact ->
                Text(
                    text = "• $fact",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "🔍 Walker Tip: ${dossier.localInsiderTip}",
                style = MaterialTheme.typography.labelMedium,
                color = GuildGold
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onSpeakMessage: () -> Unit
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleColor = when {
        message.isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        message.isVisionScan -> ObsidianSurface
        else -> ElevatedSanctum
    }
    val borderColor = when {
        message.isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        message.isVisionScan -> ArcaneEmerald
        else -> GuildGold.copy(alpha = 0.35f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
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
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            message.isUser -> MaterialTheme.colorScheme.primary
                            message.isVisionScan -> ArcaneEmerald
                            else -> GuildGold
                        },
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!message.isUser) {
                        IconButton(
                            onClick = onSpeakMessage,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Narrate message aloud",
                                tint = GuildGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                if (!message.landmarkName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (message.isVisionScan) {
                                "Multimodal Gemini Vision • ${message.landmarkName}"
                            } else {
                                "Grounded at ${message.landmarkName}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (message.isVisionScan) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
