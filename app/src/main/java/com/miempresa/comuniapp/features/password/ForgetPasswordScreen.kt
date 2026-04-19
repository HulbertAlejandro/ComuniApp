package com.miempresa.comuniapp.features.password

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import com.miempresa.comuniapp.ui.components.AppTextField
import com.miempresa.comuniapp.ui.theme.appPrimaryButtonColors

@Composable
fun ForgetPasswordScreen(
    onNavigateToReset: () -> Unit = {},
    viewModel: ForgetPasswordViewModel = hiltViewModel()
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val result by viewModel.result.collectAsState()
    val loadingMessage = stringResource(R.string.password_forget_loading)

    LaunchedEffect(result) {
        result?.let {
            val message = when (it) {
                is RequestResult.Success -> it.message
                is RequestResult.Failure -> it.errorMessage
                is RequestResult.Loading -> loadingMessage
            }

            snackbarHostState.showSnackbar(message)

            if (it is RequestResult.Success) {
                onNavigateToReset()
            }

            viewModel.resetResult()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.password_forget_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(30.dp))

            AppTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.email.onChange(it) },
                label = stringResource(R.string.password_forget_email_label),
                icon = Icons.Default.Email,
                error = viewModel.email.error
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { viewModel.sendRecoveryEmail() },
                enabled = viewModel.isFormValid,
                shape = MaterialTheme.shapes.large,
                colors = appPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                if (result is RequestResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.password_forget_button))
                }
            }
        }
    }
}
