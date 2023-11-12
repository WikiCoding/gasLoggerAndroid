package com.wikicoding.gaslogger.activities

import android.R
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
import androidx.lifecycle.lifecycleScope
import com.wikicoding.gaslogger.dao.GasLoggerApp
import com.wikicoding.gaslogger.dao.GasLoggerDao
import com.wikicoding.gaslogger.databinding.DeleteConfirmationDialogBinding
import com.wikicoding.gaslogger.model.LogEntity
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
//    var vehicleImageUri: Uri? = null

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
//
//    // Declare a contract to get a result from another activity.
//    val activityCameraLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val resultFromActivity = result.data?.extras?.get("data") as Bitmap
//
//                vehicleImageUri = saveImageToInternalStorage(resultFromActivity)
//            } else {
//                Toast.makeText(this, "Error getting image from camera", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//
//    val activityGalleryLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK) {
//                val resultFromActivity = result.data?.data as Uri
//
//                val imageBitmap = convertUriToBitmap(resultFromActivity, this)
//                if (imageBitmap != null) {
//                    vehicleImageUri = saveImageToInternalStorage(imageBitmap)
//                } else {
//                    Toast.makeText(this, "Error getting image from gallery", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            }
//        }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == CAMERA_PERMISSION_CODE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                activityCameraLauncher.launch(intent)
//            } else {
//                showRationalDialogForPermissions()
//            }
//        }
//        if (requestCode == READ_EXTERNAL_STORAGE_CODE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                val galleryIntent = Intent(
//                    Intent.ACTION_PICK,
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                )
//                activityGalleryLauncher.launch(galleryIntent)
//            } else {
//                showRationalDialogForPermissions()
//            }
//        }
//    }

    companion object {
        private const val IMAGE_DIRECTORY = "GasLogImages"
//        private const val CAMERA_PERMISSION_CODE = 1
//        private const val READ_EXTERNAL_STORAGE_CODE = 2
    }
}