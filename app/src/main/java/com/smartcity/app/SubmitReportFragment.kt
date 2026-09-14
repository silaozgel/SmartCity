package com.smartcity.app

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.smartcity.app.databinding.FragmentSubmitReportBinding
import com.smartcity.app.viewmodel.CitizenViewModel
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class SubmitReportFragment : Fragment() {

    private var _binding: FragmentSubmitReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CitizenViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    // Galeriden fotoğraf seçme işlemi
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.setImageURI(uri)
            binding.ivPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubmitReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Form kategorileri
        val categories = arrayOf(
            "Infrastructure (Pit, Water, etc.)",
            "Cleaning & Waste",
            "Transportation & Traffic",
            "Parks & Gardens",
            "Lighting",
            "Other"
        )

        //kategörileri ui'a bağlar
        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        binding.etCategory.setAdapter(adapter)

        // Fotoğraf seçme butonu
        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*") // Sadece resim dosyalarını filtreler
        }

        // Gönder butonu
        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            var base64Image = ""
            if (selectedImageUri != null) {
                base64Image = encodeImageToBase64(selectedImageUri!!)
            }


            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(requireContext(), "The title and description cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ViewModel üzerinden veritabanına yazar
            viewModel.submitNewReport(title, description, category, address, base64Image)
        }

        // Form sayfasındayken görünmez orta sekmenin seçili olmasını sağla (böylece My Requests veya Profile seçili kalıp kalın görünmez)
        binding.bottomNavSubmit.selectedItemId = R.id.nav_placeholder

        // ViewModel'den gelen yükleme durumunu dinler
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnSubmit.isEnabled = false
            } else {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true

                showSuccessNotification()

                Toast.makeText(requireContext(), "The report was sent successfully.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // İşlem bitince ana ekrana dön
            }
        }

        // Footer işlemleri
        binding.bottomNavSubmit.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home_tab -> {
                    findNavController().previousBackStackEntry?.savedStateHandle?.set("target_tab", "home")
                    findNavController().popBackStack()
                    true
                }
                R.id.nav_profile_tab -> {
                    findNavController().previousBackStackEntry?.savedStateHandle?.set("target_tab", "profile")
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        binding.fabAddReportSubmit.setOnClickListener {

        }
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }

        val outputStream = ByteArrayOutputStream()
        // Bite array 1MB sınırını aşmamak için fotoğraf boyutunu düşürüp kaliteyi azaltır.
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 800, 800, true)
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)

        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun showSuccessNotification() {
        val channelId = "smart_city_reports"
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // "Bildirim kanalı" zorunlu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Request Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Bildirimin tasarımı
        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Logo ikonunu düzenleyebilirsin (ör: R.drawable.ic_logo)
            .setContentTitle("Your request has been submitted.")
            .setContentText("Your request has been successfully recorded in the system and is being reviewed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Bildirimi ekranda gösterir
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
