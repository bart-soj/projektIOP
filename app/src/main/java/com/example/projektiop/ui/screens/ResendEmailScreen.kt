package com.example.projektiop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.example.projektiop.R
import com.example.projektiop.ui.components.GlassPanel
import com.example.projektiop.ui.components.ObserveAsEvents
import com.example.projektiop.ui.components.OutlinedTextFieldWithClearAndError
import com.example.projektiop.ui.viewmodels.ResendEmailViewModel
import com.example.projektiop.ui.viewmodels.ResendUiEvent
import org.koin.androidx.compose.koinViewModel

@Composable
fun ResendEmailScreen(emailPrefill: String? = null) {

    val viewModel = koinViewModel<ResendEmailViewModel>()
    val uiEvents = viewModel.uiEvents

    var email by remember { mutableStateOf(emailPrefill ?: "")}
    val emailErrors by viewModel.emailErrors.collectAsState()

    val errorMessage by viewModel.errorMessage.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val delaying by viewModel.delaying.collectAsState()

    ObserveAsEvents(uiEvents) { event ->
        when (event) {
            ResendUiEvent.ResendError -> {}
            ResendUiEvent.ResendSuccess -> { viewModel.onSuccessWait() }
            ResendUiEvent.ShowToast -> {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when(loading) {
            true -> Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp)) }
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
                            text = stringResource(R.string.resend_email_title),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.onResendClick(email)
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
                            enabled = delaying == null && emailErrors.isEmpty() && email.isNotBlank()
                        ) {
                            if (delaying != null) Text("$delaying") else Text(stringResource(R.string.send))
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

                        // Pole Email
                        OutlinedTextFieldWithClearAndError(
                            value = email,
                            onValueChange = {
                                viewModel.onEmailChange(it)
                                email = it
                            },
                            label = stringResource(R.string.email_label),
                            errorList = emailErrors.map { stringResource(it) },
                            isError = emailErrors.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}