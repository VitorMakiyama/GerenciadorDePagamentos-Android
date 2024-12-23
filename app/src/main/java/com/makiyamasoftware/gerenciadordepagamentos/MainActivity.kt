package com.makiyamasoftware.gerenciadordepagamentos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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

        // for setting up the channel for delivering push notifications on Android 8.0 and later
        createNotificationChannel()
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

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}