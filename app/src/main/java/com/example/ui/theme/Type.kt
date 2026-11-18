package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp
import com.example.util.FontDownloader
import java.io.File

val TurkishLocaleList = LocaleList(Locale("tr-TR"))

var AppFontFamily: FontFamily = FontFamily.Default

fun getAppFontFamily(context: Context): FontFamily {
    val resolvedFamily = runCatching {
        val selectedFont = FontDownloader.getSelectedFont(context)
        if (selectedFont == FontDownloader.FONT_DEFAULT) {
            return@runCatching FontFamily.Default
        }

        val spec = FontDownloader.getFontFileSpec(selectedFont) ?: return@runCatching FontFamily.Default
        val fontsDir = FontDownloader.getFontsDir(context)
        val regularFile = File(fontsDir, spec.regularFileName)
        val mediumFile = File(fontsDir, spec.mediumFileName)
        val boldFile = File(fontsDir, spec.boldFileName)

        if (
            regularFile.exists() && regularFile.length() > 1024L &&
            mediumFile.exists() && mediumFile.length() > 1024L &&
            boldFile.exists() && boldFile.length() > 1024L
        ) {
            FontFamily(
                Font(regularFile, FontWeight.Normal),
                Font(mediumFile, FontWeight.Medium),
                Font(boldFile, FontWeight.SemiBold),
                Font(boldFile, FontWeight.Bold),
                Font(boldFile, FontWeight.ExtraBold)
            )
        } else {
            FontFamily.Default
        }
    }.getOrDefault(FontFamily.Default)

    AppFontFamily = resolvedFamily
    return resolvedFamily
}

fun getAppTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            localeList = TurkishLocaleList
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            localeList = TurkishLocaleList
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            localeList = TurkishLocaleList
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.2).sp,
            localeList = TurkishLocaleList
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.15).sp,
            localeList = TurkishLocaleList
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            localeList = TurkishLocaleList
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.1).sp,
            localeList = TurkishLocaleList
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
            localeList = TurkishLocaleList
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
            localeList = TurkishLocaleList
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp,
            localeList = TurkishLocaleList
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            localeList = TurkishLocaleList
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.15.sp,
            localeList = TurkishLocaleList
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            localeList = TurkishLocaleList
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.15.sp,
            localeList = TurkishLocaleList
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.15.sp,
            localeList = TurkishLocaleList
        )
    )
}

val Typography = getAppTypography(FontFamily.Default)
