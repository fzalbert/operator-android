package ru.profikrol.operator.uikit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.profikrol.operator.R
import ru.profikrol.operator.core.network.NetworkStatusProvider
import ru.profikrol.operator.uikit.theme.onPrimaryLight
import ru.profikrol.operator.uikit.theme.primaryLight
import ru.profikrol.operator.core.network.LocalNetworkStatusProvider

/**
 * Дефолтный тулбар: кнопка "назад" (опционально), title и subtitle.
 * Если onBack == null — стрелка не рисуется.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val networkStatusProvider = LocalNetworkStatusProvider.current

    val isOnline by networkStatusProvider
        .isOnline()
        .collectAsState(initial = true)

    Column {
        TopAppBar(
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = onPrimaryLight,
                        maxLines = 1,
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = onPrimaryLight,
                            maxLines = 1,
                        )
                    }
                }
            },

            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = onPrimaryLight,
                        )
                    }
                }
            },

            actions = actions,

            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = primaryLight,
                titleContentColor = onPrimaryLight,
                navigationIconContentColor = onPrimaryLight,
                actionIconContentColor = onPrimaryLight,
            ),
        )

        TopBanner(
            visible = !isOnline,
            text = "Нет подключения к интернету",
        )
    }
}