package vn.kana.app.ui.lesson

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
import vn.kana.app.data.local.LessonProgressEntity
import vn.kana.app.domain.AudioProvider
import vn.kana.app.domain.KanaVocabWord
import vn.kana.app.domain.LessonStepType
import vn.kana.app.domain.QuizQuestion
import vn.kana.app.domain.QuizQuestionType
import vn.kana.app.domain.SrsScheduler
import vn.kana.app.ui.components.WordCharSlot
import vn.kana.recognizer.KanaRecognizer
import vn.kana.recognizer.KanaStrokeTemplate
import vn.kana.recognizer.RecognitionResult
import vn.kana.recognizer.RecognizerConfig
import vn.kana.recognizer.Stroke

data class LessonUiState(
    val isLoading: Boolean = true,
    val rowName: String = "あ行",
    val script: String = "hiragana",
    val rowCharacters: List<KanaStrokeTemplate> = emptyList(),
    val currentCharIndex: Int = 0,
    val stepType: LessonStepType = LessonStepType.STEP_1_OBSERVE,

    val settings: UserSettings = UserSettings(),

    // Bước 1: Xem nét
    val isAnimationPlaying: Boolean = true,
    val isAnimationLooping: Boolean = true,
    val animationRestartTrigger: Int = 0,

    // Bước 2, 3, 4 & Quiz chữ đơn
    val singleCharStrokes: List<Stroke> = emptyList(),
    val lastSingleEvaluation: RecognitionResult? = null,

    // Bước 5 & Quiz từ vựng (dãy ô vuông 原稿用紙)
    val currentCharWords: List<KanaVocabWord> = emptyList(),
    val currentWordIndex: Int = 0,
    val wordSlots: List<WordCharSlot> = emptyList(),
    val activeSlotIndex: Int = 0,

    // Trạng thái sửa lỗi bắt buộc
    val showingCorrectionAnimation: Boolean = false,
    val correctionTemplate: KanaStrokeTemplate? = null,
    val mustRewriteAfterError: Boolean = false,
    val feedbackMessage: String? = null,
    val isFeedbackError: Boolean = false,
    val shakeTrigger: Int = 0,

    // Kiểm tra nhanh 1 chữ đã biết (viết đúng 1 lần là pass chữ đó và qua chữ mới ngay!)
    val isCharQuickCheckMode: Boolean = false,
    // Kiểm tra vượt bài (cả hàng)
    val isRowTestOutMode: Boolean = false,
    val quizQuestions: List<QuizQuestion> = emptyList(),
    val currentQuizIndex: Int = 0,
    val quizFirstAttemptPassedCount: Int = 0,
    val showFallbackRomajiInListenMode: Boolean = false,
    val isLessonCompleted: Boolean = false,
    val isTestOutFailed: Boolean = false
) {
    val currentCharacter: KanaStrokeTemplate?
        get() = rowCharacters.getOrNull(currentCharIndex)

    val currentVocabWord: KanaVocabWord?
        get() = when (stepType) {
            LessonStepType.STEP_5_VOCAB -> currentCharWords.getOrNull(currentWordIndex)
            LessonStepType.END_OF_ROW_QUIZ -> currentQuizQuestion?.targetWord
            else -> null
        }

    val currentQuizQuestion: QuizQuestion?
        get() = quizQuestions.getOrNull(currentQuizIndex)

    val isMultiBoxMode: Boolean
        get() = stepType == LessonStepType.STEP_5_VOCAB ||
            (stepType == LessonStepType.END_OF_ROW_QUIZ &&
                (currentQuizQuestion?.type == QuizQuestionType.MEANING_ROMAJI_TO_WORD ||
                    currentQuizQuestion?.type == QuizQuestionType.LISTEN_TO_WORD))
}

class LessonViewModel(
    private val rowName: String,
    private val script: String,
    private val startInTestOutMode: Boolean = false,
    private val initialFocusChar: String? = null,
    private val strokeRepository: StrokeAssetRepository,
    private val vocabRepository: VocabAssetRepository,
    private val settingsRepository: UserSettingsRepository,
    private val kanaDao: KanaDao,
    private val audioProvider: AudioProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonUiState(rowName = rowName, script = script))
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    private var recognizer: KanaRecognizer? = null
    private var allTemplatesByChar: Map<String, KanaStrokeTemplate> = emptyMap()
    private val learnedCharsSet = LinkedHashSet<String>()
    private var autoAdvanceJob: Job? = null
    private var hadErrorOnCurrentQuizItem = false

    init {
        observeSettings()
        loadLessonData()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { newSettings ->
                _uiState.update { it.copy(settings = newSettings) }
            }
        }
    }

    private fun loadLessonData() {
        viewModelScope.launch {
            val allChars = strokeRepository.getAllCharacters()
            allTemplatesByChar = allChars.associateBy { it.char }
            recognizer = KanaRecognizer(allChars)

            val currentSettings = settingsRepository.settingsFlow.first()

            val dbLearned = kanaDao.getLearnedCharacters().map { it.char }.toSet()
            learnedCharsSet.addAll(dbLearned)

            val rowChars = allChars.filter { it.script == script && it.row == rowName }
            val currentCategory = rowChars.firstOrNull()?.category ?: "basic"

            val basicInScript = allChars.filter { it.script == script && it.category == "basic" }
            if (currentCategory == "basic") {
                val distinctRows = basicInScript.map { it.row }.distinct()
                val targetRowOrder = distinctRows.indexOf(rowName)
                if (targetRowOrder > 0) {
                    for (rIdx in 0 until targetRowOrder) {
                        val prevRow = distinctRows[rIdx]
                        basicInScript.filter { it.row == prevRow }.forEach { learnedCharsSet.add(it.char) }
                    }
                }
            } else {
                basicInScript.forEach { learnedCharsSet.add(it.char) }
                if (currentCategory == "small") {
                    allChars.filter { it.script == script && it.category == "dakuten" }
                        .forEach { learnedCharsSet.add(it.char) }
                }
            }

            // QUAN TRỌNG: Tìm chữ đầu tiên CHƯA HỌC trong hàng này để người dùng không bao giờ bị học lại chữ あ!
            val startIdx = when {
                !initialFocusChar.isNullOrBlank() -> {
                    rowChars.indexOfFirst { it.char == initialFocusChar }.coerceAtLeast(0)
                }
                else -> {
                    val firstUnlearned = rowChars.indexOfFirst { it.char !in dbLearned }
                    if (firstUnlearned != -1) firstUnlearned else 0
                }
            }

            for (i in 0 until startIdx) {
                learnedCharsSet.add(rowChars[i].char)
            }

            val startChar = rowChars.getOrNull(startIdx)
            val startCharWords = if (startChar != null) {
                vocabRepository.getWordsForCharacter(startChar.char)
            } else {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    settings = currentSettings,
                    rowCharacters = rowChars,
                    currentCharIndex = startIdx,
                    stepType = LessonStepType.STEP_1_OBSERVE,
                    currentCharWords = startCharWords,
                    isAnimationPlaying = true
                )
            }

            if (startInTestOutMode) {
                jumpToRowTestOut()
            } else {
                startChar?.let { speakIfEnabled(it.char, it.romaji) }
            }
        }
    }

    private fun speakIfEnabled(japaneseText: String, assetKey: String? = null) {
        if (_uiState.value.settings.soundEnabled) {
            audioProvider.speak(japaneseText, assetKey)
        }
    }

    fun speakCurrentPrompt() {
        val state = _uiState.value
        when (state.stepType) {
            LessonStepType.STEP_1_OBSERVE,
            LessonStepType.STEP_2_TRACE,
            LessonStepType.STEP_3_GUIDED,
            LessonStepType.STEP_4_RECALL -> {
                val ch = state.currentCharacter ?: return
                audioProvider.speak(ch.char, ch.romaji)
            }

            LessonStepType.STEP_5_VOCAB -> {
                val w = state.currentVocabWord ?: return
                audioProvider.speak(w.word, w.romaji)
            }

            LessonStepType.END_OF_ROW_QUIZ -> {
                val q = state.currentQuizQuestion ?: return
                if (q.targetTemplate != null) {
                    audioProvider.speak(q.targetTemplate.char, q.targetTemplate.romaji)
                } else if (q.targetWord != null) {
                    audioProvider.speak(q.targetWord.word, q.targetWord.romaji)
                }
            }
        }
    }

    fun speakCurrentExampleSentence() {
        val w = _uiState.value.currentVocabWord ?: return
        if (w.exampleSentence.isNotBlank()) {
            audioProvider.speak(w.exampleSentence, null)
        }
    }

    fun advanceFromObserveToTrace() {
        _uiState.update {
            it.copy(
                stepType = LessonStepType.STEP_2_TRACE,
                isCharQuickCheckMode = false,
                singleCharStrokes = emptyList(),
                lastSingleEvaluation = null,
                showingCorrectionAnimation = false,
                mustRewriteAfterError = false,
                feedbackMessage = null,
                isFeedbackError = false
            )
        }
    }

    /**
     * Khi người dùng bấm "Đã biết chữ này • Kiểm tra luôn":
     * Chuyển thẳng sang viết chữ đó 1 lần từ trí nhớ.
     * Nếu viết ĐÚNG -> Lưu chữ đó ĐÃ THUỘC vào DB ngay lập tức và nhảy sang chữ cái mới tiếp theo!
     */
    fun jumpToCharacterFastTest() {
        autoAdvanceJob?.cancel()
        val currentChar = _uiState.value.currentCharacter ?: return
        _uiState.update {
            it.copy(
                stepType = LessonStepType.STEP_4_RECALL,
                isCharQuickCheckMode = true,
                singleCharStrokes = emptyList(),
                lastSingleEvaluation = null,
                showingCorrectionAnimation = false,
                correctionTemplate = null,
                mustRewriteAfterError = false,
                feedbackMessage = "Viết đúng chữ '${currentChar.romaji}' 1 lần để qua ngay chữ tiếp theo!",
                isFeedbackError = false
            )
        }
        speakIfEnabled(currentChar.char, currentChar.romaji)
    }

    /**
     * Kiểm tra vượt bài (Test-out cả hàng).
     */
    fun jumpToRowTestOut() {
        autoAdvanceJob?.cancel()
        _uiState.value.rowCharacters.forEach { learnedCharsSet.add(it.char) }
        _uiState.update {
            it.copy(
                isRowTestOutMode = true,
                isTestOutFailed = false
            )
        }
        startEndOfRowQuiz(isTestOut = true)
    }

    fun switchFromTestOutToNormalLesson() {
        autoAdvanceJob?.cancel()
        val firstChar = _uiState.value.rowCharacters.firstOrNull() ?: return
        viewModelScope.launch {
            val words = vocabRepository.getWordsForCharacter(firstChar.char)
            _uiState.update {
                it.copy(
                    currentCharIndex = 0,
                    stepType = LessonStepType.STEP_1_OBSERVE,
                    isRowTestOutMode = false,
                    isCharQuickCheckMode = false,
                    isTestOutFailed = false,
                    isLessonCompleted = false,
                    currentCharWords = words,
                    currentWordIndex = 0,
                    singleCharStrokes = emptyList(),
                    lastSingleEvaluation = null,
                    showingCorrectionAnimation = false,
                    mustRewriteAfterError = false,
                    feedbackMessage = null,
                    isFeedbackError = false,
                    isAnimationPlaying = true,
                    animationRestartTrigger = it.animationRestartTrigger + 1
                )
            }
            speakIfEnabled(firstChar.char, firstChar.romaji)
        }
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
            val currentSlots = state.wordSlots
            val slot = currentSlots.getOrNull(activeIdx) ?: return
            if (slot.isPreFilledUnlearned) return

            val updatedStrokes = slot.userStrokes + stroke
            val updatedSlots = currentSlots.mapIndexed { idx, item ->
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
        _uiState.update {
            it.copy(
                activeSlotIndex = slotIndex,
                feedbackMessage = null
            )
        }
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
                val updatedSlots = state.wordSlots.mapIndexed { idx, s ->
                    if (idx == activeIdx) s.copy(userStrokes = s.userStrokes.dropLast(1), evaluation = null)
                    else s
                }
                _uiState.update {
                    it.copy(
                        wordSlots = updatedSlots,
                        feedbackMessage = null
                    )
                }
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
                    feedbackMessage = if (it.mustRewriteAfterError) {
                        "Hãy viết lại 1 lần để đi tiếp."
                    } else {
                        null
                    },
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
                    feedbackMessage = "Hãy viết lại từ này 1 lần để đi tiếp.",
                    isFeedbackError = false
                )
            }
        }
    }

    fun revealListenRomajiFallback() {
        _uiState.update { it.copy(showFallbackRomajiInListenMode = true) }
    }

    fun checkCurrentSubmission(): Boolean {
        autoAdvanceJob?.cancel()
        val state = _uiState.value
        val engine = recognizer ?: return false
        val config = RecognizerConfig.forLevel(state.settings.strictnessLevel)

        return when (state.stepType) {
            LessonStepType.STEP_1_OBSERVE -> {
                advanceFromObserveToTrace()
                true
            }

            LessonStepType.STEP_2_TRACE,
            LessonStepType.STEP_3_GUIDED,
            LessonStepType.STEP_4_RECALL -> {
                val target = state.currentCharacter ?: return false
                val result = engine.recognize(state.singleCharStrokes, target.char, config)
                handleSingleCharacterResult(target, result)
            }

            LessonStepType.STEP_5_VOCAB -> {
                handleMultiBoxWordSubmission(config)
            }

            LessonStepType.END_OF_ROW_QUIZ -> {
                val q = state.currentQuizQuestion ?: return false
                if (q.targetTemplate != null) {
                    val result = engine.recognize(state.singleCharStrokes, q.targetTemplate.char, config)
                    handleQuizSingleCharResult(q.targetTemplate, result)
                } else {
                    handleQuizWordResult(config)
                }
            }
        }
    }

    private fun handleSingleCharacterResult(
        target: KanaStrokeTemplate,
        result: RecognitionResult
    ): Boolean {
        val state = _uiState.value
        if (result.passed) {
            when (state.stepType) {
                LessonStepType.STEP_2_TRACE -> {
                    _uiState.update {
                        it.copy(
                            stepType = LessonStepType.STEP_3_GUIDED,
                            singleCharStrokes = emptyList(),
                            lastSingleEvaluation = null,
                            showingCorrectionAnimation = false,
                            mustRewriteAfterError = false,
                            feedbackMessage = "Đồ nét tốt (${result.score}đ)! Sang Bước 3: Viết có gợi ý.",
                            isFeedbackError = false
                        )
                    }
                }

                LessonStepType.STEP_3_GUIDED -> {
                    _uiState.update {
                        it.copy(
                            stepType = LessonStepType.STEP_4_RECALL,
                            singleCharStrokes = emptyList(),
                            lastSingleEvaluation = null,
                            showingCorrectionAnimation = false,
                            mustRewriteAfterError = false,
                            feedbackMessage = "Tốt lắm (${result.score}đ)! Sang Bước 4: Tự viết từ trí nhớ.",
                            isFeedbackError = false
                        )
                    }
                    speakIfEnabled(target.char, target.romaji)
                }

                LessonStepType.STEP_4_RECALL -> {
                    // Đánh dấu chữ này ĐÃ HỌC XONG và lưu ngay vào DB!
                    learnedCharsSet.add(target.char)
                    markCharacterLearnedInDb(target, result.score)

                    if (state.isCharQuickCheckMode) {
                        // Trong chế độ Kiểm tra nhanh chữ đã biết: viết đúng 1 lần là QUA NGAY chữ mới tiếp theo!
                        advanceToNextCharacterOrQuiz()
                    } else {
                        startStep5VocabForCurrentCharacter(wordIdx = 0)
                    }
                }

                else -> {}
            }
            return true
        } else {
            val errReason = result.errors.joinToString(", ") { it.messageVi }
                .ifEmpty { "Chưa khớp nét chuẩn" }
            val requireRewrite = (state.stepType == LessonStepType.STEP_4_RECALL)
            _uiState.update {
                it.copy(
                    lastSingleEvaluation = result,
                    showingCorrectionAnimation = true,
                    correctionTemplate = target,
                    mustRewriteAfterError = requireRewrite,
                    feedbackMessage = "Chưa chính xác ($errReason). Xem lại nét đúng rồi thử lại nhé!",
                    isFeedbackError = true,
                    shakeTrigger = it.shakeTrigger + 1
                )
            }
            speakIfEnabled(target.char, target.romaji)
            return false
        }
    }

    private fun startStep5VocabForCurrentCharacter(wordIdx: Int) {
        val state = _uiState.value
        val words = state.currentCharWords
        val word = words.getOrNull(wordIdx)
        if (word == null) {
            advanceToNextCharacterOrQuiz()
            return
        }

        val slots = buildWordSlots(word)
        val firstWritable = slots.indexOfFirst { !it.isPreFilledUnlearned }.coerceAtLeast(0)

        _uiState.update {
            it.copy(
                stepType = LessonStepType.STEP_5_VOCAB,
                currentWordIndex = wordIdx,
                wordSlots = slots,
                activeSlotIndex = firstWritable,
                showingCorrectionAnimation = false,
                correctionTemplate = null,
                mustRewriteAfterError = false,
                feedbackMessage = null,
                isFeedbackError = false
            )
        }
        speakIfEnabled(word.word, word.romaji)
    }

    private fun buildWordSlots(word: KanaVocabWord): List<WordCharSlot> {
        return word.chars.mapIndexed { idx, ch ->
            val tmpl = allTemplatesByChar[ch]
            val isLearned = learnedCharsSet.contains(ch)
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

    private fun handleMultiBoxWordSubmission(config: RecognizerConfig): Boolean {
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

        val firstFailedSlot = evaluatedSlots.firstOrNull {
            !it.isPreFilledUnlearned && it.evaluation?.passed != true
        }

        if (firstFailedSlot == null) {
            val nextWordIdx = state.currentWordIndex + 1
            if (nextWordIdx < state.currentCharWords.size) {
                startStep5VocabForCurrentCharacter(nextWordIdx)
            } else {
                advanceToNextCharacterOrQuiz()
            }
            return true
        } else {
            val failedTemplate = allTemplatesByChar[firstFailedSlot.targetChar]
            val errReason = firstFailedSlot.evaluation?.errors
                ?.joinToString(", ") { it.messageVi }
                .orEmpty()
                .ifEmpty { "Chưa đúng nét" }

            _uiState.update {
                it.copy(
                    wordSlots = evaluatedSlots,
                    activeSlotIndex = firstFailedSlot.index,
                    showingCorrectionAnimation = true,
                    correctionTemplate = failedTemplate,
                    mustRewriteAfterError = true,
                    feedbackMessage = "Ô số ${firstFailedSlot.index + 1} ('${firstFailedSlot.targetChar}'): $errReason.",
                    isFeedbackError = true,
                    shakeTrigger = it.shakeTrigger + 1
                )
            }
            failedTemplate?.let { speakIfEnabled(it.char, it.romaji) }
            return false
        }
    }

    private fun advanceToNextCharacterOrQuiz() {
        val state = _uiState.value
        val nextCharIdx = state.currentCharIndex + 1
        if (nextCharIdx < state.rowCharacters.size) {
            val nextChar = state.rowCharacters[nextCharIdx]
            viewModelScope.launch {
                val words = vocabRepository.getWordsForCharacter(nextChar.char)
                _uiState.update {
                    it.copy(
                        currentCharIndex = nextCharIdx,
                        stepType = LessonStepType.STEP_1_OBSERVE,
                        isCharQuickCheckMode = false,
                        currentCharWords = words,
                        currentWordIndex = 0,
                        singleCharStrokes = emptyList(),
                        lastSingleEvaluation = null,
                        showingCorrectionAnimation = false,
                        mustRewriteAfterError = false,
                        feedbackMessage = null,
                        isFeedbackError = false,
                        isAnimationPlaying = true,
                        animationRestartTrigger = it.animationRestartTrigger + 1
                    )
                }
                speakIfEnabled(nextChar.char, nextChar.romaji)
            }
        } else {
            // Đã học xong toàn bộ các chữ trong hàng!
            // Nếu tất cả các chữ trong hàng đều được vượt nhanh (Quick Check), hoàn tất luôn hàng này và mở khoá hàng tiếp theo!
            if (state.isCharQuickCheckMode) {
                completeLessonInDb(
                    firstAttemptCorrect = state.rowCharacters.size,
                    totalQuestions = state.rowCharacters.size
                )
            } else {
                startEndOfRowQuiz(isTestOut = false)
            }
        }
    }

    private fun startEndOfRowQuiz(isTestOut: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentRowChars = state.rowCharacters
            val previousLearnedTemplates = learnedCharsSet
                .mapNotNull { allTemplatesByChar[it] }
                .filter { it.script == script && it.row != rowName }

            val poolChars = (currentRowChars + previousLearnedTemplates.take(3)).distinctBy { it.char }
            val poolWords = vocabRepository.getWordsForCharacters(poolChars.map { it.char })

            val questions = ArrayList<QuizQuestion>()
            if (isTestOut) {
                // Kiểm tra vượt bài gọn: kiểm tra các chữ trong hàng này
                for (ch in currentRowChars) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.ROMAJI_TO_KANA,
                            targetTemplate = ch
                        )
                    )
                }
            } else {
                for (ch in currentRowChars) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.ROMAJI_TO_KANA,
                            targetTemplate = ch
                        )
                    )
                }
                val listenChar = currentRowChars.firstOrNull()
                if (listenChar != null) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.LISTEN_TO_KANA,
                            targetTemplate = listenChar
                        )
                    )
                }
                val meaningWord = poolWords.firstOrNull { w -> w.introduces in currentRowChars.map { it.char } }
                if (meaningWord != null) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.MEANING_ROMAJI_TO_WORD,
                            targetWord = meaningWord
                        )
                    )
                }
            }

            hadErrorOnCurrentQuizItem = false
            val firstQuestion = questions.firstOrNull()
            val initialSlots = firstQuestion?.targetWord?.let { buildWordSlots(it) }.orEmpty()
            val firstWritable = initialSlots.indexOfFirst { !it.isPreFilledUnlearned }.coerceAtLeast(0)

            _uiState.update {
                it.copy(
                    stepType = LessonStepType.END_OF_ROW_QUIZ,
                    isRowTestOutMode = isTestOut,
                    isTestOutFailed = false,
                    quizQuestions = questions,
                    currentQuizIndex = 0,
                    quizFirstAttemptPassedCount = 0,
                    singleCharStrokes = emptyList(),
                    lastSingleEvaluation = null,
                    wordSlots = initialSlots,
                    activeSlotIndex = firstWritable,
                    showingCorrectionAnimation = false,
                    correctionTemplate = null,
                    mustRewriteAfterError = false,
                    showFallbackRomajiInListenMode = false,
                    feedbackMessage = if (isTestOut) {
                        "Kiểm tra vượt bài: Viết đúng ${questions.size} chữ trong hàng để mở khoá bài tiếp theo!"
                    } else {
                        null
                    },
                    isFeedbackError = false
                )
            }

            triggerAudioForQuizQuestion(firstQuestion)
        }
    }

    private fun triggerAudioForQuizQuestion(question: QuizQuestion?) {
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

    private fun handleQuizSingleCharResult(
        target: KanaStrokeTemplate,
        result: RecognitionResult
    ): Boolean {
        if (result.passed) {
            markCharacterLearnedInDb(target, result.score)
            advanceToNextQuizQuestion()
            return true
        } else {
            hadErrorOnCurrentQuizItem = true
            val errReason = result.errors.joinToString(", ") { it.messageVi }
                .ifEmpty { "Chưa đúng nét" }
            _uiState.update {
                it.copy(
                    lastSingleEvaluation = result,
                    showingCorrectionAnimation = true,
                    correctionTemplate = target,
                    mustRewriteAfterError = true,
                    feedbackMessage = "Chưa đúng ($errReason). Đáp án là '${target.char}'. Xem lại nét rồi viết lại 1 lần!",
                    isFeedbackError = true,
                    shakeTrigger = it.shakeTrigger + 1
                )
            }
            speakIfEnabled(target.char, target.romaji)
            return false
        }
    }

    private fun handleQuizWordResult(config: RecognizerConfig): Boolean {
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

        val firstFailedSlot = evaluatedSlots.firstOrNull {
            !it.isPreFilledUnlearned && it.evaluation?.passed != true
        }

        if (firstFailedSlot == null) {
            advanceToNextQuizQuestion()
            return true
        } else {
            hadErrorOnCurrentQuizItem = true
            val failedTemplate = allTemplatesByChar[firstFailedSlot.targetChar]
            val wordText = state.currentQuizQuestion?.targetWord?.word.orEmpty()
            _uiState.update {
                it.copy(
                    wordSlots = evaluatedSlots,
                    activeSlotIndex = firstFailedSlot.index,
                    showingCorrectionAnimation = true,
                    correctionTemplate = failedTemplate,
                    mustRewriteAfterError = true,
                    feedbackMessage = "Chưa chính xác ở chữ '${firstFailedSlot.targetChar}' (từ đúng: $wordText).",
                    isFeedbackError = true,
                    shakeTrigger = it.shakeTrigger + 1
                )
            }
            failedTemplate?.let { speakIfEnabled(it.char, it.romaji) }
            return false
        }
    }

    private fun advanceToNextQuizQuestion() {
        val state = _uiState.value
        val updatedPassCount = if (!hadErrorOnCurrentQuizItem) {
            state.quizFirstAttemptPassedCount + 1
        } else {
            state.quizFirstAttemptPassedCount
        }
        val nextIdx = state.currentQuizIndex + 1

        if (nextIdx < state.quizQuestions.size) {
            hadErrorOnCurrentQuizItem = false
            val nextQ = state.quizQuestions[nextIdx]
            val slots = nextQ.targetWord?.let { buildWordSlots(it) }.orEmpty()
            val firstWritable = slots.indexOfFirst { !it.isPreFilledUnlearned }.coerceAtLeast(0)

            _uiState.update {
                it.copy(
                    currentQuizIndex = nextIdx,
                    quizFirstAttemptPassedCount = updatedPassCount,
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
            triggerAudioForQuizQuestion(nextQ)
        } else {
            if (state.isRowTestOutMode && updatedPassCount < state.quizQuestions.size) {
                _uiState.update {
                    it.copy(
                        quizFirstAttemptPassedCount = updatedPassCount,
                        isTestOutFailed = true
                    )
                }
            } else {
                completeLessonInDb(updatedPassCount, state.quizQuestions.size)
            }
        }
    }

    private fun markCharacterLearnedInDb(
        template: KanaStrokeTemplate,
        score: Int
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = kanaDao.getCharacterProgress(template.char)
            val updated = CharacterProgressEntity(
                char = template.char,
                script = template.script,
                rowName = template.row,
                learned = true,
                lastScore = score,
                practiceCount = (existing?.practiceCount ?: 0) + 1,
                errorCount = existing?.errorCount ?: 0,
                updatedAtEpochMs = now
            )
            kanaDao.upsertCharacterProgress(updated)

            val charCardId = "CHAR_${template.char}"
            val existingCard = kanaDao.getSrsCard(charCardId)
            val nextCard = SrsScheduler.computeNextCardState(
                existing = existingCard,
                cardId = charCardId,
                cardType = "CHAR",
                targetKey = template.char,
                script = template.script,
                score = score,
                passedFirstAttempt = true,
                nowEpochMs = now
            )
            kanaDao.upsertSrsCard(nextCard)
        }
    }

    private fun completeLessonInDb(firstAttemptCorrect: Int, totalQuestions: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val quizPercent = if (totalQuestions > 0) {
                (firstAttemptCorrect * 100) / totalQuestions
            } else {
                100
            }

            val rowId = "${script}_$rowName"
            kanaDao.upsertLessonProgress(
                LessonProgressEntity(
                    rowId = rowId,
                    script = script,
                    rowName = rowName,
                    completed = true,
                    bestQuizScore = quizPercent,
                    completedAtEpochMs = now
                )
            )

            val rowCharsList = _uiState.value.rowCharacters
            for (ch in rowCharsList) {
                val existingProg = kanaDao.getCharacterProgress(ch.char)
                val lastScore = existingProg?.lastScore ?: 85
                kanaDao.upsertCharacterProgress(
                    CharacterProgressEntity(
                        char = ch.char,
                        script = ch.script,
                        rowName = ch.row,
                        learned = true,
                        lastScore = lastScore,
                        practiceCount = (existingProg?.practiceCount ?: 1),
                        errorCount = existingProg?.errorCount ?: 0,
                        updatedAtEpochMs = now
                    )
                )

                val charCardId = "CHAR_${ch.char}"
                val existingCharCard = kanaDao.getSrsCard(charCardId)
                val nextCharCard = SrsScheduler.computeNextCardState(
                    existing = existingCharCard,
                    cardId = charCardId,
                    cardType = "CHAR",
                    targetKey = ch.char,
                    script = ch.script,
                    score = lastScore,
                    passedFirstAttempt = (existingProg?.errorCount ?: 0) == 0,
                    nowEpochMs = now
                )
                kanaDao.upsertSrsCard(nextCharCard)

                val wordsForChar = vocabRepository.getWordsForCharacter(ch.char)
                for (w in wordsForChar) {
                    val wordKey = w.id.ifBlank { w.word }
                    val wordCardId = "WORD_$wordKey"
                    if (kanaDao.getSrsCard(wordCardId) == null) {
                        val nextWordCard = SrsScheduler.computeNextCardState(
                            existing = null,
                            cardId = wordCardId,
                            cardType = "WORD",
                            targetKey = w.word,
                            script = w.script,
                            score = 85,
                            passedFirstAttempt = true,
                            nowEpochMs = now
                        )
                        kanaDao.upsertSrsCard(nextWordCard)
                    }
                }
            }

            settingsRepository.recordStudyActivityToday()

            _uiState.update {
                it.copy(
                    quizFirstAttemptPassedCount = firstAttemptCorrect,
                    isLessonCompleted = true
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            rowName: String,
            script: String,
            startInTestOutMode: Boolean = false,
            initialFocusChar: String? = null,
            strokeRepository: StrokeAssetRepository,
            vocabRepository: VocabAssetRepository,
            settingsRepository: UserSettingsRepository,
            kanaDao: KanaDao,
            audioProvider: AudioProvider
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LessonViewModel(
                    rowName = rowName,
                    script = script,
                    startInTestOutMode = startInTestOutMode,
                    initialFocusChar = initialFocusChar,
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
