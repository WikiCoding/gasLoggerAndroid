package com.wikicoding.gaslogger.activities

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wikicoding.explorelog.utils.SwipeToDeleteCallback
import com.wikicoding.explorelog.utils.SwipeToEditCallback
import com.wikicoding.gaslogger.R
import com.wikicoding.gaslogger.adapter.LogsAdapter
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
import java.util.function.DoubleToLongFunction
import kotlin.collections.ArrayList
import kotlin.math.round

class VehicleLogs : BaseActivity() {
    private var binding: ActivityVehicleLogsBinding? = null
    private var list: ArrayList<VehicleWithLogs>? = null
    private var logsList: List<LogEntity>? = null
    private var currentVehicle: VehicleEntity? = null
    private var calendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var actualKm: Int? = null
    private var fuelLiters: Double? = null
    private var pricePerLiter: Double? = null
    private var logDate: String? = null
    private var partialFillUpCheckBox: Boolean = false
    private var lastLog: LogEntity? = null
    private var adapter: LogsAdapter? = null
    private var lastFillKm = 0
    private var distanceTravelled = 0
    private var fuelConsumptionPer100Km = 0.0
    private var fillUpCost = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehicleLogsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        getIntentExtras()

        setupBackButton()

        getLogs(currentVehicle!!.idVehicle)

        handleEditSwipe()
        handleDeleteSwipe()

        // TODO: finish the excel export option
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu_logs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.mi_add_log -> insertLogDialog()
            R.id.mi_export_excel -> Toast.makeText(this, "not yet developed", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    private fun setupLogsRecyclerView(logsList: List<LogEntity>) {
        adapter = LogsAdapter(logsList)
        binding!!.rvLogs.layoutManager = LinearLayoutManager(this)
        binding!!.rvLogs.adapter = adapter

        if (logsList.isNotEmpty()) {
            binding!!.rvLogs.visibility = View.VISIBLE
            binding!!.tvEmptyLogs.visibility = View.INVISIBLE
        } else {
            binding!!.rvLogs.visibility = View.INVISIBLE
            binding!!.tvEmptyLogs.visibility = View.VISIBLE
        }
    }

    private fun getIntentExtras() {
        if (intent.hasExtra(Constants.SELECTED_VEHICLE_DATA)) {
            currentVehicle =
                intent.getSerializableExtra(Constants.SELECTED_VEHICLE_DATA) as VehicleEntity?
        }
    }

    private fun setupBackButton() {
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title =
            "${currentVehicle!!.make} ${currentVehicle!!.model} ${currentVehicle!!.licensePlate}"
    }

    private fun getLogs(idVehicle: Int) {
        lifecycleScope.launch {
            list = dao.fetchVehicleWithLogs(idVehicle) as ArrayList<VehicleWithLogs>

            if (list!![0].logs.isNotEmpty()) {
                sortLogsListByDateDESC()
            } else {
                logsList = listOf()
            }

            setupLogsRecyclerView(logsList!!)
        }
    }

    private fun sortLogsListByDateDESC() {
        logsList = list!![0].logs

        (logsList as ArrayList<LogEntity>).sortWith { log1, log2 ->
            val timestamp1 = dateToTimestamp(log1.logDate)
            val timestamp2 = dateToTimestamp(log2.logDate)
            when {
                timestamp1 > timestamp2 -> -1
                timestamp1 < timestamp2 -> 1
                else -> 0
            }
        }

        lastLog = logsList!![0]
    }

    private fun updateDateInView(dialogBinding: InsertNewLogDialogBinding) {
        val myFormat = "dd-MM-yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        dialogBinding.tvDateCalendar.setText(sdf.format(calendar.time).toString())
    }

    private fun insertLogDialog() {
        val insertDialog = Dialog(this, R.style.Theme_Dialog)
        //avoiding that clicking outside will not close the dialog or update data
        insertDialog.setCancelable(false)
        val dialogBinding = InsertNewLogDialogBinding.inflate(layoutInflater)
        insertDialog.setContentView(dialogBinding.root)
        getDates(dialogBinding)
        insertDialog.show()

        if (partialFillUpCheckBox) dialogBinding.checkboxPartialFillUp.isChecked = true

        listenToDateTextInputClick(dialogBinding)
        listenToPartialFillUpCheckBoxChanges(dialogBinding)

        dialogBinding.tvProceed.setOnClickListener {
            val newLog = setLogObjectFromFields(dialogBinding)
            insertLogToDatabase(newLog)

            getLogs(currentVehicle!!.idVehicle)
            insertDialog.dismiss()
            partialFillUpCheckBox = false
        }

        dialogBinding.tvCancel.setOnClickListener {
            insertDialog.dismiss()
            partialFillUpCheckBox = false
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

    private fun runCalculatedLogFields() {
        fillUpCost = round((fuelLiters!! * pricePerLiter!!) * 100.0) / 100.0

        if (logsList!!.isNotEmpty()) {
            lastFillKm = logsList!![0].currentKm
            distanceTravelled = actualKm!! - lastFillKm
            if (!partialFillUpCheckBox)
                fuelConsumptionPer100Km =
                    round(((100 * fuelLiters!!) / distanceTravelled)* 100.0) / 100.0
        }
    }

    private fun setLogObjectFromFields(dialogBinding: InsertNewLogDialogBinding): LogEntity {
        actualKm = dialogBinding.etKm.text.toString().toInt()
        fuelLiters = dialogBinding.etFuelLiters.text.toString().toDoubleOrNull()
        pricePerLiter = dialogBinding.etPricePerLiter.text.toString().toDoubleOrNull()
        logDate = dialogBinding.tvDateCalendar.text.toString()

        //TODO: validate fields

        runCalculatedLogFields()

        return LogEntity(0, actualKm!!, fuelLiters!!, pricePerLiter!!, logDate!!,
            partialFillUpCheckBox, lastFillKm, distanceTravelled, fuelConsumptionPer100Km,
            fillUpCost, currentVehicle!!.idVehicle)
    }

    private fun setLogObjectFromEditedFields(dialogBinding: InsertNewLogDialogBinding,
                                             currentLog: LogEntity): LogEntity {
        actualKm = dialogBinding.etKm.text.toString().toInt()
        fuelLiters = dialogBinding.etFuelLiters.text.toString().toDoubleOrNull()
        pricePerLiter = dialogBinding.etPricePerLiter.text.toString().toDoubleOrNull()
        logDate = dialogBinding.tvDateCalendar.text.toString()
        Log.e("currentLog", currentLog.toString())
        Log.e("partial", currentLog.toString())
        //TODO: validate fields

        runCalculatedLogFieldsForEdit(currentLog)

        return LogEntity(currentLog.id, actualKm!!, fuelLiters!!, pricePerLiter!!, logDate!!,
            partialFillUpCheckBox, lastFillKm, distanceTravelled, fuelConsumptionPer100Km,
            fillUpCost, currentVehicle!!.idVehicle)
    }

    private fun runCalculatedLogFieldsForEdit(currentLog: LogEntity) {
        val indexOfCurrentLog = logsList!!.indexOf(currentLog)

        //TODO: falta o unset do partial fill-up no edit!

        fillUpCost = round((fuelLiters!! * pricePerLiter!!) * 100.0) / 100.0

            lastFillKm = logsList!![indexOfCurrentLog + 1].currentKm
            distanceTravelled = actualKm!! - lastFillKm
            if (!partialFillUpCheckBox)
                fuelConsumptionPer100Km =
                    round(((100 * fuelLiters!!) / distanceTravelled)* 100.0) / 100.0
    }

    private fun listenToPartialFillUpCheckBoxChanges(dialogBinding: InsertNewLogDialogBinding) {
        dialogBinding.checkboxPartialFillUp.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) partialFillUpCheckBox = true
        }
    }

    private fun insertLogToDatabase(log: LogEntity) {
        lifecycleScope.launch {
            dao.insertLog(log)
        }
    }

    private fun handleEditSwipe() {
        val editItemSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val rvAdapter = binding!!.rvLogs.adapter as LogsAdapter
                val itemToEdit: LogEntity = rvAdapter.findSwipedItem(viewHolder.adapterPosition)

                editLogDialog(itemToEdit)
            }
        }

        val editItemTouchHandler = ItemTouchHelper(editItemSwipeHandler)
        editItemTouchHandler.attachToRecyclerView(binding!!.rvLogs)
    }

    private fun editLogDialog(logToEdit: LogEntity) {
        val editDialog = Dialog(this, R.style.Theme_Dialog)
        //avoiding that clicking outside will not close the dialog or update data
        editDialog.setCancelable(false)
        val dialogBinding = InsertNewLogDialogBinding.inflate(layoutInflater)
        editDialog.setContentView(dialogBinding.root)

        fillDialogFields(dialogBinding, logToEdit)

        editDialog.show()

        listenToDateTextInputClick(dialogBinding)

        dialogBinding.tvProceed.setOnClickListener {
            val newLog = setLogObjectFromEditedFields(dialogBinding, logToEdit)
            updateLogInDatabase(newLog)

            editDialog.dismiss()
            partialFillUpCheckBox = false
        }

        dialogBinding.tvCancel.setOnClickListener {
            getLogs(currentVehicle!!.idVehicle)
            editDialog.dismiss()
            partialFillUpCheckBox = false
        }
    }

    private fun updateLogInDatabase(log: LogEntity) {
        lifecycleScope.launch {
            dao.updateLog(log)
            getLogs(currentVehicle!!.idVehicle)
        }
    }

    private fun fillDialogFields(dialogBinding: InsertNewLogDialogBinding, logToEdit: LogEntity) {
        dialogBinding.etPricePerLiter.setText(logToEdit.pricePerLiter.toString())
        dialogBinding.etKm.setText(logToEdit.currentKm.toString())
        dialogBinding.etFuelLiters.setText(logToEdit.fuelLiters.toString())
        dialogBinding.tvDateCalendar.setText(logToEdit.logDate)
        partialFillUpCheckBox = logToEdit.partialFillUp

        Log.e("log from db", logToEdit.partialFillUp.toString())

        if (partialFillUpCheckBox) dialogBinding.checkboxPartialFillUp.isChecked = true
    }

    private fun handleDeleteSwipe() {
        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val rvAdapter = binding!!.rvLogs.adapter as LogsAdapter
                val itemToDelete: LogEntity = rvAdapter.findSwipedItem(viewHolder.adapterPosition)
                deleteConfirmationDialog(itemToDelete)
            }
        }

        val deleteItemTouchHandler = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHandler.attachToRecyclerView(binding!!.rvLogs)
    }

    private fun deleteLog(log: LogEntity) {
        lifecycleScope.launch {
            dao.deleteLog(log)
            getLogs(currentVehicle!!.idVehicle)
        }
    }

    private fun deleteConfirmationDialog(log: LogEntity) {
        val deleteConfirmationDialog = Dialog(this, R.style.Theme_Dialog)
        //avoiding that clicking outside will not close the dialog or update data
        deleteConfirmationDialog.setCancelable(false)
        val dialogBinding = DeleteConfirmationDialogBinding.inflate(layoutInflater)
        deleteConfirmationDialog.setContentView(dialogBinding.root)
        deleteConfirmationDialog.show()

        dialogBinding.tvProceed.setOnClickListener {
            deleteLog(log)
            deleteConfirmationDialog.dismiss()
        }

        dialogBinding.tvCancel.setOnClickListener {
            deleteConfirmationDialog.dismiss()
            getLogs(currentVehicle!!.idVehicle)
        }
    }

    override fun onDestroy() {
        binding = null

        super.onDestroy()
    }
}