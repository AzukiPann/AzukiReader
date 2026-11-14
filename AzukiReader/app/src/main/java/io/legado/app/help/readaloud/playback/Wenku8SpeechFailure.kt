package io.legado.app.help.readaloud.playback

import java.io.IOException

internal class Wenku8SpeechFailure(message: String, val contentFailure: Boolean = false) : IOException(message)
