// SPDX-License-Identifier: LGPL-3.0-only
// Kotlin adaptation of Wenku8 / rany2 edge-tts. See assets/licenses/wenku8-edge-tts.txt.
package io.legado.app.help.readaloud.playback

import io.legado.app.domain.model.readaloud.*
import io.legado.app.help.readaloud.wenku8.Wenku8EdgeProtocol
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.ByteString
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class Wenku8EdgeTtsProvider(
    private val client: OkHttpClient = wenku8SpeechHttpClient.newBuilder()
        .callTimeout(35, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build(),
    private val endpoint: HttpUrl = "https://speech.platform.bing.com/".toHttpUrl(),
) : CloudTtsProvider {
    override val type = CloudTtsProviderType.Wenku8Edge
    @Volatile private var clockSkew = 0L

    override fun fetchVoices(engine: CloudTtsEngine): List<CloudTtsVoiceDescriptor> {
        val request = headers(Request.Builder().url(endpoint.newBuilder().encodedPath("$route/voices/list").addQueryParameter("trustedclienttoken", Wenku8EdgeProtocol.clientIdentifier).build()))
            .get().build()
        return client.newCall(request).execute().use { response ->
            updateClock(response)
            check(response.isSuccessful) { "在线音色列表暂未连接，可继续使用已有音色" }
            val source = response.body.source()
            val buffer = okio.Buffer()
            while (source.read(buffer, 8192) != -1L) check(buffer.size <= 2 * 1024 * 1024) { "在线音色列表过大" }
            val bytes = buffer.readByteArray()
            val list = JSONArray(bytes.toString(Charsets.UTF_8))
            buildList {
                repeat(list.length()) { index ->
                    val voice = list.getJSONObject(index)
                    val id = voice.optString("ShortName")
                    val locale = voice.optString("Locale")
                    if (locale.startsWith("zh-") && Regex("^[a-zA-Z0-9-]+$").matches(id)) {
                        add(CloudTtsVoiceDescriptor(id, when (id) {
                            "zh-CN-XiaoxiaoNeural" -> "晓晓 · 中文女声"
                            "zh-CN-YunxiNeural" -> "云希 · 中文男声"
                            else -> voice.optString("FriendlyName", id) + if (voice.optString("Gender") == "Female") " · 女声" else " · 男声"
                        }, locale, voice.optString("Gender")))
                    }
                }
            }
        }
    }

    override fun synthesize(engine: CloudTtsEngine, request: CloudTtsSynthesisRequest): CloudTtsAudio {
        require(Regex("^zh-(?:[A-Za-z0-9]+-)+[A-Za-z0-9]+Neural$").matches(request.voiceId) && request.voiceId.length <= 120) { "在线朗读音色无效，请重新选择音色" }
        val text = wenku8SpeechText(request.text)
        require(text.codePoints().anyMatch { Character.isLetterOrDigit(it) } && text.escapeXml().toByteArray().size <= 3500) { "这一段没有可朗读的文本或过长" }
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try { return synthesizeTurn(request, text) }
            catch (error: EdgeTurnFailure) {
                if (attempt != 0 || error.code != 403) throw Wenku8TransportFailure(if (error.code > 0) "在线音源握手失败：HTTP ${error.code}，请稍后重试" else "在线音源连接失败，请检查网络后重试", error)
                runCatching { fetchVoices(engine) }
                lastError = error
            }
        }
        throw IOException("在线音源连接失败", lastError)
    }

    private fun synthesizeTurn(request: CloudTtsSynthesisRequest, text: String): CloudTtsAudio {
        val now = Instant.ofEpochMilli(System.currentTimeMillis() + clockSkew)
        val url = endpoint.newBuilder().encodedPath("$route/edge/v1")
            .addQueryParameter("TrustedClientToken", Wenku8EdgeProtocol.clientIdentifier)
            .addQueryParameter("ConnectionId", connectionId())
            .addQueryParameter("Sec-MS-GEC", Wenku8EdgeProtocol.timeProof(now.toEpochMilli()))
            .addQueryParameter("Sec-MS-GEC-Version", Wenku8EdgeProtocol.version).build()
        val done = CountDownLatch(1)
        val audio = ByteArrayOutputStream()
        var failure: Throwable? = null
        var ended = false
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                updateClock(response)
                val timestamp = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US).withZone(ZoneOffset.UTC).format(now)
                webSocket.send("X-Timestamp:$timestamp\r\nContent-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                    "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"true\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n")
                val locale = request.locale.ifBlank { request.voiceId.split('-').take(2).joinToString("-") }
                val ssml = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xml:lang=\"${locale.escapeXml()}\"><voice name=\"${request.voiceId.escapeXml()}\"><prosody pitch=\"+0Hz\" rate=\"+0%\" volume=\"+0%\">${text.escapeXml()}</prosody></voice></speak>"
                webSocket.send("X-RequestId:${connectionId()}\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:${timestamp}Z\r\nPath:ssml\r\n\r\n$ssml")
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                try {
                    val (path, body) = binaryAudio(bytes.toByteArray())
                    check(path == "audio") { "在线语音返回了未知的数据类型" }
                    check(audio.size() + body.size <= 8 * 1024 * 1024) { "单段语音超过缓存上限" }
                    audio.write(body)
                } catch (error: Exception) { failure = error; done.countDown(); webSocket.cancel() }
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (Regex("(?:^|\r\n)Path:turn.end(?:\r\n|$)").containsMatchIn(text)) { ended = true; done.countDown() }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                response?.let(::updateClock)
                failure = EdgeTurnFailure(response?.code ?: 0, t); done.countDown()
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { done.countDown() }
        }
        val socket = client.newWebSocket(headers(Request.Builder().url(url)).build(), listener)
        try {
            if (!done.await(35, TimeUnit.SECONDS)) throw Wenku8SpeechFailure("在线语音准备超时，请检查网络后重试")
            failure?.let { throw it }
            if (!ended) throw Wenku8SpeechFailure("在线音源未完成这一段，请重试", true)
            if (audio.size() < 100) throw Wenku8SpeechFailure("在线音源未生成这一段的语音", true)
            return CloudTtsAudio(audio.toByteArray(), "mp3")
        } finally { socket.cancel() }
    }

    private fun updateClock(response: Response) {
        response.header("Date")?.let { date -> runCatching {
            clockSkew = ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - System.currentTimeMillis()
        } }
    }
    private fun headers(builder: Request.Builder) = builder
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
        .header("Accept-Language", "en-US,en;q=0.9").header("Pragma", "no-cache").header("Cache-Control", "no-cache")
        .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold").header("Cookie", "muid=${connectionId().uppercase(Locale.US)};")

    private class EdgeTurnFailure(val code: Int, cause: Throwable) : IOException(cause)
    companion object {
        private const val route = "/consumer/speech/synthesize/readaloud"
        private fun connectionId() = UUID.randomUUID().toString().replace("-", "")
        internal fun binaryAudio(data: ByteArray): Pair<String, ByteArray> {
            require(data.size >= 2) { "在线语音返回了不完整的数据" }
            val length = ((data[0].toInt() and 255) shl 8) or (data[1].toInt() and 255)
            require(length > 0 && length + 2 <= data.size) { "在线语音数据头无效" }
            val header = data.copyOfRange(2, length + 2).toString(Charsets.UTF_8)
            val path = Regex("(?:^|\r\n)Path:([^\r\n]+)").find(header)?.groupValues?.get(1).orEmpty()
            return path to data.copyOfRange(length + 2, data.size)
        }
    }
}

/** Sanitizes only the synthesis copy and retains original UTF-16 offsets. */
internal fun wenku8SpeechText(text: String): String = buildString {
    text.codePoints().forEach { cp ->
        val xml = cp == 9 || cp == 10 || cp == 13 || cp in 0x20..0xd7ff || cp in 0xe000..0xfffd || cp in 0x10000..0x10ffff
        val invisible = cp in 0x7f..0x9f || cp in 0x200b..0x200f || cp in 0x202a..0x202e || cp in 0x2060..0x2069 || cp == 0xfeff || cp == 0xfffd || cp in 0xfdd0..0xfdef || cp and 0xffff >= 0xfffe
        if (!xml || invisible) append(if (cp > 0xffff) "  " else " ") else appendCodePoint(cp)
    }
}
