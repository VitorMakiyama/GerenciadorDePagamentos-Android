package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

@Composable
fun EventsHomeScreen(viewModel: EventsHomeViewModel = viewModel()) {
    val uiState = viewModel.uiState
    val mediumPadding = dimensionResource(R.dimen.margin_normal)

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
            is EventsHomeUiState.Success -> EventsHomeContent(
                onKeyboardDone = { },
                currentScrambledWord = "Photos fetched: ${uiState.events.size}",
                isError = false,
                eventID = uiState.eventID.toString(),
                onEventIDChanged = { },
                onClickSubmit = viewModel::submit,
                occurrencesNumber = uiState.occurrencesNumber.toString(),
                onOccurrencesNumberChanged = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(mediumPadding),
            )

            is EventsHomeUiState.Loading -> LoadingOverlay(isLoading = true)

            is EventsHomeUiState.Error -> {
                EventsHomeContent(
                    onKeyboardDone = { },
                    currentScrambledWord = "Photos fetched: no connection",
                    isError = true,
                    onClickRetry = viewModel::retry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(mediumPadding)
                )
            }
        }

//        GameStatus(score = 10, modifier = Modifier.padding(20.dp))
    }
}

@Composable
fun GameStatus(score: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
    ) {
        Text(
            text = "Score",// stringResource(R.string.score, score),
            style = typography.headlineMedium,
            modifier = Modifier.padding(8.dp)
        )

    }
}

@Composable
fun EventsHomeContent(
    currentScrambledWord: String,
    modifier: Modifier = Modifier,
    isError: Boolean,
    eventID: String = "0",
    onEventIDChanged: (String) -> Unit = {},
    occurrencesNumber: String = "1",
    onOccurrencesNumberChanged: (String) -> Unit = {},
    onClickSubmit: () -> Unit = {},
    onClickRetry: () -> Unit = {},
    onKeyboardDone: () -> Unit
) {
    val mediumPadding = dimensionResource(R.dimen.margin_normal)

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
                modifier = if (isError) Modifier // Escolhe a cor do chip do modifier dependendo se for ou nao erro
                    .clip(shapes.medium)
                    .background(colorScheme.error)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .align(alignment = Alignment.End)
                else
                    Modifier
                        .clip(shapes.medium)
                        .background(colorScheme.tertiary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .align(alignment = Alignment.End),
                text = if (isError) "Ping: ❌ Pong" else "Ping: Pong!",
                style = typography.titleMedium,
                color = if (isError) colorScheme.onError else colorScheme.onTertiary
            )
            Text( //TODO: remover depois de trocar para a minha API
                text = currentScrambledWord,
                style = typography.displaySmall
            )
            Text(
                text = if (isError) stringResource(R.string.EventHomeScreen_instructions_Error_label) else stringResource(
                    R.string.EventHomeScreen_instructions_label
                ),
                textAlign = TextAlign.Center,
                style = typography.titleLarge
            )
            OutlinedTextField(
                value = eventID,
                singleLine = true,
                shape = shapes.large,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    disabledContainerColor = colorScheme.surface,
                ),
                onValueChange = onEventIDChanged,
                label = {
                    Text(
                        if (isError) stringResource(R.string.generic_no_connection)
                        else stringResource(R.string.EventHomeScreen_eventID_label)
                    )
                },
                isError = isError,
                enabled = !isError,
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
                        text = if (isError) stringResource(R.string.generic_no_connection)
                        else stringResource(R.string.EventHomeScreen_occurrencesNumber_label)
                    )
                },
                isError = isError,
                enabled = !isError,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onKeyboardDone() }
                )
            )
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
            enabled = !isError
        ) {
            Text(
                text = stringResource(R.string.EventHomeScreen_submit_Button_label),
                fontSize = 16.sp
            )
        }

        if (isError) Button(
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

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    content: @Composable () -> Unit = {}
) {
    content()
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun GameScreenPreview() {
    val viewModel: EventsHomeViewModel = viewModel()
    viewModel.updateUIState(EventsHomeUiState.Success(listOf()))

    GerenciadorDePagamentosTheme {
        EventsHomeScreen()
    }
}