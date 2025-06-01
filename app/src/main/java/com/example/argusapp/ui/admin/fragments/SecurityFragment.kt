// ui/admin/fragments/SecurityFragment.kt
package com.example.argusapp.ui.admin.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.argusapp.databinding.FragmentSecurityBinding
import com.google.android.material.tabs.TabLayoutMediator

class SecurityFragment : Fragment() {

    private var _binding: FragmentSecurityBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up ViewPager with tabs
        val tabTitles = arrayOf("Журнал активності", "Сповіщення безпеки", "Масові повідомлення")

        binding.viewPager.adapter = SecurityPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class SecurityPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3  // Updated to 3 tabs

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ActivityLogsFragment()
                1 -> SecurityAlertsFragment()
                2 -> MassMessagingFragment()
                else -> ActivityLogsFragment()
            }
        }
    }
}