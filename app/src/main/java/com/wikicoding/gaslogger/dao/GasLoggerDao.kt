package com.wikicoding.gaslogger.dao

import androidx.room.*
import com.wikicoding.gaslogger.activities.VehicleLogs
import com.wikicoding.gaslogger.model.LogEntity
import com.wikicoding.gaslogger.model.VehicleEntity
import com.wikicoding.gaslogger.model.relations.VehicleWithLogs

@Dao
interface GasLoggerDao {
    @Query("SELECT * FROM vehicles ORDER BY make ASC")
    suspend fun fetchAllVehicles(): List<VehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addVehicle(vehicle: VehicleEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateVehicle(vehicle: VehicleEntity)

    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)

    @Transaction // to execute in a thread safe manner
    @Query("SELECT * FROM vehicles WHERE idVehicle= :idVehicle")
    suspend fun fetchVehicleWithLogs(idVehicle: Int): List<VehicleWithLogs>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)
}