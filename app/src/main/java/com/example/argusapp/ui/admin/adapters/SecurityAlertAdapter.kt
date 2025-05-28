// ui/admin/adapters/SecurityAlertsAdapter.kt
package com.example.argusapp.ui.admin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.argusapp.R
import com.example.argusapp.data.model.SecurityAlert
import com.example.argusapp.databinding.ItemSecurityAlertBinding
import java.text.SimpleDateFormat
import java.util.*

class SecurityAlertsAdapter(
    private val alerts: List<SecurityAlert>,
    private val onAlertClick: (SecurityAlert) -> Unit
) : RecyclerView.Adapter<SecurityAlertsAdapter.AlertViewHolder>() {

    class AlertViewHolder(val binding: ItemSecurityAlertBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemSecurityAlertBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        with(holder.binding) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = alert.timestamp.toDate()

            tvAlertType.text = alert.getAlertTypeText()
            tvDescription.text = alert.description
            tvUserEmail.text = alert.userEmail
            tvTimestamp.text = dateFormat.format(date)

            // Set status chip
            chipStatus.text = alert.getStatusText()
            when (alert.status) {
                "new" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.alert_new)
                    chipStatus.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
                "investigating" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.alert_investigating)
                    chipStatus.setTextColor(ContextCompat.getColor(root.context, R.color.white))
                }
                "resolved" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.alert_resolved)
                    chipStatus.setTextColor(ContextCompat.getColor(root.context, R.color.black))
                }
                "false_positive" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.alert_false_positive)
                    chipStatus.setTextColor(ContextCompat.getColor(root.context, R.color.black))
                }
            }

            // Set severity indicator
            when (alert.severity) {
                5 -> {
                    viewSeverity.setBackgroundResource(R.drawable.bg_severity_critical)
                    tvSeverity.text = "Критична"
                }
                4 -> {
                    viewSeverity.setBackgroundResource(R.drawable.bg_severity_high)
                    tvSeverity.text = "Висока"
                }
                3 -> {
                    viewSeverity.setBackgroundResource(R.drawable.bg_severity_medium)
                    tvSeverity.text = "Середня"
                }
                2 -> {
                    viewSeverity.setBackgroundResource(R.drawable.bg_severity_low)
                    tvSeverity.text = "Низька"
                }
                else -> {
                    viewSeverity.setBackgroundResource(R.drawable.bg_severity_info)
                    tvSeverity.text = "Інформаційна"
                }
            }

            // Set click listener
            root.setOnClickListener {
                onAlertClick(alert)
            }
        }
    }

    override fun getItemCount() = alerts.size
}