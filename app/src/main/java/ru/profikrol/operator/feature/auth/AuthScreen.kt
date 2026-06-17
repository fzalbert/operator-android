package ru.profikrol.operator.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.PrimaryButton
import ru.profikrol.operator.uikit.tokens.Spacing

@Preview
@Composable
fun AuthScreen(
    onLoggedIn: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                AuthEvent.LoggedIn -> onLoggedIn()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Логотип-эмодзи
        Text(
            text = stringResource(R.string.auth_logo),
            fontSize = 96.sp,
        )

        Spacer(Modifier.height(Spacing.md))

        // Название продукта
        Text(
            text = stringResource(R.string.auth_brand_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(Spacing.xs))

        // Подзаголовок
        Text(
            text = stringResource(R.string.auth_brand_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.lg))

        OutlinedTextField(
            value = state.login,
            onValueChange = viewModel::onLoginChange,
            label = { Text(stringResource(R.string.auth_login_label)) },
            singleLine = true,
            enabled = !state.isLoading,
            isError = state.isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.md))

        //TODO: password add show button
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.auth_password_label)) },
            singleLine = true,
            enabled = !state.isLoading,
            isError = state.isError,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.errorText != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = state.errorText!!,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        PrimaryButton(
            text = stringResource(
                if (state.isLoading) R.string.auth_submit_loading else R.string.auth_submit
            ),
            onClick = viewModel::onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
