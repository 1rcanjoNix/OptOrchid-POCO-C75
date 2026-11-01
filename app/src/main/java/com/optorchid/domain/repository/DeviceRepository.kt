package com.optorchid.domain.repository

import com.optorchid.domain.model.DeviceProfile
import com.optorchid.domain.model.QuickSnapshot
import com.optorchid.domain.model.ResourceCheck

interface DeviceRepository {
    /** Dados estáticos do aparelho (cacheados após a primeira leitura). */
    fun profile(): DeviceProfile

    /** Leitura pontual de RAM, armazenamento, tela e bateria. */
    fun snapshot(): QuickSnapshot

    /** Quais fontes de dados responderam de fato neste aparelho. */
    fun resourceChecks(profile: DeviceProfile, snapshot: QuickSnapshot): List<ResourceCheck>
}
