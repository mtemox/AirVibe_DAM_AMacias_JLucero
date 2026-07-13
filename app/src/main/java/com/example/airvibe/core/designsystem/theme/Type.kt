package com.example.airvibe.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.airvibe.R

/**
 * Familia tipográfica principal de AirVibe: **Geist** (Vercel).
 *
 * Se sirve mediante el proveedor descargable de Google Fonts para
 * mantener un peso binario bajo y delegar el cacheo al sistema. Si
 * el dispositivo no cuenta con Google Play Services, Compose
 * recurrirá automáticamente a la fuente sans-serif del sistema.
 *
 * Geometría: palo seco geométrico, tracking ajustado, números
 * tabulares — el complemento ideal para una UI minimalista tipo
 * shadcn/ui + Apple.
 */
private val GeistGoogleFont = GoogleFont("Geist")
private val VarelaRoundGoogleFont = GoogleFont("Varela Round")

private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val Geist: FontFamily = FontFamily(
    Font(googleFont = GeistGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Light, style = FontStyle.Normal),
    Font(googleFont = GeistGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(googleFont = GeistGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(googleFont = GeistGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(googleFont = GeistGoogleFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold, style = FontStyle.Normal),
)

val VarelaRound: FontFamily = FontFamily(
    Font(resId = R.font.varela_round, weight = FontWeight.Light, style = FontStyle.Normal),
    Font(resId = R.font.varela_round, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.varela_round, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.varela_round, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.varela_round, weight = FontWeight.Bold, style = FontStyle.Normal),
)

/**
 * Sistema tipográfico completo de AirVibe. Hereda la familia [Geist] y [VarelaRound].
 */
val AirVibeTypography: Typography = Typography(

    displayLarge = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),

    headlineLarge = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),

    titleLarge = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),

    labelLarge = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = VarelaRound,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)
