package com.example

import android.app.Application
import android.util.Log
import com.example.data.backup.BackupRestoreManager
import com.example.data.database.SpendWiseDatabase
import com.example.data.export.ReportExporter
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.seeder.SampleDataSeeder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface AppContainer {
    val database: SpendWiseDatabase
    val preferencesRepository: UserPreferencesRepository
    val expenseRepository: ExpenseRepository
    val backupRestoreManager: BackupRestoreManager
    val reportExporter: ReportExporter
}

class DefaultAppContainer(private val application: Application) : AppContainer {
    override val database: SpendWiseDatabase by lazy {
        SpendWiseDatabase.getDatabase(application)
    }

    override val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(application)
    }

    override val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(
            expenseDao = database.expenseDao(),
            categoryDao = database.categoryDao(),
            preferencesRepository = preferencesRepository,
            expenseSplitDao = database.expenseSplitDao(),
            friendDao = database.friendDao(),
            fundDao = database.fundDao(),
            database = database
        )
    }

    override val backupRestoreManager: BackupRestoreManager by lazy {
        BackupRestoreManager(
            categoryDao = database.categoryDao(),
            expenseDao = database.expenseDao(),
            preferencesRepository = preferencesRepository,
            expenseSplitDao = database.expenseSplitDao(),
            fundDao = database.fundDao(),
            friendDao = database.friendDao()
        )
    }

    override val reportExporter: ReportExporter by lazy {
        ReportExporter(
            context = application,
            expenseRepository = expenseRepository
        )
    }
}

class SpendWiseApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Ensure default categories exist and run safe, idempotent startup initialization
        applicationScope.launch {
            try {
                SampleDataSeeder.seedInitialDataIfNeeded(
                    categoryDao = container.database.categoryDao(),
                    expenseDao = container.database.expenseDao(),
                    preferencesRepository = container.preferencesRepository,
                    expenseSplitDao = container.database.expenseSplitDao(),
                    friendDao = container.database.friendDao(),
                    fundDao = container.database.fundDao(),
                    database = container.database
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SpendWiseApplication", "Startup initialization failed; preserving existing data", e)
            }
        }
    }
}
