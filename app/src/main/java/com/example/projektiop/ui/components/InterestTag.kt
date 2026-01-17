package com.example.projektiop.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.projektiop.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestTag(
    base: String,
    label : String = base
) {
    var showDialog by remember { mutableStateOf(false) }

    val full = if (label != "") "$base — $label" else base
    val short = if (full.length > 50) full.take(50) + "…" else full

    SuggestionChip(
        onClick = { showDialog = true },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        label = {
            Text(
                text = short,
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 4.dp
                )
            )
        }
    )

    if (showDialog) {
        if (label != "") {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(text = stringResource(R.string.ok))
                    }
                },
                title = {
                    Text(
                        base,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                text = { Text(label) }
            )
        }
        else showDialog = false
    }
}
