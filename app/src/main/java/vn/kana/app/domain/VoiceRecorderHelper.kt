package vn.kana.app.domain

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Bộ ghi âm và phát lại giọng đọc 100% offline (lưu file tạm trong `cacheDir`).
 * Giúp người học tự thu âm phát âm từ vựng / câu mẫu và nghe lại đối chiếu với âm mẫu.
 */
class VoiceRecorderHelper(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlayingRecording = MutableStateFlow(false)
    val isPlayingRecording: StateFlow<Boolean> = _isPlayingRecording.asStateFlow()

    private val _recordedKeys = MutableStateFlow<Set<String>>(emptySet())
    val recordedKeys: StateFlow<Set<String>> = _recordedKeys.asStateFlow()

    private var activeRecordingKey: String? = null

    private fun fileForKey(key: String): File {
        val safeName = key.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val dir = File(context.cacheDir, "pronunciation_records").apply { mkdirs() }
        return File(dir, "rec_$safeName.m4a")
    }

    fun startRecording(key: String): Boolean {
        stopRecording()
        stopPlayback()
        val outFile = fileForKey(key)
        return try {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(44100)
            mediaRecorder.setAudioEncodingBitRate(96000)
            mediaRecorder.setOutputFile(outFile.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()

            recorder = mediaRecorder
            activeRecordingKey = key
            _isRecording.value = true
            true
        } catch (_: Exception) {
            releaseRecorder()
            false
        }
    }

    fun stopRecording() {
        val key = activeRecordingKey
        if (recorder != null) {
            try {
                recorder?.stop()
                if (key != null && fileForKey(key).exists() && fileForKey(key).length() > 100L) {
                    _recordedKeys.value = _recordedKeys.value + key
                }
            } catch (_: Exception) {
            } finally {
                releaseRecorder()
            }
        }
    }

    fun playRecording(key: String, onFinished: (() -> Unit)? = null): Boolean {
        stopRecording()
        stopPlayback()
        val file = fileForKey(key)
        if (!file.exists() || file.length() <= 100L) return false
        return try {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener {
                stopPlayback()
                onFinished?.invoke()
            }
            mp.prepare()
            mp.start()
            player = mp
            _isPlayingRecording.value = true
            true
        } catch (_: Exception) {
            stopPlayback()
            false
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        _isPlayingRecording.value = false
    }

    private fun releaseRecorder() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        activeRecordingKey = null
        _isRecording.value = false
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
