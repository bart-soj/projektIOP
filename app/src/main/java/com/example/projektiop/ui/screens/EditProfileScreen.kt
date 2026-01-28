package com.example.projektiop.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import com.example.projektiop.data.util.isoDateStringToMillis
import com.example.projektiop.domain.models.Gender
import com.example.projektiop.domain.models.Interest
import com.example.projektiop.ui.components.DatePickerDocked
import com.example.projektiop.ui.components.MultiSelectInterestsDropdown
import com.example.projektiop.ui.components.ObserveAsEvents
import com.example.projektiop.ui.components.UserAvatar
import com.example.projektiop.ui.viewmodels.EditProfileUiEevent
import com.example.projektiop.ui.viewmodels.EditProfileViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditProfileScreen(
    navController: NavController
) {
    val viewModel = koinViewModel<EditProfileViewModel>()
    val publicInterests by viewModel.publicInterests.collectAsState()
    val myInterests by viewModel.myInterests.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val myUser by viewModel.myUser.collectAsState()

    val uploading by viewModel.uploading.collectAsState()
    val uploadError by viewModel.errorUploading.collectAsState()
    val currentAvatarUrl by viewModel.currentAvatarUrl.collectAsState()

    val context = LocalContext.current

    var name by remember { mutableStateOf(myUser?.profile?.displayName ?: "")}
    var location by remember { mutableStateOf(myUser?.profile?.location ?: "")}
    var birthDate by remember { mutableStateOf(
        myUser?.profile?.birthDate?.let{ isoDateStringToMillis(it) })}
    var gender by remember { mutableStateOf<Gender?>(myUser?.profile?.gender)}
    var description by remember { mutableStateOf(myUser?.profile?.bio ?: "") }

    var selectedInterests by remember { mutableStateOf<Set<Interest>>(myInterests?.map{it.interest}?.toSet() ?: emptySet())}
    var selectedDescriptions = remember { mutableStateMapOf<Interest, String>() }
    var interestsSaving by remember { mutableStateOf(false) }
    var broadcastMessage by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var avatarPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var avatarPreviewBytes by remember { mutableStateOf<ByteArray?>(null) }

    val uiEventFlow = viewModel.uiEvents

    ObserveAsEvents(uiEventFlow) { event ->
        when(event) {
            is EditProfileUiEevent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            EditProfileUiEevent.UpdateFailure -> {}
            EditProfileUiEevent.UpdateSuccess -> {
                navController.navigate("main")
            }
        }
    }

    LaunchedEffect(Unit) {
        // was fetching my profile and public interests
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
            Spacer(Modifier.height(16.dp))
            AvatarPicker(
                avatarPreviewUri = avatarPreviewUri,
                avatarPreviewBytes = avatarPreviewBytes,
                currentAvatarUrl = currentAvatarUrl,
                onPick = { uri, bytes ->
                    avatarPreviewUri = uri
                    avatarPreviewBytes = bytes
                },
                onUpload = {
                    viewModel.onAvatarUpload(avatarPreviewBytes, avatarPreviewUri, context)
                    avatarPreviewBytes = null
                    avatarPreviewUri = null
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
                supportingText = { Text(stringResource(id = R.string.location_count , location.length)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            DatePickerDocked(birthDate, onDateSelected = { millis -> birthDate = millis })
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
                all = publicInterests,
                selected = selectedInterests,
                onChange = { updated ->
                    val removed = selectedInterests.minus(updated)
                    val added = updated.minus(selectedInterests)
                    if (removed.isNotEmpty()) {
                        removed.forEach { selectedDescriptions.remove(it) }
                    }
                    if (added.isNotEmpty()) {
                        selectedDescriptions = selectedDescriptions.apply {
                            added.forEach { if (get(it) == null) put(it, "") }
                        }
                    }
                    selectedInterests = updated
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
                    selectedInterests.sortedBy{ it.name }.forEach { key ->
                        val value = selectedDescriptions[key] ?: ""
                        OutlinedTextField(
                            value = value,
                            onValueChange = { newVal ->
                                if (newVal.length <= 200) {
                                    selectedDescriptions = selectedDescriptions.apply { put(key, newVal) }
                                }
                            },
                            label = {
                                Text(
                                    stringResource(id = R.string.interest_description_label,
                                    key.name)
                                ) },
                            supportingText = { Text("${value.length}/200") },
                            trailingIcon = {
                                if (value.isNotBlank()) {
                                    IconButton(onClick = {
                                        selectedDescriptions = selectedDescriptions.apply { put(key, "") }
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
                    viewModel.onUpdateClick(name, gender, location, description, birthDate,
                        broadcastMessage, selectedInterests.associateWith { key ->
                            selectedDescriptions[key]?.takeIf { it.isNotBlank() }.toString()
                        })
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && name.isNotBlank() && name.length in 1..50
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
private fun GenderDropdown(gender: Gender?, onGenderChange: (Gender?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        null to stringResource(id = R.string.gender_none),
        Gender.MALE to stringResource(id = R.string.gender_male),
        Gender.FEMALE to stringResource(id = R.string.gender_female),
        Gender.OTHER  to stringResource(id = R.string.gender_other),
        Gender.PREFER_NOT_TO_SAY to stringResource(id = R.string.gender_prefer_not_to_say)
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



