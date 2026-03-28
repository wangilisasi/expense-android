package com.example.expensemanager.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Slate600,
    onPrimary = Cloud50,
    primaryContainer = Slate700,
    onPrimaryContainer = Cloud50,
    secondary = Lavender500,
    onSecondary = Cloud50,
    secondaryContainer = Slate800,
    onSecondaryContainer = Cloud50,
    tertiary = Teal300,
    onTertiary = Slate950,
    tertiaryContainer = Slate900,
    onTertiaryContainer = Cloud50,
    background = Slate950,
    onBackground = Cloud50,
    surface = Slate900,
    onSurface = Cloud50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Cloud200,
    outline = Slate700,
    error = Red600,
    errorContainer = Red600.copy(alpha = 0.2f),
    onErrorContainer = Cloud50,
    scrim = Ink900
)

private val LightColorScheme = lightColorScheme(
    primary = Slate700,
    onPrimary = Cloud50,
    primaryContainer = Slate600,
    onPrimaryContainer = Cloud50,
    secondary = Lavender500,
    onSecondary = Cloud50,
    secondaryContainer = Cloud100,
    onSecondaryContainer = Ink900,
    tertiary = Teal500,
    onTertiary = Cloud50,
    tertiaryContainer = Cloud100,
    onTertiaryContainer = Ink900,
    background = Color(0xFFF2F5FC),
    onBackground = Ink900,
    surface = Color(0xFFFBFCFF),
    onSurface = Ink900,
    surfaceVariant = Color(0xFFE3EAF6),
    onSurfaceVariant = Ink700,
    outline = Color(0xFFBEC9DE),
    error = Red600,
    errorContainer = Color(0xFFFAD4D8),
    onErrorContainer = Ink900,
    scrim = Ink900
)

@Composable
fun ExpenseManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
