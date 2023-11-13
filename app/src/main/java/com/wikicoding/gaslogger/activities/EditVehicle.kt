package com.wikicoding.gaslogger.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.wikicoding.gaslogger.R
import com.wikicoding.gaslogger.constants.Constants
import com.wikicoding.gaslogger.databinding.ActivityEditVehicleBinding
import com.wikicoding.gaslogger.model.VehicleEntity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EditVehicle : BaseActivity(), AdapterView.OnItemSelectedListener {
    private var binding: ActivityEditVehicleBinding? = null
    private var currentVehicle: VehicleEntity? = null
    private var fuelTypeEdited: String? = null
    private var editedVehicle: VehicleEntity? = null
    private var pictureIsChanged: Boolean = false
    private var vehicleImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditVehicleBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Edit Vehicle"

        getIntentExtra()

        preFillFormFields()

        handleImageClick()

        handleSaveUpdatesBtnClick()
    }

    private fun handleSaveUpdatesBtnClick() {
        binding!!.btnUpdateVehicleEdit.setOnClickListener {
            editedVehicle = setupUpdatedForm()
            if (editedVehicle == null) return@setOnClickListener

            lifecycleScope.launch {
                dao.updateVehicle(editedVehicle!!)
            }

            finish()
        }
    }

    private fun handleImageClick() {
        binding!!.ivVehicleImageEdit.setOnClickListener {
            pictureIsChanged = true
            pictureDialog()
        }
    }

    private fun getIntentExtra() {
        if (intent.hasExtra(Constants.UPDATE_VEHICLE_INTENT_EXTRA)) {
            currentVehicle = intent
                .getSerializableExtra(Constants.UPDATE_VEHICLE_INTENT_EXTRA) as VehicleEntity
            // new way is intent.getSerializableExtra(String, Class) but doesn't work!
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun setupUpdatedForm(): VehicleEntity? {
        val makeEdit = binding!!.etMakeEdit.text.toString()
        val modelEdit = binding!!.etModelEdit.text.toString()
        val licensePlateEdit = binding!!.etLicensePlate.text.toString()
        val registrationDateEdit = binding!!.etRegistrationDate.text.toString()
        val currentKm = binding!!.etKmEdit.text.toString()

        if (!validateVehicleForm(currentKm, makeEdit, modelEdit, licensePlateEdit, registrationDateEdit)) return null;
        val kmEdit = Integer.parseInt(binding!!.etKmEdit.text.toString())

        val imageEdit: String = if (!pictureIsChanged) currentVehicle!!.image
        else vehicleImageUri.toString()

        return VehicleEntity(
            currentVehicle!!.idVehicle, makeEdit, modelEdit, licensePlateEdit, kmEdit, registrationDateEdit,
            fuelTypeEdited!!, imageEdit
        )
    }

    private fun preFillFormFields() {
        binding!!.ivVehicleImageEdit.setImageURI(currentVehicle?.image?.toUri())
        binding!!.etMakeEdit.setText(currentVehicle?.make)
        binding!!.etModelEdit.setText(currentVehicle?.model)
        binding!!.etKmEdit.setText(currentVehicle?.startKm.toString())
        binding!!.etLicensePlate.setText(currentVehicle?.licensePlate)
        setupFuelTypeDropdownMenu()
        getCalendarDatePickerCallback(this, binding!!.etRegistrationDate)
    }

    private fun setupFuelTypeDropdownMenu() {
        /** adding dropdown elements see:
         * https://developer.android.com/develop/ui/views/components/spinner?hl=pt-br **/
        val dropdown: Spinner = binding!!.dropdownFuelTypeEdit
        dropdown.onItemSelectedListener = this
        val dropdownLoadedItemPosition = setupCorrectFuelTypeOnDropdown()

        /** added a resource in the strings.xml file **/
        ArrayAdapter.createFromResource(
            this,
            R.array.fuel_type_array,
            android.R.layout.simple_spinner_dropdown_item // pointing to the layout
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dropdown.adapter = adapter
            if (dropdownLoadedItemPosition != -1) dropdown.setSelection(dropdownLoadedItemPosition)
        }
    }

    private fun setupCorrectFuelTypeOnDropdown(): Int {
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.fuel_type_array,
            android.R.layout.simple_spinner_dropdown_item
        )

        val itemList = ArrayList<String>()

        for (i in 0 until adapter.count) {
            itemList.add(adapter.getItem(i) as String)
        }

        val position: Int
        for ((index, item) in itemList.withIndex()) {
            if (currentVehicle!!.fuelType == item) {
                position = index
                return position
            }
        }
        return -1
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        fuelTypeEdited = parent?.getItemAtPosition(position) as String?
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        fuelTypeEdited = currentVehicle?.fuelType
    }

    private fun pictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf(
            "Select photo from Gallery",
            "Capture photo from camera"
        )
        pictureDialog.setItems(pictureDialogItems) { _, which ->
            when (which) {
                0 -> choosePhotoFromGallery()
                1 -> takePhotoFromCamera()
            }
        }
        pictureDialog.show()
    }

    private fun takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            /** using this intent to start our camera**/
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            activityCameraLauncher.launch(intent)
            binding!!.ivVehicleImageEdit.setImageBitmap(convertUriToBitmap(vehicleImageUri!!, this))
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun choosePhotoFromGallery() {
        val galleryIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activityGalleryLauncher.launch(galleryIntent)
    }

    // Declare a contract to get a result from another activity.
    private val activityCameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultFromActivity = result.data?.extras?.get("data") as Bitmap

                // will not only save in the internal storage but
                // also update the Uri variable to save the path in the database
                vehicleImageUri = saveImageToInternalStorage(resultFromActivity)

                binding!!.ivVehicleImageEdit.setImageBitmap(resultFromActivity)
                binding!!.ivVehicleImageEdit.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }

    private val activityGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultFromActivity = result.data?.data as Uri

                val imageBitmap = convertUriToBitmap(resultFromActivity, this)
                if (imageBitmap != null) {
                    // will not only save in the internal storage but
                    // also update the Uri variable to save the path in the database
                    vehicleImageUri = saveImageToInternalStorage(imageBitmap)
                    binding!!.ivVehicleImageEdit.setImageBitmap(imageBitmap)
                    binding!!.ivVehicleImageEdit.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    Toast.makeText(this, "Error getting image from gallery", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                activityCameraLauncher.launch(intent)
            } else {
                showRationalDialogForPermissions()
            }
        }
        if (requestCode == READ_EXTERNAL_STORAGE_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val galleryIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                activityGalleryLauncher.launch(galleryIntent)
            } else {
                showRationalDialogForPermissions()
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val READ_EXTERNAL_STORAGE_CODE = 2
    }

    override fun onDestroy() {
        super.onDestroy()

        binding = null
    }
}