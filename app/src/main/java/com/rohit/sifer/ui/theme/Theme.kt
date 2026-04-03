package com.rohit.sifer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SiferGreen,
    secondary = SiferYellow,
    tertiary = SiferBlueBadge,
    background = SiferBlack,
    surface = SiferBlack,
    onPrimary = SiferBlack,
    onSecondary = SiferBlack,
    onTertiary = SiferBlack,
    onBackground = SiferWhite,
    onSurface = SiferWhite
)

private val LightColorScheme = lightColorScheme(
    primary = SiferBlack,
    secondary = SiferGreen,
    tertiary = SiferYellow,
    background = SiferWhite,
    surface = SiferWhite,
    onPrimary = SiferWhite,
    onSecondary = SiferBlack,
    onTertiary = SiferBlack,
    onBackground = SiferBlack,
    onSurface = SiferBlack
)

@Composable
fun SiferTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to maintain Neo-Brutalism look
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
