package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  lightColorScheme(
    primary = CyberCyan,
    secondary = CyberGreen,
    tertiary = CyberAmber,
    error = CyberRed,
    background = CyberDarkBg,
    surface = CyberSurface,
    surfaceVariant = CyberSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CyberCyan,
    secondary = CyberGreen,
    tertiary = CyberAmber,
    error = CyberRed,
    background = CyberDarkBg,
    surface = CyberSurface,
    surfaceVariant = CyberSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Emphasize high density light theme as defined in spec
  // Dynamic color is disabled by default to strictly enforce brand styles
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
