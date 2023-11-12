package com.wikicoding.gaslogger.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    val idVehicle: Int = 0,
    var make: String,
    var model: String,
    var licensePlate: String,
    var startKm: Int,
    var registrationDate: String,
    var fuelType: String,
    var image: String
) : Serializable