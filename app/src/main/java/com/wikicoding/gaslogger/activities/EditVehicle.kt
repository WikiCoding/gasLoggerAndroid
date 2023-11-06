package com.wikicoding.gaslogger.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
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
import java.util.*
import kotlin.collections.ArrayList

class EditVehicle : BaseActivity(), AdapterView.OnItemSelectedListener {
    private var binding: ActivityEditVehicleBinding? = null
    private var currentVehicle: VehicleEntity? = null
    private var fuelTypeEdited: String? = null
    private var editedVehicle: VehicleEntity? = null
    private var pictureIsChanged: Boolean = false
    var vehicleImage: Uri? = null

    @RequiresApi(33)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditVehicleBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Edit Vehicle"

        if (intent.hasExtra(Constants.UPDATE_VEHICLE_INTENT_EXTRA)) {
            currentVehicle = intent
                .getSerializableExtra(Constants.UPDATE_VEHICLE_INTENT_EXTRA) as VehicleEntity
        }

        preFillFormFields()

        binding!!.ivVehicleImageEdit.setOnClickListener {
            pictureIsChanged = true
            pictureDialog()
        }

        binding!!.btnUpdateVehicleEdit.setOnClickListener {
            editedVehicle = setupUpdatedForm()

            lifecycleScope.launch {
                dao.updateVehicle(editedVehicle!!)
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun validateForm(make: String, model: String, licensePlateEdit: String): Boolean {
        val km: Int
        try {
            km = Integer.parseInt(binding!!.etKmEdit.text.toString())
        } catch (e: NumberFormatException) {
            Toast.makeText(
                applicationContext, "Problem when converting km value",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (make.isEmpty() || model.isEmpty() || km < 0 || km > 1000000 || licensePlateEdit.isEmpty()) {
            Toast.makeText(
                applicationContext, "You need to fill in all the forms correctly",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun setupUpdatedForm() : VehicleEntity? {
        val makeEdit = binding!!.etMakeEdit.text.toString()
        val modelEdit = binding!!.etModelEdit.text.toString()
        val licensePlateEdit = binding!!.etLicensePlate.text.toString()

        if (!validateForm(makeEdit, modelEdit, licensePlateEdit)) return null;

        val kmEdit = Integer.parseInt(binding!!.etKmEdit.text.toString())

        val imageEdit: String = if (!pictureIsChanged) currentVehicle!!.image
        else vehicleImage.toString()

        return VehicleEntity(currentVehicle!!.idVehicle, makeEdit, modelEdit, licensePlateEdit, kmEdit,
            fuelTypeEdited!!, imageEdit)
    }

    private fun preFillFormFields() {
        binding!!.ivVehicleImageEdit.setImageURI(currentVehicle?.image?.toUri())
        binding!!.etMakeEdit.setText(currentVehicle?.make)
        binding!!.etModelEdit.setText(currentVehicle?.model)
        binding!!.etKmEdit.setText(currentVehicle?.startKm.toString())
        binding!!.etLicensePlate.setText(currentVehicle?.licensePlate)
        setupFuelTypeDropdownMenu()
    }

    private fun setupFuelTypeDropdownMenu() {
        /** adding dropdown elements see https://developer.android.com/develop/ui/views/components/spinner?hl=pt-br **/
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
        val adapter = ArrayAdapter.createFromResource(this, R.array.fuel_type_array,
            android.R.layout.simple_spinner_dropdown_item)

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
        pictureDialog.setItems(pictureDialogItems) { dialog, which ->
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
            //startActivityForResult(intent, CAMERA_REQUEST_CODE)
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
    val activityCameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultFromActivity = result.data?.extras?.get("data") as Bitmap

                // will not only save in the internal storage but also update the Uri variable to save the path in the database
                vehicleImage = saveImageToInternalStorage(resultFromActivity)

                binding!!.ivVehicleImageEdit.setImageBitmap(resultFromActivity)
            }
        }

    val activityGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultFromActivity = result.data?.data as Uri

                val imageBitmap = convertUriToBitmap(resultFromActivity, this)
                if (imageBitmap != null) {
                    // will not only save in the internal storage but also update the Uri variable to save the path in the database
                    vehicleImage = saveImageToInternalStorage(imageBitmap)
                    binding!!.ivVehicleImageEdit.setImageBitmap(imageBitmap)
                } else {
                    Toast.makeText(this, "Error getting image from gallery", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
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
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == READ_EXTERNAL_STORAGE_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val galleryIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityGalleryLauncher.launch(galleryIntent)
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val IMAGE_DIRECTORY = "GasLogImages"
        private const val CAMERA_PERMISSION_CODE = 1
        private const val READ_EXTERNAL_STORAGE_CODE = 2
    }

    override fun onDestroy() {
        super.onDestroy()

        binding = null
    }
}