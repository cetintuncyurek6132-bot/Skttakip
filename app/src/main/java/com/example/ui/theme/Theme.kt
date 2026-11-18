package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.util.FontDownloader
import java.util.Locale

val LocalLocale = staticCompositionLocalOf<Locale> { Locale.forLanguageTag("tr-TR") }

private val DarkColorScheme =
  darkColorScheme(
    primary = BrandTeal,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = TurquoiseContainer,
    onPrimaryContainer = Color.White,
    secondary = CriticalOrange,
    onSecondary = Color.White,
    tertiary = NormalGreen,
    onTertiary = Color.White,
    background = Slate900,
    surface = Slate800,
    surfaceVariant = Slate700,
    onBackground = Slate50,
    onSurface = Slate50,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Slate600
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandTealDark,
    onPrimary = Color.White,
    primaryContainer = BrandTealLight,
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = CriticalOrange,
    onSecondary = Color.White,
    tertiary = NormalGreen,
    onTertiary = Color.White,
    background = Slate50,
    surface = Color.White,
    surfaceVariant = Slate100,
    onBackground = Slate900,
    onSurface = Slate900,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    outlineVariant = Slate300
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+, but let's keep it false by default for consistent brand turquoise
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val fontChangeTrigger by FontDownloader.fontChangeTrigger.collectAsState()

  val dynamicFontFamily = remember(context, fontChangeTrigger) {
    getAppFontFamily(context)
  }
  val dynamicTypography = remember(dynamicFontFamily) {
    getAppTypography(dynamicFontFamily)
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  CompositionLocalProvider(LocalLocale provides Locale.forLanguageTag("tr-TR")) {
    MaterialTheme(colorScheme = colorScheme, typography = dynamicTypography) {
      ProvideTextStyle(
        value = TextStyle(
          fontFamily = dynamicFontFamily,
          fontWeight = FontWeight.SemiBold,
          localeList = TurkishLocaleList
        )
      ) {
        content()
      }
    }
  }
}
