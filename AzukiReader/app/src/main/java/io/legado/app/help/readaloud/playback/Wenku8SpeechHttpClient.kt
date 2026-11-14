package io.legado.app.help.readaloud.playback

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Book-source interceptors append Connection: Keep-Alive and corrupt WebSocket upgrades.
 * Speech has independent TLS, headers, timeouts and no source cache/cookie/rule interceptors.
 */
internal val wenku8SpeechHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()
}

internal class Wenku8TransportFailure(message: String, cause: Throwable? = null) : java.io.IOException(message, cause)
