package com.wikicoding.gaslogger.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wikicoding.explorelog.utils.SwipeToDeleteCallback
import com.wikicoding.explorelog.utils.SwipeToEditCallback
import com.wikicoding.gaslogger.BuildConfig
import com.wikicoding.gaslogger.R
import com.wikicoding.gaslogger.adapter.VehiclesAdapter
import com.wikicoding.gaslogger.constants.Constants
import com.wikicoding.gaslogger.databinding.ActivityMainBinding
import com.wikicoding.gaslogger.model.VehicleEntity
import com.wikicoding.gaslogger.utils.SendByEmail
import com.wikicoding.gaslogger.utils.VehiclesExcelExportCallback
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable

class MainActivity : BaseActivity() {
    private var binding: ActivityMainBinding? = null
    private var vehiclesList: ArrayList<VehicleEntity>? = null
    private var adapter: VehiclesAdapter? = null
    private var excelFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        title = "Vehicles List"

        fetchAllVehicles()

        handleEditSwipe()

        handleDeleteSwipe()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu_vehicles, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_add_vehicle -> handleAddVehicleClick()
            R.id.mi_vehicle_export_excel -> exportVehiclesToExcel()
            R.id.mi_send_excel_by_email -> {
                exportVehiclesToExcel()
                SendByEmail.handleSendByEmailClick(this, excelFile!!, packageManager, null)
            }
        }
        return true
    }

    private fun handleDeleteSwipe() {
        val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val rvAdapter = binding!!.rvVehicles.adapter as VehiclesAdapter
                val itemToDelete = rvAdapter.findSwipedItem(viewHolder.adapterPosition)
                deleteConfirmationDialog(
                    this@MainActivity, itemToDelete,
                    vehiclesList!!, null, null, adapter,
                    null, viewHolder.adapterPosition
                )
            }
        }

        val deleteItemTouchHandler = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHandler.attachToRecyclerView(binding!!.rvVehicles)
    }

    private fun handleEditSwipe() {
        val editItemSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val rvAdapter = binding!!.rvVehicles.adapter as VehiclesAdapter
                val itemToEdit: VehicleEntity = rvAdapter.findSwipedItem(viewHolder.adapterPosition)

                val intent = Intent(this@MainActivity, EditVehicle::class.java)
                intent.putExtra(Constants.UPDATE_VEHICLE_INTENT_EXTRA, itemToEdit as Serializable)
                startActivity(intent)
            }
        }

        val editItemTouchHandler = ItemTouchHelper(editItemSwipeHandler)
        editItemTouchHandler.attachToRecyclerView(binding!!.rvVehicles)
    }

    private fun handleAddVehicleClick() {
        val intent = Intent(this, AddVehicle::class.java)
        startActivity(intent)
    }

    private fun fetchAllVehicles() {
        lifecycleScope.launch {
            vehiclesList = dao.fetchAllVehicles() as ArrayList<VehicleEntity>
            vehiclesRecyclerViewSetup(vehiclesList!!)
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

    private fun exportVehiclesToExcel() {
        val exportVehiclesExcel = VehiclesExcelExportCallback(this@MainActivity, vehiclesList!!)
        excelFile = exportVehiclesExcel.exportExcel()
    }

    override fun onResume() {
        fetchAllVehicles()
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        binding = null
    }
}