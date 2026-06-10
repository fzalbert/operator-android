package ru.profikrol.operator.feature.nestpreparation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.PlaceholderScreen

@Composable
fun NestPreparationScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.nest_preparation_title),
        onBack = onBack,
    )
}
