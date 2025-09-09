package com.example.projektiop // Upewnij się, że pakiet jest poprawny

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.projektiop.data.repositories.AuthRepository
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.projektiop.ui.theme.ProjektIOPTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.projektiop.BluetoothLE.BluetoothManagerUtils.Companion.requestBluetoothPermissions
import com.example.projektiop.BluetoothLE.BluetoothManagerUtils.Companion.showPermissionDeniedMessage
import com.example.projektiop.data.db.RealmProvider
import com.example.projektiop.screens.ChatsScreen

import com.example.projektiop.screens.StartScreen
import com.example.projektiop.screens.LoginScreen
import com.example.projektiop.screens.MainScreen
import com.example.projektiop.screens.RegisterScreen
import com.example.projektiop.screens.ScannerScreen
import com.example.projektiop.screens.SettingsScreen
import com.example.projektiop.screens.EditProfileScreen
import com.example.projektiop.screens.FriendsListScreen
import io.realm.kotlin.Realm

class MainActivity : ComponentActivity() {

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach { (permission, granted) ->
                if (!granted) {
                    showPermissionDeniedMessage(this, permission)
                }
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
    }

}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    // Determine start destination based on remembered token
    val startDestination = if (AuthRepository.getToken().isNullOrBlank()) "start" else "main"
    NavHost(navController, startDestination = startDestination) {
        composable("start") { StartScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("scanner") {
            ScannerScreen(
                name = "brak",
                modifier = Modifier.padding(10.dp),
                navController = navController
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
        composable("settings") { SettingsScreen(navController) }
        composable("edit_profile") { EditProfileScreen(navController) }
        composable("friends_list") { FriendsListScreen(navController) }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProjektIOPTheme {
        ScannerScreen(12345.toString(), navController = rememberNavController())
    }
}