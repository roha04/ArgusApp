package com.example.argusapp.ui.common.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.argusapp.R
import com.example.argusapp.data.model.ReportComment
import com.example.argusapp.databinding.ItemCommentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CommentsAdapter :
    ListAdapter<ReportComment, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: ReportComment) {
            // Встановлення імені користувача
            binding.textViewUserName.text = comment.userDisplayName

            // Додаємо позначку ролі для офіційних коментарів
            if (comment.isOfficial) {
                val roleText = when (comment.userRole) {
                    "admin" -> "Адміністратор"
                    "police" -> "Поліцейський"
                    else -> ""
                }
                binding.textViewUserRole.text = roleText
                binding.textViewUserRole.visibility = ViewGroup.VISIBLE
            } else {
                binding.textViewUserRole.visibility = ViewGroup.GONE
            }

            // Текст коментаря
            binding.textViewCommentText.text = comment.text

            // Час створення коментаря
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(comment.createdAt.toDate())
            binding.textViewCommentTime.text = formattedDate

            // Зображення користувача
            if (comment.userPhotoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(comment.userPhotoUrl)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .into(binding.imageViewUserPhoto)
            } else {
                binding.imageViewUserPhoto.setImageResource(R.drawable.ic_person_placeholder)
            }

            // Офіційні коментарі виділяються
            if (comment.isOfficial) {
                binding.cardComment.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.official_comment_background)
                )
            } else {
                binding.cardComment.setCardBackgroundColor(
                    binding.root.context.getColor(android.R.color.white)
                )
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<ReportComment>() {
        override fun areItemsTheSame(oldItem: ReportComment, newItem: ReportComment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReportComment, newItem: ReportComment): Boolean {
            return oldItem == newItem
        }
    }
}