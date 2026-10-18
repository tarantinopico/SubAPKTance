package com.antigravity.substance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.antigravity.substance.data.db.SubstanceDatabase
import com.antigravity.substance.data.preferences.UserSettingsStore
import com.antigravity.substance.data.repository.SubstanceRepository
import com.antigravity.substance.ui.dashboard.DashboardViewModel
import com.antigravity.substance.ui.logger.LogDoseViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SubstanceDatabase.getInstance(applicationContext)
        val settingsStore = UserSettingsStore(applicationContext)
        val repository = SubstanceRepository(
            substanceDao = database.substanceDao(),
            intakeLogDao = database.intakeLogDao(),
            settingsStore = settingsStore
        )

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                        DashboardViewModel(repository) as T
                    modelClass.isAssignableFrom(LogDoseViewModel::class.java) ->
                        LogDoseViewModel(repository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        val dashboardVm = ViewModelProvider(this, factory)[DashboardViewModel::class.java]
        val logDoseVm = ViewModelProvider(this, factory)[LogDoseViewModel::class.java]

        setContent {
            SubstanceApp(
                repository = repository,
                dashboardViewModel = dashboardVm,
                logDoseViewModel = logDoseVm
            )
        }
    }
}
