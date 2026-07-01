package ru.profikrol.operator.uikit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.uikit.theme.statusBannerWarningLight

@Composable
fun TopBanner(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300),
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300),
        ),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = statusBannerWarningLight,
            shape = RoundedCornerShape(
                bottomStart = 12.dp,
                bottomEnd = 12.dp,
            ),
            shadowElevation = 4.dp,
        ) {
            Text(
                text = text,
                color = Color.White,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            )
        }
    }
}