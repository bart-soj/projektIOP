package com.example.projektiop.formelements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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

//================================================================================================
// z zajec, do przerobienia
//================================================================================================

@Composable
fun OutlinedTextFieldWithClearAndError(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier, // Dobrze jest dać modifier wcześniej
    errorMessage: String? = null, // Zmień na nullable, aby nie wymagać wiadomości, gdy nie ma błędu
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
                            R.string.clear_content_description, // Dostosuj nazwę zasobu
                            label // Opcjonalnie: dodaj label do opisu dla kontekstu
                            // Alternatywnie: Prostszy opis, np. stringResource(R.string.clear_text_action)
                        )
                    )
                }
            }
        },
        isError = isError,
        supportingText = {
            // Pokaż tekst błędu tylko jeśli isError jest true i errorMessage nie jest null
            if (isError && errorMessage != null) {
                Text(text = errorMessage) // Nie ma potrzeby używania Row, jeśli jest tylko jeden element
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
        // add some space between the text and the switch, could also be done with padding
        Spacer(modifier = Modifier.padding(8.dp))
        // The switch composable the most important parameters are checked and onCheckedChange
        // checked is the state of the switch and onCheckedChange is the callback that is called
        // when the switch is toggled
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}