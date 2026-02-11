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
    primary = Teal300,
    onPrimary = Slate950,
    secondary = Slate700,
    tertiary = Teal500,
    background = Slate950,
    onBackground = Cloud50,
    surface = Slate900,
    onSurface = Cloud50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Cloud200,
    error = Red600
)

private val LightColorScheme = lightColorScheme(
    primary = Teal500,
    onPrimary = Cloud50,
    secondary = Slate700,
    onSecondary = Cloud50,
    tertiary = Teal300,
    onTertiary = Slate950,
    background = Cloud50,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Cloud100,
    onSurfaceVariant = Ink700,
    outline = Cloud200,
    error = Red600
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
