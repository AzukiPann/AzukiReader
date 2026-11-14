package io.legado.app.data.repository

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.legado.app.data.entities.readRecord.ReadRecord
import io.legado.app.data.entities.readRecord.ReadRecordDetail
import io.legado.app.data.entities.readRecord.ReadRecordSession
import java.time.LocalDate

/** Reading-only backup. Session words is the last chapter index, not a word count. */
internal data class ReadingDataArchive(
    val format: String = "Azuk1P4N.reading-data",
    val version: Int = 1,
    val timeZone: String = java.time.ZoneId.systemDefault().id,
    val records: List<ReadRecord> = emptyList(),
    val details: List<ReadRecordDetail> = emptyList(),
    val sessions: List<ReadRecordSession> = emptyList(),
) {
    // File keys are a wire contract, never reflected Kotlin property names. R8 may rename
    // or fold these properties without changing Debug/Release export compatibility.
    fun encode(): String = codec.toJson(JsonObject().apply {
        addProperty("format", format)
        addProperty("version", version)
        addProperty("timeZone", timeZone)
        add("records", codec.toJsonTree(records))
        add("details", codec.toJsonTree(details))
        add("sessions", codec.toJsonTree(sessions))
    })

    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        private val codec = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        fun decode(text: String): ReadingDataArchive {
            val parsed = JsonParser.parseString(text).asJsonObject
            // 1.00.02 Release accidentally exported its R8 field names and folded version=1.
            // Recognize only that exact known format; normal files still require all headers.
            val root = if (!parsed.has("format") && parsed.get("a")?.takeIf { it.isJsonPrimitive }
                    ?.asString == "Azuk1P4N.reading-data") {
                JsonObject().apply {
                    addProperty("format", "Azuk1P4N.reading-data")
                    addProperty("version", 1)
                    add("timeZone", parsed.get("b"))
                    add("records", parsed.get("c"))
                    add("details", parsed.get("d"))
                    add("sessions", parsed.get("e"))
                }
            } else parsed
            require(root.get("format")?.asString == "Azuk1P4N.reading-data") { "不是阅读数据文件" }
            require(root.get("version")?.asString in listOf("1", "2")) { "不支持的阅读数据版本" }
            java.time.ZoneId.of(root.get("timeZone")?.asString ?: error("缺少时区"))
            // Validate the entire document, including explicit nulls, before any database write.
            for (name in listOf("records", "details", "sessions")) {
                val rows = root.getAsJsonArray(name) ?: error("缺少$name")
                require(rows.size() <= 100_000) { "记录过多" }
                for (row in rows) {
                    val obj = row.asJsonObject
                    val strings = listOf("deviceId", "bookName", "bookAuthor") +
                        when (name) { "details" -> listOf("date"); "sessions" -> listOf("bookUrl"); else -> emptyList() }
                    strings.forEach { key ->
                        val value = obj.get(key)
                        require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) { "无效字段$key" }
                        require(value.asString.length <= 16_384) { "字段过长" }
                    }
                    require(obj.get("bookName").asString.isNotBlank()) { "书名不能为空" }
                    val numbers = when (name) {
                        "records" -> listOf("readTime", "lastRead")
                        "details" -> listOf("readTime", "readWords", "firstReadTime", "lastReadTime")
                        else -> listOf("startTime", "endTime", "words")
                    }
                    numbers.forEach { key ->
                        val value = obj.get(key)
                        require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isNumber &&
                            value.asString.toLongOrNull()?.let { it in 0..100_000_000_000_000L } == true) { "无效字段$key" }
                    }
                    if (name == "details") LocalDate.parse(obj.get("date").asString)
                    if (name != "sessions") require(obj.get("readTime").asLong <= 100L * 366 * 86_400_000) { "无效阅读时长" }
                    if (name == "sessions") {
                        require(obj.get("endTime").asLong - obj.get("startTime").asLong in 1..7L * 86_400_000) { "无效阅读时段" }
                        if (root.get("version").asInt == 2) {
                            LocalDate.parse(obj.get("recordDate")?.asString ?: error("缺少记录日期"))
                            val time = obj.get("readTime") ?: error("缺少累计时长")
                            require(time.isJsonPrimitive && time.asJsonPrimitive.isNumber &&
                                time.asString.toLongOrNull()?.let { it in 1..90_000_000L &&
                                    it <= obj.get("endTime").asLong - obj.get("startTime").asLong } == true) { "无效累计时长" }
                        }
                        require(obj.get("words").asLong <= Int.MAX_VALUE) { "无效章节序号" }
                        obj.get("chapterProgress")?.takeUnless { it.isJsonNull }?.let { fraction ->
                            require(fraction.isJsonPrimitive && fraction.asJsonPrimitive.isNumber &&
                                fraction.asDouble.let { it.isFinite() && it in 0.0..1.0 }
                            ) { "无效当前章内进度" }
                        }
                        val highest = obj.get("highestChapterIndex")?.takeUnless { it.isJsonNull }
                        val fraction = obj.get("highestChapterProgress")?.takeUnless { it.isJsonNull }
                        if (highest != null) {
                            require(highest.isJsonPrimitive && highest.asJsonPrimitive.isNumber &&
                                highest.asString.toLongOrNull()?.let { it in obj.get("words").asLong..Int.MAX_VALUE.toLong() } == true
                            ) { "无效最远章节" }
                        }
                        if (fraction != null) {
                            require(highest != null && fraction.isJsonPrimitive && fraction.asJsonPrimitive.isNumber &&
                                fraction.asDouble.let { it.isFinite() && it in 0.0..1.0 }
                            ) { "无效章内进度" }
                        }
                    }
                }
            }
            return ReadingDataArchive(
                format = root.get("format").asString,
                version = root.get("version").asInt,
                timeZone = root.get("timeZone").asString,
                records = root.getAsJsonArray("records").map { codec.fromJson(it, ReadRecord::class.java) },
                details = root.getAsJsonArray("details").map { codec.fromJson(it, ReadRecordDetail::class.java) },
                sessions = root.getAsJsonArray("sessions").map { codec.fromJson(it, ReadRecordSession::class.java) },
            )
        }
    }
}
