package com.example.argusapp.ui.police.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.argusapp.R
import com.example.argusapp.databinding.ItemPoliceReportBinding
import com.example.argusapp.data.model.Report
import java.text.SimpleDateFormat
import java.util.Locale

class PoliceReportsAdapter(private val onItemClick: (Report) -> Unit) :
    ListAdapter<Report, PoliceReportsAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemPoliceReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ReportViewHolder(private val binding: ItemPoliceReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report, onItemClick: (Report) -> Unit) {
            // Заголовок звіту
            binding.textViewReportTitle.text = report.title

            // Адреса
            binding.textViewAddress.text = report.address

            // Дата створення
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(report.createdAt.toDate())
            binding.textViewReportDate.text = formattedDate

            // Категорія
            binding.textViewCategory.text = getCategoryDisplayName(report.category)

            // Рівень терміновості
            val urgencyText = when (report.urgency) {
                "high" -> "Терміново"
                "medium" -> "Середній"
                "low" -> "Низький"
                else -> "Невизначено"
            }
            binding.textViewUrgency.text = urgencyText

            val urgencyColor = when (report.urgency) {
                "high" -> R.color.urgency_high
                "medium" -> R.color.urgency_medium
                "low" -> R.color.urgency_low
                else -> R.color.urgency_unknown
            }
            binding.textViewUrgency.setTextColor(
                ContextCompat.getColor(binding.root.context, urgencyColor)
            )

            // Обробка кліку на елементі
            binding.root.setOnClickListener { onItemClick(report) }
        }

        private fun getCategoryDisplayName(categoryId: String): String {
            return when (categoryId) {
                "theft" -> "Крадіжка"
                "vandalism" -> "Вандалізм"
                "traffic" -> "Порушення ПДР"
                "noise" -> "Шум"
                "other" -> "Інше"
                else -> categoryId
            }
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