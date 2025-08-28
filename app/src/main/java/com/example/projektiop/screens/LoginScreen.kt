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
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.projektiop.R
import com.example.projektiop.formelements.OutlinedTextFieldWithClearAndError
import com.example.projektiop.formelements.SwitchWithText

@Composable
fun LoginScreen(navController: NavController) {
    // --- State Management ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) } // Przechowuje komunikat błędu lub null
    var passwordError by remember { mutableStateOf<String?>(null) } // Przechowuje komunikat błędu lub null

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
                },
                label = stringResource(R.string.email_label),
                errorMessage = emailError ?: stringResource(R.string.email_error), // Pokaż błąd walidacji lub domyślny
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość
                isError = emailError != null, // Pokaż błąd, jeśli istnieje
                //keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp między polami

            // Pole Hasło
            OutlinedTextFieldWithClearAndError(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null // Resetuj błąd przy zmianie
                },
                label = stringResource(R.string.password_label),
                errorMessage = passwordError ?: stringResource(R.string.password_error), // Pokaż błąd walidacji lub domyślny
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość
                isError = passwordError != null, // Pokaż błąd, jeśli istnieje
                visualTransformation = PasswordVisualTransformation(), // Ukryj hasło
                //keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp

            // Przełącznik "Zapamiętaj mnie" - Wyrównany do lewej
            Row(
                modifier = Modifier.fillMaxWidth(), // Wypełnij szerokość, aby móc wyrównać
                verticalAlignment = Alignment.CenterVertically,
                // horizontalArrangement = Arrangement.Start // Domyślnie jest Start
            ) {
                SwitchWithText(
                    checked = rememberMe,
                    text = stringResource(R.string.remember_me_label),
                    onCheckedChange = { rememberMe = it }
                )
            }

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
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.return_button_text))
                }

                // Przycisk Zaloguj
                Button(
                    onClick = {
                        validateFields() // Uruchom walidację
                        if (isEmailValid && isPasswordValid) {
                            // Logika logowania - nawiguj tylko jeśli pola są poprawne
                            navController.navigate("scanner")
                        } else {
                            // Opcjonalnie: Pokaż Snackbar/Toast z informacją o błędach
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = 8.dp), // Daj wagę i odstęp
                    // enabled = isEmailValid && isPasswordValid, // Opcjonalnie: wyłącz przycisk, jeśli dane są niepoprawne
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.login_button_text))
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