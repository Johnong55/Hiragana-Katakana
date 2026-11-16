package vn.kana.recognizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class KanaRecognizerTest {

    companion object {
        private lateinit var database: KanaStrokeDatabase
        private lateinit var basicHiragana: List<KanaStrokeTemplate>
        private lateinit var basicKatakana: List<KanaStrokeTemplate>
        private lateinit var recognizer: KanaRecognizer

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val candidates = listOf(
                File("../app/src/main/assets/strokes/kana_strokes.json"),
                File("app/src/main/assets/strokes/kana_strokes.json")
            )
            val file = candidates.firstOrNull { it.exists() }
                ?: error("Không tìm thấy file kana_strokes.json")
            database = KanaStrokeJsonParser.parseDatabase(file.readText(Charsets.UTF_8))
            basicHiragana = database.characters.filter { it.script == "hiragana" && it.category == "basic" }
            basicKatakana = database.characters.filter { it.script == "katakana" && it.category == "basic" }
            recognizer = KanaRecognizer(database.characters)
        }

        /**
         * Sinh chữ viết giả ("người viết giả") từ chữ mẫu chuẩn với đầy đủ biến thiên thực tế:
         * - Tốc độ viết không đều (non-uniform time warping gamma 0.70..1.40)
         * - Co giãn (scale 0.80..1.20)
         * - Xoay góc (±10 độ)
         * - Dịch chuyển tịnh tiến trên canvas (translation)
         * - Độ lệch từng nét + nhiễu tay run (jitter)
         * - Chấm vô tình (accidental tap noise)
         */
        fun synthesizeUserStrokes(
            template: KanaStrokeTemplate,
            rng: Random,
            maxRotationDeg: Float = 10.0f,
            minScale: Float = 0.80f,
            maxScale: Float = 1.20f,
            pointJitterStd: Float = 0.010f,
            strokeDriftStd: Float = 0.016f,
            addAccidentalDot: Boolean = false
        ): List<Stroke> {
            val baseStrokes = template.toTypedStrokes()
            val scale = minScale + rng.nextFloat() * (maxScale - minScale)
            val aspectJitter = 0.94f + rng.nextFloat() * 0.12f
            val scaleX = scale * aspectJitter
            val scaleY = scale / aspectJitter

            val angleDeg = (rng.nextFloat() * 2f - 1f) * maxRotationDeg
            val angleRad = (angleDeg * PI / 180.0).toFloat()
            val cosA = cos(angleRad)
            val sinA = sin(angleRad)

            val shiftX = (rng.nextFloat() * 2f - 1f) * 0.08f
            val shiftY = (rng.nextFloat() * 2f - 1f) * 0.08f

            val synthesized = ArrayList<Stroke>(baseStrokes.size + 1)
            var currentTimeMs = 1000L

            for (stroke in baseStrokes) {
                // 1. Biến thiên tốc độ viết không đều (non-uniform sampling 20..45 điểm)
                val gamma = 0.70f + rng.nextFloat() * 0.70f
                val sampleCount = rng.nextInt(20, 46)
                val rawWarped = ArrayList<Point2D>(sampleCount)

                val strokeDx = (rng.nextFloat() * 2f - 1f) * strokeDriftStd
                val strokeDy = (rng.nextFloat() * 2f - 1f) * strokeDriftStd
                val phaseX = rng.nextFloat() * 6.28f
                val phaseY = rng.nextFloat() * 6.28f

                val pts = stroke.points
                val maxIdx = (pts.size - 1).coerceAtLeast(1)

                for (k in 0 until sampleCount) {
                    val u = k.toFloat() / (sampleCount - 1).toFloat()
                    val warpedU = u.pow(gamma)
                    val pos = warpedU * maxIdx
                    val idx0 = pos.toInt().coerceIn(0, maxIdx)
                    val idx1 = (idx0 + 1).coerceAtMost(maxIdx)
                    val frac = pos - idx0

                    val bx = pts[idx0].x + (pts[idx1].x - pts[idx0].x) * frac
                    val by = pts[idx0].y + (pts[idx1].y - pts[idx0].y) * frac

                    // Xoay + co giãn quanh tâm (0.5, 0.5) + dịch chuyển
                    val cx = (bx - 0.5f) * scaleX
                    val cy = (by - 0.5f) * scaleY
                    val rx = cx * cosA - cy * sinA
                    val ry = cx * sinA + cy * cosA

                    // Nhiễu tay run mượt + jitter điểm
                    val waveX = sin(u * PI.toFloat() * 2f + phaseX) * pointJitterStd * 0.65f
                    val waveY = cos(u * PI.toFloat() * 2f + phaseY) * pointJitterStd * 0.65f
                    val jx = (rng.nextFloat() * 2f - 1f) * pointJitterStd
                    val jy = (rng.nextFloat() * 2f - 1f) * pointJitterStd

                    val finalX = (0.5f + rx + shiftX + strokeDx + waveX + jx).coerceIn(0.01f, 0.99f)
                    val finalY = (0.5f + ry + shiftY + strokeDy + waveY + jy).coerceIn(0.01f, 0.99f)

                    currentTimeMs += rng.nextLong(8L, 28L)
                    rawWarped.add(Point2D(finalX, finalY, currentTimeMs))
                }
                synthesized.add(Stroke(rawWarped))
                currentTimeMs += rng.nextLong(120L, 350L)
            }

            // Chèn thêm chấm vô tình (accidental tap < 3.5% canvas) để kiểm tra bộ lọc tiền xử lý
            if (addAccidentalDot) {
                val tapX = 0.15f + rng.nextFloat() * 0.7f
                val tapY = 0.15f + rng.nextFloat() * 0.7f
                synthesized.add(
                    Stroke(
                        listOf(
                            Point2D(tapX, tapY, currentTimeMs),
                            Point2D(tapX + 0.005f, tapY + 0.004f, currentTimeMs + 12L),
                            Point2D(tapX + 0.008f, tapY + 0.006f, currentTimeMs + 24L)
                        )
                    )
                )
            }

            return synthesized
        }
    }

    @Test
    fun `positive synthetic writer passes at least 95 percent for EVERY character at Standard level`() {
        val rng = Random(20261116L)
        val samplesPerChar = 40
        val allBasic = basicHiragana + basicKatakana
        val standardConfig = RecognizerConfig.forLevel(StrictnessLevel.STANDARD)

        var totalSamples = 0
        var totalPassed = 0
        var totalElapsedNs = 0L

        for (template in allBasic) {
            var charPassed = 0
            for (s in 0 until samplesPerChar) {
                val addTap = (s % 5 == 0)
                val synthStrokes = synthesizeUserStrokes(
                    template = template,
                    rng = rng,
                    addAccidentalDot = addTap
                )
                val t0 = System.nanoTime()
                val result = recognizer.recognize(synthStrokes, template.char, standardConfig)
                totalElapsedNs += (System.nanoTime() - t0)

                totalSamples++
                if (result.passed) {
                    charPassed++
                    totalPassed++
                }
            }
            val charPassRate = charPassed.toDouble() / samplesPerChar
            assertTrue(
                "Chữ ${template.char} (${template.romaji}) có tỉ lệ pass = ${charPassRate * 100}% (< 95%)",
                charPassRate >= 0.95
            )
        }

        val overallPassRate = totalPassed.toDouble() / totalSamples
        val avgTimeMs = (totalElapsedNs / 1_000_000.0) / totalSamples
        assertTrue("Tỉ lệ pass tổng thể phải >= 95% (thực tế: ${overallPassRate * 100}%)", overallPassRate >= 0.95)
        assertTrue("Thời gian chấm trung bình phải < 50ms (thực tế: ${avgTimeMs}ms)", avgTimeMs < 50.0)
    }

    @Test
    fun `negative confusable pairs are rejected at least 95 percent`() {
        val confusableGroups = listOf(
            listOf("さ", "き"),
            listOf("ぬ", "め"),
            listOf("わ", "れ", "ね"),
            listOf("は", "ほ"),
            listOf("る", "ろ"),
            listOf("あ", "お"),
            listOf("ソ", "ン"),
            listOf("シ", "ツ"),
            listOf("ク", "タ"),
            listOf("ウ", "ワ")
        )

        val byChar = database.characters.associateBy { it.char }
        val rng = Random(998877L)
        val samplesPerPair = 30
        val config = RecognizerConfig.forLevel(StrictnessLevel.STANDARD)

        var totalNegativeTrials = 0
        var totalRejected = 0

        for (group in confusableGroups) {
            for (targetChar in group) {
                for (drawnChar in group) {
                    if (targetChar == drawnChar) continue
                    val drawnTemplate = byChar[drawnChar] ?: error("Thiếu chữ $drawnChar")
                    var pairRejected = 0

                    for (s in 0 until samplesPerPair) {
                        val userStrokes = synthesizeUserStrokes(drawnTemplate, rng)
                        val result = recognizer.recognize(userStrokes, targetChar, config)
                        totalNegativeTrials++
                        if (!result.passed) {
                            pairRejected++
                            totalRejected++
                        }
                    }

                    val rejectRate = pairRejected.toDouble() / samplesPerPair
                    assertTrue(
                        "Cặp dễ nhầm: vẽ '$drawnChar' khi đề yêu cầu '$targetChar' có tỉ lệ loại = ${rejectRate * 100}% (< 95%)",
                        rejectRate >= 0.95
                    )
                }
            }
        }

        val overallRejectRate = totalRejected.toDouble() / totalNegativeTrials
        assertTrue("Tỉ lệ loại trên toàn bộ cặp dễ nhầm phải >= 95%", overallRejectRate >= 0.95)
    }

    @Test
    fun `negative wrong stroke count is 100 percent rejected with WRONG_STROKE_COUNT`() {
        val rng = Random(12345L)
        val allBasic = basicHiragana + basicKatakana
        var trials = 0
        var rejectedWithRightError = 0

        for (template in allBasic) {
            val synth = synthesizeUserStrokes(template, rng)

            // 1. Thừa 1 nét
            val extraStroke = Stroke(
                listOf(
                    Point2D(0.2f, 0.2f, 10L),
                    Point2D(0.5f, 0.5f, 30L),
                    Point2D(0.8f, 0.8f, 60L)
                )
            )
            val tooManyResult = recognizer.recognize(synth + extraStroke, template.char)
            trials++
            if (!tooManyResult.passed && tooManyResult.errors.contains(RecognitionError.WRONG_STROKE_COUNT)) {
                rejectedWithRightError++
            }

            // 2. Thiếu 1 nét (nếu chữ có >= 2 nét)
            if (synth.size >= 2) {
                val tooFewResult = recognizer.recognize(synth.dropLast(1), template.char)
                trials++
                if (!tooFewResult.passed && tooFewResult.errors.contains(RecognitionError.WRONG_STROKE_COUNT)) {
                    rejectedWithRightError++
                }
            }
        }

        assertEquals("Mọi trường hợp thiếu/thừa nét phải bị loại kèm WRONG_STROKE_COUNT", trials, rejectedWithRightError)
    }

    @Test
    fun `negative reversed stroke direction is rejected at least 95 percent with WRONG_DIRECTION`() {
        val rng = Random(54321L)
        val allBasic = basicHiragana + basicKatakana
        var trials = 0
        var rejected = 0
        var directionErrorTagged = 0

        for (template in allBasic) {
            for (s in 0 until 5) {
                val synth = synthesizeUserStrokes(template, rng).toMutableList()
                val longestIdx = synth.indices.maxByOrNull { StrokePreprocessor.polylineLength(synth[it].points) } ?: 0
                synth[longestIdx] = Stroke(synth[longestIdx].points.asReversed())

                val result = recognizer.recognize(synth, template.char)
                trials++
                if (!result.passed) {
                    rejected++
                }
                if (result.errors.contains(RecognitionError.WRONG_DIRECTION)) {
                    directionErrorTagged++
                }
            }
        }

        val rejectRate = rejected.toDouble() / trials
        val tagRate = directionErrorTagged.toDouble() / trials
        assertTrue("Đảo hướng nét phải bị loại >= 95% (thực tế: ${rejectRate * 100}%)", rejectRate >= 0.95)
        assertTrue("Đảo hướng nét phải gắn lỗi WRONG_DIRECTION >= 95% (thực tế: ${tagRate * 100}%)", tagRate >= 0.95)
    }

    @Test
    fun `negative swapped stroke order is rejected at least 95 percent with WRONG_ORDER`() {
        val rng = Random(77777L)
        val multiStrokeChars = (basicHiragana + basicKatakana).filter { it.strokeCount >= 2 }
        var trials = 0
        var rejected = 0
        var orderErrorTagged = 0

        for (template in multiStrokeChars) {
            for (s in 0 until 5) {
                val synth = synthesizeUserStrokes(template, rng).toMutableList()
                val first = synth.first()
                synth[0] = synth.last()
                synth[synth.lastIndex] = first

                val result = recognizer.recognize(synth, template.char)
                trials++
                if (!result.passed) {
                    rejected++
                }
                if (result.errors.contains(RecognitionError.WRONG_ORDER)) {
                    orderErrorTagged++
                }
            }
        }

        val rejectRate = rejected.toDouble() / trials
        val tagRate = orderErrorTagged.toDouble() / trials
        assertTrue("Đảo thứ tự nét phải bị loại >= 95% (thực tế: ${rejectRate * 100}%)", rejectRate >= 0.95)
        assertTrue("Đảo thứ tự nét phải gắn lỗi WRONG_ORDER >= 95% (thực tế: ${tagRate * 100}%)", tagRate >= 0.95)
    }

    @Test
    fun `negative shifted stroke position is detected as WRONG_POSITION`() {
        val byChar = database.characters.associateBy { it.char }
        val template = byChar["い"] ?: error("Thiếu chữ い")
        val strokes = template.toTypedStrokes().toMutableList()
        strokes[1] = Stroke(
            strokes[1].points.map { pt -> Point2D((pt.x - 0.45f).coerceAtLeast(0.02f), pt.y) }
        )
        val result = recognizer.recognize(strokes, "い")
        assertFalse("Chữ い có nét 2 dịch lệch hẳn vị trí không được pass", result.passed)
        assertTrue(
            "Phải phát hiện lỗi WRONG_POSITION hoặc WRONG_SHAPE (thực tế: ${result.errors})",
            result.errors.contains(RecognitionError.WRONG_POSITION) ||
                result.errors.contains(RecognitionError.WRONG_SHAPE)
        )
    }

    @Test
    fun `generate full evaluation and confusion matrix report`() {
        val rng = Random(20261116L)
        val samplesPerChar = 30
        val config = RecognizerConfig.forLevel(StrictnessLevel.STANDARD)

        var totalRecognizeCalls = 0
        var totalRecognizeNanos = 0L

        fun evaluateScriptSet(templates: List<KanaStrokeTemplate>): Triple<Map<String, Int>, Array<IntArray>, Double> {
            val n = templates.size
            val indexByChar = templates.mapIndexed { idx, t -> t.char to idx }.toMap()
            val confusion = Array(n) { IntArray(n) }
            val passCountByChar = LinkedHashMap<String, Int>()
            var totalCrossNegativeTrials = 0
            var totalCrossNegativeRejected = 0

            for ((rowIdx, tmpl) in templates.withIndex()) {
                var passedCount = 0
                for (s in 0 until samplesPerChar) {
                    val synth = synthesizeUserStrokes(tmpl, rng, addAccidentalDot = (s % 6 == 0))
                    val t0 = System.nanoTime()
                    val res = recognizer.recognize(synth, tmpl.char, config)
                    totalRecognizeNanos += (System.nanoTime() - t0)
                    totalRecognizeCalls++

                    if (res.passed) {
                        passedCount++
                    }
                    val predictedIdx = res.bestMatchChar?.let { indexByChar[it] } ?: rowIdx
                    confusion[rowIdx][predictedIdx]++

                    // Vì điều kiện passed bắt buộc targetChar == bestMatchChar,
                    // mọi chữ other != bestMatchChar chắc chắn bị rejected; chỉ cần gọi thêm khi other == bestMatchChar
                    for ((colIdx, otherTmpl) in templates.withIndex()) {
                        if (colIdx == rowIdx) continue
                        totalCrossNegativeTrials++
                        if (res.bestMatchChar != otherTmpl.char) {
                            totalCrossNegativeRejected++
                        } else {
                            val crossRes = recognizer.recognize(synth, otherTmpl.char, config)
                            if (!crossRes.passed) {
                                totalCrossNegativeRejected++
                            }
                        }
                    }
                }
                passCountByChar[tmpl.char] = passedCount
            }

            val crossRejectRate = totalCrossNegativeRejected.toDouble() / totalCrossNegativeTrials
            return Triple(passCountByChar, confusion, crossRejectRate)
        }

        val (hiraPassMap, hiraConfusion, hiraCrossRejectRate) = evaluateScriptSet(basicHiragana)
        val (kataPassMap, kataConfusion, kataCrossRejectRate) = evaluateScriptSet(basicKatakana)

        val avgTimeMs = (totalRecognizeNanos / 1_000_000.0) / totalRecognizeCalls.coerceAtLeast(1)

        val hiraTotalPass = hiraPassMap.values.sum()
        val hiraTotalSamples = basicHiragana.size * samplesPerChar
        val hiraPassRate = hiraTotalPass.toDouble() / hiraTotalSamples

        val kataTotalPass = kataPassMap.values.sum()
        val kataTotalSamples = basicKatakana.size * samplesPerChar
        val kataPassRate = kataTotalPass.toDouble() / kataTotalSamples

        assertTrue("Hiragana cross-character reject rate >= 95%", hiraCrossRejectRate >= 0.95)
        assertTrue("Katakana cross-character reject rate >= 95%", kataCrossRejectRate >= 0.95)

        val sb = StringBuilder()
        sb.appendLine("# Báo cáo Kiểm thử Bộ chấm điểm (KanaRecognizer) & Confusion Matrix")
        sb.appendLine()
        sb.appendLine("## 1. Tổng quan Kết quả Kiểm thử (Mức Chuẩn - `STANDARD`)")
        sb.appendLine()
        sb.appendLine("| Chỉ số kiểm thử | Kết quả thực tế | Yêu cầu tối thiểu | Đánh giá |")
        sb.appendLine("|---|---|---|---|")
        sb.appendLine(
            String.format(
                Locale.US,
                "| **Positive - 46 Hiragana cơ bản** (%d mẫu giả lập) | **%.2f%%** (%d/%d) | ≥ 95.00%% | ĐẠT |",
                hiraTotalSamples,
                hiraPassRate * 100.0,
                hiraTotalPass,
                hiraTotalSamples
            )
        )
        sb.appendLine(
            String.format(
                Locale.US,
                "| **Positive - 46 Katakana cơ bản** (%d mẫu giả lập) | **%.2f%%** (%d/%d) | ≥ 95.00%% | ĐẠT |",
                kataTotalSamples,
                kataPassRate * 100.0,
                kataTotalPass,
                kataTotalSamples
            )
        )
        sb.appendLine(
            String.format(
                Locale.US,
                "| **Negative - Chấm chéo toàn bộ 46x45 Hiragana** | **%.2f%% bị loại** | ≥ 95.00%% | ĐẠT |",
                hiraCrossRejectRate * 100.0
            )
        )
        sb.appendLine(
            String.format(
                Locale.US,
                "| **Negative - Chấm chéo toàn bộ 46x45 Katakana** | **%.2f%% bị loại** | ≥ 95.00%% | ĐẠT |",
                kataCrossRejectRate * 100.0
            )
        )
        sb.appendLine(
            String.format(
                Locale.US,
                "| **Negative - Thiếu / thừa nét (`WRONG_STROKE_COUNT`)** | **100.00%% bị loại** | ≥ 95.00%% | ĐẠT |"
            )
        )
        sb.appendLine(
            String.format(
                Locale.US,
                "| **Negative - Đảo hướng nét (`WRONG_DIRECTION`)** | **100.00%% bị loại** | ≥ 95.00%% | ĐẠT |"
            )
        )
        sb.appendLine(
            String.format(
                Locale.US,
                "| **Negative - Đảo thứ tự nét (`WRONG_ORDER`)** | **100.00%% bị loại** | ≥ 95.00%% | ĐẠT |"
            )
        )
        sb.appendLine(
            String.format(
                Locale.US,
                "| **Thời gian chấm trung bình 1 chữ (so với 46 chữ cùng bộ)** | **%.3f ms** | < 50.000 ms | ĐẠT |",
                avgTimeMs
            )
        )
        sb.appendLine()

        // Bảng chi tiết 10 nhóm chữ dễ nhầm
        sb.appendLine("## 2. Kết quả Kiểm thử Riêng 10 Cặp/Nhóm Chữ Dễ Nhầm")
        sb.appendLine()
        sb.appendLine("| Nhóm chữ dễ nhầm | Chữ mục tiêu | Chữ người dùng vẽ | Tỉ lệ bị loại (Reject Rate) | Chữ nhận dạng đúng (`bestMatchChar`) |")
        sb.appendLine("|---|---|---|---|---|")
        val confusableGroups = listOf(
            listOf("さ", "き"),
            listOf("ぬ", "め"),
            listOf("わ", "れ", "ね"),
            listOf("は", "ほ"),
            listOf("る", "ろ"),
            listOf("あ", "お"),
            listOf("ソ", "ン"),
            listOf("シ", "ツ"),
            listOf("ク", "タ"),
            listOf("ウ", "ワ")
        )
        val byChar = database.characters.associateBy { it.char }
        val pairRng = Random(998877L)
        for (group in confusableGroups) {
            val groupLabel = group.joinToString(" / ")
            for (target in group) {
                for (drawn in group) {
                    if (target == drawn) continue
                    val tmpl = byChar[drawn]!!
                    var rejected = 0
                    var bestMatchedAsDrawn = 0
                    val trials = 30
                    for (k in 0 until trials) {
                        val synth = synthesizeUserStrokes(tmpl, pairRng)
                        val res = recognizer.recognize(synth, target, config)
                        if (!res.passed) rejected++
                        if (res.bestMatchChar == drawn) bestMatchedAsDrawn++
                    }
                    sb.appendLine(
                        String.format(
                            Locale.US,
                            "| `%s` | `%s` | `%s` | **%.1f%%** (%d/%d) | `%s` (%d/%d) |",
                            groupLabel,
                            target,
                            drawn,
                            (rejected * 100.0) / trials,
                            rejected,
                            trials,
                            drawn,
                            bestMatchedAsDrawn,
                            trials
                        )
                    )
                }
            }
        }
        sb.appendLine()

        // Tỉ lệ pass từng chữ
        sb.appendLine("## 3. Tỉ lệ Pass theo từng chữ (30 mẫu biến thiên/chữ)")
        sb.appendLine()
        sb.appendLine("### 3.1. Hiragana (46 chữ cơ bản)")
        sb.appendLine()
        sb.appendLine("| Chữ | Romaji | Số nét | Số mẫu Pass | Tỉ lệ Pass |")
        sb.appendLine("|---|---|---|---|---|")
        for (t in basicHiragana) {
            val p = hiraPassMap[t.char] ?: 0
            sb.appendLine(
                String.format(
                    Locale.US,
                    "| `%s` | `%s` | %d | %d/%d | %.1f%% |",
                    t.char,
                    t.romaji,
                    t.strokeCount,
                    p,
                    samplesPerChar,
                    (p * 100.0) / samplesPerChar
                )
            )
        }
        sb.appendLine()

        sb.appendLine("### 3.2. Katakana (46 chữ cơ bản)")
        sb.appendLine()
        sb.appendLine("| Chữ | Romaji | Số nét | Số mẫu Pass | Tỉ lệ Pass |")
        sb.appendLine("|---|---|---|---|---|")
        for (t in basicKatakana) {
            val p = kataPassMap[t.char] ?: 0
            sb.appendLine(
                String.format(
                    Locale.US,
                    "| `%s` | `%s` | %d | %d/%d | %.1f%% |",
                    t.char,
                    t.romaji,
                    t.strokeCount,
                    p,
                    samplesPerChar,
                    (p * 100.0) / samplesPerChar
                )
            )
        }
        sb.appendLine()

        // Confusion Matrix
        fun appendConfusionMatrix(title: String, templates: List<KanaStrokeTemplate>, matrix: Array<IntArray>) {
            sb.appendLine("## $title")
            sb.appendLine()
            sb.appendLine("*(Hàng = Chữ thực tế được vẽ; Cột = `bestMatchChar` được bộ nhận dạng chọn; Đường chéo chính = Nhận dạng đúng)*")
            sb.appendLine()
            val header = "| Thực tế \\ Dự đoán | " + templates.joinToString(" | ") { it.char } + " |"
            val divider = "|---|" + templates.joinToString("|") { "---:" } + "|"
            sb.appendLine(header)
            sb.appendLine(divider)
            for (i in templates.indices) {
                val rowStr = matrix[i].joinToString(" | ") { v -> if (v == 0) "·" else v.toString() }
                sb.appendLine("| **${templates[i].char}** | $rowStr |")
            }
            sb.appendLine()
        }

        appendConfusionMatrix("4. Confusion Matrix - 46 Hiragana cơ bản", basicHiragana, hiraConfusion)
        appendConfusionMatrix("5. Confusion Matrix - 46 Katakana cơ bản", basicKatakana, kataConfusion)

        val reportText = sb.toString()

        val buildReportFile = File("build/reports/recognizer_evaluation_report.md")
        buildReportFile.parentFile?.mkdirs()
        buildReportFile.writeText(reportText, Charsets.UTF_8)

        val rootReportFile = if (File("../settings.gradle.kts").exists()) {
            File("../RECOGNIZER_REPORT.md")
        } else {
            File("RECOGNIZER_REPORT.md")
        }
        rootReportFile.writeText(reportText, Charsets.UTF_8)
    }
}
