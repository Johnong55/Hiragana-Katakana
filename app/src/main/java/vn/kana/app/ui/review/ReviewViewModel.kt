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

enum class ReviewDeckType(val labelVi: String) {
    MISTAKES("Sổ chữ hay sai"),
    SRS_DUE("Đến hạn SRS"),
    SAVED_VOCAB("Từ đã lưu"),
    ALL_LEARNED("Ôn tổng hợp")
}

data class MistakeCharStat(
    val char: String,
    val romaji: String,
    val script: String,
    val mistakeCount: Int,
    val lastScore: Int
)

enum class ReviewDrillStyle(val labelVi: String) {
    WRITE("Viết nét"),
    PICK_CHAR("Chọn chữ"),
    PICK_READING("Chọn nghĩa / đọc")
}

enum class ReviewSessionFilter(val labelVi: String) {
    MIXED("Đa dạng"),
    WRITE_ONLY("Chỉ viết")
}

data class ReviewUiState(
    val isLoading: Boolean = true,
    val isInHub: Boolean = true,
    val activeDeck: ReviewDeckType = ReviewDeckType.MISTAKES,
    val mistakeChars: List<MistakeCharStat> = emptyList(),
    val dueSrsCount: Int = 0,
    val savedVocabCount: Int = 0,
    val learnedCharsCount: Int = 0,
    val settings: UserSettings = UserSettings(),
    val questions: List<QuizQuestion> = emptyList(),
    val drillStyles: List<ReviewDrillStyle> = emptyList(),
    val choiceOptionsByIndex: Map<Int, List<String>> = emptyMap(),
    val correctChoiceIndexByIndex: Map<Int, Int> = emptyMap(),
    val selectedChoiceIndex: Int? = null,
    val sessionFilter: ReviewSessionFilter = ReviewSessionFilter.MIXED,
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

    val currentDrillStyle: ReviewDrillStyle
        get() = if (sessionFilter == ReviewSessionFilter.WRITE_ONLY) {
            ReviewDrillStyle.WRITE
        } else {
            drillStyles.getOrElse(currentIndex) { ReviewDrillStyle.WRITE }
        }

    val currentChoiceOptions: List<String>
        get() = choiceOptionsByIndex[currentIndex].orEmpty()

    val currentCorrectChoiceIndex: Int
        get() = correctChoiceIndexByIndex[currentIndex] ?: 0

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
        loadHubStats()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                _uiState.update { it.copy(settings = s, savedVocabCount = s.savedVocabWords.size) }
            }
        }
    }

    fun loadHubStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isInHub = true, isSessionCompleted = false) }
            val allChars = strokeRepository.getAllCharacters()
            templatesByChar = allChars.associateBy { it.char }
            recognizer = KanaRecognizer(allChars)

            val settings = settingsRepository.settingsFlow.first()
            val now = System.currentTimeMillis()
            val allProgress = kanaDao.observeCharacterProgress().first()
            val learnedProgress = allProgress.filter { it.learned }
            val srsCards = kanaDao.observeSrsCards().first()
            val srsByKey = srsCards.filter { it.cardType == "CHAR" }.associateBy { it.targetKey }
            val progressByChar = allProgress.associateBy { it.char }

            val dueCount = srsCards.count { it.dueAtEpochMs <= now }

            // Tổng hợp danh sách chữ hay sai từ CharacterProgress (errorCount) + SRS (lapses)
            val mistakeStats = (progressByChar.keys + srsByKey.keys)
                .distinct()
                .mapNotNull { ch ->
                    val tmpl = templatesByChar[ch] ?: return@mapNotNull null
                    val prog = progressByChar[ch]
                    val card = srsByKey[ch]
                    val totalMistakes = (prog?.errorCount ?: 0) + (card?.lapses ?: 0)
                    if (totalMistakes <= 0) return@mapNotNull null
                    MistakeCharStat(
                        char = tmpl.char,
                        romaji = tmpl.romaji,
                        script = tmpl.script,
                        mistakeCount = totalMistakes,
                        lastScore = card?.lastScore ?: prog?.lastScore ?: 0
                    )
                }
                .sortedWith(
                    compareByDescending<MistakeCharStat> { it.mistakeCount }
                        .thenBy { it.lastScore }
                )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isInHub = true,
                    settings = settings,
                    mistakeChars = mistakeStats,
                    dueSrsCount = dueCount,
                    savedVocabCount = settings.savedVocabWords.size,
                    learnedCharsCount = learnedProgress.size
                )
            }
        }
    }

    fun returnToHub() {
        autoAdvanceJob?.cancel()
        loadHubStats()
    }

    fun buildNewReviewSession() {
        startDeck(_uiState.value.activeDeck)
    }

    fun startDeck(deck: ReviewDeckType) {
        viewModelScope.launch {
            autoAdvanceJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isInHub = false,
                    activeDeck = deck,
                    isSessionCompleted = false
                )
            }
            val allChars = strokeRepository.getAllCharacters()
            templatesByChar = allChars.associateBy { it.char }
            recognizer = KanaRecognizer(allChars)

            val allWords = vocabRepository.getAllWords()
            val settings = settingsRepository.settingsFlow.first()
            val now = System.currentTimeMillis()

            val allProgress = kanaDao.observeCharacterProgress().first()
            val learnedProgress = allProgress.filter { it.learned }
            val srsCards = kanaDao.observeSrsCards().first()
            val srsById = srsCards.associateBy { it.cardId }
            val srsByCharKey = srsCards.filter { it.cardType == "CHAR" }.associateBy { it.targetKey }
            val progressByChar = allProgress.associateBy { it.char }

            learnedCharsSet.clear()
            learnedProgress.forEach { learnedCharsSet.add(it.char) }

            if (learnedCharsSet.isEmpty()) {
                allChars.filter { it.script == "hiragana" && it.row == "あ行" }
                    .forEach { learnedCharsSet.add(it.char) }
            }

            val dueSrsCards = srsCards
                .filter { it.dueAtEpochMs <= now }
                .sortedWith(
                    compareByDescending<vn.kana.app.data.local.SrsCardEntity> { it.lapses }
                        .thenBy { it.lastScore }
                )
            val dueCardsCount = dueSrsCards.size
            val wordsByKey = allWords.associateBy { it.id.ifBlank { it.word } }
            val wordsByWord = allWords.associateBy { it.word }

            val questions = ArrayList<QuizQuestion>()

            when (deck) {
                ReviewDeckType.MISTAKES -> {
                    val weakTemplates = (progressByChar.keys + srsByCharKey.keys)
                        .distinct()
                        .mapNotNull { ch ->
                            val tmpl = templatesByChar[ch] ?: return@mapNotNull null
                            val mistakes = (progressByChar[ch]?.errorCount ?: 0) + (srsByCharKey[ch]?.lapses ?: 0)
                            if (mistakes > 0) tmpl to mistakes else null
                        }
                        .sortedByDescending { it.second }
                        .map { it.first }
                        .take(20)

                    for ((idx, tmpl) in weakTemplates.withIndex()) {
                        val singleType = if (idx % 2 == 0) {
                            QuizQuestionType.ROMAJI_TO_KANA
                        } else {
                            QuizQuestionType.LISTEN_TO_KANA
                        }
                        questions.add(QuizQuestion(type = singleType, targetTemplate = tmpl))
                    }
                }

                ReviewDeckType.SRS_DUE -> {
                    for ((idx, card) in dueSrsCards.withIndex()) {
                        if (card.cardType == "WORD") {
                            val w = wordsByKey[card.targetKey] ?: wordsByWord[card.targetKey]
                            if (w != null) {
                                val wordType = if (idx % 2 == 0) {
                                    QuizQuestionType.MEANING_ROMAJI_TO_WORD
                                } else {
                                    QuizQuestionType.LISTEN_TO_WORD
                                }
                                questions.add(QuizQuestion(type = wordType, targetWord = w))
                                continue
                            }
                        }
                        val tmpl = templatesByChar[card.targetKey]
                        if (tmpl != null) {
                            val singleType = if (idx % 2 == 0) {
                                QuizQuestionType.ROMAJI_TO_KANA
                            } else {
                                QuizQuestionType.LISTEN_TO_KANA
                            }
                            questions.add(QuizQuestion(type = singleType, targetTemplate = tmpl))
                        }
                    }
                }

                ReviewDeckType.SAVED_VOCAB -> {
                    val savedWords = settings.savedVocabWords
                        .mapNotNull { wStr -> wordsByWord[wStr] ?: wordsByKey[wStr] }
                        .shuffled()
                    for ((idx, w) in savedWords.withIndex()) {
                        val wordType = if (idx % 2 == 0) {
                            QuizQuestionType.MEANING_ROMAJI_TO_WORD
                        } else {
                            QuizQuestionType.LISTEN_TO_WORD
                        }
                        questions.add(QuizQuestion(type = wordType, targetWord = w))
                    }
                }

                ReviewDeckType.ALL_LEARNED -> {
                    // Tạo gói 10 câu ôn tổng hợp nhanh từ chữ đã học (ưu tiên chữ hay sai/điểm thấp)
                    val candidateTemplates = learnedCharsSet
                        .mapNotNull { templatesByChar[it] }
                        .sortedWith(
                            compareByDescending<KanaStrokeTemplate> { tmpl ->
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
                    for ((idx, tmpl) in selectedChars.withIndex()) {
                        val singleType = if (idx % 2 == 0) {
                            QuizQuestionType.ROMAJI_TO_KANA
                        } else {
                            QuizQuestionType.LISTEN_TO_KANA
                        }
                        questions.add(QuizQuestion(type = singleType, targetTemplate = tmpl))

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
                    }
                }
            }

            if (questions.isEmpty()) {
                loadHubStats()
                return@launch
            }

            hadErrorOnCurrentItem = false
            val firstQ = questions.firstOrNull()
            val initialSlots = firstQ?.targetWord?.let { buildWordSlotsForReview(it) }.orEmpty()
            val firstWritable = initialSlots.indexOfFirst { !it.isPreFilledUnlearned }.coerceAtLeast(0)

            // Đa dạng hóa hình thức dò bài: xen kẽ Viết nét -> Chọn chữ -> Chọn nghĩa/đọc
            val drillStyles = questions.indices.map { qIdx ->
                when (qIdx % 3) {
                    0 -> ReviewDrillStyle.WRITE
                    1 -> ReviewDrillStyle.PICK_CHAR
                    else -> ReviewDrillStyle.PICK_READING
                }
            }
            val choiceOptionsByIndex = mutableMapOf<Int, List<String>>()
            val correctChoiceIndexByIndex = mutableMapOf<Int, Int>()

            questions.forEachIndexed { qIdx, q ->
                val style = drillStyles[qIdx]
                if (style != ReviewDrillStyle.WRITE) {
                    val tmpl = q.targetTemplate
                    val word = q.targetWord
                    if (tmpl != null) {
                        if (style == ReviewDrillStyle.PICK_CHAR) {
                            val pool = allChars.filter { it.script == tmpl.script && it.char != tmpl.char }
                                .ifEmpty { allChars.filter { it.char != tmpl.char } }
                            val distractors = pool.shuffled().take(3).map { it.char }
                            val options = (distractors + tmpl.char).shuffled()
                            choiceOptionsByIndex[qIdx] = options
                            correctChoiceIndexByIndex[qIdx] = options.indexOf(tmpl.char)
                        } else {
                            val pool = allChars.filter { it.script == tmpl.script && it.romaji != tmpl.romaji }
                                .distinctBy { it.romaji }
                                .ifEmpty { allChars.filter { it.romaji != tmpl.romaji }.distinctBy { it.romaji } }
                            val distractors = pool.shuffled().take(3).map { it.romaji }
                            val options = (distractors + tmpl.romaji).shuffled()
                            choiceOptionsByIndex[qIdx] = options
                            correctChoiceIndexByIndex[qIdx] = options.indexOf(tmpl.romaji)
                        }
                    } else if (word != null) {
                        if (style == ReviewDrillStyle.PICK_CHAR) {
                            val distractors = allWords.filter { it.word != word.word }
                                .distinctBy { it.word }
                                .shuffled()
                                .take(3)
                                .map { it.word }
                            val options = (distractors + word.word).shuffled()
                            choiceOptionsByIndex[qIdx] = options
                            correctChoiceIndexByIndex[qIdx] = options.indexOf(word.word)
                        } else {
                            val distractors = allWords.filter { it.meaningVi != word.meaningVi }
                                .distinctBy { it.meaningVi }
                                .shuffled()
                                .take(3)
                                .map { it.meaningVi }
                            val options = (distractors + word.meaningVi).shuffled()
                            choiceOptionsByIndex[qIdx] = options
                            correctChoiceIndexByIndex[qIdx] = options.indexOf(word.meaningVi)
                        }
                    }
                }
            }

            _uiState.update { prev ->
                ReviewUiState(
                    isLoading = false,
                    isInHub = false,
                    activeDeck = deck,
                    mistakeChars = prev.mistakeChars,
                    dueSrsCount = dueCardsCount,
                    savedVocabCount = settings.savedVocabWords.size,
                    learnedCharsCount = learnedProgress.size,
                    settings = settings,
                    questions = questions,
                    drillStyles = drillStyles,
                    choiceOptionsByIndex = choiceOptionsByIndex,
                    correctChoiceIndexByIndex = correctChoiceIndexByIndex,
                    sessionFilter = prev.sessionFilter,
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
                delay(_uiState.value.settings.autoAdvanceDelayMs.coerceAtLeast(1100L))
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
                    feedbackMessage = "ối zồi ôi làm được không đấy ($errReason). Đáp án: '${target.char}'. Xem lại nét đúng rồi viết lại 1 lần!",
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
                delay(_uiState.value.settings.autoAdvanceDelayMs.coerceAtLeast(1100L))
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
                    feedbackMessage = "ối zồi ôi làm được không đấy — Ô số ${firstFailed.index + 1} ('${firstFailed.targetChar}'): $errReason. Đáp án từ: ${word.word}.",
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
                    selectedChoiceIndex = null,
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

    fun toggleSessionFilter() {
        _uiState.update { state ->
            val next = if (state.sessionFilter == ReviewSessionFilter.MIXED) {
                ReviewSessionFilter.WRITE_ONLY
            } else {
                ReviewSessionFilter.MIXED
            }
            state.copy(sessionFilter = next, selectedChoiceIndex = null, feedbackMessage = null)
        }
    }

    fun submitChoiceAnswer(optionIndex: Int) {
        val state = _uiState.value
        if (state.selectedChoiceIndex != null) return
        val q = state.currentQuestion ?: return
        val isCorrect = optionIndex == state.currentCorrectChoiceIndex
        val score = if (isCorrect) 95 else 50

        q.targetTemplate?.let {
            updateSrsForChar(it, score = score, passedFirstAttempt = isCorrect)
            audioProvider.speak(it.char, it.romaji)
        }
        q.targetWord?.let {
            updateSrsForWord(it, score = score, passedFirstAttempt = isCorrect)
            audioProvider.speak(it.word, it.romaji)
        }

        _uiState.update {
            it.copy(
                selectedChoiceIndex = optionIndex,
                feedbackMessage = if (isCorrect) "✓ Chính xác!" else "✗ Chưa đúng — xem đáp án đúng bên trên.",
                isFeedbackError = !isCorrect
            )
        }

        if (isCorrect) {
            autoAdvanceJob?.cancel()
            autoAdvanceJob = viewModelScope.launch {
                delay(state.settings.autoAdvanceDelayMs.coerceAtLeast(1100L))
                advanceToNextQuestion(score = 95, passedFirstTry = true)
            }
        }
    }

    fun continueAfterWrongChoice() {
        autoAdvanceJob?.cancel()
        advanceToNextQuestion(score = 50, passedFirstTry = false)
    }

    fun skipCurrentQuestion() {
        autoAdvanceJob?.cancel()
        val q = _uiState.value.currentQuestion ?: return
        q.targetTemplate?.let { updateSrsForChar(it, score = 50, passedFirstAttempt = false) }
        q.targetWord?.let { updateSrsForWord(it, score = 50, passedFirstAttempt = false) }
        advanceToNextQuestion(score = 50, passedFirstTry = false)
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
