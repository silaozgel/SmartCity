package com.smartcity.app.model

//Durum akışını kontrol etmek için
enum class ReportStatus(val displayName: String) {
    PENDING("Your request has been submitted."), //Default
    FORWARDED("Your request has been forwarded to the relevant department."),
    IN_PROGRESS("Processing of your request has been initiated."),
    RESOLVED("Your request has been resolved.")
}