package com.wikicoding.gaslogger.utils

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.wikicoding.gaslogger.model.VehicleEntity
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream

class VehiclesExcelExportCallback(private val context: Context,
                                  private val list: List<VehicleEntity>) {
    private lateinit var row: HSSFRow
    private lateinit var sheet: HSSFSheet
    private lateinit var exportDir: File
    private lateinit var workbook: HSSFWorkbook

    fun exportExcel(): File {
        /** creating workbook and sheet **/
        workbook = HSSFWorkbook()
        sheet = workbook.createSheet(WORKBOOK_NAME)

        /** writing the data to the sheet **/
        row = sheet.createRow(0)
        createExcelColumns()

        // filling up rows with data
        fillColumnsWithCurrentData()

        return saveExcelToDirectory(context)
    }

    private fun saveExcelToDirectory(context: Context): File {
//        exportDir = context.getExternalFilesDir(null)!!
        exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)!!

        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        println(exportDir)

        val file =
            File(exportDir, "VehiclesList_${System.currentTimeMillis()}.xls")

        try {
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)
            workbook.close()
            outputStream.close()
            Toast.makeText(context, "Exported to ${exportDir.absolutePath}", Toast.LENGTH_LONG)
                .show()
        } catch (e: Exception) {
            Toast.makeText(context, "There was an error: $e", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

        return file
    }

    private fun fillColumnsWithCurrentData() {
        for (i in list.indices) {
            val vehicle = list[i]
            row = sheet.createRow(i + 1)
            row.createCell(0).setCellValue(vehicle.idVehicle.toString())
            row.createCell(1).setCellValue(vehicle.make)
            row.createCell(2).setCellValue(vehicle.model)
            row.createCell(3).setCellValue(vehicle.licensePlate)
            row.createCell(4).setCellValue(vehicle.startKm.toString())
            row.createCell(5).setCellValue(vehicle.registrationDate)
            row.createCell(6).setCellValue(vehicle.fuelType)
            row.createCell(7).setCellValue(vehicle.image)
        }
    }

    private fun createExcelColumns() {
        row.createCell(0).setCellValue(COLUMN_ID)
        row.createCell(1).setCellValue(COLUMN_MAKE)
        row.createCell(2).setCellValue(COLUMN_START_KM)
        row.createCell(3).setCellValue(COLUMN_MODEL)
        row.createCell(4).setCellValue(COLUMN_LICENSE_PLATE)
        row.createCell(5).setCellValue(COLUMN_REGISTRATION_DATE)
        row.createCell(6).setCellValue(COLUMN_FUEL_TYPE)
        row.createCell(7).setCellValue(COLUMN_IMAGE)
    }

    companion object {
        const val WORKBOOK_NAME = "VehiclesList"
        const val COLUMN_ID = "idVehicle"
        const val COLUMN_MAKE = "make"
        const val COLUMN_MODEL = "model"
        const val COLUMN_LICENSE_PLATE = "licensePlate"
        const val COLUMN_START_KM = "startKm"
        const val COLUMN_REGISTRATION_DATE = "registrationDate"
        const val COLUMN_FUEL_TYPE = "fuelType"
        const val COLUMN_IMAGE = "image"
    }
}