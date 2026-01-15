package com.example.projektiop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.projektiop.R
import com.example.projektiop.ui.viewmodels.KeyLoadingViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.projektiop.ui.components.OutlinedTextFieldWithClearAndError
import com.example.projektiop.ui.viewmodels.AuthViewModel


@Composable
fun KeyLoadingScreen() {
    val viewModel = koinViewModel<KeyLoadingViewModel>()
    val gettingBackup by viewModel.gettingBackup.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.ensureKeys()
    }

    when {
        gettingBackup -> GetBackupDialog(viewModel)
        else -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Image(
                painter = painterResource(id = R.drawable.start_background2),
                contentDescription = stringResource(R.string.background_image_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            CircularProgressIndicator()
        }
    }
}


@Composable
fun GetBackupDialog(viewModel: KeyLoadingViewModel, modifier: Modifier = Modifier) {
    val authViewModel = koinViewModel<AuthViewModel>()
    var password by remember { mutableStateOf("") }
    val passwordErrors by viewModel.passwordErrors.collectAsState()
    val loading by viewModel.backupLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showForgetDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.restore_backup_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextFieldWithClearAndError(
                value = password,
                onValueChange = {
                    viewModel.onPasswordChange(it)
                    password = it
                },
                label = stringResource(R.string.password_label),
                errorList = passwordErrors.map{ stringResource(it) },
                modifier = Modifier.fillMaxWidth(),
                isError = passwordErrors.isNotEmpty(),
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.onBackupClick(password)
                },
                modifier = Modifier
                    .padding(start = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                enabled = passwordErrors.isEmpty() && !loading && password.isNotBlank()
            ) {
                Text(if (loading) stringResource(R.string.getting_backup) else stringResource(R.string.get_backup))
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { showForgetDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.forget), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.size(8.dp))
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.log_out), style = MaterialTheme.typography.titleLarge)
            }
            if (showForgetDialog) {
                AlertDialog(
                    onDismissRequest = { showForgetDialog = false },
                    title = { Text(stringResource(R.string.confirmation)) },
                    text = { Text(stringResource(R.string.forget_confirm_message)) },
                    confirmButton = {
                        Button(onClick = {
                            showForgetDialog  = false
                            viewModel.onForgetClick()
                        }) {
                            Text(stringResource(R.string.forget_confirm_yes))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showForgetDialog   = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text(stringResource(R.string.confirmation)) },
                    text = { Text(stringResource(R.string.logout_confirm_message)) },
                    confirmButton = {
                        Button(onClick = {
                            showLogoutDialog = false
                            authViewModel.onLogoutClick()
                        }) {
                            Text(stringResource(R.string.logout_confirm_yes))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showLogoutDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}
