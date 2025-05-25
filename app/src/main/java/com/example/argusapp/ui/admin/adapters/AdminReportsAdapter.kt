package com.example.argusapp.ui.admin.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.argusapp.R
import com.example.argusapp.data.model.Report
import com.example.argusapp.databinding.ItemAdminReportBinding
import java.text.SimpleDateFormat
import java.util.Locale

class AdminReportsAdapter(private val onItemClick: (Report) -> Unit) :
    ListAdapter<Report, AdminReportsAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemAdminReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ReportViewHolder(private val binding: ItemAdminReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report, onItemClick: (Report) -> Unit) {
            // Заголовок заявки
            binding.textViewReportTitle.text = report.title

            // Статус заявки
            val (statusText, statusColor) = when (report.status) {
                "new" -> Pair("Новий", R.color.status_new)
                "in_progress" -> Pair("В обробці", R.color.status_in_progress)
                "resolved" -> Pair("Вирішено", R.color.status_resolved)
                else -> Pair("Закрито", R.color.status_closed)
            }

            binding.chipReportStatus.text = statusText
            binding.chipReportStatus.chipBackgroundColor = ContextCompat.getColorStateList(
                binding.root.context, statusColor
            )

            // Дата створення
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(report.createdAt.toDate())
            binding.textViewReportDate.text = formattedDate

            // Категорія заявки
            binding.textViewReportCategory.text = getCategoryDisplayName(report.category)

            // Адреса
            binding.textViewAddress.text = report.address

            // Ім'я заявника
            binding.textViewReporterName.text = report.userDisplayName

            // Кількість коментарів
            val commentsText = "Коментарі: ${report.commentCount}"
            binding.textViewCommentCount.text = commentsText

            // Інформація про призначення
            if (report.assignedToId.isNotEmpty()) {
                binding.textViewAssigned.text = "Призначено: ${report.assignedToId}"
            } else {
                binding.textViewAssigned.text = "Не призначено"
            }

            // Обробка натискання на елемент
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