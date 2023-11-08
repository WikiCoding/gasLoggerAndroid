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
import kotlin.collections.ArrayList
import kotlin.math.round

//TODO: Fix Dialog theme for dark mode
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
    private var previousLogDate = ""

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
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.mi_add_log -> insertLogDialog()
            R.id.mi_export_excel -> Toast.makeText(this, "not yet developed", Toast.LENGTH_SHORT)
                .show()
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

    private fun setDateInDialogWhenShows(dialogBinding: InsertNewLogDialogBinding) {
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

        listenToDateInputClick(dialogBinding)
        listenToCheckBoxChanges(dialogBinding)

        listenToInsertLogDialogButtons(dialogBinding, insertDialog)
    }

    private fun listenToInsertLogDialogButtons(
        dialogBinding: InsertNewLogDialogBinding, insertDialog: Dialog) {
        dialogBinding.tvProceed.setOnClickListener {
            val newLog = setLogObjectFromDialogFields(dialogBinding) ?: return@setOnClickListener
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
            setDateInDialogWhenShows(dialogBinding)
        }

        /** to fill automatically when opening the activity**/
        setDateInDialogWhenShows(dialogBinding)
    }

    private fun listenToDateInputClick(dialogBinding: InsertNewLogDialogBinding) {
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
                    round(((100 * fuelLiters!!) / distanceTravelled) * 100.0) / 100.0
        }
    }

    private fun setLogObjectFromDialogFields(dialogBinding: InsertNewLogDialogBinding): LogEntity? {
        if (dialogBinding.etKm.text.isEmpty()) {
            Toast.makeText(this, "Please provide km input!", Toast.LENGTH_SHORT).show()
            return null
        }
        actualKm = dialogBinding.etKm.text.toString().toInt()
        fuelLiters = dialogBinding.etFuelLiters.text.toString().toDoubleOrNull()
        pricePerLiter = dialogBinding.etPricePerLiter.text.toString().toDoubleOrNull()
        logDate = dialogBinding.tvDateCalendar.text.toString()

        val validationResultCode = validateDialogInputFields(actualKm, fuelLiters, pricePerLiter, logDate)

        showErrorMessages(validationResultCode)

        if (validationResultCode < 0) {
            showErrorMessages(validationResultCode)
            return null
        }

        runCalculatedLogFields()

        return LogEntity(
            0, actualKm!!, fuelLiters!!, pricePerLiter!!, logDate!!,
            partialFillUpCheckBox, lastFillKm, distanceTravelled, fuelConsumptionPer100Km,
            fillUpCost, currentVehicle!!.idVehicle
        )
    }

    private fun setLogObjectFromEditedDialogFields(
        dialogBinding: InsertNewLogDialogBinding, selectedLogToEdit: LogEntity): LogEntity? {
        if (dialogBinding.etKm.text.isEmpty()) {
            Toast.makeText(this, "Please provide km input!", Toast.LENGTH_SHORT).show()
            return null
        }

        actualKm = dialogBinding.etKm.text.toString().toInt()
        fuelLiters = dialogBinding.etFuelLiters.text.toString().toDoubleOrNull()
        pricePerLiter = dialogBinding.etPricePerLiter.text.toString().toDoubleOrNull()
        logDate = dialogBinding.tvDateCalendar.text.toString()

        val validationResultCode =
            validateEditDialogInputFields(actualKm, fuelLiters, pricePerLiter, logDate, selectedLogToEdit)

        if (validationResultCode < 0) {
            showErrorMessages(validationResultCode)
            return null
        }

        runCalculatedLogFieldsOfEdit(selectedLogToEdit)

        return LogEntity(
            selectedLogToEdit.id, actualKm!!, fuelLiters!!, pricePerLiter!!, logDate!!,
            partialFillUpCheckBox, lastFillKm, distanceTravelled, fuelConsumptionPer100Km,
            fillUpCost, currentVehicle!!.idVehicle
        )
    }

    private fun validateDialogInputFields(actualKm: Int?, fuelLiters: Double?,
                                              pricePerLiter: Double?, logDate: String?): Int {
        //no need to check for < 0 condition since I only accept positive numbers in the UI
        if (logsList!!.isNotEmpty()) {
            val providedDate = dateToTimestamp(logDate!!)
            val latestDate = dateToTimestamp(logsList!![0].logDate)
            if (providedDate < latestDate) return -5
            if (actualKm == null) return -1
            if (actualKm <= logsList!![0].currentKm) return -2
        }

        if (fuelLiters == null) return -3
        if (pricePerLiter == null) return -4

        return 0
    }

    private fun validateEditDialogInputFields(actualKm: Int?, fuelLiters: Double?,
                                              pricePerLiter: Double?, logDate: String?,
                                              currentLog: LogEntity): Int {
        //no need to check for < 0 condition since I only accept positive numbers in the UI
        if (actualKm == null) return -1
        if (logsList!!.isEmpty()) {
            val latestIndex = logsList!!.indexOf(currentLog) - 1
            val providedDate = dateToTimestamp(logDate!!)
            val latestDate = dateToTimestamp(logsList!![latestIndex].logDate)
            if (actualKm <= logsList!![latestIndex].currentKm) return -2
            if (providedDate < latestDate) return -5
        }

        if (fuelLiters == null) return -3
        if (pricePerLiter == null) return -4

        return 0
    }

    private fun showErrorMessages(validationResultCode: Int) {
        if (validationResultCode == -1) {
            Toast.makeText(this, "Km field can't be empty", Toast.LENGTH_SHORT).show()
        }
        if (validationResultCode == -2) {
            Toast.makeText(this, "Current Km can't be less than previous value",
                Toast.LENGTH_SHORT).show()
        }
        if (validationResultCode == -3) {
            Toast.makeText(this, "Fuel Liters can't be empty", Toast.LENGTH_SHORT).show()
        }
        if (validationResultCode == -4) {
            Toast.makeText(this, "Price/Liter can't be empty", Toast.LENGTH_SHORT).show()
        }
        if (validationResultCode == -5) {
            Toast.makeText(this, "The provided date can't be less than the latest date",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun runCalculatedLogFieldsOfEdit(selectedLogToEdit: LogEntity) {
        // TODO: fix the situation of having 2 fill ups in the same day,
        //  distance travelled issue (problem with indexOf if 2 changes are made)
        //  and consumption being infinity which points to a/0
        val indexOfCurrentLog = logsList!!.indexOf(selectedLogToEdit)

        fillUpCost = round((fuelLiters!! * pricePerLiter!!) * 100.0) / 100.0

        if (logsList!!.size > 1) {
            if (logDate == selectedLogToEdit.logDate) {
                lastFillKm = selectedLogToEdit.lastFillKm
                if (actualKm == selectedLogToEdit.currentKm) {
                    distanceTravelled = selectedLogToEdit.distanceTravelled
                } else {
                    distanceTravelled = actualKm!! - lastFillKm
                }
            }
            else {
                lastFillKm = logsList!![indexOfCurrentLog + 1].currentKm
                distanceTravelled = actualKm!! - lastFillKm
            }

            fuelConsumptionPer100Km = if (!partialFillUpCheckBox)
                round(((100 * fuelLiters!!) / distanceTravelled) * 100.0) / 100.0
            else 0.0
        } else distanceTravelled = 0
    }

    private fun listenToCheckBoxChanges(dialogBinding: InsertNewLogDialogBinding) {
        dialogBinding.checkboxPartialFillUp.setOnCheckedChangeListener { buttonView, isChecked ->
            partialFillUpCheckBox = isChecked
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

        getDates(dialogBinding)
        fillDialogFieldsWhenDialogShows(dialogBinding, logToEdit)

        editDialog.show()

        listenToDateInputClick(dialogBinding)
        listenToCheckBoxChanges(dialogBinding)

        listenToEditDialogButtons(dialogBinding, logToEdit, editDialog)
    }

    private fun listenToEditDialogButtons(
        dialogBinding: InsertNewLogDialogBinding, logToEdit: LogEntity, editDialog: Dialog) {
        dialogBinding.tvProceed.setOnClickListener {
            val newLog = setLogObjectFromEditedDialogFields(dialogBinding, logToEdit) ?: return@setOnClickListener
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

    private fun fillDialogFieldsWhenDialogShows(dialogBinding: InsertNewLogDialogBinding, logToEdit: LogEntity) {
        dialogBinding.etPricePerLiter.setText(logToEdit.pricePerLiter.toString())
        dialogBinding.etKm.setText(logToEdit.currentKm.toString())
        dialogBinding.etFuelLiters.setText(logToEdit.fuelLiters.toString())
        dialogBinding.tvDateCalendar.setText(logToEdit.logDate)
        previousLogDate = logToEdit.logDate

        partialFillUpCheckBox = logToEdit.partialFillUp

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