package com.mustafan4x.msbattery

import android.app.Application
import androidx.room.Room
import com.mustafan4x.msbattery.data.AppDatabase
import com.mustafan4x.msbattery.data.MIGRATION_1_2
import com.mustafan4x.msbattery.signals.AndroidImuSource

class MSBatteryApp : Application() {

    lateinit var database: AppDatabase
        private set

    /**
     * Singleton sensor source for the gait test (Phase 4). Constructed once per process so the
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
            "msbattery.db"
        ).addMigrations(MIGRATION_1_2).build()
    }
}
