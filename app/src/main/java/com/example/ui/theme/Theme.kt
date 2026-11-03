package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OrbitDarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = SlateDark950,
    primaryContainer = CyanDark,
    onPrimaryContainer = CyanNeon,
    secondary = CyanAccent,
    onSecondary = SlateDark950,
    secondaryContainer = SlateDark700,
    onSecondaryContainer = SlateLight100,
    tertiary = AmberAccent,
    onTertiary = SlateDark950,
    background = SlateDark950,
    onBackground = SlateWhite,
    surface = SlateDark900,
    onSurface = SlateWhite,
    surfaceVariant = SlateDark800,
    onSurfaceVariant = SlateDark300,
    surfaceContainer = SlateDark800,
    surfaceContainerHigh = SlateDark700,
    outline = SlateDark600,
    outlineVariant = SlateDark700
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // ORBIT Camera enforces a sleek dark anthracite studio interface
    MaterialTheme(
        colorScheme = OrbitDarkColorScheme,
        typography = Typography,
        content = content
    )
}
