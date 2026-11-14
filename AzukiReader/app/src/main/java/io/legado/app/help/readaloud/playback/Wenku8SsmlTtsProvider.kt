package io.legado.app.help.readaloud.playback

import io.legado.app.domain.model.readaloud.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Exact HTTPS Microsoft SSML endpoint; never forwards credentials through redirects. */
internal class Wenku8SsmlTtsProvider(
    private val client: OkHttpClient = wenku8SpeechHttpClient.newBuilder()
        .followRedirects(false).followSslRedirects(false).callTimeout(35, TimeUnit.SECONDS).build(),
) : CloudTtsProvider {
    override val type = CloudTtsProviderType.Wenku8Ssml
    override fun fetchVoices(engine: CloudTtsEngine) = emptyList<CloudTtsVoiceDescriptor>()
    override fun synthesize(engine: CloudTtsEngine, request: CloudTtsSynthesisRequest): CloudTtsAudio {
        validateWenku8SsmlConfig(engine.baseUrl, request.voiceId, request.locale.ifBlank { "zh-CN" }, engine.name, engine.region, engine.apiKey)
        val locale = request.locale.ifBlank { "zh-CN" }
        val ssml = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"${locale.escapeXml()}\"><voice name=\"${request.voiceId.escapeXml()}\">${wenku8SpeechText(request.text).escapeXml()}</voice></speak>"
        val builder = Request.Builder().url(engine.baseUrl)
            .header("X-Microsoft-OutputFormat", "audio-24khz-48kbitrate-mono-mp3")
            .header("User-Agent", "AzukiReader")
            .post(ssml.toRequestBody("application/ssml+xml".toMediaType()))
        when (engine.region) {
            "subscriptionKey" -> builder.header("Ocp-Apim-Subscription-Key", engine.apiKey)
            "bearer" -> builder.header("Authorization", "Bearer ${engine.apiKey}")
        }
        return client.newCall(builder.build()).execute().use { response ->
            // The server may echo credentials or book content; never surface its body.
            if (response.code in setOf(400, 408, 422)) throw Wenku8SpeechFailure("自定义音源无法处理这一段", response.code != 408)
            if (!response.isSuccessful) throw Wenku8TransportFailure("自定义语音接口请求失败：HTTP ${response.code}")
            val source = response.body.source()
            val audio = okio.Buffer()
            while (source.read(audio, 8192) != -1L) check(audio.size <= 8 * 1024 * 1024) { "单段语音超过缓存上限" }
            if (audio.size < 100) throw Wenku8SpeechFailure("自定义接口未返回有效音频", true)
            CloudTtsAudio(audio.readByteArray(), "mp3")
        }
    }
}

internal fun validateWenku8SsmlConfig(endpoint: String, voice: String, locale: String, label: String, auth: String, secret: String) {
    val uri = runCatching { java.net.URI(endpoint) }.getOrNull()
    require(uri != null && uri.scheme == "https" && !uri.host.isNullOrEmpty() && uri.userInfo == null && uri.fragment == null && endpoint.length <= 2048) { "请填写完整的 HTTPS 语音合成地址" }
    require(Regex("^[A-Za-z0-9_.-]{1,160}$").matches(voice) && Regex("^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8}){0,3}$").matches(locale) && label.isNotBlank() && label.length <= 80) { "请检查音色 ID、语言代码和名称" }
    require(auth in setOf("none", "subscriptionKey", "bearer") && secret.length <= 8192 && !Regex("[\r\n\u0000]").containsMatchIn(secret) && (auth == "none" || secret.isNotBlank())) { "请填写有效的密钥或令牌" }
}
