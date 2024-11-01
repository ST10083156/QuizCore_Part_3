package com.example.opsc_poe_part_2.Managers

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.app.ActivityCompat.recreate
import java.util.Locale

class LanguageManager(private val context: Context) {

    fun updateResource(code: String) {
        val locale = Locale(code)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)

        resources.updateConfiguration(configuration, resources.displayMetrics)

    }

}
