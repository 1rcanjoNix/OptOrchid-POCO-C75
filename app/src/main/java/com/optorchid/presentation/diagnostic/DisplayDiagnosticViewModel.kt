package com.optorchid.presentation.diagnostic

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.optorchid.core.di.AppContainer
import com.optorchid.data.diagnostic.DisplayDiagnosticCollector
import com.optorchid.domain.model.DisplayDiagnosticSnapshot
import com.optorchid.domain.model.DisplayEvent
import com.optorchid.domain.model.RateSample
import com.optorchid.domain.model.VsyncMeasurement
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiagnosticUiState(
    val snapshot: DisplayDiagnosticSnapshot? = null,
    val events: List<DisplayEvent> = emptyList(),
    val measurements: List<VsyncMeasurement> = emptyList(),
    val measuring: Boolean = false,
)

class DisplayDiagnosticViewModel(private val collector: DisplayDiagnosticCollector) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticUiState())
    val state: StateFlow<DiagnosticUiState> = _state.asStateFlow()

    /** Referência de tempo para exibir eventos como "+12,3 s" no relatório. */
    val sessionStartMs: Long = SystemClock.elapsedRealtime()

    @Volatile
    private var touched = false
    private var listener: DisplayManager.DisplayListener? = null
    private val lastSampleByDisplay = mutableMapOf<Int, RateSample?>()

    fun refresh(activity: Activity?) {
        _state.update { it.copy(snapshot = collector.collect(activity)) }
    }

    fun onTouch() {
        touched = true
    }

    fun measure(activity: Activity?) {
        if (_state.value.measuring) return
        val activityRef = WeakReference(activity)
        viewModelScope.launch {
            _state.update { it.copy(measuring = true) }
            touched = false
            val before = collector.sampleRate()
            val raw = collector.measureVsync(MEASURE_MS)
            val after = collector.sampleRate()
            val measurement = raw.copy(touchedDuringMeasurement = touched, before = before, after = after)
            _state.update {
                it.copy(
                    measuring = false,
                    measurements = (it.measurements + measurement).takeLast(10),
                    snapshot = collector.collect(activityRef.get()),
                )
            }
        }
    }

    fun startListening() {
        if (listener != null) return
        val newListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                val sample = collector.sampleRate(displayId)
                val previous = lastSampleByDisplay[displayId]
                val changed = lastSampleByDisplay.containsKey(displayId) && previous != sample
                lastSampleByDisplay[displayId] = sample
                val event = DisplayEvent(SystemClock.elapsedRealtime(), displayId, sample, changed)
                _state.update { it.copy(events = (it.events + event).takeLast(MAX_EVENTS)) }
            }
        }
        // Amostra inicial para que a primeira mudança seja marcada corretamente.
        lastSampleByDisplay[android.view.Display.DEFAULT_DISPLAY] = collector.sampleRate()
        collector.registerListener(newListener)
        listener = newListener
    }

    fun stopListening() {
        listener?.let { collector.unregisterListener(it) }
        listener = null
    }

    override fun onCleared() {
        stopListening()
    }

    companion object {
        private const val MEASURE_MS = 3_000L
        private const val MAX_EVENTS = 60

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { DisplayDiagnosticViewModel(container.displayDiagnosticCollector) }
        }
    }
}
