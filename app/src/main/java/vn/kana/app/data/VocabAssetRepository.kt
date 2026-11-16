package vn.kana.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import vn.kana.app.domain.KanaVocabDatabase
import vn.kana.app.domain.KanaVocabWord

/**
 * Kho dữ liệu từ vựng đọc 100% offline từ `assets/vocab/kana_vocab.json`.
 */
class VocabAssetRepository(
    private val appContext: Context
) {
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedWords: List<KanaVocabWord>? = null
    private var byIntroducesMap: Map<String, List<KanaVocabWord>> = emptyMap()

    suspend fun getAllWords(): List<KanaVocabWord> = mutex.withLock {
        cachedWords?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            val text = appContext.assets
                .open(ASSET_PATH)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            json.decodeFromString(KanaVocabDatabase.serializer(), text).words
        }
        cachedWords = loaded
        byIntroducesMap = loaded.groupBy { it.introduces }
        loaded
    }

    suspend fun getWordsForCharacter(char: String): List<KanaVocabWord> {
        if (cachedWords == null) {
            getAllWords()
        }
        return byIntroducesMap[char].orEmpty()
    }

    suspend fun getWordsForCharacters(chars: Collection<String>): List<KanaVocabWord> {
        val all = getAllWords()
        val charSet = chars.toSet()
        return all.filter { it.introduces in charSet }
    }

    companion object {
        const val ASSET_PATH = "vocab/kana_vocab.json"
    }
}
