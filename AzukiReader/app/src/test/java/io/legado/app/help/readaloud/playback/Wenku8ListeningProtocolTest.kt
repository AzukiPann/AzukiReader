package io.legado.app.help.readaloud.playback

import io.legado.app.help.readaloud.wenku8.Wenku8EdgeProtocol
import io.legado.app.domain.model.readaloud.wenku8SpeechSegments
import org.junit.Assert.*
import org.junit.Test

class Wenku8ListeningProtocolTest {
    @Test fun referenceProtocolVectors() {
        val vectors = mapOf(
            0L to "52BC7A17B50DFEC9-28C4394775C8751CD2F6859C030C06DB1489ACAB3B3A7651831FC26895177ED6",
            1700000000L to "2CD23A2CA0B2595A-F3C846C370E05F9CC83ACC5F995C5F5D87A4D0F14CE8936A0F902E64099E38A2",
            1792000000L to "31DE969FFA38E06B-BD29DC903722D717D852F4F854F2386D98C8D3A93663E1A1AE89223A876AFDB4",
            1792000299L to "5F8DA6933397AD95-A8B5CB1FF5822A33E4E3946887E383C696D6B8965DDAC945260125764E667EA8",
        )
        vectors.forEach { (seconds, expected) -> assertEquals(expected, Wenku8EdgeProtocol.timeProof(seconds * 1000)) }
    }
    @Test fun binaryFramesSeparateHeadersFromAudio() {
        val header = "Content-Type:audio/mpeg\r\nPath:audio\r\n".toByteArray()
        val body = byteArrayOf(0, 3, 127, -1)
        val frame = byteArrayOf((header.size ushr 8).toByte(), header.size.toByte()) + header + body
        val parsed = Wenku8EdgeTtsProvider.binaryAudio(frame)
        assertEquals("audio", parsed.first)
        assertArrayEquals(body, parsed.second)
        assertThrows(IllegalArgumentException::class.java) { Wenku8EdgeTtsProvider.binaryAudio(byteArrayOf(0, 10, 1)) }
    }
    @Test fun sanitizationPreservesUtf16Offsets() {
        val text = "开头\u0000\u200B\uD83D\uDE00\uD800结尾"
        val clean = wenku8SpeechText(text)
        assertEquals(text.length, clean.length)
        assertEquals("开头  😀 结尾", clean)
    }
    @Test fun segmentsPreserveOriginalOffsetsAndSurrogates() {
        val content = "\n  第一段。\n[插图]\n" + "汉".repeat(239) + "😀" + "字".repeat(250) + "\n𠀀"
        val units = wenku8SpeechSegments(content)
        assertEquals(3, units.first().chapterPosition)
        assertFalse(units.any { it.text == "[插图]" })
        units.forEach {
            assertEquals(it.text, content.substring(it.chapterPosition, it.chapterPosition + it.text.length))
            assertFalse(it.text.first().isLowSurrogate())
            assertFalse(it.text.last().isHighSurrogate())
            assertTrue(it.text.length <= 244)
        }
        assertEquals("𠀀", units.last().text)
    }
    @Test fun punctuationOnlyIsNotSpoken() {
        assertTrue(wenku8SpeechSegments("。……！！\n[插图]\n\uFFFC").isEmpty())
    }
    @Test fun customConfigRejectsInsecureEndpointsAndHeaderInjection() {
        fun validate(url: String = "https://example.org/tts", secret: String = "key") =
            validateWenku8SsmlConfig(url,"zh-CN-XiaoxiaoNeural","zh-CN","测试","subscriptionKey",secret)
        validate()
        listOf("http://example.org/tts","https://user:pass@example.org/tts","https://example.org/tts#fragment").forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { validate(url) }
        }
        assertThrows(IllegalArgumentException::class.java) { validate(secret = "key\r\nInjected: value") }
        assertThrows(IllegalArgumentException::class.java) { validate(secret = "") }
    }
    @Test fun audioCacheProtectsActiveRecentAndPartialFiles() {
        val dir=java.nio.file.Files.createTempDirectory("wenku8-cache").toFile()
        try {
            fun write(name:String, time:Long)=java.io.File(dir,name).apply {writeBytes(ByteArray(100));setLastModified(time)}
            val old=write("old.mp3",1_000)
            val active=write("active_part.mp3",2_000)
            val recent=write("recent.mp3",999_000)
            val part=write("pending.part",1_000)
            trimWenku8AudioCache(dir,"active_",200,1_000_000)
            assertFalse(old.exists())
            assertTrue(active.exists());assertTrue(recent.exists());assertTrue(part.exists())
        } finally {dir.deleteRecursively()}
    }

}
