package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// =========================================================================
// PREMIUM COLOR SYSTEM (Deep Near-Black, Rich Violet, Selective Cyan & Magenta)
// =========================================================================

// Main Backgrounds: Deep Near-Black Obsidian (Never navy or flat blue)
val DarkBackground = Color(0xFF090A10)
val DarkBackgroundSecondary = Color(0xFF0E0F17)

// Surfaces & Cards: Smoky Glassmorphic Surfaces with Layered Depth
val DarkSurface = Color(0xFF12131D)
val DarkSurfaceElevated = Color(0xFF181A27)
val DarkSurfaceCard = Color(0xFF141523)
val DarkSurfaceCardElevated = Color(0xFF1B1D2E)
val DarkSurfaceBorder = Color(0x22FFFFFF)
val DarkCardBorderSubtle = Color(0x18FFFFFF)

// Primary Accent: Rich Electric Violet & Royal Purple (High-end social streaming vibe)
val BrandPrimary = Color(0xFF8B5CF6)
val BrandViolet = Color(0xFF7C3AED)
val BrandVioletLight = Color(0xFFA78BFA)
val BrandVioletDark = Color(0xFF6D28D9)
val PurpleAccent = Color(0xFF8B5CF6)
val PurpleContainer = Color(0x288B5CF6)

// Compatibility aliases for legacy call sites
val IndigoPrimary = BrandPrimary
val IndigoViolet = BrandViolet
val IndigoLight = BrandVioletLight
val IndigoDark = BrandVioletDark

// Secondary Accent: Selective Cyan / Teal (Used strictly for highlights, badges, metrics)
val CyanAccent = Color(0xFF06B6D4)
val CyanLight = Color(0xFF22D3EE)
val CyanDark = Color(0xFF0891B2)
val TealAccent = Color(0xFF14B8A6)
val BlueAccent = CyanAccent

// Highlight Accent: Vivid Pink / Magenta (Subtle gradient highlights)
val MagentaAccent = Color(0xFFEC4899)
val RoseAccent = Color(0xFFF43F5E)
val PinkHighlight = MagentaAccent

// Premium Gradients with Visual Depth
val PremiumPrimaryGradient = Brush.horizontalGradient(listOf(BrandPrimary, MagentaAccent))
val PremiumVioletBlueGradient = Brush.horizontalGradient(listOf(BrandViolet, BrandPrimary))
val PremiumBlueCyanGradient = Brush.horizontalGradient(listOf(BrandPrimary, CyanAccent))
val PremiumCyanVioletGradient = Brush.horizontalGradient(listOf(CyanAccent, BrandPrimary))
val PremiumHeroGradient = Brush.verticalGradient(
  listOf(
    Color(0x2C8B5CF6),
    Color(0x14EC4899),
    Color.Transparent
  )
)

// Typography: Clean Crisp White & Neutral Slate Hierarchy
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)

// Status Badges & Roles
val EmeraldOnline = Color(0xFF10B981)
val EmeraldContainer = Color(0xFF064E3B)
val RoseError = Color(0xFFEF4444)
val RoseContainer = Color(0xFF450A0A)
val GoldOwner = Color(0xFFF59E0B)
val GoldOwnerContainer = Color(0xFF451A03)
val ShieldAdmin = Color(0xFF8B5CF6)
val AmberXp = Color(0xFFFBBF24)
val VioletAudience = Color(0xFF8B5CF6)

// Light Theme Fallbacks
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightSurfaceBorder = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)


