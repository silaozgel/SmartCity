package com.smartcity.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcity.app.model.Report
import com.smartcity.app.model.ReportStatus
import com.smartcity.app.repository.ReportRepository
import kotlinx.coroutines.launch

//Adminin tüm raporları yönetmesi ve durumlarını güncellemesini sağlar

class AdminViewModel : ViewModel() {

    private val repository = ReportRepository()

    private val _allReports = MutableLiveData<List<Report>>()
    val allReports: LiveData<List<Report>> = _allReports

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchAllReports() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val reports = repository.getAllReports()
                _allReports.value = reports
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateReportStatus(report: Report, newStatus: ReportStatus) {
        viewModelScope.launch {
            try {
                repository.updateReportStatus(report, newStatus)
                fetchAllReports()
            } catch (e: Exception) {
            }
        }
    }
}