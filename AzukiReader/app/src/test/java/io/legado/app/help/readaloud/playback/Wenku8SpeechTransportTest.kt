package io.legado.app.help.readaloud.playback

import io.legado.app.domain.model.readaloud.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test
import java.io.DataInputStream
import java.net.ServerSocket
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Wenku8SpeechTransportTest {
    @Test fun independentClientUpgradesAndCollectsAudio() = withEndpoint(false)
    @Test fun legacyKeepAliveInterceptorBreaksUpgrade() = withEndpoint(true)

    private fun withEndpoint(legacy: Boolean) {
        val executor = Executors.newSingleThreadExecutor()
        ServerSocket(0).use { server ->
            server.soTimeout = 10000
            val exchange = executor.submit<List<String>> {
                server.accept().use { socket ->
                    socket.soTimeout = 10000
                    val input = DataInputStream(socket.getInputStream())
                    val headers = mutableListOf<String>()
                    while (true) {
                        val line = buildString {
                            while (true) { val c = input.readUnsignedByte(); if (c == 10) break; if (c != 13) append(c.toChar()) }
                        }
                        if (line.isEmpty()) break
                        headers += line
                    }
                    val connections = headers.filter { it.startsWith("Connection:", true) }
                    val output = socket.getOutputStream()
                    if (connections.any { it.contains("Keep-Alive", true) }) {
                        output.write("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                        output.flush()
                    } else {
                        val key = headers.first { it.startsWith("Sec-WebSocket-Key:", true) }.substringAfter(':').trim()
                        val accept = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1")
                            .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray()))
                        output.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: $accept\r\n\r\n").toByteArray())
                        output.flush()
                        fun readText(): String {
                            assertEquals(0x81, input.readUnsignedByte())
                            val second = input.readUnsignedByte()
                            assertTrue(second and 128 != 0)
                            val length = when (val n = second and 127) { 126 -> input.readUnsignedShort(); 127 -> input.readLong().toInt(); else -> n }
                            val mask = ByteArray(4).also(input::readFully)
                            val payload = ByteArray(length).also(input::readFully)
                            return ByteArray(length) { (payload[it].toInt() xor mask[it % 4].toInt()).toByte() }.toString(Charsets.UTF_8)
                        }
                        assertTrue(readText().contains("Path:speech.config"))
                        assertTrue(readText().contains("Path:ssml"))
                        fun frame(opcode: Int, bytes: ByteArray) {
                            output.write(0x80 or opcode)
                            if (bytes.size < 126) output.write(bytes.size) else {
                                output.write(126); output.write(bytes.size ushr 8); output.write(bytes.size and 255)
                            }
                            output.write(bytes)
                        }
                        val audioHeader = "Path:audio\r\n".toByteArray()
                        frame(2, byteArrayOf(0, audioHeader.size.toByte()) + audioHeader + ByteArray(128) { 42 })
                        frame(1, "Path:turn.end\r\n\r\n".toByteArray())
                        output.flush()
                        // Let the client finish consuming before closing the transport.
                        runCatching { input.readByte() }
                    }
                    connections
                }
            }
            try {
                val client = if (legacy) wenku8SpeechHttpClient.newBuilder().addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().addHeader("Connection", "Keep-Alive").build())
                }.build() else wenku8SpeechHttpClient
                val provider = Wenku8EdgeTtsProvider(client, "http://127.0.0.1:${server.localPort}/".toHttpUrl())
                val engine = CloudTtsEngine("test", "test", CloudTtsProviderType.Wenku8Edge)
                val request = CloudTtsSynthesisRequest("你好", "zh-CN-XiaoxiaoNeural", "zh-CN")
                if (legacy) {
                    val failure = runCatching { provider.synthesize(engine, request) }.exceptionOrNull()
                    assertTrue(failure is Wenku8TransportFailure)
                    assertTrue(failure!!.message.orEmpty().contains("HTTP 400"))
                } else {
                    val audio = provider.synthesize(engine, request)
                    assertEquals("mp3", audio.format)
                    assertArrayEquals(ByteArray(128) { 42 }, audio.bytes)
                }
                val headers = exchange.get(12, TimeUnit.SECONDS)
                if (legacy) assertEquals(2, headers.size) else assertEquals(listOf("Connection: Upgrade"), headers)
            } finally { executor.shutdownNow() }
        }
    }
}
