package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ==========================================
// Spacing Tokens (8dp Grid)
// ==========================================
object SpendWiseSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// ==========================================
// Shapes Scale
// ==========================================
val SpendWiseShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ==========================================
// Light Color Scheme
// ==========================================
private val SpendWiseLightColorScheme = lightColorScheme(
    primary = BrandBlue,                     // #0171DF
    onPrimary = OnBrandBlue,                 // White
    primaryContainer = BrandBlueLightContainer,
    onPrimaryContainer = BrandNavy,
    secondary = BrandGreen,                  // #3CAE8B
    onSecondary = OnBrandGreen,              // #121660 (strictly navy)
    secondaryContainer = BrandGreenLightContainer,
    onSecondaryContainer = BrandNavy,
    tertiary = BrandYellow,                  // #FFD140
    onTertiary = OnBrandYellow,              // #121660 (strictly navy)
    tertiaryContainer = BrandYellowLightContainer,
    onTertiaryContainer = BrandNavy,
    background = BackgroundLight,            // #F4F8FC (near-white faint blue tint)
    onBackground = TextPrimaryLight,         // #121660 (Deep Navy text)
    surface = SurfaceLight,                  // #FFFFFF
    onSurface = TextPrimaryLight,            // #121660
    surfaceVariant = SurfaceVariantLight,    // #E8F0F8
    onSurfaceVariant = TextSecondaryLight,   // #4A5580
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = RedDanger,
    onError = Color.White,
    errorContainer = RedDangerContainer,
    onErrorContainer = BrandNavy
)

// ==========================================
// Dark Color Scheme
// ==========================================
private val SpendWiseDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FA5FF),             // Lighter vibrant tint of #0171DF for dark mode contrast
    onPrimary = BrandNavy,
    primaryContainer = BrandBlueDarkContainer,
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = Color(0xFF4DD2A9),           // Lighter mint green tint
    onSecondary = BrandNavy,                 // Strictly navy
    secondaryContainer = Color(0xFF165240),
    onSecondaryContainer = Color(0xFFB8F3E2),
    tertiary = BrandYellow,                  // #FFD140
    onTertiary = BrandNavy,                  // Strictly navy
    tertiaryContainer = Color(0xFF5E4B00),
    onTertiaryContainer = Color(0xFFFFEEAA),
    background = BackgroundDark,             // #080A1F (very dark navy base)
    onBackground = TextPrimaryDark,          // Crisp near-white
    surface = SurfaceDark,                   // #0E1235 (dark navy card surface)
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,     // #171D4B
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = Color(0xFFF87171),
    onError = BrandNavy,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECDD3)
)

@Composable
fun SpendWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Dynamic color disabled per specification so brand palette is always active
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SpendWiseDarkColorScheme else SpendWiseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SpendWiseShapes,
        content = content
    )
}
