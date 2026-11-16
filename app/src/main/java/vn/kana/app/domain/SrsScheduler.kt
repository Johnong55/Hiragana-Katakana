package vn.kana.app.domain

import vn.kana.app.data.local.CharacterProgressEntity
import vn.kana.app.data.local.SrsCardEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 4 mức độ thuộc của một chữ Kana trên bảng Gojūon ở màn hình Home
 * (biểu diễn bằng 4 sắc độ xám phẳng, tuyệt đối không dùng gradient).
 */
enum class MasteryLevel(val labelVi: String) {
    UNLEARNED("Chưa học"),
    LEARNING("Đang học"),
    FAMILIAR("Quen thuộc"),
    MASTERED("Đã thuộc")
}

/**
 * Bộ lập lịch ôn tập ngắt quãng (Spaced Repetition - SM-2 tinh gọn) cho từng chữ và từng từ.
 * Thuần Kotlin để kiểm thử đơn vị (JUnit) trực tiếp trên JVM.
 */
object SrsScheduler {

    const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    const val TEN_MINUTES_MS = 10L * 60L * 1000L

    /**
     * Quy đổi điểm của `KanaRecognizer` (0..100) và việc có trả lời đúng ngay lần đầu hay không
     * thành mức chất lượng nhớ SM-2 `quality` (1..5).
     */
    fun scoreToQuality(score: Int, passedFirstAttempt: Boolean): Int {
        if (!passedFirstAttempt) return 1
        return when {
            score >= 90 -> 5
            score >= 78 -> 4
            score >= 62 -> 3
            else -> 2
        }
    }

    /**
     * Cập nhật thẻ SRS sau một lần trả lời trong bài học hoặc phiên Ôn tập.
     */
    fun computeNextCardState(
        existing: SrsCardEntity?,
        cardId: String,
        cardType: String,
        targetKey: String,
        script: String,
        score: Int,
        passedFirstAttempt: Boolean,
        nowEpochMs: Long
    ): SrsCardEntity {
        val quality = scoreToQuality(score, passedFirstAttempt)
        val prevRep = existing?.repetition ?: 0
        val prevInterval = existing?.intervalDays ?: 0
        val prevEf = existing?.easeFactor ?: 2.5f
        val prevLapses = existing?.lapses ?: 0

        if (quality < 3) {
            // Trả lời sai -> đưa về hộp đầu (interval = 1 ngày, đến hạn ôn lại sau 10 phút), tăng số lần sai (lapses)
            val newEf = max(1.3f, prevEf - 0.20f)
            return SrsCardEntity(
                cardId = cardId,
                cardType = cardType,
                targetKey = targetKey,
                script = script,
                repetition = 0,
                intervalDays = 1,
                easeFactor = newEf,
                dueAtEpochMs = nowEpochMs + TEN_MINUTES_MS,
                lastScore = score,
                lapses = prevLapses + 1
            )
        }

        val newRep = prevRep + 1
        val newIntervalDays = when (newRep) {
            1 -> 1
            2 -> if (quality == 5) 4 else 3
            else -> {
                val bonus = if (quality == 5) 1.15f else 1.0f
                max(prevInterval + 1, (prevInterval * prevEf * bonus).roundToInt())
            }
        }.coerceAtMost(180)

        // Công thức cập nhật hệ số dễ (Ease Factor) của SM-2
        val qDiff = 5 - quality
        val efDelta = 0.1f - qDiff * (0.08f + qDiff * 0.02f)
        val newEf = min(3.0f, max(1.3f, prevEf + efDelta))

        return SrsCardEntity(
            cardId = cardId,
            cardType = cardType,
            targetKey = targetKey,
            script = script,
            repetition = newRep,
            intervalDays = newIntervalDays,
            easeFactor = newEf,
            dueAtEpochMs = nowEpochMs + newIntervalDays * ONE_DAY_MS,
            lastScore = score,
            lapses = prevLapses
        )
    }

    /**
     * Xác định cấp độ thuộc (4 sắc độ xám) để hiển thị trên bảng Kana ở màn hình Home.
     */
    fun resolveMasteryLevel(
        progress: CharacterProgressEntity?,
        srsCard: SrsCardEntity?
    ): MasteryLevel {
        if (progress == null || !progress.learned) {
            return MasteryLevel.UNLEARNED
        }
        val interval = srsCard?.intervalDays ?: 1
        val score = srsCard?.lastScore ?: progress.lastScore
        return when {
            interval >= 10 && score >= 80 -> MasteryLevel.MASTERED
            interval >= 3 || (progress.practiceCount >= 3 && score >= 82 && progress.errorCount == 0) -> MasteryLevel.FAMILIAR
            else -> MasteryLevel.LEARNING
        }
    }
}
