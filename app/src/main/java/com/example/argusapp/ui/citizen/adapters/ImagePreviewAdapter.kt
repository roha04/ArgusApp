package com.example.argusapp.ui.citizen.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.argusapp.R
import com.example.argusapp.databinding.ItemImagePreviewBinding

class ImagePreviewAdapter(
    private val onRemoveClicked: (Int) -> Unit
) : ListAdapter<Uri, ImagePreviewAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImagePreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding, onRemoveClicked)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class ImageViewHolder(
        private val binding: ItemImagePreviewBinding,
        private val onRemoveClicked: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUri: Uri, position: Int) {
            Glide.with(binding.root.context)
                .load(imageUri)
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_photo_error)
                .into(binding.imageView)

            binding.buttonRemove.setOnClickListener {
                onRemoveClicked(position)
            }
        }
    }

    class ImageDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }
    }
}