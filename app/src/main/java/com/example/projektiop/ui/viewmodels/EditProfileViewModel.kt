package com.example.projektiop.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projektiop.R
import com.example.projektiop.data.repositories.InterestRepository
import com.example.projektiop.data.repositories.UserRepository
import com.example.projektiop.domain.models.Gender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel(private val interestRepository: InterestRepository, private val userRepository: UserRepository): ViewModel() {

    val publicInterests = interestRepository.publicInterests
    val myInterests = userRepository.MyUserInterests
    val myUser = userRepository.myUser

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _errorUploading = MutableStateFlow<String?>(null)
    val errorUploading : StateFlow<String?> = _errorUploading.asStateFlow()

    private val _uploading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _currentAvatarUrl = MutableStateFlow<String?>(myUser.value?.profile?.avatarUrl)
    val currentAvatarUrl = _currentAvatarUrl.asStateFlow()

    fun onUpdateClick(displayName: String?, gender: Gender?, location: String?,
                      bio: String?, birthDate: String?, broadcastMessage: String?,
                      interestsWithDescriptions: Map<String, String>
                      ) {
        _loading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            userRepository.updateMyProfile(
                displayName = displayName,
                gender = gender?.name,
                location = location,
                bio = bio,
                birthDate = birthDate,
                broadcastMessage = broadcastMessage
            ).onFailure { e ->
                _errorMessage.value = e.message
            }
            userRepository.syncMyInterestsWithDescriptions(interestsWithDescriptions)
                .onFailure { e ->
                    _errorMessage.value = errorMessage.value + e.message
                }

            _loading.value = false
        }
    }

    // TODO() probably shouldn't pass context and access string resources from viewModel
    fun onAvatarUpload(toUploadBytes: ByteArray?, toUploadUri: Uri?, context: Context) {
        if (toUploadUri == null || toUploadBytes == null) return
        _uploading.value = true
        _errorUploading.value = null
        viewModelScope.launch {
            val resolver = context.contentResolver
            val type = resolver.getType(toUploadUri)
            val name = queryDisplayName(context, toUploadUri)
            val sizeOk = toUploadBytes.size <= 5 * 1024 * 1024
            if (!sizeOk) {
                _errorUploading.value = context.getString(R.string.upload_error_file_too_large)
                _uploading.value = false
                return@launch
            }
            userRepository.uploadAvatar(
                bytes = toUploadBytes,
                originalFileName = name,
                mimeType = type
            ).onFailure {
                _errorUploading.value = context.getString(R.string.avatar_upload_error) // + it.message
            }
            _currentAvatarUrl.value = myUser.value?.profile?.avatarUrl
            _uploading.value = false
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }
}