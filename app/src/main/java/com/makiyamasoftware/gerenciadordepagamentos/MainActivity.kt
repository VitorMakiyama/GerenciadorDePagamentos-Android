package com.makiyamasoftware.gerenciadordepagamentos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.makiyamasoftware.gerenciadordepagamentos.workbackground.UpdatePagamentoWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    val applicationScope = CoroutineScope(Dispatchers.IO)

    private lateinit var navController : NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navController = this.findNavController(R.id.nav_host_fragment)

        setSupportActionBar(findViewById(R.id.toolbar))

        NavigationUI.setupActionBarWithNavController(this, navController)

        delayedInit()
    }

    private fun delayedInit() {
        applicationScope.launch {
            setupWorkRecorrente()
        }
    }

    private fun setupWorkRecorrente() {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true) // Apenas se o dispositivo estiver idle (nao ativo)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<UpdatePagamentoWork>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            UpdatePagamentoWork.WORK_NAME,      // agenda o WORK_NAME work
            ExistingPeriodicWorkPolicy.UPDATE,  // politica do que fazer caso haja mais de um WORK_NAME enqueued,nesse caso, UPDATE o antigo com as especificacoes do novo
            repeatingRequest
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }
}