package io.legado.app.feature.performance

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.domain.model.settings.AppShellSettings
import io.legado.app.domain.model.settings.ThemeSettings
import io.legado.app.domain.model.settings.Wenku8PerformanceEffect
import io.legado.app.domain.model.settings.retain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Stable
data class PerformanceUiState(
    val theme: ThemeSettings = ThemeSettings(),
    val shell: AppShellSettings = AppShellSettings(),
    val saveFailed: Boolean = false,
)

sealed interface PerformanceIntent {
    data class Discovery(val visible: Boolean) : PerformanceIntent
    data class Rss(val visible: Boolean) : PerformanceIntent
    data class ReduceMotion(val enabled: Boolean) : PerformanceIntent
    data class LowPerformance(val enabled: Boolean) : PerformanceIntent
    data class Retain(val effect: Wenku8PerformanceEffect, val enabled: Boolean) : PerformanceIntent
    data class Blur(val value: Float) : PerformanceIntent
    data class Opacity(val value: Float) : PerformanceIntent
    data class RefreshRate(val hz: Int) : PerformanceIntent
    data class ReaderRefreshRate(val hz: Int) : PerformanceIntent
    data object ResetGlass : PerformanceIntent
    data object ResetLightweight : PerformanceIntent
}

class PerformanceViewModel(
    private val themeGateway: ThemeSettingsGateway,
    private val shellGateway: AppShellSettingsGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PerformanceUiState(themeGateway.currentSettings, shellGateway.currentSettings))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { themeGateway.settings.collect { theme -> _uiState.update { it.copy(theme = theme) } } }
        viewModelScope.launch { shellGateway.settings.collect { shell -> _uiState.update { it.copy(shell = shell) } } }
    }

    fun onIntent(intent: PerformanceIntent) {
        viewModelScope.launch {
            try {
                when (intent) {
                    is PerformanceIntent.Discovery -> shellGateway.update { it.copy(showDiscovery = intent.visible) }
                    is PerformanceIntent.Rss -> shellGateway.update { it.copy(showRss = intent.visible) }
                    else -> themeGateway.update { current ->
                        when (intent) {
                            is PerformanceIntent.ReduceMotion -> current.copy(wenku8ReducedEffects = intent.enabled)
                            is PerformanceIntent.LowPerformance -> current.copy(wenku8LowPerformance = intent.enabled)
                            is PerformanceIntent.Retain -> if (current.wenku8LowPerformance) current.retain(intent.effect, intent.enabled) else current
                            is PerformanceIntent.Blur -> current.copy(wenku8GlassBlur = intent.value.coerceIn(0f, 24f))
                            is PerformanceIntent.Opacity -> current.copy(wenku8GlassOpacity = intent.value.coerceIn(0f, 1f))
                            is PerformanceIntent.RefreshRate -> current.copy(wenku8RefreshRate = intent.hz.coerceAtLeast(-1))
                            is PerformanceIntent.ReaderRefreshRate -> current.copy(readerRefreshRate = intent.hz.coerceAtLeast(-1))
                            PerformanceIntent.ResetGlass -> current.copy(wenku8GlassBlur = 8f, wenku8GlassOpacity = .87f)
                            PerformanceIntent.ResetLightweight -> current.copy(wenku8LowPerformance = true, wenku8RetainedEffects = 0)
                            else -> current
                        }
                    }
                }
                _uiState.update { it.copy(saveFailed = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saveFailed = true) }
            }
        }
    }
}
