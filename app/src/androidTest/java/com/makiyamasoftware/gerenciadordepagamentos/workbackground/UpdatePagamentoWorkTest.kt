package com.makiyamasoftware.gerenciadordepagamentos.workbackground

import android.content.Context
import android.util.Log
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.makiyamasoftware.gerenciadordepagamentos.database.FakePagamentosDao
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
*/
@RunWith(JUnit4::class)
@SmallTest
class UpdatePagamentoWorkTest {
    private lateinit var context: Context
    private lateinit var pagamentosRepository: PagamentosDatabaseDao

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        // Initialize WorkManager for instrumentation tests
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // Initialise te database
        pagamentosRepository = FakePagamentosDao()
        val pagamento = Pagamento(
            pagamentoID = 1,
            nome = "TEST",
            dataDeInicio = "2024-12-01",
            numPessoas = 2,
            freqDoPag = "Diário",
            autoUpdateHistorico = false,
            podeEnviarPush = false
        )
        val pessoa1 = Pessoa(pessoaID = 1, nome = "TEST_USER_1", ordem = 1, pagamentoID = 1)
        val pessoa2 = Pessoa(pessoaID = 2, nome = "TEST_USER_2", ordem = 2, pagamentoID = 1)
        val historico1 = HistoricoDePagamento(
            historicoID = 1,
            data = "2024-12-02",
            preco = 10.0,
            pagadorID = 1,
            pagamentoID = 1,
            estaPago = false
        )
        val historico2 = HistoricoDePagamento(
            historicoID = 2,
            data = "2024-12-03",
            preco = 10.0,
            pagadorID = 2,
            pagamentoID = 1,
            estaPago = false
        )
        val historico3 = HistoricoDePagamento(
            historicoID = 3,
            data = "2024-12-04",
            preco = 10.0,
            pagadorID = 1,
            pagamentoID = 1,
            estaPago = false
        )

        pagamentosRepository.inserirPagamento(pagamento)
        pagamentosRepository.inserirPessoa(pessoa1)
        pagamentosRepository.inserirPessoa(pessoa2)
        pagamentosRepository.inserirHistoricoDePagamento(historico1)
        pagamentosRepository.inserirHistoricoDePagamento(historico2)
        pagamentosRepository.inserirHistoricoDePagamento(historico3)
    }

    @Test
    fun testAllConstraintsAndPeriodAreMet() {
        // When all constraints are met
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true) // Apenas se o dispositivo estiver idle (nao ativo)
            .build()
        // Create request
        val request = PeriodicWorkRequestBuilder<UpdatePagamentoWork>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(context)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)
        // Enqueue
        workManager.enqueue(request).result.get()
        // Tells the testing framework that all constraints are met
        testDriver?.setPeriodDelayMet(request.id)
        testDriver?.setAllConstraintsMet(request.id)
        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()


        // Then the worker should run
        assertThat(workInfo?.state, `is`(WorkInfo.State.RUNNING))
    }

    @Test
    fun doWork_WhenSuccess() {
        val worker = TestListenableWorkerBuilder<UpdatePagamentoWork>(
            context = context,
            inputData = workDataOf()
        ).build()

        runBlocking {
            val result = worker.doWork()
            MatcherAssert.assertThat(result, `is`(ListenableWorker.Result.success()))
        }
    }
}