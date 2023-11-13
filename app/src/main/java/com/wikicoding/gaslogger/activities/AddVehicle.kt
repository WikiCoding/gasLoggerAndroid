package com.wikicoding.gaslogger.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.wikicoding.gaslogger.R
import com.wikicoding.gaslogger.databinding.ActivityAddVehicleBinding
import com.wikicoding.gaslogger.model.VehicleEntity
import com.wikicoding.gaslogger.utils.CalendarDatesPickerCallback
import kotlinx.coroutines.launch

open class AddVehicle : BaseActivity(), AdapterView.OnItemSelectedListener {
    private var binding: ActivityAddVehicleBinding? = null
    private var fuelType: String? = null
    private var vehicleImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVehicleBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Add Vehicle"

        getCalendarDatePickerCallback(this, binding!!.etRegistrationDate)

        setupFuelTypeDropdownMenu()

        handleImageClick()

        handleSaveBtnClick()
    }

    private fun handleSaveBtnClick() {
        binding!!.btnSaveVehicle.setOnClickListener {
            val resultCode = saveVehicleToDatabase()
            if (resultCode == -1) return@setOnClickListener
            finish()
        }
    }

    private fun handleImageClick() {
        binding!!.ivVehicleImage.setOnClickListener {
            pictureDialog()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        /** Add to AndroidManifest on this activity the next line **/
        /** android:parentActivityName=".activities.MainActivity" **/
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun setupFuelTypeDropdownMenu() {
        /** adding dropdown elements see https://developer.android.com/develop/ui/views/components/spinner?hl=pt-br **/
        val dropdown: Spinner = binding!!.dropdownFuelType
        dropdown.onItemSelectedListener = this
        /** added a resource in the strings.xml file **/
        ArrayAdapter.createFromResource(
            this,
            R.array.fuel_type_array,
            android.R.layout.simple_spinner_dropdown_item // pointing to the layout
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dropdown.adapter = adapter
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        fuelType = parent?.getItemAtPosition(position) as String?
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    private fun saveVehicleToDatabase(): Int {
        val make = binding!!.etMake.text.toString()
        val model = binding!!.etModel.text.toString()
        val licensePlate = binding!!.etLicensePlate.text.toString()
        val registrationDate = binding!!.etRegistrationDate.text.toString()

        if (!validateForm(make, model, licensePlate, registrationDate)) return -1
        val km = Integer.parseInt(binding!!.etKm.text.toString())

        val vehicle =
            VehicleEntity(0, make, model, licensePlate, km, registrationDate, fuelType!!, vehicleImageUri.toString())

        lifecycleScope.launch {
            dao.addVehicle(vehicle)
        }
        return 0
    }

    private fun validateForm(make: String, model: String, licensePlate: String, registrationDate: String): Boolean {
        val km: Int
        try {
            km = Integer.parseInt(binding!!.etKm.text.toString())
        } catch (e: NumberFormatException) {
            Toast.makeText(
                applicationContext, "Problem when converting km value",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (make.isEmpty() || model.isEmpty() || km < 0 || km > 1000000 || licensePlate.isEmpty()) {
            Toast.makeText(
                applicationContext, "You need to fill in all the forms correctly",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        val registrationDateLong = dateToTimestamp(registrationDate)
        println(registrationDateLong)
        println(System.currentTimeMillis())
        if (registrationDateLong > System.currentTimeMillis()) return false

        return true
    }

    private fun pictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf(
            "Select photo from Gallery",
            "Capture photo from camera"
        )
        pictureDialog.setItems(pictureDialogItems) { dialog, which ->
            when (which) {
                0 -> {
                    choosePhotoFromGallery()
                }
                1 -> {
                    Log.e("camera", which.toString())
                    takePhotoFromCamera()
                }
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

                binding!!.ivVehicleImage.setImageBitmap(resultFromActivity)
                binding!!.ivVehicleImage.scaleType = ImageView.ScaleType.CENTER_CROP
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
                    binding!!.ivVehicleImage.setImageBitmap(imageBitmap)
                    binding!!.ivVehicleImage.scaleType = ImageView.ScaleType.CENTER_CROP
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