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
    val reportOptions = arrayOf("BASIC", "CHART", "T3")
    val reportData = EventsReportsData.BasicReportData(
        weekly = "1,0",
        monthly = "4,0",
        sigma = "0,5",
        startDate = "2026-05-16",
        totalOccurrences = "10",
    )

    EventsReportsContent(modifier = Modifier, false, reportOptions, reportData)
}

@Composable
fun EventsReportsContent(
    modifier: Modifier,
    isConnectionError: Boolean,
    reportOptions: Array<String>,
    reportData: EventsReportsData
) {
    val smallPadding = dimensionResource(R.dimen.margin_small)

    var selectedReport by remember { mutableStateOf(reportOptions.first()) }

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
                    MultipleFilterChip(
                        chipLabels = reportOptions,
                        onClickChip = { newReport -> selectedReport = newReport },
                        selected = selectedReport
                    )
                    EventsReportsRenderer(selectedReport, reportData)
                }
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
            reportOptions = reportOptions,
            reportData = reportData
        )
    }
}
