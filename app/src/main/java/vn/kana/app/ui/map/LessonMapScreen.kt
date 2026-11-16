package vn.kana.app.ui.map

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.kana.app.domain.GojuonRowLesson

@Composable
fun LessonMapScreen(
    viewModel: LessonMapViewModel,
    onStartRowLesson: (rowName: String, script: String) -> Unit,
    onTestOutRowLesson: (rowName: String, script: String) -> Unit = { _, _ -> },
    onBackHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Header trên 1 hàng gọn gàng: nút Trang chủ bên trái, tiêu đề ở giữa, nút Cài đặt 1 dòng bên phải
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onBackHome)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "← Trang chủ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = "BẢN ĐỒ BÀI HỌC",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                softWrap = false
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onOpenSettings)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Cài đặt",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // 2. Thanh chọn nhóm bài học ngay sát dưới Header (khoảng cách 8dp gọn gàng)
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
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. Danh sách bài học
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.filteredLessons, key = { it.rowId }) { lesson ->
                RowLessonCard(
                    lesson = lesson,
                    learnedMap = uiState.characterProgressByChar.mapValues { it.value.learned },
                    onStart = {
                        if (lesson.isUnlocked) {
                            onStartRowLesson(lesson.rowName, lesson.script)
                        }
                    },
                    onTestOut = {
                        onTestOutRowLesson(lesson.rowName, lesson.script)
                    }
                )
            }
        }
    }
}

@Composable
private fun RowLessonCard(
    lesson: GojuonRowLesson,
    learnedMap: Map<String, Boolean>,
    onStart: () -> Unit,
    onTestOut: () -> Unit
) {
    val borderColor = if (lesson.isUnlocked) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dòng 1: Tên hàng bài học + Trạng thái
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.titleVi,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (lesson.isUnlocked) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lesson.subtitleVi,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (lesson.isCompleted) {
                Text(
                    text = "Đã xong (${lesson.bestQuizScore}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Dòng 2: Các ô chữ trong hàng
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (ch in lesson.characters) {
                val isLearned = learnedMap[ch.char] == true
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isLearned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = ch.char,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isLearned) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = ch.romaji,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLearned) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Dòng 3: Hai nút hành động cân đối trên 1 hàng, không bao giờ bị ép xuống 2 dòng
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!lesson.isCompleted) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = onTestOut)
                        .padding(vertical = 9.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = if (lesson.isUnlocked) "Đã biết • Kiểm tra" else "Kiểm tra mở khoá",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (lesson.isUnlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(1.dp, borderColor)
                    .clickable(enabled = lesson.isUnlocked, onClick = onStart)
                    .padding(vertical = 9.dp, horizontal = 8.dp)
            ) {
                Text(
                    text = when {
                        lesson.isCompleted -> "Học lại bài này →"
                        lesson.isUnlocked -> "Học từng bước →"
                        else -> "Đang khoá"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (lesson.isUnlocked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
