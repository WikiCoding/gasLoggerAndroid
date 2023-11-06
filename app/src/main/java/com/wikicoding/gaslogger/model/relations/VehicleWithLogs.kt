package com.wikicoding.gaslogger.model.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.wikicoding.gaslogger.activities.VehicleLogs
import com.wikicoding.gaslogger.model.LogEntity
import com.wikicoding.gaslogger.model.VehicleEntity

data class VehicleWithLogs(
    @Embedded val vehicles: VehicleEntity, // for the main table that all the others will be linked to as 1 to n relationship
    @Relation(
        parentColumn = "idVehicle", // PK from the VehicleEntity table
        entityColumn = "idVehicle" // FK from the LogEntity table
    )
    val logs: List<LogEntity>
)
