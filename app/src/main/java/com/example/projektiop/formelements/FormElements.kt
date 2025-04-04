package com.example.projektiop.formelements

import androidx.compose.foundation.layout.Arrangement
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
    errorMessage: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine, // this is lifted to the caller to make it more reusable
        trailingIcon = {
            // The clear button is only displayed when the text field is not empty
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = {
                        // Clear is done by setting an empty string
                        onValueChange("")
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear, // The clear icon from the Material Icons
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
            Row {
                if (isError)
                    Text(errorMessage)
            }
        },
        modifier = modifier
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