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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.makiyamasoftware.gerenciadordepagamentos.ui.components.MultipleFilterChip
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme

@Composable
fun EventsReportsScreen() {
    var reportOptions = arrayOf<String>()
    EventsReportType.entries.forEach {
        reportOptions = reportOptions.plus(it.name)
    }
    val reportData = mapOf<String, EventsReportsData>(Pair(EventsReportType.BASIC.name, EventsReportsData.BasicReportData(
        weekly = "1,0",
        monthly = "4,0",
        sigma = "0,5",
        startDate = "2026-05-16",
        totalOccurrences = "10",
    )),
        Pair(EventsReportType.CHART.name, EventsReportsData.ChartReportData(data = "Teste"))
    )
    var selectedOption by remember { mutableStateOf(reportOptions.first()) }

    EventsReportsContent(modifier = Modifier, false, {}, {s -> selectedOption = s}, selectedOption, reportOptions, reportData[selectedOption]!!)
}

@Composable
fun EventsReportsContent(
    modifier: Modifier,
    isConnectionError: Boolean,
    onChangeSubjectID: (String) -> Unit,
    onChangeReportType: (String) -> Unit,
    selectedReport: String,
    reportOptions: Array<String>,
    reportData: EventsReportsData
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
            Button(
                onClick = { /*onClickRetry*/ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.EventHomeScreen_retry_Button_label),
                    fontSize = 16.sp
                )
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
                    SubjectSelectionDropdown(arrayOf("Teste 1", "teste2", "teste32"), onChangeSubjectID)
                    MultipleFilterChip(
                        chipLabels = reportOptions,
                        onClickChip = { newReportType -> onChangeReportType(newReportType) },
                        selected = selectedReport
                    )
                    EventsReportsRenderer(selectedReport, reportData)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSelectionDropdown(
    options: Array<String>,
    onChangeSubjectID: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember {mutableStateOf(options.first())}
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            label = { Text(text = stringResource(R.string.EventReports_subjectDropdownMenu_label)) },
            onValueChange = { s: String ->
                selectedOption = s
                onChangeSubjectID(s)
            },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        selectedOption = option
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun EventsReportsContentPreview() {
    val reportOptions = arrayOf("Basic", "Chart", "Teste 3", "Teste 4", "Teste 5", "Teste 6")
    val reportData = EventsReportsData.BasicReportData(
        weekly = "1,0",
        monthly = "4,0",
        sigma = "0,5",
        startDate = "2026-05-16",
        totalOccurrences = "10",
    )

    GerenciadorDePagamentosTheme {
        EventsReportsContent(
            modifier = Modifier,
            isConnectionError = false,
            onChangeSubjectID = {},
            onChangeReportType = {},
            selectedReport = reportOptions.first(),
            reportOptions = reportOptions,
            reportData = reportData
        )
    }
}
