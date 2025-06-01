// ui/admin/fragments/MassMessagingFragment.kt
package com.example.argusapp.ui.admin.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.argusapp.R
import com.example.argusapp.data.model.MassMessage
import com.example.argusapp.databinding.FragmentMassMessagingBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

class MassMessagingFragment : Fragment() {

    private var _binding: FragmentMassMessagingBinding? = null
    private val binding get() = _binding

    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMassMessagingBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        functions = Firebase.functions

        setupSpinners()
        setupSendButton()
        setupHistoryManagement()
        loadRecentMessages()
    }

    private fun setupSpinners() {
        binding?.let { safeBinding ->
            // Set up user type spinner
            val userTypes = arrayOf("all", "admin", "police", "citizen")
            val userTypeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                userTypes.map {
                    when(it) {
                        "all" -> "Всі користувачі"
                        "admin" -> "Адміністратори"
                        "police" -> "Поліцейські"
                        "citizen" -> "Громадяни"
                        else -> it
                    }
                }
            )
            safeBinding.spinnerUserType.adapter = userTypeAdapter

            // Set up topic spinner
            val topics = arrayOf("all", "emergency", "updates", "announcements")
            val topicAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                topics.map {
                    when(it) {
                        "all" -> "Всі теми"
                        "emergency" -> "Екстрені повідомлення"
                        "updates" -> "Оновлення"
                        "announcements" -> "Оголошення"
                        else -> it
                    }
                }
            )
            safeBinding.spinnerTopic.adapter = topicAdapter
        }
    }

    private fun setupSendButton() {
        binding?.buttonSend?.setOnClickListener {
            val title = binding?.editTextTitle?.text.toString().trim()
            val body = binding?.editTextMessage?.text.toString().trim()

            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(context, "Заголовок і текст повідомлення обов'язкові", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get selected user type and topic
            val userTypePosition = binding?.spinnerUserType?.selectedItemPosition ?: 0
            val topicPosition = binding?.spinnerTopic?.selectedItemPosition ?: 0

            val userTypes = arrayOf("all", "admin", "police", "citizen")
            val topics = arrayOf("all", "emergency", "updates", "announcements")

            val userType = userTypes[userTypePosition]
            val topic = topics[topicPosition]

            sendMassMessage(title, body, topic, userType)
        }
    }

    private fun setupHistoryManagement() {
        // Add buttons for managing history
        binding?.buttonRefreshHistory?.setOnClickListener {
            loadRecentMessages()
        }

        binding?.buttonClearHistory?.setOnClickListener {
            showClearHistoryConfirmation()
        }
    }

    private fun showClearHistoryConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Очистити історію повідомлень")
            .setMessage("Ви впевнені, що хочете видалити всю історію повідомлень? Цю дію неможливо скасувати.")
            .setPositiveButton("Так") { _, _ ->
                clearMessageHistory()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun clearMessageHistory() {
        binding?.progressBarHistory?.visibility = View.VISIBLE
        binding?.textViewMessageHistory?.text = "Видалення історії..."

        // Get all messages
        db.collection("mass_messages")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    binding?.progressBarHistory?.visibility = View.GONE
                    binding?.textViewMessageHistory?.text = "Історія повідомлень вже порожня"
                    return@addOnSuccessListener
                }

                // Create a batch to delete all messages
                val batch = db.batch()
                documents.forEach { document ->
                    batch.delete(document.reference)
                }

                // Commit the batch
                batch.commit()
                    .addOnSuccessListener {
                        if (isAdded && _binding != null) {
                            binding?.progressBarHistory?.visibility = View.GONE
                            binding?.textViewMessageHistory?.text = "Історію повідомлень очищено"
                            Toast.makeText(context, "Історію повідомлень очищено", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        if (isAdded && _binding != null) {
                            binding?.progressBarHistory?.visibility = View.GONE
                            binding?.textViewMessageHistory?.text = "Помилка видалення історії: ${e.message}"
                            Toast.makeText(context, "Помилка видалення історії: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                if (isAdded && _binding != null) {
                    binding?.progressBarHistory?.visibility = View.GONE
                    binding?.textViewMessageHistory?.text = "Помилка доступу до історії: ${e.message}"
                    Toast.makeText(context, "Помилка доступу до історії: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun sendMassMessage(title: String, body: String, topic: String, userType: String) {
        binding?.progressBar?.visibility = View.VISIBLE
        binding?.buttonSend?.isEnabled = false

        val data = hashMapOf(
            "title" to title,
            "body" to body,
            "topic" to topic,
            "userType" to userType
        )

        Log.d("MassMessage", "Sending message with data: $data")

        functions.getHttpsCallable("sendMassMessage")
            .call(data)
            .addOnSuccessListener { result ->
                Log.d("MassMessage", "Success response: ${result.data}")
                if (isAdded && _binding != null) {
                    binding?.progressBar?.visibility = View.GONE
                    binding?.buttonSend?.isEnabled = true

                    // Clear the form
                    binding?.editTextTitle?.text?.clear()
                    binding?.editTextMessage?.text?.clear()

                    Toast.makeText(context, "Повідомлення успішно надіслано", Toast.LENGTH_SHORT).show()

                    // Extract FCM response if available
                    val responseMap = result.data as? Map<*, *>
                    val fcmResponse = responseMap?.get("fcmResponse") as? String

                    // Log the message history
                    logMessageHistory(title, body, topic, userType, fcmResponse)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MassMessage", "Error sending message", e)
                if (isAdded && _binding != null) {
                    binding?.progressBar?.visibility = View.GONE
                    binding?.buttonSend?.isEnabled = true

                    val errorMessage = when {
                        e.message?.contains("authenticated") == true -> "Ви повинні бути авторизовані"
                        e.message?.contains("administrators") == true -> "Тільки адміністратори можуть надсилати повідомлення"
                        else -> "Помилка: ${e.message}"
                    }

                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun logMessageHistory(title: String, body: String, topic: String, userType: String, fcmResponse: String?) {
        val data = hashMapOf(
            "title" to title,
            "body" to body,
            "topic" to topic,
            "userType" to userType,
            "fcmResponse" to fcmResponse
        )

        Log.d("MassMessage", "Logging message history with data: $data")

        functions.getHttpsCallable("logMessageHistory")
            .call(data)
            .addOnSuccessListener { result ->
                Log.d("MassMessage", "Message history logged successfully: ${result.data}")
                // Reload the message history
                loadRecentMessages()
            }
            .addOnFailureListener { e ->
                Log.e("MassMessage", "Failed to log message history", e)
                Toast.makeText(context, "Повідомлення надіслано, але не збережено в історії: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadRecentMessages() {
        binding?.progressBarHistory?.visibility = View.VISIBLE
        binding?.textViewMessageHistory?.text = "Завантаження історії..."

        db.collection("mass_messages")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                if (isAdded && _binding != null) {
                    binding?.progressBarHistory?.visibility = View.GONE

                    val messagesText = StringBuilder()
                    if (documents.isEmpty) {
                        messagesText.append("Немає історії повідомлень")
                    } else {
                        documents.forEachIndexed { index, document ->
                            val message = document.toObject(MassMessage::class.java)
                            val formattedDate = message.sentAt?.toDate()?.let {
                                android.text.format.DateFormat.format("dd.MM.yyyy HH:mm", it)
                            } ?: "Невідома дата"

                            messagesText.append("${index + 1}. ${message.title}\n")
                            messagesText.append("Відправлено: $formattedDate\n")
                            messagesText.append("Кому: ${getUserTypeText(message.targetUserType)}\n")
                            messagesText.append("Статус: ${getStatusText(message.deliveryStatus)}\n\n")
                        }
                    }

                    binding?.textViewMessageHistory?.text = messagesText.toString()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded && _binding != null) {
                    binding?.progressBarHistory?.visibility = View.GONE
                    binding?.textViewMessageHistory?.text = "Помилка завантаження історії: ${e.message}"
                }
            }
    }

    private fun getUserTypeText(userType: String): String {
        return when(userType) {
            "all" -> "Всі користувачі"
            "admin" -> "Адміністратори"
            "police" -> "Поліцейські"
            "citizen" -> "Громадяни"
            else -> userType
        }
    }

    private fun getStatusText(status: String): String {
        return when(status) {
            "processing" -> "Обробляється"
            "sent" -> "Надіслано"
            "failed" -> "Помилка"
            else -> status
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}