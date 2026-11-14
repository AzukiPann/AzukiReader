package io.legado.app.feature.readaloud.wenku8

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.domain.gateway.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Stable
data class ListeningUiState(val playback: Wenku8ListeningState = Wenku8ListeningState(),
    val busy: Boolean = false, val message: String? = null, val customOpen: Boolean = false)

sealed interface ListeningIntent {
    data object PlayPause : ListeningIntent
    data object Stop : ListeningIntent
    data class Move(val direction:Int) : ListeningIntent
    data object Skip : ListeningIntent
    data class Seek(val fraction:Float) : ListeningIntent
    data class FromReader(val resume:Boolean) : ListeningIntent
    data class Speed(val value:Float) : ListeningIntent
    data class Follow(val value:Boolean) : ListeningIntent
    data class Sleep(val minutes:Int=0,val afterChapter:Boolean=false) : ListeningIntent
    data class Voice(val id:String) : ListeningIntent
    data class Source(val id:String) : ListeningIntent
    data object Refresh : ListeningIntent
    data object OpenCustom : ListeningIntent
    data object CloseCustom : ListeningIntent
    data class SaveCustom(val config:ListeningCustomConfig,val secret:String) : ListeningIntent
    data object DeleteCustom : ListeningIntent
    data class PreviewCustom(val config:ListeningCustomConfig,val secret:String,val text:String) : ListeningIntent
    data object StopPreview : ListeningIntent
    data object ClearMessage : ListeningIntent
}

class Wenku8ListeningViewModel(private val gateway:Wenku8ListeningGateway, private val reader:Boolean) : ViewModel() {
    private val local = MutableStateFlow(ListeningUiState())
    val uiState = combine(gateway.observe(reader),local) { playback, transient -> transient.copy(playback=playback) }
        .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),ListeningUiState())
    private var previewJob:Job?=null
    private var busyActions=0
    init { action { gateway.initialize() } }
    fun onIntent(intent:ListeningIntent) {
        when(intent) {
            ListeningIntent.OpenCustom -> local.update { it.copy(customOpen=true,message=null) }
            ListeningIntent.CloseCustom -> { previewJob?.cancel(); gateway.stopPreview(); local.update { it.copy(customOpen=false) } }
            ListeningIntent.StopPreview -> { previewJob?.cancel(); gateway.stopPreview() }
            ListeningIntent.ClearMessage -> local.update { it.copy(message=null) }
            is ListeningIntent.PreviewCustom -> {
                previewJob?.cancel()
                previewJob=action(true) { gateway.previewCustom(intent.config,intent.secret,intent.text) }
            }
            ListeningIntent.PlayPause -> action { gateway.togglePause(reader) }
            ListeningIntent.Stop -> action { gateway.stop() }
            is ListeningIntent.Move -> action { gateway.moveParagraph(intent.direction) }
            ListeningIntent.Skip -> action { gateway.skipSegment() }
            is ListeningIntent.Seek -> action { gateway.seek(intent.fraction) }
            is ListeningIntent.FromReader -> action { gateway.startFromReader(intent.resume) }
            is ListeningIntent.Speed -> action { gateway.setSpeed(intent.value) }
            is ListeningIntent.Follow -> action { gateway.setFollow(intent.value) }
            is ListeningIntent.Sleep -> action { gateway.setSleep(intent.minutes,intent.afterChapter) }
            is ListeningIntent.Voice -> action { gateway.selectVoice(intent.id) }
            is ListeningIntent.Source -> {
                if(intent.id=="custom" && uiState.value.playback.custom==null) onIntent(ListeningIntent.OpenCustom)
                else action { gateway.selectVoice("source:${intent.id}") }
            }
            ListeningIntent.Refresh -> action(true) { gateway.refreshVoices() }
            is ListeningIntent.SaveCustom -> action(true) {
                gateway.saveCustom(intent.config,intent.secret)
                gateway.selectVoice("source:custom")
                local.update { it.copy(customOpen=false) }
            }
            ListeningIntent.DeleteCustom -> action(true) { gateway.deleteCustom(); local.update { it.copy(customOpen=false) } }
        }
    }
    private fun action(busy:Boolean=false, block:suspend ()->Unit):Job = viewModelScope.launch {
        if(busy) { busyActions++; local.update { it.copy(busy=true,message=null) } }
        try { block() }
        catch(cancel:CancellationException) { throw cancel }
        catch(error:Exception) { local.update { it.copy(message=error.message ?: "听书操作暂未完成，请重试") } }
        finally { if(busy) { busyActions--; local.update { it.copy(busy=busyActions>0) } } }
    }
    override fun onCleared() { previewJob?.cancel(); gateway.stopPreview() }
}
