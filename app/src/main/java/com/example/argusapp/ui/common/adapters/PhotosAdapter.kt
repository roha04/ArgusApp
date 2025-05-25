package com.example.argusapp.ui.common.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.argusapp.R
import com.example.argusapp.databinding.ItemPhotoBinding

class PhotosAdapter(private val onPhotoClick: (String) -> Unit) :
    ListAdapter<String, PhotosAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding, onPhotoClick)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PhotoViewHolder(
        private val binding: ItemPhotoBinding,
        private val onPhotoClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String) {
            Glide.with(binding.root.context)
                .load(photoUrl)
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_photo_error)
                .into(binding.imageViewPhoto)

            binding.root.setOnClickListener {
                onPhotoClick(photoUrl)
            }
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}