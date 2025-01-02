package com.makiyamasoftware.gerenciadordepagamentos.telas.historicospagamento

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.makiyamasoftware.gerenciadordepagamentos.MainCoroutineRule
import com.makiyamasoftware.gerenciadordepagamentos.database.HistoricoDePagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import com.makiyamasoftware.gerenciadordepagamentos.database.mocks.FakePagamentosDao
import com.makiyamasoftware.gerenciadordepagamentos.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@ExperimentalCoroutinesApi
//@Config(sdk = [30]) // Remove when Robolectric supports SDK 31
@RunWith(AndroidJUnit4::class) // Needed to for ApplicationProvider.getApplicationContext()
class HistoricosPagamentoViewModelTest {
    // Use fake repository to be injected into the viewModel, this is possible with a Factory pattern
    private lateinit var pagamentosRepository: FakePagamentosDao

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Subject under test
    lateinit var historicosPagamentoViewModel: HistoricosPagamentoViewModel

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        // Initialise te database
        pagamentosRepository = FakePagamentosDao()
        val pagamento = Pagamento(
            id = 1,
            titulo = "TEST",
            dataDeInicio = "2024-12-01",
            numeroDePessoas = 2,
            frequencia = "Diário",
            autoUpdateHistorico = false,
            podeEnviarPush = false
        )
        val pessoa1 = Pessoa(id = 1, nome = "TEST_USER_1", ordem = 1, pagamentoId = 1)
        val pessoa2 = Pessoa(id = 2, nome = "TEST_USER_2", ordem = 2, pagamentoId = 1)
        val historico1 = HistoricoDePagamento(
            id = 1,
            data = "2024-12-02",
            preco = 10.0,
            pagadorId = 1,
            pagamentoId = 1,
            estaPago = false
        )
        val historico2 = HistoricoDePagamento(
            id = 2,
            data = "2024-12-03",
            preco = 10.0,
            pagadorId = 2,
            pagamentoId = 1,
            estaPago = false
        )
        val historico3 = HistoricoDePagamento(
            id = 3,
            data = "2024-12-04",
            preco = 10.0,
            pagadorId = 1,
            pagamentoId = 1,
            estaPago = false
        )

        pagamentosRepository.inserirPagamento(pagamento)
        pagamentosRepository.inserirPessoa(pessoa1)
        pagamentosRepository.inserirPessoa(pessoa2)
        pagamentosRepository.inserirHistoricoDePagamento(historico1)
        pagamentosRepository.inserirHistoricoDePagamento(historico2)
        pagamentosRepository.inserirHistoricoDePagamento(historico3)

        historicosPagamentoViewModel = HistoricosPagamentoViewModel(pagamentosRepository, ApplicationProvider.getApplicationContext(), pagamento)
    }

    @Test
    fun getHistoricoAt_getsHistoricoAtCorrectIndex() {
        // When trying to get an history at an index 0 and 1
        val history0 = historicosPagamentoViewModel.getHistoricoAt(0)
        val history1 = historicosPagamentoViewModel.getHistoricoAt(1)

        // Then the returned histories are the expected ones (the most recent history should be the first in the list)
        val expectedLatestHistory = pagamentosRepository.historicosData[3]
        assertEquals(expectedLatestHistory?.id, history0.id)
        assertEquals(expectedLatestHistory?.data, history0.data)
        assertEquals(expectedLatestHistory?.preco, history0.preco)
        assertEquals(expectedLatestHistory?.pagadorId, history0.pagadorId)
        assertEquals(expectedLatestHistory?.pagamentoId, history0.pagamentoId)
        assertEquals(expectedLatestHistory?.estaPago, history0.estaPago)

        val expectedSecondLatestHistory = pagamentosRepository.historicosData[2]
        assertEquals(expectedSecondLatestHistory?.id, history1.id)
        assertEquals(expectedSecondLatestHistory?.data, history1.data)
        assertEquals(expectedSecondLatestHistory?.preco, history1.preco)
        assertEquals(expectedSecondLatestHistory?.pagadorId, history1.pagadorId)
        assertEquals(expectedSecondLatestHistory?.pagamentoId, history1.pagamentoId)
        assertEquals(expectedSecondLatestHistory?.estaPago, history1.estaPago)
    }

    @Test
    fun getHistoricoAt_throwsOutOfBoundsIndex() {
        // When trying to get an history at an index out of bounds
        val oobIndex = 100
        val exception = assertThrows(IndexOutOfBoundsException::class.java) {
            historicosPagamentoViewModel.getHistoricoAt(oobIndex)
        }

        // Then throws IndexOutOfBoundsException with message
        assertEquals(
            "Index $oobIndex out of bounds for length ${pagamentosRepository.historicosData.size}",
            exception.message
        )
    }

    @Test
    fun onClickStatus_isSingleStatusChange() {
        // When click to change history status that has NO updatable previous histories
        historicosPagamentoViewModel.onClickStatus(2)

        // Then the eventUpdateStatus is set to SINGULAR
        assertEquals(StatusChangeType.SINGULAR, historicosPagamentoViewModel.eventUpdateStatus.getOrAwaitValue())
    }

    @Test
    fun onClickStatus_isMultipleStatusChange() {
        // When click to change history status that HAS updatable previous histories
        historicosPagamentoViewModel.onClickStatus(0)

        // Then the eventUpdateStatus is set to MULTIPLE
        assertEquals(StatusChangeType.MULTIPLE, historicosPagamentoViewModel.eventUpdateStatus.getOrAwaitValue())
    }

    @Test
    fun onClickStatus_isMultipleStatusChange_whenAtLeastOnePreviousHistoryIsUnpaid() {
        // When click to change history status that HAS updatable previous histories
        pagamentosRepository.historicosData[2]?.estaPago = true
        historicosPagamentoViewModel.onClickStatus(0)

        // Then the eventUpdateStatus is set to MULTIPLE
        assertEquals(StatusChangeType.MULTIPLE, historicosPagamentoViewModel.eventUpdateStatus.getOrAwaitValue())
    }

    @Test
    fun onClickStatus_isSingular_whenSelectedHistoryStatusAlreadyFalse() {
        // When click to change history status that HAS updatable previous histories, but the selected one is already 'paid'
        pagamentosRepository.historicosData[3]?.estaPago = true
        historicosPagamentoViewModel.onClickStatus(0)

        // Then the eventUpdateStatus is set to SINGULAR
        assertEquals(StatusChangeType.SINGULAR, historicosPagamentoViewModel.eventUpdateStatus.getOrAwaitValue())
    }

    @Test
    fun onAtualizarStatus_updateStatus() = runTest {
        // When updating (toggling) and saving one history status change
        historicosPagamentoViewModel.onClickStatus(2)
        historicosPagamentoViewModel.onAtualizarStatus()

        // Then the status should be changed
        val updatedHistory = pagamentosRepository.historicosData[1]
        requireNotNull(updatedHistory)
        assertEquals(true, updatedHistory.estaPago)
    }

    @Test
    fun onAtualizarMultiplosStatus_updateMultipleStatus_WhenAllAreUnpaid() = runTest {
        // When updating (toggling) and saving multiple history status change
        historicosPagamentoViewModel.onClickStatus(0) // this should be the latest one
        historicosPagamentoViewModel.onAtualizarMultiplosStatus()

        // Then all the status should have changed, except the one clicked (latest one)
        val updatedHistory1 = pagamentosRepository.historicosData[1]
        requireNotNull(updatedHistory1)
        assertEquals(true, updatedHistory1.estaPago)
        val updatedHistory2 = pagamentosRepository.historicosData[2]
        requireNotNull(updatedHistory2)
        assertEquals(true, updatedHistory2.estaPago)
        val updatedHistory3 = pagamentosRepository.historicosData[3]
        requireNotNull(updatedHistory3)
        assertEquals(false, updatedHistory3.estaPago)
    }

    @Test
    fun onAtualizarMultiplosStatus_updateMultipleStatus_WhenOneIsAlreadyPaid() = runTest {
        // When updating (toggling) and saving multiple history status change, but one is already paid
        historicosPagamentoViewModel.onClickStatus(0) // this should be the latest one
        pagamentosRepository.historicosData[2]?.estaPago = true
        historicosPagamentoViewModel.onAtualizarMultiplosStatus()

        // Then all the status should have changed, except the one clicked (latest one)
        val updatedHistory1 = pagamentosRepository.historicosData[1]
        requireNotNull(updatedHistory1)
        assertEquals(true, updatedHistory1.estaPago)
        val updatedHistory2 = pagamentosRepository.historicosData[2]
        requireNotNull(updatedHistory2)
        assertEquals(true, updatedHistory2.estaPago)
        val updatedHistory3 = pagamentosRepository.historicosData[3]
        requireNotNull(updatedHistory3)
        assertEquals(false, updatedHistory3.estaPago)
    }

    @Test
    fun getHistoricoClicado_errorSelectedHistoryNotSet() = runTest {
        // When trying to get the selected history, if it was not set
        val exception = assertThrows(UninitializedPropertyAccessException::class.java) {
            historicosPagamentoViewModel.getHistoricoClicado()
        }

        // Then throws UninitializedPropertyAccessException with message
        assertEquals(
            "Acessando historico clicado sem seta-lo!",
            exception.message
        )
    }
}