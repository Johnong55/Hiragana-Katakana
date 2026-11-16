package vn.kana.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kana.app.data.StrokeAssetRepository
import vn.kana.app.data.UserSettings
import vn.kana.app.data.UserSettingsRepository
import vn.kana.app.data.VocabAssetRepository
import vn.kana.app.data.local.CharacterProgressEntity
import vn.kana.app.data.local.KanaDao
import vn.kana.app.domain.AudioProvider
import vn.kana.app.domain.KanaVocabWord
import vn.kana.app.domain.QuizQuestion
import vn.kana.app.domain.QuizQuestionType
import vn.kana.app.domain.SrsScheduler
import vn.kana.app.ui.components.WordCharSlot
import vn.kana.recognizer.KanaRecognizer
import vn.kana.recognizer.KanaStrokeTemplate
import vn.kana.recognizer.RecognitionResult
import vn.kana.recognizer.RecognizerConfig
import vn.kana.recognizer.Stroke
import kotlin.math.roundToInt

data class ReviewUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val dueCardsCountAtStart: Int = 0,

    // Trạng thái vẽ chữ đơn
    val singleCharStrokes: List<Stroke> = emptyList(),
    val lastSingleEvaluation: RecognitionResult? = null,

    // Trạng thái vẽ từ nhiều chữ (原稿用紙)
    val wordSlots: List<WordCharSlot> = emptyList(),
    val activeSlotIndex: Int = 0,

    // Trạng thái sửa lỗi & viết lại 1 lần
    val showingCorrectionAnimation: Boolean = false,
    val correctionTemplate: KanaStrokeTemplate? = null,
    val mustRewriteAfterError: Boolean = false,
    val showFallbackRomajiInListenMode: Boolean = false,
    val feedbackMessage: String? = null,
    val isFeedbackError: Boolean = false,
    val shakeTrigger: Int = 0,
    val successOverlayText: String? = null,

    // Kết quả phiên ôn tập
    val firstAttemptCorrectCount: Int = 0,
    val totalScoreSum: Int = 0,
    val gradedItemsCount: Int = 0,
    val isSessionCompleted: Boolean = false
) {
    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)

    val isMultiBoxMode: Boolean
        get() = currentQuestion?.type == QuizQuestionType.MEANING_ROMAJI_TO_WORD ||
            currentQuestion?.type == QuizQuestionType.LISTEN_TO_WORD

    val averageScore: Int
        get() = if (gradedItemsCount > 0) (totalScoreSum.toFloat() / gradedItemsCount).roundToInt() else 0
}

class ReviewViewModel(
    private val strokeRepository: StrokeAssetRepository,
    private val vocabRepository: VocabAssetRepository,
    private val settingsRepository: UserSettingsRepository,
    private val kanaDao: KanaDao,
    private val audioProvider: AudioProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var recognizer: KanaRecognizer? = null
    private var templatesByChar: Map<String, KanaStrokeTemplate> = emptyMap()
    private val learnedCharsSet = LinkedHashSet<String>()
    private var autoAdvanceJob: Job? = null
    private var hadErrorOnCurrentItem = false

    init {
        observeSettings()
        buildNewReviewSession()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                _uiState.update { it.copy(settings = s) }
            }
        }
    }

    fun buildNewReviewSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSessionCompleted = false) }
            val allChars = strokeRepository.getAllCharacters()
            templatesByChar = allChars.associateBy { it.char }
            recognizer = KanaRecognizer(allChars)

            val allWords = vocabRepository.getAllWords()
            val settings = settingsRepository.settingsFlow.first()
            val now = System.currentTimeMillis()

            val learnedProgress = kanaDao.getLearnedCharacters()
            val srsCards = kanaDao.observeSrsCards().first()
            val srsById = srsCards.associateBy { it.cardId }
            val progressByChar = learnedProgress.associateBy { it.char }

            learnedCharsSet.clear()
            learnedProgress.forEach { learnedCharsSet.add(it.char) }

            // Nếu người dùng chưa học bài nào mà bấm vào Ôn tập, lấy mặc định hàng あ行 để luyện tập
            if (learnedCharsSet.isEmpty()) {
                allChars.filter { it.script == "hiragana" && it.row == "あ行" }
                    .forEach { learnedCharsSet.add(it.char) }
            }

            val dueCardsCount = srsCards.count { it.dueAtEpochMs <= now }

            // Sắp xếp các chữ đã học theo mức độ ưu tiên ôn tập:
            // 1. Thẻ đã đến hạn (dueAtEpochMs <= now)
            // 2. Chữ hay sai nhiều (lapses * 2 + errorCount giảm dần)
            // 3. Điểm lần cuối thấp (lastScore tăng dần)
            val candidateTemplates = learnedCharsSet
                .mapNotNull { templatesByChar[it] }
                .sortedWith(
                    compareByDescending<KanaStrokeTemplate> { tmpl ->
                        val card = srsById["CHAR_${tmpl.char}"]
                        card == null || card.dueAtEpochMs <= now
                    }.thenByDescending { tmpl ->
                        val card = srsById["CHAR_${tmpl.char}"]
                        val prog = progressByChar[tmpl.char]
                        (card?.lapses ?: 0) * 2 + (prog?.errorCount ?: 0)
                    }.thenBy { tmpl ->
                        val card = srsById["CHAR_${tmpl.char}"]
                        val prog = progressByChar[tmpl.char]
                        card?.lastScore ?: prog?.lastScore ?: 75
                    }
                )

            val selectedChars = candidateTemplates.take(6)
            val questions = ArrayList<QuizQuestion>()

            for ((idx, tmpl) in selectedChars.withIndex()) {
                val prog = progressByChar[tmpl.char]
                val card = srsById["CHAR_${tmpl.char}"]
                val isFrequentlyMissed = (prog?.errorCount ?: 0) > 0 ||
                    (card?.lapses ?: 0) > 0 ||
                    (prog?.lastScore ?: 80) < 75

                // 1. Câu hỏi chữ đơn (xen kẽ dạng a: nhìn Romaji và dạng b: nghe phát âm)
                val singleType = if (idx % 2 == 0) {
                    QuizQuestionType.ROMAJI_TO_KANA
                } else {
                    QuizQuestionType.LISTEN_TO_KANA
                }
                questions.add(QuizQuestion(type = singleType, targetTemplate = tmpl))

                // 2. Câu hỏi từ vựng chứa chữ đó (trộn chữ đơn + từ chứa chữ đó)
                val wordsContainingChar = allWords.filter {
                    it.script == tmpl.script && (it.introduces == tmpl.char || it.chars.contains(tmpl.char))
                }
                val chosenWord = wordsContainingChar.firstOrNull { w ->
                    w.chars.all { c -> learnedCharsSet.contains(c) }
                } ?: wordsContainingChar.firstOrNull()

                if (chosenWord != null) {
                    val wordType = if (idx % 2 == 0) {
                        QuizQuestionType.MEANING_ROMAJI_TO_WORD
                    } else {
                        QuizQuestionType.LISTEN_TO_WORD
                    }
                    questions.add(QuizQuestion(type = wordType, targetWord = chosenWord))
                }

                // 3. Nếu là chữ hay sai -> xuất hiện thêm 1 lần nữa ở dạng đề ngược lại!
                if (isFrequentlyMissed) {
                    val extraSingleType = if (singleType == QuizQuestionType.ROMAJI_TO_KANA) {
                        QuizQuestionType.LISTEN_TO_KANA
                    } else {
                        QuizQuestionType.ROMAJI_TO_KANA
                    }
                    questions.add(QuizQuestion(type = extraSingleType, targetTemplate = tmpl))
                }
            }

            hadErrorOnCurrentItem = false
            val firstQ = questions.firstOrNull()
            val initialSlots = firstQ?.targetWord?.let { buildWordSlotsForReview(it) }.orEmpty()
            val firstWritable = initialSlots.indexOfFirst { !it.isPreFilledUnlearned }.coerceAtLeast(0)

            _uiState.update {
                ReviewUiState(
                    isLoading = false,
                    settings = settings,
                    questions = questions,
                    currentIndex = 0,
                    dueCardsCountAtStart = dueCardsCount,
                    wordSlots = initialSlots,
                    activeSlotIndex = firstWritable
                )
            }

            triggerPromptAudio(firstQ)
        }
    }

    /**
     * Khi ôn tập:
     * - Nếu mọi chữ trong từ đã học (hoặc người dùng đã học qua các chữ đó), bắt viết cả từ.
     * - Nếu từ còn chứa chữ chưa học, chữ chưa học hiển thị sẵn màu xám.
     */
    private fun buildWordSlotsForReview(word: KanaVocabWord): List<WordCharSlot> {
        val allCharsLearned = word.chars.all { learnedCharsSet.contains(it) }
        return word.chars.mapIndexed { idx, ch ->
            val tmpl = templatesByChar[ch]
            val isLearned = allCharsLearned || learnedCharsSet.contains(ch) || ch == word.introduces
            WordCharSlot(
                index = idx,
                targetChar = ch,
                expectedStrokeCount = tmpl?.strokeCount ?: 1,
                templateStrokes = tmpl?.toTypedStrokes().orEmpty(),
                isPreFilledUnlearned = !isLearned,
                userStrokes = emptyList(),
                evaluation = null
            )
        }
    }

    private fun triggerPromptAudio(question: QuizQuestion?) {
        if (question == null) return
        when (question.type) {
            QuizQuestionType.LISTEN_TO_KANA -> {
                val t = question.targetTemplate ?: return
                audioProvider.speak(t.char, t.romaji)
            }

            QuizQuestionType.LISTEN_TO_WORD -> {
                val w = question.targetWord ?: return
                audioProvider.speak(w.word, w.romaji)
            }

            else -> {}
        }
    }

    fun speakCurrentPrompt() {
        val q = _uiState.value.currentQuestion ?: return
        if (q.targetTemplate != null) {
            audioProvider.speak(q.targetTemplate.char, q.targetTemplate.romaji)
        } else if (q.targetWord != null) {
            audioProvider.speak(q.targetWord.word, q.targetWord.romaji)
        }
    }

    fun revealRomajiFallback() {
        _uiState.update { it.copy(showFallbackRomajiInListenMode = true) }
    }

    fun onUserCanvasActivity() {
        autoAdvanceJob?.cancel()
    }

    fun onStrokeCompleted(stroke: Stroke) {
        autoAdvanceJob?.cancel()
        val state = _uiState.value
        if (state.showingCorrectionAnimation) return

        if (!state.isMultiBoxMode) {
            _uiState.update {
                it.copy(
                    singleCharStrokes = it.singleCharStrokes + stroke,
                    lastSingleEvaluation = null,
                    feedbackMessage = null
                )
            }
        } else {
            val activeIdx = state.activeSlotIndex
            val slot = state.wordSlots.getOrNull(activeIdx) ?: return
            if (slot.isPreFilledUnlearned) return

            val updatedStrokes = slot.userStrokes + stroke
            val updatedSlots = state.wordSlots.mapIndexed { idx, item ->
                if (idx == activeIdx) item.copy(userStrokes = updatedStrokes, evaluation = null)
                else item
            }
            _uiState.update {
                it.copy(
                    wordSlots = updatedSlots,
                    feedbackMessage = null
                )
            }

            if (updatedStrokes.size >= slot.expectedStrokeCount) {
                val nextWritableIdx = updatedSlots.indexOfFirst {
                    it.index > activeIdx && !it.isPreFilledUnlearned && it.userStrokes.isEmpty()
                }
                if (nextWritableIdx != -1) {
                    val delayMs = state.settings.autoAdvanceDelayMs
                    autoAdvanceJob = viewModelScope.launch {
                        delay(delayMs)
                        _uiState.update { cur ->
                            if (cur.activeSlotIndex == activeIdx && !cur.showingCorrectionAnimation) {
                                cur.copy(activeSlotIndex = nextWritableIdx)
                            } else {
                                cur
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectWordSlot(slotIndex: Int) {
        autoAdvanceJob?.cancel()
        val slot = _uiState.value.wordSlots.getOrNull(slotIndex) ?: return
        if (slot.isPreFilledUnlearned) return
        _uiState.update { it.copy(activeSlotIndex = slotIndex, feedbackMessage = null) }
    }

    fun undoLastStroke() {
        autoAdvanceJob?.cancel()
        val state = _uiState.value
        if (state.showingCorrectionAnimation) return

        if (!state.isMultiBoxMode) {
            if (state.singleCharStrokes.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        singleCharStrokes = it.singleCharStrokes.dropLast(1),
                        lastSingleEvaluation = null,
                        feedbackMessage = null
                    )
                }
            }
        } else {
            val activeIdx = state.activeSlotIndex
            val slot = state.wordSlots.getOrNull(activeIdx) ?: return
            if (slot.userStrokes.isNotEmpty()) {
                val updatedSlots = state.wordSlots.mapIndexed { idx, item ->
                    if (idx == activeIdx) item.copy(userStrokes = item.userStrokes.dropLast(1), evaluation = null)
                    else item
                }
                _uiState.update { it.copy(wordSlots = updatedSlots, feedbackMessage = null) }
            } else {
                val prevWritable = state.wordSlots
                    .lastOrNull { it.index < activeIdx && !it.isPreFilledUnlearned }
                if (prevWritable != null) {
                    _uiState.update { it.copy(activeSlotIndex = prevWritable.index) }
                }
            }
        }
    }

    fun clearCanvas() {
        autoAdvanceJob?.cancel()
        val state = _uiState.value
        if (state.showingCorrectionAnimation) return

        if (!state.isMultiBoxMode) {
            _uiState.update {
                it.copy(
                    singleCharStrokes = emptyList(),
                    lastSingleEvaluation = null,
                    feedbackMessage = null
                )
            }
        } else {
            val resetSlots = state.wordSlots.map { slot ->
                if (slot.isPreFilledUnlearned) slot
                else slot.copy(userStrokes = emptyList(), evaluation = null)
            }
            val firstWritable = resetSlots.indexOfFirst { !it.isPreFilledUnlearned }.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    wordSlots = resetSlots,
                    activeSlotIndex = firstWritable,
                    feedbackMessage = null
                )
            }
        }
    }

    fun dismissCorrectionAndStartRewrite() {
        autoAdvanceJob?.cancel()
        val state = _uiState.value
        if (!state.isMultiBoxMode) {
            _uiState.update {
                it.copy(
                    showingCorrectionAnimation = false,
                    correctionTemplate = null,
                    singleCharStrokes = emptyList(),
                    lastSingleEvaluation = null,
                    feedbackMessage = "Hãy tự viết lại đúng 1 lần từ trí nhớ.",
                    isFeedbackError = false
                )
            }
        } else {
            val resetSlots = state.wordSlots.map { slot ->
                if (slot.isPreFilledUnlearned) slot
                else slot.copy(userStrokes = emptyList(), evaluation = null)
            }
            val firstWritable = resetSlots.indexOfFirst { !it.isPreFilledUnlearned }.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    showingCorrectionAnimation = false,
                    correctionTemplate = null,
                    wordSlots = resetSlots,
                    activeSlotIndex = firstWritable,
                    feedbackMessage = "Hãy tự viết lại cả từ đúng 1 lần.",
                    isFeedbackError = false
                )
            }
        }
    }

    fun checkCurrentSubmission(): Boolean {
        autoAdvanceJob?.cancel()
        val state = _uiState.value
        val engine = recognizer ?: return false
        val q = state.currentQuestion ?: return false
        val config = RecognizerConfig.forLevel(state.settings.strictnessLevel)

        return if (q.targetTemplate != null) {
            val result = engine.recognize(state.singleCharStrokes, q.targetTemplate.char, config)
            handleSingleResult(q.targetTemplate, result)
        } else if (q.targetWord != null) {
            handleWordResult(q.targetWord, config)
        } else {
            false
        }
    }

    private fun handleSingleResult(
        target: KanaStrokeTemplate,
        result: RecognitionResult
    ): Boolean {
        if (result.passed) {
            val passedFirstTry = !hadErrorOnCurrentItem
            updateSrsForChar(target, result.score, passedFirstTry)
            audioProvider.speak(target.char, target.romaji)
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        successOverlayText = "${target.char} (${target.romaji}) • ${result.score} điểm"
                    )
                }
                delay(650L)
                _uiState.update { it.copy(successOverlayText = null) }
                advanceToNextQuestion(result.score, passedFirstTry)
            }
            return true
        } else {
            hadErrorOnCurrentItem = true
            val errReason = result.errors.joinToString(", ") { it.messageVi }
                .ifEmpty { "Chưa đúng nét" }
            _uiState.update {
                it.copy(
                    lastSingleEvaluation = result,
                    showingCorrectionAnimation = true,
                    correctionTemplate = target,
                    mustRewriteAfterError = true,
                    feedbackMessage = "Chưa đạt ($errReason). Đáp án: '${target.char}'. Xem lại nét đúng rồi viết lại 1 lần!",
                    isFeedbackError = true,
                    shakeTrigger = it.shakeTrigger + 1
                )
            }
            audioProvider.speak(target.char, target.romaji)
            return false
        }
    }

    private fun handleWordResult(
        word: KanaVocabWord,
        config: RecognizerConfig
    ): Boolean {
        val state = _uiState.value
        val engine = recognizer ?: return false

        val evaluatedSlots = state.wordSlots.map { slot ->
            if (slot.isPreFilledUnlearned) {
                slot
            } else {
                val eval = engine.recognize(slot.userStrokes, slot.targetChar, config)
                slot.copy(evaluation = eval)
            }
        }

        val firstFailed = evaluatedSlots.firstOrNull {
            !it.isPreFilledUnlearned && it.evaluation?.passed != true
        }

        if (firstFailed == null) {
            val passedFirstTry = !hadErrorOnCurrentItem
            val writableScores = evaluatedSlots
                .filter { !it.isPreFilledUnlearned }
                .mapNotNull { it.evaluation?.score }
            val avgWordScore = if (writableScores.isNotEmpty()) {
                writableScores.average().roundToInt()
            } else {
                85
            }

            // Cập nhật SRS cho từng chữ đã viết và cho cả từ
            for (slot in evaluatedSlots) {
                if (!slot.isPreFilledUnlearned) {
                    val tmpl = templatesByChar[slot.targetChar]
                    val sc = slot.evaluation?.score ?: avgWordScore
                    if (tmpl != null) {
                        updateSrsForChar(tmpl, sc, passedFirstTry)
                    }
                }
            }
            updateSrsForWord(word, avgWordScore, passedFirstTry)
            audioProvider.speak(word.word, word.romaji)

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        successOverlayText = "${word.word} (${word.meaningVi}) • $avgWordScore điểm"
                    )
                }
                delay(650L)
                _uiState.update { it.copy(successOverlayText = null) }
                advanceToNextQuestion(avgWordScore, passedFirstTry)
            }
            return true
        } else {
            hadErrorOnCurrentItem = true
            val failedTmpl = templatesByChar[firstFailed.targetChar]
            val errReason = firstFailed.evaluation?.errors
                ?.joinToString(", ") { it.messageVi }
                .orEmpty()
                .ifEmpty { "Chưa đúng nét" }
            _uiState.update {
                it.copy(
                    wordSlots = evaluatedSlots,
                    activeSlotIndex = firstFailed.index,
                    showingCorrectionAnimation = true,
                    correctionTemplate = failedTmpl,
                    mustRewriteAfterError = true,
                    feedbackMessage = "Ô số ${firstFailed.index + 1} ('${firstFailed.targetChar}'): $errReason. Đáp án từ: ${word.word}. Hãy xem nét mẫu rồi viết lại 1 lần!",
                    isFeedbackError = true,
                    shakeTrigger = it.shakeTrigger + 1
                )
            }
            failedTmpl?.let { audioProvider.speak(it.char, it.romaji) }
            return false
        }
    }

    private fun advanceToNextQuestion(score: Int, passedFirstTry: Boolean) {
        val state = _uiState.value
        val nextIdx = state.currentIndex + 1
        val nextCorrectCount = state.firstAttemptCorrectCount + if (passedFirstTry) 1 else 0
        val nextScoreSum = state.totalScoreSum + score
        val nextGradedCount = state.gradedItemsCount + 1

        if (nextIdx < state.questions.size) {
            hadErrorOnCurrentItem = false
            val nextQ = state.questions[nextIdx]
            val slots = nextQ.targetWord?.let { buildWordSlotsForReview(it) }.orEmpty()
            val firstWritable = slots.indexOfFirst { !it.isPreFilledUnlearned }.coerceAtLeast(0)

            _uiState.update {
                it.copy(
                    currentIndex = nextIdx,
                    firstAttemptCorrectCount = nextCorrectCount,
                    totalScoreSum = nextScoreSum,
                    gradedItemsCount = nextGradedCount,
                    singleCharStrokes = emptyList(),
                    lastSingleEvaluation = null,
                    wordSlots = slots,
                    activeSlotIndex = firstWritable,
                    showingCorrectionAnimation = false,
                    correctionTemplate = null,
                    mustRewriteAfterError = false,
                    showFallbackRomajiInListenMode = false,
                    feedbackMessage = null,
                    isFeedbackError = false
                )
            }
            triggerPromptAudio(nextQ)
        } else {
            viewModelScope.launch {
                settingsRepository.recordStudyActivityToday()
            }
            _uiState.update {
                it.copy(
                    firstAttemptCorrectCount = nextCorrectCount,
                    totalScoreSum = nextScoreSum,
                    gradedItemsCount = nextGradedCount,
                    isSessionCompleted = true
                )
            }
        }
    }

    private fun updateSrsForChar(
        template: KanaStrokeTemplate,
        score: Int,
        passedFirstAttempt: Boolean
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existingProg = kanaDao.getCharacterProgress(template.char)
            kanaDao.upsertCharacterProgress(
                CharacterProgressEntity(
                    char = template.char,
                    script = template.script,
                    rowName = template.row,
                    learned = true,
                    lastScore = score,
                    practiceCount = (existingProg?.practiceCount ?: 0) + 1,
                    errorCount = (existingProg?.errorCount ?: 0) + if (passedFirstAttempt) 0 else 1,
                    updatedAtEpochMs = now
                )
            )

            val cardId = "CHAR_${template.char}"
            val existingCard = kanaDao.getSrsCard(cardId)
            val updatedCard = SrsScheduler.computeNextCardState(
                existing = existingCard,
                cardId = cardId,
                cardType = "CHAR",
                targetKey = template.char,
                script = template.script,
                score = score,
                passedFirstAttempt = passedFirstAttempt,
                nowEpochMs = now
            )
            kanaDao.upsertSrsCard(updatedCard)
        }
    }

    private fun updateSrsForWord(
        word: KanaVocabWord,
        score: Int,
        passedFirstAttempt: Boolean
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val key = word.id.ifBlank { word.word }
            val cardId = "WORD_$key"
            val existingCard = kanaDao.getSrsCard(cardId)
            val updatedCard = SrsScheduler.computeNextCardState(
                existing = existingCard,
                cardId = cardId,
                cardType = "WORD",
                targetKey = word.word,
                script = word.script,
                score = score,
                passedFirstAttempt = passedFirstAttempt,
                nowEpochMs = now
            )
            kanaDao.upsertSrsCard(updatedCard)
        }
    }

    companion object {
        fun provideFactory(
            strokeRepository: StrokeAssetRepository,
            vocabRepository: VocabAssetRepository,
            settingsRepository: UserSettingsRepository,
            kanaDao: KanaDao,
            audioProvider: AudioProvider
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReviewViewModel(
                    strokeRepository = strokeRepository,
                    vocabRepository = vocabRepository,
                    settingsRepository = settingsRepository,
                    kanaDao = kanaDao,
                    audioProvider = audioProvider
                ) as T
            }
        }
    }
}
