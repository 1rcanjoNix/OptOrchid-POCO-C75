package com.optorchid.core.di

import android.content.Context
import com.optorchid.data.device.AndroidDeviceRepository
import com.optorchid.data.diagnostic.DisplayDiagnosticCollector
import com.optorchid.data.preferences.DataStoreSettingsRepository
import com.optorchid.data.preferences.settingsDataStore
import com.optorchid.domain.repository.DeviceRepository
import com.optorchid.domain.repository.SettingsRepository
import com.optorchid.domain.usecase.StartupSequence

/** Injeção de dependências manual. Simples de auditar e sem custo de build. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(appContext.settingsDataStore)

    val deviceRepository: DeviceRepository = AndroidDeviceRepository(appContext)

    val startupSequence = StartupSequence(deviceRepository)

    val displayDiagnosticCollector = DisplayDiagnosticCollector(appContext)
}
