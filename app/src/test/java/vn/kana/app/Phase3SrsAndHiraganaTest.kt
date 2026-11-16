package vn.kana.app

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.kana.app.data.local.CharacterProgressEntity
import vn.kana.app.domain.KanaVocabDatabase
import vn.kana.app.domain.MasteryLevel
import vn.kana.app.domain.SrsScheduler
import vn.kana.recognizer.KanaStrokeJsonParser
import java.io.File

class Phase3SrsAndHiraganaTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `all 46 basic hiragana across 10 gojuon rows have stroke templates and 92 vocabulary words`() {
        val strokeFile = listOf(
            File("src/main/assets/strokes/kana_strokes.json"),
            File("app/src/main/assets/strokes/kana_strokes.json")
        ).first { it.exists() }
        val vocabFile = listOf(
            File("src/main/assets/vocab/kana_vocab.json"),
            File("app/src/main/assets/vocab/kana_vocab.json")
        ).first { it.exists() }

        val strokeDb = KanaStrokeJsonParser.parseDatabase(strokeFile.readText(Charsets.UTF_8))
        val vocabDb = json.decodeFromString(KanaVocabDatabase.serializer(), vocabFile.readText(Charsets.UTF_8))

        val basicHiragana = strokeDb.characters.filter { it.script == "hiragana" && it.category == "basic" }
        assertEquals("Phải có đủ 46 chữ Hiragana cơ bản", 46, basicHiragana.size)

        val expectedRows = listOf("あ行", "か行", "さ行", "た行", "な行", "は行", "ま行", "や行", "ら行", "わ行")
        assertEquals(expectedRows, basicHiragana.map { it.row }.distinct())

        for (tmpl in basicHiragana) {
            val words = vocabDb.words.filter { it.script == "hiragana" && it.introduces == tmpl.char }
            assertEquals("Chữ ${tmpl.char} (${tmpl.row}) phải có đúng 2 từ vựng", 2, words.size)
        }
    }

    @Test
    fun `SRS scheduler converts recognizer score to SM-2 intervals and mastery levels properly`() {
        val now = 1_700_000_000_000L

        // Lần 1: Học xong chữ あ với điểm cao 92đ
        val card1 = SrsScheduler.computeNextCardState(
            existing = null,
            cardId = "CHAR_あ",
            cardType = "CHAR",
            targetKey = "あ",
            script = "hiragana",
            score = 92,
            passedFirstAttempt = true,
            nowEpochMs = now
        )
        assertEquals(1, card1.repetition)
        assertEquals(1, card1.intervalDays)
        assertEquals(0, card1.lapses)

        // Lần 2: Ôn tập đúng ngay lần đầu với điểm 90đ -> interval nhảy lên 4 ngày
        val card2 = SrsScheduler.computeNextCardState(
            existing = card1,
            cardId = "CHAR_あ",
            cardType = "CHAR",
            targetKey = "あ",
            script = "hiragana",
            score = 90,
            passedFirstAttempt = true,
            nowEpochMs = card1.dueAtEpochMs
        )
        assertEquals(2, card2.repetition)
        assertTrue("Interval lần 2 phải >= 3 ngày", card2.intervalDays >= 3)

        // Lần 3: Ôn tập đúng tiếp với điểm 94đ -> interval >= 10 ngày -> MASTERED
        val card3 = SrsScheduler.computeNextCardState(
            existing = card2,
            cardId = "CHAR_あ",
            cardType = "CHAR",
            targetKey = "あ",
            script = "hiragana",
            score = 94,
            passedFirstAttempt = true,
            nowEpochMs = card2.dueAtEpochMs
        )
        assertTrue("Interval lần 3 phải >= 10 ngày", card3.intervalDays >= 10)

        val prog = CharacterProgressEntity(
            char = "あ",
            script = "hiragana",
            rowName = "あ行",
            learned = true,
            lastScore = 94,
            practiceCount = 3,
            errorCount = 0,
            updatedAtEpochMs = now
        )
        assertEquals(MasteryLevel.MASTERED, SrsScheduler.resolveMasteryLevel(prog, card3))

        // Nếu trả lời sai ở lần tiếp theo -> reset repetition về 0, interval về 1 ngày, tăng lapses
        val cardFailed = SrsScheduler.computeNextCardState(
            existing = card3,
            cardId = "CHAR_あ",
            cardType = "CHAR",
            targetKey = "あ",
            script = "hiragana",
            score = 45,
            passedFirstAttempt = false,
            nowEpochMs = card3.dueAtEpochMs
        )
        assertEquals(0, cardFailed.repetition)
        assertEquals(1, cardFailed.intervalDays)
        assertEquals(1, cardFailed.lapses)
        assertEquals(MasteryLevel.LEARNING, SrsScheduler.resolveMasteryLevel(prog.copy(errorCount = 1), cardFailed))
    }
}
