package com.hwb.aianswerer.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════
//  Premium Luxury Palette
//  Rich indigo-violet primary, warm ivory light,
//  deep carbon dark, luminous accents
// ═══════════════════════════════════════════════

// ── Dark Accent — rich carbon with subtle warmth ──
val DarkAccent = Color(0xFF17141F)
val DarkAccentGradientEnd = Color(0xFF252030)

// ── Primary — vibrant indigo-violet with depth ──
val PremiumPrimary = Color(0xFF6C5CE7)
val PremiumPrimaryVariant = Color(0xFF8B7CF0)
val PremiumPrimaryLight = Color(0xFFB8B0F8)
val PremiumPrimaryContainer = Color(0xFFEDEBF9)
val PremiumOnPrimaryContainer = Color(0xFF1E1050)

// ── Secondary Accent — warm champagne gold ──
val AccentBronze = Color(0xFFBE8B5E)
val AccentGold = Color(0xFFD4A44B)

// ── Background & Surface — deeper contrast ──
val PremiumBgLight = Color(0xFFEEEAF2)
val PremiumBgLightEnd = Color(0xFFE8E4EE)
val PremiumCardLight = Color(0xFFFFFFFF)
val PremiumSurfaceVariant = Color(0xFFF0EDF5)

// ── Glass — luminous frosted glass ──
val GlassWhite = Color(0xFFFFFFFF).copy(alpha = 0.70f)
val GlassWhiteStrong = Color(0xFFFFFFFF).copy(alpha = 0.82f)
val GlassWhiteBorder = Color(0xFFFFFFFF).copy(alpha = 0.88f)
val GlassDark = Color(0xFFFFFFFF).copy(alpha = 0.04f)
val GlassDarkBorder = Color(0xFFFFFFFF).copy(alpha = 0.08f)

// ── Glow Orbs — rich ambient warmth ──
val WarmGlow = Color(0xFFD4A44B).copy(alpha = 0.08f)
val IndigoGlow = Color(0xFF6C5CE7).copy(alpha = 0.08f)

// ── Input — clean premium inputs ──
val InputBackground = Color(0xFFF5F3F8)
val InputBorder = Color(0xFFE8E5F0)
val InputBorderFocus = Color(0xFF6C5CE7)
val InputBgFocus = Color(0xFF6C5CE7).copy(alpha = 0.03f)

// ── State — vibrant Apple-style ──
val SuccessGreen = Color(0xFF34C759)
val SuccessGreenLight = Color(0xFF67D480)
val ErrorRed = Color(0xFFFF3B30)
val ErrorRedLight = Color(0xFFFF6961)

// ── Chip — refined segmented control ──
val ChipUnselected = Color(0xFF000000).copy(alpha = 0.04f)
val ChipSelected = Color(0xFF6C5CE7).copy(alpha = 0.10f)

// ── Toggle — Apple switch ──
val ToggleOff = Color(0xFFE5E5EA)

// ── Text — premium grays with warmth ──
val TextDark = Color(0xFF1C1B1F)
val TextSecondary = Color(0xFF6B6878)
val TextTertiary = Color(0xFFADA8BE)

// ── Shadow tint — warm violet depth ──
val ShadowPurple = Color(0xFF4A3CC8)

// ── Dark Theme — deep carbon with luminous accents ──
val PremiumBgDark = Color(0xFF08080D)
val PremiumSurfaceDark = Color(0xFFFFFFFF).copy(alpha = 0.06f)
val PremiumSurfaceDarkBorder = Color(0xFFFFFFFF).copy(alpha = 0.10f)

val TextDarkPrimary = Color(0xFFFFFFFF).copy(alpha = 0.93f)
val TextDarkSecondary = Color(0xFFFFFFFF).copy(alpha = 0.58f)
val TextDarkTertiary = Color(0xFFFFFFFF).copy(alpha = 0.32f)

// ── M3 Compatibility ──

// Light
val LightPrimary = PremiumPrimary
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = PremiumPrimaryContainer
val LightOnPrimaryContainer = PremiumOnPrimaryContainer

val LightSecondary = Color(0xFF6B6878)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE8E5F0)
val LightOnSecondaryContainer = Color(0xFF1C1B1F)

val LightTertiary = Color(0xFF8E8E93)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFF5F3F8)
val LightOnTertiaryContainer = Color(0xFF1C1B1F)

val LightError = Color(0xFFFF3B30)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = PremiumBgLight
val LightOnBackground = Color(0xFF1C1B1F)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1C1B1F)
val LightSurfaceVariant = Color(0xFFF2F0F6)
val LightOnSurfaceVariant = Color(0xFF6B6878)
val LightOutline = Color(0xFFADA8BE)
val LightOutlineVariant = Color(0xFFD8D5E0)

val LightScrim = Color(0xFF000000)
val LightInverseSurface = Color(0xFF1C1B1F)
val LightInverseOnSurface = Color(0xFFF5F3F8)
val LightInversePrimary = Color(0xFFB8B0F8)

// Dark
val DarkPrimary = Color(0xFFB8B0F8)
val DarkOnPrimary = Color(0xFF1E1050)
val DarkPrimaryContainer = Color(0xFF3A3570)
val DarkOnPrimaryContainer = Color(0xFFEDEBF9)

val DarkSecondary = Color(0xFFADA8BE)
val DarkOnSecondary = Color(0xFF1C1B1F)
val DarkSecondaryContainer = Color(0xFF3A3840)
val DarkOnSecondaryContainer = Color(0xFFE8E5F0)

val DarkTertiary = Color(0xFF8E8E93)
val DarkOnTertiary = Color(0xFFFFFFFF)
val DarkTertiaryContainer = Color(0xFF3A3840)
val DarkOnTertiaryContainer = Color(0xFFF5F3F8)

val DarkError = Color(0xFFFF6961)
val DarkOnError = Color(0xFF410002)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = PremiumBgDark
val DarkOnBackground = Color(0xFFF5F3F8)
val DarkSurface = PremiumBgDark
val DarkOnSurface = Color(0xFFF5F3F8)
val DarkSurfaceVariant = Color(0xFF1A1A20)
val DarkOnSurfaceVariant = Color(0xFFD8D5E0)
val DarkOutline = Color(0xFF6B6878)
val DarkOutlineVariant = Color(0xFF3A3840)

val DarkScrim = Color(0xFF000000)
val DarkInverseSurface = Color(0xFFF5F3F8)
val DarkInverseOnSurface = Color(0xFF1C1B1F)
val DarkInversePrimary = Color(0xFF6C5CE7)
