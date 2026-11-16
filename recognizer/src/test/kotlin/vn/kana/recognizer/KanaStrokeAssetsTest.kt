package vn.kana.recognizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KanaStrokeAssetsTest {

    private fun loadDatabase(): KanaStrokeDatabase {
        val candidates = listOf(
            File("../app/src/main/assets/strokes/kana_strokes.json"),
            File("app/src/main/assets/strokes/kana_strokes.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Không tìm thấy file kana_strokes.json tại ${candidates.map { it.absolutePath }}")
        return KanaStrokeJsonParser.parseDatabase(file.readText(Charsets.UTF_8))
    }

    @Test
    fun `kana_strokes json contains all 46 basic hiragana and 46 basic katakana`() {
        val db = loadDatabase()
        val basicHiragana = db.characters.filter { it.script == "hiragana" && it.category == "basic" }
        val basicKatakana = db.characters.filter { it.script == "katakana" && it.category == "basic" }

        assertEquals("Phải có đủ 46 Hiragana cơ bản", 46, basicHiragana.size)
        assertEquals("Phải có đủ 46 Katakana cơ bản", 46, basicKatakana.size)
        assertEquals(db.totalCharacters, db.characters.size)
    }

    @Test
    fun `every stroke is normalized in 0 to 1 with 32 points`() {
        val db = loadDatabase()
        for (entry in db.characters) {
            assertTrue("Chữ ${entry.char} phải có ít nhất 1 nét", entry.strokeCount >= 1)
            assertEquals(
                "strokeCount của ${entry.char} phải khớp số nét thực tế",
                entry.strokeCount,
                entry.strokes.size
            )
            val typedStrokes = entry.toTypedStrokes()
            for ((index, stroke) in typedStrokes.withIndex()) {
                assertEquals(
                    "Nét ${index + 1} của chữ ${entry.char} phải có ${db.pointsPerStroke} điểm",
                    db.pointsPerStroke,
                    stroke.points.size
                )
                for (pt in stroke.points) {
                    assertTrue(
                        "Toạ độ x=${pt.x} của chữ ${entry.char} vượt ngoài [0, 1]",
                        pt.x in 0.0f..1.0f
                    )
                    assertTrue(
                        "Toạ độ y=${pt.y} của chữ ${entry.char} vượt ngoài [0, 1]",
                        pt.y in 0.0f..1.0f
                    )
                }
            }
        }
    }

    @Test
    fun `verify standard stroke counts for sample kana characters`() {
        val db = loadDatabase()
        val byChar = db.characters.associateBy { it.char }

        val expectedCounts = mapOf(
            "あ" to 3,
            "い" to 2,
            "う" to 2,
            "え" to 2,
            "お" to 3,
            "か" to 3,
            "き" to 4,
            "く" to 1,
            "け" to 3,
            "こ" to 2,
            "さ" to 3,
            "し" to 1,
            "は" to 3,
            "ほ" to 4,
            "ん" to 1,
            "が" to 5,
            "ぱ" to 4,
            "ア" to 2,
            "カ" to 2,
            "シ" to 3,
            "ツ" to 3,
            "ソ" to 2,
            "ン" to 2,
            "ー" to 1
        )

        for ((ch, expected) in expectedCounts) {
            val item = byChar[ch]
            assertNotNull("Thiếu chữ $ch trong database", item)
            assertEquals("Sai số nét của chữ $ch", expected, item!!.strokeCount)
        }
    }
}
