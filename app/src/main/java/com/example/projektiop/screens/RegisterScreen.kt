package com.example.projektiop.screens

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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.projektiop.data.AuthRepository
import com.example.projektiop.formelements.OutlinedTextFieldWithClearAndError
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(navController: NavController) {
    // --- State Management ---
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var registrationError by remember { mutableStateOf<String?>(null) }

    // --- Validation Logic (Example) ---
    // W rzeczywistej aplikacji walidacja byłaby bardziej złożona (np. w ViewModel)
    val isUsernameValid = remember(username) { username.isNotBlank() } // Proste sprawdzenie czy nie jest pusty
    val isEmailValid = remember(email) { email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val isPasswordValid = remember(password) {
        val hasMinimumLength = password.length >= 8
        val hasLowercase = password.any { it.isLowerCase() }
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        hasMinimumLength && hasLowercase && hasUppercase && hasDigit
    }

    // Funkcja do walidacji pól, wywoływana przy próbie rejestracji
    fun validateFields() {
        usernameError = if (!isUsernameValid) "Niepoprawna nazwa użytkownika" else null
        emailError = if (!isEmailValid) "Niepoprawny format email" else null
        passwordError = if (!isPasswordValid) {
            "Hasło musi mieć min. 8 znaków, zawierać dużą i małą literę oraz cyfrę."
        } else null
    }

    // --- UI ---
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
                    usernameError = null // Resetuj błąd przy zmianie
                },
                label = stringResource(R.string.username_label),
                errorMessage = usernameError, // Pokaż błąd walidacji lub null
                isError = usernameError != null,
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość
            )

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp

            // Pole Email
            OutlinedTextFieldWithClearAndError(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null // Resetuj błąd przy zmianie
                },
                label = stringResource(R.string.email_label),
                errorMessage = emailError, // Pokaż błąd walidacji lub null
                isError = emailError != null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp

            // Pole Hasło
            OutlinedTextFieldWithClearAndError(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null // Resetuj błąd przy zmianie
                },
                label = stringResource(R.string.password_label),
                errorMessage = passwordError, // Pokaż błąd walidacji lub null
                isError = passwordError != null,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation() // Maskowanie hasła
            )

            Spacer(modifier = Modifier.height(24.dp)) // Większy odstęp przed przyciskami

            // Wiersz z przyciskami
            Row (
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Rozłóż przyciski
            ) {
                // Przycisk Powrót
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f).padding(end = 8.dp), // Daj wagę i odstęp
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.return_button_text))
                }

                // Przycisk Zarejestruj
                Button(
                    onClick = {
                        validateFields() // Uruchom walidację
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
                            registrationError = "Popraw dane w formularzu."
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