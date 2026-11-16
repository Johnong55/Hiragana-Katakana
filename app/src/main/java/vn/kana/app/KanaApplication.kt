package vn.kana.app

import android.app.Application
import vn.kana.app.data.DebugSampleStorage
import vn.kana.app.data.StrokeAssetRepository
import vn.kana.app.data.UserSettingsRepository
import vn.kana.app.data.VocabAssetRepository
import vn.kana.app.data.local.KanaDatabase
import vn.kana.app.domain.AndroidAudioProvider
import vn.kana.app.domain.AudioProvider

class KanaApplication : Application() {
    lateinit var strokeRepository: StrokeAssetRepository
        private set

    lateinit var vocabRepository: VocabAssetRepository
        private set

    lateinit var settingsRepository: UserSettingsRepository
        private set

    lateinit var database: KanaDatabase
        private set

    lateinit var debugSampleStorage: DebugSampleStorage
        private set

    lateinit var audioProvider: AudioProvider
        private set

    override fun onCreate() {
        super.onCreate()
        strokeRepository = StrokeAssetRepository(applicationContext)
        vocabRepository = VocabAssetRepository(applicationContext)
        settingsRepository = UserSettingsRepository(applicationContext)
        database = KanaDatabase.getInstance(applicationContext)
        debugSampleStorage = DebugSampleStorage(applicationContext)
        audioProvider = AndroidAudioProvider(applicationContext)
    }

    override fun onTerminate() {
        audioProvider.release()
        super.onTerminate()
    }
}
