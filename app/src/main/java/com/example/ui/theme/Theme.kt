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

private val SpendWiseDarkColorScheme = darkColorScheme(
    primary = Mint400,
    onPrimary = Emerald900,
    primaryContainer = Emerald800,
    onPrimaryContainer = Mint100,
    secondary = Mint300,
    onSecondary = Emerald900,
    secondaryContainer = Emerald700,
    onSecondaryContainer = Mint50,
    tertiary = AmberWarning,
    onTertiary = Color.Black,
    background = SlateBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = RedDanger,
    onError = Color.White
)

private val SpendWiseLightColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = Mint100,
    onPrimaryContainer = Emerald900,
    secondary = Emerald600,
    onSecondary = Color.White,
    secondaryContainer = Mint50,
    onSecondaryContainer = Emerald800,
    tertiary = AmberWarning,
    onTertiary = Color.Black,
    background = SlateBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = RedDanger,
    onError = Color.White
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default to our emerald wealth palette for branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SpendWiseDarkColorScheme
        else -> SpendWiseLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
