package com.optorchid.presentation.diagnostic

import com.optorchid.domain.model.DisplayReading
import com.optorchid.domain.model.ModeInfo
import com.optorchid.domain.model.RateSample
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Relatório em texto puro, pensado para ser copiado e colado. Não é localizado de propósito:
 * os termos técnicos (nomes de API) precisam ficar idênticos entre aparelhos.
 */
object DiagnosticReport {

    fun format(state: DiagnosticUiState, sessionStartMs: Long): String = buildString {
        appendLine("=== OptOrchid · Diagnóstico de tela (somente leitura) ===")
        val snapshot = state.snapshot
        if (snapshot == null) {
            appendLine("Coletando...")
            return@buildString
        }
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(snapshot.capturedAtWallMs))
        val b = snapshot.build
        appendLine("Gerado em: $time (hora local)")
        appendLine("Aparelho: ${b.manufacturer} / ${b.brand} / ${b.model}")
        appendLine("Android ${b.androidRelease} (API ${b.sdkInt}) · Build: ${b.buildDisplay}")
        appendLine("Nenhuma taxa foi solicitada nem forçada por este diagnóstico.")
        appendLine()

        appendLine("[1] Display via DisplayManager")
        appendDisplay(snapshot.managerDisplay)
        appendLine()

        appendLine("[2] Display via Activity")
        appendDisplay(snapshot.activityDisplay)
        val a = snapshot.managerDisplay
        val c = snapshot.activityDisplay
        if (a != null && c != null) {
            appendLine("  Concordância [1] x [2]: getRefreshRate ${if (a.refreshRate == c.refreshRate) "igual" else "DIFERENTE"}; " +
                "modeId ${if (a.activeMode?.modeId == c.activeMode?.modeId) "igual" else "DIFERENTE"}")
        }
        appendLine()

        appendLine("[3] O que a janela do OptOrchid pede ao sistema")
        val w = snapshot.windowRequest
        if (w == null) {
            appendLine("  indisponível (sem Activity)")
        } else {
            appendLine("  preferredRefreshRate=${w.preferredRefreshRate} (0 = sem preferência)")
            appendLine("  preferredDisplayModeId=${w.preferredDisplayModeId} (0 = sem preferência)")
        }
        appendLine()

        appendLine("[4] Contexto do sistema")
        val s = snapshot.system
        appendLine("  Economia de bateria: ${s.powerSaveMode ?: "indisponível"}")
        appendLine("  Tela interativa: ${s.interactive ?: "indisponível"}")
        appendLine("  Settings.System peak_refresh_rate=${s.peakRefreshRateSetting ?: "(ausente)"}")
        appendLine("  Settings.System min_refresh_rate=${s.minRefreshRateSetting ?: "(ausente)"}")
        appendLine("  (chaves do AOSP; o HyperOS pode usar outras, então ausência não prova nada)")
        appendLine()

        appendLine("[5] Eventos do DisplayListener (${state.events.size})")
        if (state.events.isEmpty()) appendLine("  nenhum evento desde que a tela foi aberta")
        state.events.forEach { e ->
            val sample = e.sample
            val values = if (sample == null) "sem leitura" else "getRefreshRate=${hz(sample.refreshRate)} modeId=${sample.modeId}"
            appendLine("  ${rel(e.atElapsedMs, sessionStartMs)} display=${e.displayId} $values${if (e.changed) " · MUDOU" else ""}")
        }
        appendLine()

        appendLine("[6] Cadência de vsync medida (Choreographer, ${state.measurements.size} medição(ões))")
        if (state.measurements.isEmpty()) appendLine("  nenhuma medição feita")
        state.measurements.forEachIndexed { index, m ->
            appendLine("  #${index + 1} início ${rel(m.startedAtElapsedMs, sessionStartMs)} · ${m.durationMs} ms · ${m.frames} quadros · toque durante a medição: ${if (m.touchedDuringMeasurement) "sim" else "não"}")
            appendLine("     antes: ${sampleText(m.before)} | depois: ${sampleText(m.after)}")
            if (m.meanHz != null) {
                appendLine("     média=${hz(m.meanHz)} mín=${m.minHz?.let { hz(it) }} máx=${m.maxHz?.let { hz(it) }}")
                appendLine("     distribuição (Hz:intervalos) ${m.histogram.joinToString(", ") { "${it.first}:${it.second}" }}")
            } else {
                appendLine("     quadros insuficientes para calcular")
            }
        }
        appendLine()

        appendLine("Notas:")
        appendLine(" - getRefreshRate() e Mode.refreshRate são a taxa informada ao app, não a frequência física do painel.")
        appendLine(" - Vsync medido não é FPS renderizado; medir exige desenhar quadros e pode influenciar a política do sistema.")
    }

    private fun StringBuilder.appendDisplay(reading: DisplayReading?) {
        if (reading == null) {
            appendLine("  indisponível")
            return
        }
        appendLine("  id=${reading.displayId} nome=\"${reading.name}\" estado=${reading.state}")
        appendLine("  getRefreshRate(): ${hz(reading.refreshRate)}")
        appendLine("  getMode(): ${reading.activeMode?.let { modeText(it) } ?: "indisponível"}")
        appendLine("  getSupportedRefreshRates() (obsoleto): ${reading.legacySupportedRates.joinToString(", ") { hz(it) }.ifBlank { "vazio" }}")
        appendLine("  getSupportedModes() (${reading.supportedModes.size}):")
        reading.supportedModes.forEach { appendLine("    ${modeText(it)}") }
    }

    private fun modeText(m: ModeInfo): String {
        val alt = if (m.alternativeRefreshRates.isEmpty()) "" else " alternativas=[${m.alternativeRefreshRates.joinToString(", ") { String.format(Locale.US, "%.1f", it) }}]"
        return "modeId=${m.modeId} ${m.widthPx}x${m.heightPx} @ ${hz(m.refreshRate)}$alt"
    }

    private fun sampleText(s: RateSample?): String =
        if (s == null) "sem leitura" else "${hz(s.refreshRate)} modeId=${s.modeId}"

    private fun hz(value: Float): String = String.format(Locale.US, "%.3f Hz", value)

    private fun rel(atMs: Long, startMs: Long): String =
        String.format(Locale.US, "+%.2fs", (atMs - startMs) / 1000.0)
}
