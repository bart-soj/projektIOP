package com.example.projektiop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.formelements.OutlinedTextFieldWithClearAndError
import com.example.projektiop.formelements.SwitchWithText

@Composable
fun LoginScreen(navController: NavController) {
    Surface(
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Ekran logowania")

            OutlinedTextFieldWithClearAndError(
                value = "",
                onValueChange = {},
                label = stringResource(R.string.email_label),
                errorMessage = stringResource(R.string.email_error),
                modifier = Modifier.padding(8.dp),
                isError = false // temporary
            )

            OutlinedTextFieldWithClearAndError(
                value = "",
                onValueChange = {},
                label = stringResource(R.string.password_label),
                errorMessage = stringResource(R.string.password_error),
                modifier = Modifier.padding(8.dp),
                isError = false // temporary
            )
            Row (
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ){
                SwitchWithText(
                    checked = false,
                    text = stringResource(R.string.remember_me_label),
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    // Handle the switch state change here
                }
            }
            Row (
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.errorContainer, // Background color
                        contentColor = colorScheme.error // Text color
                    )
                ) {
                    Text(stringResource(R.string.return_button_text))
                }
                Button(
                    onClick = { navController.navigate("scanner") },
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary, // Background color
                        contentColor = colorScheme.onPrimary // Text color
                    )
                ) {
                    Text(stringResource(R.string.login_button_text))
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    val navController = NavController(LocalContext.current)
    LoginScreen(navController)
}