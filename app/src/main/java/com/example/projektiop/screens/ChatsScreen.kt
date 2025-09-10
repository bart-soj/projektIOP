package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow // Do ucinania tekstu
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R
import com.example.projektiop.data.ChatListItem
import com.example.projektiop.data.ChatRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var chats by remember { mutableStateOf<List<ChatListItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            error = null
            ChatRepository.fetchChats()
                .onSuccess { chats = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    val filteredChatList = chats.filter {
        it.title.contains(searchText, ignoreCase = true) || it.lastMessage.contains(searchText, ignoreCase = true)
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
            when {
                loading -> {
                    Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error ?: "Błąd", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                scope.launch {
                                    loading = true
                                    error = null
                                    ChatRepository.fetchChats()
                                        .onSuccess { chats = it }
                                        .onFailure { error = it.message }
                                    loading = false
                                }
                            }) { Text("Spróbuj ponownie") }
                        }
                    }
                }
                filteredChatList.isEmpty() -> {
                    Box(Modifier.fillMaxSize()) { Text("Brak czatów", Modifier.align(Alignment.Center)) }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredChatList, key = { it.id }) { chatData ->
                            ChatItem(
                                chatData = chatData,
                                onClick = { navController.navigate("chat_detail?chatId=${chatData.id}&friendId=null") }
                            )
                        }
                    }
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
    chatData: ChatListItem,
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
            painter = painterResource(id = R.drawable.avatar_placeholder_background),
            contentDescription = stringResource(R.string.profile_picture_desc),
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
                text = chatData.title,
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