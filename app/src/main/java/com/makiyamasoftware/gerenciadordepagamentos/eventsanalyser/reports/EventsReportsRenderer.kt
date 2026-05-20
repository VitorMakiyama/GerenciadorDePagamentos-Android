package com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.reports

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class EventsReportType {
    BASIC,
    CHART;
}

sealed class EventsReportsData {
    @JsonClass(generateAdapter = true)
    data class BasicReportData(
        val weekly: String,
        val monthly: String,
        val sigma: String,
        @param:Json(name = "start_date")
        val startDate: String,
        @param:Json(name = "total_occurrences")
        val totalOccurrences: String
    ) : EventsReportsData()

    // TODO: Enhance this type of report
    data class ChartReportData(
        val data: String
    ) : EventsReportsData()
}

@Composable
fun EventsReportsRenderer(
    selectedReport: String,
    reportData: EventsReportsData
) {
    when (selectedReport.uppercase()) {
        EventsReportType.BASIC.name -> {
            val basicReportData = reportData as EventsReportsData.BasicReportData
            BasicEventReport(
                basicReportData.weekly,
                basicReportData.monthly,
                basicReportData.sigma,
                basicReportData.startDate,
                basicReportData.totalOccurrences
            )
        }

        EventsReportType.CHART.name -> {
            // TODO: Redo this
            val chartReportData = reportData as EventsReportsData.ChartReportData
            Text(text = chartReportData.data)
        }
    }
}

@Composable
fun BasicEventReport(
    weekly: String,
    monthly: String,
    sigma: String,
    startDate: String,
    totalOccurrences: String
) {
    val smallPadding = dimensionResource(R.dimen.margin_small)

    Column(
        verticalArrangement = Arrangement.spacedBy(smallPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(smallPadding)
    ) {
        ReportItem("Semanal", weekly)
        ReportItem("Mensal", monthly)
        ReportItem("Desvio Padrão", sigma)
        ReportItem("Data inicial", startDate)
        ReportItem("Total de ocorrências", totalOccurrences)
    }
}

@Composable
fun ReportItem(
    itemTitle: String,
    itemContent: String
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.margin_small))
    ) {
        Text(
            text = itemTitle,
            textAlign = TextAlign.Center,
            style = typography.titleLarge
        )

        Text(
            text = itemContent,
            textAlign = TextAlign.End,
            style = typography.bodyMedium
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun EventsReportsRendererPreview() {
    GerenciadorDePagamentosTheme {
        BasicEventReport(
            "0,43",
            "0,96",
            "0,2",
            "12/05/2026",
            "28"
        )
    }
}
