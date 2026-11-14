package io.legado.app.feature.bookmarks.export

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.domain.gateway.ExcerptExportGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Stable
data class ExcerptExportUiState(
    val exporting: Boolean = false,
    val exportedCount: Int? = null,
    val failed: Boolean = false,
)

sealed interface ExcerptExportIntent {
    data class Export(val destination: String) : ExcerptExportIntent
    data object MessageShown : ExcerptExportIntent
}

class ExportAllExcerptsViewModel(private val gateway: ExcerptExportGateway) : ViewModel() {
    private val _uiState = MutableStateFlow(ExcerptExportUiState())
    val uiState = _uiState.asStateFlow()

    fun onIntent(intent: ExcerptExportIntent) {
        when (intent) {
            ExcerptExportIntent.MessageShown -> _uiState.value = _uiState.value.copy(exportedCount = null, failed = false)
            is ExcerptExportIntent.Export -> {
                if (_uiState.value.exporting) return
                _uiState.value = ExcerptExportUiState(exporting = true)
                viewModelScope.launch {
                    try {
                        val count = gateway.exportAll(intent.destination)
                        _uiState.value = ExcerptExportUiState(exportedCount = count)
                    } catch (cancelled: CancellationException) {
                        _uiState.value = ExcerptExportUiState()
                        throw cancelled
                    } catch (_: Exception) {
                        _uiState.value = ExcerptExportUiState(failed = true)
                    }
                }
            }
        }
    }
}
