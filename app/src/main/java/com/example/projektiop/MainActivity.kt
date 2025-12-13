package com.example.projektiop

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projektiop.ui.theme.ProjektIOPTheme
import androidx.compose.runtime.getValue
import com.example.projektiop.data.repositories.ThemePreference
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.projektiop.BluetoothLE.BLEActions
import com.example.projektiop.BluetoothLE.BLEService
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

import com.example.projektiop.ui.viewmodels.ScannerViewModel
import com.example.projektiop.BluetoothLE.BTPermissionsManager
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.ChatUpdateService
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.data.repositories.SharedPreferencesRepository
import com.example.projektiop.ui.screens.ChatDetailScreen
import com.example.projektiop.ui.viewmodels.AuthViewModel
import com.example.projektiop.ui.viewmodels.ChatsViewModel
import com.example.projektiop.ui.viewmodels.MainViewModel
import com.example.projektiop.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import com.example.projektiop.data.repositories.AuthEvent

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
            // Obsługa powiadomień
        }


    private val scannerViewModel: ScannerViewModel by viewModel()
    private val localBTPermissionsManager: BTPermissionsManager by lazy {
        BTPermissionsManager(this)
    }

    private val authRepository: AuthRepository by inject()

    // TODO() wywalic to
    private val chatsViewModel: ChatsViewModel by viewModel()

    // Zmienne do obsługi serwisu powiadomień
    private var chatService: ChatUpdateService? = null
    private var isBound = false

    // Połączenie serwisu
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ChatUpdateService.LocalBinder
            chatService = binder.getService()
            isBound = true

            chatsViewModel.connectToService(chatService!!.chatsFlow)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
            chatService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(perm)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authRepository.authEvent.collect { event ->
                    when (event) {
                        is AuthEvent.Success -> {
                            FriendshipRepository.start()
                            val token = SharedPreferencesRepository.get("auth_token", "") // sprawdzane logowanie
                            if (token.isNotBlank()) {
                                val intent = Intent(this, ChatUpdateService::class.java)
                                startForegroundService(intent)
                            }
                            startService(Intent(this@MainActivity, ChatUpdateService::class.java))
                        }
                        is AuthEvent.Logout -> {
                            stopService(Intent(Intent(this@MainActivity, ChatUpdateService::class.java)))
                        }
                        else -> {}
                    }
                }
            }
        }


        lifecycleScope.launch {
            scannerViewModel.bleEvents.collect { event ->
                when (event) {
                    BLEActions.STOP -> {
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = BLEActions.STOP.toString()
                        }
                        startService(intent)
                    }
                    BLEActions.START_SCAN -> {
                        localBTPermissionsManager.requestBluetoothPermissions(this@MainActivity, permissionsLauncher)
                        localBTPermissionsManager.showBluetoothLocationSnackbar(this@MainActivity)
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = BLEActions.START_SCAN.toString()
                        }
                        startService(intent)
                    }
                    BLEActions.STOP_SCAN -> {
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = BLEActions.STOP_SCAN.toString()
                        }
                        startService(intent)
                    }
                    BLEActions.START_ADVERTISE -> {
                        localBTPermissionsManager.requestBluetoothPermissions(this@MainActivity, permissionsLauncher)
                        localBTPermissionsManager.showBluetoothLocationSnackbar(this@MainActivity)
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = BLEActions.START_ADVERTISE.toString()
                        }
                        startService(intent)
                    }
                    BLEActions.STOP_ADVERTISE -> {
                        val intent = Intent(this@MainActivity, BLEService::class.java).apply {
                            action = BLEActions.STOP_ADVERTISE.toString()
                        }
                        startService(intent)
                    }
                }
            }
        }
    }

    // Bindowanie serwisu gdy aplikacja jest widoczna
    override fun onStart() {
        super.onStart()
        Intent(this, ChatUpdateService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    // Odpinanie serwisu gdy aplikacja znika
    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}


private fun MainActivity.showPermissionDeniedMessage(context: Context, permission: String) {
    Log.w("BLE_PERMISSIONS", "Użytkownik odmówił uprawnienia: $permission. Funkcjonalność może być ograniczona.")
    Toast.makeText(context, "Odmówiono uprawnienia: $permission", Toast.LENGTH_SHORT).show()
}


@Composable
fun MyApp() {
    val scannerViewModel: ScannerViewModel = koinActivityViewModel()
    val themePreference: ThemePreference = koinInject<ThemePreference>()

    val darkMode by themePreference.isDark.collectAsState()
    val navController = rememberNavController()
    val startDestination = if (SharedPreferencesRepository.get("auth_token", "").isBlank()) "start" else "main" // TODO() better logged-in status verification

    ProjektIOPTheme(darkTheme = darkMode) {
        NavHost(navController, startDestination = startDestination) {
            composable("start") { StartScreen(navController) }
            composable("login") { LoginScreen(navController) }
            composable("register") { RegisterScreen(navController) }
            composable("scanner") {
                ScannerScreen(
                    modifier = Modifier.padding(10.dp),
                    navController = navController,
                    viewModel = scannerViewModel
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
                val friendName = backStack.arguments?.getString("friendName")
                ChatDetailScreen(navController, chatId, friendId)
            }
            composable("settings") { SettingsScreen(navController) }
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