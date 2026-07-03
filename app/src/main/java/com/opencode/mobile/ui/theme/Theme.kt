package com.opencode.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.opencode.mobile.data.PreferencesManager

@Composable
fun OpenCodeMobileTheme(
    themeMode: String = "system",
    skinId: String = "default",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val colors = if (darkTheme) Skins.dark(skinId) else Skins.light(skinId)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && skinId == "default" -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = colors.primary, secondary = colors.secondary, tertiary = colors.tertiary,
            background = colors.background, surface = colors.surface, error = colors.error,
            onPrimary = colors.onPrimary, onSecondary = colors.onSecondary,
            onBackground = colors.onBackground, onSurface = colors.onSurface
        )
        else -> lightColorScheme(
            primary = colors.primary, secondary = colors.secondary, tertiary = colors.tertiary,
            background = colors.background, surface = colors.surface, error = colors.error,
            onPrimary = colors.onPrimary, onSecondary = colors.onSecondary,
            onBackground = colors.onBackground, onSurface = colors.onSurface
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun ThemedApp(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val themeMode by prefs.theme.collectAsState(initial = "system")
    val skinId by prefs.skin.collectAsState(initial = "default")

    OpenCodeMobileTheme(themeMode = themeMode, skinId = skinId) {
        content()
    }
}
