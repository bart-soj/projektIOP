package com.example.projektiop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Ikony dla dolnego paska
import androidx.compose.material3.* // Material 3
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.projektiop.data.db.objects.Gender
import com.example.projektiop.util.realmInstantToMongoTimestamp
import com.example.projektiop.ui.components.PullToRefresh
import com.example.projektiop.ui.components.UserAvatar
import com.example.projektiop.ui.viewmodels.MainViewModel
import org.koin.androidx.compose.koinViewModel


private const val BASE_URL_KEY: String = "BASE_URL"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val viewModel = koinViewModel<MainViewModel>()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }


    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        val isLoading by viewModel.loading.collectAsState()

        PullToRefresh(
            refreshing = isLoading,
            onRefresh = { viewModel.refreshProfile() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Karta profilu
            ProfileCardDynamic(
                navController,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// --- Komponenty pomocnicze ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileCardDynamic(navController: NavController, modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val user by viewModel.user.collectAsState()
    val interests by viewModel.myInterests.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rawUrl = user?.profile?.avatarUrl?.takeIf { it.isNotBlank() }
                UserAvatar(rawUrl, modifier = Modifier.size(90.dp))

                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = when {
                        loading -> stringResource(R.string.profile_loading)
                        error != null -> stringResource(R.string.profile_error)
                        user?.profile?.displayName?.isNotEmpty() == true -> user!!.profile!!.displayName
                        else -> stringResource(R.string.profile_name_placeholder)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { navController.navigate("edit_profile") }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_profile))
                }
            }
            // --- Pasek z płcią, miastem i wiekiem ---
            val genderRaw = user?.profile?.gender
            val genderLabel = when (genderRaw) {
                Gender.MALE -> stringResource(R.string.gender_male)
                Gender.FEMALE -> stringResource(R.string.gender_female)
                Gender.OTHER -> stringResource(R.string.gender_other)
                Gender.PREFER_NOT_TO_SAY -> stringResource(R.string.gender_prefer_not_to_say)
                else -> null
            }
            val locationVal = user?.profile?.location?.takeIf { it.isNotBlank() }
            val ageVal = user?.profile?.birthDate?.let { bd ->
                val datePart = realmInstantToMongoTimestamp(bd)?.take(10)
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
                    loading -> stringResource(R.string.profile_loading_description)
                    error != null -> error ?: stringResource(R.string.profile_error_description)
                    user?.profile?.bio?.isNotEmpty() == true -> user!!.profile!!.bio
                    else -> stringResource(R.string.profile_description_placeholder)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.profile_interests_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (loading) {
                    InterestTag(base = "...")
                } else if (interests.isNullOrEmpty() ) {
                    InterestTag(base = stringResource(R.string.profile_no_interests))
                } else {
                    interests!!.forEach { ui ->
                        assert(ui.interest.name != null)
                        val base = ui.interest.name!!.ifBlank { stringResource(R.string.profile_unknown_interest) }
                        val label = if (!ui.customDescription.isNullOrBlank()) ui.customDescription else ""
                        InterestTag(base = base, label = label)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestTag(
    base: String,
    label : String = base
) {
    var showDialog by remember { mutableStateOf(false) }

    val full = if (label != "") "$base — $label" else base
    val short = if (full.length > 50) full.take(50) + "…" else full

    SuggestionChip(
        onClick = { showDialog = true },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        label = {
            Text(
                text = short,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 4.dp
                )
            )
        }
    )

    if (showDialog) {
        if (label != "") {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                },
                title = {
                    Text(
                        base,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                text = { Text(label) }
            )
        }
        else showDialog = false
    }
}

// --- Dolny Pasek Nawigacji  ---

data class BottomNavItem(
    val labelResId: Int,
    val icon: ImageVector,
    val route: String
)

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val items = listOf(
        BottomNavItem(R.string.bottom_nav_home, Icons.Default.Home, "main"),
        BottomNavItem(R.string.bottom_nav_friends, Icons.Default.Group, "friends_list"),
        BottomNavItem(R.string.bottom_nav_chats, Icons.Default.Chat, "chats"),
        BottomNavItem(R.string.bottom_nav_broadcast, Icons.Default.BroadcastOnPersonal, "scanner"),
        BottomNavItem(R.string.bottom_nav_settings, Icons.Default.Settings, "settings")
    )

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
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
    MaterialTheme {
        MainScreen(navController = rememberNavController())
    }
}

internal data class InfoItem(val icon: ImageVector, val text: String)