package com.example.projektiop.screens

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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.formelements.OutlinedTextFieldWithClearAndError
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var registrationError by remember { mutableStateOf<String?>(null) }
    val isUsernameValid = remember(username) { username.isNotBlank() }
    val isEmailValid = remember(email) { email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val isPasswordValid = remember(password) {
        val hasMinimumLength = password.length >= 8
        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        hasMinimumLength && hasLowercase && hasUppercase && hasDigit
    }

    fun validateFields(context: Context) {
        usernameError = if (!isUsernameValid) context.getString(R.string.error_invalid_username) else null
        emailError = if (!isEmailValid) context.getString(R.string.error_invalid_email) else null
        passwordError = if (!isPasswordValid) {
            context.getString(R.string.error_invalid_password)
        } else null
    }


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
            // Tytuł
            Text(
                text = stringResource(R.string.register_screen_title), // Użyj zasobu string
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Pole Nazwa użytkownika
            OutlinedTextFieldWithClearAndError(
                value = username,
                onValueChange = {
                    username = it
                    usernameError = null
                },
                label = stringResource(R.string.username_label),
                errorMessage = usernameError,
                isError = usernameError != null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pole Email
            OutlinedTextFieldWithClearAndError(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = stringResource(R.string.email_label),
                errorMessage = emailError,
                isError = emailError != null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pole Hasło
            OutlinedTextFieldWithClearAndError(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = stringResource(R.string.password_label),
                errorMessage = passwordError,
                isError = passwordError != null,
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
                        contentColor = colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.return_button_text))
                }

                Button(
                    onClick = {
                        validateFields(context)
                        if (isUsernameValid && isEmailValid && isPasswordValid) {
                            coroutineScope.launch {
                                val result = AuthRepository.register(username, email, password)
                                result.onSuccess {
                                    navController.navigate("scanner")
                                }.onFailure {
                                    registrationError = it.message
                                }
                            }
                        } else {
                            registrationError = context.getString(R.string.form_fields_invalid)
                        }
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
        }
    }
}

@Preview
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(navController = NavController(context = LocalContext.current))
}