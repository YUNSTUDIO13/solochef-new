package com.example.solochef.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SoloChefTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        letterSpacing = (-0.05).sp,
        color = Sage900
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        letterSpacing = (-0.03).sp,
        color = Sage900
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        color = Sage900
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        color = Sage900
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        color = Sage900
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Sage900
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = Sage500
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        color = Sage400
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        color = Sage900
    )
)
