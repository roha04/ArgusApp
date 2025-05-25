package com.example.argusapp.ui.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.argusapp.R
import com.example.argusapp.databinding.FragmentFilterBinding

class FilterFragment : Fragment() {

    private var _binding: FragmentFilterBinding? = null
    private val binding get() = _binding!!

    // Interface for filter callbacks
    interface FilterListener {
        fun applyFilters(filterOptions: Map<String, String>) {}
        fun resetFilters() {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Close button
        binding.buttonClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Apply filters button
        binding.buttonApply.setOnClickListener {
            applyFilters()
        }

        // Reset filters button
        binding.buttonReset.setOnClickListener {
            resetFilters()
        }
    }

    private fun applyFilters() {
        // Get selected role filter
        val selectedRole = when {
            binding.radioAll.isChecked -> "all"
            binding.radioCitizens.isChecked -> "citizen"
            binding.radioPolice.isChecked -> "police"
            binding.radioAdmins.isChecked -> "admin"
            else -> "all"
        }

        // Create filter options map
        val filterOptions = mapOf("role" to selectedRole)

        // Find current fragment behind the filter panel
        val fragments = parentFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment !is FilterFragment && fragment is FilterListener) {
                // Apply filters if fragment implements FilterListener
                (fragment as FilterListener).applyFilters(filterOptions)
                break
            }
        }

        // Close the filter panel
        parentFragmentManager.popBackStack()
    }

    private fun resetFilters() {
        // Reset radio buttons to default
        binding.radioAll.isChecked = true

        // Find current fragment behind the filter panel
        val fragments = parentFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment !is FilterFragment && fragment is FilterListener) {
                // Reset filters if fragment implements FilterListener
                (fragment as FilterListener).resetFilters()
                break
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}