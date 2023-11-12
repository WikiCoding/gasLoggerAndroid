package com.wikicoding.gaslogger.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.wikicoding.gaslogger.model.LogEntity
import com.wikicoding.gaslogger.model.VehicleEntity
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

// TODO: Modularize this
class ExcelExportCallback (private val context: Context,
                           private val list: List<LogEntity>,
                           private val vehicle: VehicleEntity) {
    fun exportExcel() {
        /** creating workbook and sheet **/
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet(WORKBOOK_NAME)

        /** writing the data to the sheet **/
        var row = sheet.createRow(0)
        row.createCell(0).setCellValue(COLUMN_ID)
        row.createCell(1).setCellValue(COLUMN_CURRENT_KM)
        row.createCell(2).setCellValue(COLUMN_LOG_DATE)
        row.createCell(3).setCellValue(COLUMN_FUEL_LITERS)
        row.createCell(4).setCellValue(COLUMN_PRICE_PER_LITER)
        row.createCell(5).setCellValue(COLUMN_PARTIAL_FILL_UP)
        row.createCell(6).setCellValue(COLUMN_LAST_FILL_KM)
        row.createCell(7).setCellValue(COLUMN_DISTANCE_TRAVELLED)
        row.createCell(8).setCellValue(COLUMN_FUEL_CONSUMPTION)
        row.createCell(9).setCellValue(COLUMN_FILL_UP_COST)
        row.createCell(10).setCellValue(COLUMN_VEHICLE_ID)
        row.createCell(11).setCellValue(COLUMN_VEHICLE_MAKE)
        row.createCell(12).setCellValue(COLUMN_VEHICLE_MODEL)
        row.createCell(13).setCellValue(COLUMN_VEHICLE_LICENSE_PLATE)

        // filling up rows with data
        for (i in list.indices) {
            val log = list[i]
            row = sheet.createRow(i + 1)
            row.createCell(0).setCellValue(log.id.toString())
            row.createCell(1).setCellValue(log.currentKm.toString())
            row.createCell(2).setCellValue(log.logDate)
            row.createCell(3).setCellValue(log.fuelLiters.toString())
            row.createCell(4).setCellValue(log.pricePerLiter.toString())
            row.createCell(5).setCellValue(log.partialFillUp.toString())
            row.createCell(6).setCellValue(log.lastFillKm.toString())
            row.createCell(7).setCellValue(log.distanceTravelled.toString())
            row.createCell(8).setCellValue(log.fuelConsumption.toString())
            row.createCell(9).setCellValue(log.fillUpCost.toString())
            row.createCell(10).setCellValue(log.idVehicle.toString())
            row.createCell(11).setCellValue(vehicle.make)
            row.createCell(12).setCellValue(vehicle.model)
            row.createCell(13).setCellValue(vehicle.licensePlate)
        }

        /** Saving the data to a file **/
        //val exportDir = File(Environment.getStorageDirectory(), "TasksAppData")
        val exportDir = context.getExternalFilesDir(null)

        // TODO: Delete this
        Log.e("Storage", exportDir!!.absolutePath)
        Log.e("List", list.toString())


        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, "LogsFrom_${vehicle.make}_${vehicle.model}_${vehicle.licensePlate}.xls")
        Log.e("Storage", file.toString())
        try {
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            workbook.close()
            outputStream.close()
            Toast.makeText(context, "Exported to ${exportDir.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "There was an error: $e", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    companion object {
        const val WORKBOOK_NAME = "VehicleLogs"
        const val COLUMN_ID = "id"
        const val COLUMN_CURRENT_KM = "currentKm"
        const val COLUMN_FUEL_LITERS = "fuelLiters"
        const val COLUMN_PRICE_PER_LITER = "pricePerLiter"
        const val COLUMN_LOG_DATE = "fillUpDate"
        const val COLUMN_PARTIAL_FILL_UP = "partialFillUp"
        const val COLUMN_LAST_FILL_KM = "lastFillKm"
        const val COLUMN_DISTANCE_TRAVELLED = "distanceTravelled"
        const val COLUMN_FUEL_CONSUMPTION = "fuelConsumption"
        const val COLUMN_FILL_UP_COST = "fillUpCost"
        const val COLUMN_VEHICLE_ID = "idVehicle"
        const val COLUMN_VEHICLE_MAKE = "vehicleMake"
        const val COLUMN_VEHICLE_MODEL = "vehicleModel"
        const val COLUMN_VEHICLE_LICENSE_PLATE = "vehicleLicensePlate"
    }
}