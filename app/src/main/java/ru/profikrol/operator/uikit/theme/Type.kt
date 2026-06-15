package ru.profikrol.operator.uikit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


private val Material3DefaultTypography = Typography()

val AppTypography = Material3DefaultTypography.copy(
    titleLarge = Material3DefaultTypography.titleLarge.copy(
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
