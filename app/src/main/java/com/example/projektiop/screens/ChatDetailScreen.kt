package com.example.projektiop.screens

import com.example.projektiop.R
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
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
import coil.request.ImageRequest
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.projektiop.data.api.MessageDto
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.example.projektiop.data.repositories.BlockInfo
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.FriendshipRepository
import java.time.Duration
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.O)
private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@RequiresApi(Build.VERSION_CODES.O)
private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneId.systemDefault())

@RequiresApi(Build.VERSION_CODES.O)
private fun parseTimeShort(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val inst = Instant.parse(iso)
        timeFormatter.format(inst)
    } catch (e: Exception) { "" }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun parseDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val inst = Instant.parse(iso)
        dateFormatter.format(inst)
    } catch (e: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(navController: NavController, chatId: String?, friendId: String?) {
    val scope = rememberCoroutineScope()
    var resolvedChatId by remember { mutableStateOf(chatId) }
    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var blockInfo by remember { mutableStateOf<BlockInfo?>(null) }

    LaunchedEffect(friendId, chatId) {
        if (resolvedChatId.isNullOrBlank() && !friendId.isNullOrBlank()) {
            ChatRepository.ensureChatWithUser(friendId)
                .onSuccess { resolvedChatId = it }
                .onFailure { error = it.message }
        }
        if (!friendId.isNullOrBlank()) {
            FriendshipRepository.getBlockInfo(friendId).onSuccess { blockInfo = it }
        }
        val id = resolvedChatId
        if (!id.isNullOrBlank()) {
            loading = true
            ChatRepository.loadMessages(id)
                .onSuccess { messages = it } // już w kolejności rosnącej po dacie
                .onFailure { error = it.message }
            loading = false
            // Mark read using last message timestamp
            val lastTimestamp = messages.lastOrNull()?.createdAt
            ChatRepository.markChatRead(id, lastTimestamp)
        } else {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat)) },
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
                    error != null -> Text(
                        error ?: "Błąd",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    messages.isEmpty() -> Text(
                        "Tutaj pojawi się nowa historia",
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else ->
                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                itemsIndexed(
                                    messages,
                                    key = { _, m ->
                                        m._id ?: m.hashCode().toString()
                                    }) { index, m ->
                                    val isIncoming = m.senderId == friendId

                                    val showTime = index == messages.lastIndex || runCatching {
                                        val diff = Duration.between(
                                            Instant.parse(m.createdAt),
                                            Instant.parse(messages[index + 1].createdAt)
                                        ).toMinutes()
                                        diff >= 1
                                    }.getOrDefault(false)

                                    val prevSame = index > 0 && runCatching {
                                        val sameSender = messages[index - 1].senderId == m.senderId
                                        if (!sameSender) false else {
                                            val prevInstant = Instant.parse(messages[index - 1].createdAt)
                                            val currInstant = Instant.parse(m.createdAt)
                                            val diffMinutes = Duration.between(prevInstant, currInstant).toMinutes()
                                            diffMinutes < 1
                                        }
                                    }.getOrDefault(false)

                                    val currentDate = parseDate(m.createdAt)
                                    val prevDate = if (index > 0) parseDate(messages[index - 1].createdAt) else null
                                    val showDateHeader = currentDate != null && currentDate != prevDate
                                    if (showDateHeader) {
                                        DateSeparator(date = currentDate!!)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    val urlShouldBeHere = null // TODO() not working bc of backend update
                                    MessageBubble(
                                        text = m.content ?: "",
                                        incoming = isIncoming,
                                        groupedWithPrev = prevSame,
                                        avatarUrl = if (isIncoming && !prevSame) urlShouldBeHere else null,
                                        timestampIso = if (showTime) m.createdAt else null
                                    )
                                }
                            }
                        }
                }
            }
            val isBlocked = blockInfo?.isBlocked == true
            val blockedByMe = blockInfo?.blockedByMe == true
            if (isBlocked) {
                val msg = if (blockedByMe) {
                    stringResource(R.string.blocked_by_me)
                } else {
                    stringResource(R.string.blocked_by_other)
                }
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text(
                            if (isBlocked) stringResource(R.string.sending_unavailable)
                            else stringResource(R.string.write_message)
                        )
                    },
                    enabled = !isBlocked
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val id = resolvedChatId
                    if (input.isBlank() || id.isNullOrBlank() || isBlocked) return@Button
                    val content = input
                    input = ""
                    scope.launch {
                        ChatRepository.sendMessage(id, content)
                            .onSuccess { sent -> messages = messages + sent }
                            .onFailure { error = it.message }
                        val lastTimestamp = messages.lastOrNull()?.createdAt
                        ChatRepository.markChatRead(id, lastTimestamp)
                    }
                }, enabled = !resolvedChatId.isNullOrBlank() && !isBlocked) { Text(stringResource(R.string.send)) }
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
    val bg = if (incoming) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primary
    val contentColor = if (incoming) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimary
    val shape = if (incoming) {
        RoundedCornerShape(
            topStart = if (groupedWithPrev) 0.dp else 18.dp,
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            bottomStart = 0.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = if (groupedWithPrev) 0.dp else 18.dp,
            bottomEnd = 0.dp,
            bottomStart = 18.dp
        )
    }
    val timeText = remember(timestampIso) { parseTimeShort(timestampIso) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End) {
        if (incoming) {
            if (avatarUrl != null && avatarUrl.isNotBlank()) {
                val raw = avatarUrl
                val fullUrl = if (raw.startsWith("http")) raw else "https://hellobeacon.onrender.com$raw"
                val req = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(fullUrl)
                    .crossfade(true)
                    .apply {
                        val token = com.example.projektiop.data.repositories.AuthRepository.getToken()
                        if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
                    }
                    .build()
                AsyncImage(
                    model = req,
                    contentDescription = "avatar",
                    placeholder = painterResource(com.example.projektiop.R.drawable.avatar_placeholder),
                    error = painterResource(com.example.projektiop.R.drawable.avatar_placeholder),
                    fallback = painterResource(com.example.projektiop.R.drawable.avatar_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            } else if (!groupedWithPrev) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = com.example.projektiop.R.drawable.avatar_placeholder),
                    contentDescription = "avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
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
            if (timeText.isNotBlank()) {
                Text(
                    timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        if (!incoming) {
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun DateSeparator(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}