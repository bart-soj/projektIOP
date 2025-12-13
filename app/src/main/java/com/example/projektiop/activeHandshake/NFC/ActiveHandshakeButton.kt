package com.example.projektiop.activeHandshake.NFC

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.projektiop.ui.theme.ProjektIOPTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs


private const val SWIPE_THRESHOLD_DP = 100


// needs nfc to work
@Composable
fun ActiveHandshakeButton(
    leftButtonText: String = "Left",
    rightButtonText: String = "Right",
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val swipeThresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD_DP.dp.toPx() }
    var totalDragAmount by remember { mutableStateOf(Offset.Zero) }
    val viewModel = koinViewModel<NFCVIewModel>()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // The bottom button (visible only when not expanded)
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .padding(10.dp)
                    .size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "vb",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Fullscreen overlay with animation
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(500)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(500)) + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    totalDragAmount += dragAmount
                                    change.consume()
                                },
                                onDragEnd = {
                                    val dragAmmount_y = abs(totalDragAmount.y)
                                    val dragAmount_x = abs(totalDragAmount.x)

                                    if (dragAmount_x > swipeThresholdPx) {
                                        if (totalDragAmount.x < 0) {
                                            viewModel.onBlueClick(context)
                                        } else {
                                            viewModel.onGreenClick(context)
                                        }
                                    }

                                    if (dragAmmount_y > swipeThresholdPx) {
                                        if (totalDragAmount.y > 0) {
                                            expanded = false
                                        }
                                    }
                                    totalDragAmount = Offset.Zero
                                }
                            )
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                   Button (
                        onClick = {viewModel.onGreenClick(context)},
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .weight(1f)
                            .background(Color.Green)
                            .fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(leftButtonText)
                        }
                    }
                    Button (
                        onClick = {viewModel.onBlueClick(context)},
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .weight(1f)
                            .background(Color.Blue)
                            .fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(rightButtonText)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BeforeScreenPreview() {
    ProjektIOPTheme {
        ActiveHandshakeButton() {}
    }
}
