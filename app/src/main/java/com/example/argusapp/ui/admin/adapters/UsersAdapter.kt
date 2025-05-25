package com.example.argusapp.ui.admin.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.argusapp.R
import com.example.argusapp.databinding.ItemUserBinding
import com.example.argusapp.data.model.User
import java.text.SimpleDateFormat
import java.util.Locale

class UsersAdapter(private val onItemClick: (User) -> Unit) :
    ListAdapter<User, UsersAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User, onItemClick: (User) -> Unit) {
            // Встановлення імені та email
            binding.textViewUserName.text = user.displayName
            binding.textViewUserEmail.text = user.email

            // Завантаження фото користувача
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .into(binding.imageViewUserPhoto)
            } else {
                binding.imageViewUserPhoto.setImageResource(R.drawable.ic_person_placeholder)
            }

            // Форматування дати реєстрації
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val registrationDate = dateFormat.format(user.createdAt.toDate())
            binding.textViewRegistrationDate.text = "Зареєстрований: $registrationDate"

            // Встановлення чіпа для ролі
            val (roleText, chipColor) = when (user.role) {
                "admin" -> Pair("Адміністратор", R.color.admin_role)
                "police" -> Pair("Поліцейський", R.color.police_role)
                else -> Pair("Громадянин", R.color.citizen_role)
            }

            binding.chipUserRole.text = roleText
            binding.chipUserRole.chipBackgroundColor = ContextCompat.getColorStateList(
                binding.root.context, chipColor
            )

            // Обробка кліку на елементі
            binding.root.setOnClickListener { onItemClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}