package com.example.projektiop // Upewnij się, że pakiet jest poprawny

import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.projektiop.data.repositories.AuthRepository
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.projektiop.ui.theme.ProjektIOPTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.projektiop.data.ThemePreference
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.projektiop.BluetoothLE.BluetoothManagerUtils.Companion.requestBluetoothPermissions
import com.example.projektiop.BluetoothLE.BluetoothManagerUtils.Companion.showPermissionDeniedMessage
import com.example.projektiop.screens.ChatsScreen

import com.example.projektiop.screens.StartScreen
import com.example.projektiop.screens.LoginScreen
import com.example.projektiop.screens.MainScreen
import com.example.projektiop.screens.RegisterScreen
import com.example.projektiop.screens.ScannerScreen
import com.example.projektiop.screens.SettingsScreen
import com.example.projektiop.screens.EditProfileScreen
import com.example.projektiop.screens.FriendsListScreen
import com.example.projektiop.screens.FriendProfileScreen

import com.example.projektiop.BluetoothLE.BLEViewModel

class MainActivity : ComponentActivity() {

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach { (permission, granted) ->
                if (!granted) {
                    showPermissionDeniedMessage(this, permission)
                }
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Optional: could show a Snackbar/toast; keeping silent for now
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp()
        }
        // Pass the correct context (this) and the permissionsLauncher
        requestBluetoothPermissions(this, permissionsLauncher)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(perm)
            }
        }
    }

}

@Composable
fun MyApp() {
    var darkMode by remember { mutableStateOf(ThemePreference.isDark()) }
    val navController = rememberNavController()
    val startDestination = if (AuthRepository.getToken().isNullOrBlank()) "start" else "main"

    ProjektIOPTheme(darkTheme = darkMode) {
        NavHost(navController, startDestination = startDestination) {
            composable("start") { StartScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
            composable("scanner") {
                ScannerScreen(
                    modifier = Modifier.padding(10.dp),
                    navController = navController,
                    viewModel = viewModel(LocalActivity.current as ComponentActivity)
                )
            }
            composable("main") { MainScreen(navController) }
            composable("chats") { ChatsScreen(navController) }
            composable("chat_detail?chatId={chatId}&friendId={friendId}",
                arguments = listOf(
                    navArgument("chatId") { nullable = true; defaultValue = null },
                    navArgument("friendId") { nullable = true; defaultValue = null }
                )
            ) { backStack ->
                val chatId = backStack.arguments?.getString("chatId")
                val friendId = backStack.arguments?.getString("friendId")
                com.example.projektiop.screens.ChatDetailScreen(navController, chatId, friendId)
            }
            composable("settings") { SettingsScreen(navController, darkMode = darkMode, onToggleDark = {
                darkMode = !darkMode
                ThemePreference.setDark(darkMode)
            }) }
            composable("edit_profile") { EditProfileScreen(navController) }
            composable("friends_list") { FriendsListScreen(navController) }
            composable(
                route = "friend_profile/{userId}?username={username}&displayName={displayName}&avatarUrl={avatarUrl}",
                arguments = listOf(
                    navArgument("userId") { nullable = false },
                    navArgument("username") { nullable = true; defaultValue = null },
                    navArgument("displayName") { nullable = true; defaultValue = null },
                    navArgument("avatarUrl") { nullable = true; defaultValue = null }
                )
            ) { backStack ->
                val uid = backStack.arguments?.getString("userId") ?: ""
                val uname = backStack.arguments?.getString("username")
                val dname = backStack.arguments?.getString("displayName")
                val avatar = backStack.arguments?.getString("avatarUrl")
                FriendProfileScreen(navController, uid, uname, dname, avatar)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProjektIOPTheme() {
        ScannerScreen(navController = rememberNavController(), viewModel = viewModel())
    }
}