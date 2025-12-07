package com.example.projektiop.ui.screens

import android.content.Context
import android.util.Patterns
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
import com.example.projektiop.R
import com.example.projektiop.ui.components.OutlinedTextFieldWithClearAndError
import com.example.projektiop.ui.components.SwitchWithText
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.ChatUpdateManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isEmailValid = remember(email) { email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val isPasswordValid = remember(password) { password.isNotEmpty() }

    fun validateFields(context: Context) {
        emailError = if (!isEmailValid) context.getString(R.string.invalid_email_format) else null
        passwordError = if (!isPasswordValid) context.getString(R.string.password_required) else null
    }

    // --- UI ---
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
                    email = it
                    emailError = null
                    apiError = null
                },
                label = stringResource(R.string.email_label),
                errorMessage = emailError ?: stringResource(R.string.email_error),
                modifier = Modifier.fillMaxWidth(),
                isError = emailError != null,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pole Hasło
            OutlinedTextFieldWithClearAndError(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                    apiError = null
                },
                label = stringResource(R.string.password_label),
                errorMessage = passwordError ?: stringResource(R.string.password_error),
                modifier = Modifier.fillMaxWidth(),
                isError = passwordError != null,
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
                    onCheckedChange = { rememberMe = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (apiError != null) {
                Text(
                    text = apiError ?: "",
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
                        if (isLoading) return@Button
                        validateFields(context)
                        if (isEmailValid && isPasswordValid) {
                            isLoading = true
                            apiError = null
                            scope.launch {
                                val result = AuthRepository.login(email, password)
                                result.onSuccess { token ->
                                    if (!token.isNullOrBlank()) AuthRepository.saveToken(token, remember = rememberMe)
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }.onFailure { e ->
                                    apiError = e.message ?: context.getString(R.string.login_failed)
                                }
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    enabled = isEmailValid && isPasswordValid && !isLoading
                ) {
                    Text(if (isLoading) stringResource(R.string.logging_in) else stringResource(R.string.login_button_text))
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    val navController = NavController(LocalContext.current)
    LoginScreen(navController)
}