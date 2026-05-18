package com.mustafanazeer.baselinems

import android.app.Application
import androidx.room.Room
import com.mustafanazeer.baselinems.data.AppDatabase
import com.mustafanazeer.baselinems.data.MIGRATION_1_2
import com.mustafanazeer.baselinems.data.MIGRATION_2_3
import com.mustafanazeer.baselinems.signals.AndroidImuSource

class BaselineMSApp : Application() {

    lateinit var database: AppDatabase
        private set

    /**
     * Singleton sensor source for the gait test. Constructed once per process so the
     * `SensorManager` listener registrations live across activity recreations. The sensor source
     * does not register listeners until `start()` is called by the gait view model, so holding
     * this singleton has no battery cost outside an active capture.
     */
    val imuSource: AndroidImuSource by lazy { AndroidImuSource(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "baselinems.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
