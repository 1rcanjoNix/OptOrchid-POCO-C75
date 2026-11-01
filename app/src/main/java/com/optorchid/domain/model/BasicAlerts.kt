package com.optorchid.domain.model

enum class BasicAlert { LOW_MEMORY, LOW_STORAGE, HOT_BATTERY }

/**
 * Alertas básicos da Fase 1, calculados somente com a leitura pontual.
 * Limites provisórios: o Thermal Monitor (Fase 2) definirá as classificações oficiais.
 */
object BasicAlerts {
    /** Armazenamento livre abaixo de 10% do total. */
    const val LOW_STORAGE_FRACTION = 0.10f

    /** Temperatura da bateria igual ou acima de 45 °C. */
    const val HOT_BATTERY_CELSIUS = 45f

    fun evaluate(snapshot: QuickSnapshot): List<BasicAlert> = buildList {
        if (snapshot.ramLowMemory) add(BasicAlert.LOW_MEMORY)
        if (snapshot.storageTotalBytes > 0 &&
            snapshot.storageAvailableBytes.toFloat() / snapshot.storageTotalBytes < LOW_STORAGE_FRACTION
        ) add(BasicAlert.LOW_STORAGE)
        val temperature = snapshot.battery?.temperatureCelsius
        if (temperature != null && temperature >= HOT_BATTERY_CELSIUS) add(BasicAlert.HOT_BATTERY)
    }
}
