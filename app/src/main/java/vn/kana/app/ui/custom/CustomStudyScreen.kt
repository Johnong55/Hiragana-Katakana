package vn.kana.app.ui.custom

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.kana.app.domain.KanaVocabWord
import vn.kana.app.domain.VoiceRecorderHelper
import vn.kana.app.ui.components.GenkoYoshiWordBar
import vn.kana.app.ui.components.KanaDrawingCanvas
import vn.kana.app.ui.components.WordCharSlot
import vn.kana.app.ui.map.CurriculumTab
import vn.kana.app.ui.map.LessonMapViewModel
import vn.kana.recognizer.KanaRecognizer
import vn.kana.recognizer.RecognizerConfig
import vn.kana.recognizer.Stroke

/**
 * Màn hình Tự chọn hàng/chữ bằng Checkbox [☑]:
 * - Cho phép người dùng tick chọn bất kỳ hàng Gojūon hoặc từng chữ cái cụ thể muốn học.
 * - Hiển thị toàn bộ Từ vựng liên quan + Câu mẫu tiếng Nhật chứa từ đó.
 * - Luyện phát âm (Nghe mẫu từ, Nghe mẫu câu) + Ghi âm giọng đọc của người dùng để tự nghe lại / so sánh.
 * - Chuyển sang Luyện viết trên Canvas 1:1 + dãy ô vuông 原稿用紙 cho các từ thuộc hàng đã tick.
 */
@Composable
fun CustomStudyScreen(
    viewModel: LessonMapViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val localView = LocalView.current
    val scope = rememberCoroutineScope()

    val voiceRecorder = remember { VoiceRecorderHelper(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.release()
        }
    }

    val isRecording by voiceRecorder.isRecording.collectAsState()
    val isPlayingRec by voiceRecorder.isPlayingRecording.collectAsState()
    val recordedKeys by voiceRecorder.recordedKeys.collectAsState()

    var activeRecTargetKey by remember { mutableStateOf<String?>(null) }
    var micStatusMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && activeRecTargetKey != null) {
            val ok = voiceRecorder.startRecording(activeRecTargetKey!!)
            micStatusMessage = if (ok) "Đang ghi âm... Hãy đọc to từ/câu rồi bấm Dừng." else "Không thể mở micro trên thiết bị này."
        } else if (!granted) {
            micStatusMessage = "Bạn chưa cấp quyền Micro. Bạn vẫn nghe phát âm mẫu và luyện viết bình thường."
        }
    }

    fun toggleRecordForKey(key: String) {
        if (isRecording && activeRecTargetKey == key) {
            voiceRecorder.stopRecording()
            micStatusMessage = "Đã lưu bản ghi âm. Bấm '▶ Giọng bạn' để nghe lại!"
            return
        }
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        activeRecTargetKey = key
        if (hasPerm) {
            val ok = voiceRecorder.startRecording(key)
            micStatusMessage = if (ok) "Đang ghi âm... Hãy đọc to từ/câu rồi bấm Dừng." else "Thiết bị không hỗ trợ ghi âm."
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Tập hợp các chữ Kana được tick chọn (mặc định tick sẵn hàng あ行)
    var selectedChars by remember(uiState.basicHiragana) {
        val defaultChars = uiState.basicHiragana
            .filter { it.row == "あ行" }
            .map { it.char }
            .toSet()
        mutableStateOf(defaultChars)
    }

    // Danh sách từ vựng liên quan tới các chữ đã tick
    val allWords = remember(uiState.wordsByChar) {
        uiState.wordsByChar.values.flatten().distinctBy { it.id.ifBlank { it.word } }
    }
    val matchingWords = remember(selectedChars, allWords) {
        allWords.filter { w ->
            w.introduces in selectedChars || w.chars.any { c -> c in selectedChars }
        }
    }

    // Chế độ luyện viết các từ đã chọn
    var isWritingPracticeMode by remember { mutableStateOf(false) }
    var writingWordIndex by remember { mutableIntStateOf(0) }

    val recognizer = remember(uiState.allCurriculumCharacters) {
        if (uiState.allCurriculumCharacters.isNotEmpty()) {
            KanaRecognizer(uiState.allCurriculumCharacters)
        } else {
            null
        }
    }
    val templatesByChar = remember(uiState.allCurriculumCharacters) {
        uiState.allCurriculumCharacters.associateBy { it.char }
    }

    if (isWritingPracticeMode && matchingWords.isNotEmpty()) {
        val safeIdx = writingWordIndex.coerceIn(0, matchingWords.lastIndex)
        val currentWord = matchingWords[safeIdx]
        CustomWordWritingPracticeView(
            word = currentWord,
            wordPositionLabel = "${safeIdx + 1}/${matchingWords.size}",
            templatesByChar = templatesByChar,
            recognizer = recognizer,
            strictnessLevel = uiState.settings.strictnessLevel,
            autoAdvanceDelayMs = uiState.settings.autoAdvanceDelayMs,
            isRecording = isRecording && activeRecTargetKey == currentWord.word,
            hasRecording = recordedKeys.contains(currentWord.word),
            isPlayingRecording = isPlayingRec,
            micStatusMessage = micStatusMessage,
            onSpeakWord = { viewModel.speak(currentWord.word, currentWord.romaji) },
            onSpeakSentence = {
                if (currentWord.exampleSentence.isNotBlank()) {
                    viewModel.speak(currentWord.exampleSentence, null)
                }
            },
            onToggleRecord = { toggleRecordForKey(currentWord.word) },
            onPlayUserRecording = { voiceRecorder.playRecording(currentWord.word) },
            onCompareAudio = {
                viewModel.speak(
                    if (currentWord.exampleSentence.isNotBlank()) currentWord.exampleSentence else currentWord.word,
                    null
                )
                scope.launch {
                    delay(1600L)
                    voiceRecorder.playRecording(currentWord.word)
                }
            },
            onPrevWord = {
                if (safeIdx > 0) writingWordIndex = safeIdx - 1
            },
            onNextWord = {
                if (safeIdx < matchingWords.lastIndex) {
                    writingWordIndex = safeIdx + 1
                } else {
                    isWritingPracticeMode = false
                }
            },
            onExitWriting = { isWritingPracticeMode = false },
            localView = localView,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "← Quay lại",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = "CHỌN HÀNG • TỪ VỰNG & PHÁT ÂM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 2. Thanh chọn hệ chữ: Hiragana | Katakana | Dakuten • 拗音
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (tab in CurriculumTab.entries) {
                val selected = uiState.selectedTab == tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable { viewModel.selectTab(tab) }
                        .padding(vertical = 7.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = tab.titleVi,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 3. Khung Checkbox chọn Hàng (và từng chữ cái trong hàng)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tick [☑] hàng hoặc chữ muốn học (${selectedChars.size} chữ → ${matchingWords.size} từ):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Chọn hết",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable {
                                val tabChars = uiState.filteredLessons
                                    .flatMap { it.characters.map { c -> c.char } }
                                selectedChars = selectedChars + tabChars
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Bỏ chọn",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable { selectedChars = emptySet() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Thanh cuộn ngang các Checkbox Hàng
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (lesson in uiState.filteredLessons) {
                    val rowCharSet = lesson.characters.map { it.char }.toSet()
                    val isRowChecked = rowCharSet.isNotEmpty() && selectedChars.containsAll(rowCharSet)
                    val isPartial = !isRowChecked && rowCharSet.any { it in selectedChars }

                    val checkSymbol = when {
                        isRowChecked -> "☑"
                        isPartial -> "▣"
                        else -> "☐"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isRowChecked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable {
                                selectedChars = if (isRowChecked) {
                                    selectedChars - rowCharSet
                                } else {
                                    selectedChars + rowCharSet
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$checkSymbol ${lesson.rowName} (${lesson.characters.joinToString("") { it.char }})",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isRowChecked) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Thanh cuộn ngang tick lẻ từng chữ trong tab hiện tại
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val charsInTab = uiState.filteredLessons.flatMap { it.characters }
                for (tmpl in charsInTab) {
                    val checked = tmpl.char in selectedChars
                    Text(
                        text = "${if (checked) "☑" else "☐"}${tmpl.char}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (checked) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .background(
                                if (checked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .clickable {
                                selectedChars = if (checked) {
                                    selectedChars - tmpl.char
                                } else {
                                    selectedChars + tmpl.char
                                }
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
        }

        micStatusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Nút chuyển sang Luyện viết toàn bộ các từ đã tick
        if (matchingWords.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable {
                        writingWordIndex = 0
                        isWritingPracticeMode = true
                    }
                    .padding(vertical = 10.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = "✎ Luyện viết & phát âm chuỗi ${matchingWords.size} từ đã chọn →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1
                )
            }
        }

        // 4. Danh sách Từ vựng + Câu mẫu + Nút Phát âm & Ghi âm
        if (matchingWords.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Hãy tick [☑] ít nhất 1 hàng hoặc 1 chữ cái ở trên để xem từ vựng, câu mẫu và luyện phát âm.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(matchingWords, key = { _, w -> w.id.ifBlank { w.word } }) { idx, word ->
                    val recKey = word.word
                    val isThisRecording = isRecording && activeRecTargetKey == recKey
                    val hasRec = recordedKeys.contains(recKey)

                    VocabSentencePronunciationCard(
                        word = word,
                        isRecording = isThisRecording,
                        hasRecording = hasRec,
                        isPlayingRecording = isPlayingRec,
                        onSpeakWord = { viewModel.speak(word.word, word.romaji) },
                        onSpeakSentence = {
                            if (word.exampleSentence.isNotBlank()) {
                                viewModel.speak(word.exampleSentence, null)
                            }
                        },
                        onToggleRecord = { toggleRecordForKey(recKey) },
                        onPlayUserRecording = { voiceRecorder.playRecording(recKey) },
                        onCompareAudio = {
                            viewModel.speak(
                                if (word.exampleSentence.isNotBlank()) word.exampleSentence else word.word,
                                null
                            )
                            scope.launch {
                                delay(1600L)
                                voiceRecorder.playRecording(recKey)
                            }
                        },
                        onPracticeWriteThisWord = {
                            writingWordIndex = idx
                            isWritingPracticeMode = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VocabSentencePronunciationCard(
    word: KanaVocabWord,
    isRecording: Boolean,
    hasRecording: Boolean,
    isPlayingRecording: Boolean,
    onSpeakWord: () -> Unit,
    onSpeakSentence: () -> Unit,
    onToggleRecord: () -> Unit,
    onPlayUserRecording: () -> Unit,
    onCompareAudio: () -> Unit,
    onPracticeWriteThisWord: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dòng 1: Từ vựng + Romaji + Nghĩa tiếng Việt + Nghe từ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${word.word} (${word.romaji})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Nghĩa: ${word.meaningVi} • Chữ trọng tâm: ${word.introduces}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallBorderButton(
                    label = "🔊 Nghe từ",
                    filled = true,
                    onClick = onSpeakWord
                )
                SmallBorderButton(
                    label = "✎ Viết",
                    filled = false,
                    onClick = onPracticeWriteThisWord
                )
            }
        }

        // Dòng 2: Câu mẫu chứa từ vựng đó
        if (word.exampleSentence.isNotBlank()) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Câu mẫu: ${word.exampleSentence}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = word.exampleRomaji,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = word.exampleVi,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                SmallBorderButton(
                    label = "🔊 Nghe câu",
                    filled = false,
                    onClick = onSpeakSentence
                )
            }
        }

        // Dòng 3: Cụm nút Luyện phát âm & Ghi âm đối chiếu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallBorderButton(
                label = if (isRecording) "⏹ Dừng ghi" else "🎙 Ghi âm thử",
                filled = isRecording,
                isAlert = isRecording,
                onClick = onToggleRecord,
                modifier = Modifier.weight(1f)
            )
            if (hasRecording) {
                SmallBorderButton(
                    label = if (isPlayingRecording) "Đang phát..." else "▶ Giọng bạn",
                    filled = false,
                    onClick = onPlayUserRecording,
                    modifier = Modifier.weight(1f)
                )
                SmallBorderButton(
                    label = "⇄ Mẫu + Bạn",
                    filled = false,
                    onClick = onCompareAudio,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CustomWordWritingPracticeView(
    word: KanaVocabWord,
    wordPositionLabel: String,
    templatesByChar: Map<String, vn.kana.recognizer.KanaStrokeTemplate>,
    recognizer: KanaRecognizer?,
    strictnessLevel: vn.kana.recognizer.StrictnessLevel,
    autoAdvanceDelayMs: Long,
    isRecording: Boolean,
    hasRecording: Boolean,
    isPlayingRecording: Boolean,
    micStatusMessage: String?,
    onSpeakWord: () -> Unit,
    onSpeakSentence: () -> Unit,
    onToggleRecord: () -> Unit,
    onPlayUserRecording: () -> Unit,
    onCompareAudio: () -> Unit,
    onPrevWord: () -> Unit,
    onNextWord: () -> Unit,
    onExitWriting: () -> Unit,
    localView: android.view.View,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var autoAdvanceJob by remember { mutableStateOf<Job?>(null) }

    var slots by remember(word.id, word.word) {
        mutableStateOf(
            word.chars.mapIndexed { idx, ch ->
                val tmpl = templatesByChar[ch]
                WordCharSlot(
                    index = idx,
                    targetChar = ch,
                    expectedStrokeCount = tmpl?.strokeCount ?: 1,
                    templateStrokes = tmpl?.toTypedStrokes().orEmpty(),
                    isPreFilledUnlearned = false,
                    userStrokes = emptyList(),
                    evaluation = null
                )
            }
        )
    }
    var activeSlotIndex by remember(word.id, word.word) { mutableIntStateOf(0) }
    var feedbackText by remember(word.id, word.word) { mutableStateOf<String?>(null) }
    var isFeedbackError by remember(word.id, word.word) { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallBorderButton(label = "← Danh sách từ", filled = false, onClick = onExitWriting)
            Text(
                text = "LUYỆN TỪ & CÂU ($wordPositionLabel)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallBorderButton(label = "‹ Trước", filled = false, onClick = onPrevWord)
                SmallBorderButton(label = "Sau ›", filled = false, onClick = onNextWord)
            }
        }

        // Khung Từ vựng + Câu mẫu + Phát âm / Ghi âm
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${word.meaningVi} • ${word.romaji}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (word.exampleSentence.isNotBlank()) {
                        Text(
                            text = "Câu: ${word.exampleSentence} (${word.exampleVi})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallBorderButton(label = "🔊 Từ", filled = true, onClick = onSpeakWord)
                    if (word.exampleSentence.isNotBlank()) {
                        SmallBorderButton(label = "🔊 Câu", filled = false, onClick = onSpeakSentence)
                    }
                }
            }

            // Hàng nút Ghi âm phát âm ngay trong lúc luyện viết
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallBorderButton(
                    label = if (isRecording) "⏹ Dừng ghi" else "🎙 Ghi âm phát âm",
                    filled = isRecording,
                    isAlert = isRecording,
                    onClick = onToggleRecord,
                    modifier = Modifier.weight(1f)
                )
                if (hasRecording) {
                    SmallBorderButton(
                        label = if (isPlayingRecording) "Đang phát..." else "▶ Giọng bạn",
                        filled = false,
                        onClick = onPlayUserRecording,
                        modifier = Modifier.weight(1f)
                    )
                    SmallBorderButton(
                        label = "⇄ Mẫu + Bạn",
                        filled = false,
                        onClick = onCompareAudio,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            GenkoYoshiWordBar(
                slots = slots,
                activeSlotIndex = activeSlotIndex,
                onSelectSlot = { idx ->
                    autoAdvanceJob?.cancel()
                    activeSlotIndex = idx
                }
            )
        }

        val statusMsg = feedbackText ?: micStatusMessage
        if (statusMsg != null) {
            val col = if (isFeedbackError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            Text(
                text = statusMsg,
                style = MaterialTheme.typography.labelMedium,
                color = col,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, col)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Ô vuông Canvas 1:1 cố định (không cuộn dọc)
        val activeSlot = slots.getOrNull(activeSlotIndex)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            KanaDrawingCanvas(
                userStrokes = activeSlot?.userStrokes.orEmpty(),
                onStrokeCompleted = { stroke ->
                    autoAdvanceJob?.cancel()
                    val cur = slots.getOrNull(activeSlotIndex) ?: return@KanaDrawingCanvas
                    val updatedStrokes = cur.userStrokes + stroke
                    val nextSlots = slots.mapIndexed { idx, s ->
                        if (idx == activeSlotIndex) s.copy(userStrokes = updatedStrokes, evaluation = null)
                        else s
                    }
                    slots = nextSlots
                    feedbackText = null

                    if (updatedStrokes.size >= cur.expectedStrokeCount) {
                        val nextEmpty = nextSlots.indexOfFirst { it.index > activeSlotIndex && it.userStrokes.isEmpty() }
                        if (nextEmpty != -1) {
                            autoAdvanceJob = scope.launch {
                                delay(autoAdvanceDelayMs)
                                activeSlotIndex = nextEmpty
                            }
                        }
                    }
                },
                onStrokeActivity = { autoAdvanceJob?.cancel() },
                templateStrokes = activeSlot?.templateStrokes.orEmpty(),
                showGhostTemplate = false,
                shakeTrigger = shakeTrigger,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3 nút điều khiển Canvas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallBorderButton(
                label = "Hoàn tác nét",
                filled = false,
                onClick = {
                    autoAdvanceJob?.cancel()
                    val cur = slots.getOrNull(activeSlotIndex) ?: return@SmallBorderButton
                    if (cur.userStrokes.isNotEmpty()) {
                        slots = slots.mapIndexed { idx, s ->
                            if (idx == activeSlotIndex) s.copy(userStrokes = s.userStrokes.dropLast(1), evaluation = null)
                            else s
                        }
                    } else if (activeSlotIndex > 0) {
                        activeSlotIndex -= 1
                    }
                },
                modifier = Modifier.weight(1f)
            )
            SmallBorderButton(
                label = "Xoá hết",
                filled = false,
                onClick = {
                    autoAdvanceJob?.cancel()
                    slots = slots.map { it.copy(userStrokes = emptyList(), evaluation = null) }
                    activeSlotIndex = 0
                    feedbackText = null
                },
                modifier = Modifier.weight(0.75f)
            )
            SmallBorderButton(
                label = "Kiểm tra",
                filled = true,
                onClick = {
                    autoAdvanceJob?.cancel()
                    val engine = recognizer ?: return@SmallBorderButton
                    val cfg = RecognizerConfig.forLevel(strictnessLevel)
                    val evaluated = slots.map { s ->
                        s.copy(evaluation = engine.recognize(s.userStrokes, s.targetChar, cfg))
                    }
                    slots = evaluated
                    val failed = evaluated.firstOrNull { it.evaluation?.passed != true }
                    if (failed == null) {
                        isFeedbackError = false
                        feedbackText = "Chính xác! Bạn đã viết đúng từ '${word.word}' (${word.meaningVi})."
                        onSpeakWord()
                    } else {
                        isFeedbackError = true
                        activeSlotIndex = failed.index
                        shakeTrigger++
                        localView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                        val reason = failed.evaluation?.errors?.joinToString(", ") { it.messageVi }
                            .orEmpty()
                            .ifEmpty { "Chưa đúng nét" }
                        feedbackText = "Ô số ${failed.index + 1} ('${failed.targetChar}'): $reason."
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SmallBorderButton(
    label: String,
    filled: Boolean,
    isAlert: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isAlert -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val fgColor = when {
        isAlert -> MaterialTheme.colorScheme.onError
        filled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onBackground
    }
    val borderCol = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(bgColor)
            .border(1.dp, borderCol)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fgColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
