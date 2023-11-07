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
    var logDate: String,
    var partialFillUp: Boolean,
    //calculated values to keep track of
    var lastFillKm: Int,
    var distanceTravelled: Int,
    var fuelConsumption: Double,
    var fillUpCost: Double,
    // foreign key
    var idVehicle: Int
) : Serializable