package com.example.projektiop.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projektiop.R
import com.example.projektiop.data.repositories.SharedDataSource
import org.koin.compose.koinInject


private const val BASE_URL_KEY = "BASE_URL"


@Composable // TODO() do we need to build url and get token here?
fun UserAvatar(url: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedDataSource  = koinInject<SharedDataSource>()

    val fullUrl = remember(url) {
        url?.let {
            if (it.startsWith("http")) it
            else "${sharedDataSource.get(BASE_URL_KEY, "")}$it"
        }
    }

    val model = remember(fullUrl) {
        ImageRequest.Builder(context)
            .data(fullUrl)
            .crossfade(true)
            .apply {
                val token = sharedDataSource.get("auth_token", "")
                if (token.isNotBlank()) addHeader("Authorization", "Bearer $token")
            }
            .build()
    }

    AsyncImage(
        model = model,
        contentDescription = "Avatar",
        placeholder = painterResource(R.drawable.avatar_placeholder),
        error = painterResource(R.drawable.avatar_placeholder),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(CircleShape)
    )
}
