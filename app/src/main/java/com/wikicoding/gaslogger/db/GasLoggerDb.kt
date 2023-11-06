package com.wikicoding.gaslogger.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wikicoding.gaslogger.dao.GasLoggerDao
import com.wikicoding.gaslogger.model.LogEntity
import com.wikicoding.gaslogger.model.VehicleEntity

@Database(entities = [VehicleEntity::class, LogEntity::class], version = 1)
abstract class GasLoggerDb : RoomDatabase() {

    abstract fun gasLoggerDao(): GasLoggerDao

    companion object {
        @Volatile
        private var INSTANCE: GasLoggerDb? = null

        fun getInstance(context: Context): GasLoggerDb {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        GasLoggerDb::class.java,
                        "gas_logger_db"
                    ).fallbackToDestructiveMigration().build()

                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}