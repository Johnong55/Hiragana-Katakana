package vn.kana.app.ui.dictionary

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.kana.app.domain.KanaVocabWord
import vn.kana.app.domain.N5GrammarCatalog
import vn.kana.app.domain.N5GrammarPoint
import vn.kana.recognizer.KanaStrokeTemplate

private enum class DictionaryFilter(val label: String) {
    ALL("Tất cả"),
    CHAR_KANJI("Chữ & Kanji"),
    VOCAB("Từ vựng"),
    GRAMMAR("Ngữ pháp")
}

private sealed interface DictionaryEntry {
    val key: String

    data class CharEntry(val template: KanaStrokeTemplate) : DictionaryEntry {
        override val key: String = "c_${template.script}_${template.char}"
    }

    data class VocabEntry(val vocab: KanaVocabWord) : DictionaryEntry {
        override val key: String = "v_${vocab.word}_${vocab.romaji}"
    }

    data class GrammarEntry(val point: N5GrammarPoint) : DictionaryEntry {
        override val key: String = "g_${point.id}"
    }
}

@Composable
fun OfflineDictionaryScreen(
    allCharacters: List<KanaStrokeTemplate>,
    wordsByChar: Map<String, List<KanaVocabWord>>,
    savedVocabWords: Set<String> = emptySet(),
    onToggleSaveWord: (String) -> Unit = {},
    onSpeak: (String, String?) -> Unit,
    onPracticeCharacter: (rowName: String, script: String, targetChar: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DictionaryFilter.ALL) }

    val allVocab = remember(wordsByChar) {
        wordsByChar.values.flatten().distinctBy { it.word }
    }
    val allGrammar = remember {
        N5GrammarCatalog.stage5Points + N5GrammarCatalog.stage7Points
    }

    val results = remember(query, filter, allCharacters, allVocab, allGrammar) {
        val q = query.trim().lowercase()
        val list = mutableListOf<DictionaryEntry>()

        if (filter == DictionaryFilter.ALL || filter == DictionaryFilter.CHAR_KANJI) {
            allCharacters
                .filter {
                    q.isEmpty() ||
                        it.char.lowercase().contains(q) ||
                        it.romaji.lowercase().contains(q) ||
                        it.row.lowercase().contains(q) ||
                        it.script.lowercase().contains(q)
                }
                .take(40)
                .forEach { list += DictionaryEntry.CharEntry(it) }
        }

        if (filter == DictionaryFilter.ALL || filter == DictionaryFilter.VOCAB) {
            allVocab
                .filter {
                    q.isEmpty() ||
                        it.word.lowercase().contains(q) ||
                        it.romaji.lowercase().contains(q) ||
                        it.meaningVi.lowercase().contains(q)
                }
                .take(40)
                .forEach { list += DictionaryEntry.VocabEntry(it) }
        }

        if (filter == DictionaryFilter.ALL || filter == DictionaryFilter.GRAMMAR) {
            allGrammar
                .filter {
                    q.isEmpty() ||
                        it.titleVi.lowercase().contains(q) ||
                        it.patternJp.lowercase().contains(q) ||
                        it.explanationVi.lowercase().contains(q) ||
                        it.exampleJp.lowercase().contains(q) ||
                        it.exampleVi.lowercase().contains(q)
                }
                .forEach { list += DictionaryEntry.GrammarEntry(it) }
        }

        list
    }

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
            Text(
                text = "← Quay lại",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .clickable { onBack() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = "TRA CỨU OFFLINE",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tìm tiếng Nhật, Romaji, hoặc nghĩa Việt...") },
            singleLine = true,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DictionaryFilter.entries.forEach { item ->
                val selected = filter == item
                Box(
                    modifier = Modifier
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { filter = item }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Text(
            text = "Tìm thấy ${results.size} mục",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.key }) { entry ->
                when (entry) {
                    is DictionaryEntry.CharEntry -> {
                        val t = entry.template
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = t.char,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Column {
                                    Text(
                                        text = t.romaji,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "${t.script} • ${t.row} • ${t.strokeCount} nét",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Nghe",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.outline)
                                        .clickable { onSpeak(t.char, t.romaji) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                                Text(
                                    text = "Viết →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { onPracticeCharacter(t.row, t.script, t.char) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    is DictionaryEntry.VocabEntry -> {
                        val v = entry.vocab
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onSpeak(v.word, v.romaji) }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${v.word}  (${v.romaji})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = v.meaningVi,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val isSaved = v.word in savedVocabWords
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isSaved) "★ Đã lưu" else "☆ Lưu ôn",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSaved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .background(if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outline)
                                        .clickable { onToggleSaveWord(v.word) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                                Text(
                                    text = "Nghe",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.outline)
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    is DictionaryEntry.GrammarEntry -> {
                        val g = entry.point
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "NGỮ PHÁP • ${g.titleVi}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = g.patternJp,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = g.explanationVi,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Ví dụ: ${g.exampleJp} — ${g.exampleVi}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.clickable { onSpeak(g.exampleJp, null) }
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
