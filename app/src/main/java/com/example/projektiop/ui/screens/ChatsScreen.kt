package com.example.projektiop.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R
import com.example.projektiop.data.repositories.ChatListItem
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.ui.components.UserAvatar
import com.example.projektiop.ui.viewmodels.ChatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavController, viewModel: ChatsViewModel) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val filteredChats by viewModel.filteredChats.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchText by viewModel.searchText.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshAll()
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
                onSearchTextChanged = { viewModel.searchFor(it) },
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
                                viewModel.refreshAll()
                            }) { Text("Spróbuj ponownie") }
                        }
                    }
                }
                filteredChats.isEmpty() -> {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            stringResource(R.string.no_chats),
                            Modifier.align(Alignment.Center)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredChats, key = { it.id }) { chatData ->
                            ChatItem(
                                chatData = chatData,
                                onClick = {
                                    val route = buildString {
                                        append("chat_detail?")
                                        append("chatId=").append(chatData.id)
                                        if (!chatData.friendId.isNullOrBlank()) {
                                            append("&friendId=").append(chatData.friendId)
                                        }
                                    }
                                    navController.navigate(route)
                                }
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3. Zdjęcie profilowe znajomego
            val avatarUrl = chatData.avatarUrl
            UserAvatar(avatarUrl, modifier = Modifier.size(56.dp))

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
            if (chatData.unread) {
                Spacer(Modifier.width(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(12.dp)
                ) {}
            }
        }
    }
}


// --- Podgląd ---

@Preview(showBackground = true, widthDp = 360, heightDp = 640) // Podgląd na typowym rozmiarze telefonu
@Composable
fun ChatsScreenPreview() {
    // Załóżmy, że masz zdefiniowany MaterialTheme w projekcie
    // Jeśli nie, użyj domyślnego lub zastąp go swoim
    MaterialTheme {
        ChatsScreen(navController = rememberNavController(), viewModel = ChatsViewModel() )
    }
}