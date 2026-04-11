package com.example.projektiop.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BroadcastOnPersonal
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.projektiop.R


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

    NavigationBar (
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ){
        items.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                onClick = {
                    if (currentRoute != item.route) {
                        val popped = navController.popBackStack(item.route, inclusive = false)
                        if (!popped) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                //restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}

