package com.example.opsc_poe_part_2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.lifecycle.lifecycleScope
import com.example.opsc_poe_part_2.Managers.DatabaseProvider
import com.example.opsc_poe_part_2.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private lateinit var biometricPromptManager: BiometricPromptManager
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val userScoreDao by lazy { DatabaseProvider.getDatabase(this).userScoreDao() }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        biometricPromptManager = BiometricPromptManager(this)
        createNotificationChannel(this)

        if(!isInternetAvailable(this)){
            val intent = Intent(this,DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        val googleSO = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("126804112970-tv50779stfmp3oaqj9sah18bdi180v7d.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSO)

        binding.loginBtn.setOnClickListener {
            if (!binding.emailET.text.toString().isEmpty() && !binding.passwordET.text.toString().isEmpty()) {
                login()
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.googleBtn.setOnClickListener {
          googleSSO()
        }

        binding.signUpBtn.setOnClickListener{
            val intent = Intent(this,SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
        if(checkIfUserCredentialsExist()){
            checkBiometricSupport()
        }

    }

    fun createNotificationChannel(context: Context) {
        val name = "DailyNotificationChannel"
        val descriptionText = "Channel for daily notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("DAILY_NOTIFICATION_CHANNEL", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    private fun checkBiometricSupport() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricAuth()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "No biometric hardware available", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Biometric hardware currently unavailable", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No biometric credentials enrolled", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun login() {
        val email = binding.emailET.text.toString()
        val password = binding.passwordET.text.toString()

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()
               if(!checkIfUserCredentialsExist())
               {
                   storeUserDetails()
               }
                 checkUser()
            }

            else {
                Toast.makeText(this, "Log In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginWithSharedPref(){
        val email = getUserEmail() as String
        val password = getUserPassword() as String

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()
                checkUser()
            } else {
                Toast.makeText(this, "Log In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun googleSSO() {
        val googleSignInIntent = googleSignInClient.signInIntent
        startActivityForResult(googleSignInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Successfully Logged In", Toast.LENGTH_SHORT).show()

                    checkUser()
                } else {
                    Toast.makeText(this, "Login unsuccessful", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUser() {
        val userID = auth.uid
        db.collection("Users").whereEqualTo("id", userID).get().addOnSuccessListener { users ->
            if (users.isEmpty) {
                val intent = Intent(this, UserDetailsActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
    private fun biometricAuth() {

        biometricPromptManager.showBiometricPrompt("Authentication Required", "Log in using your biometric credential")

        lifecycleScope.launch {
            biometricPromptManager.promptResults.collect { result ->
                when (result) {
                    is BiometricPromptManager.BiometricResult.AuthenticationSuccess -> {
                        Toast.makeText(this@MainActivity, "Authentication succeeded", Toast.LENGTH_SHORT).show()
                        // Proceed to the main application logic (e.g., navigate to the dashboard)
                        loginWithSharedPref()
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationFailed -> {
                        Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationError -> {
                        Toast.makeText(this@MainActivity, "${result.error}", Toast.LENGTH_SHORT).show()
                    }
                    is BiometricPromptManager.BiometricResult.HardwareUnavailable -> {
                        Toast.makeText(this@MainActivity, "Biometric hardware unavailable", Toast.LENGTH_SHORT).show()
                    }
                    is BiometricPromptManager.BiometricResult.FeatureUnavailable -> {
                        Toast.makeText(this@MainActivity, "No biometric features available", Toast.LENGTH_SHORT).show()
                    }
                    is BiometricPromptManager.BiometricResult.AuthenticationNotSet -> {
                        Toast.makeText(this@MainActivity, "No biometric credentials enrolled", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    private fun checkIfUserCredentialsExist(): Boolean {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        val emailExists = sharedPreferences.contains("email")
        val passwordExists = sharedPreferences.contains("password")

        val email = sharedPreferences.getString("email", null)
        val password = sharedPreferences.getString("password", null)

        return emailExists && email != null && email.isNotEmpty() &&
                passwordExists && password != null && password.isNotEmpty()
    }
    private fun storeUserDetails() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("email", binding.emailET.text.toString())
        editor.putString("password",binding.passwordET.text.toString())
        editor.apply()
    }

    private fun getUserEmail(): String? {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("email", null)
    }
    private fun getUserPassword(): String? {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("password", null)
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}