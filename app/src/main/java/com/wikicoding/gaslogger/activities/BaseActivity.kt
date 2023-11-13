package com.wikicoding.gaslogger.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.wikicoding.gaslogger.R
import com.wikicoding.gaslogger.adapter.LogsAdapter
import com.wikicoding.gaslogger.adapter.VehiclesAdapter
import com.wikicoding.gaslogger.dao.GasLoggerApp
import com.wikicoding.gaslogger.dao.GasLoggerDao
import com.wikicoding.gaslogger.databinding.DeleteConfirmationDialogBinding
import com.wikicoding.gaslogger.model.LogEntity
import com.wikicoding.gaslogger.model.VehicleEntity
import com.wikicoding.gaslogger.utils.CalendarDatesPickerCallback
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

open class BaseActivity : AppCompatActivity() {
    lateinit var dao: GasLoggerDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = (application as GasLoggerApp).db.gasLoggerDao()
    }

    fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)

        /** MODE_PRIVATE means that other applications will not be able to access this directory**/
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage("Permissions denied for this app")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    fun convertUriToBitmap(uri: Uri, context: Context): Bitmap? {
        val bitmap: Bitmap
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                return bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun dateToTimestamp(dateString: String): Long {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy")
        val date = dateFormat.parse(dateString)
        return date?.time ?: 0L
    }

    fun getCalendarDatePickerCallback(context: Context, calendarInputField: EditText) {
        val calendarDatePicker = CalendarDatesPickerCallback(context, calendarInputField)
        calendarDatePicker.getDates()
        calendarDatePicker.setCurrentDate()
        calendarDatePicker.listenToDateInputClick()
    }

    fun deleteConfirmationDialog(context: Context, vehicle: VehicleEntity?,
                                 vehiclesList: ArrayList<VehicleEntity>?, log: LogEntity?,
                                 logsList: ArrayList<LogEntity>?, vehiclesAdapter: VehiclesAdapter?,
                                 logsAdapter: LogsAdapter?, position: Int) {
        val (deleteConfirmationDialog, dialogBinding) = createShowDeleteDialog(context)

        handleDeleteDialogProceedClick(dialogBinding, context, vehicle, vehiclesList, vehiclesAdapter,
            position, log, logsList, logsAdapter, deleteConfirmationDialog)

        handleDeleteDialogCancelClick(dialogBinding, deleteConfirmationDialog, context, vehiclesAdapter,
            logsAdapter)
    }

    private fun createShowDeleteDialog(context: Context): Pair<Dialog, DeleteConfirmationDialogBinding> {
        val deleteConfirmationDialog = Dialog(context, R.style.Theme_Dialog)
        //avoiding that clicking outside will not close the dialog or update data
        deleteConfirmationDialog.setCancelable(false)
        val dialogBinding = DeleteConfirmationDialogBinding.inflate(layoutInflater)
        deleteConfirmationDialog.setContentView(dialogBinding.root)
        deleteConfirmationDialog.show()
        return Pair(deleteConfirmationDialog, dialogBinding)
    }

    private fun handleDeleteDialogCancelClick(
        dialogBinding: DeleteConfirmationDialogBinding,
        deleteConfirmationDialog: Dialog,
        context: Context,
        vehiclesAdapter: VehiclesAdapter?,
        logsAdapter: LogsAdapter?
    ) {
        dialogBinding.tvCancel.setOnClickListener {
            deleteConfirmationDialog.dismiss()
            if (context is MainActivity) {
                vehiclesAdapter!!.notifyDataSetChanged()
            } else {
                logsAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun handleDeleteDialogProceedClick(dialogBinding: DeleteConfirmationDialogBinding,
                                               context: Context, vehicle: VehicleEntity?,
                                               vehiclesList: ArrayList<VehicleEntity>?,
                                               vehiclesAdapter: VehiclesAdapter?, position: Int,
                                               log: LogEntity?, logsList: ArrayList<LogEntity>?,
                                               logsAdapter: LogsAdapter?,
                                               deleteConfirmationDialog: Dialog) {
        dialogBinding.tvProceed.setOnClickListener {
            if (context is MainActivity) {
                proceedDeletingVehicle(vehicle!!, vehiclesList!!, vehiclesAdapter!!, position)
            } else {
                proceedDeletingLog(log, logsList, logsAdapter, position)
            }
            deleteConfirmationDialog.dismiss()
        }
    }

    private fun proceedDeletingLog(log: LogEntity?, logsList: ArrayList<LogEntity>?,
                                   logsAdapter: LogsAdapter?, position: Int) {
        lifecycleScope.launch {
            dao.deleteLog(log!!)
            logsList!!.remove(log)
            logsAdapter!!.notifyItemRemoved(position)
        }
    }

    private fun proceedDeletingVehicle(vehicle: VehicleEntity?, vehiclesList: ArrayList<VehicleEntity>?,
                                       vehiclesAdapter: VehiclesAdapter?, position: Int) {
        lifecycleScope.launch {
            dao.deleteVehicle(vehicle!!)
            vehiclesList!!.remove(vehicle)
            vehiclesAdapter!!.notifyItemRemoved(position)
        }
    }

    companion object {
        private const val IMAGE_DIRECTORY = "GasLogImages"
    }
}