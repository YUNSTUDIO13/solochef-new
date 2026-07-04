package com.example.solochef.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─── Sage Color Palette (1:1 match with Web Tailwind theme) ───
val Sage50  = Color(0xFFF7F9F6)
val Sage100 = Color(0xFFF2F6F0)
val Sage200 = Color(0xFFE5EDE2)
val Sage300 = Color(0xFF8BA18B)
val Sage400 = Color(0xFF6A7F6A)
val Sage500 = Color(0xFF4A5C4A)
val Sage800 = Color(0xFF2D3A2D)
val Sage900 = Color(0xFF1A241A)

// ─── Accent Colors ───
val Indigo400 = Color(0xFF818CF8)
val Indigo500 = Color(0xFF6366F1)
val Amber400  = Color(0xFFFBBF24)
val Amber50   = Color(0xFFFFFBEB)
val Red50     = Color(0xFFFEF2F2)
val Red500    = Color(0xFFEF4444)
val White10   = Color(0x1AFFFFFF)
val White20   = Color(0x33FFFFFF)
val White40   = Color(0x66FFFFFF)
val White90   = Color(0xE6FFFFFF)
val Black20   = Color(0x33000000)
val Black40   = Color(0x66000000)
val Black60   = Color(0x99000000)

// ─── Functional ───
val DarkButton = Color(0xFF282C27)
val GreenPlay  = Color(0xFF07C160)
val SelectedGreen = Color(0xFFEEF5EE)
val Gray100    = Color(0xFFF3F4F6)
val Gray500    = Color(0xFF6B7280)

// ─── Warm Gradient (Pink-Beige) — global page background ───
val WarmPink50    = Color(0xFFF9E6E6)
val WarmCream100  = Color(0xFFF5E6D3)
val WarmBeige200  = Color(0xFFF2E5D5)

fun Modifier.warmGradientBackground(): Modifier = drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(WarmPink50, WarmCream100, WarmBeige200),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
}

// Frosted-glass surface background: translucent white with subtle shimmer
val FrostedGlassStart = Color(0x66FFFFFF)
val FrostedGlassEnd   = Color(0x40FFFFFF)
val FrostedBorder     = Color(0x40FFFFFF)

fun Modifier.frostedGlassBackground(): Modifier = drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(FrostedGlassStart, FrostedGlassEnd),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
}

// Liquid Glass: high opacity white for clean frosted look
val LiquidGlassStart = Color(0xCCFFFFFF)
val LiquidGlassEnd   = Color(0xA3FFFFFF)

fun Modifier.liquidGlassBackground(): Modifier = drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(LiquidGlassStart, LiquidGlassEnd),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
}

