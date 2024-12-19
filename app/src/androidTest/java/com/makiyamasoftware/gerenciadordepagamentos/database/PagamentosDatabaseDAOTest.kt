package com.makiyamasoftware.gerenciadordepagamentos.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.makiyamasoftware.gerenciadordepagamentos.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 *
 * Note that in general, make database tests instrumented tests, meaning they will be in the androidTest source set.
 * This is because if you run these tests locally, they will use whatever version of SQLite you have on your local machine,
 * which could be very different from the version of SQLite that ships with your Android device! Different Android devices
 * also ship with different SQLite versions, so it's helpful as well to be able to run these tests as instrumented tests
 * on different devices.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest /* This helps you group and choose which size test to run. DAO tests are considered unit tests since you are only
                testing the DAO, thus you can call them small tests. */
class PagamentosDatabaseDAOTest {
    // Executes each task synchronously using Architecture Components
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: PagamentosDatabase

    @Before
    fun initDb() {
        /* Using an in-memory database so that the information stored here disappears when the process
        * is killed */
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            PagamentosDatabase::class.java
        ).build()
    }
    @After
    fun closeDb() = database.close()

    @Test
    fun insertPagamentoAndGetById() = runTest {
        // GIVEN - Insert payment
        val pagamento = Pagamento(
            1,
            "title",
            "description",
            numPessoas = 1,
            freqDoPag = "TODO()",
            autoUpdateHistorico = false,
            podeEnviarPush = false
        )
        database.pagamentosDatabaseDao.inserirPagamento(pagamento)

        // WHEN - Get the payment by id from the database
        val loaded = database.pagamentosDatabaseDao.getPagamento(1).getOrAwaitValue()

        //THEN - The loaded data contains the expected values
        assertThat<Pagamento>(loaded as Pagamento, notNullValue())
        assertThat(loaded.pagamentoID, `is`(pagamento.pagamentoID))
        assertThat(loaded.nome, `is`(pagamento.nome))
        assertThat(loaded.dataDeInicio, `is`(pagamento.dataDeInicio))
        assertThat(loaded.numPessoas, `is`(pagamento.numPessoas))
        assertThat(loaded.freqDoPag, `is`(pagamento.freqDoPag))
        assertThat(loaded.autoUpdateHistorico, `is`(pagamento.autoUpdateHistorico))
        assertThat(loaded.podeEnviarPush, `is`(pagamento.podeEnviarPush))
    }

    @Test
    fun updatePagamentoAndGetById() = runTest {
        // 1. Insert a pagamento into the DAO.
        val pagamento = Pagamento(
            1,
            "title",
            "description",
            numPessoas = 1,
            freqDoPag = "TODO()",
            autoUpdateHistorico = false,
            podeEnviarPush = false
        )
        database.pagamentosDatabaseDao.inserirPagamento(pagamento)

        // 2. Update the pagamento by creating a new pagamento with the same ID but different attributes.
        val pagamentoToBeUpdated = Pagamento(
            pagamento.pagamentoID,
            "titleTest",
            "descriptionTest",
            numPessoas = 3,
            freqDoPag = "DAILY",
            autoUpdateHistorico = true,
            podeEnviarPush = true
        )
        database.pagamentosDatabaseDao.updatePagamento(pagamentoToBeUpdated)

        // 3. Check that when you get the task by its ID, it has the updated values.
        val updatedPagamento = database.pagamentosDatabaseDao.getPagamento(pagamento.pagamentoID).getOrAwaitValue()
        assertThat(updatedPagamento as Pagamento, notNullValue())
        assertThat(updatedPagamento.pagamentoID, `is`(pagamento.pagamentoID))
        assertThat(updatedPagamento.nome, `is`(pagamentoToBeUpdated.nome))
        assertThat(updatedPagamento.dataDeInicio, `is`(pagamentoToBeUpdated.dataDeInicio))
        assertThat(updatedPagamento.numPessoas, `is`(pagamentoToBeUpdated.numPessoas))
        assertThat(updatedPagamento.freqDoPag, `is`(pagamentoToBeUpdated.freqDoPag))
        assertThat(updatedPagamento.autoUpdateHistorico, `is`(pagamentoToBeUpdated.autoUpdateHistorico))
        assertThat(updatedPagamento.podeEnviarPush, `is`(pagamentoToBeUpdated.podeEnviarPush))
    }
}
/*@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.makiyamasoftware.gerenciadordepagamentos", appContext.packageName)
    }
}*/