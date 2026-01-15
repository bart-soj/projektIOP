package com.example.projektiop

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.example.projektiop.ui.theme.ProjektIOPTheme
import androidx.compose.runtime.getValue
import com.example.projektiop.data.repositories.ThemePreference
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.*
import androidx.navigation.compose.composable
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

import com.example.projektiop.BluetoothLE.BTPermissionsManager
import com.example.projektiop.BluetoothLE.BluetoothRepository
import com.example.projektiop.domain.AppEvent
import com.example.projektiop.domain.ChatEvent
import com.example.projektiop.data.api.websocket.SocketManager
import com.example.projektiop.data.repositories.AuthRepository
import com.example.projektiop.data.repositories.ChatUpdateService
import com.example.projektiop.data.repositories.FriendshipRepository
import com.example.projektiop.ui.screens.ChatDetailScreen
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.example.projektiop.data.repositories.ChatRepository
import com.example.projektiop.data.repositories.LanguageRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.AppState
import com.example.projektiop.domain.AppStateEvent
import com.example.projektiop.domain.AppStateRepository
import com.example.projektiop.domain.models.Language
import com.example.projektiop.ui.screens.KeyLoadingScreen
import com.example.projektiop.ui.screens.ReportScreen
import com.example.projektiop.ui.screens.ResendEmailScreen
import com.example.projektiop.ui.viewmodels.LanguageViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach { (permission, granted) ->
                if (!granted) {
                    val text = this.getString(R.string.permission_denied) + ": $permission"
                    showToast(text)
                }
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Obsługa powiadomień
        }


    private val localBTPermissionsManager: BTPermissionsManager by inject()

    private val languageRepository: LanguageRepository by inject()
    private val appStateRepository: AppStateRepository by inject()
    private val chatRepository: ChatRepository by inject()
    private val friendshipRepository: FriendshipRepository by inject()
    private val userRepository: UserRepository by inject()
    private val bleManager: BluetoothRepository by inject()
    private val authRepository: AuthRepository by inject()

    private val socketManager: SocketManager by inject()

    private var chatService: ChatUpdateService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ChatUpdateService.LocalBinder
            chatService = binder.getService()
            isBound = true

            chatRepository.connectToService(chatService!!.chatsFlow)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
            chatService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(perm)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appStateRepository.appStateEventFlow.collect { event ->
                    when (event) {
                        is AppStateEvent.OnGotKeys -> {
                            friendshipRepository.refreshAll()
                            userRepository.updateMyId()
                            socketManager.connect()
                            val intent = Intent(this@MainActivity, ChatUpdateService::class.java)
                            startForegroundService(intent)
                            Intent(this@MainActivity, ChatUpdateService::class.java).also { intent ->
                                bindService(intent, serviceConnection, Context.BIND_ADJUST_WITH_ACTIVITY)
                            }
                        }
                        is AppStateEvent.OnLogout -> {
                            socketManager.disconnect()
                            if (isBound) {
                                unbindService(serviceConnection)
                                isBound = false
                            }
                            stopService(Intent(Intent(this@MainActivity, ChatUpdateService::class.java)))
                        }
                        is AppStateEvent.OnAuthorization -> {}
                    }
                }
            }
        }

        lifecycleScope.launch {
            socketManager.appEventFlow.collect { event->
                when(event) {
                    AppEvent.Ban -> {
                        // delete all auth data before showing the message
                        authRepository.onLogoutCleanup()
                        val text = this@MainActivity.getString(R.string.got_banned)
                        showToast(text)
                        delay(1000)
                        authRepository.logout()
                    }
                    is AppEvent.Block -> {
                        friendshipRepository.getBlocked(event.friendId)
                    }
                    is AppEvent.Unblock -> {
                        friendshipRepository.getUnblocked(event.friendId)
                    }
                    is AppEvent.Receive -> {
                        chatRepository.receiveMessage(event.message, this@MainActivity)
                    }
                }
            }
        }

        lifecycleScope.launch {
            bleManager.bleEvents.collect { event ->
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

        enableEdgeToEdge()
        setContent {
            val languageViewModel = koinViewModel<LanguageViewModel>()
            val language by languageViewModel.language.collectAsState()

            val localizedContext = remember(language) {
                val locale = when (language) {
                    Language.POLISH -> Locale("pl")
                    Language.ENGLISH -> Locale("en")
                    Language.SYSTEM -> Resources.getSystem().configuration.locales[0]
                }
                this.createConfigurationContext(
                    Configuration(resources.configuration).apply {
                        setLocale(locale)
                    }
                )
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                MyApp()
            }


        }
    }

    override fun onStart() {
        super.onStart()
        /*
        Intent(this, ChatUpdateService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_ADJUST_WITH_ACTIVITY)
        }
         */
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}





@Composable
fun MyApp() {
    val themePreference: ThemePreference = koinInject<ThemePreference>()
    val appStateRepository = koinInject<AppStateRepository>()

    val darkMode by themePreference.isDark.collectAsState()
    val appState by appStateRepository.appState.collectAsState()

    ProjektIOPTheme (darkTheme = darkMode) {
        when(appState) {
            is AppState.GotKeys -> MainNavGraph()
            is AppState.Authenticated -> KeyLoadingScreen()
            is AppState.Unauthenticated -> AuthNavGraph()
        }
    }
}


@Composable
fun AuthNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "start"
    ) {
        composable("start") { StartScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable(
            route = "resend_email?email={email}",
            arguments = listOf(
                navArgument("email") { nullable = true; defaultValue = null }
            )
        ) { backStack ->
            val email = backStack.arguments?.getString("email")
            ResendEmailScreen(email)
        }
    }
}


@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    navController.addOnDestinationChangedListener { _, destination, _ ->
        Log.d("NAV_DEBUG", "Actual Destination: ${destination.route}")
    }
    val startDestination = "main"

    NavHost(navController, startDestination = startDestination) {
        composable("scanner") { ScannerScreen(navController) }
        composable("main") { MainScreen(navController) }
        composable("chats") { ChatsScreen(navController) }
        composable("chat_detail/{chatId}?friendId={friendId}",
            arguments = listOf(
                navArgument("chatId") { nullable = false },
                navArgument("friendId") { nullable = true; defaultValue = null }
            )
        ) { backStack ->
            val chatId = backStack.arguments?.getString("chatId")
            val friendId = backStack.arguments?.getString("friendId")
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
        composable(
            route = "report/{userId}?messageId={messageId}?content={content}",
            arguments = listOf(
                navArgument("userId") {nullable = false},
                navArgument("messageId") {nullable = true; defaultValue = ""},
                navArgument("content") {nullable = true; defaultValue = ""}
            )
        ) { backStack ->
            val userId = backStack.arguments?.getString("userId") ?: ""
            val messageId = backStack.arguments?.getString("messageId")
            val content = backStack.arguments?.getString("content")
            ReportScreen(navController, userId, messageId, content)
        }
    }
}

