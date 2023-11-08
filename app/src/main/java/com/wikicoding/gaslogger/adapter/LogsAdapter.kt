package com.wikicoding.gaslogger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wikicoding.gaslogger.databinding.LogRvItemBinding
import com.wikicoding.gaslogger.model.LogEntity

class LogsAdapter(private val logsList: List<LogEntity>) : RecyclerView.Adapter<LogsAdapter.AdapterVH>() {
    inner class AdapterVH(binding: LogRvItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val actualKm = binding.tvActualKm
        val logDate = binding.tvDate
        val pricePerLiter = binding.tvPricePerLiter
        val distanceTravelled = binding.tvDistanceTravelled
        val totalCost = binding.tvTotalCost
        val fuelConsumption = binding.tvFuelConsumption
        val liters = binding.tvLiters
        val partialFillUp = binding.tvPartialFillUp
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(
            LogRvItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val logInstance = logsList[position]
        holder.actualKm.text = "${logInstance.currentKm}Km"
        holder.logDate.text = "On: ${logInstance.logDate}"
        holder.pricePerLiter.text = "${logInstance.pricePerLiter}€/L"
        holder.distanceTravelled.text = "+${logInstance.distanceTravelled}km"
        holder.totalCost.text = "${logInstance.fillUpCost}€"
        holder.fuelConsumption.text = "${logInstance.fuelConsumption}L/100Km"
        holder.liters.text = "${logInstance.fuelLiters}L"
        if (!logInstance.partialFillUp) holder.partialFillUp.text = "Topped"
        else holder.partialFillUp.text = "Partial fill"
    }

    override fun getItemCount(): Int {
        return logsList.size
    }

    fun findSwipedItem(position: Int): LogEntity {
        return logsList[position]
    }
}