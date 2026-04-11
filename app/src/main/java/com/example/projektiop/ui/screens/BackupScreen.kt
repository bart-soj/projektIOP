package com.example.projektiop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.ui.components.GlassPanel
import com.example.projektiop.ui.components.ObserveAsEvents
import com.example.projektiop.ui.components.OutlinedTextFieldWithClearAndError
import com.example.projektiop.ui.viewmodels.BackupDialogUiEvent
import com.example.projektiop.ui.viewmodels.BackupViewModel
import org.koin.androidx.compose.koinViewModel


@Composable
fun BackupScreen(navController: NavController) {

    val viewModel = koinViewModel<BackupViewModel>()
    val uiEvents = viewModel.uiEvents
    val goBack = { navController.popBackStack() }

    ObserveAsEvents(uiEvents) { event ->
        when (event) {
            is BackupDialogUiEvent.BackupSuccess -> goBack()
            is BackupDialogUiEvent.BackupError -> {}
            is BackupDialogUiEvent.ShowToast -> {}
        }
    }

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
            true -> Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            false -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    GlassPanel {
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
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.3f
                                    )
                                ),
                                enabled = passwordErrors.isEmpty() && password == password_repeated
                            ) {
                                Text(
                                    if (loading) stringResource(R.string.saving) else stringResource(
                                        R.string.add
                                    )
                                )
                            }

                            IconButton(
                                onClick = { goBack() }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronLeft,
                                    contentDescription = null
                                )
                            }
                        }

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
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
