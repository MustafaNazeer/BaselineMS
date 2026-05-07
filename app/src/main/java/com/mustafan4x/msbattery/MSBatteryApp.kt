package com.mustafan4x.msbattery

import android.app.Application
import androidx.room.Room
import com.mustafan4x.msbattery.data.AppDatabase

class MSBatteryApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "msbattery.db"
        ).build()
    }
}
