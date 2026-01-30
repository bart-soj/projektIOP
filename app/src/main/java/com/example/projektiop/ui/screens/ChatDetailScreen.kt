package com.example.projektiop.ui.screens

import com.example.projektiop.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.ui.components.ObserveAsEvents
import com.example.projektiop.ui.components.UserAvatar
import com.example.projektiop.ui.viewmodels.ChatDetailUIEvent
import com.example.projektiop.ui.viewmodels.ChatDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Duration


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(navController: NavController, chatId: String?, friendId: String?) {
    val viewModel = koinViewModel<ChatDetailViewModel>(parameters = { parametersOf(chatId, friendId) })

    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val blockInfo by viewModel.blockInfo.collectAsState()
    val chat by viewModel.chat.collectAsState()
    val dateFormatter = viewModel.dateFormatter
    val timeFormatter = viewModel.timeFormatter

    val typing by viewModel.typing.collectAsState()
    val myUser by viewModel.myUser.collectAsState()

    var input by remember { mutableStateOf("") }

    val uiEventFlow = viewModel.uiEventFlow

    ObserveAsEvents(uiEventFlow) { event ->
        when(event) {
            is ChatDetailUIEvent.NavigateToReport -> {
                val target = "report/${event.friendId}?messageId=${event.messageId}&content=${event.content}"
                navController.navigate(target)            }
            is ChatDetailUIEvent.ShowToast -> {}
        }
    }

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
                                        avatarUrl = if (isIncoming && !prevSame) url else myUser!!.profile.avatarUrl,
                                        timeText = if (showTime) timeFormatter.format(m.createdAt) else "",
                                        onReportClick = { if (isIncoming)  viewModel.onReportClick(m.id, m.content) }
                                    )
                                }
                            }
                        }
                }
            }
            if(typing) {
                MessageBubble(
                    text = stringResource(R.string.typing), incoming = true,
                    groupedWithPrev = false,
                    avatarUrl = chat!!.participants.firstOrNull { user -> user.id != myUser!!.id }?.profile?.avatarUrl,
                    timeText = ""
                )
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
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        if (input == "") {
                            viewModel.onTypeStart()
                        } else if(it == "") {
                            viewModel.onTypeStop()
                        }
                        input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    placeholder = {
                        Text(
                            if (isBlocked) stringResource(R.string.sending_unavailable)
                            else stringResource(R.string.write_message)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.4f),
                        errorBorderColor = colorScheme.error,
                        focusedLabelColor = colorScheme.primary
                    ),
                    enabled = !isBlocked
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    modifier = Modifier.width(100.dp).height(48.dp),
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

@Composable
private fun MessageBubble(
    text: String,
    incoming: Boolean,
    groupedWithPrev: Boolean,
    avatarUrl: String?,
    timeText: String,
    onReportClick: (() -> Unit)? = null
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

    var showReportButton by remember { mutableStateOf(false) }

    if(incoming) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            if (!groupedWithPrev) {
                UserAvatar(
                    avatarUrl,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.Start) {
                Row {
                    Surface(
                        color = bg,
                        contentColor = contentColor,
                        shape = shape,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier =
                            (if (groupedWithPrev) Modifier.padding(start = 32.dp) else Modifier)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { showReportButton = true }
                                )
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text(text = text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                }

                Spacer(Modifier.height(2.dp))
                if (timeText.isNotBlank()) {
                    Text(
                        timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            if (groupedWithPrev) Modifier.padding(start = 32.dp)
                            else Modifier.padding(0.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.width(6.dp))

            if (onReportClick != null) {
                AnimatedVisibility(
                    visible = showReportButton,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(0.6f)
                    ) {
                        IconButton(onClick = onReportClick) {
                            Icon(
                                imageVector = Icons.Outlined.Report,
                                contentDescription = null,
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        IconButton(onClick = { showReportButton = false }) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronLeft,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }

    } else {

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = bg,
                    contentColor = contentColor,
                    shape = shape,
                    tonalElevation =  2.dp,
                    shadowElevation = 0.dp,
                    modifier =
                        if (groupedWithPrev) Modifier.padding(end = 32.dp)
                        else Modifier.padding(0.dp)
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
                        modifier =
                            if (groupedWithPrev) Modifier.padding(end = 32.dp)
                            else Modifier.padding(0.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.width(6.dp))

            if (!groupedWithPrev) {
                UserAvatar(
                    avatarUrl,
                    modifier = Modifier.size(32.dp)
                )
            }
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