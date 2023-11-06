package com.wikicoding.gaslogger.activities

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wikicoding.explorelog.utils.SwipeToDeleteCallback
import com.wikicoding.explorelog.utils.SwipeToEditCallback
import com.wikicoding.gaslogger.R
import com.wikicoding.gaslogger.adapter.VehiclesAdapter
import com.wikicoding.gaslogger.constants.Constants
import com.wikicoding.gaslogger.databinding.ActivityMainBinding
import com.wikicoding.gaslogger.databinding.DeleteConfirmationDialogBinding
import com.wikicoding.gaslogger.model.VehicleEntity
import kotlinx.coroutines.launch
import java.io.Serializable

class MainActivity : BaseActivity() {
    private var binding: ActivityMainBinding? = null
    private var vehiclesList: ArrayList<VehicleEntity>? = null
    private var adapter: VehiclesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        title = "Vehicles List"

        /** disabling night mode **/
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        fetchAllVehicles()

        binding!!.btnAddVehicle.setOnClickListener {
            val intent = Intent(this, AddVehicle::class.java)
            startActivity(intent)
            finishAffinity()
        }

        val editItemSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val rvAdapter = binding!!.rvVehicles.adapter as VehiclesAdapter
                val itemToEdit: VehicleEntity = rvAdapter.findSwipedItem(viewHolder.adapterPosition)

                adapter!!.notifyItemChanged(viewHolder.adapterPosition)
                val intent = Intent(this@MainActivity, EditVehicle::class.java)
                intent.putExtra(Constants.UPDATE_VEHICLE_INTENT_EXTRA, itemToEdit as Serializable)
                startActivity(intent)
                finish()
            }
        }

        val editItemTouchHandler = ItemTouchHelper(editItemSwipeHandler)
        editItemTouchHandler.attachToRecyclerView(binding!!.rvVehicles)

        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val rvAdapter = binding!!.rvVehicles.adapter as VehiclesAdapter
                val itemToDelete = rvAdapter.findSwipedItem(viewHolder.adapterPosition)
                deleteConfirmationDialog(itemToDelete, viewHolder.adapterPosition)
            }
        }

        val deleteItemTouchHandler = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHandler.attachToRecyclerView(binding!!.rvVehicles)
    }

    private fun fetchAllVehicles() {
        lifecycleScope.launch {
            vehiclesList = dao.fetchAllVehicles() as ArrayList<VehicleEntity>
            vehiclesRecyclerViewSetup(vehiclesList!!)
        }
    }

    private fun deleteVehicle(vehicle: VehicleEntity, position: Int) {
        lifecycleScope.launch {
            dao.deleteVehicle(vehicle)
            vehiclesList!!.remove(vehicle)
            adapter!!.notifyItemRemoved(position)
        }
    }

    private fun vehiclesRecyclerViewSetup(vehicleEntityList: ArrayList<VehicleEntity>) {
        adapter = VehiclesAdapter(vehicleEntityList)
        binding!!.rvVehicles.layoutManager = LinearLayoutManager(this)
        binding!!.rvVehicles.adapter = adapter

        if (vehicleEntityList.isNotEmpty()) {
            binding!!.rvVehicles.visibility = View.VISIBLE
            binding!!.tvNoVehicles.visibility = View.INVISIBLE
        } else {
            binding!!.rvVehicles.visibility = View.INVISIBLE
            binding!!.tvNoVehicles.visibility = View.VISIBLE
        }

        adapter!!.setOnClick(object : VehiclesAdapter.OnClickList {
            override fun onClick(position: Int, vehicleInstance: VehicleEntity) {
                val indexOfClickedItem = vehiclesList!!.indexOf(vehicleInstance)
                val intent = Intent(applicationContext, VehicleLogs::class.java)
                intent.putExtra(Constants.SELECTED_VEHICLE_DATA, vehiclesList!![indexOfClickedItem])
                startActivity(intent)
            }
        })
    }

    private fun deleteConfirmationDialog(vehicle: VehicleEntity, position: Int) {
        val deleteConfirmationDialog = Dialog(this, R.style.Theme_Dialog)
        //avoiding that clicking outside will not close the dialog or update data
        deleteConfirmationDialog.setCancelable(false)
        val dialogBinding = DeleteConfirmationDialogBinding.inflate(layoutInflater)
        deleteConfirmationDialog.setContentView(dialogBinding.root)
        deleteConfirmationDialog.show()

        dialogBinding.tvProceed.setOnClickListener {
            deleteVehicle(vehicle, position)
            deleteConfirmationDialog.dismiss()
        }

        dialogBinding.tvCancel.setOnClickListener {
            deleteConfirmationDialog.dismiss()
            fetchAllVehicles()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        binding = null
    }
}