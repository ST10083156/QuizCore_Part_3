package com.example.opsc_poe_part_2

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.opsc_poe_part_2.databinding.ActivityFriendsBinding
import com.example.opsc_quizcore.Models.UserModel
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import org.w3c.dom.Text

class FriendsActivity : AppCompatActivity() {
    private lateinit var binding : ActivityFriendsBinding
    private lateinit var auth :FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var friendsList: MutableList<UserModel>
    private lateinit var friendsAdapter : FriendsAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityFriendsBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        friendsList = mutableListOf()

        binding.toolbar.pageNameTxt.text = getString(R.string.friends)
        binding.toolbar.backBtn.setOnClickListener {
            val intent = Intent(this,DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.toolbar.backBtn.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.loginStatusBtn.text = getString(R.string.sign_out)
        binding.toolbar.loginStatusBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this,"Signed Out",Toast.LENGTH_SHORT).show()
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.friendsRecyclerView.layoutManager = LinearLayoutManager(this)
        friendsAdapter = FriendsAdapter(friendsList)
        binding.friendsRecyclerView.adapter = friendsAdapter

        getUserFriends()


        binding.addFriendsBtn.setOnClickListener{
            val intent = Intent(this,AddFriendsActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.requestsBtn.setOnClickListener{
            val intent = Intent(this,RequestsActivity::class.java)
            startActivity(intent)
            finish()
        }

    }


    private fun getUserFriends() {
        val userID =auth.uid.toString()
        db.collection("Friends").whereEqualTo("userid",userID).get().addOnSuccessListener {
                snapshot ->
            if(!snapshot.isEmpty){
                for(document in snapshot.documents){
                    val userFriendsList = document.get("friendslist") as? List<Map<String, Any>>
                    if(userFriendsList!=null) {
                        for (userFriend in userFriendsList){
                            val friend = UserModel(
                                id = userFriend["id"] as String,
                                name = userFriend["name"] as String,
                                username = userFriend["username"] as String,
                                image = userFriend["image"] as? String,
                                score = (userFriend["score"] as Number).toInt()
                            )

                            friendsList.add(friend)
                        }
                        friendsAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }
}

class FriendsAdapter(private val friends:MutableList<UserModel>) : RecyclerView.Adapter<FriendsAdapter.MyViewHolder>(){

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val userImage : ShapeableImageView = itemView.findViewById(R.id.userImage)
        val username : TextView = itemView.findViewById(R.id.usernameTV)
        val score : TextView = itemView.findViewById(R.id.friendScoreTV)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.friends_list_item,parent,false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FriendsAdapter.MyViewHolder, position: Int) {
        val user = friends[position]
        holder.score.text = user.score.toString()
        holder.username.text = user.username
        if (friends[position].image != null) {
            holder.userImage.setImageURI(getImageUri(user.image.toString()))
        }
    }

    override fun getItemCount(): Int {
        return friends.size
    }

    fun getImageUri(imagePath: String): Uri? {
        return try {
            Uri.parse(imagePath)
        } catch (e: Exception) {
            null
        }
    }

}