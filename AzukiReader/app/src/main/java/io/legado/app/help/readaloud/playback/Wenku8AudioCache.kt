package io.legado.app.help.readaloud.playback

import java.io.File

/** Soft cap: active chapter, recent writes and partial files are always retained. Downloads live elsewhere. */
internal fun trimWenku8AudioCache(directory: File, activePrefix: String, budget: Long = 96L * 1024 * 1024, now: Long = System.currentTimeMillis()) {
    val files = directory.listFiles()?.filter { it.isFile && it.extension == "mp3" } ?: return
    var total = files.sumOf { it.length() }
    for (file in files.sortedBy { it.lastModified() }) {
        if (total <= budget) break
        if (file.name.startsWith(activePrefix) || now - file.lastModified() < 60_000L) continue
        val size = file.length()
        if (file.delete()) {
            total -= size
            File(file.parentFile, file.nameWithoutExtension + ".txt").delete()
        }
    }
}
