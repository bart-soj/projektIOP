package com.example.projektiop.screens

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
import com.example.projektiop.formelements.OutlinedTextFieldWithClearAndError
import com.example.projektiop.formelements.SwitchWithText
import com.example.projektiop.data.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    // --- State Management ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) } // Przechowuje komunikat błędu lub null
    var passwordError by remember { mutableStateOf<String?>(null) } // Przechowuje komunikat błędu lub null
    var apiError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // --- Validation Logic (Example) ---
    // W rzeczywistej aplikacji walidacja byłaby bardziej złożona i prawdopodobnie w ViewModel
    val isEmailValid = remember(email) { email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val isPasswordValid = remember(password) { password.isNotEmpty() }

    // Prosta logika ustawiania błędów (można ją wywołać np. przy próbie logowania)
    fun validateFields() {
        emailError = if (!isEmailValid) "Niepoprawny format email" else null
        passwordError = if (!isPasswordValid) "Hasło jest wymagane" else null
    }

    // --- UI ---
    Surface(
        modifier = Modifier.fillMaxSize(), // Wypełnij cały dostępny obszar
        color = MaterialTheme.colorScheme.background // Użyj koloru tła z motywu
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp) // Dodaj padding do całej kolumny
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Wyśrodkuj zawartość pionowo
        ) {
            // Tytuł
            Text(
                text = stringResource(R.string.login_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp) // Odstęp pod tytułem
            )

            // Pole Email
            OutlinedTextFieldWithClearAndError(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null // Resetuj błąd przy zmianie
                    apiError = null
                },
                label = stringResource(R.string.email_label),
                errorMessage = emailError ?: stringResource(R.string.email_error), // Pokaż błąd walidacji lub domyślny
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość
                isError = emailError != null, // Pokaż błąd, jeśli istnieje
            )

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp między polami

            // Pole Hasło
            OutlinedTextFieldWithClearAndError(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null // Resetuj błąd przy zmianie
                    apiError = null
                },
                label = stringResource(R.string.password_label),
                errorMessage = passwordError ?: stringResource(R.string.password_error), // Pokaż błąd walidacji lub domyślny
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość
                isError = passwordError != null, // Pokaż błąd, jeśli istnieje
                visualTransformation = PasswordVisualTransformation(), // Ukryj hasło
            )

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp

            // Przełącznik "Zapamiętaj mnie" - Wyrównany do lewej
            Row(
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość, aby móc wyrównać
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SwitchWithText(
                    checked = rememberMe,
                    text = stringResource(R.string.remember_me_label),
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

            // Wiersz z przyciskami
            Row (
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Rozłóż przyciski
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
                        validateFields()
                        if (isEmailValid && isPasswordValid) {
                            isLoading = true
                            apiError = null
                            scope.launch {
                                val result = AuthRepository.login(email, password)
                                result.onSuccess { token ->
                                    if (!token.isNullOrBlank()) AuthRepository.saveToken(token)
                                    // Przykład: przejście do głównego ekranu dopiero po sukcesie
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }.onFailure { e ->
                                    apiError = e.message ?: "Nieudane logowanie"
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
                    Text(if (isLoading) "Logowanie..." else stringResource(R.string.login_button_text))
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