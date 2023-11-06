package com.wikicoding.gaslogger.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var currentKm: Int,
    var fuelLiters: Double,
    var pricePerLiter: Double,
    val logDate: String,
    val idVehicle: Int // foreign key
) : Serializable