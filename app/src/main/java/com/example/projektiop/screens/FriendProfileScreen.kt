package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.data.api.UserProfileResponse
import com.example.projektiop.data.api.RetrofitInstance
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.res.stringResource
import com.example.projektiop.data.api.Profile
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FriendProfileScreen(
    navController: NavController,
    userId: String,
    usernamePrefill: String? = null,
    displayNamePrefill: String? = null,
    avatarUrlPrefill: String? = null
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<UserProfileResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        scope.launch {
            loading = true
            error = null
            try {
                val fullResp = RetrofitInstance.userApi.getUserById(userId)
                if (fullResp.isSuccessful && fullResp.body() != null) {
                    profile = fullResp.body()
                } else {
                    if (profile == null) {
                        if (displayNamePrefill != null || usernamePrefill != null) {
                            profile = UserProfileResponse(
                                _id = userId,
                                username = usernamePrefill,
                                profile = Profile(displayName = displayNamePrefill),
                                interests = emptyList(),
                                email = null
                            )
                        }
                        val uname = usernamePrefill ?: displayNamePrefill
                        if (!uname.isNullOrBlank()) {
                            val searchResp = RetrofitInstance.userApi.searchUsers(uname)
                            if (searchResp.isSuccessful) {
                                val candidate = searchResp.body().orEmpty().firstOrNull { it._id == userId || it.username == uname }
                                if (candidate != null) {
                                    profile = UserProfileResponse(
                                        _id = candidate._id,
                                        username = candidate.username,
                                        profile = candidate.profile,
                                        interests = emptyList(),
                                        email = null
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.effectiveDisplayName ?: "Profil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Wstecz") }
                }
            )
        }
    ) { paddingValues ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(paddingValues)) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        } else if (error != null) {
            Box(Modifier.fillMaxSize().padding(paddingValues)) { Text(error ?: stringResource(R.string.error), color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center)) }
        } else if (profile == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues)) { Text(stringResource(R.string.no_data), modifier = Modifier.align(Alignment.Center)) }
        } else {
                val p = profile!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val rawUrl = (p.profile?.avatarUrl ?: avatarUrlPrefill)?.takeIf { !it.isNullOrBlank() }
                    val fullUrl = rawUrl?.let { if (it.startsWith("http")) it else "https://hellobeacon.onrender.com$it" }
                    if (fullUrl != null) {
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
                            contentDescription = null,
                            placeholder = painterResource(R.drawable.avatar_placeholder),
                            error = painterResource(R.drawable.avatar_placeholder),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.avatar_placeholder),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.effectiveDisplayName ?: p.username ?: "(bez nazwy)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        val age = p.profile?.birthDate?.let { bd ->
                            val datePart = bd.take(10)
                            try { val ld = LocalDate.parse(datePart, DateTimeFormatter.ISO_DATE); Period.between(ld, LocalDate.now()).years.takeIf { it in 0..150 } } catch (_: Exception) { null }
                        }
                        val gender = when (p.profile?.gender) {
                            "male" -> stringResource(R.string.gender_male)
                            "female" -> stringResource(R.string.gender_female)
                            "other" -> stringResource(R.string.gender_other)
                            "prefer_not_to_say" -> stringResource(R.string.gender_prefer_not_to_say)
                            else -> null
                        }
                        val location = p.profile?.location
                        val infoLine = listOfNotNull(gender, location, age?.let { "$it l." }).joinToString(" • ")
                        if (infoLine.isNotBlank()) Text(infoLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!p.effectiveDescription.isNullOrBlank()) {
                    Text(p.effectiveDescription!!, style = MaterialTheme.typography.bodyMedium)
                }
                if (!p.interests.isNullOrEmpty()) {
                    Column {
                        Text("Zainteresowania", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.interests!!.forEach { ui ->
                                val base = ui.interest.name.ifBlank { stringResource(R.string.profile_unknown_interest) }
                                val label = ui.customDescription?.takeIf { it.isNotBlank() } ?: ""
                                com.example.projektiop.screens.InterestTag(base = base, label = label)
                            }
                        }
                    }
                }
            }
        }
    }
}
