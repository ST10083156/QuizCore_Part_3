package com.example.opsc_poe_part_2

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.Visibility
import com.example.opsc_poe_part_2.databinding.ActivityDashboardBinding
import com.example.opsc_quizcore.Models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding : ActivityDashboardBinding
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    private var user: UserModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        binding.toolbar.backBtn.visibility = View.GONE
        binding.toolbar.pageNameTxt.text = getString(R.string.dashboard)

        if (!isInternetAvailable(this)) {
            binding.usernameTxt.text = getString(R.string.guest)

            binding.friendsBtn.setOnClickListener {
                Toast.makeText(this, "Sign in to access friends list", Toast.LENGTH_SHORT).show()
            }
            binding.leaderboardBtn.setOnClickListener {
                Toast.makeText(this, "Sign in to access leaderboard", Toast.LENGTH_SHORT).show()
            }
            binding.toolbar.loginStatusBtn.text = getString(R.string.sign_in)
            binding.toolbar.loginStatusBtn.setOnClickListener {
                if(isInternetAvailable(this)) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else{
                    Toast.makeText(this,"Please connect to internet first",Toast.LENGTH_SHORT).show()
                }
            }

        }
else{

            val userID = auth.currentUser?.uid.toString()

        db.collection("Users").whereEqualTo("id", userID).limit(1).get()
            .addOnSuccessListener { userSnapshot ->

                if (userSnapshot.documents.isNotEmpty()) {
                    val userDocument = userSnapshot.documents[0]
                    user = userDocument.toObject(UserModel::class.java)


                    if (user?.image != null) {
                        val profileImage = user?.image.toString()
                        val imageUri = Uri.parse(profileImage)
                    }

                    binding.usernameTxt.text = userSnapshot.documents[0].getString("username")
                } else {
                    binding.usernameTxt.text = getString(R.string.user_not_found)

                }
            }
            .addOnFailureListener { e ->
                binding.usernameTxt.text = getString(R.string.error, e.message)
            }

        binding.friendsBtn.setOnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
            binding.toolbar.loginStatusBtn.setOnClickListener {
                auth.signOut()
                Toast.makeText(this,"Signed out",Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
    }
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