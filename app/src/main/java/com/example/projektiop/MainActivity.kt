package com.example.projektiop

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
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
import com.example.projektiop.data.repositories.ThemePreference
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.projektiop.BluetoothLE.BLEService
import com.example.projektiop.BluetoothLE.BLEService.Actions
import com.example.projektiop.ui.screens.ChatsScreen

import com.example.projektiop.ui.screens.StartScreen
import com.example.projektiop.ui.screens.LoginScreen
import com.example.projektiop.ui.screens.MainScreen
import com.example.projektiop.ui.screens.RegisterScreen
import com.example.projektiop.ui.screens.ScannerScreen
import com.example.projektiop.ui.screens.SettingsScreen
import com.example.projektiop.ui.screens.EditProfileScreen
import com.example.projektiop.ui.screens.FriendsListScreen
import com.example.projektiop.ui.screens.FriendProfileScreen

import com.example.projektiop.ui.viewmodels.BLEViewModel
import com.example.projektiop.BluetoothLE.BTPermissionsManager
import com.example.projektiop.ui.screens.ChatDetailScreen
import com.example.projektiop.ui.viewmodels.ChatsViewModel

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

    private val localBLEViewModel: BLEViewModel by viewModels()
    private val localBTPermissionsManager: BTPermissionsManager by lazy {
        BTPermissionsManager(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(perm)
            }
        }

        lifecycleScope.launchWhenCreated {
            localBLEViewModel.uiEvents.collect { event ->
                when (event) {
                    BLEViewModel.Actions.STOP -> {
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = Actions.STOP.toString()
                        }
                        startService(intent)
                    }
                    BLEViewModel.Actions.START_SCAN -> {
                        localBTPermissionsManager.requestBluetoothPermissions(this@MainActivity, permissionsLauncher)
                        localBTPermissionsManager.showBluetoothLocationSnackbar(this@MainActivity)
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = Actions.START_SCAN.toString()
                        }
                        startService(intent)
                    }
                    BLEViewModel.Actions.STOP_SCAN -> {
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = Actions.STOP_SCAN.toString()
                        }
                        startService(intent)
                    }
                    BLEViewModel.Actions.START_ADVERTISE -> {
                        localBTPermissionsManager.requestBluetoothPermissions(this@MainActivity, permissionsLauncher)
                        localBTPermissionsManager.showBluetoothLocationSnackbar(this@MainActivity)
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = Actions.START_ADVERTISE.toString()
                        }
                        startService(intent)
                    }
                    BLEViewModel.Actions.STOP_ADVERTISE -> {
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = Actions.STOP_ADVERTISE.toString()
                        }
                        startService(intent)
                    }
                }
            }
        }
    }



}

private fun MainActivity.showPermissionDeniedMessage(context: Context, permission: String) {
    Log.w("BLE_PERMISSIONS", "Użytkownik odmówił uprawnienia: $permission. Funkcjonalność może być ograniczona.")
    Toast.makeText(context, "Odmówiono uprawnienia: $permission", Toast.LENGTH_SHORT).show()
}


@Composable
fun MyApp() {
    var darkMode by remember { mutableStateOf(ThemePreference.isDark()) }
    val navController = rememberNavController()
    val startDestination = if (AuthRepository.getToken().isNullOrBlank()) "start" else "main" // TODO() better logged-in status verification

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
            composable("main") { MainScreen(navController, viewModel = viewModel(LocalActivity.current as ComponentActivity)) }
            composable("chats") { ChatsScreen(navController, viewModel = ChatsViewModel()) }
            composable("chat_detail?chatId={chatId}&friendId={friendId}",
                arguments = listOf(
                    navArgument("chatId") { nullable = true; defaultValue = null },
                    navArgument("friendId") { nullable = true; defaultValue = null }
                )
            ) { backStack ->
                val chatId = backStack.arguments?.getString("chatId")
                val friendId = backStack.arguments?.getString("friendId")
                val friendName = backStack.arguments?.getString("friendName")
                ChatDetailScreen(navController, chatId, friendId)
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