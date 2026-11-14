package io.legado.app.domain.gateway

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow

@Stable
data class ListeningVoice(val id: String, val name: String, val source: String, val selected: Boolean = false)

@Stable
data class ListeningCustomConfig(
    val endpoint: String = "", val voiceId: String = "zh-CN-XiaoxiaoNeural", val locale: String = "zh-CN",
    val label: String = "自定义音色", val authentication: String = "none", val hasSecret: Boolean = false,
)

@Stable
data class Wenku8ListeningState(
    val bookName: String = "", val bookUrl: String = "", val chapterIndex: Int = -1,
    val chapterTitle: String = "", val currentText: String = "", val segmentIndex: Int = 0, val segmentCount: Int = 0,
    val fraction: Float = 0f, val active: Boolean = false, val paused: Boolean = true,
    val preparing: Boolean = false, val otherBookPlaying: Boolean = false,
    val clipPositionMs: Long = 0, val clipDurationMs: Long = 0,
    val error: String? = null, val skippedSegments: Int = 0,
    val source: String = "edge", val voiceLabel: String = "晓晓 · 中文女声", val speed: Float = 1f,
    val follow: Boolean = true, val timerMinutes: Int = 0, val sleepAfterChapter: Boolean = false,
    val voices: ImmutableList<ListeningVoice> = persistentListOf(), val custom: ListeningCustomConfig? = null,
    val resumeChapter: Int? = null,
)

interface Wenku8ListeningGateway {
    suspend fun initialize()
    fun observe(reader: Boolean): Flow<Wenku8ListeningState>
    suspend fun togglePause(reader: Boolean)
    suspend fun stop()
    suspend fun moveParagraph(direction: Int)
    suspend fun skipSegment()
    suspend fun seek(fraction: Float)
    suspend fun startFromReader(resume: Boolean)
    suspend fun setSpeed(speed: Float)
    suspend fun setFollow(follow: Boolean)
    suspend fun setSleep(minutes: Int = 0, afterChapter: Boolean = false)
    suspend fun selectVoice(id: String)
    suspend fun refreshVoices()
    suspend fun saveCustom(config: ListeningCustomConfig, secret: String)
    suspend fun deleteCustom()
    suspend fun previewCustom(config: ListeningCustomConfig, secret: String, text: String)
    fun stopPreview()
}
