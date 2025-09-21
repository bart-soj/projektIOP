package com.example.projektiop.screens // Użyj swojej właściwej nazwy pakietu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R // Importuj zasoby R z twojego pakietu

@Composable
fun StartScreen(navController: NavController) {

    Box( // Używamy Box, aby umieścić obraz tła za innymi elementami
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Obraz tła
        Image(
            painter = painterResource(id = R.drawable.start_background), // <<<--- ZMIEŃ NA NAZWĘ SWOJEGO PLIKU
            contentDescription = stringResource(R.string.background_image_description),
            modifier = Modifier.fillMaxSize(), // Rozciągnij obraz na cały ekran
            contentScale = ContentScale.Crop // Dopasuj obraz (Crop przytnie, Fit dostosuje)
            // Możesz eksperymentować z ContentScale.FillBounds, ContentScale.Fit itp.
        )

        // 2. Zawartość na wierzchu (przyciski, logo itp.)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp), // Dodaj padding, aby treść nie dotykała krawędzi
            horizontalAlignment = Alignment.CenterHorizontally, // Wycentruj elementy w kolumnie
            verticalArrangement = Arrangement.Bottom // Umieść elementy na dole (lub .Center, .SpaceBetween, itp.)
        ) {

            // Opcjonalnie: Logo aplikacji lub tytuł
            // Możesz tu dodać Image z logo lub Text
            Text(
                text = stringResource(R.string.app_name_placeholder), // Lub R.string.start_screen_welcome
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary // Użyj koloru pasującego do tła
                // Pamiętaj, że kolor musi być czytelny na tle obrazka! Możesz potrzebować dodać półprzezroczyste tło pod tekstem/przyciskami.
            )

            Spacer(modifier = Modifier.height(128.dp)) // Odstęp między tytułem a przyciskami (dostosuj)

            // 3. Przycisk Zaloguj się
            Button(
                onClick = {
                    navController.navigate("login")
                },
                modifier = Modifier
                    .fillMaxWidth() // Przycisk na całą szerokość (z uwzględnieniem paddingu kolumny)
                    .height(50.dp) // Stała wysokość przycisku
            ) {
                Text(text = stringResource(R.string.login))
            }

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp między przyciskami

            // 4. Przycisk Zarejestruj się
            OutlinedButton( // Użyj OutlinedButton dla wizualnego rozróżnienia
                onClick = {
                    navController.navigate("register")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                // Możesz dostosować kolory obramowania/tekstu, jeśli domyślne nie pasują do tła
                Text(text = stringResource(R.string.register))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("main") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Przejdź do Main Screen")
            }

            Spacer(modifier = Modifier.height(32.dp)) // Dodatkowy odstęp od dołu
        }
    }
}

// --- Podgląd ---

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun StartScreenPreview() {
    MaterialTheme { // Użyj swojego motywu
        // W podglądzie obrazek tła może się nie załadować poprawnie
        // jeśli nie masz go jeszcze w projekcie. Podgląd pokaże układ.
        StartScreen(navController = rememberNavController())
    }
}