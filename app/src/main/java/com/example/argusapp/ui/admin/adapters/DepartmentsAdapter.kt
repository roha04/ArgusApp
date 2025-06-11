// Update your DepartmentsAdapter.kt file
package com.example.argusapp.ui.admin.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.argusapp.R
import com.example.argusapp.data.model.Department
import com.example.argusapp.databinding.ItemDepartmentBinding
import com.google.firebase.firestore.FirebaseFirestore

class DepartmentsAdapter(
    private val departments: MutableList<Department>,
    private val onItemClick: (Department) -> Unit
) : RecyclerView.Adapter<DepartmentsAdapter.DepartmentViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "DepartmentsAdapter"

    inner class DepartmentViewHolder(val binding: ItemDepartmentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val binding = ItemDepartmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DepartmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        val department = departments[position]

        // Додаємо логування для налагодження
        Log.d(TAG, "Binding department: ${department.name}, isActive: ${department.isActive}")

        with(holder.binding) {
            tvDepartmentName.text = department.name
            tvDepartmentAddress.text = department.address

            // Переклад на українську
            val officerText = if ((department.officerCount ?: 0) == 1) {
                "1 поліцейський"
            } else {
                "${department.officerCount ?: 0} поліцейських"
            }
            tvOfficerCount.text = officerText

            // Встановлюємо колір індикатора статусу
            try {
                val resourceId = if (department.isActive) {
                    R.drawable.bg_status_active
                } else {
                    R.drawable.bg_status_inactive
                }
                Log.d(TAG, "Setting status indicator for ${department.name}: isActive=${department.isActive}, resourceId=$resourceId")
                statusIndicator.setBackgroundResource(resourceId)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting status indicator: ${e.message}")
            }

            // Обробник кліку на елемент
            root.setOnClickListener {
                Log.d(TAG, "Clicked on department: ${department.name}")
                onItemClick(department)
            }

            // Обробник кліку на кнопку видалення
            btnDeleteDepartment.setOnClickListener {
                Log.d(TAG, "Delete button clicked for department: ${department.name}")
                // Видалення відділу з Firestore
                deleteDepartment(department, position, root.context)
            }
        }
    }

    private fun deleteDepartment(department: Department, position: Int, context: android.content.Context) {
        department.id?.let { departmentId ->
            Log.d(TAG, "Deleting department with ID: $departmentId")

            db.collection("departments").document(departmentId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Department successfully deleted")
                    // Видаляємо елемент з локального списку та оновлюємо RecyclerView
                    departments.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, departments.size)

                    // Показуємо повідомлення про успішне видалення
                    Toast.makeText(context, "Відділ успішно видалено", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error deleting department", e)
                    // Показуємо повідомлення про помилку
                    Toast.makeText(context, "Помилка видалення відділу: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Log.e(TAG, "Cannot delete department: ID is null")
            Toast.makeText(context, "Помилка: ID відділу не вказано", Toast.LENGTH_SHORT).show()
        }
    }

    // Метод для оновлення списку відділів
    fun updateDepartments(newDepartments: List<Department>) {
        Log.d(TAG, "Updating departments list. New size: ${newDepartments.size}")
        departments.clear()
        departments.addAll(newDepartments)
        notifyDataSetChanged()
    }

    override fun getItemCount() = departments.size
}