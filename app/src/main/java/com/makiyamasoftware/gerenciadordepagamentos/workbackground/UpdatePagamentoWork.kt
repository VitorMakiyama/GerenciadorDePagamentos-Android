package com.makiyamasoftware.gerenciadordepagamentos.workbackground

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.makiyamasoftware.gerenciadordepagamentos.R
import com.makiyamasoftware.gerenciadordepagamentos.atualizarNovosHistoricosDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.convertStringToCalendar
import com.makiyamasoftware.gerenciadordepagamentos.createNewHistoryNotification
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.getPaymentNotificationContent
import com.makiyamasoftware.gerenciadordepagamentos.precisaDeNovoHistorico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

const val TAG = "UpdatePagamentoWork"
class UpdatePagamentoWork(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    // Nome unico do Work
    companion object {
        const val WORK_NAME = "UpdatePagamento"
    }
    private var notificationId = 1

    // uma funcao coroutine-friendly que vai executar o trabalho no background!
    override suspend fun doWork(): Result {
        val dataSource = PagamentosDatabase.getInstance(applicationContext).pagamentosDatabaseDao
        return try {
            withContext(Dispatchers.IO) {
                val pagamentos = dataSource.getAllPagamentosBackground()
                for (pagamento in pagamentos) {
                    if (pagamento.autoUpdateHistorico) {
                        val historico = withContext(Dispatchers.IO) { dataSource.getHistoricoDePagamentoBackground(pagamento.id) }
                        val pessoas = withContext(Dispatchers.IO) { dataSource.getPessoasDoPagamentoBackground(pagamento.id) }
                        val freqs = applicationContext.resources.getStringArray(R.array.frequencias_pagamentos)

                        getNovosHistoricosSeNecessario(pagamento, historico, pessoas, freqs, dataSource)
                        notificationId++
                    }
                }
            }
            Result.success()
        } catch (exception: UninitializedPropertyAccessException) {
            Log.e(TAG, "Erro Work: ${exception.message}")
            Result.retry()
        }
    }

    // Funcao auxiliar
    private fun getNovosHistoricosSeNecessario(pagamento: Pagamento, historico: HistoricoDePagamento?, pessoas: List<Pessoa>, freqs: Array<String>, dataSource: PagamentosDatabaseDao) {
        if (historico != null && pessoas != null)  {
            if (precisaDeNovoHistorico(pagamento.frequencia,
                    convertStringToCalendar(historico.data),
                    Calendar.getInstance(),
                    freqs)) {
                val novosHistoricos = atualizarNovosHistoricosDePagamento(historico, Calendar.getInstance(), pagamento, freqs, pessoas)
                dataSource.inserirHistoricoDePagamento(*novosHistoricos.toTypedArray())
                Log.d(TAG, "Gerou os novos historicos!\n$novosHistoricos")

                val notifyHistory = novosHistoricos.last()
                createNewHistoryNotification(
                    applicationContext,
                    pagamento.titulo,
                    getPaymentNotificationContent(
						notifyHistory,
						pessoas.find { p: Pessoa -> p.id == notifyHistory.pagadorId }!!,
						frequencia = pagamento.frequencia,
						frequencias = Resources.getSystem().getStringArray(R.array.frequencias_pagamentos)
					),
                    notificationId,
                    pagamento
                )
            }
        }
    }
}
