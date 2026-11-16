package vn.kana.recognizer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Bộ chấm điểm chữ viết tay Kana 100% offline dựa trên đối sánh hình học từng nét (Stroke-based Template Matching).
 * Thuần Kotlin/JVM, không phụ thuộc Android SDK.
 */
class KanaRecognizer(
    templates: List<KanaStrokeTemplate>,
    private val defaultConfig: RecognizerConfig = RecognizerConfig.forLevel(StrictnessLevel.STANDARD)
) {
    private data class PreparedTemplate(
        val template: KanaStrokeTemplate,
        val normalized: NormalizedCharacter
    )

    private val byChar: Map<String, PreparedTemplate>
    private val byScriptAndCategory: Map<Pair<String, String>, List<PreparedTemplate>>
    private val byScript: Map<String, List<PreparedTemplate>>

    init {
        val prepared = templates.map { tmpl ->
            PreparedTemplate(
                template = tmpl,
                normalized = StrokePreprocessor.normalizeCharacter(
                    rawStrokes = tmpl.toTypedStrokes(),
                    resamplePoints = defaultConfig.resamplePointsCount,
                    applySmoothing = false
                )
            )
        }
        byChar = prepared.associateBy { it.template.char }
        byScriptAndCategory = prepared.groupBy { it.template.script to it.template.category }
        byScript = prepared.groupBy { it.template.script }
    }

    /**
     * Chấm điểm các nét vẽ của người dùng so với chữ mục tiêu [targetChar].
     * Đồng thời so khớp với toàn bộ các chữ cùng bộ (Hiragana hoặc Katakana) để chống nhận nhầm.
     */
    fun recognize(
        userStrokes: List<Stroke>,
        targetChar: String,
        config: RecognizerConfig = defaultConfig
    ): RecognitionResult {
        val startNanos = System.nanoTime()
        val targetPrepared = byChar[targetChar]
            ?: return RecognitionResult(
                targetChar = targetChar,
                passed = false,
                score = 0,
                perStroke = emptyList(),
                errors = listOf(RecognitionError.WRONG_SHAPE),
                bestMatchChar = null
            )

        // Bước 1: Tiền xử lý - bỏ các nét chấm vô tình quá ngắn
        val validStrokes = StrokePreprocessor.filterShortStrokes(
            strokes = userStrokes,
            minLength = config.minStrokeLengthCanvasRatio
        )

        val expectedStrokeCount = targetPrepared.template.strokeCount

        // Tập chữ cùng bộ (ví dụ: 46 chữ Hiragana cơ bản hoặc 46 chữ Katakana cơ bản)
        val sameSetCandidates = byScriptAndCategory[
            targetPrepared.template.script to targetPrepared.template.category
        ] ?: byScript[targetPrepared.template.script].orEmpty()

        // Kiểm tra số nét
        if (validStrokes.size != expectedStrokeCount) {
            val bestOther = if (validStrokes.isNotEmpty()) {
                val userNorm = StrokePreprocessor.normalizeCharacter(
                    rawStrokes = validStrokes,
                    resamplePoints = config.resamplePointsCount,
                    applySmoothing = true
                )
                findBestMatchingCandidates(
                    userNorm = userNorm,
                    candidates = sameSetCandidates.filter { it.template.strokeCount == validStrokes.size },
                    config = config
                ).firstOrNull()
            } else {
                null
            }
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L
            return RecognitionResult(
                targetChar = targetChar,
                passed = false,
                score = 0,
                perStroke = emptyList(),
                errors = listOf(RecognitionError.WRONG_STROKE_COUNT),
                bestMatchChar = bestOther?.first,
                bestMatchScore = bestOther?.second ?: 0,
                elapsedTimeMs = elapsedMs
            )
        }

        // Bước 2: Chuẩn hoá toàn chữ theo bounding box (giữ tỉ lệ)
        val userNorm = StrokePreprocessor.normalizeCharacter(
            rawStrokes = validStrokes,
            resamplePoints = config.resamplePointsCount,
            applySmoothing = true
        )

        // Kiểm tra chữ viết không quá nhỏ trên khung vẽ
        if (userNorm.originalSpan < config.minCharacterBoundingBoxSpan) {
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L
            return RecognitionResult(
                targetChar = targetChar,
                passed = false,
                score = 0,
                perStroke = emptyList(),
                errors = listOf(RecognitionError.WRONG_SHAPE),
                bestMatchChar = null,
                elapsedTimeMs = elapsedMs
            )
        }

        // Bước 3: Đánh giá chi tiết từng nét với chữ mục tiêu
        val targetEval = evaluateAgainstTemplate(userNorm, targetPrepared.normalized, config)

        // Bước 4: Chống chấp nhận nhầm - chấm với TOÀN BỘ chữ cùng bộ có cùng số nét
        val sameStrokeCandidates = sameSetCandidates.filter {
            it.template.strokeCount == expectedStrokeCount
        }
        val ranked = findBestMatchingCandidates(userNorm, sameStrokeCandidates, config)
        val bestMatch = ranked.firstOrNull()
        val secondBest = ranked.getOrNull(1)

        val bestMatchChar = bestMatch?.first ?: targetChar
        val bestMatchScore = bestMatch?.second ?: targetEval.totalScore

        // Kiểm tra điều kiện đậu:
        // 1. Không có lỗi nghiêm trọng (WRONG_STROKE_COUNT, WRONG_ORDER, WRONG_DIRECTION, WRONG_SHAPE, WRONG_POSITION)
        // 2. Điểm tổng >= ngưỡng passScoreThreshold và mọi nét >= minPerStrokeScore
        // 3. Chữ mục tiêu phải là bestMatchChar
        val allStrokesPassMin = targetEval.perStroke.all { it.score >= config.minPerStrokeScore }
        val isBestMatch = bestMatchChar == targetChar
        val passed = targetEval.errors.isEmpty() &&
            targetEval.totalScore >= config.passScoreThreshold &&
            allStrokesPassMin &&
            isBestMatch

        val finalErrors = if (!passed && targetEval.errors.isEmpty()) {
            // Nếu hình dáng hơi lệch khiến sang chữ khác hoặc chưa đủ ngưỡng điểm thì gắn WRONG_SHAPE
            listOf(RecognitionError.WRONG_SHAPE)
        } else {
            targetEval.errors
        }

        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L
        return RecognitionResult(
            targetChar = targetChar,
            passed = passed,
            score = targetEval.totalScore,
            perStroke = targetEval.perStroke,
            errors = finalErrors,
            bestMatchChar = bestMatchChar,
            bestMatchScore = bestMatchScore,
            secondBestChar = secondBest?.first,
            secondBestScore = secondBest?.second ?: 0,
            elapsedTimeMs = elapsedMs
        )
    }

    private data class TemplateEvaluationSummary(
        val totalScore: Int,
        val rawDissimilarity: Float,
        val perStroke: List<StrokeEvaluation>,
        val errors: List<RecognitionError>
    )

    private fun findBestMatchingCandidates(
        userNorm: NormalizedCharacter,
        candidates: List<PreparedTemplate>,
        config: RecognizerConfig
    ): List<Pair<String, Int>> {
        if (candidates.isEmpty()) return emptyList()
        return candidates
            .map { cand ->
                val summary = evaluateAgainstTemplate(userNorm, cand.normalized, config)
                cand.template.char to summary
            }
            .sortedWith(
                compareByDescending<Pair<String, TemplateEvaluationSummary>> { it.second.totalScore }
                    .thenBy { it.second.rawDissimilarity }
            )
            .map { it.first to it.second.totalScore }
    }

    private fun evaluateAgainstTemplate(
        userNorm: NormalizedCharacter,
        tmplNorm: NormalizedCharacter,
        config: RecognizerConfig
    ): TemplateEvaluationSummary {
        val n = tmplNorm.strokes.size
        val strokeEvaluations = ArrayList<StrokeEvaluation>(n)
        val charErrors = LinkedHashSet<RecognitionError>()

        // Tính ma trận khoảng cách chéo giữa nét người dùng i và nét mẫu j để bắt lỗi WRONG_ORDER
        val crossDistanceMatrix = Array(n) { FloatArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                val uPts = userNorm.strokes[i].points
                val tPts = tmplNorm.strokes[j].points
                val fwdDist = meanPointDistance(uPts, tPts)
                val revDist = meanPointDistance(uPts.asReversed(), tPts)
                crossDistanceMatrix[i][j] = min(fwdDist, revDist * 1.1f)
            }
        }

        var weightedDissimilaritySum = 0f
        var weightSum = 0f

        for (i in 0 until n) {
            val uStroke = userNorm.strokes[i]
            val tStroke = tmplNorm.strokes[i]

            val avgDist = meanPointDistance(uStroke.points, tStroke.points)
            val revAvgDist = meanPointDistance(uStroke.points.asReversed(), tStroke.points)
            val centeredAvgDist = meanPointDistance(uStroke.centeredPoints, tStroke.centeredPoints)
            val tailCenteredDist = meanSubrangeDistance(
                uStroke.centeredPoints,
                tStroke.centeredPoints,
                startIndex = (uStroke.centeredPoints.size * 2) / 3
            )
            val tailGlobalDist = meanSubrangeDistance(
                uStroke.points,
                tStroke.points,
                startIndex = (uStroke.points.size * 2) / 3
            )

            val frechetDist = DiscreteFrechet.distance(uStroke.points, tStroke.points)
            val centeredFrechet = DiscreteFrechet.distance(uStroke.centeredPoints, tStroke.centeredPoints)

            val tangentSim = meanTangentCosine(uStroke.tangents, tStroke.tangents)
            val tailTangentSim = meanTangentCosine(
                uStroke.tangents,
                tStroke.tangents,
                startIndex = (uStroke.tangents.size * 2) / 3
            )

            val chordAngleDiffDeg = computeDirectionAngleDeg(uStroke, tStroke)
            val tangentAngleDeg = (acos(tangentSim.coerceIn(-1f, 1f)) * (180.0 / PI)).toFloat()
            val effectiveDirectionDeg = if (tStroke.startToEndLen > 0.18f && uStroke.startToEndLen > 0.18f) {
                0.55f * chordAngleDiffDeg + 0.45f * tangentAngleDeg
            } else {
                tangentAngleDeg
            }

            val centroidOffset = if (n >= 2) {
                hypot(uStroke.centroid.x - tStroke.centroid.x, uStroke.centroid.y - tStroke.centroid.y)
            } else {
                0f
            }

            val lengthFractionDiff = abs(uStroke.lengthFraction - tStroke.lengthFraction)

            // Kiểm tra xem nét i có khớp với nét j != i hơn hẳn nét i không (WRONG_ORDER)
            var bestTemplateIdx = i
            var bestCrossDist = crossDistanceMatrix[i][i]
            if (n >= 2) {
                for (j in 0 until n) {
                    if (j != i && crossDistanceMatrix[i][j] < bestCrossDist) {
                        bestCrossDist = crossDistanceMatrix[i][j]
                        bestTemplateIdx = j
                    }
                }
            }

            val strokeErrors = ArrayList<RecognitionError>()
            val isOrderSwapped = n >= 2 &&
                bestTemplateIdx != i &&
                bestCrossDist < crossDistanceMatrix[i][i] * config.orderSwapDistanceRatio &&
                avgDist > config.maxPointToPointDistance * 0.72f

            if (isOrderSwapped) {
                strokeErrors.add(RecognitionError.WRONG_ORDER)
            } else {
                // Kiểm tra hướng nét (WRONG_DIRECTION)
                val isReversedBetter = revAvgDist < avgDist * 0.78f && avgDist > 0.11f
                val isDirectionBad = isReversedBetter ||
                    tangentSim < config.minTangentCosineSimilarity ||
                    effectiveDirectionDeg > config.maxDirectionAngleDeg

                if (isDirectionBad) {
                    strokeErrors.add(RecognitionError.WRONG_DIRECTION)
                }

                // Kiểm tra vị trí tương đối giữa các nét (WRONG_POSITION)
                if (n >= 2 && centroidOffset > config.maxStrokeCentroidOffset) {
                    strokeErrors.add(RecognitionError.WRONG_POSITION)
                }

                // Kiểm tra hình dáng nét (WRONG_SHAPE)
                val isShapeBad = avgDist > config.maxPointToPointDistance ||
                    frechetDist > config.maxFrechetDistance ||
                    centeredAvgDist > config.maxPointToPointDistance * 0.85f ||
                    (n >= 2 && lengthFractionDiff > 0.24f)

                if (isShapeBad && !strokeErrors.contains(RecognitionError.WRONG_DIRECTION)) {
                    strokeErrors.add(RecognitionError.WRONG_SHAPE)
                }
            }

            charErrors.addAll(strokeErrors)

            // Tính độ sai lệch tổng hợp của nét i
            val directionPenalty = ((1f - tangentSim) * 0.5f).coerceAtLeast(0f)
            val tailDirectionPenalty = ((1f - tailTangentSim) * 0.5f).coerceAtLeast(0f)

            var strokeDissimilarity =
                0.26f * avgDist +
                    0.20f * centeredAvgDist +
                    0.18f * tailCenteredDist +
                    0.12f * tailGlobalDist +
                    0.12f * min(frechetDist, centeredFrechet * 1.25f) +
                    0.26f * directionPenalty +
                    0.18f * tailDirectionPenalty +
                    0.35f * lengthFractionDiff +
                    0.20f * centroidOffset

            if (strokeErrors.contains(RecognitionError.WRONG_ORDER)) {
                strokeDissimilarity += 0.35f
            }
            if (strokeErrors.contains(RecognitionError.WRONG_DIRECTION)) {
                strokeDissimilarity += 0.30f
            }

            val strokeScore = (100f - strokeDissimilarity * config.scorePenaltyMultiplier)
                .roundToInt()
                .coerceIn(0, 100)

            val strokeWeight = max(0.25f, tStroke.lengthFraction)
            weightedDissimilaritySum += strokeDissimilarity * strokeWeight
            weightSum += strokeWeight

            strokeEvaluations.add(
                StrokeEvaluation(
                    strokeIndex = i,
                    score = strokeScore,
                    avgPointDistance = avgDist,
                    frechetDistance = frechetDist,
                    directionAngleDiffDeg = effectiveDirectionDeg,
                    tangentSimilarity = tangentSim,
                    centroidOffset = centroidOffset,
                    matchedTemplateStrokeIndex = if (isOrderSwapped) bestTemplateIdx else i,
                    errors = strokeErrors
                )
            )
        }

        // Độ lệch tỉ lệ khung hình toàn chữ (quan trọng cho các chữ 1 nét như く vs へ, し vs つ, ー vs ノ)
        val aspectDiff = abs(userNorm.widthToSpanRatio - tmplNorm.widthToSpanRatio) +
            abs(userNorm.heightToSpanRatio - tmplNorm.heightToSpanRatio)

        // Kiểm tra bố cục tương quan giữa các nét (inter-stroke relative vector) cho chữ >= 2 nét
        var interStrokeLayoutDiff = 0f
        if (n >= 2) {
            for (i in 0 until n - 1) {
                val uRelX = userNorm.strokes[i + 1].centroid.x - userNorm.strokes[i].centroid.x
                val uRelY = userNorm.strokes[i + 1].centroid.y - userNorm.strokes[i].centroid.y
                val tRelX = tmplNorm.strokes[i + 1].centroid.x - tmplNorm.strokes[i].centroid.x
                val tRelY = tmplNorm.strokes[i + 1].centroid.y - tmplNorm.strokes[i].centroid.y
                interStrokeLayoutDiff += hypot(uRelX - tRelX, uRelY - tRelY)

                // Vector nối điểm bắt đầu của nét i sang điểm bắt đầu nét i+1 (tách biệt rõ シ và ツ)
                val uStartRelX = userNorm.strokes[i + 1].points.first().x - userNorm.strokes[i].points.first().x
                val uStartRelY = userNorm.strokes[i + 1].points.first().y - userNorm.strokes[i].points.first().y
                val tStartRelX = tmplNorm.strokes[i + 1].points.first().x - tmplNorm.strokes[i].points.first().x
                val tStartRelY = tmplNorm.strokes[i + 1].points.first().y - tmplNorm.strokes[i].points.first().y
                interStrokeLayoutDiff += 0.6f * hypot(uStartRelX - tStartRelX, uStartRelY - tStartRelY)
            }
            interStrokeLayoutDiff /= (n - 1).toFloat()
        }

        val meanStrokeDissimilarity = if (weightSum > 0f) weightedDissimilaritySum / weightSum else 1f
        val totalDissimilarity = meanStrokeDissimilarity +
            0.12f * aspectDiff +
            0.16f * interStrokeLayoutDiff

        if (aspectDiff > config.maxAspectRatioDeviation * 2f) {
            charErrors.add(RecognitionError.WRONG_SHAPE)
        }

        val totalScore = (100f - totalDissimilarity * config.scorePenaltyMultiplier)
            .roundToInt()
            .coerceIn(0, 100)

        return TemplateEvaluationSummary(
            totalScore = totalScore,
            rawDissimilarity = totalDissimilarity,
            perStroke = strokeEvaluations,
            errors = charErrors.toList()
        )
    }

    private fun meanPointDistance(a: List<Point2D>, b: List<Point2D>): Float {
        val count = min(a.size, b.size)
        if (count == 0) return 1f
        var sum = 0f
        for (i in 0 until count) {
            sum += hypot(a[i].x - b[i].x, a[i].y - b[i].y)
        }
        return sum / count
    }

    private fun meanSubrangeDistance(a: List<Point2D>, b: List<Point2D>, startIndex: Int): Float {
        val count = min(a.size, b.size)
        val start = startIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
        val subCount = count - start
        if (subCount <= 0) return 0f
        var sum = 0f
        for (i in start until count) {
            sum += hypot(a[i].x - b[i].x, a[i].y - b[i].y)
        }
        return sum / subCount
    }

    private fun meanTangentCosine(
        a: List<Point2D>,
        b: List<Point2D>,
        startIndex: Int = 0
    ): Float {
        val count = min(a.size, b.size)
        val start = startIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
        val subCount = count - start
        if (subCount <= 0) return 1f
        var dotSum = 0f
        for (i in start until count) {
            dotSum += (a[i].x * b[i].x + a[i].y * b[i].y)
        }
        return (dotSum / subCount).coerceIn(-1f, 1f)
    }

    private fun computeDirectionAngleDeg(u: NormalizedStroke, t: NormalizedStroke): Float {
        if (u.startToEndLen < 1e-4f || t.startToEndLen < 1e-4f) return 0f
        val dot = (u.startToEndDx * t.startToEndDx + u.startToEndDy * t.startToEndDy) /
            (u.startToEndLen * t.startToEndLen)
        val clamped = dot.coerceIn(-1f, 1f)
        return (acos(clamped) * (180.0 / PI)).toFloat()
    }
}
