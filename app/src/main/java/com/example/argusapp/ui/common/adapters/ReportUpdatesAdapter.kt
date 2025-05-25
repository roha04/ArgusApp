package com.example.argusapp.ui.common.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.argusapp.R
import com.example.argusapp.data.model.ReportUpdate
import com.example.argusapp.databinding.ItemReportUpdateBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReportUpdatesAdapter :
    ListAdapter<ReportUpdate, ReportUpdatesAdapter.UpdateViewHolder>(UpdateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateViewHolder {
        val binding = ItemReportUpdateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UpdateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpdateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class UpdateViewHolder(private val binding: ItemReportUpdateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(update: ReportUpdate) {
            // Форматування часу
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(update.timestamp.toDate())

            binding.textViewUpdateTime.text = formattedDate
            binding.textViewUserName.text = "${update.userDisplayName} (${getRoleText(update.userRole)})"
            binding.textViewUpdateDescription.text = update.description

            // Встановлення іконки в залежності від типу оновлення
            val iconResId = when (update.actionType) {
                "status_change" -> R.drawable.ic_status_change
                "assignment" -> R.drawable.ic_assignment
                "official_note" -> R.drawable.ic_official_note
                "comment" -> R.drawable.ic_comment
                else -> R.drawable.ic_update
            }
            binding.imageViewUpdateIcon.setImageResource(iconResId)
        }

        private fun getRoleText(role: String): String {
            return when (role) {
                "admin" -> "Адміністратор"
                "police" -> "Поліцейський"
                "citizen" -> "Громадянин"
                else -> role
            }
        }
    }

    class UpdateDiffCallback : DiffUtil.ItemCallback<ReportUpdate>() {
        override fun areItemsTheSame(oldItem: ReportUpdate, newItem: ReportUpdate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReportUpdate, newItem: ReportUpdate): Boolean {
            return oldItem == newItem
        }
    }
}