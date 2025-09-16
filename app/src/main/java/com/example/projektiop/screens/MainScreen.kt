package com.example.projektiop.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Ikony dla dolnego paska
import androidx.compose.material3.* // Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.data.api.UserProfileResponse
import kotlinx.coroutines.launch
import java.time.LocalDate
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class) // Dla Scaffold, Card, etc.
@Composable
fun MainScreen(navController: NavController) {
    // Stan dla dolnego paska nawigacji (który element jest aktywny)
    // W prawdziwej aplikacji ten stan byłby powiązany z aktualną ścieżką NavController
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    Scaffold(
        bottomBar = {
            // Definicja dolnego paska nawigacji
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues -> // paddingValues zawiera padding od Scaffold (np. dla bottomBar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Zastosuj padding od Scaffold
                .verticalScroll(rememberScrollState()) // Umożliw przewijanie, jeśli treść jest dłuższa
                .padding(horizontal = 16.dp) // Dodatkowy padding poziomy dla treści
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Odstęp od góry

            // 1. Dynamiczna karta profilu
            ProfileCardDynamic(navController)

            Spacer(modifier = Modifier.height(24.dp))
            // Usunięto sekcje filtrów, rozgłaszania oraz przeniesiono przycisk listy znajomych do dolnej nawigacji
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Komponenty pomocnicze ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCardDynamic(navController: NavController, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<UserProfileResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Re-fetch on entering screen (including returning from edit) by keying effect to current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry?.destination?.route) {
        if (navBackStackEntry?.destination?.route == "main") {
            scope.launch {
                loading = true
                error = null
                UserRepository.fetchMyProfile()
                    .onSuccess { profile = it }
                    .onFailure { error = it.message }
                loading = false
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ctx = LocalContext.current
                val rawUrl = profile?.profile?.avatarUrl?.takeIf { it.isNotBlank() }
                val fullUrl = rawUrl?.let { if (it.startsWith("http")) it else "https://hellobeacon.onrender.com$it" }
                // Cache-busting tied to user.updatedAt so image refreshes after avatar change but remains cacheable otherwise
                val versionTag = profile?.updatedAt?.takeIf { !it.isNullOrBlank() }?.hashCode()?.toString()
                val displayUrl = fullUrl?.let { url ->
                    versionTag?.let { v -> if (url.contains('?')) "$url&v=$v" else "$url?v=$v" } ?: url
                }
                if (displayUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(displayUrl)
                            .crossfade(true)
                            .apply {
                                val token = com.example.projektiop.data.repositories.AuthRepository.getToken()
                                if (!token.isNullOrBlank()) {
                                    addHeader("Authorization", "Bearer $token")
                                }
                            }
                            .build(),
                        contentDescription = "Zdjęcie profilowe",
                        placeholder = painterResource(id = R.drawable.avatar_placeholder),
                        error = painterResource(id = R.drawable.avatar_placeholder),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.avatar_placeholder),
                        contentDescription = "Zdjęcie profilowe",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = when {
                        loading -> "Ładowanie..."
                        error != null -> "Błąd"
                        !profile?.effectiveDisplayName.isNullOrBlank() -> profile?.effectiveDisplayName ?: ""
                        else -> stringResource(R.string.profile_name_placeholder)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { navController.navigate("edit_profile") }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edytuj profil")
                }
            }
            // --- Pasek z płcią, miastem i wiekiem ---
            val genderRaw = profile?.profile?.gender
            val genderLabel = when (genderRaw) {
                "male" -> "Mężczyzna"
                "female" -> "Kobieta"
                "other" -> "Inna"
                "prefer_not_to_say" -> "Nie podano"
                else -> null
            }
            val locationVal = profile?.profile?.location?.takeIf { it.isNotBlank() }
            val ageVal = profile?.profile?.birthDate?.let { bd ->
                // Obsłuż format ISO – weź tylko YYYY-MM-DD
                val datePart = bd.take(10)
                try {
                    val ld = LocalDate.parse(datePart, DateTimeFormatter.ISO_DATE)
                    val now = LocalDate.now()
                    val years = Period.between(ld, now).years
                    if (years in 0..150) years else null
                } catch (e: DateTimeParseException) { null }
            }
            val infoItems = listOfNotNull(
                genderLabel?.let { InfoItem(Icons.Default.Person, it) },
                locationVal?.let { InfoItem(Icons.Default.LocationOn, it) },
                ageVal?.let { InfoItem(Icons.Default.Cake, "$it l.") }
            )
            if (infoItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = MaterialTheme.colorScheme.onSurfaceVariant
                    infoItems.forEachIndexed { index, item ->
                        if (index > 0) Spacer(Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
                            Spacer(Modifier.width(4.dp))
                            Text(item.text, style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    loading -> "Pobieranie opisu..."
                    error != null -> error ?: "Błąd pobierania"
                    !profile?.effectiveDescription.isNullOrBlank() -> profile?.effectiveDescription ?: ""
                    else -> stringResource(R.string.profile_description_placeholder)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Zainteresowania",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                val interests = profile?.interests ?: emptyList()
                if (loading) {
                    InterestTag(text = "...")
                } else if (interests.isEmpty()) {
                    InterestTag(text = "Brak")
                } else {
                    interests.take(3).forEachIndexed { index, tag ->
                        if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                        InterestTag(text = tag.toString())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestTag(text: String) { // Prosty Chip/Tag
    SuggestionChip(onClick = { /* Nic nie rób lub pozwól na interakcję */ }, label = { Text(text) })
}

// Usunięto BroadcastToggle


// --- Dolny Pasek Nawigacji (element 9) ---

// Definicja elementów paska
data class BottomNavItem(
    val labelResId: Int, // ID zasobu string
    val icon: ImageVector,
    val route: String // Ścieżka nawigacji
)

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val items = listOf(
        BottomNavItem(R.string.bottom_nav_home, Icons.Default.Home, "main"), // Main
        BottomNavItem(R.string.bottom_nav_friends, Icons.Default.Group, "friends_list"), // Nowy: lista znajomych
        BottomNavItem(R.string.bottom_nav_chats, Icons.Default.Chat, "chats"),
        BottomNavItem(R.string.bottom_nav_broadcast, Icons.Default.BroadcastOnPersonal, "scanner"), // scanner pozostaje
        BottomNavItem(R.string.bottom_nav_settings, Icons.Default.Settings, "settings")
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        val popped = navController.popBackStack(item.route, inclusive = false)
                        if (!popped) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}

// --- Podgląd ---

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme { // Użyj swojego motywu
        MainScreen(navController = rememberNavController())
    }
}

private data class InfoItem(val icon: ImageVector, val text: String)