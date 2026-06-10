package ru.profikrol.operator.feature.nestalignment

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.PlaceholderScreen

@Composable
fun NestAlignmentScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.nest_alignment_title),
        onBack = onBack,
    )
}
