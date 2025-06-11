package com.example.argusapp.ui.admin

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.argusapp.R
import com.example.argusapp.data.model.Department
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class DepartmentInfoWindow(
    private val mapView: MapView,
    private val context: Context,
    private val department: Department
) : InfoWindow(R.layout.department_info_window, mapView) {

    override fun onOpen(item: Any?) {
        val marker = item as Marker

        val title: TextView = mView.findViewById(R.id.title)
        val address: TextView = mView.findViewById(R.id.address)
        val phone: TextView = mView.findViewById(R.id.phone)
        val officers: TextView = mView.findViewById(R.id.officers)
        val jurisdiction: TextView = mView.findViewById(R.id.jurisdiction)
        val btnDetails: Button = mView.findViewById(R.id.btnDetails)

        title.text = department.name
        address.text = department.address ?: "Not available"
        phone.text = department.phoneNumber ?: "Not available"
        officers.text = department.officerCount.toString()
        jurisdiction.text = "${department.jurisdictionRadius} km"

        btnDetails.setOnClickListener {
            val intent = Intent(context, DepartmentDetailActivity::class.java).apply {
                putExtra("departmentId", department.id)
            }
            context.startActivity(intent)
            close()
        }
    }

    override fun onClose() {
        // Clean up resources if needed
    }
}