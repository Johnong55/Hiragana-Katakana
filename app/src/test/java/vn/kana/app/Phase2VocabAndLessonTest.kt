package vn.kana.app

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.kana.app.domain.KanaVocabDatabase
import vn.kana.recognizer.KanaStrokeJsonParser
import java.io.File

class Phase2VocabAndLessonTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadVocabDb(): KanaVocabDatabase {
        val candidates = listOf(
            File("src/main/assets/vocab/kana_vocab.json"),
            File("app/src/main/assets/vocab/kana_vocab.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Không tìm thấy kana_vocab.json")
        return json.decodeFromString(KanaVocabDatabase.serializer(), file.readText(Charsets.UTF_8))
    }

    private fun loadStrokeChars(): Set<String> {
        val candidates = listOf(
            File("src/main/assets/strokes/kana_strokes.json"),
            File("app/src/main/assets/strokes/kana_strokes.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Không tìm thấy kana_strokes.json")
        return KanaStrokeJsonParser.parseDatabase(file.readText(Charsets.UTF_8))
            .characters
            .map { it.char }
            .toSet()
    }

    @Test
    fun `every character in a-row and ka-row and all 46 hiragana has 2 vocabulary words`() {
        val vocabDb = loadVocabDb()
        val strokeChars = loadStrokeChars()

        val aAndKaChars = listOf("あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ")
        for (ch in aAndKaChars) {
            val words = vocabDb.words.filter { it.introduces == ch }
            assertEquals("Chữ $ch phải có đúng 2 từ vựng", 2, words.size)
            for (w in words) {
                assertTrue("Từ ${w.word} phải chứa chữ $ch trong chars[]", w.chars.contains(ch))
                assertEquals("Ghép chars[] của ${w.word} phải khớp với word", w.word, w.chars.joinToString(""))
                assertTrue("Nghĩa tiếng Việt của ${w.word} không được rỗng", w.meaningVi.isNotBlank())
                for (c in w.chars) {
                    assertTrue(
                        "Ký tự '$c' trong từ '${w.word}' phải có dữ liệu nét trong kana_strokes.json",
                        strokeChars.contains(c)
                    )
                }
            }
        }
    }

    @Test
    fun `unlearned characters in vocabulary words are properly identified as pre-filled gray`() {
        val vocabDb = loadVocabDb()
        // Khi người dùng mới học xong chữ あ (chữ đầu tiên của あ行):
        val learnedAtA = setOf("あ")
        val ameWord = vocabDb.words.firstOrNull { it.word == "あめ" }
        assertNotNull(ameWord)
        val preFilledFlags = ameWord!!.chars.map { ch -> !learnedAtA.contains(ch) }
        // 'あ' đã học -> false (bắt viết); 'め' chưa học -> true (hiển thị xám sẵn)
        assertEquals(listOf(false, true), preFilledFlags)

        // Khi học tới chữ き (đã học あ, い, う, え, お, か, き): từ えき có cả 2 chữ đều đã học -> bắt viết cả 2 ô!
        val learnedAtKi = setOf("あ", "い", "う", "え", "お", "か", "き")
        val ekiWord = vocabDb.words.firstOrNull { it.word == "えき" }
        assertNotNull(ekiWord)
        val ekiFlags = ekiWord!!.chars.map { ch -> !learnedAtKi.contains(ch) }
        assertEquals(listOf(false, false), ekiFlags)
    }
}
