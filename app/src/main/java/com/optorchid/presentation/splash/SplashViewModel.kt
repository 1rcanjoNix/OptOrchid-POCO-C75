package com.optorchid.presentation.splash

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.optorchid.core.di.AppContainer
import com.optorchid.domain.repository.SettingsRepository
import com.optorchid.domain.usecase.StartupSequence
import com.optorchid.domain.usecase.StartupStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel(
    private val settings: SettingsRepository,
    private val startup: StartupSequence,
) : ViewModel() {

    data class State(val stage: StartupStage = StartupStage.INITIALIZING, val ready: Boolean = false)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch { runStartup() }
    }

    private suspend fun runStartup() {
        // Etapa INITIALIZING: leitura real das preferências.
        val reduceMotion = settings.settings.first().reduceMotion
        var stageShownAt = SystemClock.elapsedRealtime()

        startup.run { next ->
            // As operações levam milissegundos. Cada mensagem fica visível por um tempo mínimo
            // apenas para ser legível; o trabalho já foi concluído. Sem porcentagens.
            if (!reduceMotion) {
                val shownFor = SystemClock.elapsedRealtime() - stageShownAt
                if (shownFor < MIN_STAGE_MS) delay(MIN_STAGE_MS - shownFor)
            }
            _state.update { it.copy(stage = next) }
            stageShownAt = SystemClock.elapsedRealtime()
        }

        if (!reduceMotion) delay(READY_HOLD_MS)
        _state.update { it.copy(ready = true) }
    }

    companion object {
        private const val MIN_STAGE_MS = 600L
        private const val READY_HOLD_MS = 500L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SplashViewModel(container.settingsRepository, container.startupSequence) }
        }
    }
}
