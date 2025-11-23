package com.example.projektiop.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projektiop.R
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.FriendItem
import com.example.projektiop.data.repositories.PendingRequestItem
import com.example.projektiop.data.repositories.SharedPreferencesRepository

private const val BASE_URL_KEY = "BASE_URL"

@Composable
fun FriendCard(
    friend: FriendItem,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit = {},

    onChatClick: (() -> Unit)? = null,
    onRemoveClick: (() -> Unit)? = null,
    onBlockClick: (() -> Unit)? = null,
    onUnblockClick: (() -> Unit)? = null,

    onInviteClick: (() -> Unit)? = null,
    isInviteSent: Boolean = false,
    isAlreadyFriend: Boolean = false
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val showMenu = onRemoveClick != null || onBlockClick != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(url = friend.avatarUrl, size = 56.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(friend.displayName, style = MaterialTheme.typography.titleMedium)
                Text(friend.username, style = MaterialTheme.typography.bodySmall)
            }

            if (onInviteClick != null) {
                TextButton(
                    onClick = onInviteClick,
                    enabled = !isInviteSent && !isAlreadyFriend
                ) {
                    val label = stringResource(
                        if (isAlreadyFriend) R.string.already_friends
                        else if (isInviteSent) R.string.invite_sent
                        else R.string.add
                    )
                    Text(label)
                }
            }

            if (onChatClick != null) {
                TextButton(onClick = onChatClick) {
                    Text(stringResource(R.string.chat))
                }
            }

            if (onUnblockClick != null) {
                TextButton(onClick = onUnblockClick) {
                    Text(stringResource(R.string.unlock))
                }
            }

            if (showMenu) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (onRemoveClick != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove)) },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveClick()
                                }
                            )
                        }

                        if (onBlockClick != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.block)) },
                                onClick = {
                                    menuExpanded = false
                                    onBlockClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingRequestCard(
    item: PendingRequestItem,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(url = item.avatarUrl, size = 48.dp)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                Text(item.username, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onReject) { Text(stringResource(R.string.reject)) }
                Button(onClick = onAccept) { Text(stringResource(R.string.accept)) }
            }
        }
    }
}

@Composable
fun UserAvatar(url: String?, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current

    val fullUrl = remember(url) {
        url?.let {
            if (it.startsWith("http")) it
            else "${SharedPreferencesRepository.get(BASE_URL_KEY, "")}$it"
        }
    }

    val model = remember(fullUrl) {
        ImageRequest.Builder(context)
            .data(fullUrl)
            .crossfade(true)
            .apply {
                val token = AuthRepository.getToken()
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
            }
            .build()
    }

    AsyncImage(
        model = model,
        contentDescription = "Avatar",
        placeholder = painterResource(R.drawable.avatar_placeholder),
        error = painterResource(R.drawable.avatar_placeholder),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    )
}