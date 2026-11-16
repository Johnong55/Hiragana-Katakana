package vn.kana.app.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Trạng thái hệ thống âm thanh offline.
 */
enum class AudioAvailability {
    /** Đang khởi tạo TextToSpeech / kiểm tra gói giọng */
    INITIALIZING,
    /** Sẵn sàng phát âm offline (từ file ogg trong assets hoặc giọng TTS ja-JP đã cài trên máy) */
    READY_OFFLINE,
    /** Thiết bị chưa cài đặt gói giọng tiếng Nhật (ja-JP) offline; vẫn có thể phát từ file ogg nếu có */
    MISSING_OFFLINE_JAPANESE_VOICE,
    /** Thiết bị không hỗ trợ dịch vụ TTS; chỉ phát được file ogg trong assets nếu có */
    TTS_UNAVAILABLE
}

/**
 * Giao diện phát âm chữ Kana và từ vựng (100% offline).
 * Ưu tiên phát file `.ogg` ngắn trong `assets/audio/`, nếu chưa có thì dùng Android TextToSpeech `ja-JP`.
 */
interface AudioProvider {
    val availability: StateFlow<AudioAvailability>

    /**
     * Phát âm một chữ Kana hoặc một từ tiếng Nhật.
     * @param japaneseText Chữ Kana hoặc từ cần đọc (ví dụ: "あ", "かさ")
     * @param audioAssetKey Tên khóa file ogg trong `assets/audio/<key>.ogg` (ví dụ: "a", "kasa")
     */
    fun speak(japaneseText: String, audioAssetKey: String? = null)

    /**
     * Kiểm tra lại gói giọng tiếng Nhật offline (sau khi người dùng quay về từ màn Cài đặt TTS của máy).
     */
    fun refreshVoiceCheck()

    /**
     * Giải phóng tài nguyên MediaPlayer / TextToSpeech.
     */
    fun release()
}
