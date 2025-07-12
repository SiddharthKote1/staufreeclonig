package com.example.v02.ReelsBlockingService

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object DataStoreProvider {
    @Volatile
    private var INSTANCE: DataStore<AppSettings>? = null

    fun getInstance(context: Context): DataStore<AppSettings> {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: DataStoreFactory.create(
                serializer = AppSettingsSerializer,
                produceFile = { context.applicationContext.dataStoreFile("app_settings.json") },
                corruptionHandler = ReplaceFileCorruptionHandler {
                    AppSettings() // fallback
                },
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            ).also { INSTANCE = it }
        }
    }
}