package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LinkSaathiDarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = TealSecondaryDark,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = Color(0xFFA78BFA),
    background = NavyBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = NavySurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavySurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = RiskHighRedDark,
    errorContainer = RiskHighContainerDark,
    onErrorContainer = Color(0xFFFECACA)
)

private val LinkSaathiLightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealPrimaryContainerLight,
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = TealSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = TealSecondaryContainerLight,
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF6D28D9),
    background = AlabasterBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = PureSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = RiskHighRedLight,
    errorContainer = RiskHighContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

@Immutable
data class SemanticRiskPalette(
    val isDark: Boolean,
    val highText: Color,
    val highContainer: Color,
    val highBorder: Color,
    val mediumText: Color,
    val mediumContainer: Color,
    val mediumBorder: Color,
    val lowText: Color,
    val lowContainer: Color,
    val lowBorder: Color,
    val elevatedCard: Color
)

val LocalRiskPalette = staticCompositionLocalOf {
    SemanticRiskPalette(
        isDark = true,
        highText = RiskHighRedDark,
        highContainer = RiskHighContainerDark,
        highBorder = RiskHighBorderDark,
        mediumText = RiskMediumAmberDark,
        mediumContainer = RiskMediumContainerDark,
        mediumBorder = RiskMediumBorderDark,
        lowText = RiskLowEmeraldDark,
        lowContainer = RiskLowContainerDark,
        lowBorder = RiskLowBorderDark,
        elevatedCard = NavyElevatedCardDark
    )
}

@Composable
fun LinkSaathiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LinkSaathiDarkColorScheme else LinkSaathiLightColorScheme
    val riskPalette = if (darkTheme) {
        SemanticRiskPalette(
            isDark = true,
            highText = RiskHighRedDark,
            highContainer = RiskHighContainerDark,
            highBorder = RiskHighBorderDark,
            mediumText = RiskMediumAmberDark,
            mediumContainer = RiskMediumContainerDark,
            mediumBorder = RiskMediumBorderDark,
            lowText = RiskLowEmeraldDark,
            lowContainer = RiskLowContainerDark,
            lowBorder = RiskLowBorderDark,
            elevatedCard = NavyElevatedCardDark
        )
    } else {
        SemanticRiskPalette(
            isDark = false,
            highText = RiskHighRedLight,
            highContainer = RiskHighContainerLight,
            highBorder = RiskHighBorderLight,
            mediumText = RiskMediumAmberLight,
            mediumContainer = RiskMediumContainerLight,
            mediumBorder = RiskMediumBorderLight,
            lowText = RiskLowEmeraldLight,
            lowContainer = RiskLowContainerLight,
            lowBorder = RiskLowBorderLight,
            elevatedCard = PureSurfaceLight
        )
    }

    CompositionLocalProvider(LocalRiskPalette provides riskPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
