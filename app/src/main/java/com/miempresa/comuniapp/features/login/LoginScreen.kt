package com.miempresa.comuniapp.features.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.miempresa.comuniapp.R
import com.miempresa.comuniapp.core.utils.RequestResult
import com.miempresa.comuniapp.ui.components.AppPasswordField
import com.miempresa.comuniapp.ui.components.AppTextField
import com.miempresa.comuniapp.ui.theme.*

@Composable
fun LoginScreen(
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val loginResult by viewModel.loginResult.collectAsState()
    val loadingMessage = stringResource(R.string.common_loading)

    LaunchedEffect(loginResult) {
        loginResult?.let { result ->

            val message = when (result) {
                is RequestResult.Success -> result.message
                is RequestResult.Failure -> result.errorMessage
                is RequestResult.Loading -> loadingMessage
            }

            snackbarHostState.showSnackbar(message)

            viewModel.resetLoginResult()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo_comunidad),
                contentDescription = stringResource(R.string.home_logo_description),
                modifier = Modifier.size(280.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(40.dp))

            AppTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.email.onChange(it) },
                label = stringResource(R.string.login_email_label),
                icon = Icons.Default.Email,
                error = viewModel.email.error
            )

            AppPasswordField(
                value = viewModel.password.value,
                onValueChange = { viewModel.password.onChange(it) },
                label = stringResource(R.string.login_password_label),
                icon = Icons.Default.Lock,
                error = viewModel.password.error
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { viewModel.login() },
                enabled = viewModel.isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = MaterialTheme.shapes.large,
                colors = appPrimaryButtonColors()
            ) {
                Text(
                    stringResource(R.string.login_button),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onForgotPasswordClick) {
                Text(stringResource(R.string.login_forgot_password))
            }

            TextButton(onClick = onRegisterClick) {
                Text(stringResource(R.string.login_no_account))
            }
        }
    }
}