package com.funpixelnest.mgp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MgpColors {
    val BgTop = Color(0xFF1E1150)
    val BgMid = Color(0xFF120B2E)
    val BgBottom = Color(0xFF070618)

    val Glass = Color(0x14FFFFFF)
    val GlassStrong = Color(0x26FFFFFF)
    val GlassStroke = Color(0x29FFFFFF)

    val Gold = Color(0xFFFFD25A)
    val GoldDeep = Color(0xFFFF9F1C)
    val Pink = Color(0xFFFF4ECD)
    val Purple = Color(0xFF8B5CF6)
    val Cyan = Color(0xFF22D3EE)
    val Blue = Color(0xFF3B82F6)
    val Green = Color(0xFF34D399)
    val Red = Color(0xFFFF5470)
    val Orange = Color(0xFFFF8A3D)

    val TextPrimary = Color.White
    val TextSecondary = Color(0xB3FFFFFF)
    val TextMuted = Color(0x80FFFFFF)

    val CardRed = Color(0xFFE11D48)
    val CardBlack = Color(0xFF111827)
    val Felt = Color(0xFF0B6B3A)
    val FeltDark = Color(0xFF053D22)
}

object MgpBrushes {
    val Primary = Brush.linearGradient(listOf(MgpColors.Pink, MgpColors.Purple))
    val Gold = Brush.linearGradient(listOf(MgpColors.Gold, MgpColors.GoldDeep))
    val Cool = Brush.linearGradient(listOf(MgpColors.Cyan, MgpColors.Blue))
    val Green = Brush.linearGradient(listOf(MgpColors.Green, Color(0xFF059669)))
    val Danger = Brush.linearGradient(listOf(MgpColors.Red, Color(0xFFBE123C)))
    val Glass = Brush.linearGradient(listOf(Color(0x24FFFFFF), Color(0x0AFFFFFF)))
}

@Composable
fun MgpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = MgpColors.Pink,
            secondary = MgpColors.Cyan,
            tertiary = MgpColors.Gold,
            background = MgpColors.BgMid,
            surface = Color(0xFF1C1640),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        typography = Typography(),
        content = content,
    )
}
