package com.smartcity.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.smartcity.app.adapter.ReportAdapter
import com.smartcity.app.databinding.FragmentAdminBinding
import com.smartcity.app.model.ReportStatus
import com.smartcity.app.viewmodel.AdminViewModel

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var reportAdapter: ReportAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Admin durum güncelleme adapterı
        reportAdapter = ReportAdapter(isAdmin = true) { clickedReport ->
            val nextStatus = when (clickedReport.status) {
                ReportStatus.PENDING.name -> ReportStatus.FORWARDED
                ReportStatus.FORWARDED.name -> ReportStatus.IN_PROGRESS
                ReportStatus.IN_PROGRESS.name -> ReportStatus.RESOLVED
                else -> null
            }

            if (nextStatus != null) {
                viewModel.updateReportStatus(clickedReport, nextStatus)
                Toast.makeText(requireContext(), "Status updated: ${nextStatus.displayName}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "This report has already been resolved.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvAdminReports.adapter = reportAdapter

        // ViewModel verilerini dinler
        viewModel.allReports.observe(viewLifecycleOwner) { reports ->
            reportAdapter.setReports(reports)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarAdmin.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Sayfa açıldığında verileri çeker
        viewModel.fetchAllReports()

        binding.btnAdminLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_adminFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}