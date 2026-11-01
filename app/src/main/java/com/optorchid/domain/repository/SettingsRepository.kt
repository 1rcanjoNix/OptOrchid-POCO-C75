package com.optorchid.domain.repository

import com.optorchid.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setReduceMotion(enabled: Boolean)
}
