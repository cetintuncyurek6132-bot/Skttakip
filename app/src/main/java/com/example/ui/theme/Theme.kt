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
  darkColorScheme(
    primary = TurquoisePrimary,
    onPrimary = Color.Black,
    primaryContainer = TurquoiseDark,
    onPrimaryContainer = Color.White,
    secondary = CriticalOrange,
    tertiary = NormalGreen,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TurquoiseDark,
    onPrimary = Color.White,
    primaryContainer = TurquoiseContainer,
    onPrimaryContainer = OnTurquoiseContainer,
    secondary = CriticalOrange,
    tertiary = NormalGreen,
    background = Slate50,
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate500
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+, but let's keep it false by default for consistent brand turquoise
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
