package com.opencode.mobile.ui.theme

import androidx.compose.ui.graphics.Color

data class SkinColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val onPrimary: Color = Color.White,
    val onSecondary: Color = Color.White,
    val onBackground: Color = Color(0xFF1C1B1F),
    val onSurface: Color = Color(0xFF1C1B1F)
)

object Skins {
    val default = SkinColors(
        primary = Color(0xFF1A73E8), secondary = Color(0xFF03DAC6), tertiary = Color(0xFF7D5260),
        background = Color(0xFFF5F5F5), surface = Color(0xFFFFFFFF), error = Color(0xFFB00020)
    )

    val defaultDark = SkinColors(
        primary = Color(0xFF8AB4F8), secondary = Color(0xFF03DAC6), tertiary = Color(0xFFEFB8C8),
        background = Color(0xFF121212), surface = Color(0xFF1E1E1E), error = Color(0xFFCF6679),
        onBackground = Color(0xFFE6E1E5), onSurface = Color(0xFFE6E1E5)
    )

    val ocean = SkinColors(
        primary = Color(0xFF00695C), secondary = Color(0xFF4DB6AC), tertiary = Color(0xFF80CBC4),
        background = Color(0xFFF5FAFA), surface = Color(0xFFFFFFFF), error = Color(0xFFB00020)
    )

    val oceanDark = SkinColors(
        primary = Color(0xFF4DB6AC), secondary = Color(0xFF80CBC4), tertiary = Color(0xFFB2DFDB),
        background = Color(0xFF0D1B1A), surface = Color(0xFF142826), error = Color(0xFFCF6679),
        onBackground = Color(0xFFE6E1E5), onSurface = Color(0xFFE6E1E5)
    )

    val forest = SkinColors(
        primary = Color(0xFF2E7D32), secondary = Color(0xFF81C784), tertiary = Color(0xFFA5D6A7),
        background = Color(0xFFF5F9F5), surface = Color(0xFFFFFFFF), error = Color(0xFFB00020)
    )

    val forestDark = SkinColors(
        primary = Color(0xFF81C784), secondary = Color(0xFFA5D6A7), tertiary = Color(0xFFC8E6C9),
        background = Color(0xFF0D1A0D), surface = Color(0xFF142614), error = Color(0xFFCF6679),
        onBackground = Color(0xFFE6E1E5), onSurface = Color(0xFFE6E1E5)
    )

    val sunset = SkinColors(
        primary = Color(0xFFE65100), secondary = Color(0xFFFF8A65), tertiary = Color(0xFFFFCC80),
        background = Color(0xFFFFF8F5), surface = Color(0xFFFFFFFF), error = Color(0xFFB00020)
    )

    val sunsetDark = SkinColors(
        primary = Color(0xFFFF8A65), secondary = Color(0xFFFFCC80), tertiary = Color(0xFFFFE0B2),
        background = Color(0xFF1A0D08), surface = Color(0xFF26140E), error = Color(0xFFCF6679),
        onBackground = Color(0xFFE6E1E5), onSurface = Color(0xFFE6E1E5)
    )

    val lavender = SkinColors(
        primary = Color(0xFF7B1FA2), secondary = Color(0xFFCE93D8), tertiary = Color(0xFFE1BEE7),
        background = Color(0xFFFDF5FF), surface = Color(0xFFFFFFFF), error = Color(0xFFB00020)
    )

    val lavenderDark = SkinColors(
        primary = Color(0xFFCE93D8), secondary = Color(0xFFE1BEE7), tertiary = Color(0xFFF3E5F5),
        background = Color(0xFF140D1A), surface = Color(0xFF1E1426), error = Color(0xFFCF6679),
        onBackground = Color(0xFFE6E1E5), onSurface = Color(0xFFE6E1E5)
    )

    val all = mapOf(
        "default" to "Стандартный",
        "ocean" to "Океан",
        "forest" to "Лес",
        "sunset" to "Закат",
        "lavender" to "Лаванда"
    )

    fun light(id: String): SkinColors = when (id) {
        "ocean" -> ocean; "forest" -> forest; "sunset" -> sunset; "lavender" -> lavender
        else -> default
    }

    fun dark(id: String): SkinColors = when (id) {
        "ocean" -> oceanDark; "forest" -> forestDark; "sunset" -> sunsetDark; "lavender" -> lavenderDark
        else -> defaultDark
    }
}
