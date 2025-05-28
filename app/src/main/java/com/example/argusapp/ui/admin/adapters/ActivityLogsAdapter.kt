// ui/admin/adapters/ActivityLogsAdapter.kt
package com.example.argusapp.ui.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.argusapp.R
import com.example.argusapp.data.model.ActivityLog
import com.example.argusapp.databinding.ItemActivityLogBinding
import java.text.SimpleDateFormat
import java.util.*

class ActivityLogsAdapter(private val logs: List<ActivityLog>) :
    RecyclerView.Adapter<ActivityLogsAdapter.LogViewHolder>() {

    class LogViewHolder(val binding: ItemActivityLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemActivityLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        with(holder.binding) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = log.timestamp.toDate()

            tvUserEmail.text = log.userEmail

            // Set user type with appropriate background color
            tvUserType.text = log.userType
            tvUserType.setBackgroundResource(
                when (log.userType) {
                    "Адміністратор" -> R.drawable.bg_admin_type
                    "Поліцейський" -> R.drawable.bg_police_type
                    "Громадянин" -> R.drawable.bg_citizen_type
                    else -> R.drawable.bg_default_type
                }
            )

            tvAction.text = log.action
            tvTimestamp.text = dateFormat.format(date)

            // Show details only if not empty
            if (log.details.isNotEmpty()) {
                tvDetails.text = log.details
                tvDetails.visibility = View.VISIBLE
            } else {
                tvDetails.visibility = View.GONE
            }

            // Expand/collapse details on click
            root.setOnClickListener {
                if (log.details.isNotEmpty()) {
                    tvDetails.visibility = if (tvDetails.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount() = logs.size
}