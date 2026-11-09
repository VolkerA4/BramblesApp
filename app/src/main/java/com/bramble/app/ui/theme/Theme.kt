package com.bramble.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

// The Bramble palette: every color in the app comes from these eight.
val SageGreen = Color(0xFF91B66B)
val DeepGreen = Color(0xFF2E4A38)
val DarkPurple = Color(0xFF30253E)
val MidnightViolet = Color(0xFF241D37)
val LavenderVeil = Color(0xFFE3D6E4)
val AmethystSmoke = Color(0xFF8C80B5)
val BerryRose = Color(0xFF9A3B4A)
val BlushPink = Color(0xFFFFD9DC)

// Neutrals derived from the palette
private val SageTint = Color(0xFFD1EFA6)
private val SageSoft = lerp(SageGreen, Color.White, 0.55f)
private val GreenBlack = lerp(DeepGreen, Color.Black, 0.6f)
private val GreenSurface = lerp(DeepGreen, Color.Black, 0.45f)

// Used when the device doesn't supply wallpaper-based colors.
private val LightColors = lightColorScheme(
    primary = DeepGreen,
    onPrimary = LavenderVeil,
    primaryContainer = SageGreen,
    onPrimaryContainer = MidnightViolet,

    secondary = DarkPurple,
    onSecondary = LavenderVeil,
    secondaryContainer = LavenderVeil,
    onSecondaryContainer = MidnightViolet,

    tertiary = BerryRose,
    onTertiary = BlushPink,
    tertiaryContainer = BlushPink,
    onTertiaryContainer = MidnightViolet,

    background = SageTint,
    onBackground = MidnightViolet,
    surface = SageTint,
    onSurface = MidnightViolet,
    surfaceVariant = SageSoft,
    onSurfaceVariant = MidnightViolet,
    outline = DeepGreen,
    surfaceContainerLowest = Color.White, // task cards and panels
)

private val DarkColors = darkColorScheme(
    primary = SageGreen,
    onPrimary = MidnightViolet,
    primaryContainer = DeepGreen,
    onPrimaryContainer = SageGreen,

    secondary = AmethystSmoke,
    onSecondary = MidnightViolet,
    secondaryContainer = DarkPurple,
    onSecondaryContainer = LavenderVeil,

    tertiary = BlushPink,
    onTertiary = MidnightViolet,
    tertiaryContainer = BerryRose,
    onTertiaryContainer = BlushPink,

    background = GreenBlack,
    onBackground = SageTint,
    surface = GreenSurface,
    onSurface = SageTint,
    surfaceVariant = DeepGreen,
    onSurfaceVariant = SageGreen,
    outline = AmethystSmoke,
    surfaceContainerLowest = GreenSurface, // task cards and panels
)

@Composable
fun BrambleTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, typography = BrambleTypography, shapes = BrambleShapes, content = content)
}