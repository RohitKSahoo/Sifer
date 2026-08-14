package com.rohit.sifer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.rohit.sifer.R

val provider = GoogleFont.Provider(
    "com.google.android.gms.fonts",
    "com.google.android.gms",
    R.array.com_google_android_gms_fonts_certs
)

val OutfitFont = GoogleFont("Outfit")

val SiferFontFamily = FontFamily(
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Black)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = SiferFontFamily),
    displayMedium = TextStyle(fontFamily = SiferFontFamily),
    displaySmall = TextStyle(fontFamily = SiferFontFamily),
    headlineLarge = TextStyle(fontFamily = SiferFontFamily),
    headlineMedium = TextStyle(fontFamily = SiferFontFamily),
    headlineSmall = TextStyle(fontFamily = SiferFontFamily),
    titleLarge = TextStyle(fontFamily = SiferFontFamily),
    titleMedium = TextStyle(fontFamily = SiferFontFamily),
    titleSmall = TextStyle(fontFamily = SiferFontFamily),
    bodyLarge = TextStyle(fontFamily = SiferFontFamily, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = SiferFontFamily),
    bodySmall = TextStyle(fontFamily = SiferFontFamily),
    labelLarge = TextStyle(fontFamily = SiferFontFamily),
    labelMedium = TextStyle(fontFamily = SiferFontFamily),
    labelSmall = TextStyle(fontFamily = SiferFontFamily)
)