package com.example.projektiop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.projektiop.ui.theme.ProjektIOPTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.*
import com.example.projektiop.screens.ChatsScreen

import com.example.projektiop.screens.StartScreen
import com.example.projektiop.screens.LoginScreen
import com.example.projektiop.screens.MainScreen
import com.example.projektiop.screens.ProfileScreen
import com.example.projektiop.screens.RegisterScreen
import com.example.projektiop.screens.ScannerScreen
import com.example.projektiop.screens.SettingsScreen

class MainActivity : ComponentActivity() {

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach { (permission, granted) ->
                if (!granted) {
                    showPermissionDeniedMessage(permission)
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp()
        }
        requestBluetoothPermissions(this, permissionsLauncher)
    }

}

@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "main") {
        composable("start") { StartScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable ("scanner") {
            ScannerScreen(
                name = "brak",
                modifier = Modifier.padding(10.dp),
                navController = navController
        ) }
        composable("main") { MainScreen(navController) }
        composable("chats") { ChatsScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProjektIOPTheme {
        ScannerScreen(12345.toString(), navController = rememberNavController())
    }
}