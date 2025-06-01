package com.example.argusapp.ui.police

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.argusapp.R
import com.example.argusapp.databinding.ActivityPoliceMainBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.example.argusapp.ui.common.MapActivity
import com.example.argusapp.ui.police.fragments.CompletedReportsFragment
import com.example.argusapp.ui.police.fragments.InProgressReportsFragment
import com.example.argusapp.ui.police.fragments.NewReportsFragment
import com.example.argusapp.ui.police.fragments.PoliceProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class PoliceMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoliceMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoliceMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)

        // Set up ViewPager with Bottom Navigation
        setupNavigation()

        // Test reports query for debugging
        testReportsQuery()
        subscribeToTopics()

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
            }
        })

        // Disable swiping for map page
        binding.viewPager.isUserInputEnabled = true

        // Handle bottom navigation clicks
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_new_reports -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_in_progress -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.nav_completed -> {
                    binding.viewPager.currentItem = 2
                    true
                }
                R.id.nav_map -> {
                    // Launch Map Activity directly instead of showing a fragment
                    val intent = Intent(this, MapActivity::class.java)
                    intent.putExtra("IS_POLICE_MODE", true)
                    startActivity(intent)
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

        // Subscribe to police-specific topic
        FirebaseMessaging.getInstance().subscribeToTopic("user_type_police")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to 'user_type_police' topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to 'user_type_police' topic", task.exception)
                }
            }

        // Add debug log for token
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "FCM Token: $token")
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                }
            }
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 4 // Only 4 pages in ViewPager (map is an activity)

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> NewReportsFragment()
                1 -> InProgressReportsFragment()
                2 -> CompletedReportsFragment()
                3 -> PoliceProfileFragment()
                else -> NewReportsFragment()
            }
        }
    }

    private fun testReportsQuery() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        Log.d("PoliceDebug", "Testing direct query for user: $currentUserId")

        db.collection("reports")
            .whereEqualTo("assignedToId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("PoliceDebug", "Direct query found ${documents.size()} documents")

                for (doc in documents) {
                    Log.d("PoliceDebug", "Document ID: ${doc.id}")
                    Log.d("PoliceDebug", "Title: ${doc.getString("title")}")
                    Log.d("PoliceDebug", "Status: ${doc.getString("status")}")
                    Log.d("PoliceDebug", "AssignedToId: ${doc.getString("assignedToId")}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("PoliceDebug", "Direct query failed: ${e.message}")
            }
    }
}