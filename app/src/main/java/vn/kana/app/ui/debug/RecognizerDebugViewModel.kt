package vn.kana.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kana.app.data.DebugSampleStorage
import vn.kana.app.data.SavedHandwritingSample
import vn.kana.app.data.StrokeAssetRepository
import vn.kana.app.domain.AudioProvider
import vn.kana.recognizer.KanaRecognizer
import vn.kana.recognizer.KanaStrokeTemplate
import vn.kana.recognizer.RecognitionResult
import vn.kana.recognizer.RecognizerConfig
import vn.kana.recognizer.StrictnessLevel
import vn.kana.recognizer.Stroke
import java.util.UUID

data class RecognizerDebugUiState(
    val isLoading: Boolean = true,
    val characters: List<KanaStrokeTemplate> = emptyList(),
    val selectedScriptFilter: String = "hiragana",
    val targetTemplate: KanaStrokeTemplate? = null,
    val templateStrokes: List<Stroke> = emptyList(),
    val userStrokes: List<Stroke> = emptyList(),
    val strictnessLevel: StrictnessLevel = StrictnessLevel.STANDARD,
    val showGhostOverlay: Boolean = false,
    val showTemplateComparison: Boolean = true,
    val lastResult: RecognitionResult? = null,
    val savedSampleCount: Int = 0,
    val savedSamplePath: String = "",
    val statusBannerMessage: String? = null
)

class RecognizerDebugViewModel(
    private val strokeRepository: StrokeAssetRepository,
    private val sampleStorage: DebugSampleStorage,
    private val audioProvider: AudioProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecognizerDebugUiState())
    val uiState: StateFlow<RecognizerDebugUiState> = _uiState.asStateFlow()

    private var recognizer: KanaRecognizer? = null

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            val allChars = strokeRepository.getAllCharacters()
            recognizer = KanaRecognizer(allChars)
            val saved = sampleStorage.getSavedSamples()
            val defaultTarget = allChars.firstOrNull { it.char == "あ" } ?: allChars.firstOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    characters = allChars,
                    targetTemplate = defaultTarget,
                    templateStrokes = defaultTarget?.toTypedStrokes().orEmpty(),
                    savedSampleCount = saved.size,
                    savedSamplePath = sampleStorage.sampleFile.absolutePath
                )
            }
        }
    }

    fun setScriptFilter(script: String) {
        val currentChars = _uiState.value.characters
        val firstInScript = currentChars.firstOrNull { it.script == script && it.category == "basic" }
            ?: currentChars.firstOrNull { it.script == script }
        _uiState.update {
            it.copy(
                selectedScriptFilter = script,
                targetTemplate = firstInScript ?: it.targetTemplate,
                templateStrokes = firstInScript?.toTypedStrokes() ?: it.templateStrokes,
                userStrokes = emptyList(),
                lastResult = null,
                statusBannerMessage = null
            )
        }
    }

    fun selectTargetCharacter(template: KanaStrokeTemplate) {
        _uiState.update {
            it.copy(
                targetTemplate = template,
                templateStrokes = template.toTypedStrokes(),
                userStrokes = emptyList(),
                lastResult = null,
                statusBannerMessage = null
            )
        }
        audioProvider.speak(template.char, template.romaji)
    }

    fun setStrictness(level: StrictnessLevel) {
        _uiState.update { it.copy(strictnessLevel = level, statusBannerMessage = null) }
        if (_uiState.value.userStrokes.isNotEmpty()) {
            evaluateCurrentDrawing()
        }
    }

    fun addStroke(stroke: Stroke) {
        _uiState.update {
            it.copy(
                userStrokes = it.userStrokes + stroke,
                statusBannerMessage = null
            )
        }
    }

    fun undoLastStroke() {
        _uiState.update {
            if (it.userStrokes.isEmpty()) it
            else it.copy(
                userStrokes = it.userStrokes.dropLast(1),
                lastResult = null,
                statusBannerMessage = null
            )
        }
    }

    fun clearCanvas() {
        _uiState.update {
            it.copy(
                userStrokes = emptyList(),
                lastResult = null,
                statusBannerMessage = null
            )
        }
    }

    fun toggleGhostOverlay() {
        _uiState.update { it.copy(showGhostOverlay = !it.showGhostOverlay) }
    }

    fun toggleTemplateComparison() {
        _uiState.update { it.copy(showTemplateComparison = !it.showTemplateComparison) }
    }

    fun evaluateCurrentDrawing() {
        val state = _uiState.value
        val target = state.targetTemplate ?: return
        val engine = recognizer ?: return
        if (state.userStrokes.isEmpty()) {
            _uiState.update {
                it.copy(statusBannerMessage = "Hãy vẽ ít nhất 1 nét trước khi bấm Kiểm tra.")
            }
            return
        }

        val config = RecognizerConfig.forLevel(state.strictnessLevel)
        val result = engine.recognize(
            userStrokes = state.userStrokes,
            targetChar = target.char,
            config = config
        )
        _uiState.update {
            it.copy(
                lastResult = result,
                statusBannerMessage = null
            )
        }
    }

    fun saveCurrentSample() {
        val state = _uiState.value
        val target = state.targetTemplate ?: return
        if (state.userStrokes.isEmpty()) {
            _uiState.update {
                it.copy(statusBannerMessage = "Chưa có nét vẽ trên canvas để lưu mẫu.")
            }
            return
        }

        val engine = recognizer
        val config = RecognizerConfig.forLevel(state.strictnessLevel)
        val result = state.lastResult ?: engine?.recognize(state.userStrokes, target.char, config)

        viewModelScope.launch {
            val sample = SavedHandwritingSample(
                id = UUID.randomUUID().toString(),
                targetChar = target.char,
                romaji = target.romaji,
                strictness = state.strictnessLevel.name,
                createdAtEpochMs = System.currentTimeMillis(),
                userStrokes = state.userStrokes,
                recognitionResult = result
            )
            val totalSaved = sampleStorage.saveSample(sample)
            _uiState.update {
                it.copy(
                    lastResult = result,
                    savedSampleCount = totalSaved,
                    statusBannerMessage = "Đã lưu mẫu chữ '${target.char}' (#$totalSaved) vào JSON local."
                )
            }
        }
    }

    fun speakTarget() {
        val target = _uiState.value.targetTemplate ?: return
        audioProvider.speak(target.char, target.romaji)
    }

    companion object {
        fun provideFactory(
            strokeRepository: StrokeAssetRepository,
            sampleStorage: DebugSampleStorage,
            audioProvider: AudioProvider
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecognizerDebugViewModel(
                    strokeRepository = strokeRepository,
                    sampleStorage = sampleStorage,
                    audioProvider = audioProvider
                ) as T
            }
        }
    }
}
