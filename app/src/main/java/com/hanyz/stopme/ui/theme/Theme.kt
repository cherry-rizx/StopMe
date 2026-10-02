package com.hanyz.stopme.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ColorScheme M3 StopMe
private val StopMeColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = TextWhite,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = TextWhite,
    secondary = OrangeAccent,
    onSecondary = TextWhite,
    secondaryContainer = LightGreenChip,
    onSecondaryContainer = NavyPrimary,
    tertiary = DarkNavyCard,
    onTertiary = TextWhite,
    background = BackgroundWhite,
    onBackground = TextDark,
    surface = BackgroundWhite,
    onSurface = TextDark,
    surfaceVariant = SecondaryBackground,
    onSurfaceVariant = TextSecondary,
    outline = DisabledGrey
)

// Bentuk sudut membulat resmi StopMe: kartu 16dp, tombol 12dp, dialog/sheet 32dp
val StopMeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),   // tombol 12dp
    large = RoundedCornerShape(16.dp),    // kartu 16dp
    extraLarge = RoundedCornerShape(32.dp) // bottom sheet 32dp
)

@Composable
fun StopMeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = StopMeColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = NavyPrimary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = StopMeShapes,
        content = content
    )
}
