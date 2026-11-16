package vn.kana.app

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.kana.app.domain.KanaVocabDatabase
import vn.kana.recognizer.KanaRecognizer
import vn.kana.recognizer.KanaStrokeJsonParser
import java.io.File

class Phase4KatakanaAndExtendedTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `all 46 katakana, 50 dakuten-handakuten, and 9 yoon-sokuon-choon have stroke templates and 2 vocab words each`() {
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

        val strokeByChar = strokeDb.characters.associateBy { it.char }

        // 1. 46 Katakana cơ bản
        val basicKatakana = strokeDb.characters.filter { it.script == "katakana" && it.category == "basic" }
        assertEquals(46, basicKatakana.size)
        for (tmpl in basicKatakana) {
            val words = vocabDb.words.filter { it.introduces == tmpl.char }
            assertEquals("Chữ Katakana ${tmpl.char} phải có đúng 2 từ vựng", 2, words.size)
        }

        // 2. 50 chữ Dakuten / Handakuten (25 Hiragana + 25 Katakana)
        val dakutenChars = strokeDb.characters.filter { it.category == "dakuten" }
        assertEquals(50, dakutenChars.size)
        for (tmpl in dakutenChars) {
            val words = vocabDb.words.filter { it.introduces == tmpl.char }
            assertEquals("Chữ Dakuten ${tmpl.char} phải có đúng 2 từ vựng", 2, words.size)
        }

        // 3. Yōon, Sokuon (っ/ッ) và Chōonpu (ー)
        val specialChars = listOf("ゃ", "ゅ", "ょ", "っ", "ャ", "ュ", "ョ", "ッ", "ー")
        for (ch in specialChars) {
            assertTrue("Phải có mẫu nét cho chữ $ch", strokeByChar.containsKey(ch))
            val words = vocabDb.words.filter { it.introduces == ch }
            assertEquals("Chữ đặc biệt $ch phải có đúng 2 từ vựng", 2, words.size)
        }

        // 4. Mọi ký tự trong mọi từ vựng (302 từ) đều có mẫu nét trong kana_strokes.json
        assertEquals(302, vocabDb.words.size)
        for (w in vocabDb.words) {
            assertEquals("Từ ${w.word} phải khớp với chars[]", w.word, w.chars.joinToString(""))
            assertTrue("Từ ${w.word} phải chứa chữ introduces '${w.introduces}'", w.chars.contains(w.introduces))
            for (c in w.chars) {
                assertTrue(
                    "Ký tự '$c' trong từ '${w.word}' phải tồn tại trong kana_strokes.json",
                    strokeByChar.containsKey(c)
                )
            }
        }
    }

    @Test
    fun `recognizer accurately grades dakuten, yoon, sokuon, and choonpu characters`() {
        val strokeFile = listOf(
            File("src/main/assets/strokes/kana_strokes.json"),
            File("app/src/main/assets/strokes/kana_strokes.json")
        ).first { it.exists() }
        val strokeDb = KanaStrokeJsonParser.parseDatabase(strokeFile.readText(Charsets.UTF_8))
        val recognizer = KanaRecognizer(strokeDb.characters)
        val byChar = strokeDb.characters.associateBy { it.char }

        val testTargets = listOf("が", "ぎ", "ぱ", "ガ", "パ", "ゃ", "ゅ", "ょ", "っ", "ャ", "ュ", "ョ", "ッ", "ー")
        for (ch in testTargets) {
            val tmpl = byChar[ch]!!
            val res = recognizer.recognize(tmpl.toTypedStrokes(), ch)
            assertTrue("Chữ $ch vẽ chuẩn phải được chấm passed (score=${res.score}, errors=${res.errors})", res.passed)
            assertEquals(ch, res.bestMatchChar)
        }
    }
}
