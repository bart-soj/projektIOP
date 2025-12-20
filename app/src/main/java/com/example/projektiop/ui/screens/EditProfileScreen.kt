package com.example.projektiop.ui.screens

import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.data.repositories.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.ui.components.MultiSelectInterestsDropdown
import com.example.projektiop.ui.components.UserAvatar
import org.koin.compose.koinInject
import kotlin.collections.setValue

@Composable
fun EditProfileScreen(
    navController: NavController,
    context: Context = LocalContext.current
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var allInterests by remember { //todo: check
        mutableStateOf(
            listOf(
                context.getString(R.string.interest_cybersecurity),
                context.getString(R.string.interest_reading),
                context.getString(R.string.interest_gaming),
                context.getString(R.string.interest_mountain_hiking),
                context.getString(R.string.interest_indie_cinema),
                context.getString(R.string.interest_cycling),
                context.getString(R.string.interest_electronic_music),
                context.getString(R.string.interest_football),
                context.getString(R.string.interest_backpacking),
                context.getString(R.string.interest_programming),
                context.getString(R.string.interest_gym_fitness)
            )
        )
    }
    var selectedInterests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDescriptions by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var interestsSaving by remember { mutableStateOf(false) }
    var broadcastMessage by remember { mutableStateOf("") }
    var loadingInitial by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var avatarPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var avatarPreviewBytes by remember { mutableStateOf<ByteArray?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var currentAvatarUrl by remember { mutableStateOf<String?>(null) }
    var avatarVersionTag by remember { mutableStateOf<String?>(null) }
    val userRepository = koinInject<UserRepository>()
    val interestRepository = koinInject<InterestRepository>()

    LaunchedEffect(Unit) {
        loadingInitial = true
        userRepository.fetchMyProfile()
            .onSuccess { prof ->
                name = prof.effectiveDisplayName ?: prof.username ?: prof.email ?: ""
                description = prof.effectiveDescription ?: ""
                gender = prof.profile?.gender ?: ""
                location = prof.profile?.location ?: ""
                birthDate = prof.profile?.birthDate?.takeIf { it.length >= 10 }?.substring(0,10) ?: ""
                broadcastMessage = prof.profile?.broadcastMessage ?: ""
                currentAvatarUrl = prof.profile?.avatarUrl
                avatarVersionTag = prof.updatedAt?.hashCode()?.toString()
                val names = prof.interests?.mapNotNull { it.interest.name }?.toSet().orEmpty()
                selectedInterests = names
                val descMap = mutableMapOf<String, String>()
                prof.interests.orEmpty().forEach { ui ->
                    val nm = ui.interest.name
                    if (!nm.isNullOrBlank()) {
                        descMap[nm] = ui.customDescription ?: ""
                    }
                }
                selectedDescriptions = descMap
            }
            .onFailure { error = it.message }
        interestRepository.fetchPublicInterestsMap()
            .onSuccess { map ->
                allInterests = map.keys.sorted()
            }
            .onFailure {  }
        loadingInitial = false
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp, top = 48.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(id = R.string.edit_profile), style = MaterialTheme.typography.headlineMedium)
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
                        val resolver = context.contentResolver
                        val name = queryDisplayName(context, avatarPreviewUri!!)
                        val type = resolver.getType(avatarPreviewUri!!)
                        val sizeOk = avatarPreviewBytes?.size ?: 0 <= 5 * 1024 * 1024
                        if (!sizeOk) {
                            uploading = false
                            uploadError = context.getString(R.string.upload_error_file_too_large)
                            return@launch
                        }
                        val result = userRepository.uploadAvatar(
                            bytes = avatarPreviewBytes!!,
                            originalFileName = name,
                            mimeType = type
                        )
                        result.onSuccess {
                            uploading = false
                            avatarPreviewUri = null
                            avatarPreviewBytes = null
                            currentAvatarUrl = it.profile?.avatarUrl
                            avatarVersionTag = it.updatedAt?.hashCode()?.toString()
                        }.onFailure {
                            uploading = false
                            uploadError = it.message ?: context.getString(R.string.avatar_upload_error)
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
                label = { Text(stringResource(id = R.string.nickname_label)) },
                supportingText = { Text(stringResource(id = R.string.nickname_count, name.length)) },
                isError = name.isBlank() || name.length > 50,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { if (it.length <= 100) location = it },
                label = { Text(stringResource(id = R.string.location_label)) },
                supportingText = { Text(stringResource(id = R.string.location_count, location.length)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = birthDate,
                onValueChange = {
                    if (it.length <= 10 && it.matches(Regex("^\\d{0,4}-?\\d{0,2}-?\\d{0,2}$"))) birthDate = it
                },
                label = { Text(stringResource(id = R.string.birthdate_label)) },
                isError = birthDate.isNotBlank() && !birthDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GenderDropdown(gender = gender, onGenderChange = { gender = it })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text(stringResource(id = R.string.description_label)) },
                supportingText = { Text(stringResource(id = R.string.description_count, description.length)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            MultiSelectInterestsDropdown(
                all = allInterests,
                selected = selectedInterests,
                onChange = { updated ->
                    val removed = selectedInterests.minus(updated)
                    val added = updated.minus(selectedInterests)
                    if (removed.isNotEmpty()) {
                        selectedDescriptions = selectedDescriptions.toMutableMap().apply {
                            removed.forEach { remove(it) }
                        }
                    }
                    if (added.isNotEmpty()) {
                        selectedDescriptions = selectedDescriptions.toMutableMap().apply {
                            added.forEach { if (get(it) == null) put(it, "") }
                        }
                    }
                    selectedInterests = updated
                    interests = updated.joinToString(",")
                }
            )
            if (selectedInterests.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.interest_descriptions_label),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedInterests.sorted().forEach { nameKey ->
                        val value = selectedDescriptions[nameKey] ?: ""
                        OutlinedTextField(
                            value = value,
                            onValueChange = { newVal ->
                                if (newVal.length <= 200) {
                                    selectedDescriptions = selectedDescriptions.toMutableMap().apply { put(nameKey, newVal) }
                                }
                            },
                            label = { Text(stringResource(id = R.string.interest_description_label, nameKey)) },
                            supportingText = { Text("${value.length}/200") },
                            trailingIcon = {
                                if (value.isNotBlank()) {
                                    IconButton(onClick = {
                                        selectedDescriptions = selectedDescriptions.toMutableMap().apply { put(nameKey, "") }
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(id = R.string.clear))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = broadcastMessage,
                onValueChange = { if (it.length <= 280) broadcastMessage = it },
                label = { Text(stringResource(id = R.string.broadcast_message_label)) },
                supportingText = { Text(stringResource(id = R.string.broadcast_message_count, broadcastMessage.length)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        error = null
                        userRepository.updateMyProfile(
                            displayName = name,
                            gender = gender,
                            location = location,
                            bio = description,
                            birthDate = if (birthDate.isBlank()) null else birthDate,
                            broadcastMessage = broadcastMessage
                        ).onSuccess {
                            // After base profile is saved, sync interests if changed
                            interestsSaving = true
                            val desired = selectedInterests.associateWith { nm ->
                                selectedDescriptions[nm]?.takeIf { it.isNotBlank() }
                            }
                            val syncRes = userRepository.syncMyInterestsWithDescriptions(desired)
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
                    isLoading && interestsSaving -> stringResource(id = R.string.saving_interests)
                    isLoading -> stringResource(id = R.string.saving)
                    else -> stringResource(id = R.string.save_changes)
                }
                Text(label)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.cancel))
            }
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text(stringResource(id = R.string.confirmation)) },
                    text = { Text(stringResource(id = R.string.confirm_back_message)) },
                    confirmButton = {
                        Button(onClick = {
                            showConfirmDialog = false
                            navController.popBackStack()
                        }) {
                            Text(stringResource(id = R.string.yes_go_back))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showConfirmDialog = false }) {
                            Text(stringResource(id = R.string.cancel))
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
        "" to stringResource(id = R.string.gender_none),
        "male" to stringResource(id = R.string.gender_male),
        "female" to stringResource(id = R.string.gender_female),
        "other" to stringResource(id = R.string.gender_other),
        "prefer_not_to_say" to stringResource(id = R.string.gender_prefer_not_to_say)
    )
    val currentLabel = options.firstOrNull { it.first == gender }?.second ?: stringResource(id = R.string.gender_none)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(id = R.string.gender_label)) },
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
                runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(id = R.string.avatar_preview),
                modifier = Modifier.size(96.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Show current avatar if available, otherwise a placeholder
            val rawUrl = currentAvatarUrl?.takeIf { it.isNotBlank() }
            UserAvatar(rawUrl, modifier = Modifier.size(128.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !uploading, onClick = { pickImage.launch("image/*") }) {
                Text(stringResource(id = R.string.choose_image))
            }
            Button(enabled = !uploading && avatarPreviewUri != null, onClick = onUpload) {
                Text(if (uploading) stringResource(id = R.string.uploading) else stringResource(id = R.string.upload))
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}

