package com.smartcity.app.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcity.app.model.Report
import com.smartcity.app.model.ReportStatus
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

//Forma fotoğraf yükleme, firestore'dan veri çekip yükleme işlemleri


class ReportRepository {

    private val db = FirebaseFirestore.getInstance()

    // Yeni talep oluşturur
    suspend fun submitReport(report: Report, base64Image: String) {
        val reportToSave = report.copy(imageUrl = base64Image)
        db.collection("reports").document().set(reportToSave).await() //Firestore'a reports klasörü oluşturur, içine yazar
    }

    // Vatandaşlara ana ekranda kendi taleplerini gösterir
    suspend fun getCitizenReports(citizenId: String): List<Report> {
        val snapshot = db.collection("reports")
            .whereEqualTo("citizenId", citizenId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.toObjects(Report::class.java)
    }

    // Admin paneline tüm raporları çeker
    suspend fun getAllReports(): List<Report> {
        val snapshot = db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.toObjects(Report::class.java)
    }

    // Admin taleplerin durumunu günceller
    suspend fun updateReportStatus(report: Report, newStatus: ReportStatus) {
        // Raporun durumunu günceller
        db.collection("reports").document(report.reportId)
            .update("status", newStatus.name)
            .await()

        // Durum resolved ise kayıtlı kullanıcıya 10 puan ekler
        if (newStatus == ReportStatus.RESOLVED && report.citizenId.isNotEmpty()) {
            try {
                db.collection("users").document(report.citizenId)
                    .set(mapOf("points" to FieldValue.increment(10)), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                // Kullanıcı misafirse veya erişim sorunu varsa kodun çökmesini engeller
                android.util.Log.e("ReportRepository", "Points could not be added: ${e.localizedMessage}")
            }
        }
    }
}