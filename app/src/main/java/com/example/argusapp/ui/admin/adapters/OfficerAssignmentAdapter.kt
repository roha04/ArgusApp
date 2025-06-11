package com.example.argusapp.ui.admin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.argusapp.R
import com.example.argusapp.data.model.User
import com.example.argusapp.databinding.ItemOfficerAssignmentBinding

class OfficerAssignmentAdapter(
    private val officers: List<User>,
    private val onAssignmentChanged: (User, Boolean) -> Unit
) : RecyclerView.Adapter<OfficerAssignmentAdapter.OfficerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfficerViewHolder {
        val binding = ItemOfficerAssignmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OfficerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfficerViewHolder, position: Int) {
        holder.bind(officers[position])
    }

    override fun getItemCount(): Int = officers.size

    inner class OfficerViewHolder(private val binding: ItemOfficerAssignmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(officer: User) {
            binding.tvOfficerName.text = officer.displayName
            binding.tvOfficerEmail.text = officer.email
            binding.switchAssign.isChecked = officer.isSelected

            // Load officer photo if available
            if (!officer.photoUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(officer.photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivOfficerPhoto)
            }

            binding.switchAssign.setOnCheckedChangeListener { _, isChecked ->
                officer.isSelected = isChecked
                onAssignmentChanged(officer, isChecked)
            }

            // Make the whole card clickable to toggle the switch
            binding.root.setOnClickListener {
                binding.switchAssign.toggle()
            }
        }
    }
}
//package com.example.argusapp.ui.admin.adapters
//
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import com.example.argusapp.data.model.User
//import com.example.argusapp.databinding.ItemOfficerAssignmentBinding
//
//class OfficerAssignmentAdapter(
//    private val officers: List<User>,
//    private val onAssignmentChanged: (User, Boolean) -> Unit
//) : RecyclerView.Adapter<OfficerAssignmentAdapter.OfficerViewHolder>() {
//
//    inner class OfficerViewHolder(val binding: ItemOfficerAssignmentBinding) : RecyclerView.ViewHolder(binding.root)
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfficerViewHolder {
//        val binding = ItemOfficerAssignmentBinding.inflate(
//            LayoutInflater.from(parent.context),
//            parent,
//            false
//        )
//        return OfficerViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: OfficerViewHolder, position: Int) {
//        val officer = officers[position]
//
//        with(holder.binding) {
//            tvOfficerName.text = officer.displayName
//            tvBadgeNumber.text = "Badge: ${officer.badgeNumber}"
//
//            // Set checkbox state without triggering listener
//            checkboxAssign.setOnCheckedChangeListener(null)
//            checkboxAssign.isChecked = officer.isSelected
//
//            // Set new listener
//            checkboxAssign.setOnCheckedChangeListener { _, isChecked ->
//                officer.isSelected = isChecked
//                onAssignmentChanged(officer, isChecked)
//            }
//        }
//    }
//
//    override fun getItemCount() = officers.size
//}