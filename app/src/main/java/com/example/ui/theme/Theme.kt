package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.CharacterStyle

private fun colorSchemeForStyle(style: CharacterStyle) = darkColorScheme(
    primary = when (style) {
        CharacterStyle.CYBERPUNK_SCOUT -> CyberCyan
        CharacterStyle.VICTORIAN_HISTORIAN -> VictorianAmber
        CharacterStyle.ARCANE_CARTOGRAPHER -> ArcaneEmerald
        CharacterStyle.NOIR_DETECTIVE -> GuildGold
    },
    onPrimary = MidnightAbyss,
    primaryContainer = ElevatedSanctum,
    onPrimaryContainer = ParchmentWhite,
    secondary = GuildGold,
    onSecondary = MidnightAbyss,
    secondaryContainer = SlateCardSurface,
    onSecondaryContainer = GuildGoldSoft,
    tertiary = when (style) {
        CharacterStyle.CYBERPUNK_SCOUT -> GuildGold
        CharacterStyle.VICTORIAN_HISTORIAN -> CyberCyan
        CharacterStyle.ARCANE_CARTOGRAPHER -> GuildGold
        CharacterStyle.NOIR_DETECTIVE -> NoirCrimson
    },
    background = MidnightAbyss,
    onBackground = ParchmentWhite,
    surface = ObsidianSurface,
    onSurface = ParchmentWhite,
    surfaceVariant = SlateCardSurface,
    onSurfaceVariant = MistSilver,
    outline = MutedRuneGray,
    error = DangerRuby,
    onError = Color.Black
)

@Composable
fun SideQuestTheme(
    characterStyle: CharacterStyle = CharacterStyle.CYBERPUNK_SCOUT,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorSchemeForStyle(characterStyle),
        typography = Typography,
        content = content
    )
}
