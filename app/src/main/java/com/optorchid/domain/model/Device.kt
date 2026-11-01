package com.optorchid.domain.model

enum class DeviceFamily { MOTOROLA, XIAOMI, POCO, REDMI, SAMSUNG, REALME, ONEPLUS, OTHER }

data class DisplayInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    /** Frequências (Hz) dos modos de tela com a resolução atual, como reportado por Display.Mode. */
    val supportedHz: List<Int>,
)

/** Dados estáticos do aparelho: não mudam durante a vida do app. */
data class DeviceProfile(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val family: DeviceFamily,
    val androidRelease: String,
    val sdkInt: Int,
    val buildDisplay: String,
    val soc: String?,
    val primaryAbi: String,
    val cpuCores: Int,
    val totalRamBytes: Long,
    val totalStorageBytes: Long,
    val display: DisplayInfo,
) {
    val displayName: String
        get() {
            val maker = manufacturer.replaceFirstChar { it.titlecase() }
            return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$maker $model".trim()
        }
}

data class BatteryInfo(
    val levelPercent: Int,
    val temperatureCelsius: Float?,
    val isCharging: Boolean,
)

/** Leitura pontual de valores que variam. Não é monitoramento contínuo (Fase 2). */
data class QuickSnapshot(
    val ramTotalBytes: Long,
    val ramAvailableBytes: Long,
    val ramLowMemory: Boolean,
    val storageTotalBytes: Long,
    val storageAvailableBytes: Long,
    val currentHz: Float?,
    val battery: BatteryInfo?,
)

enum class ResourceId { MEMORY_INFO, STORAGE_STATS, BATTERY, DISPLAY_MODES, THERMAL_API }

data class ResourceCheck(val id: ResourceId, val available: Boolean)

data class AppSettings(val reduceMotion: Boolean = false)
