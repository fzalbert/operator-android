package ru.profikrol.operator.feature.rabbitculling

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.PlaceholderScreen

@Composable
fun RabbitCullingScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.rabbit_culling_title),
        onBack = onBack,
    )
}
