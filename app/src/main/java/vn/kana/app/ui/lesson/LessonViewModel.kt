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
import vn.kana.app.data.local.SrsCardEntity
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

    // Cài đặt
    val settings: UserSettings = UserSettings(),

    // Bước 1: Xem nét
    val isAnimationPlaying: Boolean = true,
    val isAnimationLooping: Boolean = true,
    val showStrokeNumbersInObserve: Boolean = true,
    val animationRestartTrigger: Int = 0,

    // Bước 2, 3, 4 & Quiz chữ đơn
    val singleCharStrokes: List<Stroke> = emptyList(),
    val lastSingleEvaluation: RecognitionResult? = null,

    // Bước 5 & Quiz từ vựng (dãy ô vuông 原稿用紙)
    val currentCharWords: List<KanaVocabWord> = emptyList(),
    val currentWordIndex: Int = 0,
    val wordSlots: List<WordCharSlot> = emptyList(),
    val activeSlotIndex: Int = 0,

    // Trạng thái sửa lỗi bắt buộc (Bước 4, 5 & Kiểm tra cuối bài)
    val showingCorrectionAnimation: Boolean = false,
    val correctionTemplate: KanaStrokeTemplate? = null,
    val mustRewriteAfterError: Boolean = false,
    val feedbackMessage: String? = null,
    val isFeedbackError: Boolean = false,
    val shakeTrigger: Int = 0,

    // Kiểm tra tổng hợp cuối bài / Kiểm tra vượt cấp (Test-out)
    val isRowTestOutMode: Boolean = false,
    val isCharFastTrackMode: Boolean = false,
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

            // Lấy danh sách chữ đã học từ DB + các hàng trước hàng hiện tại
            val dbLearned = kanaDao.getLearnedCharacters().map { it.char }
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
                // Khi đã học sang phần Dakuten/Handakuten hoặc Yōon/っ/ー, coi 46 chữ cơ bản của hệ chữ đó đã được học
                basicInScript.forEach { learnedCharsSet.add(it.char) }
                if (currentCategory == "small") {
                    allChars.filter { it.script == script && it.category == "dakuten" }
                        .forEach { learnedCharsSet.add(it.char) }
                }
            }
            val startIdx = if (!initialFocusChar.isNullOrBlank()) {
                rowChars.indexOfFirst { it.char == initialFocusChar }.coerceAtLeast(0)
            } else {
                0
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

    /**
     * Bỏ qua các bước hướng dẫn (1, 2, 3) của chữ hiện tại để kiểm tra viết từ trí nhớ (Bước 4) + từ vựng (Bước 5) luôn.
     * Nếu viết đúng hết sẽ pass chữ hiện tại và mở khoá ngay chữ cái tiếp theo.
     */
    fun jumpToCharacterFastTest() {
        autoAdvanceJob?.cancel()
        val currentChar = _uiState.value.currentCharacter ?: return
        _uiState.update {
            it.copy(
                stepType = LessonStepType.STEP_4_RECALL,
                isCharFastTrackMode = true,
                singleCharStrokes = emptyList(),
                lastSingleEvaluation = null,
                showingCorrectionAnimation = false,
                correctionTemplate = null,
                mustRewriteAfterError = false,
                feedbackMessage = "Kiểm tra nhanh chữ '${currentChar.romaji}': Viết đúng chữ và từ vựng để qua luôn chữ mới!",
                isFeedbackError = false
            )
        }
        speakIfEnabled(currentChar.char, currentChar.romaji)
    }

    /**
     * Kiểm tra vượt bài (Test-out cả hàng):
     * Mở toàn bộ các chữ trong hàng này để kiểm tra viết chữ + viết từ vựng.
     * Nếu đúng hết 100% ngay lần đầu thì được Pass cả bài và mở khoá hàng chữ cái mới.
     */
    fun jumpToRowTestOut() {
        autoAdvanceJob?.cancel()
        // Trong chế độ kiểm tra vượt cấp của hàng, coi toàn bộ các chữ trong hàng này đều phải tự viết
        _uiState.value.rowCharacters.forEach { learnedCharsSet.add(it.char) }
        _uiState.update {
            it.copy(
                isRowTestOutMode = true,
                isTestOutFailed = false
            )
        }
        startEndOfRowQuiz(isTestOut = true)
    }

    /**
     * Chuyển từ màn hình báo chưa đạt kiểm tra vượt cấp về học tuần tự từng chữ từ Bước 1.
     */
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
                    isCharFastTrackMode = false,
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

    // --- Điều khiển Bước 1: XEM ---
    fun toggleObservePlayPause() {
        _uiState.update { it.copy(isAnimationPlaying = !it.isAnimationPlaying) }
    }

    fun replayObserveAnimation() {
        _uiState.update {
            it.copy(
                isAnimationPlaying = true,
                animationRestartTrigger = it.animationRestartTrigger + 1
            )
        }
        speakCurrentPrompt()
    }

    fun toggleObserveLoop() {
        _uiState.update { it.copy(isAnimationLooping = !it.isAnimationLooping) }
    }

    fun setObserveSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepository.setAnimationSpeed(speed)
        }
    }

    fun advanceFromObserveToTrace() {
        _uiState.update {
            it.copy(
                stepType = LessonStepType.STEP_2_TRACE,
                singleCharStrokes = emptyList(),
                lastSingleEvaluation = null,
                showingCorrectionAnimation = false,
                mustRewriteAfterError = false,
                feedbackMessage = null,
                isFeedbackError = false
            )
        }
    }

    // --- Thao tác vẽ trên Canvas ---
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

            // Tự chuyển sang ô cần viết kế tiếp sau khoảng dừng (mặc định 700ms) nếu đã vẽ đủ số nét của ô hiện tại
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
                // Nếu ô hiện tại đang trống, lùi về ô cần viết liền trước đó
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
                        "Hãy tự viết lại đúng 1 lần trước khi đi tiếp."
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
                    feedbackMessage = "Hãy tự viết lại từ này đúng 1 lần trước khi đi tiếp.",
                    isFeedbackError = false
                )
            }
        }
    }

    fun revealListenRomajiFallback() {
        _uiState.update { it.copy(showFallbackRomajiInListenMode = true) }
    }

    /**
     * Bấm nút "Kiểm tra": chấm điểm toàn bộ chữ hoặc toàn bộ từ trên dãy ô vuông.
     * @return true nếu bài làm ĐẠT, false nếu SAI (để UI kích hoạt rung Haptic).
     */
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
            recordCharacterPracticeInDb(target, result.score, passed = true)
            when (state.stepType) {
                LessonStepType.STEP_2_TRACE -> {
                    _uiState.update {
                        it.copy(
                            stepType = LessonStepType.STEP_3_GUIDED,
                            singleCharStrokes = emptyList(),
                            lastSingleEvaluation = null,
                            showingCorrectionAnimation = false,
                            mustRewriteAfterError = false,
                            feedbackMessage = "Đồ nét chính xác (${result.score}đ)! Sang Bước 3: Viết có gợi ý.",
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
                    // Đánh dấu chữ này đã học xong để Bước 5 mở khoá ô viết của chữ này trong từ vựng
                    learnedCharsSet.add(target.char)
                    startStep5VocabForCurrentCharacter(wordIdx = 0)
                }

                else -> {}
            }
            return true
        } else {
            recordCharacterPracticeInDb(target, result.score, passed = false)
            val errReason = result.errors.joinToString(", ") { it.messageVi }
                .ifEmpty { "Chưa khớp nét chuẩn" }
            val requireRewrite = (state.stepType == LessonStepType.STEP_4_RECALL)
            _uiState.update {
                it.copy(
                    lastSingleEvaluation = result,
                    showingCorrectionAnimation = true,
                    correctionTemplate = target,
                    mustRewriteAfterError = requireRewrite,
                    feedbackMessage = "Chưa chính xác ($errReason). Hãy quan sát lại thứ tự nét đúng rồi viết lại!",
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
            // Cả từ đều ĐẠT!
            val nextWordIdx = state.currentWordIndex + 1
            if (nextWordIdx < state.currentCharWords.size) {
                startStep5VocabForCurrentCharacter(nextWordIdx)
            } else {
                advanceToNextCharacterOrQuiz()
            }
            return true
        } else {
            // Có ít nhất 1 ô viết sai -> hiện đáp án + animate nét chữ sai, bắt viết lại 1 lần
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
                    feedbackMessage = "Ô số ${firstFailedSlot.index + 1} ('${firstFailedSlot.targetChar}'): $errReason. Hãy xem lại nét chuẩn rồi viết lại cả từ 1 lần!",
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
                        isCharFastTrackMode = false,
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
            startEndOfRowQuiz(isTestOut = false)
        }
    }

    private fun startEndOfRowQuiz(isTestOut: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val currentRowChars = state.rowCharacters
            // Lấy thêm chữ đã học ở các hàng trước để trộn
            val previousLearnedTemplates = learnedCharsSet
                .mapNotNull { allTemplatesByChar[it] }
                .filter { it.script == script && it.row != rowName }

            val poolChars = (currentRowChars + previousLearnedTemplates.take(5)).distinctBy { it.char }
            val poolWords = vocabRepository.getWordsForCharacters(poolChars.map { it.char })

            val questions = ArrayList<QuizQuestion>()
            if (isTestOut) {
                // Chế độ Kiểm tra vượt cấp (Đã biết): Kiểm tra toàn bộ chữ cái và từ vựng của hàng này
                for (ch in currentRowChars) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.ROMAJI_TO_KANA,
                            targetTemplate = ch
                        )
                    )
                    val wordForChar = poolWords.firstOrNull { it.introduces == ch.char }
                    if (wordForChar != null) {
                        questions.add(
                            QuizQuestion(
                                type = QuizQuestionType.MEANING_ROMAJI_TO_WORD,
                                targetWord = wordForChar
                            )
                        )
                    }
                }
            } else {
                // 1. Dạng (a) Romaji -> Viết Kana cho các chữ trong hàng hiện tại
                for (ch in currentRowChars) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.ROMAJI_TO_KANA,
                            targetTemplate = ch
                        )
                    )
                }
                // 2. Dạng (b) Nghe -> Viết Kana (trộn chữ hàng này và hàng trước)
                val listenChars = (currentRowChars.take(2) + previousLearnedTemplates.take(2))
                    .ifEmpty { currentRowChars.take(2) }
                for (ch in listenChars) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.LISTEN_TO_KANA,
                            targetTemplate = ch
                        )
                    )
                }
                // 3. Dạng (c) Nghĩa tiếng Việt + Romaji -> Viết cả từ
                val meaningWords = poolWords.filter { w -> w.introduces in currentRowChars.map { it.char } }.take(2)
                for (w in meaningWords) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.MEANING_ROMAJI_TO_WORD,
                            targetWord = w
                        )
                    )
                }
                // 4. Dạng (d) Nghe -> Viết cả từ
                val listenWords = poolWords.reversed().take(2)
                for (w in listenWords) {
                    questions.add(
                        QuizQuestion(
                            type = QuizQuestionType.LISTEN_TO_WORD,
                            targetWord = w
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
                        "Chế độ Kiểm tra vượt bài: Viết đúng hết (${questions.size} câu) để pass và mở khoá bài mới!"
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

            QuizQuestionType.ROMAJI_TO_KANA,
            QuizQuestionType.MEANING_ROMAJI_TO_WORD -> {
                // Không bắt buộc tự phát âm ở dạng nhìn đề chữ, người dùng có thể bấm nút Nghe nếu muốn
            }
        }
    }

    private fun handleQuizSingleCharResult(
        target: KanaStrokeTemplate,
        result: RecognitionResult
    ): Boolean {
        if (result.passed) {
            recordCharacterPracticeInDb(target, result.score, passed = !hadErrorOnCurrentQuizItem)
            advanceToNextQuizQuestion()
            return true
        } else {
            hadErrorOnCurrentQuizItem = true
            recordCharacterPracticeInDb(target, result.score, passed = false)
            val errReason = result.errors.joinToString(", ") { it.messageVi }
                .ifEmpty { "Chưa đúng nét" }
            _uiState.update {
                it.copy(
                    lastSingleEvaluation = result,
                    showingCorrectionAnimation = true,
                    correctionTemplate = target,
                    mustRewriteAfterError = true,
                    feedbackMessage = "Sai đáp án ($errReason). Đáp án đúng là '${target.char}'. Hãy xem lại nét và viết lại 1 lần!",
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
                    feedbackMessage = "Chưa chính xác ở chữ '${firstFailedSlot.targetChar}' (từ đúng: $wordText). Hãy xem lại nét mẫu rồi viết lại 1 lần!",
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
            // Nếu đang ở chế độ Kiểm tra vượt cấp (isRowTestOutMode): bắt buộc đúng hết 100% mới được Pass và mở khoá
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

    private fun recordCharacterPracticeInDb(
        template: KanaStrokeTemplate,
        score: Int,
        passed: Boolean
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = kanaDao.getCharacterProgress(template.char)
            val updated = CharacterProgressEntity(
                char = template.char,
                script = template.script,
                rowName = template.row,
                learned = existing?.learned == true || passed,
                lastScore = score,
                practiceCount = (existing?.practiceCount ?: 0) + 1,
                errorCount = (existing?.errorCount ?: 0) + if (passed) 0 else 1,
                updatedAtEpochMs = now
            )
            kanaDao.upsertCharacterProgress(updated)
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

            // Khởi tạo / cập nhật thẻ SRS cho toàn bộ chữ và từ vựng trong hàng vừa học
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
