package com.optorchid.domain.usecase

import com.optorchid.domain.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class StartupStage { INITIALIZING, IDENTIFYING_DEVICE, CHECKING_RESOURCES, READY }

/**
 * Sequência de inicialização. Cada etapa anunciada executa trabalho real:
 * identificar o aparelho e consultar as fontes de dados. Não há porcentagem de progresso.
 * Não existe etapa "preparando monitoramento": o monitoramento só será criado na Fase 2.
 */
class StartupSequence(private val devices: DeviceRepository) {

    suspend fun run(onStage: suspend (StartupStage) -> Unit) {
        onStage(StartupStage.IDENTIFYING_DEVICE)
        val profile = withContext(Dispatchers.Default) { devices.profile() }

        onStage(StartupStage.CHECKING_RESOURCES)
        withContext(Dispatchers.Default) {
            devices.resourceChecks(profile, devices.snapshot())
        }

        onStage(StartupStage.READY)
    }
}
