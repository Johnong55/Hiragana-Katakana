package vn.kana.app.ui.phase0

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.kana.app.data.StrokeAssetRepository
import vn.kana.app.domain.AudioAvailability
import vn.kana.app.domain.AudioProvider
import vn.kana.recognizer.KanaStrokeTemplate
import vn.kana.recognizer.Stroke

data class StrokePreviewUiState(
    val isLoading: Boolean = true,
    val characters: List<KanaStrokeTemplate> = emptyList(),
    val selectedTemplate: KanaStrokeTemplate? = null,
    val typedStrokes: List<Stroke> = emptyList(),
    val isPlaying: Boolean = true,
    val isLooping: Boolean = true,
    val speedMultiplier: Float = 1.0f,
    val showStrokeNumbers: Boolean = true,
    val showDirectionArrows: Boolean = true,
    val restartTrigger: Int = 0
)

class StrokePreviewViewModel(
    private val strokeRepository: StrokeAssetRepository,
    private val audioProvider: AudioProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrokePreviewUiState())
    val uiState: StateFlow<StrokePreviewUiState> = _uiState.asStateFlow()

    val audioAvailability: StateFlow<AudioAvailability> = audioProvider.availability

    init {
        loadCharacters()
    }

    private fun loadCharacters() {
        viewModelScope.launch {
            val allChars = strokeRepository.getAllCharacters()
            val defaultTemplate = allChars.firstOrNull { it.char == "あ" } ?: allChars.firstOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    characters = allChars,
                    selectedTemplate = defaultTemplate,
                    typedStrokes = defaultTemplate?.toTypedStrokes().orEmpty()
                )
            }
            defaultTemplate?.let {
                audioProvider.speak(it.char, it.romaji)
            }
        }
    }

    fun selectCharacter(template: KanaStrokeTemplate) {
        _uiState.update {
            it.copy(
                selectedTemplate = template,
                typedStrokes = template.toTypedStrokes(),
                isPlaying = true,
                restartTrigger = it.restartTrigger + 1
            )
        }
        audioProvider.speak(template.char, template.romaji)
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun replayFromStart() {
        _uiState.update {
            it.copy(
                isPlaying = true,
                restartTrigger = it.restartTrigger + 1
            )
        }
        _uiState.value.selectedTemplate?.let {
            audioProvider.speak(it.char, it.romaji)
        }
    }

    fun setAnimationFinished() {
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun setSpeed(speed: Float) {
        _uiState.update { it.copy(speedMultiplier = speed) }
    }

    fun toggleLoop() {
        _uiState.update { it.copy(isLooping = !it.isLooping) }
    }

    fun toggleStrokeHints() {
        _uiState.update {
            val next = !it.showStrokeNumbers
            it.copy(
                showStrokeNumbers = next,
                showDirectionArrows = next
            )
        }
    }

    fun speakCurrentCharacter() {
        val current = _uiState.value.selectedTemplate ?: return
        audioProvider.speak(current.char, current.romaji)
    }

    fun refreshTtsCheck() {
        audioProvider.refreshVoiceCheck()
    }

    companion object {
        fun provideFactory(
            strokeRepository: StrokeAssetRepository,
            audioProvider: AudioProvider
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StrokePreviewViewModel(strokeRepository, audioProvider) as T
            }
        }
    }
}
