package com.wikicoding.gaslogger.activities

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.wikicoding.gaslogger.R
import com.wikicoding.gaslogger.constants.Constants
import com.wikicoding.gaslogger.databinding.ActivityVehicleLogsBinding
import com.wikicoding.gaslogger.databinding.DeleteConfirmationDialogBinding
import com.wikicoding.gaslogger.databinding.InsertNewLogDialogBinding
import com.wikicoding.gaslogger.model.LogEntity
import com.wikicoding.gaslogger.model.VehicleEntity
import com.wikicoding.gaslogger.model.relations.VehicleWithLogs
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class VehicleLogs : BaseActivity() {
    private var binding: ActivityVehicleLogsBinding? = null
    private var list: ArrayList<VehicleWithLogs>? = null
    private var logsList: ArrayList<LogEntity>? = null
    private var currentVehicle: VehicleEntity? = null
    private var calendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var actualKm: Int? = null
    private var fuelLiters: Double? = null
    private var pricePerLiter: Double? = null
    private var logDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehicleLogsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        if (intent.hasExtra(Constants.SELECTED_VEHICLE_DATA)) {
            currentVehicle = intent.getSerializableExtra(Constants.SELECTED_VEHICLE_DATA) as VehicleEntity?
        }

        supportActionBar!!.title =
            "${currentVehicle!!.make} ${currentVehicle!!.model} ${currentVehicle!!.licensePlate}"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        binding!!.tvVehicleLogs.text = "Under development"

        getLogs(currentVehicle!!.idVehicle)

        binding!!.btnAddLog.setOnClickListener {
            //val newLog = LogEntity(0, 391000, 50, 1.78, System.currentTimeMillis(), currentVehicle!!.idVehicle)
//            lifecycleScope.launch {
//                dao.insertLog(newLog)
//            }
            insertLogDialog(currentVehicle!!)
        }

        // TODO: create recyclerview adapter
        // TODO: move the button to add log to the toolbar
        // TODO: add menu to the toolbar with excel export option
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun getLogs(idVehicle: Int) {
        lifecycleScope.launch {
            list = dao.fetchVehicleWithLogs(idVehicle) as ArrayList<VehicleWithLogs>
            Log.e("logs full list", list.toString())

            if (list!![0].logs.isNotEmpty()) {
                Log.e("logs", list!![0].logs[1].toString())
            }

            logsList = list!![0].logs as ArrayList<LogEntity>

            println(logsList)
        }
    }

    private fun updateDateInView(dialogBinding: InsertNewLogDialogBinding) {
        val myFormat = "dd-MM-yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        dialogBinding.tvDateCalendar.setText(sdf.format(calendar.time).toString())
    }

    private fun insertLogDialog(vehicle: VehicleEntity) {
        val insertDialog = Dialog(this, R.style.Theme_Dialog)
        //avoiding that clicking outside will not close the dialog or update data
        insertDialog.setCancelable(false)
        val dialogBinding = InsertNewLogDialogBinding.inflate(layoutInflater)
        insertDialog.setContentView(dialogBinding.root)
        getDates(dialogBinding)
        insertDialog.show()

        listenToDateTextInputClick(dialogBinding)

        dialogBinding.tvProceed.setOnClickListener {
            val newLog = setLogFormFields(dialogBinding)
            insertNewLog(newLog)
            insertDialog.dismiss()
        }

        dialogBinding.tvCancel.setOnClickListener {
            insertDialog.dismiss()
        }
    }

    private fun getDates(dialogBinding: InsertNewLogDialogBinding) {
        dateSetListener = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView(dialogBinding)
        }

        /** to fill automatically when opening the activity**/
        updateDateInView(dialogBinding)
    }

    private fun listenToDateTextInputClick(dialogBinding: InsertNewLogDialogBinding) {
        dialogBinding.tvDateCalendar.setOnClickListener {
            DatePickerDialog(
                this@VehicleLogs, dateSetListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setLogFormFields(dialogBinding: InsertNewLogDialogBinding): LogEntity {
        actualKm = dialogBinding.etKm.text.toString().toInt()
        fuelLiters = dialogBinding.etFuelLiters.text.toString().toDoubleOrNull()
        pricePerLiter = dialogBinding.etPricePerLiter.text.toString().toDoubleOrNull()
        logDate = dialogBinding.tvDateCalendar.text.toString()

        //TODO: validate fields

        return LogEntity(0, actualKm!!, fuelLiters!!, pricePerLiter!!, logDate!!,
            currentVehicle!!.idVehicle)
    }

    private fun insertNewLog(log: LogEntity) {
        lifecycleScope.launch {
            dao.insertLog(log)
            // TODO: add adapter notify item changed
        }
    }

    override fun onDestroy() {
        binding = null

        super.onDestroy()
    }
}