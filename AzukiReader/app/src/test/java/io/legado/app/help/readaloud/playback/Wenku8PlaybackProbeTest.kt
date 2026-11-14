package io.legado.app.help.readaloud.playback

import android.app.Application
import io.legado.app.domain.gateway.CloudTtsEngineGateway
import io.legado.app.domain.model.readaloud.*
import io.legado.app.utils.GSON
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import splitties.init.injectAsAppCtx
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[35])
class Wenku8PlaybackProbeTest {
    @Test fun synthesizeThroughActualVoicePath() = runBlocking {
        val path=System.getProperty("wenku8.onlineProbeDir")
        assumeTrue("External service opt-in",path!=null)
        RuntimeEnvironment.getApplication().injectAsAppCtx()
        val engine=CloudTtsEngine("wenku8-edge","自然在线",CloudTtsProviderType.Wenku8Edge)
        val gateway=object:CloudTtsEngineGateway {
            override fun observeAll()=flowOf(listOf(engine))
            override suspend fun getAll()=listOf(engine)
            override suspend fun get(id:String)=engine.takeIf {it.id==id}
            override suspend fun upsert(engine:CloudTtsEngine) {}
            override suspend fun delete(engine:CloudTtsEngine) {}
        }
        val voice=ReadAloudVoice("wenku8-edge:zh-CN-XiaoxiaoNeural",ReadAloudVoice.ENGINE_CLOUD,
            engine.id,"zh-CN-XiaoxiaoNeural","晓晓 · 中文女声",traitsJson=GSON.toJson(CloudTtsVoiceConfig(locale="zh-CN")))
        val synthesizer=CloudTtsAudioSynthesizer(gateway)
        val texts=listOf("你好，这是朗读连接测试。", "她推开窗，晨光落在书桌上。远处传来了脚步声，\"今天也要出发吗？\"他问。".repeat(5), "你好，这是默认音色路由测试。")
        texts.forEachIndexed { index,text ->
            val file=File(path!!,"voice-$index.mp3.part")
            val selected = if(index==2) voice.copy(id="runtime-default:cloud:wenku8-edge:zh-CN-XiaoxiaoNeural",traitsJson="{}") else voice
            assertTrue(synthesizer.synthesize(selected,text,file))
            assertTrue(file.length()>100)
            println("VOICE PATH index=$index chars=${text.length} bytes=${file.length()}")
        }
    }
}
