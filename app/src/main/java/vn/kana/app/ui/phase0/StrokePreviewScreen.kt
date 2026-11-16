package vn.kana.app.ui.phase0

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import vn.kana.app.domain.AudioAvailability
import vn.kana.app.ui.components.KanaStrokeAnimatorCanvas

@Composable
fun StrokePreviewScreen(
    viewModel: StrokePreviewViewModel,
    onOpenLicenses: () -> Unit,
    onOpenDebug: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val audioAvailability by viewModel.audioAvailability.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Cập nhật trạng thái giọng offline khi người dùng quay về từ Cài đặt máy
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTtsCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var activeStrokeIndex by remember { mutableIntStateOf(0) }
    var activeStrokeProgress by remember { mutableFloatStateOf(0f) }

    val template = uiState.selectedTemplate
    val strokes = uiState.typedStrokes

    // Điều khiển vòng lặp animate từng nét theo thứ tự
    LaunchedEffect(
        template?.char,
        uiState.restartTrigger,
        uiState.isPlaying,
        uiState.isLooping,
        uiState.speedMultiplier
    ) {
        if (strokes.isEmpty()) return@LaunchedEffect
        if (!uiState.isPlaying) return@LaunchedEffect

        // Nếu đã chạy xong toàn bộ nét trước đó và bấm Phát lại thì về nét 0
        if (activeStrokeIndex >= strokes.size) {
            activeStrokeIndex = 0
            activeStrokeProgress = 0f
        }

        do {
            for (sIdx in activeStrokeIndex until strokes.size) {
                activeStrokeIndex = sIdx
                val startProgress = if (activeStrokeProgress in 0.01f..0.98f) activeStrokeProgress else 0f
                val baseDurationMs = 700f
                val remainingRatio = 1f - startProgress
                val durationMs = ((baseDurationMs * remainingRatio) / uiState.speedMultiplier)
                    .toInt()
                    .coerceAtLeast(120)

                animate(
                    initialValue = startProgress,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
                ) { value, _ ->
                    activeStrokeProgress = value
                }

                // Nghỉ ngắn giữa các nét
                delay((220f / uiState.speedMultiplier).toLong().coerceAtLeast(60L))
                activeStrokeProgress = 0f
            }

            // Hiển thị toàn bộ chữ đã vẽ xong
            activeStrokeIndex = strokes.size
            activeStrokeProgress = 1f

            if (uiState.isLooping) {
                delay((850f / uiState.speedMultiplier).toLong().coerceAtLeast(250L))
                activeStrokeIndex = 0
                activeStrokeProgress = 0f
            } else {
                viewModel.setAnimationFinished()
                break
            }
        } while (uiState.isLooping && uiState.isPlaying)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: Tên app + nút Giấy phép
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "KANA • XEM THỨ TỰ NÉT",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Bước 1: Quan sát thứ tự và hướng nét chuẩn KanjiVG",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onOpenDebug != null) {
                    Text(
                        text = "Debug chấm",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable(onClick = onOpenDebug)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = "Giấy phép",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = onOpenLicenses)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (template != null) {
            // Thông tin chữ hiện tại + Nút phát âm
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = template.char,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Romaji: ${template.romaji}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        val strokeStepLabel = if (activeStrokeIndex < strokes.size) {
                            "Đang vẽ nét ${activeStrokeIndex + 1} / ${template.strokeCount}"
                        } else {
                            "Hoàn tất ${template.strokeCount} / ${template.strokeCount} nét"
                        }
                        Text(
                            text = "${template.row} • $strokeStepLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Phát âm",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = viewModel::speakCurrentCharacter)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // Cảnh báo hướng dẫn nếu máy chưa cài gói giọng tiếng Nhật offline
            if (audioAvailability == AudioAvailability.MISSING_OFFLINE_JAPANESE_VOICE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Máy chưa cài giọng đọc tiếng Nhật (ja-JP) offline. Bạn vẫn học bình thường hoặc cài thêm trong Cài đặt máy.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Cài giọng",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onBackground)
                            .clickable {
                                try {
                                    val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Ô vuông tỉ lệ 1:1 chiếm tối đa chiều rộng để animate nét
            KanaStrokeAnimatorCanvas(
                strokes = strokes,
                activeStrokeIndex = activeStrokeIndex,
                activeStrokeProgress = activeStrokeProgress,
                showGhostStrokes = true,
                showStrokeNumbers = uiState.showStrokeNumbers,
                showDirectionArrows = uiState.showDirectionArrows
            )

            // Cụm điều khiển: Phát / Tạm dừng, Vẽ lại, Lặp lại, Gợi ý số nét
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MinimalActionButton(
                    label = if (uiState.isPlaying) "Tạm dừng" else "Tiếp tục",
                    isFilled = true,
                    onClick = viewModel::togglePlayPause,
                    modifier = Modifier.weight(1f)
                )
                MinimalActionButton(
                    label = "Vẽ lại",
                    isFilled = false,
                    onClick = {
                        activeStrokeIndex = 0
                        activeStrokeProgress = 0f
                        viewModel.replayFromStart()
                    },
                    modifier = Modifier.weight(1f)
                )
                MinimalActionButton(
                    label = if (uiState.isLooping) "Lặp: BẬT" else "Lặp: TẮT",
                    isFilled = uiState.isLooping,
                    onClick = viewModel::toggleLoop,
                    modifier = Modifier.weight(1f)
                )
            }

            // Cụm chọn tốc độ animate + Ẩn/hiện số nét
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tốc độ:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                    for (spd in speeds) {
                        val selected = uiState.speedMultiplier == spd
                        Text(
                            text = "${spd}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            modifier = Modifier
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(1.dp, MaterialTheme.colorScheme.onBackground)
                                .clickable { viewModel.setSpeed(spd) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = if (uiState.showStrokeNumbers) "Số nét: HIỆN" else "Số nét: ẨN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable(onClick = viewModel::toggleStrokeHints)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Thanh chọn nhanh chữ Kana khác để kiểm tra dữ liệu KanjiVG đã xuất ở Phase 0
            Text(
                text = "Chọn chữ để xem thử (${uiState.characters.size} chữ KanjiVG trong assets):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (item in uiState.characters) {
                    val isSelected = item.char == template.char
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                            .clickable {
                                activeStrokeIndex = 0
                                activeStrokeProgress = 0f
                                viewModel.selectCharacter(item)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = item.char,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalActionButton(
    label: String,
    isFilled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                if (isFilled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(1.dp, MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isFilled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onBackground
            }
        )
    }
}
