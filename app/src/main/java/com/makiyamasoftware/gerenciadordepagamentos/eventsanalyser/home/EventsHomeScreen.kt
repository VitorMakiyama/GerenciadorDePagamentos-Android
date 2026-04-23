package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.DatePickerFieldToModal
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.TimePickerFieldToModal
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme
import java.time.ZoneOffset
import java.time.ZonedDateTime

private const val TAG = "EventsHomeScreen"

@Composable
fun EventsHomeScreen(
    viewModel: EventsHomeViewModel = viewModel(),
    onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit
) {
    val uiState = viewModel.uiState
    val mediumPadding = dimensionResource(R.dimen.margin_normal)
    val isLoading = uiState is EventsHomeUiState.Loading

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(mediumPadding),
        verticalArrangement = Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            is EventsHomeUiState.Success -> {
                EventsHomeContent(
                    onKeyboardDone = { }, // Does something when keyboard is gone, not needed for now
                    isConnectionError = false,
                    subjectID = uiState.subjectID.toString(),
                    onSubjectIDChanged = viewModel::onSubjectIDChanged,
                    onClickSubmit = viewModel::submit,
                    occurrencesNumber = uiState.occurrencesNumber.toString(),
                    onOccurrencesNumberChanged = viewModel::onOccurrencesNumberChanged,
                    eventDateTime = uiState.eventLocalDateTime,
                    onEventDateChanged = viewModel::onEventDateChanged,
                    onEventTimeChanged = viewModel::onEventTimeChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(mediumPadding),
                )
                if (uiState.showCreatedEventSnackbar) {
                    Log.i(TAG, "Created event: updating UI...")
                    uiState.createdEvent?.let {
                        Log.d(
                            TAG,
                            "Showing snackbar EventCreated with message: ${uiState.createdEvent}"
                        )
                        onShowSnackbar(
                            stringResource(
                                R.string.EventHomeScreen_event_created_snackbar_message,
                                uiState.createdEvent.toString()
                            ),
                            null,
                            SnackbarDuration.Long,
                            {},
                            {
                                // Para mostrar o snackbar apenas uma vez
                                viewModel.updateSuccessUIState(
                                    showCreatedEventSnackbar = false
                                )
                            }
                        )
                    }
                } else if (uiState.eventAlreadyExists) {
                    // Recebeu um erro, esperado e tratável, da API: 409 Conflict, podemos sugerir ao user que envie um UPDATE à entrada encontrada no backend
                    Log.i(TAG, "Event with date conflict: updating UI...")
                    uiState.createdEvent?.let {
                        Log.d(
                            TAG,
                            "Showing snackbar EventAlreadyExists with message: ${uiState.createdEvent}"
                        )
                        onShowSnackbar(
                            stringResource(
                                R.string.EventHomeScreen_Error_409_Conflict_snackbar_message,
                                uiState.createdEvent.insertTS.toString(),
                                uiState.createdEvent.occurrences,
                                uiState.occurrencesNumber
                            ),
                            stringResource(R.string.EventHomeScreen_Error_409_Conflict_snackbar_action_label),
                            SnackbarDuration.Indefinite,
                            viewModel::onPutEventNewOccurrenceNumber
                        ) {
                            // Para mostrar o snackbar novamente, não settamos eventAlreadyExists=false, para o caso em que a pessoa o remova e depois de submit com as mesmas infos
                            // Isso deve ser feito após uma mudança na data do evento a ser cadastrado OU após atualizar com sucesso o evento
                        }
                    }
                } else if (uiState.subjectIDNotFound) {
                    // Recebeu um erro, esperado e tratável, da API: 404 Not found
                    Log.i(TAG, "Subject ID not found: updating UI...")
                    Log.d(TAG, "Showing snackbar SubjectIDNotFound")
                    onShowSnackbar(
                        stringResource(R.string.EventHomeScreen_Error_404_Not_Found_subject_id_message),
                        null,
                        SnackbarDuration.Long,
                        {},
                        {
                            // Para mostrar o snackbar apenas uma vez
                            viewModel.updateSuccessUIState(subjectIDNotFound = false)
                        }
                    )
                } else if (uiState.eventUpdated) {
                    Log.i(TAG, "Updated event: updating UI...")
                    uiState.createdEvent?.let {
                        Log.d(
                            TAG,
                            "Showing snackbar EventCreated with message: ${uiState.createdEvent}"
                        )
                        onShowSnackbar(
                            stringResource(
                                R.string.EventHomeScreen_event_updated_snackbar_message,
                                uiState.createdEvent.toString()
                            ),
                            null,
                            SnackbarDuration.Long,
                            {},
                            {
                                // Para mostrar o snackbar apenas uma vez
                                viewModel.updateSuccessUIState(
                                    eventUpdated = false,
                                )
                            }
                        )
                    }
                }
            }

            is EventsHomeUiState.Loading -> {
                // Nothing here so LoadingOverlay stays outside Column
            }

            is EventsHomeUiState.Error -> {
                EventsHomeContent(
                    onKeyboardDone = { },
                    isConnectionError = uiState.connectionError,
                    onClickRetry = viewModel::retry,
                    eventDateTime = uiState.eventLocalDateTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(mediumPadding)
                )
                onShowSnackbar(uiState.errorMessage, null, SnackbarDuration.Long, {}, {})
            }
        }
    }

    LoadingOverlay(
        modifier = Modifier.fillMaxSize(),
        isLoading = isLoading
    )
}

@Composable
fun EventsHomeContent(
    modifier: Modifier = Modifier,
    isConnectionError: Boolean,
    isSubjectIDNotFound: Boolean = false,
    subjectID: String = "0",
    onSubjectIDChanged: (String) -> Unit = {},
    occurrencesNumber: String = "1",
    onOccurrencesNumberChanged: (String) -> Unit = {},
    eventDateTime: ZonedDateTime,
    onEventDateChanged: (Long?) -> Unit = {},
    onEventTimeChanged: (Int, Int, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onClickSubmit: () -> Unit = {},
    onClickRetry: () -> Unit = {},
    onKeyboardDone: () -> Unit
) {
    val mediumPadding = dimensionResource(R.dimen.margin_small)

    Column(
        verticalArrangement = Arrangement.spacedBy(mediumPadding)
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(mediumPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(mediumPadding)
            ) {
                Text(
                    modifier = if (isConnectionError) Modifier // Escolhe a cor do chip do modifier dependendo se for ou nao erro
                        .clip(shapes.medium)
                        .background(colorScheme.error)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .align(alignment = Alignment.End)
                    else
                        Modifier
                            .clip(shapes.medium)
                            .background(colorScheme.tertiaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .align(alignment = Alignment.End),
                    text = if (isConnectionError) "Ping: ❌ Pong" else "Ping: Pong!",
                    style = typography.titleMedium,
                    color = if (isConnectionError) colorScheme.onError else colorScheme.onTertiary
                )
                Text(
                    text = if (isConnectionError) stringResource(R.string.EventHomeScreen_instructions_Error_label) else stringResource(
                        R.string.EventHomeScreen_instructions_label
                    ),
                    textAlign = TextAlign.Center,
                    style = typography.titleLarge
                )
                OutlinedTextField(
                    value = subjectID,
                    singleLine = true,
                    shape = shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface,
                        disabledContainerColor = colorScheme.surface,
                    ),
                    onValueChange = onSubjectIDChanged,
                    label = {
                        Text(
                            if (isConnectionError) stringResource(R.string.generic_connection_error)
                            else if (isSubjectIDNotFound) stringResource(R.string.EventHomeScreen_subjectID_not_found_label)
                            else stringResource(R.string.EventHomeScreen_subjectID_label)
                        )
                    },
                    isError = isSubjectIDNotFound,
                    enabled = !isConnectionError,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Number
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onKeyboardDone() }
                    )
                )
                OutlinedTextField(
                    value = occurrencesNumber,
                    singleLine = true,
                    shape = shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surface,
                        disabledContainerColor = colorScheme.surface,
                    ),
                    onValueChange = onOccurrencesNumberChanged,
                    label = {
                        Text(
                            text = if (isConnectionError) stringResource(R.string.generic_connection_error)
                            else stringResource(R.string.EventHomeScreen_occurrencesNumber_label)
                        )
                    },
                    isError = isConnectionError,
                    enabled = !isConnectionError,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Number
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onKeyboardDone() }
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(mediumPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DatePickerFieldToModal(
                        enabled = !isConnectionError,
                        currentDate = eventDateTime.toLocalDateTime().toInstant(ZoneOffset.UTC)
                            .toEpochMilli(), // Converte para LocalDate (perde o fuso horario) e depois em UTC, pois o DatePicker sempre retorna UTC
                        onDateSelected = onEventDateChanged,
                        modifier = Modifier.weight(0.55f)
                    )
                    TimePickerFieldToModal(
                        enabled = !isConnectionError,
                        currentTime = eventDateTime.toLocalTime(),
                        onTimeSelected = onEventTimeChanged,
                        modifier = Modifier.weight(0.45f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(mediumPadding),
            verticalArrangement = Arrangement.spacedBy(mediumPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClickSubmit,
                enabled = !isConnectionError
            ) {
                Text(
                    text = stringResource(R.string.EventHomeScreen_submit_Button_label),
                    fontSize = 16.sp
                )
            }

            if (isConnectionError) Button(
                onClick = onClickRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.EventHomeScreen_retry_Button_label),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    content: @Composable () -> Unit = {}
) {
    content()
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(R.dimen.loading_indicator_size)),
                color = colorScheme.primary,
                strokeWidth = dimensionResource(R.dimen.loading_indicator_stroke_width),
                trackColor = colorScheme.secondary,
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun EventsHomeContentPreview() {
    val mediumPadding = dimensionResource(R.dimen.margin_normal)

    val eventDate = ZonedDateTime.now()

    GerenciadorDePagamentosTheme {
        EventsHomeContent(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(mediumPadding),
            isConnectionError = false,
            isSubjectIDNotFound = false,
            subjectID = "1",
            onSubjectIDChanged = { },
            occurrencesNumber = "11",
            onOccurrencesNumberChanged = { },
            eventDateTime = eventDate,
            onEventDateChanged = { },
            onEventTimeChanged = { _, _, _, _ -> },
            onClickSubmit = { },
            onClickRetry = { },
            onKeyboardDone = { }
        )
    }
}

@Preview
@Composable
fun LoadingOverlayPreview() {
    GerenciadorDePagamentosTheme {
        LoadingOverlay(isLoading = true)
    }
}
