package vn.kana.app.domain

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.Locale
import java.util.UUID

/**
 * Triển khai [AudioProvider] hoạt động 100% offline:
 * 1. Ưu tiên kiểm tra và phát file `assets/audio/<audioAssetKey>.ogg` (nếu có).
 * 2. Nếu chưa có file `.ogg` tương ứng, dùng Android [TextToSpeech] với `Locale.JAPAN` (`ja-JP`).
 * 3. Kiểm tra xem gói giọng `ja-JP` offline đã cài trên thiết bị chưa để hướng dẫn người dùng cài đặt nếu thiếu.
 */
class AndroidAudioProvider(
    private val appContext: Context
) : AudioProvider, TextToSpeech.OnInitListener {

    private val _availability = MutableStateFlow(AudioAvailability.INITIALIZING)
    override val availability: StateFlow<AudioAvailability> = _availability.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (_: Exception) {
            _availability.value = AudioAvailability.TTS_UNAVAILABLE
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            evaluateJapaneseOfflineSupport()
        } else {
            isTtsInitialized = false
            _availability.value = AudioAvailability.TTS_UNAVAILABLE
        }
    }

    private fun evaluateJapaneseOfflineSupport() {
        val engine = tts ?: run {
            _availability.value = AudioAvailability.TTS_UNAVAILABLE
            return
        }
        val langResult = engine.setLanguage(Locale.JAPAN)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            _availability.value = AudioAvailability.MISSING_OFFLINE_JAPANESE_VOICE
            return
        }

        // Kiểm tra thêm danh sách voices nếu engine hỗ trợ để ưu tiên voice offline
        val voices = try {
            engine.voices
        } catch (_: Exception) {
            null
        }
        if (voices != null) {
            val offlineJpVoice = voices.firstOrNull { voice ->
                voice.locale.language == Locale.JAPAN.language &&
                    !voice.isNetworkConnectionRequired &&
                    !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            if (offlineJpVoice != null) {
                try {
                    engine.voice = offlineJpVoice
                } catch (_: Exception) {
                    // Giữ cấu hình Locale.JAPAN mặc định
                }
                _availability.value = AudioAvailability.READY_OFFLINE
            } else {
                // Nếu danh sách voice báo chưa có voice offline cài sẵn nhưng setLanguage báo hỗ trợ,
                // kiểm tra cờ KEY_FEATURE_NOT_INSTALLED của voice hiện tại
                val currentVoice = try {
                    engine.voice
                } catch (_: Exception) {
                    null
                }
                val notInstalled = currentVoice?.features?.contains(
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
                ) == true
                _availability.value = if (notInstalled) {
                    AudioAvailability.MISSING_OFFLINE_JAPANESE_VOICE
                } else {
                    AudioAvailability.READY_OFFLINE
                }
            }
        } else {
            _availability.value = AudioAvailability.READY_OFFLINE
        }
    }

    override fun speak(japaneseText: String, audioAssetKey: String?) {
        if (japaneseText.isBlank()) return

        // 1. Ưu tiên phát từ file ogg trong assets/audio/ nếu tồn tại
        if (!audioAssetKey.isNullOrBlank() && tryPlayOggAsset(audioAssetKey)) {
            return
        }

        // 2. Fallback sang Android TextToSpeech ja-JP
        val engine = tts ?: return
        if (!isTtsInitialized) return
        try {
            engine.speak(
                japaneseText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                UUID.randomUUID().toString()
            )
        } catch (_: Exception) {
            // Bỏ qua lỗi runtime của TTS để app luôn dùng được kể cả khi không có âm thanh
        }
    }

    private fun tryPlayOggAsset(key: String): Boolean {
        val assetPath = "audio/${key.lowercase(Locale.ROOT)}.ogg"
        return try {
            appContext.assets.openFd(assetPath).use { afd ->
                releaseMediaPlayer()
                val player = MediaPlayer()
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                player.setOnCompletionListener { mp ->
                    mp.release()
                    if (mediaPlayer === mp) {
                        mediaPlayer = null
                    }
                }
                player.prepare()
                player.start()
                mediaPlayer = player
                true
            }
        } catch (_: IOException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    override fun refreshVoiceCheck() {
        if (isTtsInitialized) {
            evaluateJapaneseOfflineSupport()
        } else {
            initTts()
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    override fun release() {
        releaseMediaPlayer()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        isTtsInitialized = false
    }
}
