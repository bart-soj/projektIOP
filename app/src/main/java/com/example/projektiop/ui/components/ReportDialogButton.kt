package com.example.projektiop.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun ReportDialogButton(navController: NavController, modifier: Modifier = Modifier, userId: String? = null, messageId: String? = null, content: (@Composable (onClick: () -> Unit) -> Unit)? = null) {

    val onOpenDialog = { navController.navigate("report?userid={$userId}&messageId={$messageId}") }

    if (content != null){
        content(onOpenDialog)
    } else {
        IconButton(
            onClick = onOpenDialog,
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        ) { Icon(imageVector = Icons.Filled.Report, contentDescription = null) }
    }
}
