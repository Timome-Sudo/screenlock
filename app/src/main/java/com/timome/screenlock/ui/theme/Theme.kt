package com.timome.screenlock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 扩展函数，返回颜色的负值（反色）
 */
private fun Color.inverted(): Color {
    val r = 1.0f - red
    val g = 1.0f - green
    val b = 1.0f - blue
    return Color(r, g, b, alpha)
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun ScreenlockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    inverted: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val finalScheme = if (inverted) {
        colorScheme.copy(
            primary = colorScheme.primary.inverted(),
            onPrimary = colorScheme.onPrimary.inverted(),
            primaryContainer = colorScheme.primaryContainer.inverted(),
            onPrimaryContainer = colorScheme.onPrimaryContainer.inverted(),
            secondary = colorScheme.secondary.inverted(),
            onSecondary = colorScheme.onSecondary.inverted(),
            secondaryContainer = colorScheme.secondaryContainer.inverted(),
            onSecondaryContainer = colorScheme.onSecondaryContainer.inverted(),
            tertiary = colorScheme.tertiary.inverted(),
            onTertiary = colorScheme.onTertiary.inverted(),
            tertiaryContainer = colorScheme.tertiaryContainer.inverted(),
            onTertiaryContainer = colorScheme.onTertiaryContainer.inverted(),
            background = colorScheme.background.inverted(),
            onBackground = colorScheme.onBackground.inverted(),
            surface = colorScheme.surface.inverted(),
            onSurface = colorScheme.onSurface.inverted(),
            surfaceVariant = colorScheme.surfaceVariant.inverted(),
            onSurfaceVariant = colorScheme.onSurfaceVariant.inverted(),
            error = colorScheme.error.inverted(),
            onError = colorScheme.onError.inverted(),
            errorContainer = colorScheme.errorContainer.inverted(),
            onErrorContainer = colorScheme.onErrorContainer.inverted(),
            outline = colorScheme.outline.inverted(),
            outlineVariant = colorScheme.outlineVariant.inverted(),
            scrim = colorScheme.scrim.inverted(),
            inverseSurface = colorScheme.inverseSurface.inverted(),
            inverseOnSurface = colorScheme.inverseOnSurface.inverted(),
            inversePrimary = colorScheme.inversePrimary.inverted()
        )
    } else {
        colorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = finalScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}