package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.isPm
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.makiyamasoftware.gerenciadordepagamentos.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerFieldToModal(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    currentTime: LocalTime,
    onTimeSelected: (Int, Int, Boolean, Boolean) -> Unit
) {
    var showModal by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = convertLocalTimeToTimeString(currentTime),
        enabled = enabled,
        onValueChange = { },
        label = { Text(stringResource(R.string.TimePickerFieldToModal_field_label)) },
        placeholder = { Text("hh:mm") },
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.alarm_24dp),
                contentDescription = "Select time"
            )
        },
        modifier = modifier
            .pointerInput(currentTime) {
                awaitEachGesture {
                    // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                    // in the Initial pass to observe events before the text field consumes them
                    // in the Main pass.
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (upEvent != null && enabled) {
                        showModal = true
                    }
                }
            }
    )

    if (showModal) {
        DialWithDialog(
            currentTime = currentTime,
            onConfirm = { timePickerState ->
                onTimeSelected(
                    timePickerState.hour,
                    timePickerState.minute,
                    timePickerState.is24hour,
                    timePickerState.isPm
                )
            },
            onDismiss = { showModal = false }
        )
    }
}

fun convertLocalTimeToTimeString(currentTime: LocalTime): String {
    return currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialWithDialog(
    currentTime: LocalTime,
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = true,
    )

    TimePickerDialog(
        onDismiss = { onDismiss() },
        onConfirm = { onConfirm(timePickerState) }
    ) {
        TimePicker(
            state = timePickerState,
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.generic_Cancelar))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(stringResource(R.string.generic_Ok))
            }
        },
        text = { content() }
    )
}