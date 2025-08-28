package com.example.projektiop.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.data.AuthRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

@Composable
fun EditProfileScreen(
    navController: NavController,
    currentName: String = "",
    currentLocation: String = "",
    currentAge: String = "",
    currentGender: String = "",
    currentDescription: String = "",
    currentInterests: String = ""
) {
    var name by remember { mutableStateOf(currentName) }
    var location by remember { mutableStateOf(currentLocation) }
    var age by remember { mutableStateOf(currentAge) }
    var gender by remember { mutableStateOf(currentGender) }
    var description by remember { mutableStateOf(currentDescription) }
    var interests by remember { mutableStateOf(currentInterests) }
    var error by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Edycja profilu", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Imię i nazwisko") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Miejscowość") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Wiek") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = gender,
                onValueChange = { gender = it },
                label = { Text("Płeć") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = interests,
                onValueChange = { interests = it },
                label = { Text("Zainteresowania") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        // Przykład podpięcia pod API (szablon):
                        // 1. Aktualizacja danych tekstowych:
                        // val result = AuthRepository.updateProfile(name, location, age, gender, description, interests)
                        // 2. Aktualizacja zdjęcia profilowego:
                        // if (avatarUri != null) {
                        //     val file = File(avatarUri!!.path ?: "")
                        //     val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                        //     val body = MultipartBody.Part.createFormData("avatarImage", file.name, requestFile)
                        //     // val avatarResult = AuthRepository.updateAvatar(body)
                        // }
                        // Po sukcesie:
                        isLoading = false
                        navController.popBackStack()
                        // Po błędzie:
                        // error = "Nie udało się zaktualizować profilu"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Zapisywanie..." else "Zapisz zmiany")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Powrót")
            }
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("Potwierdzenie") },
                    text = { Text("Czy na pewno chcesz wrócić? Niezapisane zmiany zostaną utracone.") },
                    confirmButton = {
                        Button(onClick = {
                            showConfirmDialog = false
                            navController.popBackStack()
                        }) {
                            Text("Tak, wróć")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showConfirmDialog = false }) {
                            Text("Anuluj")
                        }
                    }
                )
            }
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
