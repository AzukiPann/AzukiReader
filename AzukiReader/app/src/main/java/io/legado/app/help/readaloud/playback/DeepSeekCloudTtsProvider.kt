package io.legado.app.help.readaloud.playback

import io.legado.app.domain.model.readaloud.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** DeepSeek tools/tts is a separate protocol, not OpenAI audio/speech. */
internal class DeepSeekCloudTtsProvider(
    private val client: () -> OkHttpClient = { cloudTtsHttpClient },
) : CloudTtsProvider {
    override val type = CloudTtsProviderType.DeepSeek

    override fun fetchVoices(engine: CloudTtsEngine) = listOf(
        CloudTtsVoiceDescriptor("stella", "海星 · 女声"),
        CloudTtsVoiceDescriptor("tide", "暗潮 · 男声"),
    )

    override fun synthesize(engine: CloudTtsEngine, request: CloudTtsSynthesisRequest): CloudTtsAudio {
        require(engine.apiKey.isNotBlank()) { "DeepSeek API Key 不能为空" }
        require(request.text.isNotBlank()) { "朗读正文不能为空" }
        val text = request.text
        val billedCharacters = text.codePoints().toArray().sumOf {
            if (Character.UnicodeScript.of(it) == Character.UnicodeScript.HAN) 2 else 1
        }
        require(billedCharacters <= 10_000) { "DeepSeek 单次朗读不能超过 10,000 个计费字符，请减小朗读分段" }
        val voice = request.voiceId.ifBlank { "stella" }
        require(voice in setOf("stella", "tide")) { "DeepSeek 音色须为 stella 或 tide" }
        val body = JSONObject().put("text", text).put("voice_id", voice)
            .put("format", "wav").put("filter_markdown", true).toString().toByteArray(Charsets.UTF_8)
        require(body.size <= 64 * 1024) { "DeepSeek 请求正文不能超过 64 KiB" }
        val base = engine.baseUrl.ifBlank { "https://api.deepseek.com/v1" }.trimEnd('/')
        val endpoint = when {
            base.endsWith("/tools/tts") -> base
            base.endsWith("/v1") -> "$base/tools/tts"
            else -> "$base/v1/tools/tts"
        }
        val httpRequest = Request.Builder().url(endpoint)
            .header("Authorization", "Bearer ${engine.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType())).build()
        return client().newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                // Do not surface response bodies which may echo credentials.
                error("DeepSeek 语音合成失败：HTTP ${response.code}")
            }
            require(response.header("Content-Type").orEmpty().substringBefore(';').trim() in
                setOf("audio/wav", "audio/x-wav", "audio/wave")) { "DeepSeek 未返回 WAV 音频" }
            val bytes = response.body.bytes() // Premature HTTP EOF throws; no partial file is saved.
            require(bytes.size > 44 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF" &&
                bytes.copyOfRange(8, 12).decodeToString() == "WAVE") { "DeepSeek 返回的音频无效或不完整" }
            val declared = (0..3).fold(0L) { value, i -> value or ((bytes[4 + i].toLong() and 255) shl (8 * i)) }
            require(declared == 0L || declared == 0xffffffffL || declared + 8 <= bytes.size) {
                "DeepSeek 音频传输提前结束，请重试当前段落"
            }
            CloudTtsAudio(bytes, "wav")
        }
    }
}
