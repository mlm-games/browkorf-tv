package com.phlox.tvwebbrowser.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme as MobileMaterialTheme
import androidx.compose.material3.darkColorScheme as mobileDarkColorScheme
import androidx.compose.material3.lightColorScheme as mobileLightColorScheme

@Immutable
data class TvBroColors(
    val topBarBackground: Color,
    val topBarBackground2: Color,
    val buttonBackground: Color,
    val buttonBackgroundFocused: Color,
    val buttonBackgroundPressed: Color,
    val buttonBackgroundDisabled: Color,
    val buttonCorner: Color,
    val focusBorder: Color,
    val tabBackground: Color,
    val tabBackgroundSelected: Color,
    val tabTextColor: Color,
    val tabTextColorSelected: Color,
    val iconColor: Color,
    val iconColorDisabled: Color,
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val progressTint: Color,
    val divider: Color,
    val badgeBackground: Color,
    val badgeStroke: Color,
)

val LightTvBroColors = TvBroColors(
    topBarBackground = Color(0xFFE8E8E8),
    topBarBackground2 = Color(0xFFD8D8D8),
    buttonBackground = Color(0xFFE0E0E0),
    buttonBackgroundFocused = Color(0xFFADE1F5),
    buttonBackgroundPressed = Color(0xFFC0C0C0),
    buttonBackgroundDisabled = Color(0xFFF0F0F0),
    buttonCorner = Color(0xFFAFAFAF),
    focusBorder = Color(0xFF33ADD6),
    tabBackground = Color(0xFFE6E6E6),
    tabBackgroundSelected = Color(0xFFAACCFF),
    tabTextColor = Color(0xFF666666),
    tabTextColorSelected = Color(0xFF212121),
    iconColor = Color(0xFF212121),
    iconColorDisabled = Color(0xFFBDBDBD),
    background = Color(0xFFF5F5F5),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF757575),
    progressTint = Color(0xFF33ADD6),
    divider = Color(0xFFBDBDBD),
    badgeBackground = Color(0xFFE0E0E0),
    badgeStroke = Color(0xFFAFAFAF),
)

val DarkTvBroColors = TvBroColors(
    topBarBackground = Color(0xFF2D2D2D),
    topBarBackground2 = Color(0xFF1F1F1F),
    buttonBackground = Color(0xFF3D3D3D),
    buttonBackgroundFocused = Color(0xFF1A5A6E),
    buttonBackgroundPressed = Color(0xFF4D4D4D),
    buttonBackgroundDisabled = Color(0xFF2A2A2A),
    buttonCorner = Color(0xFF5F5F5F),
    focusBorder = Color(0xFF33ADD6),
    tabBackground = Color(0xFF3D3D3D),
    tabBackgroundSelected = Color(0xFF1A5A6E),
    tabTextColor = Color(0xFFAAAAAA),
    tabTextColorSelected = Color(0xFFFFFFFF),
    iconColor = Color(0xFFE0E0E0),
    iconColorDisabled = Color(0xFF666666),
    background = Color(0xFF121212),
    textPrimary = Color(0xFFE0E0E0),
    textSecondary = Color(0xFFAAAAAA),
    progressTint = Color(0xFF33ADD6),
    divider = Color(0xFF444444),
    badgeBackground = Color(0xFF444444),
    badgeStroke = Color(0xFF666666),
)

val LocalTvBroColors = staticCompositionLocalOf { LightTvBroColors }

@Composable
fun TvBroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkTvBroColors else LightTvBroColors

    val tvColorScheme = if (darkTheme) darkColorScheme(
        primary = colors.focusBorder,
        surface = colors.buttonBackground,
        onSurface = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary
    ) else lightColorScheme(
        primary = colors.focusBorder,
        surface = colors.buttonBackground,
        onSurface = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary
    )

    // This maps the TV colors to the standard Mobile M3 (for settings)
    val mobileColorScheme = if (darkTheme) mobileDarkColorScheme(
        primary = colors.focusBorder,
        surface = colors.buttonBackground,
        onSurface = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary
    ) else mobileLightColorScheme(
        primary = colors.focusBorder,
        surface = colors.buttonBackground,
        onSurface = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary
    )

    CompositionLocalProvider(LocalTvBroColors provides colors) {
        // 3. Provide BOTH themes
        MobileMaterialTheme(
            colorScheme = mobileColorScheme
            // You can also map typography here if the settings text looks weird
        ) {
            MaterialTheme(
                colorScheme = tvColorScheme,
                typography = MaterialTheme.typography,
                content = content
            )
        }
    }
}

object TvBroTheme {
    val colors: TvBroColors
        @Composable
        get() = LocalTvBroColors.current
}