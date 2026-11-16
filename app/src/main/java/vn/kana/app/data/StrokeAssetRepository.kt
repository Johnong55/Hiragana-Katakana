package vn.kana.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import vn.kana.recognizer.KanaStrokeDatabase
import vn.kana.recognizer.KanaStrokeJsonParser
import vn.kana.recognizer.KanaStrokeTemplate

/**
 * Kho dữ liệu nét chữ KanjiVG đọc 100% offline từ `assets/strokes/kana_strokes.json`.
 */
class StrokeAssetRepository(
    private val appContext: Context
) {
    private val mutex = Mutex()
    private var cachedDatabase: KanaStrokeDatabase? = null
    private var byCharMap: Map<String, KanaStrokeTemplate> = emptyMap()

    suspend fun getDatabase(): KanaStrokeDatabase = mutex.withLock {
        cachedDatabase?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            val jsonText = appContext.assets
                .open(ASSET_PATH)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            KanaStrokeJsonParser.parseDatabase(jsonText)
        }
        cachedDatabase = loaded
        byCharMap = loaded.characters.associateBy { it.char }
        loaded
    }

    suspend fun getTemplate(char: String): KanaStrokeTemplate? {
        if (cachedDatabase == null) {
            getDatabase()
        }
        return byCharMap[char]
    }

    suspend fun getBasicHiragana(): List<KanaStrokeTemplate> {
        return getDatabase().characters.filter {
            it.script == "hiragana" && it.category == "basic"
        }
    }

    suspend fun getBasicKatakana(): List<KanaStrokeTemplate> {
        return getDatabase().characters.filter {
            it.script == "katakana" && it.category == "basic"
        }
    }

    suspend fun getAllCharacters(): List<KanaStrokeTemplate> {
        return getDatabase().characters
    }

    companion object {
        const val ASSET_PATH = "strokes/kana_strokes.json"
    }
}
