package com.example.argusapp.ui.citizen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.argusapp.R
import com.example.argusapp.databinding.ActivityCitizenMainBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.example.argusapp.ui.citizen.fragments.CitizenReportsFragment
import com.example.argusapp.ui.citizen.fragments.CitizenProfileFragment
import com.example.argusapp.ui.citizen.fragments.SosFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class CitizenMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCitizenMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Reference to the reports fragment for FAB handling
    private var reportsFragment: CitizenReportsFragment? = null

    companion object {
        private const val TAG = "CitizenMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCitizenMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Встановлення toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Мої заявки"

        // Subscribe to FCM topics
        subscribeToTopics()

        // Check if opened from notification
        handleNotificationIntent(intent)

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

    // Fix the override annotation and parameter type
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle notification when app is already running
        handleNotificationIntent(intent)
    }

    // Fix the parameter type to non-nullable
    private fun handleNotificationIntent(intent: Intent) {
        // Check for notification data
        if (intent.hasExtra("messageId")) {
            val messageId = intent.getStringExtra("messageId") ?: ""
            val messageType = intent.getStringExtra("type") ?: ""

            Log.d(TAG, "Received notification: messageId=$messageId, type=$messageType")

            // Navigate based on notification type if needed
            when (messageType) {
                "report_update" -> {
                    val reportId = intent.getStringExtra("reportId") ?: ""
                    if (reportId.isNotEmpty()) {
                        // Navigate to report details
                        val detailsIntent = Intent(this, ReportDetailsActivity::class.java)
                        detailsIntent.putExtra("reportId", reportId)
                        startActivity(detailsIntent)
                    }
                }
                "mass_message" -> {
                    // For mass messages, you might want to show a dialog or navigate to a notifications screen
                    Toast.makeText(this, "Отримано важливе повідомлення", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun subscribeToTopics() {
        // Subscribe to general topic
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to 'all' topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to 'all' topic", task.exception)
                }
            }

        // Subscribe to citizen-specific topic
        FirebaseMessaging.getInstance().subscribeToTopic("user_type_citizen")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to 'user_type_citizen' topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to 'user_type_citizen' topic", task.exception)
                }
            }

        // Store FCM token in user document
        updateFcmToken()
    }

    private fun updateFcmToken() {
        val currentUser = auth.currentUser ?: return

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM Token: $token")

                    // Update token in Firestore
                    db.collection("users").document(currentUser.uid)
                        .update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM token updated successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating FCM token", e)
                        }
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                }
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