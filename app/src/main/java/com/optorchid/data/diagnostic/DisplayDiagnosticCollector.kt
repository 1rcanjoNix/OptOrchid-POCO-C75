package com.optorchid.data.diagnostic

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Choreographer
import android.view.Display
import com.optorchid.domain.model.BuildSummary
import com.optorchid.domain.model.DisplayDiagnosticSnapshot
import com.optorchid.domain.model.DisplayReading
import com.optorchid.domain.model.ModeInfo
import com.optorchid.domain.model.RateSample
import com.optorchid.domain.model.SystemContext
import com.optorchid.domain.model.VsyncMeasurement
import com.optorchid.domain.model.WindowRequest
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Coleta SOMENTE LEITURA. Nada aqui pede, define ou força taxa de atualização:
 * não há setFrameRate, preferredRefreshRate nem preferredDisplayModeId.
 */
class DisplayDiagnosticCollector(context: Context) {

    private val appContext = context.applicationContext
    private val displayManager = appContext.getSystemService(DisplayManager::class.java)
    private val powerManager = appContext.getSystemService(PowerManager::class.java)

    fun collect(activity: Activity?): DisplayDiagnosticSnapshot {
        val managerDisplay = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val activityDisplay = activity?.let { activityDisplayOf(it) }
        val attributes = activity?.window?.attributes
        return DisplayDiagnosticSnapshot(
            capturedAtWallMs = System.currentTimeMillis(),
            elapsedRealtimeMs = SystemClock.elapsedRealtime(),
            build = BuildSummary(
                manufacturer = Build.MANUFACTURER.orEmpty(),
                brand = Build.BRAND.orEmpty(),
                model = Build.MODEL.orEmpty(),
                androidRelease = Build.VERSION.RELEASE.orEmpty(),
                sdkInt = Build.VERSION.SDK_INT,
                buildDisplay = Build.DISPLAY.orEmpty(),
            ),
            managerDisplay = managerDisplay?.let { readDisplay("DisplayManager (DEFAULT_DISPLAY)", it) },
            activityDisplay = activityDisplay?.let { readDisplay("Activity (display da janela)", it) },
            windowRequest = attributes?.let {
                WindowRequest(
                    preferredRefreshRate = it.preferredRefreshRate,
                    preferredDisplayModeId = it.preferredDisplayModeId,
                )
            },
            system = SystemContext(
                powerSaveMode = runCatching { powerManager?.isPowerSaveMode }.getOrNull(),
                interactive = runCatching { powerManager?.isInteractive }.getOrNull(),
                peakRefreshRateSetting = readSetting("peak_refresh_rate"),
                minRefreshRateSetting = readSetting("min_refresh_rate"),
            ),
        )
    }

    fun sampleRate(displayId: Int = Display.DEFAULT_DISPLAY): RateSample? {
        val display = displayManager?.getDisplay(displayId) ?: return null
        return RateSample(
            refreshRate = display.refreshRate,
            modeId = runCatching { display.mode.modeId }.getOrNull(),
        )
    }

    fun registerListener(listener: DisplayManager.DisplayListener) {
        displayManager?.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
    }

    fun unregisterListener(listener: DisplayManager.DisplayListener) {
        displayManager?.unregisterDisplayListener(listener)
    }

    /**
     * Mede o intervalo entre callbacks de vsync do Choreographer durante [durationMs].
     * Observação: medir exige desenhar quadros continuamente, o que pode influenciar a política do sistema.
     * Por isso o relatório compara a taxa informada antes e depois.
     */
    suspend fun measureVsync(durationMs: Long): VsyncMeasurement = withContext(Dispatchers.Main) {
        val startedAt = SystemClock.elapsedRealtime()
        val timestamps = ArrayList<Long>()
        suspendCancellableCoroutine<Unit> { continuation ->
            val choreographer = Choreographer.getInstance()
            val callback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    timestamps.add(frameTimeNanos)
                    if (SystemClock.elapsedRealtime() - startedAt < durationMs) {
                        choreographer.postFrameCallback(this)
                    } else if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
            continuation.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
            choreographer.postFrameCallback(callback)
        }
        summarize(startedAt, durationMs, timestamps)
    }

    private fun summarize(startedAt: Long, durationMs: Long, timestamps: List<Long>): VsyncMeasurement {
        val empty = VsyncMeasurement(startedAt, durationMs, timestamps.size, null, null, null, emptyList(), false, null, null)
        if (timestamps.size < 2) return empty
        val deltas = timestamps.zipWithNext { a, b -> b - a }.filter { it > 0 }
        if (deltas.isEmpty()) return empty
        val spanNs = timestamps.last() - timestamps.first()
        val ns = 1_000_000_000f
        return VsyncMeasurement(
            startedAtElapsedMs = startedAt,
            durationMs = durationMs,
            frames = timestamps.size,
            meanHz = if (spanNs > 0) (timestamps.size - 1) * ns / spanNs.toFloat() else null,
            minHz = ns / deltas.max().toFloat(),
            maxHz = ns / deltas.min().toFloat(),
            histogram = deltas
                .groupingBy { (1_000_000_000.0 / it).roundToInt() }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(6)
                .map { it.key to it.value },
            touchedDuringMeasurement = false,
            before = null,
            after = null,
        )
    }

    @Suppress("DEPRECATION")
    private fun activityDisplayOf(activity: Activity): Display? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            activity.windowManager.defaultDisplay
        }
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun readDisplay(label: String, display: Display): DisplayReading = DisplayReading(
        label = label,
        displayId = display.displayId,
        name = display.name.orEmpty(),
        state = stateName(display.state),
        refreshRate = display.refreshRate,
        activeMode = runCatching { display.mode.toInfo() }.getOrNull(),
        supportedModes = runCatching { display.supportedModes.map { it.toInfo() } }.getOrDefault(emptyList()),
        legacySupportedRates = runCatching { display.supportedRefreshRates.toList() }.getOrDefault(emptyList()),
    )

    private fun Display.Mode.toInfo(): ModeInfo = ModeInfo(
        modeId = modeId,
        widthPx = physicalWidth,
        heightPx = physicalHeight,
        refreshRate = refreshRate,
        alternativeRefreshRates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alternativeRefreshRates.toList()
        } else {
            emptyList()
        },
    )

    private fun stateName(state: Int): String = when (state) {
        Display.STATE_UNKNOWN -> "UNKNOWN"
        Display.STATE_OFF -> "OFF"
        Display.STATE_ON -> "ON"
        Display.STATE_DOZE -> "DOZE"
        Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND"
        Display.STATE_ON_SUSPEND -> "ON_SUSPEND"
        else -> "código $state"
    }

    /** Lê uma chave de Settings.System sem permissão. Chaves ausentes retornam null. */
    private fun readSetting(key: String): String? =
        runCatching { Settings.System.getString(appContext.contentResolver, key) }.getOrNull()
}
