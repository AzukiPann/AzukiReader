package io.legado.app.data.repository

import android.content.Context
import android.net.Uri
import io.legado.app.domain.gateway.ReadingDataGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ReadingDataRepository(
    private val context: Context,
    private val records: ReadRecordRepository,
) : ReadingDataGateway {
    override suspend fun exportData(destination: String) = withContext(Dispatchers.IO) {
        val json = records.exportReadingData()
        require(json.toByteArray(Charsets.UTF_8).size <= ReadingDataArchive.MAX_BYTES) { "文件不能超过20MB" }
        val stream = context.contentResolver.openOutputStream(Uri.parse(destination), "wt")
            ?: throw IOException("无法打开导出文件")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }
    }

    override suspend fun importData(source: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(Uri.parse(source))
            ?: throw IOException("无法打开阅读数据")
        val bytes = stream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= ReadingDataArchive.MAX_BYTES) { "文件不能超过20MB" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        records.importReadingData(bytes.toString(Charsets.UTF_8))
    }
}
