package com.miempresa.comuniapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.miempresa.comuniapp.core.notifications.EventReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        programarRecordatorioDeEventos()
    }

    private fun programarRecordatorioDeEventos() {

        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val solicitudPeriodica =
            PeriodicWorkRequestBuilder<EventReminderWorker>(
                24,
                TimeUnit.HOURS
            )
                .setConstraints(restricciones)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            EventReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            solicitudPeriodica
        )
    }
}