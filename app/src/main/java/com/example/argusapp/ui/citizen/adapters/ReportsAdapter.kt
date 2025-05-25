package com.example.argusapp.ui.citizen.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.argusapp.R
import com.example.argusapp.databinding.ItemReportBinding
import com.example.argusapp.data.model.Report
import java.text.SimpleDateFormat
import java.util.Locale

class ReportsAdapter(private val onItemClick: (Report) -> Unit) :
    ListAdapter<Report, ReportsAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ReportViewHolder(private val binding: ItemReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report, onItemClick: (Report) -> Unit) {
            binding.textViewTitle.text = report.title

            // Форматування дати
            val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
            binding.textViewDate.text = sdf.format(report.createdAt.toDate())

            // Встановлення статусу
            val statusText: String
            val statusColor: Int

            when (report.status) {
                "new" -> {
                    statusText = "Новий"
                    statusColor = R.color.status_new
                }
                "in_progress" -> {
                    statusText = "В обробці"
                    statusColor = R.color.status_in_progress
                }
                "resolved" -> {
                    statusText = "Вирішено"
                    statusColor = R.color.status_resolved
                }
                else -> {
                    statusText = "Закрито"
                    statusColor = R.color.status_closed
                }
            }

            binding.chipStatus.text = statusText
            binding.chipStatus.setChipBackgroundColorResource(statusColor)


            // Клік на елемент
            binding.root.setOnClickListener { onItemClick(report) }
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem == newItem
        }
    }
}