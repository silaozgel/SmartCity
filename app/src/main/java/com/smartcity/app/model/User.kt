package com.smartcity.app.model

//Rolleri tutmak için

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val uid: String = "",
    val email: String = "",
    val role: String = "citizen", // Citizen veya Admin, Firebase'den kontrol edilebilecek
    val points: Int = 0
)