package vn.kana.recognizer

import kotlinx.serialization.json.Json

/**
 * Bộ đọc JSON dữ liệu nét chữ KanjiVG dùng chung cho cả app và JVM unit test.
 */
object KanaStrokeJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseDatabase(jsonString: String): KanaStrokeDatabase {
        return json.decodeFromString(KanaStrokeDatabase.serializer(), jsonString)
    }
}
