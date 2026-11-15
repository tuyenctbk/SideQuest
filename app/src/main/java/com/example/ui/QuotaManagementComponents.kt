package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.AiValueReceipt
import com.example.model.SmartQuotaRecoveryState
import com.example.model.UsageQuotaLedger
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateCardSurface
import com.example.ui.theme.VictorianAmber
import com.example.util.AiModelConfig.AiCapability

/**
 * Stage 1: Soft Nudge Banner (Playbook Section 3).
 * Appears when the user reaches >= 80% of their daily standard AI allowance.
 * Provides transparent status with an immediate self-serve action to enable Eco Lite Mode.
 */
@Composable
fun QuotaSoftNudgeBanner(
    ledger: UsageQuotaLedger,
    onToggleEcoLite: (Boolean) -> Unit,
    onOpenRecoverySheet: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (ledger.ecoLiteMode) {
        // Compact Eco Lite Indicator Pill
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("eco_lite_active_pill"),
            color = ArcaneEmerald.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ArcaneEmerald.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = ArcaneEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.quota_eco_lite_active_pill),
                        style = MaterialTheme.typography.labelMedium,
                        color = ArcaneEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = stringResource(id = R.string.quota_left_format, ledger.remainingCredits),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
        return
    }

    if (!ledger.isNearLimit) return

    val usagePercent = (ledger.usageRatio * 100).toInt().coerceAtMost(99)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("quota_soft_nudge_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VictorianAmber.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, VictorianAmber.copy(alpha = 0.85f))
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(VictorianAmber.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = VictorianAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(
                            id = R.string.quota_soft_nudge_format,
                            usagePercent,
                            ledger.remainingCredits,
                            ledger.resetsInFormatted
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.quota_eco_nudge_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = VictorianAmber
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { onToggleEcoLite(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VictorianAmber,
                        contentColor = MidnightAbyss
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("switch_to_eco_lite_btn")
                ) {
                    Text(
                        text = stringResource(id = R.string.quota_eco_lite_short),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.action_dismiss),
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Stage 2: Smart Quota Recovery Dialog (Playbook Section 3).
 * Renders the friendly Smart Quota Recovery Card when rate limited (429) or at 100% limit.
 * Presents 4 self-serve recovery actions in order of friction:
 * 1. Claim One-Time Grace Credits (+3 Free Credits)
 * 2. Retry with Eco Lite AI (Fast & Quota-Friendly)
 * 3. Use Zero-Quota Local Alternative (Honor Pledge / Local Reflex / Canvas Emblem / Raw Log)
 * 4. Visible Live Reset Countdown
 */
@Composable
fun SmartQuotaRecoveryDialog(
    recoveryState: SmartQuotaRecoveryState,
    onClaimGraceCredits: () -> Unit,
    onActivateEcoLite: () -> Unit,
    onUseZeroQuotaAlternative: (AiCapability) -> Unit,
    onOpenUpgradeSheet: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!recoveryState.isVisible) return

    val ledger = recoveryState.ledger
    val capability = recoveryState.triggeringCapability

    val localAltTitle = when (capability) {
        AiCapability.VISION_PROOF -> stringResource(id = R.string.quota_alt_honor_title)
        AiCapability.COMPANION_CHAT -> stringResource(id = R.string.quota_alt_reflex_title)
        AiCapability.IMAGE_STUDIO -> stringResource(id = R.string.quota_alt_canvas_title)
        AiCapability.AUDIO_FIELD_NOTE -> stringResource(id = R.string.quota_alt_audio_title)
        else -> stringResource(id = R.string.quota_alt_default_title)
    }

    val localAltDesc = when (capability) {
        AiCapability.VISION_PROOF -> stringResource(id = R.string.quota_alt_honor_desc)
        AiCapability.COMPANION_CHAT -> stringResource(id = R.string.quota_alt_reflex_desc)
        AiCapability.IMAGE_STUDIO -> stringResource(id = R.string.quota_alt_canvas_desc)
        AiCapability.AUDIO_FIELD_NOTE -> stringResource(id = R.string.quota_alt_audio_desc)
        else -> stringResource(id = R.string.quota_alt_default_desc)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .testTag("smart_quota_recovery_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.5.dp, if (recoveryState.isRateLimited429) VictorianAmber else GuildGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (recoveryState.isRateLimited429) VictorianAmber.copy(alpha = 0.2f)
                                    else GuildGold.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (recoveryState.isRateLimited429) Icons.Default.HourglassTop else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (recoveryState.isRateLimited429) VictorianAmber else GuildGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(
                                    id = if (recoveryState.isRateLimited429) R.string.quota_header_429 else R.string.quota_header_smart
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (recoveryState.isRateLimited429) VictorianAmber else CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.quota_header_sub),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.action_close),
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = stringResource(
                        id = if (recoveryState.isRateLimited429) R.string.quota_desc_429 else R.string.quota_desc_exhausted
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = SlateCardSurface)

                // Suggestion 1: Claim One-Time Grace Credits
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
                    border = BorderStroke(1.dp, if (!ledger.graceClaimed) ArcaneEmerald else SlateCardSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loyalty,
                                contentDescription = null,
                                tint = if (!ledger.graceClaimed) ArcaneEmerald else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.quota_step1_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!ledger.graceClaimed) ArcaneEmerald else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.quota_suggestion_grace_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onClaimGraceCredits,
                            enabled = !ledger.graceClaimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArcaneEmerald,
                                contentColor = MidnightAbyss
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("claim_grace_credits_button")
                        ) {
                            Text(
                                text = stringResource(
                                    id = if (!ledger.graceClaimed) R.string.quota_step1_btn_claim else R.string.quota_step1_btn_claimed
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Suggestion 2: Retry with Eco Lite AI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.quota_step2_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.quota_step2_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onActivateEcoLite,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = MidnightAbyss
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("retry_eco_lite_button")
                        ) {
                            Text(
                                text = stringResource(id = R.string.quota_step2_btn),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Suggestion 3: Use Zero-Quota Local Alternative
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
                    border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GuildGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.quota_step3_title_format, localAltTitle),
                                style = MaterialTheme.typography.labelLarge,
                                color = GuildGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = localAltDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onUseZeroQuotaAlternative(capability) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GuildGold,
                                contentColor = MidnightAbyss
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("use_zero_quota_alternative_button")
                        ) {
                            Text(
                                text = stringResource(id = R.string.quota_step3_btn),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Suggestion 4: Visible Reset Countdown
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateCardSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            tint = VictorianAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(id = R.string.quota_step4_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(id = R.string.quota_suggestion_wait_format, ledger.resetsInFormatted),
                                style = MaterialTheme.typography.bodySmall,
                                color = VictorianAmber
                            )
                        }
                    }
                }

                // Personal Value Receipt Link & Upgrade Option
                OutlinedButton(
                    onClick = onOpenUpgradeSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_value_receipt_upgrade_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateCardSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = GuildGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.quota_open_receipt_btn),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Stage 4: Personal "Value Receipt" & Flexible Upgrade Sheet (Playbook Section 4).
 * Displays real stats so users see the clear value they've already received before upgrading.
 */
@Composable
fun AiValueReceiptDialog(
    receipt: AiValueReceipt,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .testTag("ai_value_receipt_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.5.dp, GuildGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.quota_value_receipt_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = GuildGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.quota_value_receipt_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(id = R.string.action_close), tint = Color.White)
                    }
                }

                // Status Badge
                Surface(
                    color = ArcaneEmerald.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ArcaneEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = ArcaneEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.quota_starter_tier_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = ArcaneEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Real Value Receipt Grid
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = ElevatedSanctum,
                    border = BorderStroke(1.dp, SlateCardSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ValueStatRow(
                            icon = Icons.Default.Verified,
                            label = stringResource(id = R.string.quota_stat_landmarks_label),
                            value = stringResource(id = R.string.quota_stat_landmarks_val, receipt.landmarksVerified)
                        )
                        ValueStatRow(
                            icon = Icons.Default.AutoAwesome,
                            label = stringResource(id = R.string.quota_stat_chats_label),
                            value = stringResource(id = R.string.quota_stat_chats_val, receipt.dmChatsExchanged)
                        )
                        ValueStatRow(
                            icon = Icons.Default.HistoryEdu,
                            label = stringResource(id = R.string.quota_stat_relics_label),
                            value = stringResource(id = R.string.quota_stat_relics_val, receipt.relicsCrafted)
                        )
                        ValueStatRow(
                            icon = Icons.Default.DirectionsWalk,
                            label = stringResource(id = R.string.quota_stat_walk_label),
                            value = stringResource(id = R.string.quota_stat_walk_val, receipt.distanceMetersWalked, receipt.questsCompleted)
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.quota_upgrade_paths_header),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                // Zero-Dollar Conversion: Google Sign-In Cloud Sync & Backup
                UpgradeOptionCard(
                    title = stringResource(id = R.string.quota_sync_title),
                    subtitle = stringResource(id = R.string.quota_sync_desc),
                    badge = stringResource(id = R.string.quota_badge_free_unlock),
                    accentColor = ArcaneEmerald
                )

                // Option 1: Occasional Users (Micro-Pack Top-Up)
                UpgradeOptionCard(
                    title = stringResource(id = R.string.quota_micropack_title),
                    subtitle = stringResource(id = R.string.quota_micropack_desc),
                    badge = stringResource(id = R.string.quota_badge_non_expiring),
                    accentColor = CyberCyan
                )

                // Option 2: Daily Power Walkers (Pro Subscription)
                UpgradeOptionCard(
                    title = stringResource(id = R.string.quota_pro_title),
                    subtitle = stringResource(id = R.string.quota_pro_desc),
                    badge = stringResource(id = R.string.quota_badge_best_value),
                    accentColor = GuildGold
                )

                // Option 3: Technical / Developer Users (BYOK)
                UpgradeOptionCard(
                    title = stringResource(id = R.string.quota_byok_title),
                    subtitle = stringResource(id = R.string.quota_byok_desc),
                    badge = stringResource(id = R.string.quota_badge_byok),
                    accentColor = ArcaneEmerald
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateCardSurface,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.quota_close_receipt_btn))
                }
            }
        }
    }
}

@Composable
private fun ValueStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
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
            Icon(imageVector = icon, contentDescription = null, tint = GuildGold, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = CyberCyan,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UpgradeOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
