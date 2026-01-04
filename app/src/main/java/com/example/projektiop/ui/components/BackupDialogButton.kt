package com.example.projektiop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.projektiop.R
import com.example.projektiop.ui.viewmodels.BackupDialogUiEvent
import com.example.projektiop.ui.viewmodels.BackupDialogViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupDialogButton(modifier: Modifier = Modifier) {

    val viewModel = koinViewModel<BackupDialogViewModel>()
    var expanded by remember{mutableStateOf(false)}
    val uiEvents = viewModel.uiEvents

    ObserveAsEvents(uiEvents) { event ->
        when (event) {
            is BackupDialogUiEvent.BackupSuccess -> expanded = false
            is BackupDialogUiEvent.BackupError -> {}
            is BackupDialogUiEvent.ShowToast -> {}
        }
    }

    when(expanded) {
        true -> {
            BackupDialog(viewModel, onClose = {expanded = false})
        }
        false -> {
            Button(
                onClick = {expanded = true},
                modifier = modifier,
                enabled = !viewModel.isBackedUp,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color(0xFF4CAF50),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (viewModel.isBackedUp) stringResource(R.string.backed_up) else stringResource(R.string.create_backup))
            }
        }
    }
}

@Composable
fun BackupDialog(viewModel: BackupDialogViewModel, onClose: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var password_repeated by remember { mutableStateOf("") }

    val loading by viewModel.loading.collectAsState()
    val passwordErrors by viewModel.passwordErrors.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when(loading) {
            true -> CircularProgressIndicator()
            false -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.backup_dialog_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(
                            onClick = {
                                viewModel.onSetClick(password)
                            },
                            modifier = Modifier
                                .padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            ),
                            enabled = passwordErrors.isEmpty() && password == password_repeated
                        ) {
                            Text(if (loading) stringResource(R.string.saving) else stringResource(R.string.add))
                        }

                        IconButton(
                            onClick = onClose
                        ) { Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextFieldWithClearAndError(
                        value = password,
                        onValueChange = {
                            viewModel.onPasswordChange(it)
                            password = it
                        },
                        label = stringResource(R.string.password_label),
                        errorList = passwordErrors.map { stringResource(it) },
                        isError = passwordErrors.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextFieldWithClearAndError(
                        value = password_repeated,
                        onValueChange = {
                            viewModel.onPasswordChange(it)
                            password_repeated = it
                        },
                        label = stringResource(R.string.password_repeated_label),
                        errorList = emptyList(),
                        isError = password != password_repeated,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
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
                }
            }
        }
    }
}