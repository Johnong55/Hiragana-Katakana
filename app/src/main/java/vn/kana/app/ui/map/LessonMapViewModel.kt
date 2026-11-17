package vn.kana.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kana.app.data.StrokeAssetRepository
import vn.kana.app.data.ThemeMode
import vn.kana.app.data.UserSettings
import vn.kana.app.data.UserSettingsRepository
import vn.kana.app.data.VocabAssetRepository
import vn.kana.app.data.local.CharacterProgressEntity
import vn.kana.app.data.local.KanaDao
import vn.kana.app.domain.AudioProvider
import vn.kana.app.domain.GojuonRowLesson
import vn.kana.app.domain.KanaVocabWord
import vn.kana.app.domain.MasteryLevel
import vn.kana.app.domain.SrsScheduler
import vn.kana.recognizer.KanaStrokeTemplate
import vn.kana.recognizer.StrictnessLevel

/**
 * 4 nhóm chương trình học chính:
 * 1. HIRAGANA_BASIC (46 chữ Hiragana cơ bản)
 * 2. KATAKANA_BASIC (46 chữ Katakana cơ bản)
 * 3. EXTENDED (Dakuten/Handakuten, Yōon, っ, ー)
 * 4. KANJI_N5 (80 chữ Kanji JLPT N5 cốt lõi)
 */
enum class CurriculumTab(val titleVi: String) {
    HIRAGANA_BASIC("Hiragana"),
    KATAKANA_BASIC("Katakana"),
    EXTENDED("Âm đục/ghép"),
    KANJI_N5("Kanji N5")
}

data class LessonMapUiState(
    val isLoading: Boolean = true,
    val selectedTab: CurriculumTab = CurriculumTab.HIRAGANA_BASIC,
    val allLessons: List<GojuonRowLesson> = emptyList(),
    val basicHiragana: List<KanaStrokeTemplate> = emptyList(),
    val basicKatakana: List<KanaStrokeTemplate> = emptyList(),
    val extendedCharacters: List<KanaStrokeTemplate> = emptyList(),
    val n5KanjiCharacters: List<KanaStrokeTemplate> = emptyList(),
    val allCurriculumCharacters: List<KanaStrokeTemplate> = emptyList(),
    val wordsByChar: Map<String, List<KanaVocabWord>> = emptyMap(),
    val characterProgressByChar: Map<String, CharacterProgressEntity> = emptyMap(),
    val lessonProgressByRowId: Map<String, Boolean> = emptyMap(),
    val masteryByChar: Map<String, MasteryLevel> = emptyMap(),
    val dueSrsCardsCount: Int = 0,
    val totalLearnedCount: Int = 0,
    val settings: UserSettings = UserSettings()
) {
    val filteredLessons: List<GojuonRowLesson>
        get() = allLessons.filter { lesson ->
            when (selectedTab) {
                CurriculumTab.HIRAGANA_BASIC ->
                    lesson.script == "hiragana" && lesson.characters.firstOrNull()?.category == "basic"

                CurriculumTab.KATAKANA_BASIC ->
                    lesson.script == "katakana" && lesson.characters.firstOrNull()?.category == "basic"

                CurriculumTab.EXTENDED ->
                    lesson.script != "kanji" && lesson.characters.firstOrNull()?.category != "basic"

                CurriculumTab.KANJI_N5 ->
                    lesson.script == "kanji"
            }
        }
}

class LessonMapViewModel(
    private val strokeRepository: StrokeAssetRepository,
    private val vocabRepository: VocabAssetRepository,
    private val settingsRepository: UserSettingsRepository,
    private val kanaDao: KanaDao,
    private val audioProvider: AudioProvider
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(CurriculumTab.HIRAGANA_BASIC)
    private val _uiState = MutableStateFlow(LessonMapUiState())
    val uiState: StateFlow<LessonMapUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    fun selectTab(tab: CurriculumTab) {
        _selectedTab.value = tab
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun observeData() {
        viewModelScope.launch {
            val allChars = strokeRepository.getAllCharacters()
            val basicHiragana = allChars.filter { it.script == "hiragana" && it.category == "basic" }
            val basicKatakana = allChars.filter { it.script == "katakana" && it.category == "basic" }
            // Các chữ mở rộng có trong chương trình học: dakuten/handakuten + ゃゅょっ / ャュョッー
            val learnedSmallChars = setOf("ゃ", "ゅ", "ょ", "っ", "ャ", "ュ", "ョ", "ッ", "ー")
            val extendedChars = allChars.filter {
                it.category == "dakuten" || (it.category == "small" && it.char in learnedSmallChars)
            }
            val kanjiN5Chars = allChars.filter { it.script == "kanji" }
            val curriculumChars = basicHiragana + basicKatakana + extendedChars + kanjiN5Chars

            val allWords = vocabRepository.getAllWords()
            val wordsByIntro = allWords.groupBy { it.introduces }

            val rowMeta = mapOf(
                // 1. Hiragana cơ bản
                ("hiragana" to "あ行") to ("Hiragana • Hàng A (あ行)" to "5 nguyên âm nền tảng: あ い う え お"),
                ("hiragana" to "か行") to ("Hiragana • Hàng KA (か行)" to "Phụ âm K: か き く け こ"),
                ("hiragana" to "さ行") to ("Hiragana • Hàng SA (さ行)" to "Phụ âm S: さ し す せ そ"),
                ("hiragana" to "た行") to ("Hiragana • Hàng TA (た行)" to "Phụ âm T: た ち つ て と"),
                ("hiragana" to "な行") to ("Hiragana • Hàng NA (な行)" to "Phụ âm N: な に ぬ ね の"),
                ("hiragana" to "は行") to ("Hiragana • Hàng HA (は行)" to "Phụ âm H: は ひ ふ へ ほ"),
                ("hiragana" to "ま行") to ("Hiragana • Hàng MA (ま行)" to "Phụ âm M: ま み む め も"),
                ("hiragana" to "や行") to ("Hiragana • Hàng YA (や行)" to "Bán nguyên âm Y: や ゆ よ"),
                ("hiragana" to "ら行") to ("Hiragana • Hàng RA (ら行)" to "Phụ âm R: ら り る れ ろ"),
                ("hiragana" to "わ行") to ("Hiragana • Hàng WA (わ行)" to "Hàng cuối & âm mũi: わ を ん"),
                // 2. Katakana cơ bản
                ("katakana" to "ア行") to ("Katakana • Hàng A (ア行)" to "5 nguyên âm Katakana: ア イ ウ エ オ"),
                ("katakana" to "カ行") to ("Katakana • Hàng KA (カ行)" to "Phụ âm K: カ キ ク ケ コ"),
                ("katakana" to "サ行") to ("Katakana • Hàng SA (サ行)" to "Phụ âm S: サ シ ス セ ソ"),
                ("katakana" to "タ行") to ("Katakana • Hàng TA (タ行)" to "Phụ âm T: タ チ ツ テ ト"),
                ("katakana" to "ナ行") to ("Katakana • Hàng NA (ナ行)" to "Phụ âm N: ナ ニ ヌ ネ ノ"),
                ("katakana" to "ハ行") to ("Katakana • Hàng HA (ハ行)" to "Phụ âm H: ハ ヒ フ ヘ ホ"),
                ("katakana" to "マ行") to ("Katakana • Hàng MA (マ行)" to "Phụ âm M: マ ミ ム メ モ"),
                ("katakana" to "ヤ行") to ("Katakana • Hàng YA (ヤ行)" to "Bán nguyên âm Y: ヤ ユ ヨ"),
                ("katakana" to "ラ行") to ("Katakana • Hàng RA (ラ行)" to "Phụ âm R: ラ リ ル レ ロ"),
                ("katakana" to "ワ行") to ("Katakana • Hàng WA (ワ行)" to "Hàng cuối & âm mũi: ワ ヲ ン"),
                // 3. Dakuten / Handakuten Hiragana
                ("hiragana" to "が行") to ("Hiragana Dakuten • Hàng GA (が行)" to "Âm đục G: が ぎ ぐ げ ご"),
                ("hiragana" to "ざ行") to ("Hiragana Dakuten • Hàng ZA (ざ行)" to "Âm đục Z: ざ じ ず ぜ ぞ"),
                ("hiragana" to "だ行") to ("Hiragana Dakuten • Hàng DA (だ行)" to "Âm đục D: だ ぢ づ で ど"),
                ("hiragana" to "ば行") to ("Hiragana Dakuten • Hàng BA (ば行)" to "Âm đục B: ば び ぶ べ ぼ"),
                ("hiragana" to "ぱ行") to ("Hiragana Handakuten • Hàng PA (ぱ行)" to "Âm nửa đục P: ぱ ぴ ぷ ぺ ぽ"),
                // 4. Dakuten / Handakuten Katakana
                ("katakana" to "ガ行") to ("Katakana Dakuten • Hàng GA (ガ行)" to "Âm đục G: ガ ギ グ ゲ ゴ"),
                ("katakana" to "ザ行") to ("Katakana Dakuten • Hàng ZA (ザ行)" to "Âm đục Z: ザ ジ ズ ゼ ゾ"),
                ("katakana" to "ダ行") to ("Katakana Dakuten • Hàng DA (ダ行)" to "Âm đục D: ダ ヂ ヅ デ ド"),
                ("katakana" to "バ行") to ("Katakana Dakuten • Hàng BA (バ行)" to "Âm đục B: バ ビ ブ ベ ボ"),
                ("katakana" to "パ行") to ("Katakana Handakuten • Hàng PA (パ行)" to "Âm nửa đục P: パ ピ プ ペ ポ"),
                // 5. Yōon, Sokuon, Chōonpu
                ("hiragana" to "拗音") to ("Hiragana • Âm ghép Yōon (拗音)" to "Chữ nhỏ ghép âm: ゃ ゅ ょ"),
                ("hiragana" to "促音") to ("Hiragana • Âm ngắt Sokuon (促音)" to "Chữ つ nhỏ gấp đôi phụ âm: っ"),
                ("katakana" to "拗音") to ("Katakana • Âm ghép Yōon (拗音)" to "Chữ nhỏ ghép âm: ャ ュ ョ"),
                ("katakana" to "促音") to ("Katakana • Âm ngắt Sokuon (促音)" to "Chữ ツ nhỏ gấp đôi phụ âm: ッ"),
                ("katakana" to "長音") to ("Katakana • Trường âm (長音)" to "Dấu kéo dài nguyên âm: ー"),
                // 6. Kanji JLPT N5 (Chặng 06 & Chặng 08)
                ("kanji" to "Kanji Số đếm") to ("Kanji N5 • Số đếm (1–10)" to "一 二 三 四 五 六 七 八 九 十"),
                ("kanji" to "Kanji Số lớn") to ("Kanji N5 • Số lớn & Tiền tệ" to "百 千 万 円"),
                ("kanji" to "Kanji Tự nhiên") to ("Kanji N5 • Tự nhiên & Thứ ngày" to "日 月 火 水 木 金 土 山 川 田"),
                ("kanji" to "Kanji Con người") to ("Kanji N5 • Con người & Gia đình" to "人 子 女 男 父 母 友"),
                ("kanji" to "Kanji Trường học") to ("Kanji N5 • Trường học & Ngôn ngữ" to "先 生 学 校 本 名 語"),
                ("kanji" to "Kanji Thời gian") to ("Kanji N5 • Thời gian" to "時 分 半 年 今"),
                ("kanji" to "Kanji Phương hướng") to ("Kanji N5 • Phương hướng & Vị trí" to "前 後 上 下 中 外 右 左 北 南 東 西"),
                ("kanji" to "Kanji Động từ N5") to ("Kanji N5 • Động từ cốt lõi" to "行 来 食 飲 見 聞 読 書 話 買 休 出 入"),
                ("kanji" to "Kanji Tính từ N5") to ("Kanji N5 • Tính từ & Màu sắc" to "大 小 高 安 新 古 多 少 白"),
                ("kanji" to "Kanji Đời sống N5") to ("Kanji N5 • Giao thông & Đời sống" to "車 電 駅")
            )

            val groupedByScriptAndRow = curriculumChars.groupBy { it.script to it.row }
            val orderedKeys = rowMeta.keys.filter { groupedByScriptAndRow.containsKey(it) }

            combine(
                kanaDao.observeLessonProgress(),
                kanaDao.observeCharacterProgress(),
                kanaDao.observeSrsCards(),
                settingsRepository.settingsFlow
            ) { lessonList, charList, srsCards, settings ->
                val now = System.currentTimeMillis()
                val lessonMap = lessonList.associateBy { it.rowId }
                val charMap = charList.associateBy { it.char }
                val srsByKey = srsCards.filter { it.cardType == "CHAR" }.associateBy { it.targetKey }

                val dueCount = srsCards.count { it.dueAtEpochMs <= now }

                val masteryMap = LinkedHashMap<String, MasteryLevel>()
                for (tmpl in curriculumChars) {
                    val prog = charMap[tmpl.char]
                    val card = srsByKey[tmpl.char]
                    masteryMap[tmpl.char] = SrsScheduler.resolveMasteryLevel(prog, card)
                }

                val lessons = orderedKeys.mapIndexed { index, (script, rowName) ->
                    val rowId = "${script}_$rowName"
                    val progress = lessonMap[rowId]
                    val rowChars = groupedByScriptAndRow[script to rowName].orEmpty()
                    val allCharsInRowLearned = rowChars.isNotEmpty() && rowChars.all { c ->
                        charMap[c.char]?.learned == true
                    }
                    val isRowDone = (progress?.completed == true) || allCharsInRowLearned

                    val prevCompleted = if (index == 0) {
                        true
                    } else {
                        val (prevScript, prevRow) = orderedKeys[index - 1]
                        val prevChars = groupedByScriptAndRow[prevScript to prevRow].orEmpty()
                        val prevAllLearned = prevChars.isNotEmpty() && prevChars.all { c ->
                            charMap[c.char]?.learned == true
                        }
                        lessonMap["${prevScript}_$prevRow"]?.completed == true || prevAllLearned
                    }
                    // Mở sẵn bài đầu tiên của mỗi nhóm chính (あ行, ア行, が行, 拗音) hoặc theo tiến trình
                    val isStageStarter = (script == "hiragana" && rowName == "あ行") ||
                        (script == "katakana" && rowName == "ア行") ||
                        (script == "hiragana" && rowName == "が行") ||
                        (script == "hiragana" && rowName == "拗音") ||
                        (script == "kanji" && rowName == "Kanji Số đếm") ||
                        (script == "kanji" && rowName == "Kanji Thời gian")
                    val isUnlocked = isStageStarter ||
                        prevCompleted ||
                        isRowDone ||
                        settings.unlockAllRows

                    val (titleVi, subVi) = rowMeta[script to rowName] ?: (rowName to "")
                    GojuonRowLesson(
                        rowId = rowId,
                        script = script,
                        rowName = rowName,
                        titleVi = titleVi,
                        subtitleVi = subVi,
                        characters = rowChars,
                        isUnlocked = isUnlocked,
                        isCompleted = isRowDone,
                        bestQuizScore = progress?.bestQuizScore ?: if (allCharsInRowLearned) 100 else 0
                    )
                }

                LessonMapUiState(
                    isLoading = false,
                    selectedTab = _selectedTab.value,
                    allLessons = lessons,
                    basicHiragana = basicHiragana,
                    basicKatakana = basicKatakana,
                    extendedCharacters = extendedChars,
                    n5KanjiCharacters = kanjiN5Chars,
                    allCurriculumCharacters = curriculumChars,
                    wordsByChar = wordsByIntro,
                    characterProgressByChar = charMap,
                    lessonProgressByRowId = lessonMap.mapValues { it.value.completed },
                    masteryByChar = masteryMap,
                    dueSrsCardsCount = dueCount,
                    totalLearnedCount = charList.count { it.learned },
                    settings = settings
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun speak(japaneseText: String, romajiKey: String? = null) {
        audioProvider.speak(japaneseText, romajiKey)
    }

    fun updateStrictness(level: StrictnessLevel) {
        viewModelScope.launch {
            settingsRepository.setStrictnessLevel(level)
        }
    }

    fun updateAnimationSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepository.setAnimationSpeed(speed)
        }
    }

    fun updateAutoAdvanceDelay(delayMs: Long) {
        viewModelScope.launch {
            settingsRepository.setAutoAdvanceDelayMs(delayMs)
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
        }
    }

    fun updateUnlockAllRows(unlockAll: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUnlockAllRows(unlockAll)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun toggleLightDarkQuick(currentlyDark: Boolean) {
        viewModelScope.launch {
            val nextMode = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
            settingsRepository.setThemeMode(nextMode)
        }
    }

    fun markSingleCharacterPassed(tmpl: KanaStrokeTemplate, score: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = kanaDao.getCharacterProgress(tmpl.char)
            kanaDao.upsertCharacterProgress(
                CharacterProgressEntity(
                    char = tmpl.char,
                    script = tmpl.script,
                    rowName = tmpl.row,
                    learned = true,
                    lastScore = score,
                    practiceCount = (existing?.practiceCount ?: 0) + 1,
                    errorCount = existing?.errorCount ?: 0,
                    updatedAtEpochMs = now
                )
            )
        }
    }

    /**
     * Đánh dấu hoàn thành một Chặng (Stage 01..08) trên Lộ trình N5 sau khi người dùng vượt qua bài Kiểm tra Mở khoá.
     */
    fun markRoadmapStagePassed(stageNumber: Int, charsToMarkLearned: List<KanaStrokeTemplate> = emptyList()) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            kanaDao.upsertLessonProgress(
                 vn.kana.app.data.local.LessonProgressEntity(
                    rowId = "roadmap_stage_$stageNumber",
                    script = "roadmap",
                    rowName = "Stage $stageNumber",
                    completed = true,
                    bestQuizScore = 100,
                    completedAtEpochMs = now
                )
            )
            for (tmpl in charsToMarkLearned) {
                kanaDao.upsertCharacterProgress(
                    CharacterProgressEntity(
                        char = tmpl.char,
                        script = tmpl.script,
                        rowName = tmpl.row,
                        learned = true,
                        lastScore = 90,
                        practiceCount = 1,
                        errorCount = 0,
                        updatedAtEpochMs = now
                    )
                )
            }
            settingsRepository.recordStudyActivityToday()
        }
    }

    fun clearAllProgress() {
        viewModelScope.launch {
            kanaDao.clearLessonProgress()
            kanaDao.clearCharacterProgress()
            kanaDao.clearSrsCards()
            settingsRepository.resetStreak()
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
                return LessonMapViewModel(
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
