package com.makiyamasoftware.gerenciadordepagamentos

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makiyamasoftware.gerenciadordepagamentos.database.Pagamento
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabase
import com.makiyamasoftware.gerenciadordepagamentos.database.PagamentosDatabaseDao
import com.makiyamasoftware.gerenciadordepagamentos.database.Pessoa
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * This is not meant to be a full set of tests. For simplicity, most of your samples do not
 * include tests. However, when building the Room, it is helpful to make sure it works before
 * adding the UI.
 */

//@RunWith(AndroidJUnit4::class)
//class PagamentoDatabaseTest {
//
//    private lateinit var pagamentoDao: PagamentosDatabaseDao
//    private lateinit var db: PagamentosDatabase
//
//    @Before
//    fun createDb() {
//        val context = InstrumentationRegistry.getInstrumentation().targetContext
//        // Using an in-memory database because the information stored here disappears when the
//        // process is killed.
//        db = Room.inMemoryDatabaseBuilder(context, PagamentosDatabase::class.java)
//            // Allowing main thread queries, just for testing.
//            .allowMainThreadQueries()
//            .build()
//        pagamentoDao = db.pagamentosDatabaseDao
//    }
//
//    @After
//    @Throws(IOException::class)
//    fun closeDb() {
//        db.close()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun insertAndGetNight() {
//        val pagamento = Pagamento(dataDeInicio = 2, freqDoPag = "mes", numPessoas = 2, nome = "teste")
//        val pessoa1 = Pessoa(nome = "Jose", ordem = 1, pagamentoId = pagamento.pagamentoID)
//        val pessoa2 = Pessoa(nome = "Mari", ordem = 2, pagamentoId = pagamento.pagamentoID)
//
//        pagamentoDao.inserirPagamento(pagamento)
//        pagamentoDao.inserirPessoa(pessoa1)
//        pagamentoDao.inserirPessoa(pessoa2)
//
//        val pag = pagamentoDao.getPagamento(pagamento.pagamentoID)
//        assertEquals(pag.nome, "teste")
//    }
//}
