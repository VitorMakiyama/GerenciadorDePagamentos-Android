package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home.LoadingOverlay
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.MultipleFilterChip
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

@Composable
fun EventsReportsScreen(
    viewModel: EventsReportsViewModel,
    onShowSnackbar: (message: String, actionLabel: String?, duration: SnackbarDuration, onActionPerformed: () -> Unit, onDismissed: () -> Unit) -> Unit,
) {
    val uiState = viewModel.uiState

    LaunchedEffect(uiState) {
        val success = (uiState as? EventsReportsUIState.Success)
        if (success != null && success.reportsData == null) {
            // If uiState is Success and reportsData is null
            viewModel.getReportData(
                reportType = viewModel.selectedReportType,
                subjectID = viewModel.selectedSubjectID
            )
        }
    }

    when (uiState) {
        is EventsReportsUIState.Success -> {
            EventsReportsContent(
                modifier = Modifier,
                isConnectionError = false,
                onChangeSubjectID = viewModel::onChangeSubjectID,
                onChangeReportType = viewModel::onChangeReportType,
                selectedReport = viewModel.selectedReportType,
                reportOptions = uiState.reportTypes,
                selectedSubjectID = viewModel.selectedSubjectID.toString(),
                subjectIDs = uiState.subjectIDs,
                reportData = uiState.reportsData,
            )
        }

        is EventsReportsUIState.Error -> EventsReportsContent(
            modifier = Modifier,
            isConnectionError = uiState.connectionError,
            isReportDataRetrieveError = uiState.reportDataRetrieveError,
            onChangeSubjectID = viewModel::onChangeSubjectID,
            onChangeReportType = viewModel::onChangeReportType,
            selectedReport = viewModel.selectedReportType,
            reportOptions = viewModel.reportsTypes,
            selectedSubjectID = viewModel.selectedSubjectID.toString(),
            subjectIDs = viewModel.subjectsIDs,
            reportData = null,
            onClickRetry = viewModel::pingEventsAnalyserServer
        )

        is EventsReportsUIState.Loading -> LoadingOverlay(isLoading = true)
    }

    if (uiState.showSnackbar) {
        onShowSnackbar(
            uiState.snackbarMessage,
            null,
            SnackbarDuration.Long,
            {},
            viewModel::finishedShowingSnackbar
        )
    }
}

@Composable
fun EventsReportsContent(
    modifier: Modifier,
    isConnectionError: Boolean,
    isReportDataRetrieveError: Boolean = false,
    onChangeSubjectID: (String) -> Unit,
    onChangeReportType: (String) -> Unit,
    selectedReport: String,
    reportOptions: List<String>,
    selectedSubjectID: String,
    subjectIDs: List<String>,
    reportData: EventsReportsData?,
    onClickRetry: () -> Unit = {}
) {
    val smallPadding = dimensionResource(R.dimen.margin_small)

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()) // When editing, allows user to scroll when keyboard covers screen content
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(smallPadding)
    ) {
        if (isConnectionError) {
            Card(
                modifier = modifier.padding(smallPadding),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Button(
                    onClick = onClickRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.EventHomeScreen_retry_Button_label),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            Card(
                modifier = modifier.padding(smallPadding),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(smallPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(smallPadding)
                ) {
                    SubjectSelectionDropdown(
                        options = subjectIDs,
                        selectedOption = selectedSubjectID,
                        onChangeSubjectID = onChangeSubjectID
                    )
                    MultipleFilterChip(
                        chipLabels = reportOptions,
                        onClickChip = { newReportType -> onChangeReportType(newReportType) },
                        selected = selectedReport
                    )
                    reportData?.let {
                        // If reportData is not null, there is data, so show it!
                        EventsReportsRenderer(selectedReport, reportData, isReportDataRetrieveError)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSelectionDropdown(
    options: List<String>,
    selectedOption: String,
    onChangeSubjectID: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            label = { Text(text = stringResource(R.string.EventReports_subjectDropdownMenu_label)) },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onChangeSubjectID(option)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun EventsReportsContentPreview() {
    val reportOptions = listOf(
        EventsReportType.BASIC.name,
        EventsReportType.CHART.name,
        "Teste 3",
        "Teste 4",
        "Teste 5",
        "Teste 6"
    )
    val reportData = EventsReportsData.BasicReportData(
        type = "BASIC",
        details = BasicReport(
            weekly = "1,0",
            monthly = "4,0",
            sigma = "0,5",
            startDate = "2026-05-16",
            totalOccurrences = "10",
        ),
    )

    GerenciadorDePagamentosTheme {
        EventsReportsContent(
            modifier = Modifier,
            isConnectionError = false,
            onChangeSubjectID = {},
            onChangeReportType = {},
            selectedReport = reportOptions.first(),
            reportOptions = reportOptions,
            selectedSubjectID = "Teste 1",
            subjectIDs = listOf("Teste 1", "teste2", "teste32"),
            reportData = reportData,
            onClickRetry = {},
        )
    }
}
