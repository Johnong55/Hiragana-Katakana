package vn.kana.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.kana.recognizer.RecognitionResult
import vn.kana.recognizer.Stroke
import java.io.File

@Serializable
data class SavedHandwritingSample(
    val id: String,
    val targetChar: String,
    val romaji: String,
    val strictness: String,
    val createdAtEpochMs: Long,
    val userStrokes: List<Stroke>,
    val recognitionResult: RecognitionResult?
)

@Serializable
data class HandwritingSampleCollection(
    val samples: List<SavedHandwritingSample> = emptyList()
)

/**
 * Lưu các mẫu chữ viết tay thật từ màn hình Debug vào file JSON cục bộ
 * (`filesDir/debug_samples/handwriting_samples.json`) phục vụ thu thập dữ liệu & tinh chỉnh ngưỡng.
 */
class DebugSampleStorage(
    private val appContext: Context
) {
    private val mutex = Mutex()
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val sampleFile: File
        get() = File(File(appContext.filesDir, "debug_samples"), "handwriting_samples.json")

    suspend fun getSavedSamples(): List<SavedHandwritingSample> = mutex.withLock {
        withContext(Dispatchers.IO) {
            readUnsafe().samples
        }
    }

    suspend fun saveSample(sample: SavedHandwritingSample): Int = mutex.withLock {
        withContext(Dispatchers.IO) {
            val file = sampleFile
            file.parentFile?.mkdirs()
            val existing = readUnsafe().samples
            val updated = existing + sample
            file.writeText(
                json.encodeToString(HandwritingSampleCollection(updated)),
                Charsets.UTF_8
            )
            updated.size
        }
    }

    private fun readUnsafe(): HandwritingSampleCollection {
        val file = sampleFile
        if (!file.exists()) return HandwritingSampleCollection()
        return try {
            json.decodeFromString(
                HandwritingSampleCollection.serializer(),
                file.readText(Charsets.UTF_8)
            )
        } catch (_: Exception) {
            HandwritingSampleCollection()
        }
    }
}
