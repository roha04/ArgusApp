package com.example.argusapp.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Set up toolbar
        setSupportActionBar(binding.toolbar)

        // Setup ViewPager with Bottom Navigation
        setupNavigation()

        // FAB for adding police officers
        setupFab()
    }

    private fun setupNavigation() {
        // Set up ViewPager
        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        // Prevent manual swiping (optional - remove if you want swipe capability)
        // binding.viewPager.isUserInputEnabled = false

        // Handle page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Update bottom navigation when page changes
                binding.bottomNavigation.menu.getItem(position).isChecked = true

                // Show/hide FAB based on selected tab
                binding.fabAddPolice.visibility = if (position == 0) View.VISIBLE else View.GONE
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
                0 -> UsersFragment()
                1 -> ReportsFragment()
                2 -> StatisticsFragment()
                3 -> ProfileFragment() // You'll need to create this fragment
                else -> UsersFragment()
            }
        }
    }
}
//package com.example.argusapp.ui.admin
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.Menu
//import android.view.MenuItem
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.FragmentActivity
//import androidx.viewpager2.adapter.FragmentStateAdapter
//import com.google.android.material.tabs.TabLayoutMediator
//import com.google.firebase.auth.FirebaseAuth
//import com.example.argusapp.R
//import com.example.argusapp.databinding.ActivityAdminMainBinding
//import com.example.argusapp.ui.admin.RegisterPoliceActivity
//import com.example.argusapp.ui.admin.fragments.ReportsFragment
//import com.example.argusapp.ui.admin.fragments.StatisticsFragment
//import com.example.argusapp.ui.admin.fragments.UsersFragment
//import com.example.argusapp.ui.auth.LoginActivity
//
//class AdminMainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityAdminMainBinding
//    private lateinit var auth: FirebaseAuth
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityAdminMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        auth = FirebaseAuth.getInstance()
//
//        // Встановлення toolbar
//        setSupportActionBar(binding.toolbar)
//
//        // Налаштування ViewPager та TabLayout
//        setupViewPager()
//
//        // Кнопка для додавання поліцейського
//        binding.fabAddPolice.setOnClickListener {
//            startActivity(Intent(this, RegisterPoliceActivity::class.java))
//        }
//    }
//
//    private fun setupViewPager() {
//        val adapter = ViewPagerAdapter(this)
//        binding.viewPager.adapter = adapter
//
//        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
//            tab.text = when (position) {
//                0 -> "Користувачі"
//                1 -> "Заявки"
//                2 -> "Статистика"
//                else -> null
//            }
//        }.attach()
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_admin, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_logout -> {
//                auth.signOut()
//                startActivity(Intent(this, LoginActivity::class.java))
//                finish()
//                true
//            }
//            R.id.action_profile -> {
//                Toast.makeText(this, "Профіль адміністратора", Toast.LENGTH_SHORT).show()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
//        override fun getItemCount(): Int = 3
//
//        override fun createFragment(position: Int): Fragment {
//            return when (position) {
//                0 -> UsersFragment()
//                1 -> ReportsFragment()
//                2 -> StatisticsFragment()
//                else -> UsersFragment()
//            }
//        }
//    }
//}