package vn.kana.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey
    val rowId: String,          // Ví dụ: "hiragana_あ行", "hiragana_か行"
    val script: String,         // "hiragana" | "katakana"
    val rowName: String,        // "あ行", "か行"
    val completed: Boolean,
    val bestQuizScore: Int,
    val completedAtEpochMs: Long
)

@Entity(tableName = "character_progress")
data class CharacterProgressEntity(
    @PrimaryKey
    val char: String,           // Ví dụ: "あ", "か"
    val script: String,
    val rowName: String,
    val learned: Boolean,
    val lastScore: Int,
    val practiceCount: Int,
    val errorCount: Int,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "srs_cards")
data class SrsCardEntity(
    @PrimaryKey
    val cardId: String,         // Ví dụ: "CHAR_あ" hoặc "WORD_hira_001"
    val cardType: String,       // "CHAR" | "WORD"
    val targetKey: String,      // "あ" hoặc "hira_001"
    val script: String,
    val repetition: Int,
    val intervalDays: Int,
    val easeFactor: Float,
    val dueAtEpochMs: Long,
    val lastScore: Int,
    val lapses: Int
)

@Dao
interface KanaDao {
    @Query("SELECT * FROM lesson_progress")
    fun observeLessonProgress(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM character_progress")
    fun observeCharacterProgress(): Flow<List<CharacterProgressEntity>>

    @Query("SELECT * FROM character_progress WHERE learned = 1")
    suspend fun getLearnedCharacters(): List<CharacterProgressEntity>

    @Query("SELECT * FROM character_progress WHERE char = :kanaChar LIMIT 1")
    suspend fun getCharacterProgress(kanaChar: String): CharacterProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCharacterProgress(entity: CharacterProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(entity: LessonProgressEntity)

    @Query("SELECT * FROM srs_cards")
    fun observeSrsCards(): Flow<List<SrsCardEntity>>

    @Query("SELECT * FROM srs_cards WHERE cardId = :cardId LIMIT 1")
    suspend fun getSrsCard(cardId: String): SrsCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSrsCard(card: SrsCardEntity)

    @Query("DELETE FROM lesson_progress")
    suspend fun clearLessonProgress()

    @Query("DELETE FROM character_progress")
    suspend fun clearCharacterProgress()

    @Query("DELETE FROM srs_cards")
    suspend fun clearSrsCards()
}

@Database(
    entities = [
        LessonProgressEntity::class,
        CharacterProgressEntity::class,
        SrsCardEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KanaDatabase : RoomDatabase() {
    abstract fun kanaDao(): KanaDao

    companion object {
        @Volatile
        private var INSTANCE: KanaDatabase? = null

        fun getInstance(context: Context): KanaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KanaDatabase::class.java,
                    "kana_progress.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
