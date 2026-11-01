package com.optorchid.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.optorchid.core.di.AppContainer
import com.optorchid.domain.model.BasicAlert
import com.optorchid.domain.model.BasicAlerts
import com.optorchid.domain.model.DeviceProfile
import com.optorchid.domain.model.QuickSnapshot
import com.optorchid.domain.model.ResourceCheck
import com.optorchid.domain.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val profile: DeviceProfile? = null,
    val snapshot: QuickSnapshot? = null,
    val resources: List<ResourceCheck> = emptyList(),
    val alerts: List<BasicAlert> = emptyList(),
)

class DashboardViewModel(private val devices: DeviceRepository) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    /** Leitura pontual, chamada quando a tela volta ao primeiro plano. Não há polling. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.Default) {
                val profile = devices.profile()
                val snapshot = devices.snapshot()
                DashboardUiState(
                    profile = profile,
                    snapshot = snapshot,
                    resources = devices.resourceChecks(profile, snapshot),
                    alerts = BasicAlerts.evaluate(snapshot),
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { DashboardViewModel(container.deviceRepository) }
        }
    }
}
