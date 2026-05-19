package com.fordham.toolbelt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

val IndustrialDarkColors = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    secondary = BrandOrange,
    onSecondary = Color.Black,
    surface = DarkBackground,
    background = DarkBackground,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurface = Color.White,
    onBackground = Color.White,
    error = Color(0xFFE57373),
    outline = Color.White
)

val IndustrialLightColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    secondary = BrandOrange,
    onSecondary = Color.White,
    surface = Color.White,
    background = GlobalBackground,
    surfaceVariant = Color.White,
    onSurface = Color.Black,
    onBackground = Color.Black,
    outline = Color.Black
)

val ToolbeltTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ToolbeltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) IndustrialDarkColors else IndustrialLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ToolbeltTypography,
        content = content
    )
}
