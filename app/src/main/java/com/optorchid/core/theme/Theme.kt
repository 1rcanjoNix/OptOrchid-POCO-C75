package com.optorchid.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val OrchidDarkColorScheme = darkColorScheme(
    primary = OrchidColors.Violet,
    onPrimary = OrchidColors.Background,
    secondary = OrchidColors.VioletGlow,
    onSecondary = OrchidColors.White,
    tertiary = OrchidColors.TechBlue,
    onTertiary = OrchidColors.Background,
    background = OrchidColors.Background,
    onBackground = OrchidColors.White,
    surface = OrchidColors.Surface,
    onSurface = OrchidColors.White,
    surfaceVariant = OrchidColors.SurfaceHigh,
    onSurfaceVariant = OrchidColors.Gray,
    outline = OrchidColors.Outline,
    error = OrchidColors.Critical,
)

/** Fase 1: apenas tema escuro. Tema claro e automático pertencem à Fase 5 (Personalização). */
@Composable
fun OptOrchidTheme(reduceMotion: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = OrchidDarkColorScheme,
            typography = OrchidTypography,
            content = content,
        )
    }
}
