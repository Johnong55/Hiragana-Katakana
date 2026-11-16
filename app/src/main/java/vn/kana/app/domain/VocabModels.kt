package vn.kana.app.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KanaVocabWord(
    val id: String = "",
    val word: String,
    val romaji: String,
    @SerialName("meaning_vi")
    val meaningVi: String,
    val chars: List<String>,
    val introduces: String,
    val script: String = "hiragana"
)

@Serializable
data class KanaVocabDatabase(
    val totalWords: Int,
    val words: List<KanaVocabWord>
)
