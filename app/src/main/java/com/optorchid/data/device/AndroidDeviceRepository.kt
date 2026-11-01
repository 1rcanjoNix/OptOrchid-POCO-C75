package com.optorchid.data.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.Display
import com.optorchid.domain.model.BatteryInfo
import com.optorchid.domain.model.DeviceFamily
import com.optorchid.domain.model.DeviceProfile
import com.optorchid.domain.model.DisplayInfo
import com.optorchid.domain.model.QuickSnapshot
import com.optorchid.domain.model.ResourceCheck
import com.optorchid.domain.model.ResourceId
import com.optorchid.domain.repository.DeviceRepository
import kotlin.math.roundToInt

/** Leituras reais por APIs públicas do Android. Nenhuma exige permissão. */
class AndroidDeviceRepository(context: Context) : DeviceRepository {

    private val appContext = context.applicationContext
    private val activityManager = appContext.getSystemService(ActivityManager::class.java)
    private val displayManager = appContext.getSystemService(DisplayManager::class.java)

    private val cachedProfile: DeviceProfile by lazy { buildProfile() }

    override fun profile(): DeviceProfile = cachedProfile

    override fun snapshot(): QuickSnapshot {
        val memory = readMemory()
        val (storageTotal, storageFree) = readStorage()
        return QuickSnapshot(
            ramTotalBytes = memory.totalMem,
            ramAvailableBytes = memory.availMem,
            ramLowMemory = memory.lowMemory,
            storageTotalBytes = storageTotal,
            storageAvailableBytes = storageFree,
            currentHz = defaultDisplay()?.refreshRate?.takeIf { it > 0f },
            battery = readBattery(),
        )
    }

    override fun resourceChecks(profile: DeviceProfile, snapshot: QuickSnapshot): List<ResourceCheck> = listOf(
        ResourceCheck(ResourceId.MEMORY_INFO, snapshot.ramTotalBytes > 0),
        ResourceCheck(ResourceId.STORAGE_STATS, snapshot.storageTotalBytes > 0),
        ResourceCheck(ResourceId.BATTERY, snapshot.battery != null),
        ResourceCheck(ResourceId.DISPLAY_MODES, profile.display.supportedHz.isNotEmpty()),
        // Indica apenas que a API existe nesta versão do Android, não que o sensor responda.
        ResourceCheck(ResourceId.THERMAL_API, Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q),
    )

    private fun buildProfile(): DeviceProfile {
        val memory = readMemory()
        val (storageTotal, _) = readStorage()
        return DeviceProfile(
            manufacturer = Build.MANUFACTURER.orEmpty().trim(),
            brand = Build.BRAND.orEmpty().trim(),
            model = Build.MODEL.orEmpty().trim(),
            family = detectFamily(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty()),
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            buildDisplay = Build.DISPLAY.orEmpty(),
            soc = readSoc(),
            primaryAbi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamBytes = memory.totalMem,
            totalStorageBytes = storageTotal,
            display = readDisplayInfo(),
        )
    }

    /** POCO e Redmi reportam fabricante "Xiaomi"; a marca (Build.BRAND) os distingue. */
    private fun detectFamily(manufacturer: String, brand: String): DeviceFamily {
        val b = brand.lowercase()
        val m = manufacturer.lowercase()
        return when {
            b.contains("poco") -> DeviceFamily.POCO
            b.contains("redmi") -> DeviceFamily.REDMI
            b.contains("xiaomi") || m.contains("xiaomi") -> DeviceFamily.XIAOMI
            m.contains("samsung") || b.contains("samsung") -> DeviceFamily.SAMSUNG
            m.contains("motorola") || b.contains("motorola") -> DeviceFamily.MOTOROLA
            m.contains("realme") || b.contains("realme") -> DeviceFamily.REALME
            m.contains("oneplus") || b.contains("oneplus") -> DeviceFamily.ONEPLUS
            else -> DeviceFamily.OTHER
        }
    }

    private fun readSoc(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val maker = Build.SOC_MANUFACTURER.cleanOrNull()
        val model = Build.SOC_MODEL.cleanOrNull()
        return listOfNotNull(maker, model).joinToString(" ").ifBlank { null }
    }

    private fun String?.cleanOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && !it.equals(Build.UNKNOWN, ignoreCase = true) }

    private fun defaultDisplay(): Display? = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)

    private fun readDisplayInfo(): DisplayInfo {
        val display = defaultDisplay()
        val metrics = appContext.resources.displayMetrics
        val mode = display?.mode
        val allModes = display?.supportedModes?.toList().orEmpty()
        val sameSize = allModes.filter {
            mode != null && it.physicalWidth == mode.physicalWidth && it.physicalHeight == mode.physicalHeight
        }
        val hz = sameSize.ifEmpty { allModes }
            .map { it.refreshRate.roundToInt() }
            .filter { it > 0 }
            .distinct()
            .sorted()
        return DisplayInfo(
            widthPx = mode?.physicalWidth ?: metrics.widthPixels,
            heightPx = mode?.physicalHeight ?: metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            supportedHz = hz,
        )
    }

    private fun readMemory(): ActivityManager.MemoryInfo =
        ActivityManager.MemoryInfo().also { info -> activityManager?.getMemoryInfo(info) }

    private fun readStorage(): Pair<Long, Long> = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.totalBytes to stat.availableBytes
    }.getOrDefault(0L to 0L)

    private fun readBattery(): BatteryInfo? {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return BatteryInfo(
            levelPercent = level * 100 / scale,
            temperatureCelsius = if (rawTemp == Int.MIN_VALUE) null else rawTemp / 10f,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }
}
