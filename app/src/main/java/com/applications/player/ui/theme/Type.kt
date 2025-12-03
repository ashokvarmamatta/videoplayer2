package com.applications.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.applications.player.R

// 1. Define the FontFamily using your font resource
val ReadexPro = FontFamily(
    Font(R.font.readx_pro_regular, FontWeight.Normal)
    // You can add other weights here if you have them, e.g., bold
    // Font(R.font.readx_pro_bold, FontWeight.Bold)
)
// Set of Material typography styles to start with
val Typography1 = Typography(
    bodyLarge = TextStyle(
        fontFamily = ReadexPro,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

/*
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val bodyFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Plus Jakarta Sans"),
        fontProvider = provider,
    )
)

val displayFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Inter"),
        fontProvider = provider,
    )
)*/

// Default Material 3 typography values
val baseline = Typography()

val Typography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = ReadexPro),
    displayMedium = baseline.displayMedium.copy(fontFamily = ReadexPro),
    displaySmall = baseline.displaySmall.copy(fontFamily = ReadexPro),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = ReadexPro),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = ReadexPro),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = ReadexPro),
    titleLarge = baseline.titleLarge.copy(fontFamily = ReadexPro),
    titleMedium = baseline.titleMedium.copy(fontFamily = ReadexPro),
    titleSmall = baseline.titleSmall.copy(fontFamily = ReadexPro),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = ReadexPro),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = ReadexPro),
    bodySmall = baseline.bodySmall.copy(fontFamily = ReadexPro),
    labelLarge = baseline.labelLarge.copy(fontFamily = ReadexPro),
    labelMedium = baseline.labelMedium.copy(fontFamily = ReadexPro),
    labelSmall = baseline.labelSmall.copy(fontFamily = ReadexPro),
)