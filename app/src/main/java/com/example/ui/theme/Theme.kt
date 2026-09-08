package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ModernDarkColorScheme =
  darkColorScheme(
    primary = IndigoLight,
    onPrimary = DarkBackground,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = TextPrimary,
    secondary = PurpleAccent,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberXp,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceCard,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkSurfaceBorder,
    error = RoseError,
    onError = TextPrimary,
  )

private val ModernLightColorScheme =
  lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceElevated,
    onPrimaryContainer = LightTextPrimary,
    secondary = PurpleAccent,
    onSecondary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    error = RoseError,
    onError = LightSurface,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) ModernDarkColorScheme else ModernLightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

