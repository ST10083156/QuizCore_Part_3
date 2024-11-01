package com.example.opsc_quizcore.Models

import java.sql.Time

data class SettingsModel(
    val userid : String,
    val mode : String = "Light",
    val theme : String = "Red",
    val size: String = "Small",
    val remindertime: Time? = null
)
