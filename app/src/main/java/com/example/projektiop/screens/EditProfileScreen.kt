package com.example.projektiop.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun EditProfileScreen(
    navController: NavController,
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") } // YYYY-MM-DD
    var gender by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") } // legacy text field
    var allInterests by remember {
        mutableStateOf(
            listOf(
                "Programowanie",
                "Gry Komputerowe",
                "Cyberbezpieczeństwo",
                "Technologia i IT",
                "Sport i Aktywność Fizyczna",
                "Sztuka i Kultura",
                "Podróże i Odkrywanie"
            )
        )
    }
    var selectedInterests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var interestsSaving by remember { mutableStateOf(false) }
    var broadcastMessage by remember { mutableStateOf("") }
    var loadingInitial by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var avatarPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var avatarPreviewBytes by remember { mutableStateOf<ByteArray?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var currentAvatarUrl by remember { mutableStateOf<String?>(null) }
    var avatarVersionTag by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loadingInitial = true
        UserRepository.fetchMyProfile()
            .onSuccess { prof ->
                // Preferuj dowolne wystąpienie displayName (top-level lub nested profile)
                name = prof.effectiveDisplayName ?: prof.username ?: prof.email ?: ""
                description = prof.effectiveDescription ?: ""
                gender = prof.profile?.gender ?: ""
                location = prof.profile?.location ?: ""
                birthDate = prof.profile?.birthDate?.takeIf { it.length >= 10 }?.substring(0,10) ?: ""
                broadcastMessage = prof.profile?.broadcastMessage ?: ""
                currentAvatarUrl = prof.profile?.avatarUrl
                avatarVersionTag = prof.updatedAt?.hashCode()?.toString()
                // Initialize interests selection from server-provided interests by name
                val names = prof.interests?.mapNotNull { it.interest.name }?.toSet().orEmpty()
                selectedInterests = names
            }
            .onFailure { error = it.message }
        // Fetch available interests from backend and sort by name
        UserRepository.fetchInterestsMap()
            .onSuccess { map ->
                allInterests = map.keys.sorted()
            }
            .onFailure { /* keep defaults if API fails */ }
        loadingInitial = false
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Edycja profilu", style = MaterialTheme.typography.headlineMedium)
            if (loadingInitial) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            AvatarPicker(
                avatarPreviewUri = avatarPreviewUri,
                avatarPreviewBytes = avatarPreviewBytes,
                currentAvatarUrl = currentAvatarUrl,
                versionTag = avatarVersionTag,
                onPick = { uri, bytes ->
                    avatarPreviewUri = uri
                    avatarPreviewBytes = bytes
                },
                onUpload = {
                    if (avatarPreviewUri == null || avatarPreviewBytes == null) return@AvatarPicker
                    coroutineScope.launch {
                        uploading = true
                        uploadError = null
                        // Read metadata
                        val resolver = context.contentResolver
                        val name = queryDisplayName(context, avatarPreviewUri!!)
                        val type = resolver.getType(avatarPreviewUri!!)
                        // Basic validation: <= 5MB as per backend
                        val sizeOk = avatarPreviewBytes?.size ?: 0 <= 5 * 1024 * 1024
                        if (!sizeOk) {
                            uploading = false
                            uploadError = "Plik jest zbyt duży (max 5MB)."
                            return@launch
                        }
                        val result = UserRepository.uploadAvatar(
                            bytes = avatarPreviewBytes!!,
                            originalFileName = name,
                            mimeType = type
                        )
                        result.onSuccess {
                            uploading = false
                            // Clear preview and maybe show a toast/snackbar
                            avatarPreviewUri = null
                            avatarPreviewBytes = null
                            // Update current avatar to freshly returned one and bump version tag to refresh cache
                            currentAvatarUrl = it.profile?.avatarUrl
                            avatarVersionTag = it.updatedAt?.hashCode()?.toString()
                        }.onFailure {
                            uploading = false
                            uploadError = it.message ?: "Nie udało się wgrać avatara"
                        }
                    }
                },
                uploading = uploading
            )
            if (uploadError != null) {
                Spacer(Modifier.height(4.dp))
                Text(uploadError!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 50) name = it },
                label = { Text("Nick (1-50)") },
                supportingText = { Text("${name.length}/50") },
                isError = name.isBlank() || name.length > 50,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { if (it.length <= 100) location = it },
                label = { Text("Miejscowość (max 100)") },
                supportingText = { Text("${location.length}/100") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = birthDate,
                onValueChange = {
                    // Prosta walidacja format YYYY-MM-DD (pozwól wpisywać częściowo)
                    if (it.length <= 10 && it.matches(Regex("^\\d{0,4}-?\\d{0,2}-?\\d{0,2}$"))) birthDate = it
                },
                label = { Text("Data urodzenia (YYYY-MM-DD)") },
                isError = birthDate.isNotBlank() && !birthDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GenderDropdown(gender = gender, onGenderChange = { gender = it })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text("Opis (max 500)") },
                supportingText = { Text("${description.length}/500") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            MultiSelectInterestsDropdown(
                all = allInterests,
                selected = selectedInterests,
                onChange = { updated ->
                    selectedInterests = updated
                    interests = updated.joinToString(",")
                }
            )
            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = broadcastMessage,
                onValueChange = { if (it.length <= 280) broadcastMessage = it },
                label = { Text("Wiadomość (max 280)") },
                supportingText = { Text("${broadcastMessage.length}/280") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        error = null
                        UserRepository.updateMyProfile(
                            displayName = name,
                            gender = gender,
                            location = location,
                            bio = description,
                            birthDate = if (birthDate.isBlank()) null else birthDate,
                            broadcastMessage = broadcastMessage
                        ).onSuccess {
                            // After base profile is saved, sync interests if changed
                            interestsSaving = true
                            val syncRes = UserRepository.syncMyInterestsByNames(selectedInterests)
                            interestsSaving = false
                            isLoading = false
                            syncRes.onSuccess {
                                navController.popBackStack()
                            }.onFailure { e ->
                                error = e.message
                            }
                        }.onFailure {
                            error = it.message
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingInitial && !isLoading && name.isNotBlank() && name.length in 1..50 && (birthDate.isBlank() || birthDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")))
            ) {
                val label = when {
                    isLoading && interestsSaving -> "Zapisywanie (zainteresowania)..."
                    isLoading -> "Zapisywanie..."
                    else -> "Zapisz zmiany"
                }
                Text(label)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderDropdown(gender: String, onGenderChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Map backend values to Polish labels
    val options = listOf(
        "" to "(brak)",
        "male" to "Mężczyzna",
        "female" to "Kobieta",
        "other" to "Inna",
        "prefer_not_to_say" to "Wolę nie podawać"
    )
    val currentLabel = options.firstOrNull { it.first == gender }?.second ?: "(brak)"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = { },
            readOnly = true,
            label = { Text("Płeć") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onGenderChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectInterestsDropdown(
    all: List<String>,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val summary = if (selected.isEmpty()) "Wybierz zainteresowania" else selected.joinToString(limit = 3, truncated = "…")
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = summary,
            onValueChange = {},
            readOnly = true,
            label = { Text("Zainteresowania") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .heightIn(max = 260.dp)
                    .verticalScroll(scrollState)
            ) {
                all.forEach { item ->
                    val checked = item in selected
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(item)
                            }
                        },
                        onClick = {
                            val new = selected.toMutableSet().apply {
                                if (checked) remove(item) else add(item)
                            }
                            onChange(new)
                        }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Zamknij") }
        }
    }
}

@Composable
private fun AvatarPicker(
    avatarPreviewUri: Uri?,
    avatarPreviewBytes: ByteArray?,
    currentAvatarUrl: String?,
    versionTag: String?,
    onPick: (Uri?, ByteArray?) -> Unit,
    onUpload: () -> Unit,
    uploading: Boolean
) {
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            onPick(uri, bytes)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val bitmap = remember(avatarPreviewBytes) {
            avatarPreviewBytes?.let { bytes ->
                kotlin.runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Podgląd avatara",
                modifier = Modifier.size(96.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Show current avatar if available, otherwise a placeholder
            val rawUrl = currentAvatarUrl?.takeIf { it.isNotBlank() }
            val fullUrl = rawUrl?.let { if (it.startsWith("http")) it else "https://hellobeacon.onrender.com$it" }
            val displayUrl = fullUrl?.let { url ->
                versionTag?.let { v -> if (url.contains('?')) "$url&v=$v" else "$url?v=$v" } ?: url
            }
            if (displayUrl != null) {
                val request = ImageRequest.Builder(context)
                    .data(displayUrl)
                    .crossfade(true)
                    .apply {
                        val token = com.example.projektiop.data.repositories.AuthRepository.getToken()
                        if (!token.isNullOrBlank()) {
                            addHeader("Authorization", "Bearer $token")
                        }
                    }
                    .build()
                AsyncImage(
                    model = request,
                    contentDescription = "Aktualny avatar",
                    placeholder = painterResource(R.drawable.avatar_placeholder),
                    error = painterResource(R.drawable.avatar_placeholder),
                    modifier = Modifier.size(96.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.avatar_placeholder),
                    contentDescription = "Brak avatara",
                    modifier = Modifier.size(96.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !uploading, onClick = { pickImage.launch("image/*") }) { Text("Wybierz zdjęcie") }
            Button(enabled = !uploading && avatarPreviewUri != null, onClick = onUpload) {
                Text(if (uploading) "Wgrywanie..." else "Wgraj")
            }
        }
    }
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}

