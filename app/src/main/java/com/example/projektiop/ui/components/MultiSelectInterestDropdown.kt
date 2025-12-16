package com.example.projektiop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.projektiop.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectInterestsDropdown(
    all: List<String>,
    selected: Set<String>,
    onChange: (Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val summary = if (selected.isEmpty()) stringResource(id = R.string.choose_interests_empty)
    else selected.joinToString(limit = 3, truncated = "…")
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = summary,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(id = R.string.interests_label)) },
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
            ) { Text(stringResource(id = R.string.close)) }
        }
    }
}