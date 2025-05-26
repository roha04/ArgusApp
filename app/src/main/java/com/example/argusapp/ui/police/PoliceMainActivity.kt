package com.example.argusapp.ui.police


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.argusapp.R
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.example.argusapp.databinding.ActivityPoliceMainBinding
import com.example.argusapp.ui.auth.LoginActivity
import com.example.argusapp.ui.common.MapActivity
import com.example.argusapp.ui.police.fragments.CompletedReportsFragment
import com.example.argusapp.ui.police.fragments.InProgressReportsFragment
import com.example.argusapp.ui.police.fragments.NewReportsFragment
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore



class PoliceMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoliceMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoliceMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Встановлення toolbar
        setSupportActionBar(binding.toolbar)

        // Налаштування ViewPager та TabLayout
        setupViewPager()

        // Кнопка для перегляду карти правопорушень
        binding.fabMapView.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        testReportsQuery()

    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Нові"
                1 -> "В обробці"
                2 -> "Завершені"
                else -> null
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_police, menu)
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
            R.id.action_profile -> {
                startActivity(Intent(this, PoliceProfileActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> NewReportsFragment()
                1 -> InProgressReportsFragment()
                2 -> CompletedReportsFragment()
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