package com.smartcity.app.model

//Vatandaşın göndereceği verilerini tutmak için

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Report(
    @DocumentId val reportId: String = "",
    val citizenId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val status: String = ReportStatus.PENDING.name,
    @ServerTimestamp val timestamp: Date? = null //Firestore index'ler için
)