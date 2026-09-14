package com.client.xvideos.ui.theme

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

import com.client.xvideos.common.theme.Theme as AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF381E72),
    secondary = AppTheme.R.colorYellow,
    onSecondary = Color.Black,
    tertiary = AppTheme.L.lavender,
    onTertiary = Color.Black,
    background = AppTheme.background,
    onBackground = AppTheme.L.textColor,
    surface = AppTheme.tabLevel1,
    onSurface = AppTheme.L.textColor,
    surfaceVariant = AppTheme.tabLevel2,
    onSurfaceVariant = AppTheme.L.grey2,
    outline = AppTheme.R.colorBorderGray,
    error = AppTheme.Feedback.error,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun XvideosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (false by default to preserve custom dark theme)
    dynamicColor: Boolean = false,
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
