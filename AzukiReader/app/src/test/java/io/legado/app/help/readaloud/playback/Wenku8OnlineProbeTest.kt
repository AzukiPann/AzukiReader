package io.legado.app.help.readaloud.playback

import android.app.Application
import io.legado.app.domain.model.readaloud.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class Wenku8OnlineProbeTest {
    @Test fun liveSynthesis() {
        val output = System.getProperty("wenku8.onlineProbeDir")
        assumeTrue("Explicit opt-in for external service", output != null)
        val provider = Wenku8EdgeTtsProvider()
        val engine = CloudTtsEngine("probe", "probe", CloudTtsProviderType.Wenku8Edge)
        val voices = provider.fetchVoices(engine)
        println("LIVE Chinese voices: ${voices.size}")
        assertTrue(voices.isNotEmpty())
        val audio = provider.synthesize(engine, CloudTtsSynthesisRequest("你好，这是朗读连接测试。", "zh-CN-XiaoxiaoNeural", "zh-CN"))
        assertTrue(audio.bytes.size > 100)
        File(output!!).mkdirs()
        File(output, "online-probe.mp3").writeBytes(audio.bytes)
        println("LIVE audio bytes: ${audio.bytes.size}")
        val legacyClient = wenku8SpeechHttpClient.newBuilder().addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().addHeader("Keep-Alive", "300")
                .addHeader("Connection", "Keep-Alive").build())
        }.build()
        val legacy = runCatching {
            Wenku8EdgeTtsProvider(legacyClient).synthesize(engine,
                CloudTtsSynthesisRequest("你好，这是朗读连接测试。", "zh-CN-XiaoxiaoNeural", "zh-CN"))
        }
        println("LIVE legacy headers result: " + legacy.fold({ "audio ${it.bytes.size} bytes" }, { it.message.orEmpty() }))
    }
}
