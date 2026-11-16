package vn.kana.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import vn.kana.app.data.UserSettings
import vn.kana.recognizer.StrictnessLevel

/**
 * Màn hình Cài đặt:
 * - Độ nghiêm bộ chấm điểm (Dễ, Chuẩn, Nghiêm)
 * - Tốc độ animate nét (0.5x, 1.0x, 1.5x, 2.0x)
 * - Âm thanh phát âm (Bật / Tắt + mở cài đặt TTS hệ thống)
 * - Thời gian tự chuyển ô khi viết từ nhiều chữ (400ms, 700ms, 1000ms, 1500ms)
 * - Xoá tiến độ học
 * - Màn hình Debug (ẩn trong Cài đặt: có thể mở trực tiếp hoặc chạm vào mục Công cụ phát triển)
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onChangeStrictness: (StrictnessLevel) -> Unit,
    onChangeAnimationSpeed: (Float) -> Unit,
    onChangeAutoAdvanceDelay: (Long) -> Unit,
    onChangeSoundEnabled: (Boolean) -> Unit,
    onChangeUnlockAllRows: (Boolean) -> Unit = {},
    onClearProgress: () -> Unit,
    onOpenDebugScreen: () -> Unit,
    onOpenLicenses: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConfirmReset by remember { mutableStateOf(false) }
    var resetDoneBanner by remember { mutableStateOf(false) }
    var devTapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "← Quay lại",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Text(
                text = "CÀI ĐẶT",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { devTapCount++ }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Độ nghiêm khi chấm nét
            SettingCard(title = "1. Độ nghiêm khi chấm chữ viết") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (level in StrictnessLevel.entries) {
                        val selected = settings.strictnessLevel == level
                        SelectableChip(
                            label = level.displayNameVi,
                            selected = selected,
                            onClick = { onChangeStrictness(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Tốc độ animate nét mẫu
            SettingCard(title = "2. Tốc độ animate nét mẫu") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (spd in listOf(0.5f, 1.0f, 1.5f, 2.0f)) {
                        SelectableChip(
                            label = "${spd}x",
                            selected = settings.animationSpeed == spd,
                            onClick = { onChangeAnimationSpeed(spd) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Thời gian tự chuyển ô khi viết từ vựng
            SettingCard(title = "3. Thời gian tự chuyển ô (khi viết từ nhiều chữ)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (ms in listOf(400L, 700L, 1000L, 1500L)) {
                        SelectableChip(
                            label = "${ms}ms",
                            selected = settings.autoAdvanceDelayMs == ms,
                            onClick = { onChangeAutoAdvanceDelay(ms) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Âm thanh
            SettingCard(title = "4. Âm thanh phát âm") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableChip(
                        label = "Bật âm thanh",
                        selected = settings.soundEnabled,
                        onClick = { onChangeSoundEnabled(true) },
                        modifier = Modifier.weight(1f)
                    )
                    SelectableChip(
                        label = "Tắt âm thanh",
                        selected = !settings.soundEnabled,
                        onClick = { onChangeSoundEnabled(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Mở Cài đặt Giọng đọc (TTS) của hệ thống Android →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .clickable {
                            try {
                                context.startActivity(
                                    Intent("com.android.settings.TTS_SETTINGS").apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            } catch (_: Exception) {
                            }
                        }
                        .padding(10.dp)
                )
            }

            // 5. Xoá tiến độ
            SettingCard(title = "5. Quản lý dữ liệu học tập") {
                if (!showConfirmReset) {
                    SelectableChip(
                        label = "Xoá toàn bộ tiến độ học",
                        selected = false,
                        onClick = {
                            showConfirmReset = true
                            resetDoneBanner = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Bạn có chắc chắn muốn xoá toàn bộ tiến độ học và thẻ ôn tập không?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectableChip(
                            label = "Huỷ",
                            selected = false,
                            onClick = { showConfirmReset = false },
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.error)
                                .border(1.dp, MaterialTheme.colorScheme.error)
                                .clickable {
                                    onClearProgress()
                                    showConfirmReset = false
                                    resetDoneBanner = true
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "Xác nhận xoá",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
                if (resetDoneBanner) {
                    Text(
                        text = "Đã xoá toàn bộ tiến độ học.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // 6. Giấy phép & Màn hình Debug (ẩn trong Cài đặt)
            SettingCard(title = "6. Thông tin & Công cụ kỹ thuật") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableChip(
                        label = if (settings.unlockAllRows) "Mở mọi bài học: BẬT" else "Mở mọi bài học: TẮT",
                        selected = settings.unlockAllRows,
                        onClick = { onChangeUnlockAllRows(!settings.unlockAllRows) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableChip(
                        label = "Giấy phép (KanjiVG)",
                        selected = false,
                        onClick = onOpenLicenses,
                        modifier = Modifier.weight(1f)
                    )
                    SelectableChip(
                        label = "Màn hình Debug chấm nét",
                        selected = false,
                        onClick = onOpenDebugScreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        content()
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .border(1.dp, MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onBackground
        )
    }
}
