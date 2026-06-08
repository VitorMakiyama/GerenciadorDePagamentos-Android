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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.eventsanalyser.home.LoadingOverlay
import com.makiyamasoftware.gerenciadordepagamentos.ui.theme.GerenciadorDePagamentosTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.runBlocking

enum class EventsReportType {
    BASIC,
    CHART_DAILY,
    CHART_WEEKLY,
    CHART_MONTHLY,
    CHART_YEARLY;
}

sealed class EventsReportsData {
    abstract val type: String

    @JsonClass(generateAdapter = true)
    data class BasicReportData(
        override val type: String,
        val details: BasicReport
    ) : EventsReportsData()

    data class ChartReportData(
        override val type: String,
        val details: ChartReport
    ) : EventsReportsData()
}

@JsonClass(generateAdapter = true)
data class BasicReport(
    val weekly: String,
    val monthly: String,
    val yearly: String,
    val sigma: String,
    @param:Json(name = "start_date")
    val startDate: String,
    @param:Json(name = "end_date")
    val endDate: String,
    @param:Json(name = "total_occurrences")
    val totalOccurrences: String
)

@JsonClass(generateAdapter = true)
data class ChartReport(
    val data: List<Int>,
    @param:Json(name = "x_labels")
    val xLabels: List<String>
)

@Composable
fun EventsReportsRenderer(
    selectedReport: String,
    reportData: EventsReportsData?,
    reportDataRetrieveError: Boolean
) {
    if (!reportDataRetrieveError) {
        when (selectedReport.uppercase()) {
            EventsReportType.BASIC.name -> {
                val basicReportData = reportData as? EventsReportsData.BasicReportData
                basicReportData?.let {
                    BasicEventReport(
                        basicReportData.details.weekly,
                        basicReportData.details.monthly,
                        basicReportData.details.yearly,
                        basicReportData.details.sigma,
                        basicReportData.details.startDate,
                        basicReportData.details.endDate,
                        basicReportData.details.totalOccurrences
                    )
                }
            }

            EventsReportType.CHART_DAILY.name, EventsReportType.CHART_WEEKLY.name, EventsReportType.CHART_MONTHLY.name, EventsReportType.CHART_YEARLY.name -> {
                val chartReportData = reportData as? EventsReportsData.ChartReportData
                if (chartReportData != null) {
                    ChartEventsReport(
                        chartReportData.details.data,
                        chartReportData.details.xLabels,
                    )
                } else {
                    EventsReportsRendererError()
                }
            }
        }
    } else {
        EventsReportsRendererError()
    }
}


@Composable
fun EventsReportsRendererError() {
    Text(text = "unimplemented or error!")
}

@Composable
fun BasicEventReport(
    weekly: String,
    monthly: String,
    yearly: String,
    sigma: String,
    startDate: String,
    endDate: String,
    totalOccurrences: String
) {
    val smallPadding = dimensionResource(R.dimen.margin_small)

    Column(
        verticalArrangement = Arrangement.spacedBy(smallPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(smallPadding)
    ) {
        ReportItem(stringResource(R.string.EventsReportsRenderer_BasicReport_weekly), weekly)
        ReportItem(stringResource(R.string.EventsReportsRenderer_BasicReport_monthly), monthly)
        ReportItem(stringResource(R.string.EventsReportsRenderer_BasicReport_yearly), yearly)
        ReportItem(stringResource(R.string.EventsReportsRenderer_BasicReport_sigma), sigma)
        ReportItem(stringResource(R.string.EventsReportsRenderer_BasicReport_startDate), startDate)
        ReportItem(stringResource(R.string.EventsReportsRenderer_BasicReport_endDate), endDate)
        ReportItem(
            stringResource(R.string.EventsReportsRenderer_BasicReport_totalOccurrences),
            totalOccurrences
        )
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

@Composable
fun ChartEventsReport(
    data: List<Int>,
    xLabels: List<String>
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            columnModel {
                series(*data.toTypedArray())
            }
        }
    }

    ChartEventsReportContent(modelProducer, xLabels)
}

@Composable
fun ChartEventsReportContent(
    modelProducer: CartesianChartModelProducer,
    xLabels: List<String>
) {
    val xAxisFormatter = CartesianValueFormatter { _, x, _ ->
        xLabels.getOrNull(x.toInt()) ?: x.toString()
    }

    CartesianChartHost(
        rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = xAxisFormatter,
                labelRotationDegrees = 45f,
                size = BaseAxis.Size.Fixed(70.dp)
            ),
        ),
        modelProducer,
        placeholder = { LoadingOverlay(modifier = Modifier, isLoading = true) }
    )
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun BasicEventReportPreview() {
    GerenciadorDePagamentosTheme {
        BasicEventReport(
            "0,43",
            "0,96",
            "28",
            "0,2",
            "12/05/2026",
            "03/06/2026",
            "28"
        )
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ChartEventsReportContentPreview() {
    val labels = listOf("2026-05-12", "2026-05-13", "2026-05-14", "2026-05-15", "2026-05-16")

    GerenciadorDePagamentosTheme {
        val modelProducer = remember { CartesianChartModelProducer() }
        LaunchedEffect(Unit) {
            runBlocking {
                modelProducer.runTransaction {
                    columnModel {
                        series(5, 6, 5, 2, 11) //8, 5, 2, 15, 11, 8, 13, 12, 10, 2, 7)
                    }
                }
            }
        }
        ChartEventsReportContent(
            modelProducer = modelProducer,
            xLabels = labels,
        )
    }
}
