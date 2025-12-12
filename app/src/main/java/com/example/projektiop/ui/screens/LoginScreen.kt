package com.example.projektiop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.projektiop.R
import com.example.projektiop.data.repositories.AuthEvent
import com.example.projektiop.ui.components.OutlinedTextFieldWithClearAndError
import com.example.projektiop.ui.components.SwitchWithText
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.ui.components.ObserveAsEvents
import com.example.projektiop.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val rememberMe by viewModel.rememberMe.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val emailErrors by viewModel.emailErrors.collectAsState()
    val passwordErrors by viewModel.passwordErrors.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val inputsValid by viewModel.inputsValid.collectAsState()


    val authEvents = viewModel.authEventFlow

    ObserveAsEvents(authEvents) { event -> when(event) {
        is AuthEvent.Error -> {}
        AuthEvent.Logout -> {}
        AuthEvent.Success -> navController.navigate("main")
    } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp)
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tytuł
            Text(
                text = stringResource(R.string.login_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            // Pole Email
            OutlinedTextFieldWithClearAndError(
                value = email,
                onValueChange = {
                    viewModel.onEmailChange(it)
                    email = it
                },
                label = stringResource(R.string.email_label),
                errorList = emailErrors,
                modifier = Modifier.fillMaxWidth(),
                isError = emailErrors.isNotEmpty(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pole Hasło
            OutlinedTextFieldWithClearAndError(
                value = password,
                onValueChange = {
                    viewModel.onPasswordChange(it)
                    password = it
                },
                label = stringResource(R.string.password_label),
                errorList = passwordErrors,
                modifier = Modifier.fillMaxWidth(),
                isError = emailErrors.isNotEmpty(),
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SwitchWithText(
                    checked = rememberMe,
                    text = stringResource(R.string.remember_me),
                    onCheckedChange = { viewModel.rememberMe() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Przycisk Powrót
                Button(
                    onClick = { if (!isLoading) navController.popBackStack() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.return_button_text))
                }

                // Przycisk Zaloguj
                Button(
                    onClick = {
                        viewModel.onLoginClick(email, password)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    enabled = inputsValid && !isLoading
                ) {
                    Text(if (isLoading) stringResource(R.string.logging_in) else stringResource(R.string.login_button_text))
                }
            }
        }
    }
}
