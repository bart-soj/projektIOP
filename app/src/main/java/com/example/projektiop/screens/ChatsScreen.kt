package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Import dla LazyColumn items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search // Ikona wyszukiwania
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue // Potrzebne dla TextField
import androidx.compose.ui.text.style.TextOverflow // Do ucinania tekstu
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R // Upewnij się, że R jest poprawnie zaimportowane

// --- Przykładowa struktura danych dla elementu listy ---
data class ChatPreviewData(
    val id: String, // Unikalny identyfikator czatu/znajomego
    val friendName: String,
    val lastMessage: String,
    val avatarResId: Int // ID zasobu obrazka (drawable)
)

// --- Przykładowe dane (zastąp prawdziwymi danymi) ---
val sampleChatListData = listOf(
    ChatPreviewData("1", "Anna Kowalska", "Hej! Co tam?", R.drawable.avatar_placeholder_background), // Użyj placeholdera lub dodaj inne obrazki
    ChatPreviewData("2", "Piotr Nowak", "Widziałeś ten nowy film?", R.drawable.avatar_placeholder_background),
    ChatPreviewData("3", "Ewa Zielińska", "Dzięki za pomoc wczoraj :)", R.drawable.avatar_placeholder_background),
    ChatPreviewData("4", "Janusz Programista", "OK, sprawdzę to jutro.", R.drawable.avatar_placeholder_background),
    ChatPreviewData("5", "Alicja Testerka", "Zgłosiłam buga #123", R.drawable.avatar_placeholder_background),
    ChatPreviewData("6", "Krypto Entuzjasta", "...", R.drawable.avatar_placeholder_background)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavController) {

    // Stan dla pola wyszukiwania
    var searchText by remember { mutableStateOf("") }

    // Pobieranie aktualnej ścieżki dla dolnego paska nawigacji
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Filtrowanie listy na podstawie wyszukiwania (prosta implementacja)
    val filteredChatList = sampleChatListData.filter {
        it.friendName.contains(searchText, ignoreCase = true) ||
                it.lastMessage.contains(searchText, ignoreCase = true)
    }

    Scaffold(
        bottomBar = {
            // Użyj tego samego komponentu paska nawigacji co w MainScreen
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Zastosuj padding od Scaffold
            // Nie dodajemy .verticalScroll(), bo użyjemy LazyColumn
        ) {
            // 1 & 2. Pole wyszukiwania i przycisk (w jednym komponencie TextField)
            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { searchText = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Spacer między wyszukiwaniem a listą
            Spacer(modifier = Modifier.height(8.dp))

            // 3, 4, 5. Lista znajomych/czatów (używamy LazyColumn dla wydajności)
            // 6. Pasek przewijania jest automatycznie obsługiwany przez LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxSize(), // Wypełnij pozostałe miejsce
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), // Padding dla elementów listy
                verticalArrangement = Arrangement.spacedBy(12.dp) // Odstęp między elementami listy
            ) {
                items(filteredChatList, key = { it.id }) { chatData -> // Użyj klucza dla lepszej wydajności
                    ChatItem(
                        chatData = chatData,
                        onClick = {
                            // TODO: Nawiguj do szczegółów czatu, np.:
                            // navController.navigate("chatDetail/${chatData.id}")
                            println("Clicked chat: ${chatData.friendName}") // Tymczasowe logowanie
                        }
                    )
                }
            }
        }
    }
}

// --- Komponenty pomocnicze dla ChatsScreen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.search_label)) }, // Dodaj zasób string dla "Szukaj..."
        leadingIcon = { // Ikona wewnątrz pola tekstowego
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_icon_desc) // Dodaj opis dla dostępności
            )
        },
        singleLine = true // Zapobiega wieloliniowości
    )
}

@Composable
fun ChatItem(
    chatData: ChatPreviewData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Cały wiersz klikalny
            .padding(vertical = 8.dp), // Dodaj trochę pionowego paddingu
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 3. Zdjęcie profilowe znajomego
        Image(
            painter = painterResource(id = chatData.avatarResId),
            contentDescription = stringResource(R.string.profile_picture_desc, chatData.friendName), // Opis dla dostępności
            modifier = Modifier
                .size(56.dp) // Nieco mniejsze niż w profilu głównym
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Kolumna na nazwę i ostatnią wiadomość
        Column(
            modifier = Modifier.weight(1f) // Zajmij dostępną przestrzeń, aby tekst się zawijał/ucinał
        ) {
            // 4. Nazwa znajomego
            Text(
                text = chatData.friendName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1, // Maksymalnie jedna linia
                overflow = TextOverflow.Ellipsis // Utnij, jeśli za długie
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 5. Ostatnia wiadomość z czatu
            Text(
                text = chatData.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Stonowany kolor
                maxLines = 1, // Maksymalnie jedna linia
                overflow = TextOverflow.Ellipsis // Utnij, jeśli za długie
            )
        }
        // Można tu dodać np. wskaźnik nieprzeczytanych wiadomości lub czas ostatniej wiadomości
    }
}


// --- Podgląd ---

@Preview(showBackground = true, widthDp = 360, heightDp = 640) // Podgląd na typowym rozmiarze telefonu
@Composable
fun ChatsScreenPreview() {
    // Załóżmy, że masz zdefiniowany MaterialTheme w projekcie
    // Jeśli nie, użyj domyślnego lub zastąp go swoim
    MaterialTheme {
        ChatsScreen(navController = rememberNavController())
    }
}

// --- Dodaj te zasoby string do pliku strings.xml ---
/*
<resources>
    ... inne stringi ...
    <string name="search_label">Szukaj...</string>
    <string name="search_icon_desc">Ikona wyszukiwania</string>
    <string name="profile_picture_desc">Zdjęcie profilowe %1$s</string> // %1$s zostanie zastąpione nazwą znajomego
    // Dodaj stringi dla dolnego paska nawigacji jeśli jeszcze ich nie masz
    <string name="bottom_nav_home">Główna</string>
    <string name="bottom_nav_profile">Profil</string>
    <string name="bottom_nav_chats">Czaty</string>
    <string name="bottom_nav_broadcast">Rozgłaszanie</string>
    <string name="bottom_nav_settings">Ustawienia</string>
    // Dodaj stringi używane w MainScreen (jeśli ich nie ma)
    <string name="filter_settings_label">Ustawienia filtrów</string>
    <string name="profile_name_placeholder">Jan Kowalski</string>
    <string name="profile_description_placeholder">Opis profilu użytkownika, może być dłuższy.</string>
    <string name="profile_interests_label">Zainteresowania:</string>
    <string name="profile_interest_1">Programowanie</string>
    <string name="profile_interest_2">Gry</string>
    <string name="profile_interest_3">Muzyka</string>
    <string name="broadcasting_label">Rozgłaszanie lokalizacji</string>


</resources>
*/