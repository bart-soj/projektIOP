package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape // Lub RoundedCornerShape jeśli kwadrat z zaokrągleniami
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit // Ikona do edycji
import androidx.compose.material.icons.filled.Info // Przykładowa ikona dla statystyk
import androidx.compose.material.icons.filled.Settings // Ikona ustawień
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R // Upewnij się, że R jest poprawnie zaimportowane
// Jeśli używasz FlowRow, upewnij się, że masz odpowiednią zależność (zazwyczaj jest w foundation)
// import androidx.compose.foundation.layout.FlowRow // Potrzebne dla FlowRow


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // Dodaj ExperimentalLayoutApi dla FlowRow
@Composable
fun ProfileScreen(navController: NavController) {

    // --- Przykładowe dane profilu (zastąp prawdziwymi) ---
    val profileName = stringResource(R.string.profile_name_placeholder) // Użyj istniejącego stringa
    val age = 28
    val gender = "Mężczyzna" // Lub zasób string
    val location = "Wrocław, Polska" // Lub zasób string
    val description = stringResource(R.string.profile_description_placeholder) // Użyj istniejącego
    val interests = listOf(
        stringResource(R.string.profile_interest_1), // Użyj istniejących
        stringResource(R.string.profile_interest_2),
        stringResource(R.string.profile_interest_3)
    )
    val stats = mapOf(
        "Nawiązane kontakty" to 42
    )
    // ----------------------------------------------------

    // Pobieranie aktualnej ścieżki dla dolnego paska nawigacji
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Użyj tego samego komponentu paska nawigacji
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Padding od Scaffold
                .verticalScroll(rememberScrollState()) // Umożliw przewijanie
                .padding(horizontal = 16.dp) // Dodatkowy padding poziomy dla całej treści
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Odstęp od góry

            // --- Górna sekcja: Zdjęcie, Imię, Dane, Ustawienia ---
            ProfileHeader(
                profileName = profileName,
                age = age,
                gender = gender,
                location = location,
                avatarResId = R.drawable.avatar_placeholder, // Użyj placeholdera
                onSettingsClick = {
                    // TODO: Nawiguj do ekranu edycji/ustawień profilu
                    // navController.navigate("profile/edit")
                    println("Settings clicked")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 6. Opis profilu ---
            ProfileSection(title = stringResource(R.string.profile_section_description)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 7. Zainteresowania/Hobby ---
            ProfileSection(title = stringResource(R.string.profile_section_interests)) {
                // Użyj FlowRow, aby tagi zawijały się do następnej linii
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    // Definiuj odstępy między elementami w poziomie i pionie
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Dodano verticalArrangement
                ) {
                    interests.forEach { interest ->
                        InterestTag(text = interest) // Użyj tego samego komponentu co w MainScreen
                    }
                    // Ewentualnie przycisk do edycji zainteresowań
                    // IconButton(onClick = { /* TODO: Nawiguj do edycji zainteresowań */ }) {
                    //     Icon(Icons.Default.Edit, contentDescription = "Edytuj zainteresowania")
                    // }
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            // --- 8. Statystyki konta ---
            ProfileSection(title = stringResource(R.string.profile_section_stats)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.forEach { (label, value) ->
                        StatItem(label = label, value = value.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp na dole przed paskiem nawigacji
        }
    }
}

// --- Komponenty pomocnicze dla ProfileScreen ---

@Composable
fun ProfileHeader(
    profileName: String,
    age: Int,
    gender: String,
    location: String,
    avatarResId: Int,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top // Wyrównaj do góry dla przycisku ustawień
    ) {
        // 1. Zdjęcie profilowe
        Image(
            painter = painterResource(id = avatarResId),
            contentDescription = stringResource(R.string.profile_picture_desc, profileName),
            modifier = Modifier
                .size(80.dp) // Trochę większe niż w liście
                .clip(CircleShape), // Okrągłe zdjęcie
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Kolumna z informacjami tekstowymi
        Column(modifier = Modifier.weight(1f)) { // Pozwól kolumnie zająć dostępną przestrzeń
            // 2. Nazwa profilu
            Text(
                text = profileName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 3. Płeć i wiek
            Text(
                text = "$gender, $age lat", // Połącz dane
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 4. Miejscowość
            Text(
                text = location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 5. Ustawienia Profilu (Przycisk)
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.profile_settings_button_desc)
            )
        }
    }
}

// Komponent do tworzenia sekcji z tytułem
@Composable
fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit // Treść sekcji (lambda)
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp) // Odstęp pod tytułem
        )
        content() // Dodano wywołanie lambdy content, aby sekcja wyświetlała treść
    }
}
// Komponent dla pojedynczej statystyki
@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info, // Przykładowa ikona, można zmieniać
            contentDescription = null, // Opis niekonieczny, jeśli label jest czytelny
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary // Dopasuj kolor
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Używamy tego samego InterestTag co w MainScreen, upewnij się, że jest dostępny
// Jeśli nie, skopiuj go tutaj lub przenieś do wspólnego pliku.
// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// fun InterestTag(text: String) {
//    SuggestionChip(onClick = { /* Można dodać nawigację do edycji opisu */ }, label = { Text(text) })
// }


// --- Podgląd ---

@Preview(showBackground = true, widthDp = 360, heightDp = 740)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme { // Użyj swojego motywu
        ProfileScreen(navController = rememberNavController())
    }
}
