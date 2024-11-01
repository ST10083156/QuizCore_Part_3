package com.example.opsc_poe_part_2

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe_part_2.Managers.LanguageManager
import com.example.opsc_poe_part_2.databinding.ActivityDashboardBinding
import com.example.opsc_poe_part_2.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding : ActivitySettingsBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private lateinit var languageManager : LanguageManager
    private lateinit var currentLanguageCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        binding.toolbar.pageNameTxt.text = getString(R.string.settings)
        binding.toolbar.backBtn.setOnClickListener {
            val intent = Intent(this,DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.toolbar.backBtn.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.loginStatusBtn.text = getString(R.string.sign_out)
        binding.toolbar.loginStatusBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this,"Signed Out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        // Retrieve the current language from the app's resources or shared preferences
        currentLanguageCode = getCurrentLanguageCode()

        // Set up the spinner with languages
        val languages = listOf("English", "Spanish", "French")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = adapter

        // Set the current selected item in the spinner
        binding.languageSpinner.setSelection(languages.indexOf(getLanguageDisplayName(currentLanguageCode)))

        languageManager = LanguageManager(this)
        binding.languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = parent.getItemAtPosition(position).toString()
                Log.d("LanguageSelection", "Selected language: $selectedLanguage")
                val selectedLanguageCode = when (selectedLanguage) {
                    "Spanish" -> "es"
                    "French" -> "fr"
                    else -> "en"
                }
                    setSelectedLanguageCode(selectedLanguageCode)
                if (selectedLanguageCode != currentLanguageCode) {
                    languageManager.updateResource(selectedLanguageCode)
                    currentLanguageCode = selectedLanguageCode
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.setNotificationTimeBtn.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun getCurrentLanguageCode(): String {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        return sharedPreferences.getString("selected_language", Locale.getDefault().language) ?: Locale.getDefault().language
    }

    fun setSelectedLanguageCode(languageCode: String) {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("selected_language", languageCode)
            apply()
        }
    }

    private fun getLanguageDisplayName(code: String): String {
        return when (code) {
            "es" -> "Spanish"
            "fr" -> "French"
            else -> "English"
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            scheduleDailyNotification(this, selectedHour, selectedMinute)
        }, hour, minute, true)

        timePickerDialog.show()
    }

    fun scheduleDailyNotification(context: Context, hour: Int, minute: Int) {
        Log.d("SettingsActivity", "Scheduling notification for $hour:$minute")
        val intent = Intent(context, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        Log.d("SettingsActivity","Calender time : ${calendar.time}")
        // If the time is in the past, add a day
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
        Log.d("SettingsActivity", "Alarm set for: ${calendar.timeInMillis}")

    }



}
class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
    }
    fun showNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, "DAILY_NOTIFICATION_CHANNEL")
            .setSmallIcon(R.drawable.quiz)
            .setContentTitle("Daily Reminder")
            .setContentText("Time to get quizzical!")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(1001, notificationBuilder.build())
    }


}
