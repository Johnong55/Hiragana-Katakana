package vn.kana.app.ui.custom

import android.Manifest
import android.content.pm.PackageManager
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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

private enum class CustomStudyStage {
    SELECT_ROWS,
    VOCAB_AND_PRONUNCIATION,
    WRITING_PRACTICE
}

/**
 * Tab "Chọn hàng":
 * Bước 1: Danh sách dọc các hàng có Checkbox [☑] rõ ràng, dễ bấm.
 * Bước 2: Sau khi tick các hàng muốn học -> bấm 1 nút dưới cùng để mở Từ vựng + Câu mẫu + Ghi âm phát âm.
 * Bước 3: Bấm "✎ Tập viết" nếu muốn viết từ vựng đó trên Canvas.
 */
@Composable
fun CustomStudyScreen(
    viewModel: LessonMapViewModel,
    onStartRowLesson: (rowName: String, script: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val localView = LocalView.current
    val scope = rememberCoroutineScope()

    val voiceRecorder = remember { VoiceRecorderHelper(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { voiceRecorder.release() }
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
            micStatusMessage = if (ok) "Đang ghi âm... Hãy đọc to rồi bấm Dừng." else "Thiết bị không hỗ trợ micro."
        } else if (!granted) {
            micStatusMessage = "Chưa cấp quyền Micro. Bạn vẫn nghe mẫu và tập viết bình thường."
        }
    }

    fun toggleRecordForKey(key: String) {
        if (isRecording && activeRecTargetKey == key) {
            voiceRecorder.stopRecording()
            micStatusMessage = "Đã lưu thu âm! Bấm '▶ Giọng bạn' để nghe lại."
            return
        }
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        activeRecTargetKey = key
        if (hasPerm) {
            val ok = voiceRecorder.startRecording(key)
            micStatusMessage = if (ok) "Đang ghi âm... Hãy đọc to rồi bấm Dừng." else "Không mở được micro."
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Tập hợp các rowId được tick chọn (mặc định tick sẵn "hiragana_あ行")
    var checkedRowIds by remember {
        mutableStateOf(setOf("hiragana_あ行"))
    }
    var stage by remember { mutableStateOf(CustomStudyStage.SELECT_ROWS) }
    var writingWordIndex by remember { mutableIntStateOf(0) }

    val selectedChars = remember(checkedRowIds, uiState.allLessons) {
        uiState.allLessons
            .filter { it.rowId in checkedRowIds }
            .flatMap { lesson -> lesson.characters.map { it.char } }
            .toSet()
    }

    val allWords = remember(uiState.wordsByChar) {
        uiState.wordsByChar.values.flatten().distinctBy { it.id.ifBlank { it.word } }
    }
    val matchingWords = remember(selectedChars, allWords) {
        allWords.filter { w -> w.introduces in selectedChars }
    }

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

    when (stage) {
        CustomStudyStage.SELECT_ROWS -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tiêu đề + Chọn nhanh
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHỌN HÀNG MUỐN HỌC",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallBtn(
                            label = "Chọn hết",
                            filled = false,
                            onClick = {
                                checkedRowIds = checkedRowIds + uiState.filteredLessons.map { it.rowId }
                            }
                        )
                        SmallBtn(
                            label = "Bỏ chọn",
                            filled = false,
                            onClick = { checkedRowIds = emptySet() }
                        )
                    }
                }

                // 3 tab hệ chữ
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
                                .padding(vertical = 8.dp, horizontal = 4.dp)
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

                // Danh sách dọc các hàng có Checkbox [☑] to rõ
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredLessons, key = { it.rowId }) { lesson ->
                        val isChecked = lesson.rowId in checkedRowIds
                        val charsPreview = lesson.characters.joinToString("  ") { it.char }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    if (isChecked) 2.dp else 1.dp,
                                    if (isChecked) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.outline
                                )
                                .clickable {
                                    checkedRowIds = if (isChecked) {
                                        checkedRowIds - lesson.rowId
                                    } else {
                                        checkedRowIds + lesson.rowId
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Ô Checkbox to rõ
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .background(
                                            if (isChecked) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isChecked) "✓" else " ",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isChecked) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Column {
                                    Text(
                                        text = "${lesson.rowName}  •  $charsPreview",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (lesson.isCompleted) "Đã thuộc hàng này" else lesson.titleVi,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            SmallBtn(
                                label = "Học chữ →",
                                filled = false,
                                onClick = { onStartRowLesson(lesson.rowName, lesson.script) }
                            )
                        }
                    }
                }

                // Thanh hành động dưới cùng khi đã tick >= 1 hàng
                if (matchingWords.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable {
                                stage = CustomStudyStage.VOCAB_AND_PRONUNCIATION
                            }
                            .padding(vertical = 13.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Xem ${matchingWords.size} Từ vựng & Câu mẫu (Luyện phát âm) →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        CustomStudyStage.VOCAB_AND_PRONUNCIATION -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallBtn(
                        label = "← Chọn lại hàng",
                        filled = false,
                        onClick = { stage = CustomStudyStage.SELECT_ROWS }
                    )
                    Text(
                        text = "${matchingWords.size} TỪ VỰNG & CÂU MẪU",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    SmallBtn(
                        label = "✎ Tập viết tất cả",
                        filled = true,
                        onClick = {
                            writingWordIndex = 0
                            stage = CustomStudyStage.WRITING_PRACTICE
                        }
                    )
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(matchingWords, key = { _, w -> w.id.ifBlank { w.word } }) { idx, word ->
                        val recKey = word.word
                        val isThisRecording = isRecording && activeRecTargetKey == recKey
                        val hasRec = recordedKeys.contains(recKey)

                        VocabSentenceCard(
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
                                stage = CustomStudyStage.WRITING_PRACTICE
                            }
                        )
                    }
                }
            }
        }

        CustomStudyStage.WRITING_PRACTICE -> {
            val safeIdx = writingWordIndex.coerceIn(0, (matchingWords.size - 1).coerceAtLeast(0))
            val currentWord = matchingWords.getOrNull(safeIdx)
            if (currentWord == null) {
                stage = CustomStudyStage.SELECT_ROWS
            } else {
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
                    onPrevWord = { if (safeIdx > 0) writingWordIndex = safeIdx - 1 },
                    onNextWord = {
                        if (safeIdx < matchingWords.lastIndex) {
                            writingWordIndex = safeIdx + 1
                        } else {
                            stage = CustomStudyStage.VOCAB_AND_PRONUNCIATION
                        }
                    },
                    onExitWriting = { stage = CustomStudyStage.VOCAB_AND_PRONUNCIATION },
                    localView = localView,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun VocabSentenceCard(
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
                    text = "Nghĩa: ${word.meaningVi}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallBtn(label = "🔊 Nghe từ", filled = true, onClick = onSpeakWord)
                SmallBtn(label = "✎ Tập viết", filled = false, onClick = onPracticeWriteThisWord)
            }
        }

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
                        text = word.exampleSentence,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${word.exampleRomaji} — ${word.exampleVi}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SmallBtn(label = "🔊 Nghe câu", filled = false, onClick = onSpeakSentence)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallBtn(
                label = if (isRecording) "⏹ Dừng ghi" else "🎙 Ghi âm phát âm",
                filled = isRecording,
                isAlert = isRecording,
                onClick = onToggleRecord,
                modifier = Modifier.weight(1f)
            )
            if (hasRecording) {
                SmallBtn(
                    label = if (isPlayingRecording) "Đang phát..." else "▶ Giọng bạn",
                    filled = false,
                    onClick = onPlayUserRecording,
                    modifier = Modifier.weight(1f)
                )
                SmallBtn(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallBtn(label = "← Danh sách từ", filled = false, onClick = onExitWriting)
            Text(
                text = "TẬP VIẾT ($wordPositionLabel)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallBtn(label = "‹ Trước", filled = false, onClick = onPrevWord)
                SmallBtn(label = "Sau ›", filled = false, onClick = onNextWord)
            }
        }

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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable(onClick = onSpeakSentence)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallBtn(label = "🔊 Từ", filled = true, onClick = onSpeakWord)
                    if (word.exampleSentence.isNotBlank()) {
                        SmallBtn(label = "🔊 Câu", filled = false, onClick = onSpeakSentence)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallBtn(
                    label = if (isRecording) "⏹ Dừng ghi" else "🎙 Ghi âm phát âm",
                    filled = isRecording,
                    isAlert = isRecording,
                    onClick = onToggleRecord,
                    modifier = Modifier.weight(1f)
                )
                if (hasRecording) {
                    SmallBtn(
                        label = if (isPlayingRecording) "Đang phát..." else "▶ Giọng bạn",
                        filled = false,
                        onClick = onPlayUserRecording,
                        modifier = Modifier.weight(1f)
                    )
                    SmallBtn(
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallBtn(
                label = "Hoàn tác",
                filled = false,
                onClick = {
                    autoAdvanceJob?.cancel()
                    val cur = slots.getOrNull(activeSlotIndex) ?: return@SmallBtn
                    if (cur.userStrokes.isNotEmpty()) {
                        slots = slots.mapIndexed { idx, s ->
                            if (idx == activeSlotIndex) s.copy(userStrokes = s.userStrokes.dropLast(1), evaluation = null)
                            else s
                        }
                    } else if (activeSlotIndex > 0) {
                        activeSlotIndex -= 1
                    }
                },
                modifier = Modifier.weight(0.85f)
            )
            SmallBtn(
                label = "Xoá",
                filled = false,
                onClick = {
                    autoAdvanceJob?.cancel()
                    slots = slots.map { it.copy(userStrokes = emptyList(), evaluation = null) }
                    activeSlotIndex = 0
                    feedbackText = null
                },
                modifier = Modifier.weight(0.65f)
            )
            SmallBtn(
                label = "Kiểm tra →",
                filled = true,
                onClick = {
                    autoAdvanceJob?.cancel()
                    val engine = recognizer ?: return@SmallBtn
                    val cfg = RecognizerConfig.forLevel(strictnessLevel)
                    val evaluated = slots.map { s ->
                        s.copy(evaluation = engine.recognize(s.userStrokes, s.targetChar, cfg))
                    }
                    slots = evaluated
                    val failed = evaluated.firstOrNull { it.evaluation?.passed != true }
                    if (failed == null) {
                        isFeedbackError = false
                        feedbackText = "Chính xác! Từ '${word.word}' (${word.meaningVi}) đã viết đúng."
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
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
private fun SmallBtn(
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
