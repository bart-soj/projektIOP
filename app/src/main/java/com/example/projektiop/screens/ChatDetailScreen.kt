package com.example.projektiop.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import coil.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projektiop.data.ChatRepository
import kotlinx.coroutines.launch
import androidx.navigation.NavController

// Prosty ekran szczegółów czatu (placeholder) – do zastąpienia real-time logiką.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(navController: NavController, chatId: String?, friendId: String?) {
    val scope = rememberCoroutineScope()
    var resolvedChatId by remember { mutableStateOf(chatId) }
    var messages by remember { mutableStateOf<List<com.example.projektiop.api.MessageDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }

    LaunchedEffect(friendId, chatId) {
        if (resolvedChatId.isNullOrBlank() && !friendId.isNullOrBlank()) {
            ChatRepository.ensureChatWithUser(friendId)
                .onSuccess { resolvedChatId = it }
                .onFailure { error = it.message }
        }
        val id = resolvedChatId
        if (!id.isNullOrBlank()) {
            loading = true
            ChatRepository.loadMessages(id)
                .onSuccess { messages = it } // już w kolejności rosnącej po dacie
                .onFailure { error = it.message }
            loading = false
        } else {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Czat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error != null -> Text(error ?: "Błąd", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    messages.isEmpty() -> Text("Tutaj pojawi się nowa historia", modifier = Modifier.align(Alignment.Center))
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(messages, key = { _, m -> m._id ?: m.hashCode().toString() }) { index, m ->
                            val isIncoming = m.senderId?._id == friendId
                            val prevSame = index > 0 && (messages[index - 1].senderId?._id == m.senderId?._id)
                            MessageBubble(
                                text = m.content ?: "",
                                incoming = isIncoming,
                                groupedWithPrev = prevSame,
                                avatarUrl = if (isIncoming && !prevSame) m.senderId?.profile?.avatarUrl else null,
                                timestampIso = m.createdAt
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Napisz wiadomość...") }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val id = resolvedChatId
                    if (input.isBlank() || id.isNullOrBlank()) return@Button
                    val content = input
                    input = ""
                    scope.launch {
                        ChatRepository.sendMessage(id, content)
                            .onSuccess { sent -> messages = messages + sent }
                            .onFailure { error = it.message }
                    }
                }, enabled = !resolvedChatId.isNullOrBlank()) { Text("Wyślij") }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MessageBubble(
    text: String,
    incoming: Boolean,
    groupedWithPrev: Boolean,
    avatarUrl: String?,
    timestampIso: String?
) {
    // Kolory i kształt zależne od kierunku
    val bg = if (incoming) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
    val contentColor = if (incoming) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    val baseShape = RoundedCornerShape(18.dp)
    val shape = if (incoming) {
        RoundedCornerShape(
            topStart = if (groupedWithPrev) 6.dp else 18.dp,
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            bottomStart = 0.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = if (groupedWithPrev) 6.dp else 18.dp,
            bottomEnd = 0.dp,
            bottomStart = 18.dp
        )
    }
    val timeText = remember(timestampIso) { parseTimeShort(timestampIso) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End) {
        if (incoming) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "avatar",
                    modifier = Modifier.size(32.dp).padding(end = 6.dp)
                )
            } else if (!groupedWithPrev) {
                // placeholder circle
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(32.dp).padding(end = 6.dp)) {}
            } else Spacer(Modifier.width(38.dp))
        }
        Column(horizontalAlignment = if (incoming) Alignment.Start else Alignment.End) {
            Surface(
                color = bg,
                contentColor = contentColor,
                shape = shape,
                tonalElevation = if (incoming) 0.dp else 2.dp,
                shadowElevation = 0.dp,
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!incoming) {
            Spacer(Modifier.width(6.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@RequiresApi(Build.VERSION_CODES.O)
private fun parseTimeShort(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val inst = Instant.parse(iso)
        timeFormatter.format(inst)
    } catch (e: Exception) { "" }
}
