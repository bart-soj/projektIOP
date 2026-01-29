package com.example.projektiop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.projektiop.R
import com.example.projektiop.data.repositories.ChatListItem
import com.example.projektiop.ui.components.BottomNavigationBar
import com.example.projektiop.ui.components.UserAvatar
import com.example.projektiop.ui.viewmodels.ChatsViewModel
import org.koin.androidx.compose.koinViewModel
import com.example.projektiop.ui.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavController) {
    val viewModel = koinViewModel<ChatsViewModel>()
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
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                searchText = searchText,
                onSearchTextChanged = { viewModel.searchFor(it) },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> {
                    Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
                }

                error != null -> {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(error ?: "Błąd", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                viewModel.refreshAll()
                            }) { Text(text = stringResource(R.string.retry)) }
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
                                        append("chat_detail/")
                                        .append(chatData.id)
                                        if (!chatData.friendId.isNullOrBlank()) {
                                            append("?friendId=").append(chatData.friendId)
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
            val avatarUrl = chatData.avatarUrl
            UserAvatar(avatarUrl, modifier = Modifier.size(56.dp))

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = chatData.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = chatData.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (chatData.unread) {
                Spacer(Modifier.width(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    tonalElevation = 0.dp,
                    modifier = Modifier.size(12.dp)
                ) {

                }
            }
        }
    }
}