package com.example.argusapp.ui.citizen

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.argusapp.R
import com.example.argusapp.databinding.ActivityCitizenMainBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.example.argusapp.ui.citizen.fragments.CitizenReportsFragment
import com.example.argusapp.ui.citizen.fragments.CitizenProfileFragment
import com.example.argusapp.ui.citizen.fragments.SosFragment
import com.google.firebase.auth.FirebaseAuth

class CitizenMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitizenMainBinding
    private lateinit var auth: FirebaseAuth

    // Reference to the reports fragment for FAB handling
    private var reportsFragment: CitizenReportsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitizenMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Встановлення toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Мої заявки"

        // Налаштування BottomNavigationView
        setupBottomNavigation()

        // Кнопка для створення нової заявки
        binding.fabAddReport.setOnClickListener {
            // If we're on the reports fragment, delegate the click
            if (reportsFragment != null && reportsFragment?.isVisible == true) {
                reportsFragment?.onAddReportClicked()
            } else {
                // Direct handling if fragment reference is not available
                startActivity(Intent(this, CreateReportActivity::class.java))
            }
        }

        // За замовчуванням показуємо фрагмент заявок
        if (savedInstanceState == null) {
            loadReportsFragment()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_reports -> {
                    supportActionBar?.title = "Мої заявки"
                    binding.fabAddReport.show()
                    loadReportsFragment()
                    true
                }
                R.id.nav_sos -> {  // Changed from nav_map to nav_sos
                    supportActionBar?.title = "SOS"
                    binding.fabAddReport.hide()
                    replaceFragment(SosFragment())  // Changed from MapFragment to SOSFragment
                    reportsFragment = null
                    true
                }
                R.id.nav_profile -> {
                    supportActionBar?.title = "Профіль"
                    binding.fabAddReport.hide()
                    replaceFragment(CitizenProfileFragment())
                    reportsFragment = null
                    true
                }
                else -> false
            }
        }
    }

    private fun loadReportsFragment() {
        // Create new instance if needed
        if (reportsFragment == null) {
            reportsFragment = CitizenReportsFragment()
        }

        replaceFragment(reportsFragment!!)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_citizen, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}