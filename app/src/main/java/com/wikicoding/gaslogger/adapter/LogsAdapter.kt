package com.wikicoding.gaslogger.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wikicoding.gaslogger.databinding.LogRvItemBinding
import com.wikicoding.gaslogger.model.LogEntity
import com.wikicoding.gaslogger.model.VehicleEntity

class LogsAdapter(private val logsList: List<LogEntity>) : RecyclerView.Adapter<LogsAdapter.AdapterVH>() {
    inner class AdapterVH(binding: LogRvItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val actualKm = binding.tvActualKm
        val logDate = binding.tvDate
        val pricePerLiter = binding.tvPricePerLiter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterVH {
        return AdapterVH(
            LogRvItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: AdapterVH, position: Int) {
        val logInstance = logsList[position]
        holder.actualKm.text = logInstance.currentKm.toString()
        holder.logDate.text = logInstance.logDate
        holder.pricePerLiter.text = logInstance.pricePerLiter.toString()
    }

    override fun getItemCount(): Int {
        return logsList.size
    }

    fun findSwipedItem(position: Int): LogEntity {
        return logsList[position]
    }
}