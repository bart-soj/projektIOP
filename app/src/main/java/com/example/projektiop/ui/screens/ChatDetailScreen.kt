package com.example.projektiop.ui.screens

import com.example.projektiop.R
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.ui.components.UserAvatar
import com.example.projektiop.ui.viewmodels.ChatDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(navController: NavController, chatId: String?, friendId: String?) {
    val viewModel = koinViewModel<ChatDetailViewModel>(parameters = { parametersOf(friendId) })

    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val blockInfo by viewModel.blockInfo.collectAsState()
    val chat by viewModel.chat.collectAsState()
    val dateFormatter = viewModel.dateFormatter
    val timeFormatter = viewModel.timeFormatter

    var input by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                        error ?: stringResource(R.string.error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    messages.isEmpty() -> Text(
                        text = stringResource(R.string.chat_empty_state),
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else ->
                        Box(Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                itemsIndexed(messages, key = { _, m -> m.id }) { index, m ->
                                    val isIncoming = m.senderId == friendId

                                    val showTime = index == messages.lastIndex || runCatching {
                                        val diff = Duration.between(
                                            m.createdAt,
                                            messages[index + 1].createdAt
                                        ).toMinutes()
                                        diff >= 1
                                    }.getOrDefault(false)

                                    val prevSame = index > 0 && runCatching {
                                        val sameSender = messages[index - 1].senderId == m.senderId
                                        if (!sameSender) false else {
                                            val prevInstant = messages[index - 1].createdAt
                                            val currInstant = m.createdAt
                                            val diffMinutes = Duration.between(prevInstant, currInstant).toMinutes()
                                            diffMinutes < 1
                                        }
                                    }.getOrDefault(false)
                                    val currentDate = dateFormatter.format(m.createdAt)
                                    val prevDate = if (index > 0)
                                        dateFormatter.format(messages[index - 1].createdAt) else null
                                    val showDateHeader = currentDate != prevDate
                                    if (showDateHeader) {
                                        DateSeparator(date = currentDate)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    val url: String? = chat!!.participants.firstOrNull { user -> user.id == m.senderId }?.profile?.avatarUrl

                                    MessageBubble(
                                        text = m.content,
                                        incoming = isIncoming,
                                        groupedWithPrev = prevSame,
                                        avatarUrl = if (isIncoming && !prevSame) url else null,
                                        timeText = if (showTime) timeFormatter.format(m.createdAt) else ""
                                    )
                                }
                            }
                        }
                }
            }

            val isBlocked = blockInfo != null
            if (isBlocked) {
                val msg = if (blockInfo?.blockedByMe == true) {
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
                Button(
                    onClick = {
                        viewModel.onSendClick(input)
                        input = "" },
                    enabled = !chat?.id.isNullOrBlank() && !isBlocked && viewModel.validateInput(input),
                    colors = buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ))
                { Text(stringResource(R.string.send)) }
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
    timeText: String
) {
    val bg = if (incoming) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.tertiary
    val contentColor = if (incoming) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onTertiary
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End) {
        if (incoming) {
            if (!groupedWithPrev) {
                UserAvatar(
                    avatarUrl,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (incoming) Alignment.Start else Alignment.End) {
            Surface(
                color = bg,
                contentColor = contentColor,
                shape = shape,
                tonalElevation = if (incoming) 0.dp else 2.dp,
                shadowElevation = 0.dp,
                modifier = if (groupedWithPrev)
                    if (incoming) {
                        Modifier.padding(start = 32.dp)
                    } else {
                        Modifier.padding(end = 32.dp)
                    }
                else Modifier
                    .padding(0.dp)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (groupedWithPrev) if (incoming) {
                        Modifier.padding(start = 32.dp)
                    } else {
                        Modifier.padding(end = 32.dp)
                    }
                    else Modifier
                        .padding(0.dp)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        if (!incoming) {
            Spacer(Modifier.width(6.dp))
        }
        if (!incoming && !groupedWithPrev) {
            UserAvatar(
                avatarUrl,
                modifier = Modifier.size(32.dp)
            )
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