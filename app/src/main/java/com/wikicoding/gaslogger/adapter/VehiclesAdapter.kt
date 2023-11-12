package com.wikicoding.gaslogger.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wikicoding.gaslogger.R
import com.wikicoding.gaslogger.databinding.VehicleRvItemBinding
import com.wikicoding.gaslogger.model.VehicleEntity

class VehiclesAdapter(private val vehiclesList: List<VehicleEntity>) : RecyclerView.Adapter<VehiclesAdapter.AdapterVH>() {
    private var onClicked: OnClickList? = null

    inner class AdapterVH(binding: VehicleRvItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val rvVehicleInfo: TextView = binding.tvVehicle
        val rvVehicleLicensePlate: TextView = binding.tvLicensePlate
        val rvVehicleGasKm: TextView = binding.tvGasKm
        val rvVehicleRegistration: TextView = binding.tvRegistrationDate
        val rvVehicleImage: ImageView = binding.ivVehicleImage
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(
            VehicleRvItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val vehicleInstance = vehiclesList[position]
        holder.rvVehicleInfo.text = "${vehicleInstance.make} ${vehicleInstance.model}"
        holder.rvVehicleRegistration.text = "From: ${vehicleInstance.registrationDate}"
        holder.rvVehicleLicensePlate.text = "${vehicleInstance.licensePlate}"
        holder.rvVehicleGasKm.text = "${vehicleInstance.startKm}km on ${vehicleInstance.fuelType}"
        if (Uri.parse(vehicleInstance.image) == null) {
            holder.rvVehicleImage.setImageResource(R.drawable.add_screen_image_placeholder)
            holder.rvVehicleImage.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            holder.rvVehicleImage.setImageURI(Uri.parse(vehicleInstance.image))
            holder.rvVehicleImage.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        holder.itemView.setOnClickListener {
            if (onClicked != null) {
                val indexClicked = vehiclesList.indexOf(vehicleInstance)
                onClicked!!.onClick(indexClicked, vehicleInstance)
            }
        }
    }

    override fun getItemCount(): Int {
        return vehiclesList.size
    }

    interface OnClickList {
        fun onClick(position: Int, vehicleInstance: VehicleEntity)
    }

    fun setOnClick(onClick: OnClickList) {
        this.onClicked = onClick
    }

    fun findSwipedItem(position: Int): VehicleEntity {
        return vehiclesList[position]
    }
}