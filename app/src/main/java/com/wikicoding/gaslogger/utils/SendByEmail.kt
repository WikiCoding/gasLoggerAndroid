package com.wikicoding.gaslogger.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.wikicoding.gaslogger.BuildConfig
import com.wikicoding.gaslogger.activities.MainActivity
import com.wikicoding.gaslogger.activities.VehicleLogs
import com.wikicoding.gaslogger.model.VehicleEntity
import java.io.File

object SendByEmail {
    fun handleSendByEmailClick(
        context: Context,
        excelFile: File,
        packageManager: PackageManager,
        currentVehicle: VehicleEntity?
    ) {
        val intent = Intent(Intent.ACTION_SEND)
        /** added a provider tag to AndroidManifest.xml and also a xml resource with the file_paths.xml **/
        /** specifying which type of content I'm sending, for normal text should be text/plain
         * since I'm also sending the attachment, application/octet-stream **/
        intent.type = "application/octet-stream"
        val fileUri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.provider", excelFile
        )

        val logsSubject = "Your logs list @gasLogger for ${currentVehicle?.make} " +
                "${currentVehicle?.model} ${currentVehicle?.licensePlate}."
        val vehiclesSubject = "Your vehicles list @gasLogger."

        /** passing data to the intent **/
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(""))
        if (context is VehicleLogs) { intent.putExtra(Intent.EXTRA_SUBJECT, logsSubject) }
        if (context is MainActivity) { intent.putExtra(Intent.EXTRA_SUBJECT, vehiclesSubject) }

        intent.putExtra(Intent.EXTRA_TEXT, "Attached you can find your vehicles list.")

        intent.putExtra(Intent.EXTRA_STREAM, fileUri)

        /** setting temporary permission to files dir **/
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        /** to satisfy this resolveActivity warning we need to add <queries> to our AndroidManifest **/
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent)
        }
    }
}