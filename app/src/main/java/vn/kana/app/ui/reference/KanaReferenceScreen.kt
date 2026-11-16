package vn.kana.app.ui.reference

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import vn.kana.app.ui.components.KanaStrokeAnimatorCanvas
import vn.kana.app.ui.components.drawSmoothStrokePath
import vn.kana.app.ui.home.EXTENDED_KANA_LAYOUT
import vn.kana.app.ui.home.HIRAGANA_GOJUON_LAYOUT
import vn.kana.app.ui.home.KATAKANA_GOJUON_LAYOUT
import vn.kana.app.ui.map.CurriculumTab
import vn.kana.app.ui.map.LessonMapViewModel

/**
 * Màn hình Bảng Kana tham khảo đầy đủ (Hiragana, Katakana, Dakuten/Handakuten, Yōon, っ, ー):
 * - Chữ mẫu render từ dữ liệu nét KanjiVG.
 * - Khi chạm vào bất kỳ chữ nào: xem animate từng nét theo thứ tự, phát âm và 2 từ vựng đi kèm.
 */
@Composable
fun KanaReferenceScreen(
    viewModel: LessonMapViewModel,
    initialChar: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val byChar = remember(uiState.allCurriculumCharacters) {
        uiState.allCurriculumCharacters.associateBy { it.char }
    }

    var selectedChar by remember(initialChar, uiState.allCurriculumCharacters) {
        mutableStateOf(
            initialChar ?: uiState.allCurriculumCharacters.firstOrNull()?.char ?: "あ"
        )
    }
    var replayTrigger by remember { mutableIntStateOf(0) }

    val selectedTemplate = byChar[selectedChar] ?: uiState.allCurriculumCharacters.firstOrNull()
    val selectedStrokes = remember(selectedTemplate?.char) {
        selectedTemplate?.toTypedStrokes().orEmpty()
    }
    val selectedWords = uiState.wordsByChar[selectedTemplate?.char.orEmpty()].orEmpty()

    var animStrokeIdx by remember { mutableIntStateOf(0) }
    var animStrokeProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedTemplate?.char, replayTrigger, uiState.settings.animationSpeed) {
        if (selectedStrokes.isEmpty()) return@LaunchedEffect
        val speed = uiState.settings.animationSpeed.coerceIn(0.5f, 2.0f)
        while (true) {
            for (sIdx in selectedStrokes.indices) {
                animStrokeIdx = sIdx
                animStrokeProgress = 0f
                val dur = (660f / speed).toInt().coerceAtLeast(130)
                animate(0f, 1f, animationSpec = tween(dur, easing = LinearEasing)) { v, _ ->
                    animStrokeProgress = v
                }
                delay((180f / speed).toLong().coerceAtLeast(60L))
            }
            animStrokeIdx = selectedStrokes.size
            animStrokeProgress = 1f
            delay((850f / speed).toLong().coerceAtLeast(250L))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "← Trang chủ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Text(
                text = "BẢNG KANA THAM KHẢO",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(8.dp))

        // Khung trên cố định: Animate nét của chữ đang chọn + Từ vựng
        if (selectedTemplate != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(132.dp)) {
                    KanaStrokeAnimatorCanvas(
                        strokes = selectedStrokes,
                        activeStrokeIndex = animStrokeIdx,
                        activeStrokeProgress = animStrokeProgress,
                        showGhostStrokes = true,
                        showStrokeNumbers = true,
                        showDirectionArrows = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedTemplate.char} (${selectedTemplate.romaji})",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${selectedTemplate.row} • ${selectedTemplate.strokeCount} nét",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Phát âm",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    replayTrigger++
                                    viewModel.speak(selectedTemplate.char, selectedTemplate.romaji)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                    for (w in selectedWords) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.speak(w.word, w.romaji) }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${w.word} (${w.romaji})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = w.meaningVi,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Thanh chuyển hệ chữ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (tab in CurriculumTab.entries) {
                val active = uiState.selectedTab == tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable {
                            viewModel.selectTab(tab)
                            val firstCharInTab = when (tab) {
                                CurriculumTab.HIRAGANA_BASIC -> "あ"
                                CurriculumTab.KATAKANA_BASIC -> "ア"
                                CurriculumTab.EXTENDED -> "が"
                            }
                            selectedChar = firstCharInTab
                        }
                        .padding(vertical = 7.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = tab.titleVi,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val rowsLayout = when (uiState.selectedTab) {
            CurriculumTab.HIRAGANA_BASIC -> HIRAGANA_GOJUON_LAYOUT
            CurriculumTab.KATAKANA_BASIC -> KATAKANA_GOJUON_LAYOUT
            CurriculumTab.EXTENDED -> EXTENDED_KANA_LAYOUT
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for ((rowLabel, slots) in rowsLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rowLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(width = 42.dp, height = 20.dp)
                    )
                    for (ch in slots) {
                        if (ch == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        } else {
                            val tmpl = byChar[ch]
                            val isSelected = ch == selectedTemplate?.char
                            val strokes = remember(tmpl?.char) { tmpl?.toTypedStrokes().orEmpty() }
                            val fgColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onBackground
                                        else MaterialTheme.colorScheme.outline
                                    )
                                    .clickable {
                                        selectedChar = ch
                                        tmpl?.let { viewModel.speak(it.char, it.romaji) }
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(2.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.minDimension * 0.08f
                                        for (s in strokes) {
                                            drawSmoothStrokePath(
                                                points = s.points,
                                                progress = 1f,
                                                color = fgColor,
                                                strokeWidth = w
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${tmpl?.romaji.orEmpty()} (${tmpl?.strokeCount ?: 0})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = fgColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
