package io.legado.app.help.readaloud.playback

import android.app.Application
import io.legado.app.domain.model.readaloud.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class DeepSeekCloudTtsProviderTest {
    private val engine = CloudTtsEngine("deepseek", "DeepSeek", CloudTtsProviderType.DeepSeek, apiKey = "test-only")
    private val wav = pcm16MonoToWav(ByteArray(100), 24000)

    @Test fun officialVoicesAndDedicatedPayloadDoNotUseOpenAiParameters() {
        var captured: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header("Content-Type", "audio/wav").body(wav.toResponseBody("audio/wav".toMediaType())).build()
        }.build()
        val provider = DeepSeekCloudTtsProvider { client }
        assertEquals(listOf("stella", "tide"), provider.fetchVoices(engine).map { it.id })
        val audio = provider.synthesize(engine, CloudTtsSynthesisRequest("你好", "tide", speed = 2f))
        assertEquals("https://api.deepseek.com/v1/tools/tts", captured!!.url.toString())
        assertEquals("Bearer test-only", captured!!.header("Authorization"))
        val buffer = Buffer(); captured!!.body!!.writeTo(buffer)
        val body = JSONObject(buffer.readUtf8())
        assertEquals(setOf("text", "voice_id", "format", "filter_markdown"), body.keys().asSequence().toSet())
        assertEquals("tide", body.getString("voice_id"))
        assertEquals("wav", body.getString("format"))
        assertArrayEquals(wav, audio.bytes)
    }

    @Test fun rejectsJsonAndTruncatedWaveAndDoesNotExposeErrorBodies() {
        fun provider(bytes: ByteArray, contentType: String, status: Int = 200) = DeepSeekCloudTtsProvider {
            OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("test")
                    .header("Content-Type", contentType).body(bytes.toResponseBody(contentType.toMediaType())).build()
            }.build()
        }
        val request = CloudTtsSynthesisRequest("正文", "stella")
        assertThrows(IllegalArgumentException::class.java) {
            provider("{}".toByteArray(), "application/json").synthesize(engine, request)
        }
        assertThrows(IllegalArgumentException::class.java) {
            provider(wav.copyOf(60), "audio/wav").synthesize(engine, request)
        }
        val error = assertThrows(IllegalStateException::class.java) {
            provider("test-only".toByteArray(), "application/json", 401).synthesize(engine, request)
        }
        assertTrue(error.message!!.contains("401"))
        assertFalse(error.message!!.contains(engine.apiKey))
    }

    @Test fun validatesVoiceAndBillingBeforeMakingNetworkRequests() {
        val provider = DeepSeekCloudTtsProvider { error("Network must not run") }
        assertThrows(IllegalArgumentException::class.java) {
            provider.synthesize(engine, CloudTtsSynthesisRequest("汉".repeat(5001), "stella"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            provider.synthesize(engine, CloudTtsSynthesisRequest("你好", "alloy"))
        }
    }
}
