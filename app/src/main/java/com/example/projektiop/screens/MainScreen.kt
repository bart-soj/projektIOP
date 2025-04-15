package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Ikony dla dolnego paska
import androidx.compose.material3.* // Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R

@OptIn(ExperimentalMaterial3Api::class) // Dla Scaffold, Card, etc.
@Composable
fun MainScreen(navController: NavController) {

    // Stany dla elementów interaktywnych
    var broadcastMessage by remember { mutableStateOf("") }
    var isBroadcasting by remember { mutableStateOf(false) }
    // Stan dla dolnego paska nawigacji (który element jest aktywny)
    // W prawdziwej aplikacji ten stan byłby powiązany z aktualną ścieżką NavController
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    Scaffold(
        bottomBar = {
            // Definicja dolnego paska nawigacji
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues -> // paddingValues zawiera padding od Scaffold (np. dla bottomBar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Zastosuj padding od Scaffold
                .verticalScroll(rememberScrollState()) // Umożliw przewijanie, jeśli treść jest dłuższa
                .padding(horizontal = 16.dp) // Dodatkowy padding poziomy dla treści
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Odstęp od góry

            // 1. Karta Profilu (elementy 1-5)
            ProfileCard()

            Spacer(modifier = Modifier.height(24.dp))


            Spacer(modifier = Modifier.height(16.dp))

            // 7. Ustawienia filtrów (jako przycisk tekstowy)
            TextButton(
                onClick = { /* TODO: Nawiguj do ustawień filtrów */ },
                modifier = Modifier.align(Alignment.Start) // Wyrównaj do lewej
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.filter_settings_label))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 8. Włącznik rozgłaszania
            BroadcastToggle(
                isBroadcasting = isBroadcasting,
                onCheckedChange = { isBroadcasting = it }
            )

            Spacer(modifier = Modifier.height(16.dp)) // Odstęp na dole przed bottom bar
        }
    }
}

// --- Komponenty pomocnicze ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCard(modifier: Modifier = Modifier) {
    Card( // Użyj Card dla wizualnego odseparowania
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 2. Zdjęcie profilowe
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder_background), // Zastąp prawdziwym obrazkiem
                    contentDescription = "Zdjęcie profilowe",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape), // Okrągły obrazek
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                // 3. Nazwa profilu
                Text(
                    text = stringResource(R.string.profile_name_placeholder),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 4. Opis profilu
            Text(
                text = stringResource(R.string.profile_description_placeholder),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            // 5. Zainteresowania
            Text(
                text = stringResource(R.string.profile_interests_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Prosty sposób wyświetlenia zainteresowań (można użyć Chipów, FlowRow etc.)
            Row {
                InterestTag(text = stringResource(R.string.profile_interest_1))
                Spacer(modifier = Modifier.width(8.dp))
                InterestTag(text = stringResource(R.string.profile_interest_2))
                Spacer(modifier = Modifier.width(8.dp))
                InterestTag(text = stringResource(R.string.profile_interest_3))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestTag(text: String) { // Prosty Chip/Tag
    SuggestionChip(onClick = { /* Nic nie rób lub pozwól na interakcję */ }, label = { Text(text) })
}

@Composable
fun BroadcastToggle(
    isBroadcasting: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Rozmieść elementy
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ikona statusu rozgłaszania (może się zmieniać)
            Icon(
                imageVector = if (isBroadcasting) Icons.Default.WifiTethering  else Icons.Default.PortableWifiOff,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.broadcasting_label),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Switch(
            checked = isBroadcasting,
            onCheckedChange = onCheckedChange
        )
    }
}


// --- Dolny Pasek Nawigacji (element 9) ---

// Definicja elementów paska
data class BottomNavItem(
    val labelResId: Int, // ID zasobu string
    val icon: ImageVector,
    val route: String // Ścieżka nawigacji
)

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val items = listOf(
        BottomNavItem(R.string.bottom_nav_home, Icons.Default.Home, "main"), // Załóżmy, że MainScreen ma route "main"
        BottomNavItem(R.string.bottom_nav_profile, Icons.Default.Person, "profile"),
        BottomNavItem(R.string.bottom_nav_chats, Icons.Default.Chat, "chats"),
        BottomNavItem(R.string.bottom_nav_broadcast, Icons.Default.BroadcastOnPersonal, "broadcast_control"),
        BottomNavItem(R.string.bottom_nav_settings, Icons.Default.Settings, "start") // Użyj odpowiedniej ścieżki
    )

    NavigationBar { // Material 3 Bottom Navigation Bar
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
                //label = { Text(stringResource(item.labelResId)) },
                selected = currentRoute == item.route, // Zaznacz, jeśli aktualna ścieżka pasuje
                onClick = {
                    // Nawiguj tylko, jeśli nie jesteśmy już na tym ekranie
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Wyczyść stos do ekranu startowego grafu, by uniknąć budowania wielkiego stosu
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true // Zapisz stan ekranów na stosie
                            }
                            // Unikaj wielokrotnego tworzenia tego samego ekranu na szczycie stosu
                            launchSingleTop = true
                            // Przywróć stan, jeśli wracamy do ekranu
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

// --- Podgląd ---

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme { // Użyj swojego motywu
        MainScreen(navController = rememberNavController())
    }
}