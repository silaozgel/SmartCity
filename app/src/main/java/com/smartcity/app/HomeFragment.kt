package com.smartcity.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.smartcity.app.adapter.ReportAdapter
import com.smartcity.app.databinding.FragmentHomeBinding
import com.smartcity.app.viewmodel.CitizenViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CitizenViewModel by viewModels()
    private lateinit var reportAdapter: ReportAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // İzin verildiğinde çalışacak kodlar buraya eklenebilir
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bildirim izni kontrolü (Android 13+ için)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            binding.tvEmail.text = currentUser.email
        } else {
            binding.tvEmail.text = "Guest"
        }

        reportAdapter = ReportAdapter(isAdmin = false) {}
        binding.rvCitizenReports.adapter = reportAdapter

        viewModel.myReports.observe(viewLifecycleOwner) { reports ->
            reportAdapter.setReports(reports)
            if (reports.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvCitizenReports.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvCitizenReports.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarCitizen.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.citizenPoints.observe(viewLifecycleOwner) { points ->
            binding.tvPoints.text = "Total Points: $points"
        }

        viewModel.fetchMyReports()
        viewModel.listenToMyPoints()

        viewModel.listenToMyReports { report ->
            showResolvedNotification(report.title)
        }

        // Talep oluşturma butonu
        binding.fabAddReport.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_submitReportFragment)
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }

        // Footer tıklama işlemleri
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home_tab -> {
                    if (binding.layoutHome.visibility != View.VISIBLE) {
                        binding.layoutHome.visibility = View.VISIBLE
                        binding.layoutProfile.visibility = View.GONE
                    }
                    true
                }
                R.id.nav_profile_tab -> {
                    if (binding.layoutProfile.visibility != View.VISIBLE) {
                        binding.layoutHome.visibility = View.GONE
                        binding.layoutProfile.visibility = View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }

        // Form sayfasından dönen sekme hedefini dinler. Hata alındığı için eklendi
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("target_tab")?.observe(viewLifecycleOwner) { tab ->
            when (tab) {
                "profile" -> {
                    binding.bottomNav.selectedItemId = R.id.nav_profile_tab
                }
                "home" -> {
                    binding.bottomNav.selectedItemId = R.id.nav_home_tab
                }
            }
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("target_tab")
        }
    }

    private fun showResolvedNotification(reportTitle: String) {
        val channelId = "smart_city_reports"
        val notificationManager = requireContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Request Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your request has been resolved!")
            .setContentText("Your request titled \"$reportTitle\" has been marked as resolved. You earned 10 points.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
