package com.optorchid.domain.model

/** Modelos do diagnóstico de tela (somente leitura). Valores brutos, sem arredondar nem filtrar. */

data class ModeInfo(
    val modeId: Int,
    val widthPx: Int,
    val heightPx: Int,
    val refreshRate: Float,
    /** Display.Mode.getAlternativeRefreshRates() (Android 12+); vazio em versões anteriores. */
    val alternativeRefreshRates: List<Float>,
)

data class DisplayReading(
    val label: String,
    val displayId: Int,
    val name: String,
    val state: String,
    /** Display.getRefreshRate(): taxa informada ao app, não a frequência física do painel. */
    val refreshRate: Float,
    val activeMode: ModeInfo?,
    val supportedModes: List<ModeInfo>,
    /** Display.getSupportedRefreshRates() (obsoleto), listado só para comparação. */
    val legacySupportedRates: List<Float>,
)

data class RateSample(val refreshRate: Float, val modeId: Int?)

/** O que a janela do OptOrchid pede ao sistema. Zero significa "sem preferência". */
data class WindowRequest(val preferredRefreshRate: Float, val preferredDisplayModeId: Int)

data class SystemContext(
    val powerSaveMode: Boolean?,
    val interactive: Boolean?,
    val peakRefreshRateSetting: String?,
    val minRefreshRateSetting: String?,
)

data class BuildSummary(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val buildDisplay: String,
)

data class DisplayDiagnosticSnapshot(
    val capturedAtWallMs: Long,
    val elapsedRealtimeMs: Long,
    val build: BuildSummary,
    val managerDisplay: DisplayReading?,
    val activityDisplay: DisplayReading?,
    val windowRequest: WindowRequest?,
    val system: SystemContext,
)

data class DisplayEvent(
    val atElapsedMs: Long,
    val displayId: Int,
    val sample: RateSample?,
    val changed: Boolean,
)

/** Cadência de vsync entregue ao app por Choreographer. Não é FPS renderizado nem a frequência do painel. */
data class VsyncMeasurement(
    val startedAtElapsedMs: Long,
    val durationMs: Long,
    val frames: Int,
    val meanHz: Float?,
    val minHz: Float?,
    val maxHz: Float?,
    /** Pares (Hz arredondado, quantidade de intervalos), do mais frequente ao menos. */
    val histogram: List<Pair<Int, Int>>,
    val touchedDuringMeasurement: Boolean,
    val before: RateSample?,
    val after: RateSample?,
)
