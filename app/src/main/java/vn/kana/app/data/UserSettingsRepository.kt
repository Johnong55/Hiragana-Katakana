package vn.kana.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import vn.kana.recognizer.StrictnessLevel
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kana_settings")

/**
 * Chế độ giao diện Sáng / Tối / Theo hệ thống.
 */
enum class ThemeMode(val displayNameVi: String) {
    SYSTEM("Theo máy"),
    LIGHT("Sáng"),
    DARK("Tối")
}

data class UserSettings(
    val strictnessLevel: StrictnessLevel = StrictnessLevel.EASY,
    val animationSpeed: Float = 1.0f,
    val autoAdvanceDelayMs: Long = 700L,
    val soundEnabled: Boolean = true,
    val unlockAllRows: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val streakDays: Int = 0,
    val lastStudyEpochDay: Long = -1L
)

/**
 * Lưu trữ cài đặt người dùng, chế độ Sáng/Tối và chuỗi ngày học (Streak đơn giản) bằng Jetpack DataStore Preferences.
 */
class UserSettingsRepository(
    private val appContext: Context
) {
    private object Keys {
        val STRICTNESS = stringPreferencesKey("strictness_level")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val AUTO_ADVANCE_DELAY_MS = longPreferencesKey("auto_advance_delay_ms")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val UNLOCK_ALL_ROWS = booleanPreferencesKey("unlock_all_rows")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_STUDY_EPOCH_DAY = longPreferencesKey("last_study_epoch_day")
    }

    val settingsFlow: Flow<UserSettings> = appContext.dataStore.data.map { prefs ->
        val strictnessStr = prefs[Keys.STRICTNESS] ?: StrictnessLevel.EASY.name
        val strictness = StrictnessLevel.entries.firstOrNull { it.name == strictnessStr }
            ?: StrictnessLevel.EASY

        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = ThemeMode.entries.firstOrNull { it.name == themeStr }
            ?: ThemeMode.SYSTEM

        val savedStreak = prefs[Keys.STREAK_DAYS] ?: 0
        val lastEpochDay = prefs[Keys.LAST_STUDY_EPOCH_DAY] ?: -1L
        val todayEpochDay = LocalDate.now().toEpochDay()
        val effectiveStreak = if (lastEpochDay >= 0 && todayEpochDay - lastEpochDay > 1L) {
            0
        } else {
            savedStreak
        }

        UserSettings(
            strictnessLevel = strictness,
            animationSpeed = prefs[Keys.ANIMATION_SPEED] ?: 1.0f,
            autoAdvanceDelayMs = prefs[Keys.AUTO_ADVANCE_DELAY_MS] ?: 700L,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            unlockAllRows = prefs[Keys.UNLOCK_ALL_ROWS] ?: false,
            themeMode = themeMode,
            streakDays = effectiveStreak,
            lastStudyEpochDay = lastEpochDay
        )
    }

    suspend fun setStrictnessLevel(level: StrictnessLevel) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.STRICTNESS] = level.name
        }
    }

    suspend fun setAnimationSpeed(speed: Float) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.ANIMATION_SPEED] = speed.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun setAutoAdvanceDelayMs(delayMs: Long) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.AUTO_ADVANCE_DELAY_MS] = delayMs.coerceIn(300L, 2500L)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setUnlockAllRows(unlockAll: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.UNLOCK_ALL_ROWS] = unlockAll
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun recordStudyActivityToday() {
        val today = LocalDate.now().toEpochDay()
        appContext.dataStore.edit { prefs ->
            val lastDay = prefs[Keys.LAST_STUDY_EPOCH_DAY] ?: -1L
            val currentStreak = prefs[Keys.STREAK_DAYS] ?: 0
            val nextStreak = when {
                lastDay == today -> currentStreak.coerceAtLeast(1)
                lastDay == today - 1L -> currentStreak + 1
                else -> 1
            }
            prefs[Keys.STREAK_DAYS] = nextStreak
            prefs[Keys.LAST_STUDY_EPOCH_DAY] = today
        }
    }

    suspend fun resetStreak() {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.STREAK_DAYS] = 0
            prefs[Keys.LAST_STUDY_EPOCH_DAY] = -1L
        }
    }
}
