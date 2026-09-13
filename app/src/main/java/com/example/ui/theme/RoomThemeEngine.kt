package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.RoomBackgroundEffect
import com.example.data.model.RoomThemeConfig

object RoomThemeConstants {
  const val MIDNIGHT_AURORA = "midnight_aurora"
  const val SUNSET_LOUNGE = "sunset_lounge"
  const val EMERALD_NIGHT = "emerald_night"
  const val CYBER_VIOLET = "cyber_violet"
  const val COSMIC_GOLD = "cosmic_gold"
}

data class RoomThemePreset(
  val id: String,
  val name: String,
  val subtitle: String,
  val requiredLevel: Int,
  val primaryAccent: Color,
  val secondaryAccent: Color,
  val bgColors: List<Color>,
  val surfaceGlass: Color,
  val cardBorder: Color,
  val glowColor: Color,
  val badgeColor: Color
) {
  fun isUnlocked(currentRoomLevel: Int): Boolean = currentRoomLevel >= requiredLevel
}

object RoomThemePresets {
  val PRESETS = listOf(
    RoomThemePreset(
      id = RoomThemeConstants.MIDNIGHT_AURORA,
      name = "Midnight Aurora",
      subtitle = "Deep cosmic space with electric purple & vibrant cyan",
      requiredLevel = 1,
      primaryAccent = Color(0xFF06B6D4), // Cyan
      secondaryAccent = Color(0xFF8B5CF6), // Purple
      bgColors = listOf(
        Color(0xFF080B14),
        Color(0xFF110C24),
        Color(0xFF061824),
        Color(0xFF04060C)
      ),
      surfaceGlass = Color(0xFF101322),
      cardBorder = Color(0xFF06B6D4),
      glowColor = Color(0xFF06B6D4),
      badgeColor = Color(0xFF06B6D4)
    ),
    RoomThemePreset(
      id = RoomThemeConstants.SUNSET_LOUNGE,
      name = "Sunset Lounge",
      subtitle = "Velvet dusk ambience with neon rose, peach & warm amber",
      requiredLevel = 1,
      primaryAccent = Color(0xFFF43F5E), // Rose Pink
      secondaryAccent = Color(0xFFF97316), // Warm Orange
      bgColors = listOf(
        Color(0xFF14080F),
        Color(0xFF260C1B),
        Color(0xFF210F0A),
        Color(0xFF0A0407)
      ),
      surfaceGlass = Color(0xFF1E0F17),
      cardBorder = Color(0xFFF43F5E),
      glowColor = Color(0xFFF43F5E),
      badgeColor = Color(0xFFF43F5E)
    ),
    RoomThemePreset(
      id = RoomThemeConstants.EMERALD_NIGHT,
      name = "Emerald Night",
      subtitle = "Prestige lounge feeling with deep teal & jade aura",
      requiredLevel = 1,
      primaryAccent = Color(0xFF10B981), // Emerald
      secondaryAccent = Color(0xFF14B8A6), // Teal
      bgColors = listOf(
        Color(0xFF040F0A),
        Color(0xFF081C16),
        Color(0xFF061619),
        Color(0xFF020906)
      ),
      surfaceGlass = Color(0xFF0B1712),
      cardBorder = Color(0xFF10B981),
      glowColor = Color(0xFF10B981),
      badgeColor = Color(0xFF10B981)
    ),
    RoomThemePreset(
      id = RoomThemeConstants.CYBER_VIOLET,
      name = "Cyber Violet",
      subtitle = "High-voltage neon synthwave with cyber magenta glow",
      requiredLevel = 2,
      primaryAccent = Color(0xFFA855F7), // Neon Violet
      secondaryAccent = Color(0xFFEC4899), // Hot Pink
      bgColors = listOf(
        Color(0xFF090615),
        Color(0xFF1E0A36),
        Color(0xFF2B082D),
        Color(0xFF05030C)
      ),
      surfaceGlass = Color(0xFF170E28),
      cardBorder = Color(0xFFA855F7),
      glowColor = Color(0xFFA855F7),
      badgeColor = Color(0xFFA855F7)
    ),
    RoomThemePreset(
      id = RoomThemeConstants.COSMIC_GOLD,
      name = "Cosmic Gold",
      subtitle = "VIP obsidian prestige adorned in royal liquid gold",
      requiredLevel = 3,
      primaryAccent = Color(0xFFF59E0B), // Pure Gold Amber
      secondaryAccent = Color(0xFFFCD34D), // Sparkling Gold
      bgColors = listOf(
        Color(0xFF120E04),
        Color(0xFF261D07),
        Color(0xFF1D1404),
        Color(0xFF080602)
      ),
      surfaceGlass = Color(0xFF1C160A),
      cardBorder = Color(0xFFF59E0B),
      glowColor = Color(0xFFF59E0B),
      badgeColor = Color(0xFFF59E0B)
    )
  )

  fun getPreset(themeId: String): RoomThemePreset {
    return PRESETS.find { it.id == themeId } ?: PRESETS.first()
  }
}

data class ResolvedRoomTheme(
  val preset: RoomThemePreset,
  val primaryAccent: Color,
  val secondaryAccent: Color,
  val backgroundBrush: Brush,
  val surfaceGlass: Color,
  val surfaceGlassBorder: Color,
  val seatContainerBg: Color,
  val seatContainerBorder: Color,
  val seatItemEmptyBg: Color,
  val seatItemEmptyBorder: Color,
  val speakingRingColor: Color,
  val glowColor: Color,
  val buttonActiveBg: Color,
  val buttonActiveContent: Color = Color.White,
  val announcementBg: Color,
  val announcementBorder: Color,
  val chatBubbleHighlight: Color,
  val ambientParticles: Boolean,
  val backgroundEffect: RoomBackgroundEffect,
  val glowIntensity: Float,
  val glassAlpha: Float
)

object RoomThemeEngine {

  fun safeParseColor(hex: String, fallback: Color): Color {
    return try {
      val clean = hex.removePrefix("#").trim()
      val colorLong = clean.toLong(16)
      if (clean.length == 6) {
        Color(0xFF000000 or colorLong)
      } else if (clean.length == 8) {
        Color(colorLong)
      } else {
        fallback
      }
    } catch (_: Exception) {
      fallback
    }
  }

  fun resolveTheme(
    themeId: String,
    customConfig: RoomThemeConfig? = null
  ): ResolvedRoomTheme {
    val preset = RoomThemePresets.getPreset(themeId)

    val primaryAccent = if (customConfig != null && customConfig.primaryAccentHex.isNotBlank()) {
      safeParseColor(customConfig.primaryAccentHex, preset.primaryAccent)
    } else {
      preset.primaryAccent
    }

    val secondaryAccent = if (customConfig != null && customConfig.secondaryAccentHex.isNotBlank()) {
      safeParseColor(customConfig.secondaryAccentHex, preset.secondaryAccent)
    } else {
      preset.secondaryAccent
    }

    val bgIntensity = (customConfig?.backgroundIntensity ?: 0.85f).coerceIn(0.4f, 1.0f)
    val gradIntensity = (customConfig?.gradientIntensity ?: 0.70f).coerceIn(0.2f, 1.0f)
    val glowIntensity = (customConfig?.glowIntensity ?: 0.80f).coerceIn(0.0f, 1.0f)
    val glassTransp = (customConfig?.glassTransparency ?: 0.65f).coerceIn(0.15f, 0.95f)
    val borderBright = (customConfig?.borderBrightness ?: 0.50f).coerceIn(0.1f, 1.0f)
    val ambientParticles = customConfig?.ambientParticles ?: true
    val effect = customConfig?.backgroundEffect ?: RoomBackgroundEffect.AURORA

    // Compute gradient colors tuned by intensity
    val tunedBgColors = preset.bgColors.mapIndexed { idx, c ->
      if (idx == 1 || idx == 2) {
        c.copy(alpha = (c.alpha * gradIntensity * bgIntensity).coerceIn(0.3f, 1.0f))
      } else {
        c.copy(alpha = bgIntensity)
      }
    }

    val backgroundBrush = Brush.verticalGradient(tunedBgColors)

    val surfaceGlass = preset.surfaceGlass.copy(alpha = glassTransp)
    val surfaceGlassBorder = primaryAccent.copy(alpha = (borderBright * 0.45f).coerceIn(0.1f, 0.9f))

    val seatContainerBg = preset.surfaceGlass.copy(alpha = (glassTransp * 0.85f).coerceIn(0.1f, 0.9f))
    val seatContainerBorder = primaryAccent.copy(alpha = (borderBright * 0.4f).coerceIn(0.1f, 0.9f))

    val seatItemEmptyBg = Color.Black.copy(alpha = 0.35f)
    val seatItemEmptyBorder = primaryAccent.copy(alpha = (borderBright * 0.5f).coerceIn(0.2f, 0.9f))

    val speakingRingColor = primaryAccent.copy(alpha = (glowIntensity).coerceIn(0.3f, 1.0f))
    val glowColor = primaryAccent.copy(alpha = (glowIntensity * 0.6f).coerceIn(0.1f, 1.0f))

    val buttonActiveBg = primaryAccent
    val announcementBg = preset.surfaceGlass.copy(alpha = 0.75f)
    val announcementBorder = secondaryAccent.copy(alpha = (borderBright * 0.55f).coerceIn(0.15f, 0.95f))
    val chatBubbleHighlight = primaryAccent.copy(alpha = 0.15f)

    return ResolvedRoomTheme(
      preset = preset,
      primaryAccent = primaryAccent,
      secondaryAccent = secondaryAccent,
      backgroundBrush = backgroundBrush,
      surfaceGlass = surfaceGlass,
      surfaceGlassBorder = surfaceGlassBorder,
      seatContainerBg = seatContainerBg,
      seatContainerBorder = seatContainerBorder,
      seatItemEmptyBg = seatItemEmptyBg,
      seatItemEmptyBorder = seatItemEmptyBorder,
      speakingRingColor = speakingRingColor,
      glowColor = glowColor,
      buttonActiveBg = buttonActiveBg,
      announcementBg = announcementBg,
      announcementBorder = announcementBorder,
      chatBubbleHighlight = chatBubbleHighlight,
      ambientParticles = ambientParticles,
      backgroundEffect = effect,
      glowIntensity = glowIntensity,
      glassAlpha = glassTransp
    )
  }
}

val LocalRoomTheme = staticCompositionLocalOf {
  RoomThemeEngine.resolveTheme(RoomThemeConstants.MIDNIGHT_AURORA)
}

@Composable
fun ProvideRoomTheme(
  themeId: String,
  customConfig: RoomThemeConfig? = null,
  content: @Composable () -> Unit
) {
  val resolved = RoomThemeEngine.resolveTheme(themeId, customConfig)
  CompositionLocalProvider(LocalRoomTheme provides resolved) {
    content()
  }
}
