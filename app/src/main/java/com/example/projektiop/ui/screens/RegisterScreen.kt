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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.ui.components.ObserveAsEvents
import com.example.projektiop.ui.components.OutlinedTextFieldWithClearAndError
import com.example.projektiop.ui.viewmodels.AuthUiEvent
import com.example.projektiop.ui.viewmodels.AuthViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterScreen(navController: NavController) {
    val viewModel = koinViewModel<AuthViewModel>()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val usernameErrors by viewModel.usernameErrors.collectAsState()
    val emailErrors by viewModel.emailErrors.collectAsState()
    val passwordErrors by viewModel.passwordErrors.collectAsState()
    val registrationError by viewModel.errorMessage.collectAsState()
    val uiEvents = viewModel.authUiFlow

    ObserveAsEvents(uiEvents) { event -> when(event) {
        is AuthUiEvent.NavigateToResend-> {
            if (!event.email.isNullOrBlank())
                navController.navigate("resend_email?email=${event.email}")
            else
                navController.navigate("resend_email")
        }
    }}

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp), // Padding dla całej kolumny
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Wyśrodkowanie zawartości
        ) {
            Text(
                text = stringResource(R.string.register_screen_title), // Użyj zasobu string
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextFieldWithClearAndError(
                value = username,
                onValueChange = {
                    viewModel.onUsernameChange(it)
                    username = it
                },
                label = stringResource(R.string.username_label),
                errorList = usernameErrors.map { stringResource(it) },
                isError = emailErrors.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextFieldWithClearAndError(
                value = email,
                onValueChange = {
                    viewModel.onEmailChange(it)
                    email = it
                },
                label = stringResource(R.string.email_label),
                errorList = emailErrors.map { stringResource(it) },
                isError = emailErrors.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextFieldWithClearAndError(
                value = password,
                onValueChange = {
                    viewModel.onPasswordChange(it)
                    password = it
                },
                label = stringResource(R.string.password_label),
                errorList = passwordErrors.map { stringResource(it) },
                isError = passwordErrors.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.return_button_text))
                }

                Button(
                    onClick = {
                        viewModel.onRegisterClick(username, email, password)
                    },
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.register_button_text))
                }
            }
            if (registrationError != null) {
                Text(registrationError!!, color = colorScheme.error)
            }
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,

            ) {
                Button(
                    onClick = { viewModel.onResendClick() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.background,
                        contentColor = colorScheme.onPrimary
                    )
                )
                { Text(stringResource(R.string.resend_email_title),
                    textDecoration = TextDecoration.Underline ) }
            }
        }
    }
}