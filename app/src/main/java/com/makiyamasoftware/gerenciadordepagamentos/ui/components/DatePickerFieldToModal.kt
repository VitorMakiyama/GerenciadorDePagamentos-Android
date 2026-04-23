package com.makiyamasoftware.gerenciadordepagamentos.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun DatePickerFieldToModal(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    currentDate: Long, // Must be in milliseconds!
    onDateSelected: (Long?) -> Unit
) {
    var selectedDate: Long? by remember { mutableStateOf(currentDate) }
    var showModal by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate?.let { convertUTCMillisToDate(it) } ?: "",
        onValueChange = { },
        enabled = enabled,
        label = { Text(stringResource(R.string.DatePickerFieldToModal_field_label)) },
        placeholder = { Text("DD/MM/YYYY") },
        trailingIcon = {
            Icon(
                painter = painterResource(R.drawable.date_range_24dp),
                contentDescription = "Select date"
            )
        },
        modifier = modifier
            .pointerInput(selectedDate) {
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
        DatePickerModal(
            selectedDate = selectedDate,
            onDateSelected = {
                selectedDate = it
                onDateSelected(it)
            },
            onDismiss = { showModal = false }
        )
    }
}

@Composable
private fun DatePickerModal(
    selectedDate: Long?,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(selectedDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text(stringResource(R.string.generic_Ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.generic_Cancelar))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun convertUTCMillisToDate(millis: Long): String {
    // 'millis' é um Long representando a data em milissegundos UTC
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = millis

    // Exemplo: formatar para exibição sem converter para fuso local
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val dateString = sdf.format(calendar.time)
    return dateString
}