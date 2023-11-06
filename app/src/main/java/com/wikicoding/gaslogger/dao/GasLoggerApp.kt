package com.wikicoding.gaslogger.dao

import android.app.Application
import com.wikicoding.gaslogger.db.GasLoggerDb

class GasLoggerApp : Application() {
    val db by lazy {
        GasLoggerDb.getInstance(this)
    }
}