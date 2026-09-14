package com.smartcity.app.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcity.app.model.Report
import com.smartcity.app.repository.ReportRepository
import kotlinx.coroutines.launch

//Kullanıcıların raporlarını yönetme ve bildirim token'larını kaydetme işlemlerini yönetir

class CitizenViewModel : ViewModel() {

    private val repository = ReportRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _myReports = MutableLiveData<List<Report>>()
    val myReports: LiveData<List<Report>> = _myReports

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchMyReports() { //Kullanıcılara ana sayfada kendi taleplerini gösterir
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val reports = repository.getCitizenReports(userId)
                _myReports.value = reports
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitNewReport(title: String, description: String, category: String, address: String, base64Image: String) {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val newReport = Report(
                    citizenId = userId,
                    title = title,
                    description = description,
                    category = category,
                    address = address
                )
                repository.submitReport(newReport, base64Image)
                fetchMyReports()
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _citizenPoints = MutableLiveData<Int>()
    val citizenPoints: LiveData<Int> = _citizenPoints

    fun listenToMyPoints() {
        val userId = auth.currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val points = snapshot.getLong("points")?.toInt() ?: 0
                    _citizenPoints.value = points
                } else {
                    _citizenPoints.value = 0
                }
            }
    }

    private var isFirstLoad = true
    private var reportsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun listenToMyReports(onReportResolved: (Report) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        
        // Önceki dinleyiciyi temizle
        reportsListener?.remove()
        
        reportsListener = FirebaseFirestore.getInstance().collection("reports")
            .whereEqualTo("citizenId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val currentReports = snapshot.toObjects(Report::class.java).sortedByDescending { it.timestamp }
                    _myReports.value = currentReports

                    if (!isFirstLoad) {
                        for (docChange in snapshot.documentChanges) {
                            if (docChange.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                val updatedReport = docChange.document.toObject(Report::class.java)
                                // Sadece resolved yapıldığında tetikler
                                if (updatedReport.status == "RESOLVED") {
                                    onReportResolved(updatedReport)
                                }
                            }
                        }
                    } else {
                        isFirstLoad = false
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        reportsListener?.remove()
    }
}