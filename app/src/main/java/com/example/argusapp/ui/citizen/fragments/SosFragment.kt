package com.example.argusapp.ui.citizen.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.argusapp.databinding.FragmentSosBinding
import com.example.argusapp.utils.LocationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SosFragment : Fragment() {

    private var _binding: FragmentSosBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var locationHelper: LocationHelper

    private val CALL_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        locationHelper = LocationHelper(requireContext())

        setupSosButton()
        setupEmergencyButtons()
        loadEmergencyContacts()
    }

    private fun setupSosButton() {
        binding.buttonSos.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Екстрений виклик")
                .setMessage("Ви впевнені, що хочете викликати екстрені служби?")
                .setPositiveButton("Так") { _, _ ->
                    sendSosSignal()
                }
                .setNegativeButton("Ні", null)
                .show()
        }
    }

    private fun setupEmergencyButtons() {
        binding.buttonCallPolice.setOnClickListener {
            makeEmergencyCall("102")
        }

        binding.buttonCallAmbulance.setOnClickListener {
            makeEmergencyCall("103")
        }

        binding.buttonCallFirefighters.setOnClickListener {
            makeEmergencyCall("101")
        }

        binding.buttonCallEmergency.setOnClickListener {
            makeEmergencyCall("112")
        }
    }

    private fun makeEmergencyCall(number: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_REQUEST_CODE
            )
            return
        }

        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        startActivity(intent)
    }

    private fun sendSosSignal() {
        showLoading(true)

        // Get current location
        locationHelper.getCurrentLocation(
            onSuccess = { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)

                // Get address from location
                locationHelper.getAddressFromLocation(
                    location.latitude,
                    location.longitude,
                    onSuccess = { address ->
                        createSosAlert(geoPoint, address)
                    },
                    onError = { error ->
                        createSosAlert(geoPoint, "Невідома адреса")
                        showError("Не вдалося отримати адресу: $error")
                    }
                )
            },
            onError = { error ->
                showLoading(false)
                showError("Помилка отримання місцезнаходження: $error")
            }
        )
    }

    private fun createSosAlert(location: GeoPoint, address: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            showError("Помилка аутентифікації")
            return
        }

        // Get user info
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("displayName") ?: "Невідомо"
                val userPhone = document.getString("phone") ?: "Невідомо"

                // Create SOS alert
                val sosAlert = hashMapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "userPhone" to userPhone,
                    "location" to location,
                    "address" to address,
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "status" to "new",
                    "notes" to "Екстрений виклик"
                )

                // Save to Firestore
                db.collection("sos_alerts").add(sosAlert)
                    .addOnSuccessListener {
                        showLoading(false)
                        showSuccess()
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        showError("Помилка відправлення сигналу: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Помилка отримання даних користувача: ${e.message}")
            }
    }

    private fun loadEmergencyContacts() {
        // You could load custom emergency contacts from Firestore here
        // For now, we'll just display the standard emergency numbers
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        binding.textViewLastUpdated.text = "Останнє оновлення: ${dateFormat.format(Date())}"
    }

    private fun showSuccess() {
        binding.layoutSuccess.visibility = View.VISIBLE
        binding.layoutMain.visibility = View.GONE

        // Hide success message after some time
        binding.layoutSuccess.postDelayed({
            if (isAdded && _binding != null) {
                binding.layoutSuccess.visibility = View.GONE
                binding.layoutMain.visibility = View.VISIBLE
            }
        }, 5000) // Hide after 5 seconds
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonSos.isEnabled = !isLoading
        binding.buttonCallPolice.isEnabled = !isLoading
        binding.buttonCallAmbulance.isEnabled = !isLoading
        binding.buttonCallFirefighters.isEnabled = !isLoading
        binding.buttonCallEmergency.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CALL_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Дозвіл на дзвінки надано", Toast.LENGTH_SHORT).show()
            } else {
                showError("Для здійснення екстрених дзвінків потрібен дозвіл")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}