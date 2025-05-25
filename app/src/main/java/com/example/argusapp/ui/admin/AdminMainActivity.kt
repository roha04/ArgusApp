package com.example.argusapp.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.argusapp.R
import com.example.argusapp.databinding.ActivityAdminMainBinding
import com.example.argusapp.ui.admin.fragments.ProfileFragment
import com.example.argusapp.ui.admin.fragments.ReportsFragment
import com.example.argusapp.ui.admin.fragments.StatisticsFragment
import com.example.argusapp.ui.admin.fragments.UsersFragment
import com.example.argusapp.ui.auth.LoginActivity

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private lateinit var auth: FirebaseAuth
    private var usersFragment: UsersFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Setup ViewPager with Bottom Navigation
        setupNavigation()

        // Setup Filter Button
        setupFilterButton()

        // FAB for adding police officers
        setupFab()
    }

    private fun setupNavigation() {
        // Set up ViewPager
        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        // Handle page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Update bottom navigation when page changes
                binding.bottomNavigation.menu.getItem(position).isChecked = true

                // Show/hide FAB based on selected tab
                binding.fabAddPolice.visibility = if (position == 0) View.VISIBLE else View.GONE

                // Show/hide filter button based on selected tab
                binding.btnFilter.visibility = if (position == 0) View.VISIBLE else View.GONE
            }
        })

        // Handle bottom navigation clicks
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_reports -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.nav_statistics -> {
                    binding.viewPager.currentItem = 2
                    true
                }
                R.id.nav_profile -> {
                    binding.viewPager.currentItem = 3
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_user_filter, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.filter_all -> {
                        usersFragment?.filterByRole(null)
                        true
                    }
                    R.id.filter_admin -> {
                        usersFragment?.filterByRole("admin")
                        true
                    }
                    R.id.filter_police -> {
                        usersFragment?.filterByRole("police")
                        true
                    }
                    R.id.filter_user -> {
                        usersFragment?.filterByRole("user")
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }
    }

    private fun setupFab() {
        binding.fabAddPolice.setOnClickListener {
            startActivity(Intent(this, RegisterPoliceActivity::class.java))
        }
    }

    // Show logout confirmation
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Вийти з облікового запису")
            .setMessage("Ви дійсно бажаєте вийти?")
            .setPositiveButton("Так") { _, _ ->
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Ні", null)
            .show()
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    // Create and store a reference to the UsersFragment
                    usersFragment = UsersFragment()
                    usersFragment!!
                }
                1 -> ReportsFragment()
                2 -> StatisticsFragment()
                3 -> ProfileFragment()
                else -> UsersFragment()
            }
        }
    }
}
