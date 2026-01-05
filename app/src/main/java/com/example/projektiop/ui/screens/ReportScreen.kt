package com.example.projektiop.ui.screens

import android.util.Log
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.domain.ReportType
import com.example.projektiop.ui.components.ObserveAsEvents
import com.example.projektiop.ui.viewmodels.ReportUiEvent
import com.example.projektiop.ui.viewmodels.ReportViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReportScreen(navController: NavController,
                 userId: String, messageId: String?) {

    val viewModel = koinViewModel<ReportViewModel>()
    val uiEvents = viewModel.uiEvents

    var reason by remember { mutableStateOf("") }
    var type: ReportType? by remember { mutableStateOf(null)}

    val errorMessage by viewModel.errorMessage.collectAsState()
    val loading by viewModel.loading.collectAsState()

    Log.d("NAV", "inside report")

    ObserveAsEvents(uiEvents) { event ->
        when (event) {
            ReportUiEvent.ReportError -> {}
            ReportUiEvent.ReportSuccess -> { navController.popBackStack() }
            ReportUiEvent.ShowToast -> {}
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
                    Text(
                        text = stringResource(R.string.report_dialog_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.onReportClick(userId, messageId, type!!, reason)
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
                        enabled = reason.isNotBlank() && type != null
                    ) {
                        Text(
                            if (loading) stringResource(R.string.reporting) else stringResource(
                                R.string.report
                            )
                        )
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

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { if (it.length <= 1000) reason = it },
                        label = { Text(stringResource(R.string.reason)) },
                        supportingText = {
                            val color = if (reason.length < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            Text(
                                text = stringResource(R.string.report_count, reason.length, 1000),
                                color = color
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReportDropDown(type, {type = it})

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDropDown(type: ReportType?, onTypeChange: (ReportType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        null to stringResource(id = R.string.report_type_none),
        ReportType.IMPERSONATION to stringResource(id = R.string.report_type_impersonation),
        ReportType.INAPPROPRIATE_CONTENT to stringResource(id = R.string.report_type_inappropriate_content),
        ReportType.HATE_SPEECH to stringResource(id = R.string.report_type_hate_speech),
        ReportType.SCAM to stringResource(id = R.string.report_type_scam),
        ReportType.OTHER to stringResource(id = R.string.report_type_other),
        ReportType.SPAM to stringResource(id = R.string.report_type_spam)

    )
    val currentType = options.firstOrNull { it.first == type }?.second ?: stringResource(id = R.string.gender_none)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentType,
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(id = R.string.report_type_label)) },
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
                        onTypeChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}