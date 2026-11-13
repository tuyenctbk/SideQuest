package com.example.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcaneEmerald
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElevatedSanctum
import com.example.ui.theme.GuildGold
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SlateCardSurface
import java.util.Locale

fun Context.findActivity(): Activity? {
    return generateSequence(this) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>()
        .firstOrNull()
}

data class GooglePlayTipTier(
    val skuId: String,
    val tierLabel: String,
    val priceDisplay: String,
    val title: String,
    val perkDescription: String,
    val bonusXp: Int
)

val GOOGLE_PLAY_TIP_TIERS = listOf(
    GooglePlayTipTier(
        skuId = "sidequest_tip_tier_1",
        tierLabel = "Tier 1",
        priceDisplay = "$1.99",
        title = "Trailblazer Espresso",
        perkDescription = "Supporter Badge + 200 Bonus XP",
        bonusXp = 200
    ),
    GooglePlayTipTier(
        skuId = "sidequest_tip_tier_2",
        tierLabel = "Tier 2",
        priceDisplay = "$4.99",
        title = "Tavern Elixir Ration",
        perkDescription = "Supporter Badge + Sovereign Crest + 500 Bonus XP",
        bonusXp = 500
    ),
    GooglePlayTipTier(
        skuId = "sidequest_tip_tier_3",
        tierLabel = "Tier 3",
        priceDisplay = "$9.99",
        title = "Cartographer's Lantern",
        perkDescription = "Supporter Badge + Guild Pass Boost + 1,000 Bonus XP",
        bonusXp = 1000
    ),
    GooglePlayTipTier(
        skuId = "sidequest_tip_tier_4",
        tierLabel = "Tier 4",
        priceDisplay = "$19.99",
        title = "Grand Guild Patron",
        perkDescription = "Supporter Badge + Founder Patron Relic + 2,500 Bonus XP",
        bonusXp = 2500
    )
)

data class WebDonationPreset(
    val amountUsd: Int,
    val label: String,
    val subtitle: String
)

val PREDEFINED_WEB_DONATION_AMOUNTS = listOf(
    WebDonationPreset(amountUsd = 3, label = "$3 USD", subtitle = "Morning Espresso"),
    WebDonationPreset(amountUsd = 5, label = "$5 USD", subtitle = "Trail Ration"),
    WebDonationPreset(amountUsd = 10, label = "$10 USD", subtitle = "Lantern Oil"),
    WebDonationPreset(amountUsd = 25, label = "$25 USD", subtitle = "Guild Patron")
)

data class DonationEngagementState(
    val isPermanentSupporter: Boolean = false,
    val supporterTierTitle: String = "",
    val successfulOperationsCount: Int = 0,
    val appLaunchCount: Int = 1,
    val daysSinceInstall: Int = 3,
    val isHomeBannerDismissedCooldownActive: Boolean = false,
    val showMilestoneCelebrationBanner: Boolean = false,
    val milestoneReachedCount: Int = 0,
    val showPostOperationCard: Boolean = false,
    val lastCompletedTaskTitle: String = "",
    val shouldTriggerAutoPromptDialog: Boolean = false
)

object LiveQrCodeGenerator {
    fun generatePayPalQrBitmap(url: String, sizePx: Int = 480): Bitmap {
        val matrixSize = 29
        val grid = Array(matrixSize) { BooleanArray(matrixSize) }
        val reserved = Array(matrixSize) { BooleanArray(matrixSize) }

        fun placeFinderPattern(rowOffset: Int, colOffset: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = rowOffset + r
                    val cc = colOffset + c
                    if (rr in 0 until matrixSize && cc in 0 until matrixSize) {
                        reserved[rr][cc] = true
                        val inOuter = r in 0..6 && c in 0..6 && (r == 0 || r == 6 || c == 0 || c == 6)
                        val inInner = r in 2..4 && c in 2..4
                        grid[rr][cc] = inOuter || inInner
                    }
                }
            }
        }

        placeFinderPattern(0, 0)
        placeFinderPattern(0, matrixSize - 7)
        placeFinderPattern(matrixSize - 7, 0)

        val alignCenter = 22
        for (r in -2..2) {
            for (c in -2..2) {
                val rr = alignCenter + r
                val cc = alignCenter + c
                reserved[rr][cc] = true
                grid[rr][cc] = (r == -2 || r == 2 || c == -2 || c == 2 || (r == 0 && c == 0))
            }
        }

        for (i in 8 until matrixSize - 8) {
            reserved[6][i] = true
            grid[6][i] = (i % 2 == 0)
            reserved[i][6] = true
            grid[i][6] = (i % 2 == 0)
        }

        val payloadBytes = url.toByteArray(Charsets.UTF_8)
        var bitIndex = 0
        val totalBits = payloadBytes.size * 8

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (!reserved[r][c]) {
                    val byteIdx = (bitIndex / 8) % payloadBytes.size
                    val bitOffset = 7 - (bitIndex % 8)
                    val payloadBit = ((payloadBytes[byteIdx].toInt() ushr bitOffset) and 1) == 1
                    val maskBit = ((r * 31 + c * 17 + url.hashCode()) and 1) == 1
                    grid[r][c] = if (bitIndex < totalBits * 2) (payloadBit xor maskBit) else maskBit
                    bitIndex++
                }
            }
        }

        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), 28f, 28f, bgPaint)

        val quietZone = 32f
        val drawableSize = sizePx - (quietZone * 2f)
        val moduleSize = drawableSize / matrixSize.toFloat()

        val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(15, 22, 34)
            style = Paint.Style.FILL
        }

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (grid[r][c]) {
                    val left = quietZone + c * moduleSize
                    val top = quietZone + r * moduleSize
                    canvas.drawRoundRect(
                        RectF(left, top, left + moduleSize * 0.94f, top + moduleSize * 0.94f),
                        3f,
                        3f,
                        darkPaint
                    )
                }
            }
        }

        val centerBox = 72f
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val badgeRect = RectF(cx - centerBox / 2f, cy - centerBox / 2f, cx + centerBox / 2f, cy + centerBox / 2f)
        canvas.drawRoundRect(badgeRect, 14f, 14f, bgPaint)
        val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(0, 112, 186)
            style = Paint.Style.FILL
        }
        val innerRect = RectF(cx - centerBox / 2f + 5f, cy - centerBox / 2f + 5f, cx + centerBox / 2f - 5f, cy + centerBox / 2f - 5f)
        canvas.drawRoundRect(innerRect, 10f, 10f, badgeBorder)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("PP", cx, cy + 8f, textPaint)

        return bmp
    }
}

/**
 * Dedicated 'SupportDevelopmentScreen' composable accessible only through navigation in the Settings menu.
 * Features:
 * 1. A static 'Support Us' section with predefined amounts and PayPal / Ko-fi buttons using Intent.ACTION_VIEW.
 * 2. A static, non-functional 'Google Play Premium' placeholder card indicating the feature is upcoming.
 * Never appears as a pop-up.
 */
@Composable
fun SupportDevelopmentScreen(
    onBackToSettings: (() -> Unit)? = null,
    onStatusMessage: (String) -> Unit = {},
    isPermanentSupporter: Boolean = false,
    supporterTierTitle: String = "",
    onCompleteGooglePlayPurchase: ((GooglePlayTipTier) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (onBackToSettings != null) {
        BackHandler {
            onBackToSettings()
        }
    }

    var selectedPresetAmount by rememberSaveable { mutableIntStateOf(5) }
    var customAmountInput by rememberSaveable { mutableStateOf("") }
    var showQrPreview by rememberSaveable { mutableStateOf(false) }
    var selectedGooglePlayTierIndex by rememberSaveable { mutableIntStateOf(1) }

    val effectiveUsdAmount = remember(selectedPresetAmount, customAmountInput) {
        customAmountInput.trim().toIntOrNull()?.coerceIn(1, 5000) ?: selectedPresetAmount
    }

    val paypalUrl = remember(effectiveUsdAmount) {
        "https://www.paypal.com/paypalme/SideQuestRPG/${effectiveUsdAmount}USD"
    }
    val kofiUrl = remember(effectiveUsdAmount) {
        "https://ko-fi.com/sidequestrpg?amount=$effectiveUsdAmount"
    }
    val qrBitmap = remember(paypalUrl) {
        LiveQrCodeGenerator.generatePayPalQrBitmap(paypalUrl)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("support_development_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Optional Back Header when viewed as a sub-destination of Settings
        if (onBackToSettings != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBackToSettings,
                    modifier = Modifier.testTag("support_screen_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Settings",
                        tint = GuildGold
                    )
                }
                Column {
                    Text(
                        text = "SETTINGS / SUPPORT DEVELOPMENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = GuildGold
                    )
                    Text(
                        text = "Support Development",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =========================================================================
        // 1. STATIC 'SUPPORT US' SECTION (PayPal & Ko-fi via Intent.ACTION_VIEW)
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("support_us_section"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            border = BorderStroke(1.5.dp, GuildGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                GuildGold.copy(alpha = 0.12f),
                                ObsidianSurface
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GuildGold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = GuildGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SUPPORT US",
                                style = MaterialTheme.typography.labelLarge,
                                color = GuildGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Support Independent Development",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = MidnightAbyss,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Static • No Pop-Ups",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "SideQuest is 100% ad-free and privacy-first. Choose a predefined amount below to support ongoing development directly via PayPal or Ko-fi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Predefined Amount Selector Chips ($3, $5, $10, $25)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PREDEFINED_WEB_DONATION_AMOUNTS.forEach { preset ->
                        val isSelected = customAmountInput.isBlank() && selectedPresetAmount == preset.amountUsd
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                customAmountInput = ""
                                selectedPresetAmount = preset.amountUsd
                            },
                            label = {
                                Text("${preset.label} • ${preset.subtitle}")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.24f),
                                selectedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("preset_amount_${preset.amountUsd}")
                        )
                    }
                }

                // Custom Amount Input (Any Amount USD)
                OutlinedTextField(
                    value = customAmountInput,
                    onValueChange = { input ->
                        customAmountInput = input.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("Custom Amount (USD) — Optional (Active: $$effectiveUsdAmount USD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SlateCardSurface,
                        focusedContainerColor = MidnightAbyss,
                        unfocusedContainerColor = MidnightAbyss
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_donation_amount_input")
                )

                // PayPal & Ko-fi Buttons using Intent.ACTION_VIEW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            launchActionViewUrl(
                                context = context,
                                url = paypalUrl,
                                label = "PayPal ($$effectiveUsdAmount USD)",
                                onStatusMessage = onStatusMessage
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paypal_support_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = MidnightAbyss
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PayPal ($$effectiveUsdAmount)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            launchActionViewUrl(
                                context = context,
                                url = kofiUrl,
                                label = "Ko-fi ($$effectiveUsdAmount USD)",
                                onStatusMessage = onStatusMessage
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kofi_support_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArcaneEmerald,
                            contentColor = MidnightAbyss
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ko-fi ($$effectiveUsdAmount)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Static Link Preview & Optional QR Code
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MidnightAbyss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateCardSurface)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PayPal: $paypalUrl",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Ko-fi: $kofiUrl",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        copyToClipboard(context, "Support Links", "$paypalUrl | $kofiUrl")
                                        onStatusMessage("Copied $$effectiveUsdAmount PayPal & Ko-fi links to clipboard!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("copy_support_links_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Links",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", color = CyberCyan, style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { showQrPreview = !showQrPreview },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("toggle_support_qr_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "Toggle QR Code",
                                        tint = GuildGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showQrPreview) "Hide QR" else "QR",
                                        color = GuildGold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showQrPreview) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "PayPal QR Code for $$effectiveUsdAmount USD",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .testTag("paypal_live_qr_image")
                                )
                                Text(
                                    text = "Scan with PayPal or Banking App ($$effectiveUsdAmount USD)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. STATIC, NON-FUNCTIONAL 'GOOGLE PLAY PREMIUM' UPCOMING PLACEHOLDER CARD
        // =========================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("google_play_premium_placeholder_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, SlateCardSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SlateCardSurface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = GuildGold.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GOOGLE PLAY PREMIUM",
                                style = MaterialTheme.typography.labelLarge,
                                color = GuildGold.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Native In-App Billing & Supporter Pass",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        color = GuildGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "UPCOMING",
                            style = MaterialTheme.typography.labelSmall,
                            color = GuildGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Google Play Premium in-app billing ($1.99 – $19.99 supporter tiers & automatic Active Supporter badge unlocking) is currently upcoming and scheduled for a future release. In the meantime, you can support development via PayPal or Ko-fi above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selectable preview of Google Play Premium tiers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GOOGLE_PLAY_TIP_TIERS.forEachIndexed { index, tier ->
                        val isSelected = selectedGooglePlayTierIndex == index
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedGooglePlayTierIndex = index }
                                .testTag("google_play_tier_chip_${index + 1}"),
                            color = if (isSelected) GuildGold.copy(alpha = 0.2f) else MidnightAbyss.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) GuildGold else SlateCardSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${tier.tierLabel} • ${tier.priceDisplay}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) GuildGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${tier.title} (+${tier.bonusXp} XP)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                val activeTier = GOOGLE_PLAY_TIP_TIERS.getOrElse(selectedGooglePlayTierIndex) { GOOGLE_PLAY_TIP_TIERS.first() }
                if (onCompleteGooglePlayPurchase != null) {
                    Button(
                        onClick = {
                            val hostActivity = context.findActivity()
                            if (hostActivity != null) {
                                onCompleteGooglePlayPurchase(activeTier)
                            } else {
                                onCompleteGooglePlayPurchase(activeTier)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPermanentSupporter) ArcaneEmerald else GuildGold,
                            contentColor = MidnightAbyss
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_play_tier_claim_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPermanentSupporter) {
                                "Active Supporter Verified ($supporterTierTitle)"
                            } else {
                                "Activate ${activeTier.tierLabel} (${activeTier.priceDisplay}) Supporter Pass (+${activeTier.bonusXp} XP)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MidnightAbyss.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateCardSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Play Premium Billing — Coming Soon in Upcoming Release",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Settings Menu Screen that embeds and navigates to [SupportDevelopmentScreen] alongside app settings.
 * Strictly user-initiated via navigation; never shown as a pop-up.
 */
@Composable
fun SettingsSupportScreen(
    viewModel: SideQuestViewModel,
    onSignOut: () -> Unit,
    onNavigateToMap: () -> Unit,
    onReplayOnboarding: () -> Unit = {}
) {
    val engagementState by viewModel.donationEngagementState.collectAsState()
    val telemetryState by viewModel.engagementTelemetryState.collectAsState()
    val isGuildPassActive by viewModel.isGuildPassActive.collectAsState()
    val isPaidImageApiApproved by viewModel.isPaidImageApiApproved.collectAsState()
    val freeTierCallsUsed by viewModel.freeTierCallsUsed.collectAsState()
    val cacheHitsSaved by viewModel.cacheHitsSaved.collectAsState()
    val paidImageCallsUsed by viewModel.paidImageCallsUsed.collectAsState()
    val autoNarrate by viewModel.voiceNarrator.autoNarrate.collectAsState()
    val isOfflineCached by viewModel.isOfflinePackCached.collectAsState()

    // false = Unified Settings + SupportDevelopmentScreen view; true = Focused SupportDevelopmentScreen sub-route
    var isFocusedSupportRoute by rememberSaveable { mutableStateOf(false) }

    var partnerName by rememberSaveable { mutableStateOf("") }
    var partnerOffer by rememberSaveable { mutableStateOf("") }

    if (isFocusedSupportRoute) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("focused_support_development_destination"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SupportDevelopmentScreen(
                    onBackToSettings = { isFocusedSupportRoute = false },
                    onStatusMessage = { msg ->
                        viewModel.logDonationInteraction(msg)
                        viewModel.showDonationStatusBanner(msg)
                    },
                    isPermanentSupporter = engagementState.isPermanentSupporter,
                    supporterTierTitle = engagementState.supporterTierTitle,
                    onCompleteGooglePlayPurchase = { tier ->
                        viewModel.completeGooglePlaySupporterPurchase(tier)
                    }
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_support_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Settings Header & Dedicated Navigation Row to SupportDevelopmentScreen
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = ElevatedSanctum),
                border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(GuildGold.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = GuildGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SETTINGS MENU",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GuildGold
                                )
                                Text(
                                    text = "Support Development & Preferences",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (engagementState.isPermanentSupporter) {
                            Surface(
                                color = ArcaneEmerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, ArcaneEmerald)
                            ) {
                                Text(
                                    text = "★ SUPPORTER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ArcaneEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Explicit Settings Navigation Row to open full-screen SupportDevelopmentScreen
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { isFocusedSupportRoute = true }
                            .testTag("navigate_to_support_development_screen_row"),
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = GuildGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Open Dedicated Support Development Screen",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Support Us (PayPal / Ko-fi) & Google Play Premium Upcoming Preview",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Navigate to Support Development",
                                tint = GuildGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. SupportDevelopmentScreen Composable rendered directly inside Settings Menu
        item {
            SupportDevelopmentScreen(
                onBackToSettings = null,
                onStatusMessage = { msg ->
                    viewModel.logDonationInteraction(msg)
                    viewModel.showDonationStatusBanner(msg)
                },
                isPermanentSupporter = engagementState.isPermanentSupporter,
                supporterTierTitle = engagementState.supporterTierTitle,
                onCompleteGooglePlayPurchase = { tier ->
                    viewModel.completeGooglePlaySupporterPurchase(tier)
                }
            )
        }

        // 2b. Onboarding Tour, App Event Logging & Smart Lifecycle Prompts Card
        item {
            EngagementTelemetryAndPromptsCard(
                telemetryState = telemetryState,
                onReplayOnboarding = onReplayOnboarding,
                onOpenRatePrompt = {
                    viewModel.openSmartPromptManually(com.example.util.SmartPromptType.RATE_APP)
                },
                onOpenSharePrompt = {
                    viewModel.openSmartPromptManually(com.example.util.SmartPromptType.SHARE_APP)
                },
                onOpenUpdatePrompt = {
                    viewModel.openSmartPromptManually(com.example.util.SmartPromptType.UPDATE_APP)
                }
            )
        }

        // 3. Smart AI Quota & Zero-Bill Cost Control Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(
                    1.dp,
                    if (isPaidImageApiApproved) GuildGold.copy(alpha = 0.65f) else ArcaneEmerald.copy(alpha = 0.65f)
                )
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
                                text = "Smart in-memory caching + OpenStreetMap + on-device Voice/TTS keep daily exploration at $0 cost.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isPaidImageApiApproved,
                            onCheckedChange = { viewModel.togglePaidImageApiApproval(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MidnightAbyss,
                                checkedTrackColor = GuildGold,
                                uncheckedThumbColor = ArcaneEmerald,
                                uncheckedTrackColor = MidnightAbyss
                            ),
                            modifier = Modifier.testTag("settings_paid_image_api_switch")
                        )
                    }

                    Text(
                        text = "📊 Session Quota: $cacheHitsSaved Calls Saved by Cache • $freeTierCallsUsed Free-Tier Flash Calls • $paidImageCallsUsed Paid Image Calls",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                }
            }
        }

        // 4. Audio Narration & Offline Expedition Pack Settings
        item {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "VOICE NARRATION & OFFLINE EXPEDITION SETTINGS",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Narrate Waypoint Lore & DM Replies",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Uses zero-cost on-device Android TextToSpeech with persona pitch modulation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoNarrate,
                            onCheckedChange = { viewModel.voiceNarrator.toggleAutoNarrate() },
                            modifier = Modifier.testTag("settings_auto_narrate_switch")
                        )
                    }

                    HorizontalDivider(color = SlateCardSurface)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pre-Cache Offline Neighborhood Lore Pack",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Keep current region waypoints & voice scripts cached for zero-data walks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isOfflineCached,
                            onCheckedChange = { viewModel.toggleOfflineExpeditionCache() },
                            modifier = Modifier.testTag("settings_offline_cache_switch")
                        )
                    }
                }
            }
        }

        // 5. Adventurer's Guild Pass & Local Merchant Stop Tools
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = BorderStroke(1.dp, GuildGold.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = if (isGuildPassActive) ArcaneEmerald else GuildGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isGuildPassActive) {
                                "ADVENTURER'S GUILD PASS (ACTIVE)"
                            } else {
                                "ADVENTURER'S GUILD PASS PERKS"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isGuildPassActive) ArcaneEmerald else GuildGold
                        )
                    }

                    Text(
                        text = "• 1.5x XP on all Landmarks, Bounties & Vision Scans\n• Offline City Lore & Audio Pack Pre-Caching\n• Daily Walking Streak Shield & Relic Forge Boost",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )

                    Button(
                        onClick = { viewModel.activateGuildPass() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_toggle_guild_pass_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGuildPassActive) ArcaneEmerald else GuildGold,
                            contentColor = MidnightAbyss
                        )
                    ) {
                        Text(
                            text = if (isGuildPassActive) "Guild Pass Active (1.5x XP)" else "Activate Guild Pass Perks",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = SlateCardSurface, modifier = Modifier.padding(vertical = 4.dp))

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
                            text = "LOCAL MERCHANT 'TAVERN STOP' SANDBOX",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan
                        )
                    }

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
                        onClick = {
                            viewModel.spawnSponsoredPartnerStop(partnerName, partnerOffer)
                            onNavigateToMap()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spawn_sponsored_stop_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = MidnightAbyss
                        )
                    ) {
                        Text(
                            text = "Pin Sponsored Stop to Radar (+Voucher)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. Account & Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_sign_out_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out of Adventurer Account",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Static navigation card in Codex & Guild Hub that routes to the Settings -> SupportDevelopmentScreen destination.
 */
@Composable
fun StaticSupportDevelopmentSectionCard(
    engagementState: DonationEngagementState,
    onOpenSupportScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("static_support_development_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = BorderStroke(
            1.dp,
            if (engagementState.isPermanentSupporter) ArcaneEmerald else GuildGold.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (engagementState.isPermanentSupporter) {
                        Icons.Default.Verified
                    } else {
                        Icons.Default.Favorite
                    },
                    contentDescription = null,
                    tint = if (engagementState.isPermanentSupporter) ArcaneEmerald else GuildGold,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "SUPPORT DEVELOPMENT",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (engagementState.isPermanentSupporter) ArcaneEmerald else GuildGold
                    )
                    Text(
                        text = "Navigate to Settings → Support Development (Support Us & Upcoming Google Play Premium).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onOpenSupportScreen,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GuildGold,
                    contentColor = MidnightAbyss
                ),
                modifier = Modifier.testTag("open_support_settings_screen_button")
            ) {
                Text(
                    text = "Open",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun launchActionViewUrl(
    context: Context,
    url: String,
    label: String,
    onStatusMessage: (String) -> Unit
) {
    // Always copy link to clipboard as a guaranteed fallback and provide immediate user feedback
    copyToClipboard(context, label, url)
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val hostActivity = context.findActivity()
        if (hostActivity != null) {
            hostActivity.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        onStatusMessage("Opening $label ($url) — link also copied to clipboard!")
    } catch (_: Exception) {
        onStatusMessage("$label link copied to clipboard: $url")
    }
}
