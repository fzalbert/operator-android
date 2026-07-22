package ru.profikrol.operator.uikit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


private val Material3DefaultTypography = Typography()

val AppTypography = Material3DefaultTypography.copy(
    headlineLarge = Material3DefaultTypography.headlineLarge.copy(
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = Material3DefaultTypography.headlineMedium.copy(
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = Material3DefaultTypography.titleLarge.copy(
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = Material3DefaultTypography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = Material3DefaultTypography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
    labelLarge = Material3DefaultTypography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    labelMedium = Material3DefaultTypography.labelMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
)
