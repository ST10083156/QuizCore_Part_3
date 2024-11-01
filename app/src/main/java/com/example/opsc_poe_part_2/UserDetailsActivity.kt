package com.example.opsc_poe_part_2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.opsc_poe_part_2.databinding.ActivityUserDetailsBinding
import com.example.opsc_quizcore.Models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserDetailsActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityUserDetailsBinding
    private var imageUri: Uri? = null
    private lateinit var user: UserModel

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        binding.addImageBtn.setOnClickListener {
            pictureAdd()
        }

        binding.continueBtn.setOnClickListener {

            if (binding.nameET.text.isNotEmpty() && binding.usernameET.text.isNotEmpty()) {
                user = UserModel(
                    id = auth.uid.toString(),
                    name = binding.nameET.text.toString(),
                    username = binding.usernameET.text.toString(),
                    image = imageUri?.toString(),
                    score = 0
                )


                Log.d("UserDetailsActivity", "User data: $user")

                db.collection("Users").add(user).addOnSuccessListener {
                    Toast.makeText(this, "User Details captured successfully",Toast.LENGTH_SHORT).show()
                    val intent = Intent(this,DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this@UserDetailsActivity, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pictureAdd() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            binding.userImageView.setImageURI(selectedImageUri)
            imageUri = selectedImageUri
        }
    }
}