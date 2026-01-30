package com.example.projektiop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.ui.components.GlassPanel
import com.example.projektiop.ui.viewmodels.AuthViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.*
import com.example.projektiop.ui.components.LanguageButton
import com.example.projektiop.ui.viewmodels.LanguageViewModel

@Composable
fun StartScreen(navController: NavController) {
    val viewModel = koinViewModel<AuthViewModel>()
    val loading by viewModel.loading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Image(
            painter = painterResource(id = R.drawable.start_background2),
            contentDescription = stringResource(R.string.background_image_description),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                val languageViewModel = koinViewModel<LanguageViewModel>()
                LanguageButton(languageViewModel)
            }

            Spacer(modifier = Modifier.weight(1f))

            GlassPanel {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = stringResource(R.string.app_logo_image_description),
                    modifier = Modifier.size(128.dp)
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Spacer(modifier = Modifier.height(52.dp)) // Odstęp między tytułem a przyciskami

                when (loading) {
                    true -> { CircularProgressIndicator() }
                    false -> {
                        // Przycisk Zaloguj się
                        Button(
                            onClick = {
                                navController.navigate("login")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor =  MaterialTheme.colorScheme.onSecondary,
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(text = stringResource(R.string.start_screen_login_button))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Przycisk Zarejestruj się
                        OutlinedButton(
                            onClick = {
                                navController.navigate("register")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor =  MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(text = stringResource(R.string.start_screen_register_button))
                        }
                    }
                }

            }
        }
        Spacer(modifier = Modifier.height(32.dp)) // Dodatkowy odstęp od dołu
    }
}