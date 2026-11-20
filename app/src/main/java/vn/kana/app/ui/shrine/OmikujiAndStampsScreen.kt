package vn.kana.app.ui.shrine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.kana.app.R
import vn.kana.app.domain.HankoStampSpec
import vn.kana.app.domain.OmikujiAndStampsCatalog
import vn.kana.app.domain.OmikujiSlip
import vn.kana.app.ui.map.LessonMapUiState
import kotlin.math.roundToInt
import kotlin.math.sqrt

private enum class ShrineTab(val label: String) {
    OMIKUJI("おみくじ Rút Quẻ"),
    STAMPS("スタンプ帳 Sổ Dấu")
}

private val HankoRed = Color(0xFFB83227)

@Composable
fun OmikujiAndStampsScreen(
    uiState: LessonMapUiState,
    onSpeak: (String) -> Unit,
    onRecordOmikujiDraw: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ShrineTab.OMIKUJI) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
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
                text = "ĐỀN MÈO N5 & SỔ DẤU",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 2-Tab switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShrineTab.entries.forEach { tab ->
                val selected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable { activeTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        when (activeTab) {
            ShrineTab.OMIKUJI -> OmikujiShrineView(
                drawCount = uiState.settings.omikujiDrawCount,
                onSpeak = onSpeak,
                onRecordOmikujiDraw = onRecordOmikujiDraw
            )

            ShrineTab.STAMPS -> HankoStampPassportView(
                uiState = uiState
            )
        }
    }
}

@Composable
private fun OmikujiShrineView(
    drawCount: Int,
    onSpeak: (String) -> Unit,
    onRecordOmikujiDraw: () -> Unit
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val scope = rememberCoroutineScope()

    var currentSlip by remember { mutableStateOf<OmikujiSlip?>(null) }
    var isShaking by remember { mutableStateOf(false) }
    val shakeX = remember { Animatable(0f) }

    fun triggerDraw() {
        if (isShaking) return
        isShaking = true
        localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        scope.launch {
            val offsets = listOf(-12f, 12f, -10f, 10f, -6f, 6f, 0f)
            for (o in offsets) {
                shakeX.animateTo(o, animationSpec = tween(42))
            }
            delay(80L)
            val candidates = OmikujiAndStampsCatalog.omikujiSlips.filter { it.id != currentSlip?.id }
            val picked = (candidates.ifEmpty { OmikujiAndStampsCatalog.omikujiSlips }).random()
            currentSlip = picked
            isShaking = false
            localView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onRecordOmikujiDraw()
        }
    }

    // Hỗ trợ lắc nhẹ điện thoại để rút quẻ (100% offline bằng cảm biến gia tốc)
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (gForce > 2.3 && now - lastShakeMs > 1200L) {
                    lastShakeMs = now
                    triggerDraw()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accel != null) {
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val slip = currentSlip
        if (slip == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeX.value.roundToInt(), 0) }
                    .border(2.dp, MaterialTheme.colorScheme.onBackground)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HankoRedSeal(kanji = "おみくじ", sizeDp = 64)
                Image(
                    painter = painterResource(id = R.drawable.meme_omikuji_happy),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Text(
                    text = "HỘP QUẺ ĐỀN MÈO JLPT N5",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Lắc nhẹ điện thoại hoặc bấm nút bên dưới để rút 1 lá quẻ tiếng Nhật kèm nhiệm vụ vui hôm nay! (Đã rút: $drawCount lần)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { triggerDraw() },
                    enabled = !isShaking,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isShaking) "Đang lắc hộp quẻ..." else "Lắc hộp rút quẻ hôm nay →")
                }
            }
        } else {
            // Lá quẻ giấy Washi cổ điển
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeX.value.roundToInt(), 0) }
                    .border(2.dp, if (slip.isWarning) HankoRed else MaterialTheme.colorScheme.onBackground)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "QUẺ HÔM NAY (${slip.tierRomaji.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = slip.tierVi,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (slip.isWarning) HankoRed else MaterialTheme.colorScheme.primary
                        )
                    }
                    HankoRedSeal(kanji = slip.tierJa, sizeDp = 54)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                Image(
                    painter = painterResource(id = slip.memeRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(106.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (slip.isWarning) HankoRed else MaterialTheme.colorScheme.primary, CircleShape)
                )

                // Câu châm ngôn tiếng Nhật
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = slip.proverbJa,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Nghe",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onSpeak(slip.proverbJa) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Text(
                        text = slip.proverbRomaji,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "\"${slip.proverbVi}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Lời phán học tập & nhiệm vụ vui
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Vận học tập: ${slip.studyFortuneVi}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Chữ may mắn hôm nay: ${slip.luckyChar}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = slip.funQuestVi,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                OutlinedButton(
                    onClick = { triggerDraw() },
                    enabled = !isShaking,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isShaking) "Đang lắc hộp quẻ..." else "Lắc rút quẻ khác (hoặc lắc điện thoại)")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun HankoStampPassportView(
    uiState: LessonMapUiState
) {
    val stamps = OmikujiAndStampsCatalog.hankoStamps
    val unlockedCount = stamps.count { it.isUnlocked(uiState) }
    var inspectingStamp by remember { mutableStateOf<HankoStampSpec?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SỔ ĐÓNG DẤU HANKO & THẺ MÈO",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$unlockedCount / ${stamps.size} dấu",
                    style = MaterialTheme.typography.labelLarge,
                    color = HankoRed
                )
            }
            LinearProgressIndicator(
                progress = { unlockedCount.toFloat() / stamps.size.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = HankoRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "Hoàn thành các mốc học tập để đóng dấu đỏ son Hanko và mở khoá thẻ Meme Mèo sưu tầm!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Lưới 2 cột Sổ đóng dấu
        val rows = stamps.chunked(2)
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { stamp ->
                    val unlocked = stamp.isUnlocked(uiState)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (unlocked) 2.dp else 1.dp,
                                color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            .background(
                                if (unlocked) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                            .clickable(enabled = unlocked) { inspectingStamp = stamp }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(76.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (unlocked) {
                                Image(
                                    painter = painterResource(id = stamp.memeRes),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Box(
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                ) {
                                    HankoRedSeal(kanji = stamp.sealKanji, sizeDp = 30)
                                }
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    Text(
                                        text = "?",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = stamp.titleVi,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (unlocked) "Đã đóng dấu • Chạm xem" else stamp.conditionVi,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unlocked) HankoRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }

    inspectingStamp?.let { stamp ->
        Dialog(onDismissRequest = { inspectingStamp = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.onBackground)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HankoRedSeal(kanji = stamp.sealKanji, sizeDp = 50)
                Image(
                    painter = painterResource(id = stamp.memeRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Text(
                    text = stamp.titleVi.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stamp.flavorVi,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { inspectingStamp = null },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đóng thẻ")
                }
            }
        }
    }
}

@Composable
private fun HankoRedSeal(
    kanji: String,
    sizeDp: Int
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .rotate(-9f)
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(Color(0xFFFBF7EC))
            .border(2.dp, HankoRed, CircleShape)
            .padding(2.dp)
            .border(1.dp, HankoRed, CircleShape)
    ) {
        Text(
            text = kanji,
            color = HankoRed,
            fontWeight = FontWeight.ExtraBold,
            fontSize = when {
                kanji.length >= 4 -> 11.sp
                kanji.length == 2 -> 15.sp
                sizeDp <= 32 -> 13.sp
                else -> 20.sp
            },
            maxLines = 1
        )
    }
}
