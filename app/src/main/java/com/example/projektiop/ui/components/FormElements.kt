package com.example.projektiop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.projektiop.R


@Composable
fun ValidationErrorList(items: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items.forEach { item ->
            Text(
                text = item ,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun OutlinedTextFieldWithClearAndError(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier, // Dobrze jest dać modifier wcześniej
    errorList: List<String> = emptyList(),// Zmień na nullable, aby nie wymagać wiadomości, gdy nie ma błędu
    isError: Boolean = false, // Zachowaj isError jako główny wskaźnik błędu
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None // Domyślnie brak transformacji (tekst widoczny)
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier, // Przekaż modifier
        label = { Text(label) },
        singleLine = singleLine,
        trailingIcon = {
            // Ikona czyszczenia tylko gdy pole nie jest puste
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = {
                        // Wyczyszczenie przez ustawienie pustego stringa
                        onValueChange("")
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear, // Ikona z Material Icons
                        contentDescription = stringResource(
                            R.string.clear_content_description,
                            label
                        )
                    )
                }
            }
        },
        isError = isError,
        supportingText = {
            if (isError && errorList.isNotEmpty() ) {
                ValidationErrorList(errorList)
            }
        },
        visualTransformation = visualTransformation
    )
}


@Composable
fun SwitchWithText(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    )
    {
        Text(text)
        Spacer(modifier = Modifier.padding(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}